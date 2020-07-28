package enumeration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import myUtils.Clustering;
import myUtils.EditDistance;

public class SearchForward2 extends EnumerationBFS {

	Map<Long, SolutionState> discoveredSolutions = new HashMap<>();
	Map<Long, Long> cameFrom = new HashMap<>();
	Map<Long, Integer> gScore = new HashMap<>();
	Map<Long, Double> fScore = new HashMap<>();
	
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
				return value;
			}
		}
	);
	
	
	public SearchForward2(double[][] adjMat_, int NB_THREAD_, int maxNbEdit_)
	{	super(adjMat_, -1, NB_THREAD_, maxNbEdit_);
		path = new ArrayList<Clustering>();
		//path.add(initClustering);
	}
	
	// it is Astart-alike algo
	// https://en.wikipedia.org/wiki/A*_search_algorithm
	public boolean findPathForward(Clustering startClustering){
		long startTime = System.currentTimeMillis();
		
		
		startClustering.setId(idCounter);
		gScore.put(idCounter,0);
		idCounter++;
		SolutionState solStart = new SolutionState(startClustering, 0, 0.0, 0);
		orderedSet.add(solStart);
		
		Clustering bestBorderClustering = startClustering;
		int bestNbEditFromStart = 0;
		
		int pass = 0;
		while(orderedSet.size()>0){
			pass++;
			// System.out.println("pass: "+pass);
			
			SolutionState currSol = orderedSet.pollFirst();
			Clustering currClustering = currSol.getClustering();
			long currId = currClustering.getId();
			discoveredSolutions.put(currId,currSol);
			System.out.println("pass: "+pass+", gScore.get(currId):"+gScore.get(currId)
			+ ", estTarget:"+currSol.estTarget + ", nbEditFromStart:"+currSol.nbEditFromStart);
			//System.out.println(currClustering);
			
			if(bestNbEditFromStart<currSol.nbEditFromStart){
				bestNbEditFromStart = currSol.nbEditFromStart;
				bestBorderClustering = currClustering;
			}
			
			Set<Clustering> currInitClusterings = new HashSet<Clustering>();
			currInitClusterings.add(currClustering); // since we insert 1 element at a time, nb thread will be 1
			ArrayList<MyGenericEnumeration> threads = processParallelCurrInitClusteringsWithoutTimeLimit(
					currInitClusterings, maxNbEdit, NB_THREAD, -1); // TODO for the last parameter
			
		    // collect the results
	        for (int i=0; i<threads.size(); i++) 
	        {	MyGenericEnumeration myEnum = threads.get(i);
	        	Set<Clustering> neighbors = myEnum.foundClusterings;
				
				for(Clustering neighbor : neighbors){ // for each neighbor
					int tentative_gScore = neighbor.getNbEditParent() + gScore.get(currId);
					EditDistance eDist = new EditDistance();
					int neighborNbEditFromStart = eDist.calculateEditDistance(startClustering, neighbor);
					
					if(neighborNbEditFromStart>=currSol.eDistToStart){
						//EditDistance eDist = new EditDistance();
						//int neighborNbEditFromTarget;
						//int neighborNbEditFromStart = eDist.calculateEditDistance(startClustering, neighbor);
						
						// we do not know the id of the clustering when its generated from the enumeration process, we assign it afterwards
						neighbor = bindToExistingClusteringIfAlreadyDiscovered(neighbor);
						long neighborId = neighbor.getId();
						boolean alreadyDiscovered = true;
						if(neighborId == -1){// if it is not already discovered
							alreadyDiscovered = false;
							neighbor.setId(idCounter);
							neighborId = idCounter;
							idCounter++; // prep for the next one
							neighbor.computeImbalance(adjMat); //System.out.println(c1);
						}
						
						// This path to neighbor is better than any previous one
						if(!alreadyDiscovered || (alreadyDiscovered && tentative_gScore < gScore.get(neighborId))){
							cameFrom.put(neighborId, currId);
							gScore.put(neighborId, tentative_gScore);
							double estimationTarget = (float) 1/neighborNbEditFromStart;
							fScore.put(neighborId, tentative_gScore+estimationTarget);
							SolutionState neighborSol = new SolutionState(neighbor, neighborNbEditFromStart, estimationTarget, tentative_gScore);
							orderedSet.add(neighborSol);
							discoveredSolutions.put(neighborId, neighborSol);
						}
					
					}
						
				}
				
	        }
	        // ========================================================================================
			//System.out.println("Current size: " + foundClusterings.size());
			
		} // end of pass
		
		
		reconstructPath(bestBorderClustering);

		System.out.println("FINISHED !!! path cost: "+costPath);
		System.out.println("FINISHED !!! bestNbEditFromStartt: "+bestNbEditFromStart);
		System.out.println("path length: " + this.path.size());
		System.out.println("path:");
		System.out.println(this.path);


		
		long endTime = System.currentTimeMillis();
		execTime = (float) (endTime-startTime)/1000;
		System.out.println("execution time: " + execTime + "s");
		
		
		
		return(false);
	}
	
	
	public void reconstructPath(Clustering currClustering){
		path.add(currClustering);
		costPath += currClustering.getNbEditParent();
		while(cameFrom.containsKey(currClustering.getId())){
			long parentId = cameFrom.get(currClustering.getId());
			currClustering = discoveredSolutions.get(parentId).getClustering();
			path.add(currClustering);
			costPath += currClustering.getNbEditParent();
		}
	}
	
	
	public Clustering bindToExistingClusteringIfAlreadyDiscovered(Clustering c){
		for(Clustering c2 : foundClusterings){
			if(c.equals(c2)){
				c.setId(c2.getId());
				break;
			}
		}
		return(c);
	}
	
	
	public class SolutionState {
		Double fscore;
		Clustering solution;
		int eDistToStart;
		Double estTarget;
		int nbEditFromStart;
		
		// eDistToStart is calculated from successive nb edit values, like sum of edge weights
		public SolutionState(Clustering solution, int nbEditFromStart, Double estTarget, int eDistToStart){
			this.solution = solution;
			this.nbEditFromStart = nbEditFromStart;
			this.fscore = estTarget+eDistToStart;
			this.eDistToStart = eDistToStart;
			this.estTarget = estTarget;
		}
		
		public Clustering getClustering(){
			return(this.solution);
		}
	}
	
	
}



