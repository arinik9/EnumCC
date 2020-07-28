package enumeration;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import myUtils.ArrayOperations;
import myUtils.Clustering;
import myUtils.Combination;
import myUtils.MyCGraph;
import myUtils.Permutation;
import myUtils.UniquePartitionSize;

public class MyGenericBruteForceEnumeration extends Thread  {
	Clustering initClustering;
	int nbEdit;
	int n;
	MyCGraph g;
	//long idCounter;
	double[][] adjMat;
	public Set<Clustering> foundClusterings;
	int[] clusterIds;
	int nbSourceCluster=0;
	
	//public MyGenericEnumeration(int nbEdit_, double[][] adjMat_, Clustering initClustering_, long idCounter_){
	public MyGenericBruteForceEnumeration(int nbEdit_, double[][] adjMat_, Clustering initClustering_){
		nbEdit = nbEdit_;
		initClustering = initClustering_;
		n = initClustering.n;
		g = new MyCGraph(n, initClustering);
		g.fillInNodeToWeightedDegreeSum(adjMat_);
		//idCounter = idCounter_;
		adjMat = adjMat_;
		foundClusterings = new HashSet<Clustering>();
	}
	
	
	
	public void setNbSourceCluster(int nbSourceCluster_){
		this.nbSourceCluster = nbSourceCluster_;
	}
	
	
	public void setClusterIds(int[] clusterIds_){
		this.clusterIds = new int[clusterIds_.length];
		for(int i=0; i<clusterIds_.length; i++)
			this.clusterIds[i] = clusterIds_[i];
	}
	
	
	public void run() 
    { 
        try
        { 
            // Displaying the thread that is running 
            System.out.println ("Thread " + 
                  Thread.currentThread().getId() + 
                  " is running"); 
            
            if(this.clusterIds.length>0 && this.nbSourceCluster!=0)
            	enumerateByNbSourceCluster(this.nbSourceCluster, this.clusterIds);
  
        } 
        catch (Exception e) 
        { 
            // Throwing an exception 
            System.out.println ("Exception is caught"); 
            //Thread.currentThread().interrupt();
        } 
    }
	
	public long getNbEdit(){
		return(nbEdit);
	}
	
//	public long getIdCounter(){
//		return(idCounter);
//	}
	
	// main method
	public void enumerate() {
		foundClusterings.clear();	
		//ArrayList<ArrayList<Integer>> clusters = initClustering.getClustersInArrayFormat();
		
		List<Integer> clusterIds = new ArrayList<Integer>();
		for(int i=1; i<=initClustering.getNbCluster(); i++)
			clusterIds.add(i);
		
		//List<int[]> allPermCombClusterIds = new ArrayList<>();
		for(int nbSourceCluster=1; nbSourceCluster<=nbEdit; nbSourceCluster++){
			List<int[]> combClusterIds = Combination.generate(clusterIds, nbSourceCluster);
			
			for(int[] subClusterIds : combClusterIds){
				enumerateByNbSourceCluster(nbSourceCluster, subClusterIds);
			}
			
		}
		
	}

	
	
	
	public void enumerateByNbSourceCluster(int nbSourceCluster, int[] clusterIds) {
		// clusterIds: {1,4,5}
		TreeSet<int[]> allUniquePermClusterSizes = new TreeSet<int[]>(
				new Comparator<int[]>(){

					@Override
					public int compare(int[] o1, int[] o2) {
						int value=0;
						for(int i=0; i<o1.length; i++){
							if(o1[i] != o2[i]){
								value = 1;
								break;
							}
								
						}
						return value;
					}
				}
		);
		
		List<int[]> result = UniquePartitionSize.generate(nbEdit, nbSourceCluster);
		for(int[] a : result){
			allUniquePermClusterSizes.addAll(Permutation.permute(a));
		}
		
		int[] initClusterSizes = initClustering.getClusterSizes();
		for(int[] sourceClusterSizes : allUniquePermClusterSizes){
			//List<Integer> sizes = Arrays.stream(psizes).boxed().collect(Collectors.toList());
			boolean process = true;
			for(int i=1; i<=nbSourceCluster; i++){
				if(sourceClusterSizes[i-1] > initClusterSizes[clusterIds[i-1]-1]){
					process = false;
					break;
				}
			}
			
			if(process){ // if process = true, then clusterSizes is eligible
				enumerateByNodes(sourceClusterSizes, clusterIds);
			}
			
		}
	}
	
	
	
