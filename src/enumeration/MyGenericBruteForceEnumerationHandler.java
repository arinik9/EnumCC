package enumeration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import enumeration.MyGenericBruteForceEnumeration;
import myUtils.Clustering;
import myUtils.Combination;


public class MyGenericBruteForceEnumerationHandler {
	
	String outDirPath = "";
	double[][] adjMat;
	public Set<Clustering> nextInitClusterings = new HashSet<Clustering>();
	public Set<Clustering> foundClusterings = new HashSet<Clustering>();
	long idCounter;
	
	//static final int NB_CHUNK = 5;
	//static final int NB_THREAD = 2;
	
	public MyGenericBruteForceEnumerationHandler(double[][] adjMat_, Clustering initClustering_, String outDirPath_){
		outDirPath = outDirPath_;
		adjMat = adjMat_;
		nextInitClusterings.add(initClustering_);
		foundClusterings.add(initClustering_);
		idCounter=initClustering_.getId();
	}
	
	
	// for each init optimal solution, we apply 1-Edit, 2-Edit, 3-Edit, etc. in order. If any new optimal clustering is found, we finish the cycle, and restart again
	public void enumerate(int maxNbEdit, int NB_THREAD){
		long startTime = System.currentTimeMillis();
		
		//foundClusterings.clear();
		int pass = 0;
		while(true){
			if(nextInitClusterings.size() == 0)
				break;
			System.out.println("pass: " + pass++);
			
			Set<Clustering> currInitClusterings = new HashSet<Clustering>();
			currInitClusterings.addAll(nextInitClusterings);
			nextInitClusterings.clear();
			
			for(int nbEdit=1;nbEdit<=maxNbEdit;nbEdit++){
				System.out.println("-------------------------");
				System.out.println("nbEdit: " + nbEdit);
				for(Clustering initClustering : currInitClusterings){
					
					if(NB_THREAD>1){
						// ===============================================================================
						ExecutorService executor = Executors.newFixedThreadPool(NB_THREAD);
						ArrayList<MyGenericBruteForceEnumeration> threads = new ArrayList<MyGenericBruteForceEnumeration>();
						
						List<Integer> clusterIds = new ArrayList<Integer>();
						for(int i=1; i<=initClustering.getNbCluster(); i++)
							clusterIds.add(i);
						
						//List<int[]> allPermCombClusterIds = new ArrayList<>();
						for(int nbSourceCluster=1; nbSourceCluster<=nbEdit; nbSourceCluster++){
							List<int[]> combClusterIds = Combination.generate(clusterIds, nbSourceCluster);
							
							for(int[] subClusterIds : combClusterIds){
								//MyGenericEnumeration e = new MyGenericEnumeration(nbEdit, adjMat, initClustering, idCounter);
								MyGenericBruteForceEnumeration e = new MyGenericBruteForceEnumeration(nbEdit, adjMat, initClustering);
								e.setNbSourceCluster(nbSourceCluster);
								e.setClusterIds(subClusterIds);
								
								threads.add(e);
						    	executor.execute(e);
							}
						}
						
						//Tear Down
					    executor.shutdown();

					    //Wait for all threads to finish
					    while (!executor.isTerminated())
					    {
					        //wait for infinity time
					    }
					    System.out.println("Finished all threads");	
				        
				        
					    //Set<Clustering> currFoundClusterings = new HashSet<>();
				        for (int i=0; i<threads.size(); i++) 
				        {
				        	Set<Clustering> currFoundClusterings = threads.get(i).foundClusterings;
							Set<Clustering> subset = keepUndiscoveredClusterings(currFoundClusterings);
							nextInitClusterings.addAll(subset);
				        }
					    //System.out.println (nextInitClusterings);
						foundClusterings.addAll(nextInitClusterings);
						
					    
					 // ===============================================================================	
					} else {
						//MyGenericEnumeration e = new MyGenericEnumeration(nbEdit, adjMat, initClustering, idCounter);
						MyGenericBruteForceEnumeration e = new MyGenericBruteForceEnumeration(nbEdit, adjMat, initClustering);
						e.enumerate();
						
						Set<Clustering> currFoundClusterings = e.foundClusterings;
						System.out.println("-------------------------");
						for(Clustering c1 : currFoundClusterings){
							c1.computeImbalance(adjMat);
							System.out.println(c1);
						}
						System.out.println("current size: " + currFoundClusterings.size());
						
						Set<Clustering> subset = keepUndiscoveredClusterings(currFoundClusterings);
						System.out.println("current final size: " + subset.size());

						nextInitClusterings.addAll(subset);
						foundClusterings.addAll(nextInitClusterings);
					}
					
				}
			}
			
			System.out.println("Current size: " + foundClusterings.size());
		} // end of pass
			
		
		System.out.println("-------------------------");
		for(Clustering c1 : foundClusterings){
			c1.computeImbalance(adjMat);
			System.out.println(c1);
		}
		System.out.println("Final size: " + foundClusterings.size());
		
		long endTime = System.currentTimeMillis();
		double execTime = (float) (endTime-startTime)/1000;
		System.out.println("execution time: " + execTime + "s");
	}
	
	
//	public void enumerateByNbEditNumber(int nbEdit){
//		//foundClusterings.clear();
//		long startTime = System.currentTimeMillis();
//		
//		int pass = 0;
//		while(true){
//			if(nextInitClusterings.size() == 0)
//				break;
//			System.out.println("pass: " + pass++);
//			
//			Set<Clustering> currInitClusterings = new HashSet<Clustering>();
//			
//			currInitClusterings.addAll(nextInitClusterings);
//			nextInitClusterings.clear();
//			
//			for(Clustering initClustering : currInitClusterings){
//				//MyGenericEnumeration e = new MyGenericEnumeration(nbEdit, adjMat, initClustering, idCounter);
//				MyGenericBruteForceEnumeration e = new MyGenericBruteForceEnumeration(nbEdit, adjMat, initClustering);
//				e.enumerate();
//				Set<Clustering> currFoundClusterings = e.foundClusterings;
//				Set<Clustering> subset = keepUndiscoveredClusterings(currFoundClusterings);
//				nextInitClusterings.addAll(subset);
//				foundClusterings.addAll(nextInitClusterings);
//			}
//			
//			for(Clustering c1 : foundClusterings){
//				c1.computeImbalance(adjMat);
//				System.out.println("size: " + foundClusterings.size());
//			}
//		
//		}
//		
//		System.out.println("-------------------------");
//		for(Clustering c1 : foundClusterings){
//			c1.computeImbalance(adjMat);
//		}
//		System.out.println("size: " + foundClusterings.size());
//		long endTime = System.currentTimeMillis();
//		double execTime = (float) (endTime-startTime)/1000;
//		System.out.println("execution time: " + execTime + "s");
//	}
	
	
	public Set<Clustering> keepUndiscoveredClusterings(Set<Clustering> candidates){
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
				for(Clustering c2 : foundClusterings){
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
}
