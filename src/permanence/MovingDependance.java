package permanence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import myUtils.ArrayOperations;
import myUtils.Clustering;
import myUtils.DirectedGraph;
import myUtils.MyCGraph;

public class MovingDependance {

	MyCGraph g;
	//long idCounter;
	double[][] adjMat;
	Clustering initClustering;
	int nbEdit;
	int subsetMaxSize;
	int maxNbDistinctDeltaFitnessForPossibleTargetClusters; // not including the new empty cluster
	public boolean[] isPermanentNode;
	
    public LinkedList<Integer>[] unlikelyTargetClusterIdListArray; 
    public LinkedList<Integer>[] possibleTargetClusterIdListArray; 
    LinkedList<Integer>[] possibleIncomingNodeIdListArray; 

    
 // Remark: This class is intended to make the execution of 4-Edit and 5-Edit faster. So, starting from 6-Edit is out of scope.
 	public MovingDependance(MyCGraph g_, double[][] adjMat_, Clustering initClustering_,int nbEdit_, int levelNo)
 	{
 		this(g_, adjMat_, initClustering_, nbEdit_, levelNo,
 				12, // subsetMaxSize
 				3 // maxNbDistinctDeltaFitnessForPossibleTargetClusters
		);
 	}
 	
 	
	// Remark: This class is intended to make the execution of 4-Edit and 5-Edit faster. So, starting from 6-Edit is out of scope.
	public MovingDependance(MyCGraph g_, double[][] adjMat_, Clustering initClustering_,int nbEdit_, int levelNo,
			int subsetMaxSize_, int maxNbDistinctDeltaFitnessForPossibleTargetClusters_)
	{
		g = g_;
		adjMat = adjMat_;
		initClustering = initClustering_;
		nbEdit = nbEdit_;
		subsetMaxSize = subsetMaxSize_;
		maxNbDistinctDeltaFitnessForPossibleTargetClusters = maxNbDistinctDeltaFitnessForPossibleTargetClusters_;
		isPermanentNode = new boolean[g.n];
		for(int i=0; i<g.n; i++)
			isPermanentNode[i] = false; // init
		
		
		unlikelyTargetClusterIdListArray = new LinkedList[g.n]; 
        for(int i = 0; i < g.n ; i++){ 
        	unlikelyTargetClusterIdListArray[i] = new LinkedList<Integer>();
        	for(int j=1; j<=initClustering.getNbCluster(); j++)
        		unlikelyTargetClusterIdListArray[i].add(j);
        }
        
		possibleTargetClusterIdListArray = new LinkedList[g.n]; 
        for(int i = 0; i < g.n ; i++){ 
        	possibleTargetClusterIdListArray[i] = new LinkedList<Integer>(); 
        }
        
        possibleIncomingNodeIdListArray = new LinkedList[initClustering.getNbCluster()+1]; // +1 for the empty cluster 
        for(int i=1 ; i <= (initClustering.getNbCluster()+1) ; i++){ 
        	possibleIncomingNodeIdListArray[i-1] = new LinkedList<Integer>(); 
        } 
        
        determinePermanentNodes(levelNo);
	}
	
	
	
