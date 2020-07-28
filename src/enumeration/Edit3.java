package enumeration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import myUtils.Clustering;
import myUtils.Combination;



public class Edit3 extends AbstractEdit {

	
	public Edit3(double[][] adjMat_, Clustering initClustering_){
		super(adjMat_,initClustering_);
	}
	

	// only enumerate non-decomposable 3-Edit 
	// note that targetIndex starts from 1
	@Override
	public void enumerate(){ // =====> TODO boyle biraz sacma geliyo yapmak: otomatik oalrak yapmak varken ..²
		foundClusterings.clear();	
		//ArrayList<ArrayList<Integer>> clusters = initClustering.getClustersInArrayFormat();
		
		ArrayList<Integer> targetIndexes;
		ArrayList<Integer> targetClusterIds;
		
//		// 1st case: pick 3 nodes from the same cluster, and and they join together another cluster C2
//		targetIndexes = new ArrayList<>(Arrays.asList(1,1,1));
//		enumerateFromTheSameCluster(targetIndexes);
//		
//		// 2nd case: pick 3 nodes from the same cluster, and and they join different clusters
//		targetIndexes = new ArrayList<>(Arrays.asList(1,1,2));
//		enumerateFromTheSameCluster(targetIndexes);
//				
//		targetIndexes = new ArrayList<>(Arrays.asList(1,2,1));
//		enumerateFromTheSameCluster(targetIndexes);
//				
//		targetIndexes = new ArrayList<>(Arrays.asList(1,2,2));
//		enumerateFromTheSameCluster(targetIndexes);
//		
//		// **********************************************************************************
//		// pick 3 nodes from different clusters
//		// 3.11
//		targetClusterIds = new ArrayList<>(Arrays.asList(2,3,1));
//		targetIndexes = new ArrayList<>(Arrays.asList(-1,-1,-1));
//		enumerateFromDifferentClusters(targetClusterIds, targetIndexes);
//		// ---
//		targetClusterIds = new ArrayList<>(Arrays.asList(3,1,2));
//		targetIndexes = new ArrayList<>(Arrays.asList(-1,-1,-1));
//		enumerateFromDifferentClusters(targetClusterIds, targetIndexes);
//
//		// 3.12
//		targetClusterIds = new ArrayList<>(Arrays.asList(2,3,-1));
//		targetIndexes = new ArrayList<>(Arrays.asList(-1,-1,1));
//		enumerateFromDifferentClusters(targetClusterIds, targetIndexes);
//		targetClusterIds = new ArrayList<>(Arrays.asList(3,2,-1));
//		targetIndexes = new ArrayList<>(Arrays.asList(-1,-1,1));
//		enumerateFromDifferentClusters(targetClusterIds, targetIndexes);
//		// ---
//		targetClusterIds = new ArrayList<>(Arrays.asList(2,-1,3));
//		targetIndexes = new ArrayList<>(Arrays.asList(-1,1,-1));
//		enumerateFromDifferentClusters(targetClusterIds, targetIndexes);
//		targetClusterIds = new ArrayList<>(Arrays.asList(3,-1,2));
//		targetIndexes = new ArrayList<>(Arrays.asList(-1,1,-1));
//		enumerateFromDifferentClusters(targetClusterIds, targetIndexes);
//		// ---
//		targetClusterIds = new ArrayList<>(Arrays.asList(-1,2,3));
//		targetIndexes = new ArrayList<>(Arrays.asList(1,-1,-1));
//		enumerateFromDifferentClusters(targetClusterIds, targetIndexes);
//		targetClusterIds = new ArrayList<>(Arrays.asList(-1,3,2));
//		targetIndexes = new ArrayList<>(Arrays.asList(1,-1,-1));
//		enumerateFromDifferentClusters(targetClusterIds, targetIndexes);
//		
//		// 3.13
//		targetClusterIds = new ArrayList<>(Arrays.asList(3,3,1));
//		targetIndexes = new ArrayList<>(Arrays.asList(-1,-1,-1));
//		enumerateFromDifferentClusters(targetClusterIds, targetIndexes);
//		targetClusterIds = new ArrayList<>(Arrays.asList(3,3,2));
//		targetIndexes = new ArrayList<>(Arrays.asList(-1,-1,-1));
//		enumerateFromDifferentClusters(targetClusterIds, targetIndexes);
//		// ---
//		targetClusterIds = new ArrayList<>(Arrays.asList(2,3,2));
//		targetIndexes = new ArrayList<>(Arrays.asList(-1,-1,-1));
//		enumerateFromDifferentClusters(targetClusterIds, targetIndexes);
//		targetClusterIds = new ArrayList<>(Arrays.asList(2,1,2));
//		targetIndexes = new ArrayList<>(Arrays.asList(-1,-1,-1));
//		enumerateFromDifferentClusters(targetClusterIds, targetIndexes);
//		// ---
//		targetClusterIds = new ArrayList<>(Arrays.asList(2,1,1));
//		targetIndexes = new ArrayList<>(Arrays.asList(-1,-1,-1));
//		enumerateFromDifferentClusters(targetClusterIds, targetIndexes);
//		targetClusterIds = new ArrayList<>(Arrays.asList(3,1,1));
//		targetIndexes = new ArrayList<>(Arrays.asList(-1,-1,-1));
//		enumerateFromDifferentClusters(targetClusterIds, targetIndexes);
//		
//		// 3.14
//		targetClusterIds = new ArrayList<>(Arrays.asList(-1,-1,-1));
//		targetIndexes = new ArrayList<>(Arrays.asList(1,1,1));
//		enumerateFromDifferentClusters(targetClusterIds, targetIndexes);

		// **********************************************************************************
		
		
		//3.4
			// node 1 and node 2 are in the same cluster, and they move into the cluster of node3. Likewise, node3 moves into the cluster of node1 (and node2).
		targetClusterIds = new ArrayList<>(Arrays.asList(3,3,1));
		targetIndexes = new ArrayList<>(Arrays.asList(-1,-1,-1));
		enumerateFrom1Different2SameClusters(targetClusterIds, targetIndexes);
		// ---
			// node 1 and node 3 are in the same cluster
		targetClusterIds = new ArrayList<>(Arrays.asList(2,1,2));
		targetIndexes = new ArrayList<>(Arrays.asList(-1,-1,-1));
		enumerateFrom1Different2SameClusters(targetClusterIds, targetIndexes);
		// ---
		// node 2 and node 3 are in the same cluster
		targetClusterIds = new ArrayList<>(Arrays.asList(2,1,1));
		targetIndexes = new ArrayList<>(Arrays.asList(-1,-1,-1));
		enumerateFrom1Different2SameClusters(targetClusterIds, targetIndexes);

		
		// **********************************************************************************
		

				
				
		
		
				
	}
	
	
	
