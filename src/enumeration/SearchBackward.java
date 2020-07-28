package enumeration;

import java.util.ArrayList;
import java.util.Set;

import myUtils.Clustering;
import myUtils.EditDistance;

// 
public class SearchBackward  extends AbstractSearch {
	
	Clustering foundBorderClustering;
	
	public SearchBackward(double[][] adjMat_, int maxNbEdit_, int nbRepetiton_, Clustering initClustering_, Clustering foundBorderClustering_)
	{
		super(adjMat_, maxNbEdit_, nbRepetiton_, initClustering_);
		foundBorderClustering = foundBorderClustering_;
	}
	
	
	
	// neighborClusterings are the neighbor of 'currStartClustering'
	public ArrayList<Clustering> selectNeighborhoodWithDirection(Clustering currClustering, Set<Clustering> neighborClusterings)
	{
		ArrayList<Clustering> eligibleClusterings = new ArrayList<Clustering>();
		EditDistance eDist = new EditDistance();
		for(Clustering neighbor : neighborClusterings){ // for each neighbor
			int neighborNbEditFromInit = eDist.calculateEditDistance(this.initClustering, neighbor);
			int neighborNbEditFromFoundBorder = eDist.calculateEditDistance(this.foundBorderClustering, neighbor);

			int currNbEditFromInit = eDist.calculateEditDistance(this.initClustering, currClustering);
			int currNbEditFromFoundBorder = eDist.calculateEditDistance(this.foundBorderClustering, currClustering);
			
			// strict comparison, because the nb edit distance between 'c' and its parent 'start' is > 0
			if(neighborNbEditFromInit>currNbEditFromInit && neighborNbEditFromFoundBorder>currNbEditFromFoundBorder) 
				eligibleClusterings.add(neighbor);
		}
		return(eligibleClusterings);
	}
	
	
	
}
