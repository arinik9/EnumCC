package enumeration;

import java.util.ArrayList;
import java.util.Arrays;
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
import myUtils.Combination2;
import myUtils.DirectedGraph;
import myUtils.MyCGraph;
import myUtils.Permutation;
import myUtils.UndirectedGraph;
import myUtils.UniquePartitionSize;
import permanence.MovingDependance;
// import permanence.Permanence;
// import permanence.Permanence.PermanenceData;
// import weka.Kmeans;
// import weka.core.Instances;

public class MyGenericEnumeration extends Thread {
//public class MyGenericEnumeration {
	int pass; // pass id
	Clustering initClustering;
	int maxNbEdit;
	int n;
	MyCGraph g;
	//long idCounter;
	double[][] adjMat;
	public Set<Clustering> foundClusterings;
	public Map<Integer, ArrayList<Clustering>> foundClusteringsByNbEditMap;
	int[] clusterIds;
	double[] execTimesByNbEdit;
	double totalExecTime;
	
	boolean isBruteForce = false; // value by default
	boolean usePermenanceScores = true;
	boolean[] isWeakPermanent;
	//final int NB_NODE_FOR_PERMANENCE = 20;
	
	// if true, a moving node can move into 2 or 3 target clusters (these clusters are the most likely one) in order to obtain an edit operation in the end
	// this might gain some time
	boolean useMostLikelyTargetClusters = true;
	MovingDependance movDep;
//	boolean[] isLikelyMovingNode;
//	int[] nbLikelyMovingNodesByCluster;
	
