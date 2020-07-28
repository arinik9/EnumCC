package myUtils;

import java.util.ArrayList;

public class EditDistance {

	public EditDistance(){
	}
	
	
	public int calculateEditDistance(Clustering c1, Clustering c2){
		int[] relMem = obtainRelativeMembership(c1, c2);
		Clustering Crel = new Clustering(relMem, 0);
		//System.out.println("Crel:" + Crel);
		int edit = calculateEditDistance(c1.membership, relMem);
		return(edit);
	}
	
	/*
	 *  An example in which the relative membership can have empty cluster ids for max matching for the ref clustering (here, cluster 9 is empty)
	 * refClustering:(-1) Cluster1: [2, 3, 17, 25]
	 *		Cluster2: [4, 6]
	 *		Cluster3: [5, 12, 18, 26]
	 *		Cluster4: [0, 23, 24]
	 *		Cluster5: [14]
	 *		Cluster6: [9, 16]
	 *		Cluster7: [1, 19, 27]
	 *		Cluster8: [22]
	 *		Cluster9: [11, 20]
	 *		Cluster10: [10, 13, 15, 21]
	 *		Cluster11: [7, 8]
	 *		
 	 *	curr Clustering:(-1) Cluster1: [2, 3, 17, 25]
	 *		Cluster2: [5, 12, 18, 26]
	 *		Cluster3: [7, 23]
	 *		Cluster4: [8, 14]
	 *		Cluster5: [9, 16, 24]
	 *		Cluster6: [1, 19, 27]
	 *		Cluster7: [0, 4, 6, 21]
	 *		Cluster8: [10, 11, 22]
	 *		Cluster9: [13, 15, 20]
	 *		
	 *	Crel:(0) Cluster1: [2, 3, 17, 25]
	 *		Cluster2: [0, 4, 6, 21]
	 *		Cluster3: [5, 12, 18, 26]
	 *		Cluster4: [7, 23]
	 *		Cluster5: [8, 14]
	 *		Cluster6: [9, 16, 24]
	 *		Cluster7: [1, 19, 27]
	 *		Cluster8: [10, 11, 22]
	 *		Cluster10: [13, 15, 20]
	 *
	 * 
	 */
	// asymmetric method
	public int[] obtainRelativeMembership(Clustering c1, Clustering c2){
		// https://cstheory.stackexchange.com/questions/6569/edit-distance-between-two-partitions
		// this is an assignment problem, which can be solved optimally with Hungarian algorithm
		int n = c1.n;
		int[] relMem = new int[n];
		for(int i=0; i<n; i++)
			relMem[i] = 0; // init
		
		int k1 = c1.getNbCluster();
		ArrayList<ArrayList<Integer>> clustersRef = c1.getClustersInArrayFormat();
		ArrayList<ArrayList<Integer>> clustersRel = c2.getClustersInArrayFormat();

		int k2 = c2.getNbCluster();
		
		double[][] contingencyTable = new double[k1][k2];

		for(int i=0; i<k1; i++){
			ArrayList<Integer> clusterRef = clustersRef.get(i);
			for(int j=0; j<k2; j++){
				ArrayList<Integer> clusterRel = clustersRel.get(j);
				int nbComon = countNbCommonElement(clusterRef, clusterRel);
				contingencyTable[i][j] = -1*nbComon; // since Hungarian is a minimization problem, we negate the weights for a maximization
			}
		}
		
		HungarianAlgorithm algo = new HungarianAlgorithm(contingencyTable);
		int[] result = algo.execute();
//		System.out.println("worker labels:");
//		for(int i=0; i<10; i++){
//			System.out.println(i+":"+(result[i]+1));
//		}
		
		// result[0]=3 means that the first cluster of reference clustering matches the 4th cluster of the other clustering
		// so we assign the cluster id 1 to the elements of the 4th cluster of the other clustering
		boolean[] processedRelClusters = new boolean[k2];
		for(int clusterIdRef=1; clusterIdRef<=k1; clusterIdRef++){
			if(result[clusterIdRef-1] != -1){ //-1 means there is no assignment
				int targetClusterId = result[clusterIdRef-1]+1; //+1 since cluster labels start from 0
				processedRelClusters[targetClusterId-1] = true;
				ArrayList<Integer> clusterRel = clustersRel.get(targetClusterId-1);
				for(Integer elt : clusterRel){
					relMem[elt] = clusterIdRef;
				}
			}
		}

		// ===============================
		
		// post processing for non-assigned clusters of the other clustering (not ref clustering)
		// this is the case when k1 < k2
		for(int clusterIdRel=1; clusterIdRel<=k2; clusterIdRel++){
			if(!processedRelClusters[clusterIdRel-1]){
				ArrayList<Integer> clusterRel = clustersRel.get(clusterIdRel-1);
				k1++;
				for(Integer elt : clusterRel)
					relMem[elt] = k1;
			}
		}
		
		return(relMem);
	}
	
	
	public int calculateEditDistance(int[] mem1, int[] mem2){
		int edit = 0;
		int n = mem1.length;
		
		for(int i=0; i<n; i++){
			if(mem1[i]!=mem2[i])
				edit++;
		}
		
		return(edit);
	}
	
	
	// symmetric method
	public int countNbCommonElement(ArrayList<Integer> a1, ArrayList<Integer> a2){
		int count = 0;
		
		for(int elt1 : a1){ 
			if(a2.contains(elt1))
				count++;
		}
		
		return(count);
	}
	
}
