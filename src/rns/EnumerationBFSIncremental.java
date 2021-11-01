package rns;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import myUtils.Clustering;


public class EnumerationBFSIncremental extends AbstractEnumeration {
	
	public Set<Clustering> nextInitClusterings;
	public double tilim;
	public double solLim;
	public boolean isBruteForce;
	public Map<Integer, ArrayList<String>> execTimesByNbEditMap;
	public ArrayList<Integer> clusteringSizesByPass;
	int NB_THREAD;
	double remainingTime;

	
	public EnumerationBFSIncremental(double[][] adjMat_, double tilim_, int solLim_, int NB_THREAD_, int maxNbEdit_, boolean isBruteForce_)
	{
		super(adjMat_, maxNbEdit_);
		idCounter++;
		tilim = tilim_;
		solLim = solLim_;
		isBruteForce = isBruteForce_;
		execTimesByNbEditMap = new HashMap<Integer, ArrayList<String>>();
		for(int i=1; i<=this.maxNbEdit; i++)
			this.execTimesByNbEditMap.put(i, new ArrayList<String>());
		
		clusteringSizesByPass = new ArrayList<Integer>();
		
		NB_THREAD = NB_THREAD_;
		
		nextInitClusterings = new HashSet<Clustering>();
	}
	
	
	public void reset(){
		this.idCounter = 0;
		this.execTime = 0.0;
		this.outDirPath="";
		
		this.execTimesByNbEditMap.clear();
		for(int nbEdit=1; nbEdit<=this.maxNbEdit; nbEdit++)
			this.execTimesByNbEditMap.put(nbEdit, new ArrayList<String>());
		
		this.clusteringSizesByPass.clear();
		this.initClustering = null;
		this.nextInitClusterings.clear();
		this.foundClusterings.clear();
	}
	
	
	// for each init optimal solution, we apply 1-Edit, 2-Edit, 3-Edit, etc. in order.
	// If any new optimal clustering is found, we finish the cycle, and restart again
	public void enumerate(Clustering initClustering, String passOutputDirPath, Set<Clustering> discoveredClusterings){
		
		initClustering.setId(idCounter);
		this.initClustering = initClustering;
		idCounter++;
		
		outDirPath = passOutputDirPath;
		System.out.println("--- "+outDirPath);
		
		if(tilim > 0)
			remainingTime = tilim;
		else
			remainingTime = Double.MAX_VALUE;
		
		//foundClusterings.clear();
		
		foundClusterings.add(this.initClustering);
		discoveredClusterings.add(this.initClustering);
		clusteringSizesByPass.add(nextInitClusterings.size());
		
		enumerateHelper(1, maxNbEdit, passOutputDirPath, discoveredClusterings); // it loads new clusterings into 'nextInitClusterings'

		
		System.out.println("Final size: " + foundClusterings.size());
		System.out.println("execution time: " + execTime + "s");
		
		writeClusteringIntoFiles(outDirPath);
		writeCommonStatisticsIntoFiles(outDirPath);
		writeStatisticsIntoFiles(outDirPath);
	}
	
	
	// for each init optimal solution, we apply 1-Edit, 2-Edit, 3-Edit, etc. in order.
	// If any new optimal clustering is found, we finish the cycle, and restart again
	public void enumerateHelper(int minNbEdit, int maxNbEdit, String passOutputDirPath, Set<Clustering> discoveredClusterings){
		initClustering.setId(idCounter);
		idCounter++;
		
		outDirPath = passOutputDirPath;
		System.out.println("--- "+outDirPath);

		
		//foundClusterings.clear();
		int pass = 0;
		nextInitClusterings.add(this.initClustering);
		foundClusterings.add(this.initClustering);
		clusteringSizesByPass.add(nextInitClusterings.size());
		while(remainingTime>0 && nextInitClusterings.size()>0 && (this.solLim<0 || foundClusterings.size()<this.solLim)){
			pass++;
			System.out.println("pass: "+pass);
//			if(pass==2)
//				break;
			
			double enumTime = 0;
			long startTime = System.currentTimeMillis();

			Set<Clustering> currInitClusterings = new HashSet<Clustering>();
			currInitClusterings.addAll(nextInitClusterings);
			nextInitClusterings.clear();

//			// sequential version, we put it here just in case // TODO there is a bug ...... USE ONLY FOR DEBUG
//			processSeqCurrInitClusteringsWithoutTimeLimit(currInitClusterings, minNbEdit, maxNbEdit, -1, discoveredClusterings);
			
			// ========================================================================================
//////			int timeoutThreads = 10; // 10 seconds => arbitrary
			ArrayList<MyGenericEnumeration> threads;
			if(tilim > 0)
				threads = processParallelCurrInitClusteringsWithTimeLimit(
						currInitClusterings, minNbEdit, maxNbEdit, NB_THREAD, (long) remainingTime, pass);
			else
				threads = processParallelCurrInitClusteringsWithoutTimeLimit(
						currInitClusterings, minNbEdit, maxNbEdit, NB_THREAD, pass);
				
		    //Set<Clustering> currFoundClusterings = new HashSet<>();
	        for (int i=0; i<threads.size(); i++) 
	        {
	        	MyGenericEnumeration myEnum = threads.get(i);
	        	Set<Clustering> currFoundClusterings = myEnum.foundClusterings;
				Set<Clustering> subset = keepUndiscoveredClusterings(currFoundClusterings, discoveredClusterings);
				for(Clustering c1 : subset){ 
					c1.computeImbalance(adjMat); 
					// id should be handled here, since we cannot generate only non-visited clusterings in BFS
					//	so, we need to check if a clustering is already visited or not
					c1.setId(idCounter++); 
					System.out.println("nb edit parent: " + c1.getNbEditParent());
				}
				nextInitClusterings.addAll(subset);
				foundClusterings.addAll(nextInitClusterings);
                discoveredClusterings.addAll(nextInitClusterings);
				
				// ======
				
				for(int nbEdit=minNbEdit;nbEdit<=maxNbEdit;nbEdit++){
					String desc = "solId:"+myEnum.initClustering.getId()+",time:"+myEnum.execTimesByNbEdit[nbEdit-1];
					execTimesByNbEditMap.get(nbEdit).add(desc);
				}
	        }
	        // ========================================================================================
	        clusteringSizesByPass.add(nextInitClusterings.size());
			//System.out.println("Current size: " + foundClusterings.size());
			
	        
	        enumTime = (float) (System.currentTimeMillis()-startTime)/1000;
	        execTime += enumTime;
			if(tilim > 0) { // if time limit is provided by user
				remainingTime = tilim-enumTime;
				System.out.println("22remainingTime: " + remainingTime + "s");
			}
    		System.out.println("Final size (during the passes): " + foundClusterings.size());
		} // end of pass
			
		
		System.out.println("Final size: " + foundClusterings.size());
		System.out.println("execution time: " + execTime + "s");
		
		
		writeClusteringIntoFiles(outDirPath);
		writeCommonStatisticsIntoFiles(outDirPath);
		writeStatisticsIntoFiles(outDirPath);
	}
	
	

	
	public void processSeqCurrInitClusteringsWithoutTimeLimit(Set<Clustering> currInitClusterings, int minNbEdit, int maxNbEdit, int pass, Set<Clustering> discoveredClusterings)
	{
		int initCounter = 0;
		for(Clustering initClustering : currInitClusterings){
			initCounter++;
//			System.out.println("-------------------------");
//			System.out.println("initCounter:"+initCounter+" of " + currInitClusterings.size());
//			System.out.println("Current foundClusterings size: " + foundClusterings.size());
//			System.out.println(initClustering);
//			System.out.println("!!!!!!!!! nb edit parent:" + initClustering.getNbEditParent());
//			Iterator<Clustering> it = foundClusterings.iterator();
//			System.out.println("parent clustering id: "+initClustering.getParentClusteringId());
//			while(it.hasNext()){
//				Clustering p = it.next();
//				if(p.getId() == initClustering.getParentClusteringId()){
//					System.out.println("parent");
//					System.out.println(p);
//					break;
//				}
//			}

			MyGenericEnumeration edit = new MyGenericEnumeration(minNbEdit, maxNbEdit, adjMat, initClustering, pass, isBruteForce);
			edit.enumerate();
			Set<Clustering> currFoundClusterings = edit.foundClusterings;
			
			System.out.println("-------------------------");
			for(Clustering c1 : currFoundClusterings){
				c1.computeImbalance(adjMat); //System.out.println(c1);
				c1.setId(idCounter++); 
//				System.out.println("!!!!!!!!! nb edit parent:" + c1.getNbEditParent());
//				System.out.println(c1);
			}
//			System.out.println("current size: " + currFoundClusterings.size());
			
			Set<Clustering> subset = keepUndiscoveredClusterings(currFoundClusterings, discoveredClusterings);
			//System.out.println("current final size: " + subset.size());

			// for(Clustering c1 : subset){ c1.computeImbalance(adjMat); System.out.println(c1); }
			nextInitClusterings.addAll(subset);
			foundClusterings.addAll(nextInitClusterings);
			System.out.println("final size: " + foundClusterings.size());
		}
		
	}
	
	
	
