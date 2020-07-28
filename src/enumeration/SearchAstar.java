package enumeration;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import myUtils.Clustering;
import myUtils.EditDistance;

public class SearchAstar extends EnumerationBFS {

	HashSet<SolutionState> closedList = new HashSet<>();
	Map<Long, SolutionState> discoveredSolutions = new HashMap<>();
	Map<Long, Long> cameFrom = new HashMap<>();
	Map<Long, Integer> gScore = new HashMap<>();
	Map<Long, Integer> fScore = new HashMap<>();
	
	ArrayList<Clustering> path;
	int costPath;
	
	TreeSet<SolutionState> orderedSet = new TreeSet<SolutionState>(
		new Comparator<SolutionState>(){

			@Override
			public int compare(SolutionState o1, SolutionState o2) {
				int value=0;
				if(o1.fscore > o2.fscore)
					value = 1;
				else if(o1.fscore < o2.fscore)
					value = -1;
				else { // if equal fscore ===============> we need to handle the equality, otherwise, it will not be inserted into treeset
					if(o1.eDistToTarget > o2.eDistToTarget)
						value = 1;
					else if(o1.eDistToTarget < o2.eDistToTarget)
						value = -1;
					else { // if equal eDistToTarget
						if(o1.eDistToStart > o2.eDistToStart)
							value = -1;
						else if(o1.eDistToStart < o2.eDistToStart)
							value = 1;
//						else { // if equal eDistToStart
//							if(!o1.solution.equals(o2.solution)) // if we do not allow the same objects, otherwise keep all of them
//								value = 1;
//						}
					}
				}
				return value;
			}
			
		}
	);
	
	
	public SearchAstar(double[][] adjMat_, int NB_THREAD_, int maxNbEdit_)
	{	super(adjMat_, -1, NB_THREAD_, maxNbEdit_);
		path = new ArrayList<Clustering>();
		//path.add(initClustering);
	}
	
	
	// we find shorthest path in terms of edit distance
	// https://en.wikipedia.org/wiki/A*_search_algorithm
	public boolean findPathIfExists(Clustering startClustering, Clustering targetClustering){
		long startTime = System.currentTimeMillis();
		
//		ArrayList<Clustering> clusterings = new ArrayList<Clustering>();
//		for(int i=0; i<1093; i++){
//			int[] membership = readMembership("out/net/1", i, adjMat.length);
//			Clustering c = new Clustering(membership, -1);
//			c.computeImbalance(adjMat);
//			clusterings.add(c);
//		}
		
		EditDistance eDist1 = new EditDistance();
		int nbEditFromStartToTarget = eDist1.calculateEditDistance(startClustering, targetClustering);
		
		startClustering.setId(idCounter);
		gScore.put(idCounter,0);
		idCounter++;
		SolutionState solStart = new SolutionState(startClustering, nbEditFromStartToTarget, 0);
		orderedSet.add(solStart);
		
		int pass = 0;
		while(orderedSet.size()>0){
			pass++;
			// System.out.println("pass: "+pass);
			
			SolutionState currSol = orderedSet.pollFirst();
			Clustering currClustering = currSol.getClustering();
			closedList.add(currSol);
			
//			for(int i=0; i<1093; i++){
//				if(currClustering.equals(clusterings.get(i))){
//					System.out.println("Curr Clustering Id in solution space: "+i);
//					break;
//				}
//			}
			
			long currId = currClustering.getId();
			currClustering.computeImbalance(adjMat);
			discoveredSolutions.put(currId,currSol);
			costPath += currClustering.getNbEditParent(); // it is only for print
			if(currClustering.equals(targetClustering)){
				reconstructPath(currClustering);
				// print this.path
				System.out.println("FINISHED !!! path cost: "+costPath);
				System.out.println("final path:");
				System.out.println(this.path);
				return(true);
			}
			System.out.println("pass: "+pass+", gScore.get(currId):"+gScore.get(currId)+", nbEditFromTarget:"+currSol.eDistToTarget);
			//System.out.println(currClustering);
			
			Set<Clustering> currInitClusterings = new HashSet<Clustering>();
			currInitClusterings.add(currClustering); // since we insert 1 element at a time, nb thread will be 1
			ArrayList<MyGenericEnumeration> threads = processParallelCurrInitClusteringsWithoutTimeLimit(
					currInitClusterings, maxNbEdit, NB_THREAD, -1); //// TODO for the last parameter
			
		    // collect the results
	        for (int i=0; i<threads.size(); i++) 
	        {	MyGenericEnumeration myEnum = threads.get(i);
	        	Set<Clustering> neighbors = myEnum.foundClusterings;
	        	System.out.println("neighbors.Size():" + neighbors.size());
	        	
				for(Clustering neighbor : neighbors){ // for each neighbor
					int tentative_gScore = neighbor.getNbEditParent() + gScore.get(currId);
					
//					EditDistance eDist2 = new EditDistance();
//					int neighborNbEditFromTarget2 = eDist2.calculateEditDistance(targetClustering, neighbor);
//					for(int j=0; j<1093; j++){
//						if(neighbor.equals(clusterings.get(j))){
//							System.out.println("enumerated from nbEdit:" +neighbor.getNbEditParent()+
//									" => neighbor Clustering Id in solution space: "+j+", nb edit fromt target:"
//									+neighborNbEditFromTarget2+", tentative_gScore:"+tentative_gScore);
//							break;
//						}
//						
//					}
					
					//EditDistance eDist = new EditDistance();
					//int neighborNbEditFromTarget;
					//int neighborNbEditFromStart = eDist.calculateEditDistance(startClustering, neighbor);
					
					// we do not know the id of the clustering when its generated from the enumeration process, we assign it afterwards
					neighbor = bindToExistingClusteringIfAlreadyDiscovered(neighbor);
					long neighborId = neighbor.getId();
					boolean alreadyDiscovered = true;
					if(neighborId == -1){// if it is not already discovered
						foundClusterings.add(neighbor);
						
						alreadyDiscovered = false;
						neighbor.setId(idCounter);
						neighborId = idCounter;
						idCounter++; // prep for the next one
						neighbor.computeImbalance(adjMat); //System.out.println(c1);
					}
					
					// This path to neighbor is better than any previous one
//					int currGscoreTemp = -1000;
//					if(alreadyDiscovered) currGscoreTemp = gScore.get(neighborId);
					
					if(!closedList.contains(neighbor)){
					
						if(!alreadyDiscovered || (alreadyDiscovered && tentative_gScore < gScore.get(neighborId))){
							cameFrom.put(neighborId, currId);
							gScore.put(neighborId, tentative_gScore);
							EditDistance eDist = new EditDistance();
							int neighborNbEditFromTarget = eDist.calculateEditDistance(targetClustering, neighbor);
							fScore.put(neighborId, tentative_gScore+neighborNbEditFromTarget);
							SolutionState neighborSol = new SolutionState(neighbor, neighborNbEditFromTarget, tentative_gScore);
							//System.out.println("before => orderedSet size: " + orderedSet.size());
							orderedSet.add(neighborSol);
							//System.out.println("after => orderedSet size: " + orderedSet.size());
							discoveredSolutions.put(neighborId, neighborSol);
						}
					
					}
						
				}
				
	        }
	        // ========================================================================================
//			System.out.println("foundClusterings size: " + foundClusterings.size());
//			Iterator<SolutionState> it = orderedSet.iterator();
//			while(it.hasNext()){
//				System.out.println("fscore size: " + it.next().fscore);
//			}
//			System.out.println("---------------");

			
		} // end of pass
			
		
		long endTime = System.currentTimeMillis();
		execTime = (float) (endTime-startTime)/1000;
		System.out.println("execution time: " + execTime + "s");
		
		
		
		return(false);
	}
	
	
	public void reconstructPath(Clustering currClustering){
		path.add(currClustering);
		while(cameFrom.containsKey(currClustering.getId())){
			long parentId = cameFrom.get(currClustering.getId());
			currClustering = discoveredSolutions.get(parentId).getClustering();
			path.add(currClustering);
		}
	}
	
	
	public Clustering bindToExistingClusteringIfAlreadyDiscovered(Clustering c){
		for(Clustering c2 : foundClusterings){
		//for(Clustering c2 : discoveredSolutions.entrySet().){
			if(c.equals(c2)){
				c.setId(c2.getId());
				break;
			}
		}
		return(c);
	}
	
	
	public class SolutionState {
		int fscore;
		Clustering solution;
		int eDistToStart;
		int eDistToTarget;
		