	// we limit the number of possible target clusters up to 3 clusters (including the new empty cluster) for a faster execution time
	public LinkedList<Integer> getPossibleTargetClusterIds(int nodeId, int clusterId, int nbEdit, int levelNo){
		LinkedList<Integer> possibleTargetClusterIds = new LinkedList<Integer>();
		int UP = (nbEdit-levelNo); // upper bound

		Map<Double, ArrayList<Integer>> map = new TreeMap<>();
		
		double sourceFitness = g.weightSumInClusters[nodeId][clusterId-1];
		for(int targetClusterId=1; targetClusterId<=(initClustering.getNbCluster()); targetClusterId++){
			if(targetClusterId != clusterId){
				double targetFitness = g.weightSumInClusters[nodeId][targetClusterId-1];
				double diff = sourceFitness-targetFitness;
				
				if(targetFitness>=-1.0 && diff <= UP){
					if(!map.keySet().contains(diff))
						map.put(diff, new ArrayList<>());
					map.get(diff).add(targetClusterId);
					//possibleTargetClusterIds.add(targetClusterId);
				}
			}
		}
		
		int counter = 1;
		for(Double key : map.keySet()){
			if(counter>maxNbDistinctDeltaFitnessForPossibleTargetClusters)
				break;
			if(key!=0.0)
				possibleTargetClusterIds.addAll(map.get(key));
			counter++;
		}
		
		// last step: if node can move into a new cluster, add this possibility as well
		int targetClusterId = initClustering.getNbCluster()+1;
		//if(sourceFitness < (nbEdit-1)) // since targetFitness=0
		if(sourceFitness <= UP)
			possibleTargetClusterIds.add(targetClusterId);
		
		return(possibleTargetClusterIds);
	}
	
	
	public ArrayList<Integer> getNodeIdsWithNegativeLink(int nodeId, ArrayList<Integer> nodeIdsInTargetCluster){
		ArrayList<Integer> negNodeIds = new ArrayList<Integer>();
		for(Integer otherNodeId : nodeIdsInTargetCluster){
			if(adjMat[nodeId][otherNodeId]<0.0)
				negNodeIds.add(otherNodeId);
		}
		return(negNodeIds);
	}
	
	
	
	
	public void determinePermanentNodes(int levelNo){
		ArrayList<ArrayList<Integer>> initClusters = initClustering.getClustersInArrayFormat();
		
		for(int clusterId=1; clusterId<=initClusters.size(); clusterId++){
			ArrayList<Integer> cluster = initClusters.get(clusterId-1);
			int clusterSize = cluster.size();
			if(clusterSize>1){
				for(Integer nodeId : cluster){
					LinkedList<Integer> possibleTargetClusterIds = getPossibleTargetClusterIds(nodeId, clusterId, nbEdit, levelNo);
					if(possibleTargetClusterIds.size()==0)
						isPermanentNode[nodeId] = true;
					else {
						possibleTargetClusterIdListArray[nodeId].addAll(possibleTargetClusterIds); // outgoing
						unlikelyTargetClusterIdListArray[nodeId].removeAll(possibleTargetClusterIds);
						
						for(int tid : possibleTargetClusterIds){
							possibleIncomingNodeIdListArray[tid-1].add(nodeId);
						}
					}
				}
			}
		}
		
	}
	
	
	public DirectedGraph buildMovingDependenceGraph(){
		DirectedGraph diG = new DirectedGraph(g.n);
		
		ArrayList<ArrayList<Integer>> initClusters = initClustering.getClustersInArrayFormat();
		
		for(int clusterId=1; clusterId<=initClusters.size(); clusterId++){
			ArrayList<Integer> cluster = initClusters.get(clusterId-1);
			int clusterSize = cluster.size();
			if(clusterSize>1){
				for(Integer nodeId : cluster){
					// intra cluster
//					double posSum = g.posSumInClusters[nodeId][clusterId-1];
//					double weightSum = g.weightSumInClusters[nodeId][clusterId-1];
//					double absWeightSum = g.absWeightSumInClusters[nodeId][clusterId-1];
					
					// inter cluster
					if(!isPermanentNode[nodeId]) {
						ArrayList<Integer> targetClusterIdsToRemove = new ArrayList<Integer>();
						for(int targetClusterId : possibleTargetClusterIdListArray[nodeId]){
							if(targetClusterId != initClustering.getNbCluster()+1){ // except the new empty cluster
								ArrayList<Integer> targetCluster = initClusters.get(targetClusterId-1);
								double targetFitness = g.weightSumInClusters[nodeId][targetClusterId-1];
								ArrayList<Integer> negNodeIds = getNodeIdsWithNegativeLink(nodeId, targetCluster);
								boolean ok = true;

								
								if(targetFitness<=0.0){ // TODO I am not sure if this step is really beneficial
									int posCounter = 0;
									for(int v : possibleIncomingNodeIdListArray[targetClusterId-1]){
										if(adjMat[nodeId][v]>0.0)
											posCounter++;
									}
									int negCounter = negNodeIds.size();
									if((targetFitness+posCounter+negCounter)<=0.0) {
										ok = false;
										targetClusterIdsToRemove.add(targetClusterId);
									}
								}
								
								if(ok && negNodeIds.size()>0){ // target fitness should be positive
									for(int otherNodeId : negNodeIds)
										if(!isPermanentNode[otherNodeId])
											diG.addEdge(nodeId, otherNodeId);
								}
							}
						}
						// remove target cluster ids, if they exist
						for(Integer tid : targetClusterIdsToRemove){
							possibleTargetClusterIdListArray[nodeId].remove(tid);
							possibleIncomingNodeIdListArray[tid-1].remove(nodeId);
						}
					}
					
				}
			}
		}
		
//		diG.displayGraph();
//		for(int clusterId=1; clusterId<=initClusters.size(); clusterId++){
//			System.out.println("cluster id: " + clusterId + " => " + possibleIncomingNodeIdListArray[clusterId-1] + "of size " + possibleIncomingNodeIdListArray[clusterId-1].size());
//		}
		
		return(diG);
	}
	
	
	
//	// direction: -1 for in neighbors, and +1 for out neighbors
//	// 'output' and 'additionals' should be initialiazed before this method
//	// note that the 'nodeId' is not inserted into the list 
//	public void getNeighborsRec(DirectedGraph diG, int direction, int nodeId, int nbEdit, int currLevelNo, int targetLevelNo, int subsetMaxSize,
//			ArrayList<Integer> output, ArrayList<ArrayList<Integer>> additionals)
//	{
//		LinkedList<Integer> I = diG.getInNeighbors(nodeId);
//		if(direction == +1)
//			I = diG.getOutNeighbors(nodeId);
//		
//		if(I.size()==0 || (targetLevelNo-currLevelNo)==0)
//			return;
//		else if(currLevelNo == (targetLevelNo-1)){
//			output.add(nodeId);
//			
//			if(I.size()>=nbEdit && (I.size()+output.size()<=subsetMaxSize)){
//				ArrayList<Integer> subset = new ArrayList<Integer>(output);
//				subset.addAll(I);
//				additionals.add(subset);
//			} else {
//				output.addAll(I);
//			}
//		}
//		else {
//			// output.add(nodeId);
//
//			for(int v : I){
//				output.add(v);
//				getNeighborsRec(diG, direction, v, nbEdit, currLevelNo+1, targetLevelNo, subsetMaxSize, output, additionals);
//			}
//		}
//	}
//	
//	
//	
//	
//	public ArrayList<ArrayList<Integer>> DetectPeripheralNodes(DirectedGraph diG, int levelNo){
//		ArrayList<ArrayList<Integer>> initClusters = initClustering.getClustersInArrayFormat();
//		
//		if(levelNo == 0){
//			
//		}
//	
//		for(int clusterId=1; clusterId<=initClusters.size(); clusterId++){
//			ArrayList<Integer> cluster = initClusters.get(clusterId-1);
//			int clusterSize = cluster.size();
//			
//			if(clusterSize>2){
//				
//				ArrayList<Integer> commonSubset = new ArrayList<Integer>();
//				for(Integer nodeId : cluster)
//					if(!isPermanentNode[nodeId])
//						commonSubset.add(nodeId);
//				
//				
//				for(Integer nodeId : cluster) // levelNo == 0
//					if(!isPermanentNode[nodeId]){
//						
//						if ((levelNo % 2) == 0) {
//							int lvl = levelNo/2;
//							// DOWN
//							ArrayList<ArrayList<Integer>> subsetList = _DetectPeripheralNodes(diG, lvl, -1, nodeId, commonSubset);
//							
//							// UP
//							ArrayList<ArrayList<Integer>> subsetList2 = _DetectPeripheralNodes(diG, lvl, +1, nodeId, commonSubset);
//						}
//						else
//						{
//							
//						}
//						// DOWN
//						ArrayList<ArrayList<Integer>> subsetList = _DetectPeripheralNodes(diG, levelNo, -1, nodeId, commonSubset);
//						
//						// UP
//						ArrayList<ArrayList<Integer>> subsetList2 = _DetectPeripheralNodes(diG, levelNo, +1, nodeId, commonSubset);
//				
//					}
//				}
//				
//			}
//			
//		}
//		
//	}
//	
//	
//	// direction: -1 for DOWN, 1 for UP
//	public ArrayList<ArrayList<Integer>> _DetectPeripheralNodes(DirectedGraph diG, int levelNo, int direction, int nodeId, 
//			ArrayList<Integer> commonSubset)
//	{
//		ArrayList<ArrayList<Integer>> subsetList = new ArrayList<ArrayList<Integer>>();
//		ArrayList<Integer> subset = new ArrayList<Integer>(commonSubset);
//
//		getNeighborsRec(diG, direction, nodeId, this.nbEdit, 0, levelNo, this.subsetMaxSize,
//				subset, subsetList);
//		if(subset.size() > commonSubset.size())
//			subsetList.add(subset);
//				
//		return(subsetList);
//	}


		
	
