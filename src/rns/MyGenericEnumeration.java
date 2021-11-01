package rns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import rns.utils.ArrayOperations;
import myUtils.Clustering;
import rns.utils.Combination;
import rns.utils.Combination2;
import rns.utils.DirectedGraph;
import rns.utils.MyCGraph;
import rns.utils.Permutation;
import rns.utils.UndirectedGraph;
import rns.utils.UniquePartitionSize;
import rns.utils.TNode;


public class MyGenericEnumeration extends Thread {

	int pass; // pass id
	Clustering initClustering;
	int maxNbEdit;
	int minNbEdit;

	int n;
	MyCGraph g;
	double[][] adjMat;
	public Set<Clustering> foundClusterings;
	public Map<Integer, ArrayList<Clustering>> foundClusteringsByNbEditMap;
	int[] clusterIds;
	double[] execTimesByNbEdit;
	double totalExecTime;
	boolean isBruteForce = false; // value by default
	

	public MyGenericEnumeration(int minNbEdit_, int maxNbEdit_, double[][] adjMat_, Clustering initClustering_, int pass_, boolean isBruteForce_){
		maxNbEdit = maxNbEdit_;
		minNbEdit = minNbEdit_;
		initClustering = initClustering_;
		n = initClustering.n;
		g = new MyCGraph(n, initClustering);
		g.fillInNodeToWeightedDegreeSum(adjMat_);
		//idCounter = idCounter_;
		adjMat = adjMat_;
		foundClusterings = new HashSet<Clustering>();
		foundClusteringsByNbEditMap = new HashMap<Integer, ArrayList<Clustering>>();
		execTimesByNbEdit = new double[maxNbEdit];
		this.pass = pass_;
		this.isBruteForce = isBruteForce_;
		
	}
	
	
	// main method for Thread
	public void run() 
    { 
        try
        {       
            enumerate();
        } 
        catch (Exception e) 
        { 
            // Throwing an exception 
            System.out.println ("Exception is caught in 'run()'. Switch to sequantial enum (without thread) to see the error."); 
            //Thread.currentThread().interrupt();
        } 
    }
	
	
	
