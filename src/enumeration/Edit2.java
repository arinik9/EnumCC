package enumeration;

import java.util.ArrayList;
import java.util.Set;

import myUtils.Clustering;



public class Edit2 extends AbstractEdit {

	
	public Edit2(double[][] adjMat_, Clustering initClustering_){
		super(adjMat_,initClustering_);
	}
	

	// only enumerate non-decomposable 2-Edit 
	// note that targetIndex starts from 1
	@Override
	public void enumerate(){
		foundClusterings.clear();	
		ArrayList<ArrayList<Integer>> clusters = initClustering.getClustersInArrayFormat();
		
		// 1st case: pick 2 nodes from the same cluster, and and they join together another cluster C2
		for(int clusterId=1; clusterId<=initClustering.getNbCluster(); clusterId++){
			if(initClustering.clusterSizes[clusterId-1]>1){
				for(int i=1; i<clusters.get(clusterId-1).size(); i++){ // iterate over nodes
					for(int j=0; j<i; j++){ // iterate over nodes
						int nodeId1 = clusters.get(clusterId-1).get(i);
						int nodeId2 = clusters.get(clusterId-1).get(j);

						if(this.adjMat[nodeId1][nodeId2]>0){
							
							ArrayList<Integer> notEqualToValues = new ArrayList<>();
							notEqualToValues.add(clusterId);
							TNode node1 = new TNode(nodeId1, clusterId, 1, notEqualToValues); // here targetIndex=1 means that they will be in the same cluster
							TNode node2 = new TNode(nodeId2, clusterId, 1, notEqualToValues); // here targetIndex=1
							ArrayList<TNode> selNodes = new ArrayList<TNode>();
							selNodes.add(node1);
							selNodes.add(node2);
							
							ArrayList<ArrayList<TNode>> optimalTransformations = findCandidateTransformations(selNodes);
							ArrayList<ArrayList<TNode>> subset = filterByDecomposabe1EditOptimalTransformations(optimalTransformations);
							Set<Clustering> set = enumerateClusterings(subset);
							if(set.size()>0)
								foundClusterings.addAll(set);
						}


					}
				}
			}
		}
		
		
		// **********************************************************************************

		
		// 2nd case: pick 2 nodes from the same cluster, and and they join different clusters
		for(int clusterId=1; clusterId<=initClustering.getNbCluster(); clusterId++){
			if(initClustering.clusterSizes[clusterId-1]>1){
				for(int i=1; i<clusters.get(clusterId-1).size(); i++){ // iterate over nodes
					for(int j=0; j<i; j++){ // iterate over nodes
						int nodeId1 = clusters.get(clusterId-1).get(i);
						int nodeId2 = clusters.get(clusterId-1).get(j);

						if(this.adjMat[nodeId1][nodeId2]>0){
							
							ArrayList<Integer> notEqualToValues = new ArrayList<>();
							notEqualToValues.add(clusterId);
							TNode node1 = new TNode(nodeId1, clusterId, 1, notEqualToValues); // here targetIndex=1
							TNode node2 = new TNode(nodeId2, clusterId, 2, notEqualToValues); // here targetIndex=2
							ArrayList<TNode> selNodes = new ArrayList<TNode>();
							selNodes.add(node1);
							selNodes.add(node2);
							
							ArrayList<ArrayList<TNode>> optimalTransformations = findCandidateTransformations(selNodes);
							ArrayList<ArrayList<TNode>> subset = filterByDecomposabe1EditOptimalTransformations(optimalTransformations);
							Set<Clustering> set = enumerateClusterings(subset);
							if(set.size()>0)
								foundClusterings.addAll(set);
						}


					}
				}
			}
		}
	
		
		// **********************************************************************************
		
		// 3rd case: exchange nodes between 2 clusters
		for(int clusterId1=2; clusterId1<=initClustering.getNbCluster(); clusterId1++){
			for(int clusterId2=1; clusterId2<clusterId1; clusterId2++){
				if(initClustering.clusterSizes[clusterId1-1]!=1 || initClustering.clusterSizes[clusterId2-1]!=1){ // no allowing an exchange between 2 single node clusters
					for(int i=1; i<clusters.get(clusterId1-1).size(); i++){ // iterate over nodes of cluster 1
						for(int j=1; j<clusters.get(clusterId2-1).size(); j++){ // iterate over nodes of cluster 2
							int nodeId1 = clusters.get(clusterId1-1).get(i);
							int nodeId2 = clusters.get(clusterId2-1).get(j);
							
							if(this.adjMat[nodeId1][nodeId2]<0){
								
								TNode node1 = new TNode(nodeId1, clusterId1, -1, clusterId2, new ArrayList<Integer>()); // here targetIndex=-1
								TNode node2 = new TNode(nodeId2, clusterId2, -1, clusterId1, new ArrayList<Integer>()); // here targetIndex=-1
								ArrayList<TNode> selNodes = new ArrayList<TNode>();
								selNodes.add(node1);
								selNodes.add(node2);
								
								ArrayList<ArrayList<TNode>> optimalTransformations = findCandidateTransformations(selNodes);
								ArrayList<ArrayList<TNode>> subset = filterByDecomposabe1EditOptimalTransformations(optimalTransformations);
								Set<Clustering> set = enumerateClusterings(subset);
								if(set.size()>0)
									foundClusterings.addAll(set);
							}
							
						}
					}
				}
			}
		}
		
		
		// **********************************************************************************
		
		// 4th case: pick 2 nodes from different clusters C1 and C2, and they join together another cluster C3
		for(int clusterId1=2; clusterId1<=initClustering.getNbCluster(); clusterId1++){
			for(int clusterId2=1; clusterId2<clusterId1; clusterId2++){

				for(int i=0; i<clusters.get(clusterId1-1).size(); i++){ // iterate over nodes of cluster 1
					for(int j=0; j<clusters.get(clusterId2-1).size(); j++){ // iterate over nodes of cluster 2
						int nodeId1 = clusters.get(clusterId1-1).get(i);
						int nodeId2 = clusters.get(clusterId2-1).get(j);
						
						if(this.adjMat[nodeId1][nodeId2]>0){
							
							ArrayList<Integer> notEqualToValues1 = new ArrayList<>();
							notEqualToValues1.add(clusterId1);
							TNode node1 = new TNode(nodeId1, clusterId1, 1, notEqualToValues1); // here targetIndex=1 means that they will be in the same cluster
							ArrayList<Integer> notEqualToValues2 = new ArrayList<>();
							notEqualToValues2.add(clusterId2);
							TNode node2 = new TNode(nodeId2, clusterId2, 1, notEqualToValues2); // here targetIndex=1
							ArrayList<TNode> selNodes = new ArrayList<TNode>();
							selNodes.add(node1);
							selNodes.add(node2);
							
							ArrayList<ArrayList<TNode>> optimalTransformations = findCandidateTransformations(selNodes);
							ArrayList<ArrayList<TNode>> subset = filterByDecomposabe1EditOptimalTransformations(optimalTransformations);
							Set<Clustering> set = enumerateClusterings(subset);
							if(set.size()>0)
								foundClusterings.addAll(set);
						}
					}
				}
			}
		}
				
				
		// **********************************************************************************
		
		// 5th case: one node joins the cluster of the other node, and the other node join a different cluster. There are 2 asymmetric cases
		for(int clusterId1=2; clusterId1<=initClustering.getNbCluster(); clusterId1++){
			for(int clusterId2=1; clusterId2<clusterId1; clusterId2++){

				for(int i=1; i<clusters.get(clusterId1-1).size(); i++){ // iterate over nodes of cluster 1
					for(int j=1; j<clusters.get(clusterId2-1).size(); j++){ // iterate over nodes of cluster 2
						int nodeId1 = clusters.get(clusterId1-1).get(i);
						int nodeId2 = clusters.get(clusterId2-1).get(j);

						if(this.adjMat[nodeId1][nodeId2]<0){ // only negative edge
							
							// subcase1: node1 joins clusterId2, and node2 joins another cluster
							TNode node1 = new TNode(nodeId1, clusterId1, -1, clusterId2, new ArrayList<Integer>()); // here targetIndex=-1
							ArrayList<Integer> notEqualToValues2 = new ArrayList<>();
							notEqualToValues2.add(clusterId1);
							notEqualToValues2.add(clusterId2);
							TNode node2 = new TNode(nodeId2, clusterId2, 1, notEqualToValues2); // here targetIndex=1
							ArrayList<TNode> selNodes = new ArrayList<TNode>();
							selNodes.add(node1);
							selNodes.add(node2);
							
							ArrayList<ArrayList<TNode>> optimalTransformations = findCandidateTransformations(selNodes);
							ArrayList<ArrayList<TNode>> subset = filterByDecomposabe1EditOptimalTransformations(optimalTransformations);
							Set<Clustering> set = enumerateClusterings(subset);
							if(set.size()>0)
								foundClusterings.addAll(set);
							
							
							// subcase2: node2 joins clusterId1, and node1 joins another cluster
							set.clear();
							subset.clear();
							optimalTransformations.clear();
							notEqualToValues2.clear();
							selNodes.clear();
							ArrayList<Integer> notEqualToValues1 = new ArrayList<>();
							notEqualToValues1.add(clusterId1);
							notEqualToValues1.add(clusterId2);
							node1 = new TNode(nodeId1, clusterId1, 1, notEqualToValues1); // here targetIndex=-1
							node2 = new TNode(nodeId2, clusterId2, -1, clusterId1, new ArrayList<Integer>()); // here targetIndex=1
							selNodes.add(node1);
							selNodes.add(node2);
							
							optimalTransformations = findCandidateTransformations(selNodes);
							subset = filterByDecomposabe1EditOptimalTransformations(optimalTransformations);
							set = enumerateClusterings(subset);
							if(set.size()>0)
								foundClusterings.addAll(set);
						}
					}
				}
			}
		}
		
				
	}
	
	
}