	public void enumerateFromTheSameCluster(ArrayList<Integer> targetIndexes){
		ArrayList<ArrayList<Integer>> clusters = initClustering.getClustersInArrayFormat();

		for(int clusterId=1; clusterId<=initClustering.getNbCluster(); clusterId++){
			if(initClustering.clusterSizes[clusterId-1]>2){
				for(int i=2; i<clusters.get(clusterId-1).size(); i++){ // iterate over nodes
					int nodeId1 = clusters.get(clusterId-1).get(i);
					for(int j=1; j<i; j++){ // iterate over nodes
						int nodeId2 = clusters.get(clusterId-1).get(j);
						
						if(this.adjMat[nodeId1][nodeId2]!=0){// if there is a link
							for(int k=0; k<j; k++){ // iterate over nodes
								
								int nodeId3 = clusters.get(clusterId-1).get(k);
	
								double w1_first = this.adjMat[nodeId1][nodeId2]+this.adjMat[nodeId1][nodeId3];
								double w2_first = this.adjMat[nodeId1][nodeId2]+this.adjMat[nodeId2][nodeId3];
								double w3_first = this.adjMat[nodeId2][nodeId3]+this.adjMat[nodeId1][nodeId3];
								double w1_last = 0;
								if(targetIndexes.get(0) == targetIndexes.get(1))
									w1_last += this.adjMat[nodeId1][nodeId2];
								if(targetIndexes.get(0) == targetIndexes.get(2))
									w1_last += this.adjMat[nodeId1][nodeId3];
								double w2_last = 0;
								if(targetIndexes.get(0) == targetIndexes.get(1))
									w2_last += this.adjMat[nodeId1][nodeId2];
								if(targetIndexes.get(1) == targetIndexes.get(2))
									w2_last += this.adjMat[nodeId1][nodeId3];
								double w3_last = 0;
								if(targetIndexes.get(0) == targetIndexes.get(2))
									w3_last += this.adjMat[nodeId1][nodeId2];
								if(targetIndexes.get(1) == targetIndexes.get(2))
									w3_last += this.adjMat[nodeId1][nodeId3];
								
								if((w1_first+w1_last)>0 && (w2_first+w2_last)>0 && (w3_first+w3_last)>0){
									
									ArrayList<Integer> notEqualToValues = new ArrayList<>();
									notEqualToValues.add(clusterId);
									TNode node1 = new TNode(nodeId1, clusterId, targetIndexes.get(0), notEqualToValues); // here targetIndex=1 means that they will be in the same cluster
									TNode node2 = new TNode(nodeId2, clusterId, targetIndexes.get(1), notEqualToValues); // here targetIndex=1
									TNode node3 = new TNode(nodeId3, clusterId, targetIndexes.get(2), notEqualToValues); // here targetIndex=1
									ArrayList<TNode> selNodes = new ArrayList<TNode>();
									selNodes.add(node1);
									selNodes.add(node2);
									selNodes.add(node3);
	
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
		}
	}
	
	
	
	
	public void enumerateFromDifferentClusters(ArrayList<Integer> targetClusterIds, ArrayList<Integer> targetIndexes){
		ArrayList<ArrayList<Integer>> clusters = initClustering.getClustersInArrayFormat();

		for(int clusterId1=3; clusterId1<=initClustering.getNbCluster(); clusterId1++){
			for(int clusterId2=2; clusterId2<clusterId1; clusterId2++){
				for(int clusterId3=1; clusterId3<clusterId2; clusterId3++){
					
						for(int i=0; i<clusters.get(clusterId1-1).size(); i++){ // iterate over nodes
							int nodeId1 = clusters.get(clusterId1-1).get(i);
							
							for(int j=0; j<clusters.get(clusterId2-1).size(); j++){ // iterate over nodes
								int nodeId2 = clusters.get(clusterId2-1).get(j);
								
								if(this.adjMat[nodeId1][nodeId2]!=0){// if there is a link
									for(int k=0; k<clusters.get(clusterId3-1).size(); k++){ // iterate over nodes
										int nodeId3 = clusters.get(clusterId3-1).get(k);
			
										double w1_first = 0;
										// if node1 will be in the cluster of node2 and node2 moves into another cluster
										if(targetClusterIds.get(0) == clusterId2)
											w1_first += -this.adjMat[nodeId1][nodeId2];
										if(targetClusterIds.get(0) == clusterId3)
											w1_first += -this.adjMat[nodeId1][nodeId3];
										
										double w2_first = 0;
										if(targetClusterIds.get(1) == clusterId1)
											w2_first += -this.adjMat[nodeId1][nodeId2];
										if(targetClusterIds.get(1) == clusterId3)
											w2_first += -this.adjMat[nodeId2][nodeId3];
										
										double w3_first = 0;
										if(targetClusterIds.get(2) == clusterId1)
											w3_first += -this.adjMat[nodeId1][nodeId3];
										if(targetClusterIds.get(2) == clusterId2)
											w3_first += -this.adjMat[nodeId2][nodeId3];
										
										double w1_last = 0;
										// if node1 and node2 will be in the same cluster
										if(targetIndexes.get(0) == targetIndexes.get(1) && targetClusterIds.get(0) == targetClusterIds.get(1))
											w1_last += this.adjMat[nodeId1][nodeId2];
										// if node2 will be in the cluster of node1 (and node1 moves into another cluster)
										else if(clusterId1 == targetClusterIds.get(1))
											w1_last += -this.adjMat[nodeId1][nodeId2];
										if(targetIndexes.get(0) == targetIndexes.get(2) && targetClusterIds.get(0) == targetClusterIds.get(2))
											w1_last += this.adjMat[nodeId1][nodeId3];
										else if(clusterId1 == targetClusterIds.get(2))
											w1_last += -this.adjMat[nodeId1][nodeId3];
										
										
										double w2_last = 0;
										if(targetIndexes.get(1) == targetIndexes.get(0) && targetClusterIds.get(1) == targetClusterIds.get(0))
											w2_last += this.adjMat[nodeId1][nodeId2];
										else if(clusterId2 == targetClusterIds.get(0))
											w2_last += -this.adjMat[nodeId1][nodeId2];
										if(targetIndexes.get(1) == targetIndexes.get(2) && targetClusterIds.get(1) == targetClusterIds.get(2))
											w2_last += this.adjMat[nodeId2][nodeId3];
										else if(clusterId2 == targetClusterIds.get(2))
											w2_last += -this.adjMat[nodeId2][nodeId3];
										
										
										double w3_last = 0;
										if(targetIndexes.get(2) == targetIndexes.get(0) && targetClusterIds.get(2) == targetClusterIds.get(0))
											w3_last += this.adjMat[nodeId1][nodeId3];
										else if(clusterId3 == targetClusterIds.get(0))
											w3_last += -this.adjMat[nodeId1][nodeId3];
										if(targetIndexes.get(2) == targetIndexes.get(1) && targetClusterIds.get(2) == targetClusterIds.get(1))
											w3_last += this.adjMat[nodeId2][nodeId3];
										else if(clusterId3 == targetClusterIds.get(1))
											w3_last += -this.adjMat[nodeId2][nodeId3];
										
										if((w1_first+w1_last)>0 && (w2_first+w2_last)>0 && (w3_first+w3_last)>0){
											
											ArrayList<Integer> notEqualToValues1 = new ArrayList<>();
											notEqualToValues1.add(clusterId1);
											TNode node1 = new TNode(nodeId1, clusterId1, targetIndexes.get(0), targetClusterIds.get(0),
													notEqualToValues1); // here targetIndex=1 means that they will be in the same cluster
											
											ArrayList<Integer> notEqualToValues2 = new ArrayList<>();
											notEqualToValues2.add(clusterId2);
											TNode node2 = new TNode(nodeId2, clusterId2, targetIndexes.get(1), targetClusterIds.get(1),
													notEqualToValues2); // here targetIndex=1
											
											ArrayList<Integer> notEqualToValues3 = new ArrayList<>();
											notEqualToValues3.add(clusterId3);
											TNode node3 = new TNode(nodeId3, clusterId3, targetIndexes.get(2), targetClusterIds.get(2),
													notEqualToValues3); // here targetIndex=1
											
											ArrayList<TNode> selNodes = new ArrayList<TNode>();
											selNodes.add(node1);
											selNodes.add(node2);
											selNodes.add(node3);
			
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
			}
		}
	}
	
	
	
	
	
	// we suppose that first 2 nodes in the same cluster, the other node is in diff cluster
	public void enumerateFrom1Different2SameClusters(ArrayList<Integer> targetClusterIndexes, ArrayList<Integer> targetIndexes){
		ArrayList<ArrayList<Integer>> clusters = initClustering.getClustersInArrayFormat();
		int nbEdit=3;
		int nbSourceCluster=2;
		int[] partitionSizes = new int[nbSourceCluster];
		partitionSizes[0]=2;
		partitionSizes[1]=1;
		
		List<Integer> allClusterIds = new ArrayList<Integer>();
		for(int i=1; i<=initClustering.getNbCluster(); i++)
			allClusterIds.add(i);
		List<int[]> combClusterIds = Combination.generate(allClusterIds, 2);
		
		for(int[] cIds : combClusterIds){
			int[] clusterIds = new int[3];
			int counter=0;
			for(int i=0; i<nbSourceCluster; i++)
				for(int j=0; j<partitionSizes[i]; j++)
					clusterIds[counter++]=cIds[i];
					
			int[] targetClusterIds = new int[3];
			for(int i=0; i<nbEdit; i++)
				targetClusterIds[i]=clusterIds[targetClusterIndexes.get(i)-1];
	

			
			if(initClustering.clusterSizes[clusterIds[0]-1]>1){
		
				for(int i=1; i<clusters.get(clusterIds[0]-1).size(); i++){ // iterate over nodes
					for(int j=0; j<i; j++){ // iterate over nodes
						int nodeId1 = clusters.get(clusterIds[0]-1).get(i);
						int nodeId2 = clusters.get(clusterIds[1]-1).get(j);
						
						if(clusterIds[0]==3 && clusterIds[2]==4 && nodeId1==18 && nodeId2==14)
							System.out.println("a");
						
						if(this.adjMat[nodeId1][nodeId2]!=0){// if there is a link
							for(int k=0; k<clusters.get(clusterIds[2]-1).size(); k++){ // iterate over nodes
								int nodeId3 = clusters.get(clusterIds[2]-1).get(k);
	
								double w1_first = this.adjMat[nodeId1][nodeId2];
								// if node1 will be in the cluster of node3 and node3 moves into another cluster
								if(targetClusterIds[0] == clusterIds[2])
									w1_first += -this.adjMat[nodeId1][nodeId3];
								double w2_first = 0;
								if(targetClusterIds[1] == clusterIds[2])
									w2_first += -this.adjMat[nodeId2][nodeId3];
								double w3_first = 0;
								if(targetClusterIds[2] == clusterIds[0]){
									w3_first += -this.adjMat[nodeId1][nodeId3];
									w3_first += -this.adjMat[nodeId2][nodeId3];
								}
								
								
								double w1_last = 0;
								// if node1 and node2 will be in the same cluster
								if(targetIndexes.get(0) == targetIndexes.get(1) && clusterIds[0] == clusterIds[1])
									w1_last += this.adjMat[nodeId1][nodeId2];
								// if node2 will be in the cluster of node1 (and node1 moves into another cluster)
								else if(clusterIds[0] == targetClusterIds[1])
									w1_last += -this.adjMat[nodeId1][nodeId2];
								// if node1 and node3 will be in the same cluster
								if(targetIndexes.get(0) == targetIndexes.get(1) && clusterIds[0] == clusterIds[2])
									w1_last += this.adjMat[nodeId1][nodeId3];
								// if node3 will be in the cluster of node1 (and node1 moves into another cluster)
								else if(clusterIds[0] == targetClusterIds[2])
									w1_last += -this.adjMat[nodeId1][nodeId3];

								double w2_last = 0;
								// if node2 and node1 will be in the same cluster
								if(targetIndexes.get(1) == targetIndexes.get(0) && targetClusterIds[1] == targetClusterIds[0])
									w2_last += this.adjMat[nodeId1][nodeId2];
								// if node1 will be in the cluster of node2 (and node2 moves into another cluster)
								else if(clusterIds[1] == targetClusterIds[0])
									w2_last += -this.adjMat[nodeId1][nodeId2];
								if(targetIndexes.get(1) == targetIndexes.get(2) && targetClusterIds[1] == targetClusterIds[2])
									w2_last += this.adjMat[nodeId2][nodeId3];
								else if(clusterIds[1] == targetClusterIds[2])
									w2_last += -this.adjMat[nodeId2][nodeId3];
								
								double w3_last = 0;
								// if node3 and node1 will be in the same cluster
								if(targetIndexes.get(2) == targetIndexes.get(0) && targetClusterIds[2] == targetClusterIds[0])
									w3_last += this.adjMat[nodeId1][nodeId3];
								// if node1 will be in the cluster of node3 (and node3 moves into another cluster)
								else if(clusterIds[2] == targetClusterIds[0])
									w3_last += -this.adjMat[nodeId1][nodeId3];
								if(targetIndexes.get(2) == targetIndexes.get(1) && targetClusterIds[2] == targetClusterIds[1])
									w3_last += this.adjMat[nodeId2][nodeId3];
								else if(clusterIds[2] == targetClusterIds[1])
									w3_last += -this.adjMat[nodeId2][nodeId3];
								
								if((w1_first+w1_last)>=0 && (w2_first+w2_last)>=0 && (w3_first+w3_last)>=0){
									
									ArrayList<Integer> notEqualToValues1 = new ArrayList<>();
									notEqualToValues1.add(clusterIds[0]);
									TNode node1 = new TNode(nodeId1, clusterIds[0], targetIndexes.get(0), targetClusterIds[0],
											notEqualToValues1); // here targetIndex=1 means that they will be in the same cluster
									
									ArrayList<Integer> notEqualToValues2 = new ArrayList<>();
									notEqualToValues2.add(clusterIds[1]);
									TNode node2 = new TNode(nodeId2, clusterIds[1], targetIndexes.get(1), targetClusterIds[1],
											notEqualToValues2); // here targetIndex=1
									
									ArrayList<Integer> notEqualToValues3 = new ArrayList<>();
									notEqualToValues3.add(clusterIds[2]);
									TNode node3 = new TNode(nodeId3, clusterIds[2], targetIndexes.get(2), targetClusterIds[2],
											notEqualToValues3); // here targetIndex=1
									
									ArrayList<TNode> selNodes = new ArrayList<TNode>();
									selNodes.add(node1);
									selNodes.add(node2);
									selNodes.add(node3);
	
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
		}
	}
	
	
}