	//public MyGenericEnumeration(int maxNbEdit_, double[][] adjMat_, Clustering initClustering_, long idCounter_){
	public MyGenericEnumeration(int maxNbEdit_, double[][] adjMat_, Clustering initClustering_, int pass_){
		maxNbEdit = maxNbEdit_;
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
		
		
		
		
//		movDep = new MovingDependance(g, adjMat, initClustering, maxNbEdit); // TOOD nbEdit OR maxNbEdit ??
//		DirectedGraph diG = movDep.buildMovingDependenceGraph();
//		ArrayList<ArrayList<Integer>> subsetList = movDep.prepareResult(diG);
		
		
//		// ========================================
//		isLikelyMovingNode = new boolean[n];
//		for(int i=0; i<n; i++)
//			isLikelyMovingNode[i] = true; // by default
//		// ========================================

		
//		if(usePermenanceScores && maxNbEdit>3){
//			Permanence p = new Permanence(adjMat, g);
//			PermanenceData[] results = p.computePermananceScores();
//			isWeakPermanent = new boolean[n];
//			int nb = 0;
			
//			Kmeans kmeans = new Kmeans(); // TODO
//			int[] assignments = kmeans.run(scores, 2);
//			if(assignments != null) { // TODO why kmeans may have an error ? seed problem ?
//				double[] centroids = kmeans.getCentroids();
//				int labelWeak = 1;
//				if(centroids[0] < centroids[1])
//					labelWeak = 0;
//				
//				for(int i=0; i<adjMat.length; i++){
//					isWeakPermanent[i] = false;
//					//if(scores[i]<0){ // TODO: how to decide this criteria: which nodes will be considered as weak ?
//					if(assignments[i] == labelWeak){
//						nb += 1;
//						isWeakPermanent[i] = true;
//					}
//				}
//			} 
			
//			for(int i=0; i<adjMat.length; i++)
//				isWeakPermanent[i] = false;
//			
//			for(int i=0; i<NB_NODE_FOR_PERMANENCE; i++){
//				System.out.println("node id: " + results[i].getNodeId() + ", score: "+results[i].getPermScore());
//				isWeakPermanent[results[i].getNodeId()] = true;
//			}
//			System.out.println("nb weak permanent: " + NB_NODE_FOR_PERMANENCE);

//			int counter = 0;
//			for(int i=0; i<n; i++){
//				isWeakPermanent[i] = !movDep.isPermanentNode[i];
//				if(!movDep.isPermanentNode[i])
//					counter++;
//			}
//			System.out.println("nb weak permanent: " + counter);
//		}
	}
	
	
	// main method for Thread
	public void run() 
    { 
        try
        {
//            // Displaying the thread that is running 
//            System.out.println ("Thread " + 
//                  Thread.currentThread().getId() + 
//                  " is running"); 
            
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
//		foundClusterings.clear();
//		foundClusteringsByNbEditMap.clear();
		
		// ===============
		System.out.println(initClustering);
		for(int nbEdit=1;nbEdit<=this.maxNbEdit;nbEdit++){
			//System.out.println("nbEdit: " + nbEdit);
			
			long startTime = System.currentTimeMillis();
			
			if(usePermenanceScores && nbEdit>3){ // multiple times
				for(int levelNo=0; levelNo<nbEdit; levelNo++){
					//System.out.println("levelNo: " + levelNo);
					
					if(nbEdit ==4)
						movDep = new MovingDependance(g, adjMat, initClustering, nbEdit, levelNo, 12, 2); // TODO put 15, 3, when n>40
					else 
						movDep = new MovingDependance(g, adjMat, initClustering, nbEdit, levelNo, 12, 2);

					DirectedGraph diG = movDep.buildMovingDependenceGraph();
					
					for(ArrayList<Integer> subset : movDep.prepareResult(diG, levelNo)){
						isWeakPermanent = new boolean[n];
						for(int v : subset){
							isWeakPermanent[v] = true;
						}
						//System.out.println("nb weak permanent: " + subset.size() + " => " + subset);
						
						enumerateByNbEdit(nbEdit);
					}
				}
				
//				for(int levelNo : subsetListMap.keySet()){
//					System.out.println("levelNo: " + levelNo);
//
//					for(ArrayList<Integer> subset : subsetListMap.get(levelNo)){
//						isWeakPermanent = new boolean[n];
//						for(int v : subset){
//							isWeakPermanent[v] = true;
//						}
//						System.out.println("nb weak permanent: " + subset.size() + " => " + subset);
//						
//						enumerateByNbEdit(nbEdit);
//					}
//				}
			} 
			else // Once
				enumerateByNbEdit(nbEdit);
			
//			enumerateByNbEdit(nbEdit); // this put all found clusterings into 'foundClusteringsByNbEditMap'
//			System.out.println("nbEdit: " + nbEdit + ", curr found size:"+this.foundClusterings.size());
//			totalExecTime += execTimesByNbEdit[nbEdit-1]; // 'execTimesByNbEdit[nbEdit]' is assigned in 'enumerateByNbEdit'
			
			long endTime = System.currentTimeMillis();
			execTimesByNbEdit[nbEdit-1] = (float) (endTime-startTime)/1000;
			//System.out.println("exec time: " + execTimesByNbEdit[nbEdit-1]);
			totalExecTime += execTimesByNbEdit[nbEdit-1]; // 'execTimesByNbEdit[nbEdit]' is assigned in 'enumerateByNbEdit'

		}
	}
	
	
	// main method by nbEdit
	public void enumerateByNbEdit(int nbEdit) {
		// actually, the object 'foundClusteringsByNbEditMap' is not important for output, it only stores the discovered solutions in the current method, 
		//		then they are transferred into 'foundClusterings'
		foundClusteringsByNbEditMap.put(nbEdit, new ArrayList<Clustering>());
		
//		long startTime = System.currentTimeMillis();
		
		List<Integer> clusterIds = new ArrayList<Integer>();
		for(int i=1; i<=initClustering.getNbCluster(); i++)
			clusterIds.add(i);

		//List<int[]> allPermCombClusterIds = new ArrayList<>();
		for(int nbSourceCluster=1; nbSourceCluster<=nbEdit; nbSourceCluster++){
			List<int[]> combClusterIds = Combination.generate(clusterIds, nbSourceCluster);
			
			// long startTime2 = System.currentTimeMillis();
			for(int[] subClusterIds : combClusterIds){
				enumerateByNbSourceCluster(nbEdit, nbSourceCluster, subClusterIds);
			}
			// long endTime2 = System.currentTimeMillis();
			// System.out.println("nbSourceCluster: " + nbSourceCluster + " => " + (float) (endTime2-startTime2)/1000);
		}
		
//		long endTime = System.currentTimeMillis();
//		execTimesByNbEdit[nbEdit-1] = (float) (endTime-startTime)/1000;
//		System.out.println("exec time: " + execTimesByNbEdit[nbEdit-1]);
		
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
		
		// =========================================
		// permanance
		int[] initClusterSizes = initClustering.getClusterSizes();
		if(usePermenanceScores && nbEdit>3){ // the enumeration is fast up to 3edit >> previous version
		//if(usePermenanceScores && nbEdit>3 && nbSourceCluster>1){ // the enumeration is fast up to 3edit
			// do not use permanence based filtering, if nbSourceCluster=1 (i.e. split from a big cluster)
			Clustering temp = new Clustering(initClustering);
			for(int i=0; i<n; i++)
				if(!isWeakPermanent[i])
					temp.removeElementWithoutRemovingItsCluster(i);
			initClusterSizes = temp.getClusterSizes();
		}
		// =========================================

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
	
	
	
//	public ArrayList<Integer> getUnlikelyTargetClusterIds(TNode node){
//		ArrayList<ArrayList<Integer>> initClusters = initClustering.getClustersInArrayFormat();
//		ArrayList<Integer> unlikelyTargetClusterIds = new ArrayList<Integer>();
//		
//		double sourceFitness = g.weightSumInClusters[node.nodeId][node.clusterId-1];
//		for(int targetClusterId=1; targetClusterId<=(initClustering.getNbCluster()); targetClusterId++){
//			if(targetClusterId != node.clusterId){
//				double targetFitness = g.weightSumInClusters[node.nodeId][targetClusterId-1];
//				
//				int nbPossibleGain = 0;
//				for(int otherNodeId : initClusters.get(targetClusterId-1)){
//					// if a node v has a negative link with another node in a target cluster, and that other node is likely to move,
//					//		then this would be good for node v.
//					if(this.adjMat[node.nodeId][otherNodeId]<0 && this.isLikelyMovingNode[otherNodeId]){
//						nbPossibleGain++;
//					}
//				}
//				
//				if(targetFitness < -2)
//					unlikelyTargetClusterIds.add(targetClusterId);
//				else if((targetFitness+nbPossibleGain) < 0) // TODO we might handle better
//					unlikelyTargetClusterIds.add(targetClusterId);
//				else if( (sourceFitness-targetFitness)>=4 ) // TODO we need to adjust 4 based on 'maxNbEdit'
//					unlikelyTargetClusterIds.add(targetClusterId);
//			}
//		}
//		
//		return(unlikelyTargetClusterIds);
//	}
//	
	
	
	
	
	public void enumerateByNodes(int nbEdit, int[] sourceClusterSizes, int[] sourceClusterIds) {
		// sourceClusterSizes : {6, 1, 1}
		// clusterIds : {2,4,5}
		ArrayList<ArrayList<ArrayList<TNode>>> allSelNodesList = new ArrayList<ArrayList<ArrayList<TNode>>>();
		


		// =========================================
		// permanance
		ArrayList<ArrayList<Integer>> clusters = initClustering.getClustersInArrayFormat();
		if(usePermenanceScores && nbEdit>3){ // the enumeration is fast up to 3edit >> previous version
		//if(usePermenanceScores && nbEdit>3 && sourceClusterSizes.length>1){ // nbSourceCluster = sourceClusterSizes.length
			Clustering temp = new Clustering(initClustering);
			for(int i=0; i<n; i++)
				if(!isWeakPermanent[i])
					temp.removeElementWithoutRemovingItsCluster(i);
			clusters = temp.getClustersInArrayFormat();
		}
		// =========================================
		
		
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
					if(nbEdit>3 && this.useMostLikelyTargetClusters){
						ArrayList<Integer> unlikelyTargetClusterIds = 
								new ArrayList<Integer>(movDep.unlikelyTargetClusterIdListArray[node.nodeId]);
						node.addNotEqualToConstraints(unlikelyTargetClusterIds);
					}
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
		
		boolean isConnectedSourceNodes = isConnectedInternalMovingNodes(selNodesList); // not neceseraly positive connectivity
		boolean isWeightSumZeroInternalSourceNeighbors = isWeightSumZeroWithInternalSourceNeighborNodes(selNodesList);
		boolean isPositiveInternalNeighLinks = true;
		if(nbEdit <= 3)
			isPositiveInternalNeighLinks = isPositiveInternalNeighborLinksForUpTo3Edit(selNodesList);
		
		boolean ok = true;
		
		if(isConnectedSourceNodes && !isWeightSumZeroInternalSourceNeighbors && isPositiveInternalNeighLinks && ok){ // !isBruteForce && 
		
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
					node.addNotEqualToConstraints(new ArrayList<Integer>(Arrays.asList(node.getClusterId()))); // a moving node can not stay in its current cluster 
					selNodes.add(node);
					
					int[] clusterIdsInterest = selSourceClusterIds.clone();// i.e. the cluster ids of all moving nodes, except its cluster id
					clusterIdsInterest[i] = -1; // replace the id of the source cluster by -1, where -1 means a new cluster (not necesserily empty new one)
					allClusterIdsInterest.add(clusterIdsInterest);
				}
				
			} // selNodes is an array list containing all selected moving nodes of size 'nbEdit'
			
			
				
			// =============================================================================
			// PART 2: Trying to filter after target cluster ids & before determining target indexes 
			// =============================================================================
			
			ArrayList<ArrayList<TNode>> updatedSelNodesList = prepareSelNodesWithTargetClusterIds(selNodes, allClusterIdsInterest);
			
			
			if(nbEdit>3 && this.useMostLikelyTargetClusters){
				updatedSelNodesList = filterByLessLikelyTargetClusters(updatedSelNodesList);
			}
			
//			for(ArrayList<TNode> selNodes1 : updatedSelNodesList){
//				if(nbEdit==4 && selNodes1.get(0).getNodeId() == 34 && selNodes1.get(1).getNodeId() == 15 && 
//					selNodes1.get(2).getNodeId() == 31 && selNodes1.get(3).getNodeId() == 33
//					&& selNodes1.get(0).getTargetClusterId() == -1
//					&& selNodes1.get(1).getTargetClusterId() == 8
//					&& selNodes1.get(2).getTargetClusterId() == -1 && selNodes1.get(3).getTargetClusterId() == -1
//					)
//						System.out.println("d1");
//			}
			
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
			
			ArrayList<ArrayList<TNode>> updatedSelNodesList4 = prepareSelNodesWithTargetIndexes(updatedSelNodesList3);
						
			if(selNodesList.size()>1) // the size of selNodesList is 1, this means, all moving nodes are in the same source cluster, so they are connected by construction
				updatedSelNodesList4 = filterByConnectedComponent(updatedSelNodesList4); // remove decomposable Edit transformations
				// TODO rename it "filterByPosConnectedComponent"
			

//			for(ArrayList<TNode> selNodes1 : updatedSelNodesList4){
//				if(nbEdit==4 && selNodes1.get(0).getNodeId() == 12 && selNodes1.get(1).getNodeId() == 18 && 
//					selNodes1.get(2).getNodeId() == 17 && selNodes1.get(3).getNodeId() == 35
//					&& selNodes1.get(0).getTargetClusterId() == 5
//					&& selNodes1.get(1).getTargetClusterId() == 5
//					&& selNodes1.get(2).getTargetClusterId() == 4 && selNodes1.get(3).getTargetClusterId() == -1
//					)
//						System.out.println("d1");
//			}
			
//			for(ArrayList<TNode> selNodes1 : updatedSelNodesList4){
//				if(nbEdit==5 && selNodes1.get(0).getNodeId() == 13 && selNodes1.get(1).getNodeId() == 22 && 
//					selNodes1.get(2).getNodeId() == 15 && selNodes1.get(3).getNodeId() == 21 && selNodes1.get(4).getNodeId() == 34
//					&& selNodes1.get(0).getTargetClusterId() == 6 
//					&& selNodes1.get(1).getTargetClusterId() == -1
//					&& selNodes1.get(2).getTargetClusterId() == 4 && selNodes1.get(3).getTargetClusterId() == 4 && selNodes1.get(4).getTargetClusterId() == 4
//					)
//						System.out.println("d1");
//			}
			
			
			ArrayList<ArrayList<TNode>> updatedSelNodesList5 = updatedSelNodesList4;
			if(!isBruteForce && nbEdit>3) {
				updatedSelNodesList5 = filterByWeightSumZeroWithInternalTargetNeighborNodes(updatedSelNodesList4); // we do not need to do for 2edit or 3edit, because we implicitely do it in 'filterForUnweightedGraphBeforeTargetIndexesForExternalLinksUpTo3Edit()'
				if(isSourceClusterSizesLessThanThree){ // seek for sub 2edit or sub 3edit operation for additional filtering
					//updatedSelNodesList5 = filterForUnweightedGraphAfterTargetClusterIndexesFrom4Edit(updatedSelNodesList5);
					updatedSelNodesList5 = filterForUnweightedGraphBeforeTargetIndexesForExternalLinks(updatedSelNodesList5, true);
				}
			}
			
			
//			for(ArrayList<TNode> selNodes1 : updatedSelNodesList5){
//				if(nbEdit==4 && selNodes1.get(0).getNodeId() == 12 && selNodes1.get(1).getNodeId() == 18 && 
//					selNodes1.get(2).getNodeId() == 17 && selNodes1.get(3).getNodeId() == 35
//					&& selNodes1.get(0).getTargetClusterId() == 5
//					&& selNodes1.get(1).getTargetClusterId() == 5
//					&& selNodes1.get(2).getTargetClusterId() == 4 && selNodes1.get(3).getTargetClusterId() == -1
//					)
//						System.out.println("d1");
//			}
			
//			for(ArrayList<TNode> selNodes1 : updatedSelNodesList5){
//			if(nbEdit==5 && selNodes1.get(0).getNodeId() == 13 && selNodes1.get(1).getNodeId() == 22 && 
//				selNodes1.get(2).getNodeId() == 15 && selNodes1.get(3).getNodeId() == 21 && selNodes1.get(4).getNodeId() == 34
//				&& selNodes1.get(0).getTargetClusterId() == 6 
//				&& selNodes1.get(1).getTargetClusterId() == -1
//				&& selNodes1.get(2).getTargetClusterId() == 4 && selNodes1.get(3).getTargetClusterId() == 4 && selNodes1.get(4).getTargetClusterId() == 4
//				)
//					System.out.println("d1");
//		}
			
//			
//			if(nbEdit == 4) // hasTwoSourceClusters
//				System.out.println("aa");
			
			//System.out.println("size of 'updatedSelNodesList4':" + updatedSelNodesList4.size());
			for(ArrayList<TNode> updatedSelNodes : updatedSelNodesList5){			
				boolean isEligible = true;
				if(!isBruteForce)
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
			
//			if(nbEdit > 3){
//				System.out.println("!!!");
//			}
		
		//}
		
		}
	}
	

	
	public ArrayList<ArrayList<TNode>> filterByLessLikelyTargetClusters(ArrayList<ArrayList<TNode>> selNodesList){
		ArrayList<ArrayList<TNode>> collector = new ArrayList<ArrayList<TNode>>();
		
		for(ArrayList<TNode> selNodes : selNodesList){
			
			boolean ok = true;
			for(TNode node : selNodes){
				if(node.targetClusterId!=-1){
					for(Predicate<Integer> p : node.getNotEqualToConstraints()){
						if(!p.test(node.targetClusterId)){ // if targetClusterId is in the list
							ok = false;
							break;
						}
					}
					if(!ok)
						break;
				}
			}
			if(ok)
				collector.add(selNodes);
		}
		
		return(collector);
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
			int[] clusterIdsInterest = allClusterIdsInterest.get(i);
			TNode node = selNodes.get(i);
			
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
	
	
	// for uncomplete networks 
	public boolean isConnectedInternalMovingNodes(ArrayList<ArrayList<TNode>> selNodesList){

		for(ArrayList<TNode> selNodes : selNodesList){ // sel nodes are organized by source cluster
			// build the graph for each source cluster where size>1: add an edge when there is a real link
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
		}
		
		return(true);
	}
	
	
	
	
	
	// Up to 3-edit, all links of a node wiht the other moving nodes in the same cluster have to be positive.
		public boolean isPositiveInternalNeighborLinksForUpTo3Edit(ArrayList<ArrayList<TNode>> selNodesList){
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
		
	
	
	// we do no want that the sum of the weights of a node with its neighbors being the same cluster to be zero.
	public boolean isWeightSumZeroWithInternalSourceNeighborNodes(ArrayList<ArrayList<TNode>> selNodesList){

		for(ArrayList<TNode> selNodes : selNodesList){ // sel nodes are organized by source cluster
			if(selNodes.size()>1){ // a cluster having only 1 node is by construction connected here
				for(TNode node : selNodes){ // for each node being in one of the source clusters
					double sum = 0.0;
					for(TNode otherNode : selNodes){
						if(node.nodeId != otherNode.nodeId){
							sum += adjMat[node.getNodeId()][otherNode.getNodeId()];
						}
					}
					if(sum == 0.0)
						return(true);
				}
				
			}
		}
		
		return(false);
	}
	
	
	// OLD version, which was erroneous.
//	public ArrayList<ArrayList<TNode>> filterByWeightSumZeroWithInternalTargetNeighborNodes(ArrayList<ArrayList<TNode>> selNodesList){
//		ArrayList<ArrayList<TNode>> collector = new ArrayList<ArrayList<TNode>>();
//		
//		for(ArrayList<TNode> selNodes : selNodesList){
//			
//			if(selNodes.size()==4 && selNodes.get(0).getNodeId() == 4 && selNodes.get(1).getNodeId() == 24 && 
//					selNodes.get(2).getNodeId() == 30 && selNodes.get(3).getNodeId() == 17
//					&& selNodes.get(0).getTargetClusterId() == -1
//					&& selNodes.get(1).getTargetClusterId() == 6
//					&& selNodes.get(2).getTargetClusterId() == 6 && selNodes.get(3).getTargetClusterId() == 2
//					)
//						System.out.println("d1");
//			
//			HashMap<Integer, ArrayList<TNode>> sourceClusterIdCounterMap = new HashMap<>(); //
//			HashMap<Integer, ArrayList<TNode>> targetClusterIdCounterMap = new HashMap<>();
//			for(TNode node : selNodes){
//				// reorganize moving nodes by their target cluster
//				if(node.targetClusterId != -1){
//					if(!targetClusterIdCounterMap.containsKey(node.targetClusterId))
//						targetClusterIdCounterMap.put(node.targetClusterId, new ArrayList<TNode>());
//					targetClusterIdCounterMap.get(node.targetClusterId).add(node);
//				}
//				if(node.targetClusterId == -1 && node.targetIndex != -1){
//					if(!targetClusterIdCounterMap.containsKey(-node.targetIndex)) // I put minus to distinguish from the other target clusters
//						targetClusterIdCounterMap.put(-node.targetIndex, new ArrayList<TNode>());
//					 targetClusterIdCounterMap.get(-node.targetIndex).add(node);
//				}
//				
//				// reorganize moving nodes by their source cluster
//				if(!sourceClusterIdCounterMap.containsKey(node.clusterId))
//					sourceClusterIdCounterMap.put(node.clusterId, new ArrayList<TNode>());
//				sourceClusterIdCounterMap.get(node.clusterId).add(node);
//			}
//			
//			// for instance
//			// targetClusterIdCounterMap:
//			// {-1=[(nodeId:22)], 4=[(nodeId:15), (nodeId:21), (nodeId:34)], 6=[(nodeId:13)]}
//			// sourceClusterIdCounterMap:
//			// {4=[(nodeId:13), (nodeId:22)], 5=[(nodeId:15)], 6=[(nodeId:21), (nodeId:34)]}
//			
//			// ========================
//			
//
//			boolean ok = true;
//			for(TNode node : selNodes){ // for each node
//				
//				if( !(targetClusterIdCounterMap.get(node.targetClusterId).size()==1 // since 'node' has to be there
//					&& !sourceClusterIdCounterMap.containsKey(node.targetClusterId) ) )
//				{
//					// we will verify the following cases:
//					// let 'v' a moving node among other moving nodes
//					// 1) let A the sum of the links between 'v' and the other moving nodes which are moving into the target cluster of 'v'
//					// 2) let B the sum of the links between 'v' and the other moving nodes which are in the target cluster of 'v'
//					// if an edit operation is undecomposable, then A+B should not be zero
//	
//					double sum = 0.0;
//	
//					ArrayList<TNode> tNeighs = targetClusterIdCounterMap.get(node.targetClusterId);
//					if(tNeighs.size()>1){ // since 'node' has to be in 'tNeights', tNeighs.size() should be at least 2
//						for(TNode neigh : tNeighs){
//							if(neigh.nodeId != node.nodeId)
//								sum += adjMat[node.getNodeId()][neigh.getNodeId()];
//						}
//					}
//					
//					if(sourceClusterIdCounterMap.containsKey(node.targetClusterId)){
//						ArrayList<TNode> sNeighs = sourceClusterIdCounterMap.get(node.targetClusterId);
//						if(sNeighs.size()>0){
//							for(TNode neigh : sNeighs){
//								if(neigh.nodeId != node.nodeId)
//									sum += adjMat[node.getNodeId()][neigh.getNodeId()];
//							}
//						}
//					}
//					
//	
//					if(sum == 0.0){
//						ok = false;
//						break;
//					}
//				}
//			}
//			
//			if(ok)
//				collector.add(selNodes);
//			
//		}
//		
//		return(collector);
//	}
	
	
	public ArrayList<ArrayList<TNode>> filterByWeightSumZeroWithInternalTargetNeighborNodes(ArrayList<ArrayList<TNode>> selNodesList){
		ArrayList<ArrayList<TNode>> collector = new ArrayList<ArrayList<TNode>>();
		
		for(ArrayList<TNode> selNodes : selNodesList){
			
//			if(selNodes.size()==5 && selNodes.get(0).getNodeId() == 13 && selNodes.get(1).getNodeId() == 22 && 
//					selNodes.get(2).getNodeId() == 15 && selNodes.get(3).getNodeId() == 21 && selNodes.get(4).getNodeId() == 34
//				&& selNodes.get(0).getTargetClusterId() == 6 
//				&& selNodes.get(1).getTargetClusterId() == -1
//				&& selNodes.get(2).getTargetClusterId() == 4 && selNodes.get(3).getTargetClusterId() == 4 && selNodes.get(4).getTargetClusterId() == 4
//			)
//				System.out.println("d1");
			
			HashMap<Integer, ArrayList<TNode>> sourceClusterIdCounterMap = new HashMap<>(); //
			HashMap<Integer, ArrayList<TNode>> targetClusterIdCounterMap = new HashMap<>();
			for(TNode node : selNodes){
				// reorganize moving nodes by their target cluster
				if(node.targetClusterId != -1){
					if(!targetClusterIdCounterMap.containsKey(node.targetClusterId))
						targetClusterIdCounterMap.put(node.targetClusterId, new ArrayList<TNode>());
					targetClusterIdCounterMap.get(node.targetClusterId).add(node);
				}
				if(node.targetClusterId == -1 && node.targetIndex != -1){
					if(!targetClusterIdCounterMap.containsKey(-node.targetIndex)) // I put minus to distinguish from the other target clusters
						targetClusterIdCounterMap.put(-node.targetIndex, new ArrayList<TNode>());
					 targetClusterIdCounterMap.get(-node.targetIndex).add(node);
				}
				
				// reorganize moving nodes by their source cluster
				if(!sourceClusterIdCounterMap.containsKey(node.clusterId))
					sourceClusterIdCounterMap.put(node.clusterId, new ArrayList<TNode>());
				sourceClusterIdCounterMap.get(node.clusterId).add(node);
			}
			
			// for instance
			// targetClusterIdCounterMap:
			// {-1=[(nodeId:22)], 4=[(nodeId:15), (nodeId:21), (nodeId:34)], 6=[(nodeId:13)]}
			// sourceClusterIdCounterMap:
			// {4=[(nodeId:13), (nodeId:22)], 5=[(nodeId:15)], 6=[(nodeId:21), (nodeId:34)]}
			
			// ========================
			

			boolean ok = true;
			for(TNode node : selNodes){ // for each node
				// if there are some moving nodes in the target cluster, and there is only moving node which will move into that target cluster
				if( sourceClusterIdCounterMap.containsKey(node.targetClusterId) && targetClusterIdCounterMap.get(node.targetClusterId).size()==1)
				{
					// we will verify the following cases:
					// let 'v' a moving node among other moving nodes
					// 1) let A the sum of the links between 'v' and the other moving nodes which are moving into the target cluster of 'v'
					// 2) let B the sum of the links between 'v' and the other moving nodes which are in the target cluster of 'v'
					// if an edit operation is undecomposable, then A+B should not be zero
	
					double sum = 0.0;
	
//					ArrayList<TNode> tNeighs = targetClusterIdCounterMap.get(node.targetClusterId);
//					if(tNeighs.size()>1){ // since 'node' has to be in 'tNeights', tNeighs.size() should be at least 2
//						for(TNode neigh : tNeighs){
//							if(neigh.nodeId != node.nodeId)
//								sum += adjMat[node.getNodeId()][neigh.getNodeId()];
//						}
//					}
					
					if(sourceClusterIdCounterMap.containsKey(node.targetClusterId)){
						ArrayList<TNode> sNeighs = sourceClusterIdCounterMap.get(node.targetClusterId);
						if(sNeighs.size()>0){
							for(TNode neigh : sNeighs){
								if(neigh.nodeId != node.nodeId)
									sum += adjMat[node.getNodeId()][neigh.getNodeId()];
							}
						}
					}
					
	
					if(sum == 0.0){
						ok = false;
						break;
					}
				}
			}
			
			if(ok)
				collector.add(selNodes);
			
		}
		
		return(collector);
	}
	
	
	
	
	
	public ArrayList<ArrayList<TNode>> filterByFakeEditTransformation(ArrayList<ArrayList<TNode>> selNodesList,
			int[] selSourceClusterIds, boolean[] isWholeClusterBoolArr){
		ArrayList<ArrayList<TNode>> collector = new ArrayList<ArrayList<TNode>>();
		
		int[] initClusterSizes = initClustering.getClusterSizes();
		
		for(ArrayList<TNode> selNodes : selNodesList){
			boolean fakeEditTransformation = false; // if the target cluster of some moving nodes is known, there check it for them
			// scenario 1: All the elements in one of the source clusters moves into the one of the other source clusters,
			//		where all the elements of this target cluster move into another cluster ==> so, it becomes identical to the first case
			for(TNode node : selNodes){ // iterate the other nodes
				int index = ArrayOperations.whichIndex(selSourceClusterIds, node.targetClusterId); // source cluster index of the other node
				if(index!=-1 && isWholeClusterBoolArr[index]) {
					fakeEditTransformation = true;
					break;
				}
			}
			
			// scenario 2: this is the case where two clusters exchange some nodes. It is possible that a 3-edit operation is fake,
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
	
	
	
	
	public ArrayList<ArrayList<TNode>> filterByConnectedComponent(ArrayList<ArrayList<TNode>> selNodesList){
		ArrayList<ArrayList<TNode>> collector = new ArrayList<ArrayList<TNode>>();
			
		for(ArrayList<TNode> selNodes : selNodesList){
			
//				if(selNodes.size()==5 && selNodes.get(0).getNodeId() == 13 && selNodes.get(1).getNodeId() == 22 && 
//						selNodes.get(2).getNodeId() == 15 && selNodes.get(3).getNodeId() == 21 && selNodes.get(4).getNodeId() == 34
//					&& selNodes.get(0).getTargetClusterId() == 6 
//					&& selNodes.get(1).getTargetClusterId() == -1
//					&& selNodes.get(2).getTargetClusterId() == 4 && selNodes.get(3).getTargetClusterId() == 4 && selNodes.get(4).getTargetClusterId() == 4
//					)
//						System.out.println("d1");
			
//			if(selNodes.size()==4 && selNodes.get(0).getNodeId() == 12 && selNodes.get(1).getNodeId() == 18 && 
//					selNodes.get(2).getNodeId() == 17 && selNodes.get(3).getNodeId() == 35
////					&& selNodes.get(0).getTargetClusterId() == -1
////					&& selNodes.get(1).getTargetClusterId() == -1
////					&& selNodes.get(2).getTargetClusterId() == 4 && selNodes.get(3).getTargetClusterId() == 4
//					)
//						System.out.println("d1");
			
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
				
				for(TNode node : selNodes){ // we treat the case of uncomplete graphs
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
	

	// this is used from 4-edit
	public ArrayList<ArrayList<TNode>> filterForUnweightedGraphBeforeTargetIndexesForExternalLinks(
			ArrayList<ArrayList<TNode>> selNodesList, boolean afterTargetIndexes)
	{
		ArrayList<ArrayList<TNode>> collector = new ArrayList<ArrayList<TNode>>();

		for(ArrayList<TNode> selNodes : selNodesList){

			int[] nbConnections = new int[selNodes.size()];
			int indx = 0;
			for(TNode node : selNodes){
				int nb = 0;
				
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
					}
				}
				nbConnections[indx] = nb;
				indx++;
			}
			
			boolean ok = true;
			indx=-1;
			for(TNode node : selNodes){ // for each pair of nodes interacting between them
				indx++;
				if(!ok)
					break;
				
				// we check if the vertex is like in 2- or 3-edit operation, i.e. not many interactions
				if(nbConnections[indx]<=2){ // the upper bound is 2, because in 3-edit operation, a moving node has 2 interactions
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
									if(adjMat[node.getNodeId()][otherNode.getNodeId()]<0) // we do not want negative weight
										ok = false;
								} 
								else if(node.getTargetClusterId() != otherNode.getTargetClusterId()
										&& (node.getTargetClusterId() == otherNode.clusterId || otherNode.getTargetClusterId() == node.clusterId)){
									// target cluster ids are diff, but also 'node' moves into the other's cluster, i.e. interaction
									if(adjMat[node.getNodeId()][otherNode.getNodeId()]>0) // we do not want positive weight
										ok = false;
								}
							}
							else {
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
	
	
	
	// we are interested in the case where the moving nodes are not in the same cluster. Then, we check if they will move into
	//	the same or different cluster.
	// For up to 3-edit operations, this is efficient filtering. From 4-edit, this becomes less efficient, since
	// the number of cases where the pair of moving nodes has known target cluster id decreases
	public ArrayList<ArrayList<TNode>> filterForUnweightedGraphBeforeTargetIndexesForExternalLinksUpTo3Edit(
			ArrayList<ArrayList<TNode>> selNodesList, boolean hasTwoSourceClusters){
		ArrayList<ArrayList<TNode>> collector = new ArrayList<ArrayList<TNode>>();
		for(ArrayList<TNode> selNodes : selNodesList){
			boolean ok = true;
			boolean isExceptionalCaseFor3Edit = false;
			int nbEdit = selNodes.size();
			

			// =======
			// this exceptional case occurs because we want to perform the filtering before determining target indexes 
			//	in order to gain exec time
			
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
				// to multiply target cluster id of two nodes allows to know if only one of them moves into unknown cluster
				if((selNodes2.get(0).targetClusterId * selNodes2.get(1).targetClusterId) < 0 && selNodes2.get(2).targetClusterId == -1){
					isExceptionalCaseFor3Edit = true;
					if(selNodes2.get(0).targetClusterId != -1 && adjMat[selNodes2.get(0).getNodeId()][selNodes2.get(2).getNodeId()]>=0)
						ok = false; // should be negative
					else if(selNodes2.get(1).targetClusterId != -1 && adjMat[selNodes2.get(1).getNodeId()][selNodes2.get(2).getNodeId()]>=0)
						ok = false; // should be negative
				}
				
			} 
			
			// =======
			
			if(!isExceptionalCaseFor3Edit){
				// to adapt this to the case of the uncomplete graph, a pair of nodes may have no link. 
				// But, note that we check before this method that the moving nodes are connected internally and externally between them (i.e. connected component)
				for(TNode node : selNodes){ // for each pair of nodes interacting between them
					if(!ok)
						break;
					
					for(TNode otherNode : selNodes){
						if(ok && node.getNodeId() != otherNode.getNodeId() && node.getClusterId() != otherNode.getClusterId()){ 
							// ensure that 2 different nodes and they are not in the same cluster
							
							if(node.getTargetClusterId() == otherNode.getTargetClusterId()){
								if(adjMat[node.getNodeId()][otherNode.getNodeId()]<0) // we do not want negative weight
									ok = false;
								
							} else if(node.getTargetClusterId() != otherNode.getTargetClusterId()
									&& (node.getTargetClusterId() == otherNode.clusterId || otherNode.getTargetClusterId() == node.clusterId)){
								// target cluster ids are diff, but also one of them (or both) moves into the other's cluster, i.e. interaction
								if(adjMat[node.getNodeId()][otherNode.getNodeId()]>0) // we do not want positive weight
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
	
	
	
	
//	public ArrayList<ArrayList<TNode>> filterForUnweightedGraphAfterTargetClusterIndexesFrom4Edit(ArrayList<ArrayList<TNode>> selNodesList){
//		ArrayList<ArrayList<TNode>> collector = new ArrayList<ArrayList<TNode>>();
//		
//		for(ArrayList<TNode> selNodes : selNodesList){
//			boolean ok = true;
//			
//			if(isSubTwoOrThreeEditOperation(selNodes)){ // if this is the case, filter regarding positive/negative links between moving nodes
//				for(TNode node : selNodes){ // for each pair of nodes
//					if(!ok)
//						break;
//					
//					for(TNode otherNode : selNodes){
//						boolean nowSameCluster = (node.getClusterId() == otherNode.getClusterId());
//						if(ok && node.getNodeId() != otherNode.getNodeId()){ // ensure that 2 different nodes
//							
//							boolean afterSameCluster1 = (node.getTargetClusterId() == otherNode.getTargetClusterId());
//							boolean afterSameCluster2 = (node.getTargetIndex() == otherNode.getTargetIndex());
//							boolean afterSameCluster = (afterSameCluster1 && afterSameCluster2);
//							
//							if(nowSameCluster) {
//								if(adjMat[node.getNodeId()][otherNode.getNodeId()]<=0) 
//									ok = false;
//								
//							} else { // !nowSameCluster
//								
//								if(afterSameCluster){
//									if(adjMat[node.getNodeId()][otherNode.getNodeId()]<=0) // we do not want negative weight or missing link
//										ok = false;
//									
//								} else if(!afterSameCluster
//										&& (node.getTargetClusterId() == otherNode.clusterId || otherNode.getTargetClusterId() == node.clusterId)){
//									// target cluster ids are diff, but also one of them (or both) moves into the other's cluster, i.e. interaction
//									if(adjMat[node.getNodeId()][otherNode.getNodeId()]>=0) // we do not want positive weight or missing link
//										ok = false;
//								}
//								
//							}
//							
//							
//						}
//					}
//				}
//			} // even if the operation is not sub 2edit or sub 3edit, add it
//			if(ok) // if it is still true
//				collector.add(selNodes);
//		}
//		
//		return(collector);
//	}
//	
//	
//	// this is related to our MVMO property in order to say something about the sign of the edges
//	public boolean isSubTwoOrThreeEditOperation(ArrayList<TNode> selNodes){
//		HashMap<Integer, Integer> sourceClusterIdCounterMap = new HashMap<>(); 
//		HashMap<Integer, Integer> targetClusterIdCounterMap = new HashMap<>();
//		HashMap<Integer, Integer> targetClusterIndexesCounterMap = new HashMap<>();
//		
//		for(TNode node : selNodes){
//			if(!sourceClusterIdCounterMap.containsKey(node.clusterId))
//				sourceClusterIdCounterMap.put(node.clusterId, 0);
//			sourceClusterIdCounterMap.put(node.clusterId, sourceClusterIdCounterMap.get(node.clusterId) + 1);
//			if(sourceClusterIdCounterMap.get(node.clusterId)>3)
//				return(false);
//			
//			// -------------------
//			
//			if(node.targetClusterId != -1){
//				if(!targetClusterIdCounterMap.containsKey(node.targetClusterId))
//					targetClusterIdCounterMap.put(node.targetClusterId, 0);
//				targetClusterIdCounterMap.put(node.targetClusterId, targetClusterIdCounterMap.get(node.targetClusterId) + 1);
//				if(targetClusterIdCounterMap.get(node.targetClusterId)>3)
//					return(false);
//			}
//			
//			// -------------------
//			
//			if(node.targetClusterId == -1){
//				if(!targetClusterIndexesCounterMap.containsKey(node.targetIndex))
//					targetClusterIndexesCounterMap.put(node.targetIndex, 0);
//				targetClusterIndexesCounterMap.put(node.targetIndex, targetClusterIndexesCounterMap.get(node.targetIndex) + 1);
//				if(targetClusterIndexesCounterMap.get(node.targetIndex)>3)
//					return(false);
//			}
//		}
//		
//		// ==========================
//		
//		for(TNode node : selNodes){
//			int nbOthers1 = 0;
//			int nbOthers2 = 0;
//			if(targetClusterIdCounterMap.containsKey(node.clusterId))
//				nbOthers1 = targetClusterIdCounterMap.get(node.clusterId);
//			if(sourceClusterIdCounterMap.containsKey(node.targetClusterId))
//				nbOthers2 = sourceClusterIdCounterMap.get(node.targetClusterId);
//			
//			
//			if((sourceClusterIdCounterMap.get(node.clusterId)+nbOthers1)>3) {
//				// the number of nodes in the source cluster + the number of other nodes coming into source cluster 
//				return(false);
//			}
//			else if(node.targetClusterId != -1 && (targetClusterIdCounterMap.get(node.targetClusterId)+nbOthers2)>3) {
//				// the number of nodes moving into the target cluster + the number of other nodes leaving the target cluster 
//				return(false);
//			}
//		}
//		
//		return(true);
//	}
	
	
	
	
	
	
	// we suppose that first 2 nodes in the same cluster, the other node is in diff cluster
	public boolean isEligibleTransformation(ArrayList<TNode> selNodes, boolean withKnownTargetClusters){
		//ArrayList<ArrayList<Integer>> clusters = initClustering.getClustersInArrayFormat();

		/*
		 * Input: [(nodeId:7, clusterId:2, targetClusterId:-1, targetIndex:1), (nodeId:8, clusterId:2, targetClusterId:-1, targetIndex:1), (nodeId:14, clusterId:3, targetClusterId:2, targetIndex:-1)]
		 */
		
//		if(selNodes.get(0).getNodeId() == 14 && selNodes.get(1).getNodeId() == 15 && selNodes.get(2).getNodeId() == 18
//				//&& selNodes.get(0).getTargetClusterId() == 4 && selNodes.get(1).getTargetClusterId() == 4 && selNodes.get(2).getTargetClusterId() == -1
//				)
//			System.out.println("d");
			
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
			
//			if(selNodes.get(0).getNodeId() == 14 && selNodes.get(1).getNodeId() == 15 && selNodes.get(2).getNodeId() == 18
//					//&& selNodes.get(0).getTargetClusterId() == 4 && selNodes.get(1).getTargetClusterId() == 4 && selNodes.get(2).getTargetClusterId() == -1
//					)
//				System.out.println("d");
			
			if(!withKnownTargetClusters){
				// calculate the inequalities
				double tot = weightsNowSame+weightsAfterSame-weightsNowNextTarget-NowDiffAfterCurr;
				if(tot<0){
					return(false);
				}
			} else {
				// if(deltaNode<0 || deltaNode<(-weightsAfterSame+NowDiffAfterCurr)) // (-weightsAfterSame+NowDiffAfterCurr)>deltaNode
				if(deltaNode<(-weightsAfterSame+NowDiffAfterCurr)) // (-weightsAfterSame+NowDiffAfterCurr)>deltaNode
					return(false);

			}
		}
				
		return(true);
				
	}
	
	
////	// TODO - TEMPORARY, DELETE IT
////	public ArrayList<ArrayList<TNode>> filterByDecomposabe1EditOptimalTransformations(ArrayList<ArrayList<TNode>> optimalTransformations){
////		ArrayList<ArrayList<TNode>> subset = new ArrayList<ArrayList<TNode>>();
////
////		for(ArrayList<TNode> selNodes : optimalTransformations){
////			boolean nonDecomposable = true;
////			for(TNode node : selNodes){
////				TNode newNode = new TNode(node);
////				newNode.computeCurrNodeFitness(g, adjMat);
////				newNode.computeTargetNodeFitness(g, adjMat);
////				double delta = newNode.calculateDeltaFitness();
////				if(delta == 0.0){ // if the delta of any of them is zero, this means that it is a decomposable 1-Edit transformation
////					nonDecomposable = false;
////					break;
////				}
////			}
////			
////			if(nonDecomposable)
////				subset.add(selNodes);
////		}
////		
////		return(subset);
////	}
//	
//	// TODO: YOU CAN NORMALLY USE IT UP TO 3-EDIT, BUT THERE IS A BUG
//	public ArrayList<ArrayList<TNode>> filterByDecomposabe1EditOptimalTransformations(ArrayList<ArrayList<TNode>> optimalTransformations){
//		ArrayList<ArrayList<TNode>> subset = new ArrayList<ArrayList<TNode>>();
//
//		for(ArrayList<TNode> selNodes : optimalTransformations){
//			boolean nonDecomposable = true;
//			
//			for(TNode node : selNodes){
//				// there are 2 cases to be checked:
//				// Case 1) 'node' is moved to its target cluster BEFORE the moves of the other moving nodes
//				TNode newNode = new TNode(node);
//				newNode.computeCurrNodeFitness(g, adjMat);
//				newNode.computeTargetNodeFitness(g, adjMat);
//				double delta = newNode.calculateDeltaFitness();
//				if(delta == 0.0){ // if the delta of any of them is zero, this means that it is a decomposable 1-Edit transformation
//					nonDecomposable = false;
//					break;
//				}
//				
//				// Case 2) 'node' is moved to its target cluster AFTER the moves of the other moving nodes
//				TNode newNode2 = new TNode(node);
//				newNode2.computeCurrNodeFitness(g, adjMat);
//				newNode2.computeTargetNodeFitness(g, adjMat);
//				ArrayList<TNode> otherSelNodes = new ArrayList<TNode>(selNodes);
//				otherSelNodes.remove(node);
//				for(TNode otherNode : otherSelNodes){
//					// update curr fitness
//					if(newNode2.getClusterId() == otherNode.getTargetClusterId()){
//						// the operation is an addition here: substraction of a negative value
//						newNode2.substractFromCurrNodeFitness(-adjMat[newNode2.getNodeId()][otherNode.getNodeId()]);
//					}
//					if(newNode2.getClusterId() == otherNode.getClusterId()){
//						newNode2.substractFromCurrNodeFitness(adjMat[newNode2.getNodeId()][otherNode.getNodeId()]);
//					}
//					
//					// update target fitness
//					if(newNode2.getClusterId() == otherNode.getTargetClusterId()){
//						newNode2.substractFromTargetNodeFitness(adjMat[newNode2.getNodeId()][otherNode.getNodeId()]);
//					}
//					if(newNode2.getTargetClusterId() == otherNode.getTargetClusterId()){
//						// the operation is an addition here: substraction of a negative value
//						newNode2.substractFromTargetNodeFitness(-adjMat[newNode2.getNodeId()][otherNode.getNodeId()]);
//					}
//				}
//				double delta2 = newNode2.calculateDeltaFitness();
//				if(delta2 == 0.0){ // if the delta of any of them is zero, this means that it is a decomposable 1-Edit transformation
//					nonDecomposable = false;
//					break;
//				}
//			}
//			
//			if(nonDecomposable)
//				subset.add(selNodes);
//		}
//		
//		return(subset);
//	}
	
	
	// nbEditForDecomposability should not exceed ceiling(nbEdit)/2
	public ArrayList<ArrayList<TNode>> filterByDecomposableEditOptimalTransformations(ArrayList<ArrayList<TNode>> optimalTransformations,
			int nbEditForDecomposability){
		ArrayList<ArrayList<TNode>> subset = new ArrayList<ArrayList<TNode>>();
		

		for(ArrayList<TNode> selNodes : optimalTransformations){
			int[] indexs = ArrayOperations.seq(0, selNodes.size()-1);
			List<int[]> combinations = Combination.generate(indexs, nbEditForDecomposability);
			
//			boolean debug = false;
//			if(this.pass == 1 && selNodes.size()>2)
//				debug = true;
			
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
				
				
				// Case 1) 'subSelNodes' are moved to their target clusters BEFORE the moves of the other moving nodes
				double delta = 0.0;
				for(TNode node : subSelNodes){
					node.computeCurrNodeFitness(g, adjMat);
					node.computeTargetNodeFitness(g, adjMat);
					delta += node.calculateDeltaFitness();
				}
				delta += weightSum;
				if(delta == 0.0){ // if the delta of any of them is zero, this means that it is a decomposable 1-Edit transformation
					nonDecomposable = false;
					break;
				}
					
				
				
				// Case 2) 'subSelNodes' are moved to their target clusters AFTER the moves of the other moving nodes
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
	
	
	
	/*
	 * It is a generic method to find candidate transformations in the aim of finding alternative optimal clustering
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
		// isWholeClusterBoolArr is an array of size "nb source clsuter",
		//		and tells us that if the cluster of selected moving nodes will be empty after the transformation 
		
		/*
		 * input: [(nodeId:7, clusterId:2, targetClusterId:-1, targetIndex:1), (nodeId:9, clusterId:2, targetClusterId:-1, targetIndex:1),
		 * 			 (nodeId:14, clusterId:3, targetClusterId:-1, targetIndex:2)]
		 * 
		 */

//		if(selNodes.get(0).getTargetClusterId()!=-1 && selNodes.get(1).getTargetClusterId()!=-1 && selNodes.get(2).getTargetClusterId()!=-1)
//			System.out.println("d");
		
		
	
				
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
					
//					if(selNodes.get(0).getNodeId() == 13 && selNodes.get(1).getNodeId() == 16 && selNodes.get(2).getNodeId() == 17)
//						node.computeCurrNodeFitness(g, adjMat);
					
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
			
			int newEmptyClusterId = initClustering.getNbCluster()+1;
			
			ArrayList<List<Integer>> remainingClusterIdsList = new ArrayList<List<Integer>>();
			for(TNode node : subsetNodes){
				if(node.notEqualToConstraints.size()>0 && node.getTargetClusterId() == -1){
					List<Integer> remainingClusterIds = new ArrayList<>();
					for(int cid=1; cid<=(initClustering.getNbCluster()+1); cid++) // +1 for a new cluster
						remainingClusterIds.add(cid);
					
					// chaining all predicates of a given node, if there are many
					Predicate<Integer> compositePredicate = node.notEqualToConstraints.stream().reduce(w -> true, Predicate::and); 
					remainingClusterIds = remainingClusterIds.stream()   // convert list to stream
			                .filter(compositePredicate)
			                .collect(Collectors.toList());
					
					remainingClusterIdsList.add(remainingClusterIds);
				}
			} // note that remainingClusterIds are common for all those subset nodes
			
			
//			// 3.2) generate combinations of cluster ids of size 'nbTargetClusters'
//			List<int[]> permCombs = new ArrayList<int[]>();
//			List<int[]> combinations = Combination.generate(remainingClusterIds, nbTargetClustersToFind);
//			for (int[] combination : combinations)
//				permCombs.addAll(Permutation.permute(combination));
			
			List<ArrayList<Integer>> permCombs = Combination2.generate(remainingClusterIdsList);
			
			// ===========================================================================

		
//			if(selNodes.size() == 3 && selNodes.get(0).getNodeId() == 17 && selNodes.get(1).getNodeId() == 20 && selNodes.get(2).getNodeId() == 23)
//				System.out.println("d");
			
//			if(selNodes.size()>3 && selNodes.get(0).getNodeId() == 0 && selNodes.get(1).getNodeId() == 4 && selNodes.get(2).getNodeId() == 10 && selNodes.get(3).getNodeId() == 13 
//					&& selNodes.get(2).getTargetIndex() == -1 && selNodes.get(3).getTargetIndex() == -1
//					)
//						System.out.println("d");
			
			
			// 4) for each combination, check if the global objective function remains unchanged, i.e. it is an optimal clustering ?
			//for (int[] combination : permCombs) {
			for (ArrayList<Integer> combination : permCombs) {

			    //System.out.println(Arrays.toString(combination));
				
//				ArrayList<Integer> selNodeIds = new ArrayList<Integer>();
//				selNodeIds.add(selNodes.get(0).getNodeId());
//				selNodeIds.add(selNodes.get(1).getNodeId());
//				selNodeIds.add(selNodes.get(2).getNodeId());
//				if(selNodeIds.contains(24) && selNodeIds.contains(13) && selNodeIds.contains(14) && combination.length==1 && combination[0]==10)
//					System.out.println("'selNodes':" + selNodes);
				
				
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
						//there are two possibilities for a fake edit transformation
						// 1) all the elements in one of the source clusters moves into a new (so, empty) cluster
						
						if(isWholeClusterBoolArr[nodeSourceClusterIndex]){
							if(newTargetClusterId == newEmptyClusterId){
								fakeEditTransformation = true;
							} else { // the whole source cluster does non move into a new cluster, the second fake scenario is that
								// the whole source cluster moves into an existing cluster whose the size is less than the source size
								if(initClusters.get(nodeClusterId-1).size()>initClusters.get(newTargetClusterId-1).size())
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
				
				boolean isEligible = isEligibleTransformation(updatedNodes, true);
				if(isEligible && ok && delta == 0.0){
					optimalTransformations.add(updatedNodes);
				
//					Clustering cnew = new Clustering(initClustering);
////					if(selNodes.size()==1)
////						cnew.changeClusterOfNode(selNodes.get(0).getNodeId(), selNodes.get(0).getClusterId(), true);
////					else
//					cnew.changeClusterOfMultipleNodes(updatedNodes);
//					cnew.computeImbalance(adjMat);
//					System.out.println(cnew.toString());
				}
				
			}
			
		} else { // if(nbTargetClusters == 0){ ==> i.e. we know where to move all selected nodes
			
			double delta = change; // change comes from the step 1
			for(TNode node : selNodes)
				delta += node.calculateDeltaFitness();
			if(delta == 0.0){
				optimalTransformations.add(selNodes);
			
//				Clustering cnew = new Clustering(initClustering);
////				if(selNodes.size()==1)
////					cnew.changeClusterOfNode(selNodes.get(0).getNodeId(), selNodes.get(0).getClusterId(), true);
////				else
//				cnew.changeClusterOfMultipleNodes(selNodes);
//				cnew.computeImbalance(adjMat);
//				System.out.println(cnew.toString());
			}
		}
		
		return(optimalTransformations);
	}
	
	
	
	

	
	
	public Set<Clustering> enumerateClusterings(ArrayList<ArrayList<TNode>> optimalTransformations){
		Set<Clustering> s = new HashSet<>();
		

				
		for(ArrayList<TNode> selNodes : optimalTransformations){
//			if(selNodes.size()>3 && selNodes.get(0).getNodeId() == 0 && selNodes.get(1).getNodeId() == 4 && selNodes.get(2).getNodeId() == 10 && selNodes.get(3).getNodeId() == 13 
//					&& selNodes.get(2).getTargetClusterId() == 10 && selNodes.get(3).getTargetClusterId() == 10
//					)
//						System.out.println("d");
			
			Clustering cnew = new Clustering(initClustering);
//			if(selNodes.size()==1)
//				cnew.changeClusterOfNode(selNodes.get(0).getNodeId(), selNodes.get(0).getClusterId(), true);
//			else
			//System.out.println(selNodes);
			//System.out.println(cnew.toString());
			cnew.changeClusterOfMultipleNodes(selNodes);
			cnew.computeImbalance(adjMat);
			//System.out.println(cnew.toString());
			//System.out.println("---");
			s.add(cnew);
		}
		//System.out.println(s.size());
		return(s);
	}
	
	
	
}