	public ArrayList<ArrayList<Integer>> prepareResult(DirectedGraph diG, int levelNo){
		
		ArrayList<ArrayList<Integer>> subsetList = new ArrayList<ArrayList<Integer>>();
		ArrayList<ArrayList<Integer>> initClusters = initClustering.getClustersInArrayFormat();

//		Permanence p = new Permanence(adjMat, g);
//		double[] signedCluCoefs = p.computeSignedClusteringCoefs();
		
		for(int clusterId=1; clusterId<=initClusters.size(); clusterId++){
			ArrayList<Integer> cluster = initClusters.get(clusterId-1);
			int clusterSize = cluster.size();
			ArrayList<Integer> commonSubset = new ArrayList<Integer>();
			
//			ArrayList<Double> vals = new ArrayList<>();
//			for(Integer nodeId : cluster)
//				vals.add(signedCluCoefs[nodeId]);
//			System.out.println("clusterId: " + clusterId + " => cluster: [" + cluster.toString() + "] => signedCluCoefs: " + vals.toString());
			
			
			for(Integer nodeId : cluster) // levelNo == 0
				if(!isPermanentNode[nodeId])
					commonSubset.add(nodeId);
			
			
			if(levelNo==0 && clusterSize>=nbEdit && clusterSize<=this.subsetMaxSize){
				subsetList.add(cluster);
			}
			
			if(clusterSize>2 && levelNo==1){
				
				// ===============================
				// LEVEL 1
				// ===============================
				ArrayList<Integer> subset1 = new ArrayList<>(commonSubset);
				ArrayList<Integer> subset2 = new ArrayList<>(commonSubset);
				
				for(Integer nodeId : cluster){
					if(!isPermanentNode[nodeId]){
						// level 1 - 1
						if(diG.getInNeighbors(nodeId).size()>=nbEdit){
							ArrayList<Integer> subset3 = new ArrayList<>(commonSubset);
							subset3.addAll(diG.getInNeighbors(nodeId));
							if(subset3.size()<=this.subsetMaxSize)
								subsetList.add(subset3);
						} 
						else {
							for(int v : diG.getInNeighbors(nodeId)) // by definition, incoming nodes have negative link with the node
								if(!subset1.contains(v))
									subset1.add(v);
						}
						
						// level 1 - 2
						if(diG.getOutNeighbors(nodeId).size()>=nbEdit){
							ArrayList<Integer> subset3 = new ArrayList<>(commonSubset);
							subset3.addAll(diG.getOutNeighbors(nodeId));
							if(subset3.size()<=this.subsetMaxSize)
								subsetList.add(subset3);
						}
						else {
							for(int v : diG.getOutNeighbors(nodeId))
								if(!subset2.contains(v))
									subset2.add(v);
						}
					}
				}
				
				if(subset1.size()<=this.subsetMaxSize)
					subsetList.add(subset1);
				
				if(subset2.size()<=this.subsetMaxSize)
					subsetList.add(subset2);
				//System.out.println("clusterId: " + clusterId + " => in subset: [" + subset1.toString() + "] of size " + subset1.size());
			}
			

			if(clusterSize>2 && levelNo==2){
				
				// ===============================
				// LEVEL 2
				// ===============================
				ArrayList<Integer> subset = new ArrayList<>(commonSubset);

				for(Integer nodeId : cluster){
					if(!isPermanentNode[nodeId]){
						ArrayList<Integer> subset2 = new ArrayList<>(commonSubset);
						// level 1 - 1
						for(int v : diG.getInNeighbors(nodeId)) // by definition, incoming nodes have negative link with the node
							if(!subset2.contains(v))
								subset2.add(v);
						
						// level 1 - 2
						for(int v : diG.getOutNeighbors(nodeId))
							if(!subset2.contains(v))
								subset2.add(v);
						
						if(subset2.size()>=nbEdit){
							if(subset2.size()<=this.subsetMaxSize)
								subsetList.add(subset2);
						}
						else
							subset.addAll(subset2);
							
					}
				}
				
				if(subset.size()<=this.subsetMaxSize)
				subsetList.add(subset);
			
			}
				
			
			if(clusterSize>2 && levelNo==3){

				// ===============================
				// LEVEL 3
				// ===============================
				ArrayList<Integer> subset1 = new ArrayList<>(commonSubset);
				ArrayList<Integer> subset2 = new ArrayList<>(commonSubset);
//				ArrayList<Integer> subset3 = new ArrayList<>(commonSubset);
				//ArrayList<Integer> subset = new ArrayList<>(commonSubset);
				
				for(Integer nodeId : cluster){
					if(!isPermanentNode[nodeId]){
						ArrayList<Integer> subset3 = new ArrayList<>(commonSubset);
						ArrayList<Integer> subset4 = new ArrayList<>(commonSubset);

						// level 1 - 1
						for(int v : diG.getInNeighbors(nodeId)) // by definition, incoming nodes have negative link with the node
							if(!subset3.contains(v))
								subset3.add(v);
						
						// level 1 - 2
						for(int v : diG.getOutNeighbors(nodeId))
							if(!subset3.contains(v))
								subset3.add(v);
						
						// level 1 - 1
						for(int v : diG.getInNeighbors(nodeId)) // by definition, incoming nodes have negative link with the node
							if(!subset4.contains(v))
								subset4.add(v);
						
						// level 1 - 2
						for(int v : diG.getOutNeighbors(nodeId))
							if(!subset4.contains(v))
								subset4.add(v);

//						// level 1 - 1
//						for(int v : diG.getInNeighbors(nodeId)) // by definition, incoming nodes have negative link with the node
//							if(!subset3.contains(v))
//								subset3.add(v);
//						
//						// level 1 - 2
//						for(int v : diG.getOutNeighbors(nodeId))
//							if(!subset3.contains(v))
//								subset3.add(v);
						
						// level 2 - 1
						for(int targetClusterId : possibleTargetClusterIdListArray[nodeId]){
							for(int v : possibleIncomingNodeIdListArray[targetClusterId-1])
								if(adjMat[nodeId][v]>0.0 && !subset3.contains(v))
									subset3.add(v);
						}
						
						// level 2 - 2
						for(int v : diG.getInNeighbors(nodeId)) // by definition, incoming nodes have negative link with the node
							for(int v2 : diG.getInNeighbors(v))
							if(!subset4.contains(v2))
								subset4.add(v2);

//						// level 2 - 2
//						for(int v : diG.getOutNeighbors(nodeId)) // by definition, incoming nodes have negative link with the node
//							for(int v2 : diG.getOutNeighbors(v))
//							if(!subset3.contains(v2))
//								subset3.add(v2);
						
						//
						
						if(subset3.size()>=nbEdit){
							if(subset3.size()<=this.subsetMaxSize)
								subsetList.add(subset3);
						}
						else
							subset1.addAll(subset3);
						
						if(subset4.size()>=nbEdit){
							if(subset4.size()<=this.subsetMaxSize)
								subsetList.add(subset4);
						}
						else
							subset2.addAll(subset4);
					}
						
				}
				
				if(subset1.size()<=this.subsetMaxSize)
					subsetList.add(subset1);
				if(subset2.size()<=this.subsetMaxSize)
					subsetList.add(subset2);
//				if(subset3.size()<=this.subsetMaxSize)
//					subsetList.add(subset3);
				
			}
			
			
			if(clusterSize>2 && levelNo==4){

				// ===============================
				// LEVEL 4
				// ===============================
				ArrayList<Integer> subset = new ArrayList<>(commonSubset);
				//ArrayList<Integer> subset2 = new ArrayList<>(commonSubset);

				for(Integer nodeId : cluster){
					if(!isPermanentNode[nodeId]){
						ArrayList<Integer> subset2 = new ArrayList<>(commonSubset);

						// level 1 - 1
						for(int v : diG.getInNeighbors(nodeId)) // by definition, incoming nodes have negative link with the node
							if(!subset2.contains(v))
								subset2.add(v);
						
						// level 1 - 2
						for(int v : diG.getOutNeighbors(nodeId))
							if(!subset2.contains(v))
								subset2.add(v);
						
						
						// level 2 - 1
						for(int targetClusterId : possibleTargetClusterIdListArray[nodeId]){
							for(int v : possibleIncomingNodeIdListArray[targetClusterId-1])
								if(adjMat[nodeId][v]>0.0 && !subset2.contains(v))
									subset2.add(v);
						}
						
						// level 2 - 2
						for(int v : diG.getInNeighbors(nodeId)) // by definition, incoming nodes have negative link with the node
							for(int v2 : diG.getInNeighbors(v))
							if(!subset2.contains(v2))
								subset2.add(v2);

//						// level 2 - 2
//						for(int v : diG.getOutNeighbors(nodeId)) // by definition, incoming nodes have negative link with the node
//							for(int v2 : diG.getOutNeighbors(v))
//							if(!subset.contains(v2))
//								subset.add(v2);
					
						//
						
						if(subset2.size()>=nbEdit){
							if(subset2.size()<=this.subsetMaxSize)
								subsetList.add(subset2);
						}
						else
							subset.addAll(subset2);
					}
						
				}
				
				if(subset.size()<=this.subsetMaxSize)
					subsetList.add(subset);
				
			}
			
		}
		
				
		return(subsetList);
	}

}