	public ArrayList<MyGenericEnumeration> processParallelCurrInitClusteringsWithTimeLimit(
			Set<Clustering> currInitClusterings, int minNbEdit, int maxNbEdit, int NB_THREAD, long timeoutThreads, int pass)
	{
		ExecutorService executor = Executors.newFixedThreadPool(NB_THREAD);
		ArrayList<MyGenericEnumeration> threads = new ArrayList<MyGenericEnumeration>();
		List<Callable<MyGenericEnumeration>> callables = new ArrayList<>();
		
		int initCounter = 0;
		for(Clustering initClustering : currInitClusterings){
			initCounter++;
//			System.out.println("-------------------------");
//			System.out.println("nb thread: " + NB_THREAD);
//			System.out.println("now :"+initCounter+" of " + currInitClusterings.size());
//			System.out.println("Current size: " + foundClusterings.size());
			
			MyGenericEnumeration e = new MyGenericEnumeration(minNbEdit, maxNbEdit, adjMat, initClustering, pass, isBruteForce);
			threads.add(e);
	    	//executor.execute(e);
			
			Callable<MyGenericEnumeration> task = () -> {
				e.enumerate();
				return e;
			};
			callables.add(task);
		} // end of initClustering
		
		try {
			// source1: https://stackoverflow.com/questions/2758612/executorservice-that-interrupts-tasks-after-a-timeout
			// source2: https://gkemayo.developpez.com/tutoriels/java/introduction-java-8-concurrency/
			// source3: https://www.baeldung.com/java-runnable-callable
			
			// whatever the size, timeout is 'timeoutThreads' seconds for the totality of all these tasks
			System.out.println("tilim: "+ timeoutThreads);
			executor.invokeAll(callables, timeoutThreads*1000, TimeUnit.MILLISECONDS); 
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		//Tear Down
	    executor.shutdown();

	    //Wait for all threads to finish
	    while (!executor.isTerminated())
	    {
	        //wait for infinity time
	    }
	    System.out.println("Finished threads with time limit!!");	
	    
	    return(threads);
	} 
	
	
	
	
	public ArrayList<MyGenericEnumeration> processParallelCurrInitClusteringsWithoutTimeLimit(
			Set<Clustering> currInitClusterings, int minNbEdit, int maxNbEdit, int NB_THREAD, int pass)
	{
		ExecutorService executor = Executors.newFixedThreadPool(NB_THREAD);
		ArrayList<MyGenericEnumeration> threads = new ArrayList<MyGenericEnumeration>();
		
		int initCounter = 0;
		for(Clustering initClustering : currInitClusterings){
			initCounter++;
//			System.out.println("-------------------------");
//			System.out.println("nb thread: " + NB_THREAD);
//			System.out.println("now :"+initCounter+" of " + currInitClusterings.size());
//			System.out.println("Current size: " + foundClusterings.size());
//			System.out.println("maxNbEdit: " + maxNbEdit);

			MyGenericEnumeration e = new MyGenericEnumeration(minNbEdit, maxNbEdit, adjMat, initClustering, pass, isBruteForce);
			threads.add(e);
	    	executor.execute(e);

		} // end of initClustering

		
		//Tear Down
	    executor.shutdown();

	    //Wait for all threads to finish
	    while (!executor.isTerminated())
	    {
	        //wait for infinity time
	    }
	    System.out.println("Finished all threads (without time limit) !!");	
	    
	    return(threads);
	} 
	
	
	public Set<Clustering> keepUndiscoveredClusterings(Set<Clustering> candidates, Set<Clustering> discoveredClusterings){
		Set<Clustering> subset = new HashSet<Clustering>(); // hashset does not eliminate duplicated Clustering objects
		
		for(Clustering c1 : candidates){
			// --------------------
			boolean skip = false;
			for(Clustering c2 : subset){
				if(c2.equals(c1)){
					skip = true;
					break;
				}
			}
			// --------------------
			if(!skip){
				boolean add = true;
				for(Clustering c2 : discoveredClusterings){
					if(c1.equals(c2)){
						add = false;
						break;
					}
				}
				if(add)
					subset.add(c1);
			}
		}
		return(subset);
	}
	
	


	public void writeStatisticsIntoFiles(String outputDirPath){
		String filepath;

		// ===============================
		
		for(int nbEdit=1;nbEdit<=maxNbEdit;nbEdit++){
			String content="";
			for(String t : execTimesByNbEditMap.get(nbEdit))
				content += t+"\n";
			
			filepath = outputDirPath+"/"+"execTimes_nbEdit"+nbEdit+".txt";
			writeIntoFile(filepath, content);
		}
		
		// ===============================
		
		filepath = outputDirPath+"/"+"nbPass.txt";
		writeIntoFile(filepath, clusteringSizesByPass.size()+"");
		
		// ===============================

		
		String content="";
		for(Integer size : clusteringSizesByPass)
			content += size+"\n";
		
		filepath = outputDirPath+"/"+"clusteringSizesByPass.txt";
		writeIntoFile(filepath, content);
	
	}
	

	
	
}