		public SolutionState(Clustering solution, int eDistToTarget, int eDistToStart){
			this.solution = solution;
			this.fscore = eDistToTarget+eDistToStart;
			this.eDistToStart = eDistToStart;
			this.eDistToTarget = eDistToTarget;
		}
		
		public Clustering getClustering(){
			return(this.solution);
		}
		
		// Overriding equals() to compare two Complex objects 
	    @Override
	    public boolean equals(Object o) { 
	        // If the object is compared with itself then return true   
	        if (o == this) { 
	            return true; 
	        } 
	  
	        /* Check if o is an instance of Complex or not "null instance of [type]" also returns false */
	        if (!(o instanceof SolutionState)) { 
	            return false; 
	        } 
	          
	        // type cast o to Complex so that we can compare data members  
	        SolutionState ss = (SolutionState) o; 

	        // Compare the data members and return accordingly
	        boolean equal = false;
	        if(this.solution.equals(ss.getClustering()))
	        	equal = true;
	        
	        return equal; 
	    } 
	}
	
	
//	/**
//	 * read a solution from file
//	 * 
//	 */
//	public int[] readMembership(String inputDirPath, long id_, int n){
//		String fileName = "membership" + id_ + ".txt";
//		String filepath = inputDirPath + "/" + fileName;
//		int[] membership_ = new int[n];
//		
//		try{
//			InputStream  ips = new FileInputStream(filepath);
//			InputStreamReader ipsr=new InputStreamReader(ips);
//			BufferedReader br = new BufferedReader(ipsr);
//			String line;
//			  
//			for(int i=0; i<n; i++){ // for each node
//				line = br.readLine();
//				membership_[i] = Integer.parseInt(line);	
//			}
//			
//			line = br.readLine();
//			br.close();
//			
//			// verify that the file we just read corresponds to a correct nb node
//			if(line != null){
//				return(null);
//			}
//		
//		}catch(Exception e){
//		  System.out.println(e.toString());
//		  return(null);
//		}
//		
//		return(membership_);
//	}
	
}