	// main method
	public void enumerate(){
		
		System.out.println(initClustering);
		for(int nbEdit=minNbEdit;nbEdit<=this.maxNbEdit;nbEdit++){
			//System.out.println("nbEdit: " + nbEdit);
			long startTime = System.currentTimeMillis();
			
			enumerateByNbEdit(nbEdit);
			
			long endTime = System.currentTimeMillis();
			execTimesByNbEdit[nbEdit-1] = (float) (endTime-startTime)/1000;
			System.out.println("nbEdit: " + nbEdit + ", exec time: " + execTimesByNbEdit[nbEdit-1]);
			totalExecTime += execTimesByNbEdit[nbEdit-1]; // 'execTimesByNbEdit[nbEdit]' is assigned in 'enumerateByNbEdit'

		}
	}
	
	
	// main method by nbEdit
	public void enumerateByNbEdit(int nbEdit) {
		// actually, the object 'foundClusteringsByNbEditMap' is not important for output, it only stores the discovered solutions in the current method, 
		//		then they are transferred into 'foundClusterings'
		foundClusteringsByNbEditMap.put(nbEdit, new ArrayList<Clustering>());
				
		
		List<Integer> clusterIds = new ArrayList<Integer>();
		for(int i=1; i<=initClustering.getNbCluster(); i++)
			clusterIds.add(i);
		
		for(int nbSourceCluster=1; nbSourceCluster<=nbEdit; nbSourceCluster++){
			List<int[]> combClusterIds = Combination.generate(clusterIds, nbSourceCluster);
			for(int[] subClusterIds : combClusterIds){
				enumerateByNbSourceCluster(nbEdit, nbSourceCluster, subClusterIds);
			}
		}
		
		// ========
		
		for(Clustering c : foundClusteringsByNbEditMap.get(nbEdit)){
			c.setParentClusteringId(initClustering.getId());
			c.setNbEditParent(nbEdit);
			foundClusterings.add(c); // before adding c into this generic list, update its info related to its parent
		}
	}

	
	
	
	
	
	public void enumerateByNbSourceCluster(int nbEdit, int nbSourceCluster, int[] clusterIds) {
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
				enumerateByNodes(nbEdit, sourceClusterSizes, clusterIds);
			}
			
		}
		
	}

		
	
	public void enumerateByNodes(int nbEdit, int[] sourceClusterSizes, int[] sourceClusterIds) {
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

				boolean ok = true;
				for(int j=0; j<nodeIds.length; j++){
					TNode node = new TNode(nodeIds[j],sourceClusterId,-1, new ArrayList<>());
					
					
					// =========================
					selNodes.add(node);
				}
				if(ok)
					newSelNodesList.add(selNodes);
				
			}
			
			// if there are already items in 'selNodesList', we need to duplicate each item in 'selNodesList' by tempSelNodesList.size() times
			//ArrayList<ArrayList<TNode>> combSelNodesList = new ArrayList<ArrayList<TNode>>();
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
		

		for(ArrayList<ArrayList<TNode>> selNodesList : allSelNodesList){ // by nb source cluster ?
//			if(nbEdit > 3 && sourceClusterSizes.length>1 && sourceClusterSizes[0]==3){
//				System.out.println("!!!");
//			}
			enumerateByTargetClusters(nbEdit, selNodesList, sourceClusterSizes, sourceClusterIds);
		}
		
	}
	
	
	///////////////
	// selSourceClusterIds: {4,5}
	// selNodesList: [[1, 2, 3, 4], [6, 7, 8]] when nb edit is 7 (i.e. 7 nodes) and nb source cluster is 2
	public void enumerateByTargetClusters(int nbEdit, ArrayList<ArrayList<TNode>> selNodesList, int[] sourceClusterSizes,
			int[] selSourceClusterIds) 
	{
		// here we decide which nodes moves into which cluster
		//	- they can move some existing cluster
		//	- or a new cluster
		
		//ArrayList<ArrayList<ArrayList<TNode>>> allSelNodesList = new ArrayList<ArrayList<ArrayList<TNode>>>();
		// selNodesList: [[(nodeId:0, clusterId:1, targetClusterId:-1)], [(nodeId:7, clusterId:2, targetClusterId:-1)], [(nodeId:14, clusterId:3, targetClusterId:-1)]]
		// source cluster size: [1, 1, 1]
		// selSourceClusterIds: [1, 2, 3]
		
		
		// selNodesList:  [[(nodeId:0, clusterId:1, targetClusterId:-1), (nodeId:1, clusterId:1, targetClusterId:-1)], [(nodeId:7, clusterId:2, targetClusterId:-1)]] ==> an array of size 2
		// source cluster size: [2,1]
		// // selSourceClusterIds: [1, 1, 2]
		//System.out.println("end");
		ArrayList<ArrayList<Integer>> initClusters = initClustering.getClustersInArrayFormat();
		//System.out.println(initClusters);
		
		boolean hasSingleSourceCluster = (selNodesList.size()==1);
		boolean hasTwoSourceClusters = (selNodesList.size()==2);
		boolean isSourceClusterSizesLessThanThree = true; // in case of 4edit or more, this will be a helper function to find sub2edit or sub3edit operation
		for(int i=0; i<sourceClusterSizes.length; i++){
			if(sourceClusterSizes[i]>2){
				isSourceClusterSizesLessThanThree = false;
				break;
			}
		}

		
		// ==========================================================
		// PART 1: Trying to filter before determining target cluster ids
		// ==========================================================
		boolean isConnectedSourceNodes = isConnectedMovingNodes_(selNodesList, this.adjMat); // not necessarily positive connectivity
		boolean isFakeLinkConnectivity = isFakeLinkConnectivityWithInternalSourceNeighborNodes(selNodesList);
		boolean isNonNegativeInternalNeighLinks = true;
		if(nbEdit <= 3 && !isBruteForce)
			isNonNegativeInternalNeighLinks = isNonNegativeInternalNeighborLinksForUpTo3Edit(selNodesList); // TODO change the name: NotNegative ...
		
		boolean ok = true;
		
		if(isConnectedSourceNodes && !isFakeLinkConnectivity && isNonNegativeInternalNeighLinks && ok){ // !isBruteForce && 
		
			ArrayList<TNode> selNodes = new ArrayList<TNode>();
			ArrayList<int[]> allClusterIdsInterest = new ArrayList<>();
			boolean[] isWholeClusterBoolArr = new boolean[sourceClusterSizes.length];
			for(int i=0; i<selNodesList.size(); i++){ // for each source cluster => selNodesList.size() is equal to nbSourceCluster
				ArrayList<TNode> selNodesBySourceCluster = selNodesList.get(i);
	
				// when some nodes are selected from selected source cluster, check if the cluster contains only those moving nodes or not
				// if so, do not allow this, since it is a fake edit transformation
				boolean isWholeCluster = initClusters.get(selNodesBySourceCluster.get(0).getClusterId()-1).size() == sourceClusterSizes[i]; // get the first node of the cluster
				isWholeClusterBoolArr[i] = isWholeCluster;
				
				for(TNode node : selNodesBySourceCluster){
					if(node.getPossibleTargetClusterIds().size()>0){
						selNodes.add(node);
						
						int[] clusterIdsInterest = new int[node.getPossibleTargetClusterIds().size()];
						for(int l=0; l<node.getPossibleTargetClusterIds().size(); l++)
							clusterIdsInterest[l] = node.getPossibleTargetClusterIds().get(l);
						allClusterIdsInterest.add(clusterIdsInterest);
						
					} else {
						node.addNotEqualToConstraints(new ArrayList<Integer>(Arrays.asList(node.getClusterId()))); // a moving node can not stay in its current cluster 
						selNodes.add(node);
						
						int[] clusterIdsInterest = selSourceClusterIds.clone();// i.e. the cluster ids of all moving nodes, except its cluster id
						clusterIdsInterest[i] = -1; // replace the id of the source cluster by -1, where -1 means a new cluster (not necesserily empty new one)
						allClusterIdsInterest.add(clusterIdsInterest);
					}
				}
				
			} // selNodes is an array list containing all selected moving nodes of size 'nbEdit'
			
			
				
			// =============================================================================
			// PART 2: Trying to filter after target cluster ids & before determining target indexes 
			// =============================================================================
			
			ArrayList<ArrayList<TNode>> updatedSelNodesList = prepareSelNodesWithTargetClusterIds(selNodes, allClusterIdsInterest);
			
			// When heuristic 4-edit or 5-edit is used
			boolean okForTargetIdexes = true;
			if(!isBruteForce && nbEdit > 3){
				okForTargetIdexes = false;
				for(ArrayList<TNode> selNodes1 : updatedSelNodesList){
					for(TNode node : selNodes1){
						if(node.getTargetClusterId() == -1){
							okForTargetIdexes = true;
							break;
						}
					}
					if(okForTargetIdexes)
						break;
				}
			}

			
			// note that the next filtering (i.e. connected comp) does not completely guarantee 'indecomposability' at this stage,
			//		because we do not know the target indexes. But it might remove some decomposable cases, so it might beneficial to call the method here
			ArrayList<ArrayList<TNode>> updatedSelNodesList2 = updatedSelNodesList;
			if(!isBruteForce && nbEdit <= 3)
				updatedSelNodesList2 = filterByFakeEditTransformation(updatedSelNodesList, selSourceClusterIds, isWholeClusterBoolArr);
			
			
			// ==============================
			ArrayList<ArrayList<TNode>> updatedSelNodesList3 = updatedSelNodesList2;
			
			// trying to filter for 4-edit without strict condition is possible, but it will not gain much time, like 3-edit
			if(!isBruteForce && nbEdit <= 3) 
				updatedSelNodesList3 = filterForUnweightedGraphBeforeTargetIndexesForExternalLinksUpTo3Edit(updatedSelNodesList2, hasTwoSourceClusters);
			else if(!isBruteForce && nbEdit > 3)
				updatedSelNodesList3 = filterForUnweightedGraphBeforeTargetIndexesForExternalLinks(updatedSelNodesList2, false);

			
			// =============================================================================
			// PART 3: Trying to filter after determining target indexes 
			// =============================================================================

			ArrayList<ArrayList<TNode>> updatedSelNodesList4 = updatedSelNodesList3;
			if(okForTargetIdexes)
				updatedSelNodesList4 = prepareSelNodesWithTargetIndexes(updatedSelNodesList3);
						
			if(selNodesList.size()>1 && okForTargetIdexes) // the size of selNodesList is 1, this means, all moving nodes are in the same source cluster, so they are connected by construction
				updatedSelNodesList4 = filterByConnectedComponentInteractionConnectivity(updatedSelNodesList4); // remove decomposable Edit transformations
				// TODO rename it "filterByPosConnectedComponent"
			
			
			if(!isBruteForce && okForTargetIdexes && nbEdit>1)
				updatedSelNodesList4 = filterByMVMOCorollary(updatedSelNodesList4);
			
			
			ArrayList<ArrayList<TNode>> updatedSelNodesList5 = updatedSelNodesList4;
			if(nbEdit>3) {// TODO we will change the name 'filterByWeightSumZeroWithInternalTargetNeighborNodes'
				updatedSelNodesList5 = filterByFakeConnectivityWithInternalTargetNeighborNodes(updatedSelNodesList4); // we do not need to do for 2edit or 3edit, because we implicitely do it in 'filterForUnweightedGraphBeforeTargetIndexesForExternalLinksUpTo3Edit()'
				if(!isBruteForce && isSourceClusterSizesLessThanThree){ // seek for sub 2edit or sub 3edit operation for additional filtering
					updatedSelNodesList5 = filterForUnweightedGraphBeforeTargetIndexesForExternalLinks(updatedSelNodesList5, true);
				}
			}
			
			
			//System.out.println("size of 'updatedSelNodesList4':" + updatedSelNodesList4.size());
			for(ArrayList<TNode> updatedSelNodes : updatedSelNodesList5){
//				if(nbEdit == 5)
//					System.out.println(updatedSelNodes);
				boolean isEligible = true;
				if(!isBruteForce && updatedSelNodes.size()>1)
					isEligible = isEligibleTransformation(updatedSelNodes, false);
				if(isEligible){					
					ArrayList<ArrayList<TNode>> optimalTransformations = findOptimalTransformations(updatedSelNodes, selSourceClusterIds,
							isWholeClusterBoolArr, initClusters);
					
					if(optimalTransformations.size()>0){
						Set<Clustering> set = new HashSet<Clustering>();
						if(nbEdit > 1){
//							ArrayList<ArrayList<TNode>> subset = filterByDecomposabe1EditOptimalTransformations(optimalTransformations);
							
							ArrayList<ArrayList<TNode>> subset = optimalTransformations;
							int maxNbEditForDecomposability = (int) Math.floor(nbEdit/2);
							for(int nbEditForDecomposability=1; nbEditForDecomposability<=maxNbEditForDecomposability; nbEditForDecomposability++){
								//System.out.println("nbEditForDecomposability: "+nbEditForDecomposability);
								subset = filterByDecomposableEditOptimalTransformations(subset, nbEditForDecomposability);
								if(subset.size()==0)
									break;
							}
							
							if(subset.size()>0){
								set = enumerateClusterings(subset);
							}
						} 
						else {
							set = enumerateClusterings(optimalTransformations);
						}
						if(set.size()>0){
							//foundClusterings.addAll(set);
							foundClusteringsByNbEditMap.get(nbEdit).addAll(set);
						}
						
					}
				}
			}
			
		
		}
		
	}
	


	
	public ArrayList<ArrayList<TNode>> prepareSelNodesWithTargetIndexes(ArrayList<ArrayList<TNode>> selNodesList){
		ArrayList<ArrayList<TNode>> collector = new ArrayList<ArrayList<TNode>>();
		ArrayList<ArrayList<TNode>> collector_known = new ArrayList<ArrayList<TNode>>();

		/*
		 * Input: [(nodeId:7, clusterId:2, targetClusterId:-1, targetIndex:-1), (nodeId:8, clusterId:2, targetClusterId:-1, targetIndex:-1), (nodeId:14, clusterId:3, targetClusterId:2, targetIndex:-1)],
		 * 			 [(nodeId:7, clusterId:2, targetClusterId:3, targetIndex:-1), (nodeId:8, clusterId:2, targetClusterId:-1, targetIndex:-1), (nodeId:14, clusterId:3, targetClusterId:2, targetIndex:-1)]
		 * 
		 * Output: [[(nodeId:7, clusterId:2, targetClusterId:-1, targetIndex:1), (nodeId:8, clusterId:2, targetClusterId:-1, targetIndex:1), (nodeId:14, clusterId:3, targetClusterId:2, targetIndex:-1)], 
					[(nodeId:7, clusterId:2, targetClusterId:-1, targetIndex:1), (nodeId:8, clusterId:2, targetClusterId:-1, targetIndex:2), (nodeId:14, clusterId:3, targetClusterId:2, targetIndex:-1)],
 					[(nodeId:7, clusterId:2, targetClusterId:-1, targetIndex:2), (nodeId:8, clusterId:2, targetClusterId:-1, targetIndex:1), (nodeId:14, clusterId:3, targetClusterId:2, targetIndex:-1)],
 					[(nodeId:7, clusterId:2, targetClusterId:3, targetIndex:-1), (nodeId:8, clusterId:2, targetClusterId:-1, targetIndex:1), (nodeId:14, clusterId:3, targetClusterId:2, targetIndex:-1)],
		 */
		
		
		for(ArrayList<TNode> selNodes : selNodesList){
			Set<Integer> uniqueTargetClusterIds = new HashSet<>();
			ArrayList<Integer> nodeIndexesUnknown = new ArrayList<>();
			int nbUnknown = 0;
			for(int i=0; i<selNodes.size(); i++){
				if(selNodes.get(i).getTargetClusterId() == -1){
					nbUnknown++;
					nodeIndexesUnknown.add(i);
				} else {
					uniqueTargetClusterIds.add(selNodes.get(i).getTargetClusterId());
				}
			}
			ArrayList<Integer> uniqueTargetClusterIds2 = new ArrayList<>();
			uniqueTargetClusterIds2.addAll(uniqueTargetClusterIds);
			
			if(nbUnknown>0){
				
				for(int nbPossibleGroups=1; nbPossibleGroups<=nbUnknown; nbPossibleGroups++){
					List<int[]> result = UniquePartitionSize.generate(nbUnknown, nbPossibleGroups);
					
					// ========================================
					for(int[] partitionSize : result){ // let say partitionSize = [3,1] for nbPossibleGroups=2
						// nodeIndexesUnknown.stream().mapToInt(Integer::intValue).toArray()
						List<ArrayList<int[]>> indexesListByPartitionSize = helperAllPossibleTargetIndexesByComb(nbUnknown, partitionSize);
						// an ex of 'indexesListByPartitionSize': [ [[0],[1,2],[3,4]], [[0],[1,3],[2,4]] ] for nbUnknown=5 and partitionSize=[1,2,2]
						// [[0, 1]] for nbUnknown=2 and partitionSize=[2]
						

						for(ArrayList<int[]> indexesByPartitionSize : indexesListByPartitionSize ){
							ArrayList<TNode> updatedSelNodes = new ArrayList<>();
							for(TNode node : selNodes){ // deep copy
								TNode newNode = new TNode(node);
								newNode.addNotEqualToConstraints(uniqueTargetClusterIds2);
								updatedSelNodes.add(newNode);
							}
							
							
							for(int targetIndex=1; targetIndex<=partitionSize.length; targetIndex++){
								int[] indArr = indexesByPartitionSize.get(targetIndex-1);
								//if(ArrayOperations.isIdentical(indArr, ArrayOperations.rep(1,partitionSize[targetIndex-1])))
								
								for(int i : indArr){
									updatedSelNodes.get(nodeIndexesUnknown.get(i)).setTargetIndex(targetIndex);
								}
							}
							
							collector.add(updatedSelNodes);
						}
					}
					// ========================================
					
				}
				
				
			} else {
				collector_known.add(selNodes);
			}
		}
		
		collector.addAll(collector_known);
		return(collector);
	}
	
	
	
	public List<ArrayList<int[]>> helperAllPossibleTargetIndexesByComb(int nbUnknown, int[] partitionSize){
		List<ArrayList<int[]>> collector = new ArrayList<ArrayList<int[]>>();
		
		int[] targetIndexes = ArrayOperations.seq(0, nbUnknown-1);
		
		for(int nbNodeInPartition : partitionSize){ // let say partitionSize = [3,1] for nbPossibleGroups=2
			
			if(collector.size()>0){
				List<ArrayList<int[]>> collector2 = new ArrayList<ArrayList<int[]>>();

				// ex: collector: {{{1,3}},{{2,5}}}
				for(ArrayList<int[]> arrayOfExistingIndexes : collector){
					int[] existingIndexes = ArrayOperations.mergeMultipleArrays(arrayOfExistingIndexes);
					int[] remainingTargetIndexes = ArrayOperations.removeMultipleElements(targetIndexes, existingIndexes);
					List<int[]> combTargetIndexes = Combination.generate(remainingTargetIndexes, nbNodeInPartition);
					// ex: 	existingIndexes: {1,3}, combNodeIndexesToMove: {{2,4},{2,5}}
					// output: {{{1,3},{2,4}},{{1,3},{2,5}}}
					
					for(int[] inds : combTargetIndexes){
						ArrayList<int[]> newArray = new ArrayList<int[]>(arrayOfExistingIndexes);
						newArray.add(inds);
						collector2.add(newArray);
					}
					
				}
				
				collector.clear();
				collector = new ArrayList<ArrayList<int[]>>(collector2);
				
				
			} else {
				List<int[]> combIndexes = Combination.generate(targetIndexes,nbNodeInPartition); //ex: combNodeIds=[[1,2],[1,3],[2,3]]
				for(int[] indexes : combIndexes){
					ArrayList<int[]> temp = new ArrayList<>();
					temp.add(indexes);
					collector.add(temp);
				}
			}
			
			
		}
		
		
		return(collector);
	}
	
	
	public ArrayList<ArrayList<TNode>> prepareSelNodesWithTargetClusterIds(ArrayList<TNode> selNodes, ArrayList<int[]> allClusterIdsInterest){
		
		// [1, ..,  ..], [2, .., ..] ==> [1, 3,  ...], [2, 3, ..], [1, 4,  ...], [2, 4, ..]
		ArrayList<ArrayList<TNode>> collector = new ArrayList<ArrayList<TNode>>();
		for(int i=0; i<selNodes.size(); i++){ // possible target cluster ids for for each node
			TNode node = selNodes.get(i);

			int[] clusterIdsInterest = allClusterIdsInterest.get(i);
			
			
			if(collector.size()>0){
				ArrayList<ArrayList<TNode>> collectorCopy = new ArrayList<ArrayList<TNode>>();
				for(ArrayList<TNode> arr : collector){
					collectorCopy.add(new ArrayList<TNode>(arr));
				}
				collector.clear();
				
				for(int targetClusterId : clusterIdsInterest){
					ArrayList<ArrayList<TNode>> collector2 = new ArrayList<ArrayList<TNode>>();

					for(ArrayList<TNode> currNodes : collectorCopy){
						TNode newNode = new TNode(node);
						newNode.setTargetClusterId(targetClusterId);
						ArrayList<TNode> updatedArr = new ArrayList<>(currNodes);
						updatedArr.add(newNode);
						collector2.add(updatedArr);
					}
					collector.addAll(collector2);
				}
				
				
			} else {
				
				ArrayList<ArrayList<TNode>> collector2 = new ArrayList<ArrayList<TNode>>();
				for(int targetClusterId : clusterIdsInterest){
					TNode newNode = new TNode(node);
					newNode.setTargetClusterId(targetClusterId);
					ArrayList<TNode> updatedNodes = new ArrayList<>();
					updatedNodes.add(newNode);
					collector2.add(updatedNodes);
				}
				collector.addAll(collector2);
			}
		}

		
		return(collector);
	}
	
	
	
	
	

	/**
	 * It is a helper method in order to apply the pruning strategy, so-called "edge connectivity", onto a list of node subsets.
	 * 	  This method is used for incomplete networks and for d-edit operations with d>=2.
	 * 
	 * See the method 'isConnectedMovingNodes_()'
	 * 
	 * @param selNodesList
	 * @param adjMat
	 * 
	 * @return collector: a list of node subsets which satisfy the "edge connectivity" property.
	 */
	public boolean isConnectedMovingNodes(ArrayList<TNode> selNodes, double[][] adjMat){
		
		// build the subgraph where size>1: add an edge when there is a real link
		UndirectedGraph g = new UndirectedGraph(selNodes.size());
		HashMap<Integer, Integer> NewIdMap = new HashMap<>(); 
		
		// source cluster
		if(selNodes.size()>1){ // a cluster having only 1 node is by construction connected here
			for(int i=0; i<selNodes.size(); i++)
				NewIdMap.put(selNodes.get(i).nodeId, i);
			
			for(int i=0; i<(selNodes.size()-1); i++){
				TNode node = selNodes.get(i);
				for(int j=i+1; j<selNodes.size(); j++){
					TNode otherNode = selNodes.get(j);
					if(adjMat[node.getNodeId()][otherNode.getNodeId()]!=0.0){
						g.addEdge(NewIdMap.get(node.nodeId), NewIdMap.get(otherNode.nodeId));
					}
				}
			}
			
			if(!g.isSingleConnectedComponent())
				return(false);
		}
		
		return(true);
	}
	
	
	
	
	/**
	 * It applies the pruning strategy, so-called "edge connectivity", onto a list of node subsets and returns those which satisfy this property.
	 * 	  This method is used for incomplete networks and for d-edit operations with d>=2.
	 * 
	 * See Property 3 in Section 6.2 of the reference article for more details.
	 * 
	 * @param selNodesList
	 * @param adjMat
	 * 
	 * @return collector: a list of node subsets which satisfy the "edge connectivity" property.
	 */
	public boolean isConnectedMovingNodes_(ArrayList<ArrayList<TNode>> selNodesList, double[][] adjMat){
		ArrayList<TNode> selNodes = new ArrayList<>();
		for(ArrayList<TNode> selNodes2 : selNodesList){ // sel nodes are organized by source cluster
			selNodes.addAll(selNodes2);
		}
		return(isConnectedMovingNodes(selNodes, adjMat));
	}
	
	
	
	
	
	// 

	/**
	 * It partially applies the pruning strategy, so-called "MVMO", onto a list of node subsets.
	 * 	  Up to 3-edit, all links of a node with the other moving nodes in the same cluster have to be non-negative.
	 * 	  This method is used for d-edit operations with d>=2.
	 * 
	 * See Algorithm 4 in Section 5.2 & Lemmas 2 and 3 in Section 6.4 of the reference source for more details.
	 * 
	 * @param selNodesList
	 * 
	 * @return collector: a list of node subsets which satisfy the "edge connectivity" property.
	 */
	public boolean isNonNegativeInternalNeighborLinksForUpTo3Edit(ArrayList<ArrayList<TNode>> selNodesList){
		for(ArrayList<TNode> selNodes : selNodesList){ // sel nodes are organized by source cluster
			if(selNodes.size()>1){ // a cluster having only 1 node is by construction connected here
				for(int i=0; i<(selNodes.size()-1); i++){
					TNode node = selNodes.get(i);
					for(int j=i+1; j<selNodes.size(); j++){
						TNode otherNode = selNodes.get(j);
						if(adjMat[node.getNodeId()][otherNode.getNodeId()]<0){
							return(false);
						}
					}
				}
			}
		}
		return(true);
	}
		
	
	
	/**
	 * It applies the pruning strategy, so-called "fake edge connectivity", onto a list of node subsets and returns those which satisfy this property.
	 * 	  This method is used for d-edit operations with d>=4.
	 * 	  We do not need to do for 2edit or 3edit, because we implicitly do it in 'filterForUnweightedGraphBeforeTargetIndexesForExternalLinksUpTo3Edit()'
	 * 
	 * See Property 4.a in Section 6.2 of the reference article for more details.
	 * 
	 * @param selNodesList
	 * 
	 * @return collector: a list of node subsets which satisfy the "fake edge connectivity" property.
	 */
	public boolean isFakeLinkConnectivityWithInternalSourceNeighborNodes(ArrayList<ArrayList<TNode>> selNodesList){

		
		for(ArrayList<TNode> selNodes : selNodesList){ // sel nodes are organized by source cluster
			if(selNodes.size()>1){ // a cluster having only 1 node is by construction connected here
				for(TNode node : selNodes){ // for each node being in one of the source clusters
					double sum = 0.0;
					for(TNode otherNode : selNodes){
						if(node.nodeId != otherNode.nodeId){
							sum += adjMat[node.getNodeId()][otherNode.getNodeId()];
						}
					}
					if(sum == 0.0){
						double[][] adjMatCopy = java.util.Arrays.stream(adjMat).map(el -> el.clone()).toArray(double[][]::new);
						for(TNode otherNode : selNodes)
							adjMatCopy[node.getNodeId()][otherNode.getNodeId()] = 0.0;
						boolean isConn = !isConnectedMovingNodes_(selNodesList, adjMatCopy);
						if(!isConn)
							return(true);
					}
				}
			}
		}
		
		return(false);
	}
	
	
	
	
	
	/**
	 * It applies the pruning strategy, so-called "fake edge connectivity", onto a list of node subsets and returns those which satisfy this property.
	 * 	  This method is used for d-edit operations with d>=4.
	 * 	  We do not need to do for 2edit or 3edit, because we implicitly do it in 'filterForUnweightedGraphBeforeTargetIndexesForExternalLinksUpTo3Edit()'
	 * 
	 * See Property 4.b in Section 6.2 of the reference article for more details.
	 * 
	 * @param selNodesList
	 * 
	 * @return collector: a list of node subsets which satisfy the "fake edge connectivity" property.
	 */
	public ArrayList<ArrayList<TNode>> filterByFakeConnectivityWithInternalTargetNeighborNodes(ArrayList<ArrayList<TNode>> selNodesList){
		ArrayList<ArrayList<TNode>> collector = new ArrayList<ArrayList<TNode>>();
		
		for(ArrayList<TNode> selNodes : selNodesList){
			
			HashMap<Integer, ArrayList<TNode>> targetClusterIdCounterMap = new HashMap<>();
			for(TNode node : selNodes){
				// reorganize moving nodes by their target cluster
				if(node.targetClusterId != -1){
					if(!targetClusterIdCounterMap.containsKey(node.targetClusterId))
						targetClusterIdCounterMap.put(node.targetClusterId, new ArrayList<TNode>());
					targetClusterIdCounterMap.get(node.targetClusterId).add(node);
				}
				if(node.targetClusterId == -1 && node.targetIndex != -1){
					if(!targetClusterIdCounterMap.containsKey(-node.targetIndex)) // I put the sign 'minus' to distinguish from the other target clusters
						targetClusterIdCounterMap.put(-node.targetIndex, new ArrayList<TNode>());
					 targetClusterIdCounterMap.get(-node.targetIndex).add(node);
				}
			}
			
			// for instance
			// targetClusterIdCounterMap:
			// {-1=[(nodeId:22)], 4=[(nodeId:15), (nodeId:21), (nodeId:34)], 6=[(nodeId:13)]}
			// sourceClusterIdCounterMap:
			// {4=[(nodeId:13), (nodeId:22)], 5=[(nodeId:15)], 6=[(nodeId:21), (nodeId:34)]}
			
			// ========================
			
			boolean ok = true;
			for(Integer key : targetClusterIdCounterMap.keySet()){ // for each target cluster of the moving vertices
				ArrayList<TNode> nodeListSameTarget = targetClusterIdCounterMap.get(key);
				if(nodeListSameTarget.size()>1){ // make sure that there is at least one other node moving into the same target
					//int maxSubsetSize = (int) Math.floor(nodeListSameTarget.size()/2);
					int maxSubsetSize = 1; // use in this way to speed up

					for(int subsetSize=1; subsetSize<=maxSubsetSize; subsetSize++){
						int[] indexs = ArrayOperations.seq(0, nodeListSameTarget.size()-1);
						List<int[]> combinations = Combination.generate(indexs, subsetSize);
						
						for(int[] comb : combinations){ // the size of 'comb' is 'subsetSize'
							ArrayList<TNode> nodeSubListSameTarget = new ArrayList<TNode>();
							ArrayList<TNode> otherSubListSameTarget = new ArrayList<TNode>(nodeListSameTarget);
							for(int i=0; i<subsetSize; i++){
								nodeSubListSameTarget.add(nodeListSameTarget.get(comb[i]));
								otherSubListSameTarget.remove(nodeListSameTarget.get(comb[i]));
							}
							// check
							double sum = 0.0;
							for(TNode node : nodeSubListSameTarget){ // for each node
								for(TNode otherNode : otherSubListSameTarget)// for each other node
									sum += adjMat[node.getNodeId()][otherNode.getNodeId()];
							}
							if(sum == 0.0){
								double[][] adjMatCopy = java.util.Arrays.stream(adjMat).map(el -> el.clone()).toArray(double[][]::new);
								for(TNode node : nodeSubListSameTarget){ // for each node
									for(TNode otherNode : otherSubListSameTarget)// for each other node
										adjMatCopy[node.getNodeId()][otherNode.getNodeId()] = 0.0;
								}
								boolean isConn = !isConnectedMovingNodes(selNodes, adjMatCopy);
								if(!isConn){
									ok = false;
									break;
								}
							}
						}
						if(!ok)
							break;
					}
				}
				if(!ok)
					break;
			}
			if(ok)
				collector.add(selNodes);
		}
		
		return(collector);
	}
	
	
	
	
	/**
	 * It applies the pruning strategy, so-called "non-min-edit operation", onto a list of node subsets and returns those which satisfy this property.
	 * 	  This method is used for d-edit operations with d>=1.
	 * 
	 * See Property 1 in Section 6.1 of the reference article for more details.
	 * 
	 * @param selNodesList
	 * 
	 * @return collector: a list of node subsets which satisfy the "Fake edge connectivity" property.
	 */
	public ArrayList<ArrayList<TNode>> filterByFakeEditTransformation(ArrayList<ArrayList<TNode>> selNodesList,
			int[] selSourceClusterIds, boolean[] isWholeClusterBoolArr){
		ArrayList<ArrayList<TNode>> collector = new ArrayList<ArrayList<TNode>>();
		
		int[] initClusterSizes = initClustering.getClusterSizes();
		
		for(ArrayList<TNode> selNodes : selNodesList){
			boolean fakeEditTransformation = false; // if the target cluster of some moving nodes is known, then check it for them
			// scenario 1 (Property 1.a): All the elements in one of the source clusters moves into the one of the other source clusters,
			//		where all the elements of this target cluster move into another cluster.
			for(TNode node : selNodes){ // iterate the other nodes
				int index = ArrayOperations.whichIndex(selSourceClusterIds, node.targetClusterId); // source cluster index of the other node
				if(index!=-1 && isWholeClusterBoolArr[index]) {
					fakeEditTransformation = true;
					break;
				}
			}
			
			// scenario 2 (Property 1.b): this is the case where two clusters exchange some nodes. It is possible that a 3-edit operation is fake,
			//				since exchanging the other nodes in both clusters result in a 2-edit operation.
			//			So, the rule: the number of exchanging nodes between 2 clusters is greater than the number of the other nodes in the same 2 clusters,
			//							then, this is a fake edit operation.
			for(TNode node : selNodes){ // given a node, we know those 2 clusters >> source and target clusters of 'node'
				if(node.targetClusterId != -1){
					int nbIncomingNode = 0;
					for(TNode otherNode : selNodes){
						if(node.nodeId != otherNode.nodeId && node.targetClusterId == otherNode.clusterId 
								&& node.clusterId == otherNode.targetClusterId){ // find exchanging nodes
							nbIncomingNode += 1;
						}
					}
					int remainingNumberOfNodes = initClusterSizes[node.clusterId-1]-1+initClusterSizes[node.targetClusterId-1]-nbIncomingNode;
					if(remainingNumberOfNodes<(nbIncomingNode+1)){ // +1 for the 'node', which moves into its target cluster
						fakeEditTransformation = true;
						break;
					}
				}
			}
			
			if(!fakeEditTransformation) // if it is still false
				collector.add(selNodes);
		}
		
		return(collector);
	}
	
	
	
	/**
	 * It applies the pruning strategy, so-called "Interaction connectivity", onto a list of node subsets and returns those which satisfy the connectivity property.
	 * 	  This method is used for d-edit operations with d>=3.
	 * 
	 * See Property 5 in Section 6.2 of the reference article for more details.
	 * 
	 * @param selNodesList
	 * 
	 * @return collector: a list of node subsets which satisfy the "Interaction connectivity" property.
	 */
	public ArrayList<ArrayList<TNode>> filterByConnectedComponentInteractionConnectivity(ArrayList<ArrayList<TNode>> selNodesList){
		ArrayList<ArrayList<TNode>> collector = new ArrayList<ArrayList<TNode>>();
			
		for(ArrayList<TNode> selNodes : selNodesList){
			
			if(selNodes.size()>2){
				Map<Integer, Integer> ClusterIdToVertexIdMap = new HashMap<>();
				int id=0; // it will be a new vertex id in the graph 'g' (in order that ids start from 1)
				for(TNode node : selNodes){
					if(!ClusterIdToVertexIdMap.containsKey(node.getClusterId()))
						ClusterIdToVertexIdMap.put(node.getClusterId(), id++);
					if(node.getTargetClusterId() != -1 && !ClusterIdToVertexIdMap.containsKey(node.getTargetClusterId())) 
						ClusterIdToVertexIdMap.put(node.getTargetClusterId(), id++);
					else if(node.getTargetClusterId() == -1 && node.getTargetIndex() != -1 && !ClusterIdToVertexIdMap.containsKey(-node.getTargetIndex()))
						ClusterIdToVertexIdMap.put(-node.getTargetIndex(), id++); // I put minus sign to distinguish with other cluster ids
				}
				
				UndirectedGraph g = new UndirectedGraph(ClusterIdToVertexIdMap.keySet().size());
				
				for(TNode node : selNodes){ // we also handle incomplete graphs
					if(node.getTargetClusterId() == -1){
						g.addEdge(ClusterIdToVertexIdMap.get(node.getClusterId()), ClusterIdToVertexIdMap.get(-node.getTargetIndex()));
					}
					else {
						for(TNode otherNode : selNodes){
							if(node.getNodeId() != otherNode.getNodeId()){
								if(node.getTargetClusterId() == otherNode.getClusterId() && adjMat[node.getNodeId()][otherNode.getNodeId()]!=0.0){
									// we ensure that when one nodes moves into a known cluster, check the existence of link
									g.addEdge(ClusterIdToVertexIdMap.get(node.getClusterId()), ClusterIdToVertexIdMap.get(node.getTargetClusterId()));
									break;
								}
							}
						}
					}
					
				}
				if(g.isSingleConnectedComponent())
					collector.add(selNodes);
				
			} else { // no need to check for 1-Edit and 2-edit
				collector.add(selNodes);
			}
			
		}
		return(collector);
	}
	
	
	
	/**
	 * It applies the MVMO pruning strategy onto a list of node subsets and returns those which satisfy the MVMO corollary.
	 *    This method is used for edit operations starting from d-edit with d>=2.
	 * 
	 * See Corollary 1 in Section 6.3 of the reference article for more details.
	 * 
	 * @param selNodesList
	 * 
	 * @return collector: a list of node subsets which satisfy the MVMO property
	 */
	public ArrayList<ArrayList<TNode>> filterByMVMOCorollary(
			ArrayList<ArrayList<TNode>> selNodesList)
	{
		ArrayList<ArrayList<TNode>> collector = new ArrayList<ArrayList<TNode>>();

		for(ArrayList<TNode> selNodes : selNodesList){
			
			// 1) compute the base values for lhs (left hand side) and rhs (right hand side)

			int[] lhs = new int[selNodes.size()];
			int[] rhs = new int[selNodes.size()];

			int indx = 0;
			for(TNode node : selNodes){
				lhs[indx] = 0;
				rhs[indx] = 0;
				for(TNode otherNode : selNodes){
					if(node.getNodeId() != otherNode.getNodeId()){
						boolean afterSameCluster1 = (node.getTargetClusterId()!=-1 && node.getTargetClusterId()==otherNode.getTargetClusterId());
						boolean afterSameCluster2 = (node.getTargetClusterId()==-1 && node.getTargetIndex()==otherNode.getTargetIndex());
						boolean afterSameCluster = (afterSameCluster1 || afterSameCluster2);
						
						if(node.getClusterId() == otherNode.getClusterId())
							lhs[indx] += adjMat[node.getNodeId()][otherNode.getNodeId()];
						if(afterSameCluster)
							rhs[indx] -= adjMat[node.getNodeId()][otherNode.getNodeId()];
						if(node.getTargetClusterId() == otherNode.getClusterId())
							lhs[indx] -= adjMat[node.getNodeId()][otherNode.getNodeId()];
						if(node.getClusterId() == otherNode.getTargetClusterId())
							rhs[indx] += adjMat[node.getNodeId()][otherNode.getNodeId()];
					}
				}
				indx++;
			}
			
			// 2) perform the combination & compute final rhs and lhs values & check if it is ok
			boolean ok = true;
			
			// int maxNbEditForDecomposability = (int) Math.floor(selNodes.size()/2);
			int maxNbEditForDecomposability = 1; // use this to speed up
			for(int nbEditForDecomposability=1; nbEditForDecomposability<=maxNbEditForDecomposability; nbEditForDecomposability++){
				int[] indexs = ArrayOperations.seq(0, selNodes.size()-1);
				List<int[]> combinations = Combination.generate(indexs, nbEditForDecomposability);
				
				for(int[] comb : combinations){
					int[] lhsCopy = Arrays.copyOf(lhs, lhs.length);
					int[] rhsCopy = Arrays.copyOf(rhs, rhs.length);

					if(nbEditForDecomposability>1){
						for(int i : comb){ // for each node index in 'comb'
							TNode node = selNodes.get(i);
							for(int j : comb){
								if(i!=j){
									TNode otherNode = selNodes.get(i);
									boolean afterSameCluster1 = (node.getTargetClusterId()!=-1 && node.getTargetClusterId()==otherNode.getTargetClusterId());
									boolean afterSameCluster2 = (node.getTargetClusterId()==-1 && node.getTargetIndex()==otherNode.getTargetIndex());
									boolean afterSameCluster = (afterSameCluster1 || afterSameCluster2);
									
									if(node.getClusterId() == otherNode.getClusterId())
										lhsCopy[i] -= adjMat[node.getNodeId()][otherNode.getNodeId()]; // we use the opposite sign to remove the effect of the 'otherNode'
									else if(afterSameCluster)
										rhsCopy[i] += adjMat[node.getNodeId()][otherNode.getNodeId()]; // we use the opposite sign
									else if(node.getTargetClusterId() == otherNode.getClusterId())
										lhsCopy[i] += adjMat[node.getNodeId()][otherNode.getNodeId()]; // we use the opposite sign
									else if(node.getClusterId() == otherNode.getTargetClusterId())
										rhsCopy[i] -= adjMat[node.getNodeId()][otherNode.getNodeId()]; // we use the opposite sign
								}
							}
						}
					}
					
					int lhsFinalValue = 0;
					int rhsFinalValue = 0;
					for(int i : comb){ // for each node index in 'comb'
						lhsFinalValue += lhsCopy[i];
						rhsFinalValue += rhsCopy[i];
					}
					
					// the inequality is in this form: lhs > delta > rhs
					// Since delta can have only integer values, this is possible when the difference between lhs and rhs is at least 2
					if(lhsFinalValue>rhsFinalValue && (lhsFinalValue-rhsFinalValue)>1){
						ok = true;
					} else {
						ok = false;
						break;
					}
					
				}
				if(!ok)
					break;
			}
			
			if(ok) // if it is still true
				collector.add(selNodes);
			
		}
		
		return(collector);
	}
	
	
	
	
	
	/**
	 * 	It applies the MVMO pruning strategy onto a list of node subsets and returns those which satisfy the MVMO property.
	 *    This method is used for edit operations starting from d-edit with d>=4.
	 *    
	 *    See Algorithm 5 in Section 5.2 & Lemma 4 in Section 6.4 of the reference article for more details.
	 *  
	 *  We are interested in the case where the moving nodes are not in the same cluster. Then, we check if they will move into
	 *  the same or different cluster.
	 *  
	 *  We also handle incomplete graphs, so a pair of nodes may have no link.
	 * 
	 * 
	 * @param selNodesList
	 * @param afterTargetIndexes
	 * 
	 * @return collector: a list of node subsets which satisfy the MVMO property
	 */
	// 
	public ArrayList<ArrayList<TNode>> filterForUnweightedGraphBeforeTargetIndexesForExternalLinks(
			ArrayList<ArrayList<TNode>> selNodesList, boolean afterTargetIndexes)
	{
		ArrayList<ArrayList<TNode>> collector = new ArrayList<ArrayList<TNode>>();

		for(ArrayList<TNode> selNodes : selNodesList){

			int[] nbConnections = new int[selNodes.size()];
			int[] nbIntersectionConnections = new int[selNodes.size()];
			int indx = 0;
			for(TNode node : selNodes){
				int nb = 0;
				int nbIntersections = 0;
				
				for(TNode otherNode : selNodes){
					if(node.getNodeId() != otherNode.getNodeId()){
						
						boolean afterSameCluster1 = (node.getTargetClusterId()!=-1 && node.getTargetClusterId()==otherNode.getTargetClusterId());
						boolean afterSameCluster2 = (node.getTargetClusterId()==-1 && node.getTargetIndex()>0 && 
								node.getTargetIndex()==otherNode.getTargetIndex());
						boolean afterPossiblySameCluster = (node.getTargetClusterId()==-1 && node.getTargetIndex()<0 && 
								node.getTargetClusterId()==otherNode.getTargetClusterId());
						boolean afterSameCluster = (afterSameCluster1 || afterSameCluster2);
						
						if(node.getClusterId() == otherNode.getClusterId())
							nb++;
						else if(afterSameCluster)
							nb++;
						else if(node.getTargetClusterId() == otherNode.getClusterId())
							nb++;
						else if(node.getClusterId() == otherNode.getTargetClusterId())
							nb++;
						else if(!afterTargetIndexes && afterPossiblySameCluster)
							nb++; // we add this, because just in case they might move into the same target cluster
						
						
						if(node.getClusterId() == otherNode.getClusterId() && afterSameCluster) // when afterTargetIndexes = true
							nbIntersections++;
						else if(node.getClusterId() == otherNode.getClusterId() && !afterTargetIndexes && afterPossiblySameCluster)
							nbIntersections++;
						else if(node.getClusterId() == otherNode.getTargetClusterId() && node.getTargetClusterId() == otherNode.getClusterId())
							nbIntersections++;
					}
				}
				nbConnections[indx] = nb;
				nbIntersectionConnections[indx] = nbIntersections;
				indx++;
			}
			
			boolean ok = true;
			indx=-1;
			for(TNode node : selNodes){ // for each pair of nodes interacting between them
				indx++;
				if(!ok)
					break;
				
				// we require the edge weight between u and v to be positive or negative (cannot be empty link)
				if((nbConnections[indx]==2 && nbIntersectionConnections[indx]==0) ||
						(nbConnections[indx]==2 && nbIntersectionConnections[indx]==1) ||
						(nbConnections[indx]==1 && nbIntersectionConnections[indx]==1) )
				{
					// for external connections
					for(TNode otherNode : selNodes){
						if(ok && node.getNodeId() != otherNode.getNodeId()){ 
							// ensure that 2 different nodes and they are not in the same cluster
							
							boolean afterSameCluster1 = (node.getTargetClusterId()!=-1 && node.getTargetClusterId()==otherNode.getTargetClusterId());
							boolean afterSameCluster2 = (node.getTargetClusterId()==-1 && node.getTargetIndex()>0 && 
									node.getTargetIndex()==otherNode.getTargetIndex());
							boolean afterSameCluster = (afterSameCluster1 || afterSameCluster2);
							
							if(node.getClusterId() != otherNode.getClusterId()){
								// for external connections
								if(afterSameCluster){
									if(adjMat[node.getNodeId()][otherNode.getNodeId()]<=0) // we want positive weight
										ok = false;
								} 
								else if(node.getTargetClusterId() != otherNode.getTargetClusterId()
										&& (node.getTargetClusterId() == otherNode.clusterId || node.clusterId == otherNode.getTargetClusterId())){
									// target cluster ids are diff, but also 'node' moves into the other's cluster, i.e. interaction
									if(adjMat[node.getNodeId()][otherNode.getNodeId()]>=0) // we want negative weight
										ok = false;
								}
							}
							else {
								// for internal connections
								if(adjMat[node.getNodeId()][otherNode.getNodeId()]<=0) // we want positive weight
									ok = false;
							}
						}
					}
					
				}
				// we require the edge weight between u and v to be positive/empty or negative/empty
				else if((nbConnections[indx]==3 && nbIntersectionConnections[indx]==0) ||
						(nbConnections[indx]==2 && nbIntersectionConnections[indx]==2) )
				{
					// for external connections
					for(TNode otherNode : selNodes){
						if(ok && node.getNodeId() != otherNode.getNodeId()){ 
							// ensure that 2 different nodes and they are not in the same cluster
							
							boolean afterSameCluster1 = (node.getTargetClusterId()!=-1 && node.getTargetClusterId()==otherNode.getTargetClusterId());
							boolean afterSameCluster2 = (node.getTargetClusterId()==-1 && node.getTargetIndex()>0 && 
									node.getTargetIndex()==otherNode.getTargetIndex());
							boolean afterSameCluster = (afterSameCluster1 || afterSameCluster2);
							
							if(node.getClusterId() != otherNode.getClusterId()){ // they are not in the same source cluster
								// for external connections
								if(afterSameCluster){
									if(adjMat[node.getNodeId()][otherNode.getNodeId()]<0) // we do not want negative weight
										ok = false;
								} 
								else if(node.getTargetClusterId() != otherNode.getTargetClusterId()
										&& (node.getTargetClusterId() == otherNode.clusterId || node.clusterId == otherNode.getTargetClusterId())){
									// target cluster ids are diff, but also 'node' moves into the other's cluster, i.e. interaction
									if(adjMat[node.getNodeId()][otherNode.getNodeId()]>0) // we do not want positive weight
										ok = false;
								}
							}
							else { // they are in the same source cluster
								// for internal connections
								if(adjMat[node.getNodeId()][otherNode.getNodeId()]<0) // we do not want negative weight
									ok = false;
							}
						}
					}
					
				}

			}
			if(ok) // if it is still true
				collector.add(selNodes);
			
		}
		
		return(collector);
	}
	
	
	
	
	
	/**
	 *  It applies the MVMO pruning strategy onto a list of node subsets and returns those which satisfy the MVMO property.
	 *    This method is used for edit operations starting from d-edit with d<=3.
	 *    
	 *    See Algorithm 5 in Section 5.2 & Lemmas 2 and 3 in Section 6.4 of the reference article for more details.
	 *    
	 *  We are interested in the case where the moving nodes are not in the same cluster. Then, we check if they will move into
	 *  the same or different cluster.
	 *  For up to 3-edit operations, this is efficient filtering. From 4-edit, this becomes less efficient, since
	 *  the number of cases where the pair of moving nodes has known target cluster id decreases.
	 *  
	 *  We also handle incomplete graphs, so a pair of nodes may have no link.
	 *  
	 * @param selNodesList
	 * @param hasTwoSourceClusters
	 * 
	 * @return collector: a list of node subsets which satisfy the MVMO property
	 */
	public ArrayList<ArrayList<TNode>> filterForUnweightedGraphBeforeTargetIndexesForExternalLinksUpTo3Edit(
			ArrayList<ArrayList<TNode>> selNodesList, boolean hasTwoSourceClusters){
		ArrayList<ArrayList<TNode>> collector = new ArrayList<ArrayList<TNode>>();
		for(ArrayList<TNode> selNodes : selNodesList){
			boolean ok = true;
			boolean isExceptionalCaseFor3Edit = false;
			int nbEdit = selNodes.size();
			

			// =======
			// this exceptional case occurs because we want to perform the filtering before determining target indexes 
			//	in order to gain time
			
			if(nbEdit == 3 && hasTwoSourceClusters){
				ArrayList<TNode> selNodes2 = new ArrayList<TNode>();
				// two nodes being in the same cluster, one node moves into third node's cluster and the other moves into unknown cluster
				// the third node moves into unknown cluster (i.e. total nb unknown target cluster = 2)
				
				if(selNodes.get(0).clusterId == selNodes.get(1).clusterId && selNodes.get(0).clusterId != selNodes.get(2).clusterId) {
					// nodes 0 and 1 are in the same cluster: put them as the first two items
					selNodes2.add(selNodes.get(0));selNodes2.add(selNodes.get(1));selNodes2.add(selNodes.get(2));
				} 
				else if(selNodes.get(0).clusterId == selNodes.get(2).clusterId && selNodes.get(0).clusterId != selNodes.get(1).clusterId) {
					// nodes 0 and 2 are in the same cluster: put them as the first two items
					selNodes2.add(selNodes.get(0));selNodes2.add(selNodes.get(2));selNodes2.add(selNodes.get(1));
				}
				else if(selNodes.get(1).clusterId == selNodes.get(2).clusterId && selNodes.get(0).clusterId != selNodes.get(1).clusterId) {
					// nodes 1 and 2 are in the same cluster: put them as the first two items
					selNodes2.add(selNodes.get(1));selNodes2.add(selNodes.get(1));selNodes2.add(selNodes.get(0));
				}
				
				// ====
				// nodes being at index 0 and 1 are in the same cluster, but the last is in a different cluster
				if(selNodes2.get(0).targetClusterId==selNodes2.get(2).clusterId 
						&& selNodes2.get(1).targetClusterId==selNodes2.get(2).clusterId
						&& selNodes2.get(2).targetClusterId==selNodes2.get(0).clusterId)
				{
					isExceptionalCaseFor3Edit = true;
					if(adjMat[selNodes2.get(0).getNodeId()][selNodes2.get(2).getNodeId()]>0) // we do not want positive weight outside a cluster
						ok = false;
					if(adjMat[selNodes2.get(1).getNodeId()][selNodes2.get(2).getNodeId()]>0) // we do not want positive weight outside a cluster
						ok = false;
				}
			} 
			
			// =======
			
			if(!isExceptionalCaseFor3Edit){
				// we also handle incomplete graphs, so a pair of nodes may have no link. 
				// But, note that we check before this method that the moving nodes are connected internally and externally between them (i.e. connected component)
				for(TNode node : selNodes){ // for each pair of nodes interacting between them
					if(!ok)
						break;
					
					for(TNode otherNode : selNodes){
						if(ok && node.getNodeId() != otherNode.getNodeId() && node.getClusterId() != otherNode.getClusterId()){ 
							// ensure that 2 different nodes and they are not in the same cluster
							
							if(node.getTargetClusterId() == otherNode.getTargetClusterId()){
								if(adjMat[node.getNodeId()][otherNode.getNodeId()]<=0) // we do not want negative weight
									ok = false;
								
							} else if(node.getTargetClusterId() != otherNode.getTargetClusterId()
									&& (node.getTargetClusterId() == otherNode.clusterId || otherNode.getTargetClusterId() == node.clusterId)){
								// target cluster ids are diff, but also one of them (or both) moves into the other's cluster, i.e. interaction
								if(adjMat[node.getNodeId()][otherNode.getNodeId()]>=0) // we do not want positive weight
									ok = false;
							}
							
						}
					}
				}
			}
			
			if(ok) // if it is still true
				collector.add(selNodes);
		}
		
		return(collector);
	}
	


	
	/**
	 * 
	 * Used for d-edit operations with d>1
	 * 
	 * See Property 6 in Section 6.3 of the reference article for more details.
	 * 
	 * @param selNodes
	 * @param withKnownTargetClusters
	 * @return
	 */
	public boolean isEligibleTransformation(ArrayList<TNode> selNodes, boolean withKnownTargetClusters){
		//ArrayList<ArrayList<Integer>> clusters = initClustering.getClustersInArrayFormat();

		/*
		 * Input: [(nodeId:7, clusterId:2, targetClusterId:-1, targetIndex:1), (nodeId:8, clusterId:2, targetClusterId:-1, targetIndex:1), (nodeId:14, clusterId:3, targetClusterId:2, targetIndex:-1)]
		 */
			
		for(TNode node : selNodes){
			double deltaNode = 0;
			if(withKnownTargetClusters)
				deltaNode = node.calculateDeltaFitness(); // note that curr and target fitness should be calculated before this
			
			// let suppose that the reference node v_r is in cluster C1 and moves into C3
			double weightsAfterSame = 0.0; // the link weights that v_r will share later in its cluster with other nodes (in its next cluster)
			double weightsNowSame = 0.0; // the link weights that v_r shares currently and later in its cluster with other nodes
			double weightsNowNextTarget = 0.0; // the link weights that v_r has with the nodes located currently in C3 but then those nodes leave their cluster
			double NowDiffAfterCurr = 0.0; //the link weights that v_r has with the nodes located currently in different cluster,ode nodes move into C1
			
			for(TNode otherNode : selNodes){
				if(node.getNodeId() != otherNode.getNodeId()){
					
					double w = adjMat[node.getNodeId()][otherNode.getNodeId()];
					if(w != 0.0){
					
						boolean nowSameCluster = (node.getClusterId() == otherNode.getClusterId());
						boolean afterSameCluster1 = (node.getTargetClusterId() == otherNode.getTargetClusterId());
						boolean afterSameCluster2 = (node.getTargetIndex() == otherNode.getTargetIndex());
						boolean afterSameCluster = (afterSameCluster1 && afterSameCluster2);
						
						if(nowSameCluster){
							weightsNowSame += w;
						} if(afterSameCluster){
							weightsAfterSame += w;
						} if(node.getTargetClusterId() == otherNode.getClusterId()){
							weightsNowNextTarget += w;
						} if(node.getClusterId() == otherNode.getTargetClusterId()){
							NowDiffAfterCurr += w;
						}
					}
				}
			}
			
			if(!withKnownTargetClusters){
				// Equation gamma^{right}_(u} - gamma^{left}_(u} > 0 in Property 6 in Section 6.3 of the referance article.
				// calculate the inequalities
				double tot = weightsNowSame+weightsAfterSame-weightsNowNextTarget-NowDiffAfterCurr;
				//if(tot<0){ // OLD
				if(tot<=0){ // NEW
					return(false);
				}
			} else {
				// Equation gamma^{right}_(u} in Property 6 in Section 6.3 of the referance article.
				if(deltaNode<(-weightsAfterSame+NowDiffAfterCurr)) // (-weightsAfterSame+NowDiffAfterCurr)>deltaNode
					return(false);

			}
		}
				
		return(true);
	}
	
	
	
	
	/**
	 *  It applies the so-called "Atomic edit operation" pruning strategy onto a list of node subsets and returns those which satisfy this property.
	 *    This method is used for edit operations starting from d-edit with d>=2. 
	 * 
	 * See Property 2 in Section 6.2 of the reference article for more details. 
	 * 
	 * Note that 'nbEditForDecomposability' should not exceed 'ceiling(nbEdit)/2'.
	 * 
	 * @param optimalTransformations
	 * @param nbEditForDecomposability
	 * @return subset: a list of node subsets which satisfy the "Atomic edit operation" property

	 */
	public ArrayList<ArrayList<TNode>> filterByDecomposableEditOptimalTransformations(ArrayList<ArrayList<TNode>> optimalTransformations,
			int nbEditForDecomposability){
		ArrayList<ArrayList<TNode>> subset = new ArrayList<ArrayList<TNode>>();
		

		for(ArrayList<TNode> selNodes : optimalTransformations){
			int[] indexs = ArrayOperations.seq(0, selNodes.size()-1);
			List<int[]> combinations = Combination.generate(indexs, nbEditForDecomposability);
			
			
			boolean nonDecomposable = true;
			for(int[] comb : combinations){
				ArrayList<TNode> subSelNodes = new ArrayList<TNode>();
				ArrayList<TNode> otherSelNodes = new ArrayList<TNode>(selNodes);
				for(int i=0; i<nbEditForDecomposability; i++){
					TNode node = selNodes.get(comb[i]);
					TNode newNode = new TNode(node);
					subSelNodes.add(newNode);
					otherSelNodes.remove(node);
				}
				
				// there are 2 cases to be checked:
				// common part: take into account the links between 'subSelNodes', which will change the overall imbalance
				double weightSum = 0.0;
				if(subSelNodes.size()>1){
					for(int i=0; i<subSelNodes.size()-1; i++){
						for(int j=i+1; j<subSelNodes.size(); j++){
							TNode node1 = subSelNodes.get(i);
							TNode node2 = subSelNodes.get(j);
							if(node1.getClusterId()==node2.getClusterId() && node1.getTargetClusterId()!=node2.getTargetClusterId())
								weightSum -= adjMat[node1.getNodeId()][node2.getNodeId()];
							else if(node1.getClusterId()!=node2.getClusterId() && node1.getTargetClusterId()==node2.getTargetClusterId())
								weightSum += adjMat[node1.getNodeId()][node2.getNodeId()];
						}
					}
				}
				
				
				// Case 1) 'subSelNodes' are moved to their target clusters (BEFORE the moves of the other moving nodes)
				double delta = 0.0;
				for(TNode node : subSelNodes){
					node.computeCurrNodeFitness(g, adjMat);
					node.computeTargetNodeFitness(g, adjMat);
					delta += node.calculateDeltaFitness();
				}
				delta += weightSum;
				if(delta == 0.0){ // if the delta of any of them is zero, this means that it is a decomposable Edit transformation
					nonDecomposable = false;
					break;
				}
					
				
				
				// Case 2) 'subSelNodes' are moved to their target clusters (AFTER the moves of the other moving nodes)
				// 2.1) update target fitness values based on the moving nodes
				if(subSelNodes.size()>1){
					for(int i=0; i<subSelNodes.size()-1; i++){
						for(int j=i+1; j<subSelNodes.size(); j++){
							TNode node1 = subSelNodes.get(i);
							TNode node2 = subSelNodes.get(j);
							double w = adjMat[node1.getNodeId()][node2.getNodeId()];

							if(node1.getClusterId()!=node2.getClusterId() && node1.getTargetClusterId()==node2.getClusterId())
								node1.substractFromTargetNodeFitness(w);
							
							if(node1.getClusterId()!=node2.getClusterId() && node2.getTargetClusterId()==node1.getClusterId())
								node2.substractFromTargetNodeFitness(w);
							
							if(node1.getClusterId()==node2.getClusterId()){
								node1.substractFromCurrNodeFitness(w);
								node2.substractFromCurrNodeFitness(w);
							}
						}
					}
				}
				
				// 2.2) update curr and target fitness values of the subSelNodes, after the moves of the other moving nodes are completed 
				for(TNode node : subSelNodes){
					// node.computeCurrNodeFitness(g, adjMat); // no need, since it is calculated in case 1
					// node.computeTargetNodeFitness(g, adjMat); // no need, since it is calculated in case 1
					for(TNode otherNode : otherSelNodes){
						double w = adjMat[node.getNodeId()][otherNode.getNodeId()];
								
						// update curr fitness
						if(node.getClusterId() == otherNode.getTargetClusterId()){
							// the operation is an addition here: substraction of a negated value
							node.substractFromCurrNodeFitness(-w);
						}
						if(node.getClusterId() == otherNode.getClusterId()){
							node.substractFromCurrNodeFitness(w);
						}
						
						// update target fitness
						if(node.getTargetClusterId() == otherNode.getClusterId()){
							node.substractFromTargetNodeFitness(w);
						}
						if(node.getTargetClusterId() == otherNode.getTargetClusterId()){
							// the operation is an addition here: substraction of a negated value
							node.substractFromTargetNodeFitness(-w);
						}
					}
				}
				// 2.3) repeat the case 2
				double delta2 = 0.0;
				for(TNode node : subSelNodes){
					// node's curr and target fitness values are changed in 2.1)
					delta2 += node.calculateDeltaFitness();
				}
				delta2 += weightSum;
				if(delta2 == 0.0){ // if the delta of any of them is zero, this means that it is a decomposable 1-Edit transformation
					nonDecomposable = false;
					break;
				}
			}		
			if(nonDecomposable)
				subset.add(selNodes);
			
		}
		
		return(subset);
	}
	
	
	
	/**
	 * It is a generic method to find candidate transformations in the aim of finding alternative optimal clustering.
	 *   Note that the target clusters of a subset of nodes might be known or unknown. Nevertheless, for those nodes whose the target cluster is unknown, 
	 *   the corresponding target cluster indexes are still known, though. 
	 *   This method corresponds to Lines 24--29 and 33--35 in Algorithm 3 of Section 5 in the reference article.
	 *   In Lines 24--29, we assign target cluster ids to those which have not and then check if this new partition is optimal.
	 *   		By the way, it is not shown in the article, but we also quickly apply the so-called "non-min-edit operation" pruning strategy  (property 1.a).
	 *   In Lines 33--35,  we directly check if this new partition is optimal.
	 * 
	 * 	1) adjust fitness values
	 * 	2) retrieve possible cluster ids
	 * 	3) generate combinations of cluster ids of size 'nbTargetClusters'
	 * 	4) for each combination, check if the global objective function remains unchanged
	 * 
	 * Note that we make use of the attributes of 'TNode'.
	 * For instance, if the attribute 'clusterId' is not assigned to any value, we look for a candidate cluster if in this method.
	 * 
	 * Params:
	 * 	ArrayList<TNode> selNodes: 
	 * int nbTargetClusters: desired number of clusters to be found for transformation. 
	 * 							Those clusters will be selected from the list of possible cluster ids
	 * 
	 */
	public ArrayList<ArrayList<TNode>> findOptimalTransformations(ArrayList<TNode> selNodes,
																		int[] selSourceClusterIds,
																		boolean[] isWholeClusterBoolArr,
																		ArrayList<ArrayList<Integer>> initClusters){
		ArrayList<ArrayList<TNode>> optimalTransformations = new ArrayList<ArrayList<TNode>>();
		// isWholeClusterBoolArr is an array of size "nb source cluster",
		//		and tells us that if the cluster of selected moving nodes will be empty after the transformation 
		
		/*
		 * input: [(nodeId:7, clusterId:2, targetClusterId:-1, targetIndex:1), (nodeId:9, clusterId:2, targetClusterId:-1, targetIndex:1),
		 * 			 (nodeId:14, clusterId:3, targetClusterId:-1, targetIndex:2)]
		 * 
		 */


		// 1) we know which node will be together in their target clusters.
		//	Based on this, compute the change of fitness caused by the links between moving nodes
		// double change = -1; // => eger daha iyi fonc obj value'su olan solution'a gecmek istersen, eksili deger koy
		double change = 0; // change (not in imbalance, opposite, in terms of balance)
		for(int i=1; i<selNodes.size(); i++){
			TNode node = selNodes.get(i);
			
			
			for(int j=0; j<i; j++){
				TNode otherNode = selNodes.get(j);
				
				double w = adjMat[node.getNodeId()][otherNode.getNodeId()];
				if(w != 0.0){
					String nodeTargetComparator = "";
					String otherNodeTargetComparator = "";
					if(node.getTargetClusterId()!=-1 && otherNode.getTargetClusterId()!=-1){
						nodeTargetComparator = String.valueOf(node.getTargetClusterId());
						otherNodeTargetComparator = String.valueOf(otherNode.getTargetClusterId());
					} else if(node.getTargetClusterId()!=-1 && otherNode.getTargetClusterId()==-1){
						nodeTargetComparator = String.valueOf(node.getTargetClusterId());
						otherNodeTargetComparator = String.valueOf("I" + otherNode.getTargetIndex());
					} else if(node.getTargetClusterId()==-1 && otherNode.getTargetClusterId()!=-1){
						nodeTargetComparator = String.valueOf("I" + node.getTargetIndex());
						otherNodeTargetComparator = String.valueOf(otherNode.getTargetClusterId());
					} else if(node.getTargetClusterId()==-1 && otherNode.getTargetClusterId()==-1){
						nodeTargetComparator = String.valueOf("I" + node.getTargetIndex());
						otherNodeTargetComparator = String.valueOf("I" + otherNode.getTargetIndex());
					}
					
					
					double w_abs = w;
					if(w<0)
						w_abs = -w;
					
					if(node.getClusterId() == otherNode.getClusterId() && !nodeTargetComparator.equals(otherNodeTargetComparator)){
						if(w<0) // now intra-negative link, but later inter-negative link
							change += w_abs; // system gain, so imbalance decreases
						else if(w>0) // now intra-positive link, but later inter-positive link
							change -= w_abs; // system loss
					}
					
					if(node.getClusterId() != otherNode.getClusterId() && nodeTargetComparator.equals(otherNodeTargetComparator)){
						if(w<0) // now inter-negative link, but later intra-negative link
							change -= w_abs; // system loss
						else if(w>0) // now inter-positive link, but later intra-positive link
							change += w_abs; // system gain
					} 
					
				}
			}
		}


		
		// 2) adjust fitness values of the selected nodes in their current clusters
			// if there are some nodes sharing the same cluster, we adjust current fitness
			// we need also to adjust target fitness, if we know where to put some nodes
		for(TNode node : selNodes){

			
			node.computeCurrNodeFitness(g, adjMat); // calculate fitness value
			if(node.getTargetClusterId()!=-1)
				node.computeTargetNodeFitness(g, adjMat); // calculate fitness value, if target cluster is known
			
			for(TNode otherNode : selNodes){
				if(node.getNodeId() != otherNode.getNodeId()){
					if(node.getClusterId() == otherNode.getClusterId()){
						node.substractFromCurrNodeFitness(adjMat[node.getNodeId()][otherNode.getNodeId()]);
					}
					
					// for those from which target cluster id is known
					if(node.getTargetClusterId()!=-1 && node.getTargetClusterId() == otherNode.getClusterId()){
						node.substractFromTargetNodeFitness(adjMat[node.getNodeId()][otherNode.getNodeId()]);
					}
				}
			}
		}
		
		
		// 3) we need to find possible cluster ids for nodes whose the 'targetClusterId' is not assigned yet
		Predicate<TNode> existsTargetClusterId = p -> p.getTargetClusterId() > 0;
		ArrayList<TNode> subsetNodes = new ArrayList<>(selNodes);
		subsetNodes.removeIf(existsTargetClusterId); // we keep only the nodes which do not have any target cluster id
		int nbTargetClustersToFind = 0;
		if(subsetNodes.size()>0){
			// retrieve all targetIndex values into a list
			Set<Integer> uniqueTargetIndexes = new TreeSet<>( subsetNodes.stream().map(p -> p.getTargetIndex()).collect(Collectors.toList()) );
			nbTargetClustersToFind = uniqueTargetIndexes.size();
		}

		
		if(nbTargetClustersToFind > 0){
		
			// ===========================================================================
			// 3.1) retrieve possible cluster ids
			
			// -----
			int newEmptyClusterId = initClustering.getNbCluster()+1;
			// +n for new clusters (normally it is sufficient to add k new clusters, where k is the number of moving vertices)
			// UPDATE: we take into account multiple  empty clusters starting from 'newEmptyClusterId' >> line 2535: if(tid < newEmptyClusterId){
			// -----
			
			
			ArrayList<List<Integer>> remainingClusterIdsList = new ArrayList<List<Integer>>();
			for(TNode node : subsetNodes){
				if(node.notEqualToConstraints.size()>0 && node.getTargetClusterId() == -1){
					List<Integer> remainingClusterIds = new ArrayList<>();
					for(int cid=1; cid<=(initClustering.getNbCluster()+nbTargetClustersToFind); cid++) // +nbTargetClustersToFind for new clusters
						remainingClusterIds.add(cid);
					
					// chaining all predicates of a given node, if there are many
					Predicate<Integer> compositePredicate = node.notEqualToConstraints.stream().reduce(w -> true, Predicate::and); 
					remainingClusterIds = remainingClusterIds.stream()   // convert list to stream
			                .filter(compositePredicate)
			                .collect(Collectors.toList());
					
					remainingClusterIdsList.add(remainingClusterIds);
				}
			} // note that remainingClusterIds are common for all those subset nodes
			
			
			List<ArrayList<Integer>> permCombs = Combination2.generate(remainingClusterIdsList);
			
			// ===========================================================================

			
			// 4) for each combination, check if the global objective function remains unchanged, i.e. it is an optimal clustering ?
			//for (int[] combination : permCombs) {
			for (ArrayList<Integer> combination : permCombs) {			
				
				ArrayList<TNode> updatedNodes = new ArrayList<>();
				// update temporarily target cluster id, and then calculate the fitness of target cluster for this node
				double delta = change; // change comes from the step 1
				boolean ok = true;
				for(TNode node : selNodes){
					TNode newNode = new TNode(node);
					int nodeClusterId = newNode.getClusterId();
					
					boolean fakeEditTransformation = false; 
					if(newNode.getTargetClusterId() == -1){ // for the moving nodes where the target cluster id was unknown
						int nodeSourceClusterIndex = ArrayOperations.whichIndex(selSourceClusterIds, nodeClusterId);
						int newTargetClusterId = combination.get(newNode.getTargetIndex()-1);
						//there are two possibilities for the so-called "non-min-edit operation" (property 1.a)
						// 1) all the elements in one of the source clusters moves into a new (so, empty) cluster
						
						if(isWholeClusterBoolArr[nodeSourceClusterIndex]){
							if(newTargetClusterId == newEmptyClusterId){
								fakeEditTransformation = true;
							}
						}
						
						if(!fakeEditTransformation){
							newNode.setTargetClusterId(newTargetClusterId); // target indexes start from 1

							newNode.setTargetIndex(-1); // since we just assigned a target cluster id
							newNode.computeTargetNodeFitness(g, adjMat);
							
							if(newNode.getClusterId() == newNode.getTargetClusterId())
								newNode.setTargetIndex(-1);
						} else {
							ok = false;
							break;
						}
					} 
						
					delta += newNode.calculateDeltaFitness();
					updatedNodes.add(newNode);
				}

                // ========================================
				// TODO: we have already another method doing this in a batch for multiple candidates: "filterByFakeEditTransformation()"
				//		It would be good to create a method which is used in both methods. But consider the performance aspect as well.
				//
				// 2nd possibility for the so-called "non-min-edit operation" (property 1.a)
				// Applying Property 1.a in Section 6.1 of the reference article: the strategy so-called "non-min-edit operation"
				// the second fake scenario is that the whole source cluster moves into
				// an existing cluster whose the size is less than the source size
				if(updatedNodes.size()>1){
					boolean isSingleSourceCluster = true;
					boolean isSingleTargetCluster = true; // we could also use 'isWholeClusterBoolArr'
					int tid = updatedNodes.get(0).getTargetClusterId();
					int sid = updatedNodes.get(0).getClusterId();
					//if(tid != newEmptyClusterId){ // OLD
					if(tid < newEmptyClusterId){ // NEW: when taking into account for multiple empty cluster ids
						for(TNode node : updatedNodes){
							if(tid != node.getTargetClusterId()){
								isSingleTargetCluster = false;
								break;
							}
							if(sid != node.getClusterId()){
								isSingleSourceCluster = false;
								break;
							}	
						}
						if(isSingleSourceCluster && isSingleTargetCluster && initClusters.get(sid-1).size()==updatedNodes.size()){
							if(initClusters.get(sid-1).size()>initClusters.get(tid-1).size())
								ok = false;
						}
					}
				}
				// ========================================

				
				boolean isEligible = true;
				if(updatedNodes.size()>1)
					isEligible = isEligibleTransformation(updatedNodes, true);
				if(isEligible && ok && delta == 0.0){
					optimalTransformations.add(updatedNodes);
				}
				
			}
			
		} else { // if(nbTargetClusters == 0){ ==> i.e. we know where to move all selected nodes
			
			double delta = change; // change comes from the step 1
			for(TNode node : selNodes)
				delta += node.calculateDeltaFitness();
			if(delta == 0.0){
				optimalTransformations.add(selNodes);
			}
		}
		
		return(optimalTransformations);
	}
	
	
	
	

	
	
	public Set<Clustering> enumerateClusterings(ArrayList<ArrayList<TNode>> optimalTransformations){
		Set<Clustering> s = new HashSet<>();
		
		for(ArrayList<TNode> selNodes : optimalTransformations){
			
			Clustering cnew = new Clustering(initClustering);
			cnew.changeClusterOfMultipleNodes(selNodes);
			cnew.computeImbalance(adjMat);
			s.add(cnew);
		}
		//System.out.println(s.size());
		return(s);
	}
	
	
	
}

