package enumeration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import myUtils.Clustering;

public class EstimateDiameterSolutionClass extends AbstractEnumeration {

	int nbRepetiton=1;
	ArrayList<Clustering> bestPath;

	
	public EstimateDiameterSolutionClass(double[][] adjMat_, int maxNbEdit_, int nbRepetiton_){
		super(adjMat_, maxNbEdit_);
		nbRepetiton = nbRepetiton_;
		bestPath = new ArrayList<Clustering>();
	}
	
	
	public void reset(){
		this.initClustering = null;
		this.outDirPath="";
		this.bestPath.clear();
		this.foundClusterings.clear();
		this.bestEditDistance=0;
		this.idCounter=0;
		this.execTime=0.0;
		//this.nbRepetiton=1;
	}
	
	
	@Override
	public void enumerate(Clustering initClustering, String passOutputDirPath,  Set<Clustering> discoveredClusterings) {
		this.initClustering = initClustering;
		this.outDirPath = passOutputDirPath;
		
		findBestEstimatedPath();
		System.out.println("best edit distance: " + this.bestEditDistance);
		this.foundClusterings.addAll(this.bestPath);
		
		writeClusteringIntoFiles(outDirPath);
		writeCommonStatisticsIntoFiles(outDirPath);
		//writeStatisticsIntoFiles(outDirPath);
	}
	
	
	
	// for each iteration
	// 1) fist, search forward, and obtain a path, called 'forwardPath'
	// 2) then search backward, and obtain the best path among the ones found during repetitions, called 'bestbackwardPath'
	// 3) create a new path object, reverse the 'backwardPath', and add it into the new path object. Then add 'forwardPath'
	public void findBestEstimatedPath(){
		
		Clustering nextStartClustering = this.initClustering; 
		bestPath.add(nextStartClustering); // in case there is only 1 optimal solution
		this.bestEditDistance = 0; // init

		
		// our aim is to find the best combination of forward and backward paths 
		//		so as to maximize the final edit distance of the sum of both paths 
		for(int i=1; i<=this.nbRepetiton; i++){ // for each forward search
			System.out.println("repetition: "+i);
			AbstractSearch searchForward = new SearchForward(adjMat, maxNbEdit, nbRepetiton, this.initClustering);
			searchForward.findPathWithDirection();
			
			// path.size()>1 means there is any other clustering other than start clustering
			if(searchForward.getPath().size()>1){
				System.out.println("forward path size: "+searchForward.getPath().size());
				AbstractSearch bestFoundBackwardSearch = findBestBackwardPath(searchForward.getBorderClustering());
				System.out.println("backward path size: "+bestFoundBackwardSearch.getPath().size());
				int currFinalEditDistance = searchForward.getFinalEditDistance() + bestFoundBackwardSearch.getFinalEditDistance();

				if(this.bestEditDistance<currFinalEditDistance){
					this.bestEditDistance = currFinalEditDistance;
					this.execTime += searchForward.getExecTime() + bestFoundBackwardSearch.getExecTime();
					
					this.bestPath.clear();
					ArrayList<Clustering> bestBackwardPath = bestFoundBackwardSearch.getPath();
					if(bestBackwardPath.size()>0){
						bestBackwardPath.remove(0);
						Collections.reverse(bestBackwardPath); // reverse the order
					}
					this.bestPath.addAll(bestBackwardPath);
					this.bestPath.addAll(searchForward.getPath());
				} 
			}
			//System.out.println("finalEditDistance: "+finalEditDistance);
		}
		//System.out.println(bestClustering);
		
		System.out.println("execution time: " + this.execTime + "s");	
	}
	
	
	
	public AbstractSearch findBestBackwardPath(Clustering forwardBorderClustering){
		AbstractSearch bestSearchBackward = new SearchBackward(adjMat, maxNbEdit, nbRepetiton, this.initClustering, 
				forwardBorderClustering); // init clustering is included in the path variable, in case there is only 1 optimal solution
		
		int bestFinalEditDistance = 0;
		for(int i=1; i<=this.nbRepetiton; i++){
			System.out.println("repetition: "+i);
			
			AbstractSearch searchBackward = new SearchBackward(adjMat, maxNbEdit, nbRepetiton, this.initClustering, 
					forwardBorderClustering);
			 searchBackward.findPathWithDirection();
			 ArrayList<Clustering> currBestPath = searchBackward.getPath();
			
			// currBestPath.size()>1 means there is any other clustering other than start clustering
			if(currBestPath.size()>1){
				int currFinalEditDistance = searchBackward.getFinalEditDistance();
				
				if(bestFinalEditDistance<currFinalEditDistance){
					bestSearchBackward = searchBackward;
				}
			}
			//System.out.println("bestFinalEditDistance: "+bestFinalEditDistance);
		}
		//System.out.println(bestClustering);
		
		return(bestSearchBackward);
	}
	
	
	
//	public void writeStatisticsIntoFiles(String outputDirPath){
//		String filepath ="";
//		// TODO
//		String content="";
//		// TODO
//		writeIntoFile(filepath, content);
//	
//	}
	
}
