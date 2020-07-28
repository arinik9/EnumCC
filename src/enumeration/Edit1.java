package enumeration;

import java.util.ArrayList;
import java.util.Set;
//import java.util.Random;

import myUtils.Clustering;

public class Edit1 extends AbstractEdit {
	
	public Edit1(double[][] adjMat_, Clustering initClustering_){
		super(adjMat_,initClustering_);
		
	}
	
	@Override
	public void enumerate(){
		foundClusterings.clear();	
		ArrayList<ArrayList<Integer>> clusters = initClustering.getClustersInArrayFormat();
		
		for(int clusterId=1; clusterId<=initClustering.getNbCluster(); clusterId++){
			for(int i=0; i<clusters.get(clusterId-1).size(); i++){ // for each node of a cluster
				int nodeId = clusters.get(clusterId-1).get(i);
				
				ArrayList<Integer> notEqualToValues = new ArrayList<>();
				notEqualToValues.add(clusterId);
				TNode node = new TNode(nodeId, clusterId, 1, notEqualToValues); // here targetIndex=-1
				ArrayList<TNode> selNodes = new ArrayList<TNode>();
				selNodes.add(node);
				
				ArrayList<ArrayList<TNode>> optimalTransformations = findCandidateTransformations(selNodes);
				Set<Clustering> set = enumerateClusterings(optimalTransformations);
				if(set.size()>0)
					foundClusterings.addAll(set);
			}
		}
	}

	
//	// node id
//	// cluster id: the cluster no to which node belongs
//	//@Override
//	public ArrayList<Integer> findCandidateClusterIdsOfNode(int nodeId, int clusterId){
//		ArrayList<Integer> candidateClusterIds = new ArrayList<Integer>();
//		double nodeFitnessValue = g.weightSumInClusters[nodeId][clusterId-1];
//		double currFitnessValue;
//		for(int cid=1; cid<=initClustering.getNbCluster(); cid++){
//			if(cid != clusterId){
//				currFitnessValue = g.weightSumInClusters[nodeId][cid-1];
//				if(nodeFitnessValue == currFitnessValue)
//					candidateClusterIds.add(cid);
//			}
//		}
//		
//		// last verification: if the node u is not already a single cluster node, it can leave its cluster to create its own cluster
//		if(nodeFitnessValue == 0.0 && initClustering.clusterSizes[clusterId-1]>1)
//			candidateClusterIds.add(initClustering.getNbCluster()+1); // a new cluster
//		
//		return(candidateClusterIds);
//	}
//	
	

	
}