	public void enumerateByNodes(int[] sourceClusterSizes, int[] sourceClusterIds) {
		// sourceClusterSizes : {6, 1, 1}
		// clusterIds : {2,4,5}
		ArrayList<ArrayList<ArrayList<TNode>>> allSelNodesList = new ArrayList<ArrayList<ArrayList<TNode>>>();
		
		ArrayList<ArrayList<Integer>> clusters = initClustering.getClustersInArrayFormat();
		for(int i=1; i<=sourceClusterIds.length; i++){
			int sourceClusterId = sourceClusterIds[i-1];
			int nbNode = sourceClusterSizes[i-1];
			List<int[]> combNodeIds = Combination.generate(clusters.get(sourceClusterId-1), nbNode);
			
			ArrayList<ArrayList<TNode>> newSelNodesList = new ArrayList<ArrayList<TNode>>();
			for(int[] nodeIds : combNodeIds){
				// nodeIds : {2,3,5,7,8,9}
				ArrayList<TNode> selNodes = new ArrayList<>();
				for(int j=0; j<nodeIds.length; j++){
					TNode node = new TNode(nodeIds[j],sourceClusterId,-1, new ArrayList<>());
					selNodes.add(node);
				}
				newSelNodesList.add(selNodes);
			}
			
			// if there are already items in 'selNodesList', we need to duplicate each item in 'selNodesList' by tempSelNodesList.size() times
			ArrayList<ArrayList<TNode>> combSelNodesList = new ArrayList<ArrayList<TNode>>();
			if(allSelNodesList.size()>0){
				// ex: newSelNodesList = {{6,7},{6,8},{7,8}} --> it is created for each source cluster and 1 item per each combination
				// selNodesList: {{{1,2}},{{1,3}},{{2,3}}}
				//output: {{{1,2},{6,7}},{{1,2},{6,8}},{{1,2},{7,8}}, {{1,3},{6,7}},{{1,3},{6,8}},{{1,3},{7,8}}, {{2,3},{6,7}},{{2,3},{6,8}},{{2,3},{7,8}}}
				ArrayList<ArrayList<ArrayList<TNode>>> allSelNodesList2 = new ArrayList<ArrayList<ArrayList<TNode>>>(allSelNodesList);
				allSelNodesList.clear();
				for(ArrayList<TNode> newSelNodes : newSelNodesList){
					for(ArrayList<ArrayList<TNode>> selNodesList : allSelNodesList2){
						ArrayList<ArrayList<TNode>> selNodesList2 = new ArrayList<ArrayList<TNode>>(selNodesList);
						selNodesList2.add(newSelNodes);
						allSelNodesList.add(selNodesList2);
					}
				}
				
			} else { // if empty
				// ex: newSelNodesList = {{1,2},{1,3},{2,3}}
				// output: {{{1,2}},{{1,3}},{{2,3}}}
				for(ArrayList<TNode> newSelNodes : newSelNodesList){
					ArrayList<ArrayList<TNode>> temp = new ArrayList<ArrayList<TNode>>();
					temp.add(newSelNodes);
					allSelNodesList.add(temp);
				}
			}
			
		}
		
		
		for(ArrayList<ArrayList<TNode>> selNodesList : allSelNodesList){
			enumerateByTargetClusters(selNodesList, sourceClusterSizes, sourceClusterIds);
		}
	}
	
	
	///////////////
	// selSourceClusterIds: {4,5}
	// selNodesList: [[1, 2, 3, 4], [6, 7, 8]] when nb edit is 7 (i.e. 7 nodes) and nb source cluster is 2
	public void enumerateByTargetClusters(ArrayList<ArrayList<TNode>> selNodesList, int[] sourceClusterSizes, int[] selSourceClusterIds) {
		// here we decide which nodes moves into which cluster
		//	- they can move some existing cluster
		//	- or a new cluster
		
		//ArrayList<ArrayList<ArrayList<TNode>>> allSelNodesList = new ArrayList<ArrayList<ArrayList<TNode>>>();
			
		Map<Integer, ArrayList<ArrayList<ArrayList<TNode>>>> selNodesListBySourceClusterMap = new HashMap<>();
		
		
		//ArrayList<ArrayList<Integer>> allSelTargetClusterIdList = new ArrayList<ArrayList<Integer>>();
		for(int sourceClusterId=1; sourceClusterId<=selSourceClusterIds.length; sourceClusterId++){ // for each source cluster (we can choose target cluster ids independently of the other clusters)
			ArrayList<TNode> selNodesInCluster = selNodesList.get(sourceClusterId-1);
			
			int sourceClusterSize = sourceClusterSizes[sourceClusterId-1];
			int selSourceClusterId = selSourceClusterIds[sourceClusterId-1];
			int[] remainingClusterIds = new int[initClustering.getNbCluster()];
			int index=0;
			for(int j=1; j<=initClustering.getNbCluster(); j++){
				if(j != selSourceClusterId)
					remainingClusterIds[index++] = j;
			}
			
			int nbMaxTargetCluster = remainingClusterIds.length;
			if(sourceClusterSize<remainingClusterIds.length)
				nbMaxTargetCluster = sourceClusterSize;
			
			if(nbMaxTargetCluster>0){
				
				ArrayList<ArrayList<ArrayList<TNode>>> updatedSelNodesList = new ArrayList<ArrayList<ArrayList<TNode>>>();
				
				for(int nbTargetCluster=1; nbTargetCluster<=nbMaxTargetCluster; nbTargetCluster++){ // for each possible nb target cluster
					
					// 1) combinations of target cluster ids based on nbTargetCluster
					List<int[]> allPermCombTargetClusterIds = new ArrayList<>();
					List<int[]> combTargetClusterIds = Combination.generate(remainingClusterIds, nbTargetCluster); // ex: combTargetClusterIds: {2,3}
					for(int[] a : combTargetClusterIds){
						//allPermCombTargetClusterIds.addAll(Permutation.permute(a));
						allPermCombTargetClusterIds.add(a); // TODO it seems to be correct in this way (so without permutation), but why ?
					} //ex:  allPermCombTargetClusterIds: {{2,3},{3,2}}
					
					
					// 2) combinations of nodes to move
					List<ArrayList<int[]>> collector = new ArrayList<ArrayList<int[]>>();
					List<int[]> result = UniquePartitionSize.generate(sourceClusterSize, nbTargetCluster);
					for(int j=0; j<result.size(); j++){ // ex: result: {{3,1},{2,2}} for nbTargetCluster=2 and for source cluster1 whose size is 4
						int[] partitionsizes = result.get(j);
						
						int[] nodeIndexes = new int[selNodesInCluster.size()];
						for(int k=0; k<nodeIndexes.length; k++)
							nodeIndexes[k] = k;
						
						List<ArrayList<int[]>> collector2 = new ArrayList<ArrayList<int[]>>();
						
						for(int nbNodeToMove : partitionsizes){ // ex: partitionsizes: {3,1}, nodeIndexes:{0,1,2,3,4}
							
							if(collector2.size()>0){
								
								List<ArrayList<int[]>> collector3 = new ArrayList<ArrayList<int[]>>();
								
								// ex: collector2: {{{1,3}},{{2,5}}}
								for(ArrayList<int[]> arrayOfExistingIndexes : collector2){
									int[] existingIndexes = ArrayOperations.mergeMultipleArrays(arrayOfExistingIndexes);
									
									int[] remainingNodeIndexes = ArrayOperations.removeMultipleElements(nodeIndexes, existingIndexes);
									List<int[]> combNodeIndexesToMove = Combination.generate(remainingNodeIndexes, nbNodeToMove);
									// ex: 	existingIndexes: {1,3}, combNodeIndexesToMove: {{2,4},{2,5}}
									// output: {{{1,3},{2,4}},{{1,3},{2,5}}}
									
									for(int[] indexesToMove : combNodeIndexesToMove){
										ArrayList<int[]> newArray = new ArrayList<int[]>(arrayOfExistingIndexes);
										newArray.add(indexesToMove);
										collector3.add(newArray);
									}
									
								}
								
								collector2.clear();
								collector2 = new ArrayList<ArrayList<int[]>>(collector3);
								
							} else {
								List<int[]> combNodeIndexesToMove = Combination.generate(nodeIndexes, nbNodeToMove);
								
								for(int[] d : combNodeIndexesToMove){
									ArrayList<int[]> temp = new ArrayList<int[]>();
									temp.add(d);
									collector2.add(temp);
								}
								
							}
							
						}
						
						collector.addAll(collector2);
					}
					

					
					// 3) associate node indexes and target cluster ids AND call another method for the next part
					// ex: nbTargetCluster:2, nbNodeInSourceCluster=4,  collector: {{{1,2,4},{3}},{{2,3,4},{1}},{{1,2,3},{4}},{{1,2},{3,4}},{{1,3},{2,4}}}
					// 		allPermCombTargetClusterIds: {{6,7},{7,6}}
					
					ArrayList<ArrayList<TNode>> updatedSelNodesListByNbTargetCluster = new ArrayList<ArrayList<TNode>>();
					
					// selNodesList: [[1, 2, 3, 4], [6, 7, 8]]
					for(int k1=0; k1<allPermCombTargetClusterIds.size(); k1++){
						int[] targetClusterIds = allPermCombTargetClusterIds.get(k1);
						
						for(int k2=0; k2<collector.size(); k2++){ // collector by nbTargetCluster
							ArrayList<int[]> arrNodeIndexes = collector.get(k2);
							
							// deep copy
							ArrayList<TNode> selNodesInCluster2 = new ArrayList<TNode>();
							for(TNode node : selNodesInCluster){
								TNode node2 = new TNode(node);
								selNodesInCluster2.add(node2);
							}
							
							
							
							for(int k3=0; k3<nbTargetCluster; k3++){
								int[] nodeIndexes = arrNodeIndexes.get(k3);
								int targetClusterId = targetClusterIds[k3];
								for(int indx : nodeIndexes){
									selNodesInCluster2.get(indx).setTargetClusterId(targetClusterId);
								}
							}
							
							updatedSelNodesListByNbTargetCluster.add(selNodesInCluster2);
						}
					}
					
					updatedSelNodesList.add(updatedSelNodesListByNbTargetCluster);
				}
				
				selNodesListBySourceClusterMap.put(sourceClusterId, updatedSelNodesList);
			}
		}
		
		
		
		// 4) In selNodesListBySourceClusterMap, we grouped selected node objects by source cluster id
		//		Now, we combine all this information in order that all nodes to move (in all source clusters) are together in the same arraylist
		
		ArrayList<ArrayList<TNode>> finalSelNodesList = new ArrayList<ArrayList<TNode>>();
		boolean init = true;
		for(int sourceClusterId=1; sourceClusterId<=selSourceClusterIds.length; sourceClusterId++){ // for each source cluster
			ArrayList<ArrayList<ArrayList<TNode>>> updatedSelNodesListByNbTargetCluster = selNodesListBySourceClusterMap.get(sourceClusterId);

			ArrayList<ArrayList<TNode>> finalSelNodesList2 = new ArrayList<ArrayList<TNode>>(finalSelNodesList);

			if(init==true && finalSelNodesList.size()>0)
				init = false;
			if(init == false)
				finalSelNodesList.clear();
			
			for(ArrayList<ArrayList<TNode>> updatedSelNodesList : updatedSelNodesListByNbTargetCluster){ // by nb target cluster
				
				for(ArrayList<TNode> updatedSelNodes : updatedSelNodesList){
					
					if(init == true){
						finalSelNodesList.add(updatedSelNodes);
					} else {
						
						for(int a=0; a<finalSelNodesList2.size(); a++){
							ArrayList<TNode> finalSelNodes = new ArrayList<TNode>();
							ArrayList<TNode> tmp = finalSelNodesList2.get(a); //.addAll(updatedSelNodes);
							for(TNode node : tmp){
								finalSelNodes.add(new TNode(node));
							}
							finalSelNodes.addAll(updatedSelNodes);
							finalSelNodesList.add(finalSelNodes);
						}
						
					}
				}
			}
			
			
		}
		
		
		
		
		
		
		// 5) finally, call the next method
		for(ArrayList<TNode> finalSelNodes : finalSelNodesList){
			//System.out.println(finalSelNodes);
			ArrayList<TNode> finalSelNodes2 = new ArrayList<TNode>();
					
			// 5.1) we ensure that the current edit operation is not decomposable
			Map<Integer,TNode> IdToTNodeMap = new HashMap<>();
			
			int[] nodeIds = new int[finalSelNodes.size()];
			int index=0;
			for(TNode node : finalSelNodes){
				nodeIds[index++] = node.getNodeId();
				IdToTNodeMap.put(node.getNodeId(), node);
				finalSelNodes2.add(new TNode(node));
			}
			
			boolean decomposableEditOp = false; 
			for(int r=1; r<nbEdit; r++){
				List<int[]> combNodeIds = Combination.generate(nodeIds, r);
				if(decomposableEditOp == false){
					for(int[] currNodeIds : combNodeIds){ // for each combination of node ids
						ArrayList<TNode> subFinalSelNodes = new ArrayList<TNode>();
						for(int currNodeId : currNodeIds){
							TNode node = IdToTNodeMap.get(currNodeId);
							subFinalSelNodes.add(node);
						}
						
						boolean isOptimalTransformation = checkIfOptimalTransformation(subFinalSelNodes);
						if(isOptimalTransformation){
							decomposableEditOp = true;
							break;
						}
						
					}
				}
			}
			
			
			// 5.2)
			if(decomposableEditOp == false){
				boolean isOptimalTransformation = checkIfOptimalTransformation(finalSelNodes);
				if(isOptimalTransformation){
					
					//double w = adjMat[finalSelNodes.get(0).getNodeId()][finalSelNodes.get(1).getNodeId()];
					
					//System.out.println(finalSelNodes);
					//Clustering cnew = new Clustering(initClustering, idCounter++);
					Clustering cnew = new Clustering(initClustering);
					//System.out.println(cnew);
					cnew.changeClusterOfMultipleNodes(finalSelNodes);
					cnew.computeImbalance(adjMat);
					//System.out.println(cnew);
					
					foundClusterings.add(cnew);
				}
			}
		}
		
		
		
	}
	
	
	
