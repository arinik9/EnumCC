package enumeration;

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;

import myUtils.Clustering;
import myUtils.EditDistance;

public abstract class AbstractSearch {

	Clustering initClustering;
	Clustering borderClustering;
	ArrayList<Clustering> path;
	double[][] adjMat;
	public double execTime;
	int maxNbEdit;
	
	int finalEditDistance;
	int nbRepetiton=1;
	
	
	public AbstractSearch(double[][] adjMat_, int maxNbEdit_, int nbRepetiton_, Clustering initClustering_)
	{
		this.adjMat = adjMat_;
		this.maxNbEdit = maxNbEdit_;
		this.nbRepetiton = nbRepetiton_;
		this.initClustering = initClustering_;
		path = new ArrayList<Clustering>();
		path.add(initClustering);
	}
	
	
	public void reset(){
		this.borderClustering = null;
		this.initClustering = null;
		this.path.clear();
		this.execTime = 0;
		this.finalEditDistance = 0;
	}
	
	
	
	public double getExecTime(){
		return(execTime);
	}
	
	
	public int getFinalEditDistance(){
		return(this.finalEditDistance);
	}
	
	
	public ArrayList<Clustering> getPath(){
		return(this.path);
	}
	
	
	public Clustering getBorderClustering(){
		return(this.borderClustering);
	}
	
	
	
	public ArrayList<Clustering> findPathWithDirection(){
		this.execTime = 0;
		long startTime = System.currentTimeMillis();
		
		ArrayList<Clustering> path = new ArrayList<Clustering>();
		
		int pass=0;
		int currNbEdit = 1;
		
		Clustering currClustering  = this.initClustering;
		path.add(currClustering);
		
		while(currClustering != null){
			pass++;
			System.out.println("pass: "+pass+", currNbEdit:"+currNbEdit);

			// Clustering startClustering
			Set<Clustering> neighborClusterings = obtainNeighborhoodByNbEditWithoutTimeLimit(currClustering, currNbEdit);
			//System.out.println("is equal to init: " + this.initClustering.equals(neighborClusterings.iterator().next()));	
			
			// even if neighborClusterings.size()=0, it is ok, it is handled
			ArrayList<Clustering> eligibleNeighborClusterings = selectNeighborhoodWithDirection(currClustering, neighborClusterings);
			//System.out.println("neighborClusterings size: "+neighborClusterings.size()+", eligibleNeighborClusterings size:"+eligibleNeighborClusterings.size());
			if(eligibleNeighborClusterings.size() == 0){ // not found any eligible
				if(currNbEdit == this.maxNbEdit){
					currClustering = null; // stop the main while
				}
				else { // try to find new neighbors with increasing nb edit based on the same startClustering
					currNbEdit++;
				}
			}
			else { // choose a random one
				int randIndx = 0;
				if(eligibleNeighborClusterings.size()>1)
					randIndx = getRandomNumberInRange(0,eligibleNeighborClusterings.size()-1);
				currClustering = eligibleNeighborClusterings.get(randIndx);
				path.add(currClustering);
				
				currNbEdit=1; // reset
			}
			
		} // end of pass
		
		
		long endTime = System.currentTimeMillis();
		this.execTime = (float) (endTime-startTime)/1000;
		
		this.path = path;
		this.borderClustering = path.get(path.size()-1);
		EditDistance eDist = new EditDistance();
		this.finalEditDistance = eDist.calculateEditDistance(this.initClustering,this.borderClustering);
		
		return(path);
	}
	
	
	public abstract ArrayList<Clustering> selectNeighborhoodWithDirection(Clustering currStartClustering,
			Set<Clustering> neighborClusterings);
	

	
	
	
	
	public Set<Clustering> obtainNeighborhoodByNbEditWithoutTimeLimit(Clustering startClustering, int nbEdit)
	{
		MyGenericEnumeration edit = new MyGenericEnumeration(nbEdit, adjMat, startClustering, -1); // TODO for the last parameter
		edit.enumerateByNbEdit(nbEdit);
		Set<Clustering> currFoundClusterings = edit.foundClusterings;
		
		for(Clustering c1 : currFoundClusterings){
			c1.computeImbalance(adjMat); 
			//System.out.println(c1);
		}
		return(currFoundClusterings);
	}
	
	
	
	public int getRandomNumberInRange(int min, int max) {

		if (min >= max) {
			throw new IllegalArgumentException("max must be greater than min");
		}

		Random r = new Random();
		return r.nextInt((max - min) + 1) + min;
	}
	
	
	
//	public void writeClusteringIntoFiles(String outputDirPath){
//
//		if(this.foundClusterings.size()>0){
//			Clustering c = this.foundClusterings.iterator().next(); // there is only 1 element
//			c.setId(0);
//			c.writeMembership(outputDirPath);
//		}
//	}
//
//
//	public void writeStatisticsIntoFiles(String outputDirPath){
//		
//		String filepath = outputDirPath+"/"+"maxNbEdit.txt";
//		writeIntoFile(filepath, this.maxNbEdit+"");
//		
//		// ===============================
//		
//		filepath = outputDirPath+"/"+"execTime.txt";
//		writeIntoFile(filepath, this.execTime+"");
//				
//		// ===============================
//		
//		filepath = outputDirPath+"/"+"finalEditDistance.txt";
//		writeIntoFile(filepath, this.finalEditDistance+"");
//	}
	
	
	
}
