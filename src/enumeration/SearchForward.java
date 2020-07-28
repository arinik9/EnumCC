package enumeration;

import java.util.ArrayList;
import java.util.Set;

import myUtils.Clustering;
import myUtils.EditDistance;



public class SearchForward extends AbstractSearch {
	
	public SearchForward(double[][] adjMat_, int maxNbEdit_, int nbRepetiton_,  Clustering initClustering_)
	{
		super(adjMat_, maxNbEdit_, nbRepetiton_, initClustering_);
	}
	
	
	
	// forward direction
	public ArrayList<Clustering> selectNeighborhoodWithDirection(Clustering currClustering, Set<Clustering> neighborClusterings){
		ArrayList<Clustering> eligibleClusterings = new ArrayList<Clustering>();
		if(neighborClusterings.size()==0)
			return(eligibleClusterings);
		
		for(Clustering neighbor : neighborClusterings){
			EditDistance eDist = new EditDistance();
			int neighborNbEditFromInit = eDist.calculateEditDistance(this.initClustering, neighbor);
			int currNbEditFromInit = eDist.calculateEditDistance(this.initClustering, currClustering);
			
			// strict comparison, because the nb edit distance between 'c' and its parent 'start' is > 0
			if(neighborNbEditFromInit>currNbEditFromInit) 
				eligibleClusterings.add(neighbor);
			else {
//				System.out.println("neighborNbEditFromInit: "+neighborNbEditFromInit);
//				System.out.println("currNbEditFromInit: "+currNbEditFromInit);
			}
		}
		return(eligibleClusterings);
	}
	
}