	public boolean checkIfOptimalTransformation(ArrayList<TNode> selNodes){
		boolean isOptimalTransformation = true;
	
//		boolean isDecomposableEditOp = checkIfDecomposableEditOp(selNodes);
//		if(isDecomposableEditOp == true)
//			return(false);
		
//		boolean ok = false;
//		if(selNodes.size() == 3 && selNodes.get(0).getNodeId()==8 && selNodes.get(0).getTargetClusterId()==2
//				&& selNodes.get(1).getNodeId()==14 && selNodes.get(1).getTargetClusterId()==3
//				&& selNodes.get(2).getNodeId()==12 && selNodes.get(2).getTargetClusterId()==2){
//			ok = true;
//		}
//		
//		if(ok){
//			System.out.println(ok);
//		}
		
		// 1) calculate fitness value for each node
		for(TNode node : selNodes){
			node.computeCurrNodeFitness(g, adjMat);
			node.computeTargetNodeFitness(g, adjMat);
		}
		
		
		// 2) adjust fitness values of the selected nodes in their current clusters
			// if there are some nodes sharing the same cluster, we adjust current fitness
			// we need also to adjust target fitness, if we know where to put some nodes
		for(TNode node : selNodes){
			for(TNode otherNode : selNodes){
				if(node.getNodeId() != otherNode.getNodeId()){
					if(node.getClusterId() == otherNode.getClusterId()){ // if they are in the same cluster
						node.substractFromCurrNodeFitness(adjMat[node.getNodeId()][otherNode.getNodeId()]);
					}
					
					if(node.getTargetClusterId() == otherNode.getClusterId()){ // if 'otherNode' is placed in the cluster where 'node' will move into
						node.substractFromTargetNodeFitness(adjMat[node.getNodeId()][otherNode.getNodeId()]);
					}
				}
			}
		}
		
		
		
		// 3) for each moving node, check the main inequality
		for(TNode node : selNodes){
			
			// let suppose that the reference node v_r is in cluster C1 and moves into C3
			double weightsNowSameThenSame = 0.0; // the link weights that v_r shares currently and later in its cluster with other nodes
			double weightsNowNextTargetThenDiff = 0.0; // the link weights that v_r has with the nodes located currently in C3 but then those nodes leave their cluster
			double weightsNowDiffThenSame = 0.0; // the link weights that v_r has with the nodes located currently in different cluster, then they will be together in C3
			double NowDiffThenCurr = 0.0; //the link weights that v_r has with the nodes located currently in different cluster,ode nodes move into C1
			
			for(TNode otherNode : selNodes){
				if(node.getNodeId() != otherNode.getNodeId()){
					
					double w = adjMat[node.getNodeId()][otherNode.getNodeId()];
					if(w != 0.0){
					
						if(node.getClusterId() == otherNode.getClusterId() && node.getTargetClusterId() == otherNode.getTargetClusterId()){
							weightsNowSameThenSame += w;
						} else if(node.getTargetClusterId() == otherNode.getClusterId() && node.getTargetClusterId() != otherNode.getTargetClusterId()){
							weightsNowNextTargetThenDiff += w;
						} else if(node.getClusterId() != otherNode.getClusterId() && node.getTargetClusterId() == otherNode.getTargetClusterId()){
							weightsNowDiffThenSame += w;
						} else if(node.getClusterId() != otherNode.getClusterId() && node.getClusterId() == otherNode.getTargetClusterId()
								&& node.getClusterId() != node.getTargetClusterId()){
							NowDiffThenCurr += w;
						}
						
						// calculate the inequalities
						if((weightsNowSameThenSame - weightsNowNextTargetThenDiff) < node.calculateDeltaFitness()){
							return(false);
						}
						if(node.calculateDeltaFitness() < (-weightsNowSameThenSame -weightsNowDiffThenSame +NowDiffThenCurr)){
							return(false);
						}
						if(node.calculateDeltaFitness() < (-2*weightsNowSameThenSame +weightsNowNextTargetThenDiff -weightsNowDiffThenSame +NowDiffThenCurr)){
							return(false);
						}
						
					}
				}
				
			}
		}
		
		
		
		// 4) observe the change of fitness caused by links between moving nodes
		double change = 0;
		for(int i=0; i<selNodes.size(); i++){
			TNode node = selNodes.get(i);
			for(int j=0; j<i; j++){
				TNode otherNode = selNodes.get(j);
				
				double w = adjMat[node.getNodeId()][otherNode.getNodeId()];
				if(w != 0.0){
					if(node.getClusterId() == otherNode.getClusterId() && node.getTargetClusterId() != otherNode.getTargetClusterId()){
						if(w<0) // now intra-negative link, but later inter-negative link
							change += w; // system gain
						else if(w>0) // now intra-positive link, but later inter-positive link
							change -= w; // system loss
					}
					
					if(node.getClusterId() != otherNode.getClusterId() && node.getTargetClusterId() == otherNode.getTargetClusterId()){
						if(w<0) // now inter-negative link, but later intra-negative link
							change -= w; // system loss
						else if(w>0) // now inter-positive link, but later intra-positive link
							change += w; // system gain
					}
				}
			}
		}
		
		
		// 5) check if it is indeed optimal transformation
		double delta = change;
		for(TNode node : selNodes)
			delta += node.calculateDeltaFitness();
		if(delta != 0.0){
			isOptimalTransformation = false;
		}
			
//		if(selNodes.size()==2 && isOptimalTransformation){
//			double w = adjMat[selNodes.get(0).getNodeId()][selNodes.get(1).getNodeId()];
//			Clustering cnew = new Clustering(initClustering, idCounter++);
//			for(TNode node : selNodes){
//				cnew.changeClusterOfNode(node.getNodeId(), node.getTargetClusterId());
//			}
//			cnew.computeImbalance(adjMat);
//			System.out.println(cnew);
//			System.out.println(isOptimalTransformation);
//		}
		
		return(isOptimalTransformation);
	}
	
	

	
	

	
	
	
}


