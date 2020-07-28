package enumeration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import myUtils.Clustering;
import myUtils.Combination;
import myUtils.MyCGraph;
import myUtils.Permutation;

public abstract class AbstractEdit {

	
	Clustering initClustering;
	int n;
	MyCGraph g;
	//long idCounter;
	double[][] adjMat;
	public Set<Clustering> foundClusterings;
	int[] clusterIds;
	int nbSourceCluster=0;
	
	//public MyGenericEnumeration(int nbEdit_, double[][] adjMat_, Clustering initClustering_, long idCounter_){
	public AbstractEdit(double[][] adjMat_, Clustering initClustering_){
		initClustering = initClustering_;
		n = initClustering.n;
		g = new MyCGraph(n, initClustering);
		g.fillInNodeToWeightedDegreeSum(adjMat_);
		//idCounter = idCounter_;
		adjMat = adjMat_;
		foundClusterings = new HashSet<Clustering>();
	}
	
	
	public abstract void enumerate();
	
	
	public void setNbSourceCluster(int nbSourceCluster_){
		this.nbSourceCluster = nbSourceCluster_;
	}
	
	
	public void setClusterIds(int[] clusterIds_){
		this.clusterIds = new int[clusterIds_.length];
		for(int i=0; i<clusterIds_.length; i++)
			this.clusterIds[i] = clusterIds_[i];
	}
	
	
	
	public ArrayList<ArrayList<TNode>> filterByDecomposabe1EditOptimalTransformations(ArrayList<ArrayList<TNode>> optimalTransformations){
		ArrayList<ArrayList<TNode>> subset = new ArrayList<ArrayList<TNode>>();

		for(ArrayList<TNode> selNodes : optimalTransformations){
			boolean nonDecomposable = true;
			for(TNode node : selNodes){
				TNode newNode = new TNode(node);
				newNode.computeCurrNodeFitness(g, adjMat);
				newNode.computeTargetNodeFitness(g, adjMat);
				double delta = newNode.calculateDeltaFitness();
				if(delta == 0.0){ // if the delta of any of them is zero, this means that it is a decomposable 1-Edit transformation
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
	public ArrayList<ArrayList<TNode>> findCandidateTransformations(ArrayList<TNode> selNodes){
		ArrayList<ArrayList<TNode>> optimalTransformations = new ArrayList<ArrayList<TNode>>();


		
				
		// 1) we know which node will be together in their target clusters.
		//	Based on this, compute the change of fitness caused by the links between moving nodes
		double change = 0;
		for(int i=0; i<selNodes.size(); i++){
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
					
					if(node.getClusterId() == otherNode.getClusterId() && !nodeTargetComparator.equals(otherNodeTargetComparator)){
						if(w<0) // now intra-negative link, but later inter-negative link
							change += w; // system gain
						else if(w>0) // now intra-positive link, but later inter-positive link
							change -= w; // system loss
					}
					
					if(node.getClusterId() != otherNode.getClusterId() && nodeTargetComparator.equals(otherNodeTargetComparator)){
						if(w<0) // now inter-negative link, but later intra-negative link
							change -= w; // system loss
						else if(w>0) // now inter-positive link, but later intra-positive link
							change += w; // system gain
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
		
			// 3.1) retrieve possible cluster ids
			List<Integer> remainingClusterIds = new ArrayList<>();
			for(int cid=1; cid<=(initClustering.getNbCluster()+1); cid++) // +1 for a new cluster
				remainingClusterIds.add(cid);
			
			for(TNode node : subsetNodes){
				if(node.notEqualToConstraints.size()>0){
					// chaining all predicates of a given node, if there are many
					Predicate<Integer> compositePredicate = node.notEqualToConstraints.stream().reduce(w -> true, Predicate::and); 
					remainingClusterIds = remainingClusterIds.stream()   // convert list to stream
			                .filter(compositePredicate)
			                .collect(Collectors.toList());
				}
			} // note that remainingClusterIds are common for all those subset nodes
			
			// 3.2) generate combinations of cluster ids of size 'nbTargetClusters'
			List<int[]> permCombs = new ArrayList<int[]>();
			List<int[]> combinations = Combination.generate(remainingClusterIds, nbTargetClustersToFind);
			for (int[] combination : combinations)
				permCombs.addAll(Permutation.permute(combination));
		
			
			// 4) for each combination, check if the global objective function remains unchanged, i.e. it is an optimal clustering ?
			for (int[] combination : permCombs) {
			    //System.out.println(Arrays.toString(combination));
				
				ArrayList<TNode> updatedNodes = new ArrayList<>();
				// update temporarily target cluster id, and then calculate the fitness of target cluster for this node
				double delta = change; // change comes from the step 1
				for(TNode node : selNodes){
					TNode newNode = new TNode(node);
					
					if(newNode.getTargetClusterId() == -1){
						newNode.setTargetClusterId(combination[newNode.getTargetIndex()-1]); // target indexes start from 1
						newNode.computeTargetNodeFitness(g, adjMat);
					}
					delta += newNode.calculateDeltaFitness();
					updatedNodes.add(newNode);
				}
				
				if(delta == 0.0)
					optimalTransformations.add(updatedNodes);
				
			}
			
		} else { // if(nbTargetClusters == 0){ ==> i.e. we know where to move all selected nodes
			
			double delta = change; // change comes from the step 1
			for(TNode node : selNodes)
				delta += node.calculateDeltaFitness();
			if(delta == 0.0)
				optimalTransformations.add(selNodes);
		}
		
		return(optimalTransformations);
	}
	
	
	
	public Set<Clustering> enumerateClusterings(ArrayList<ArrayList<TNode>> optimalTransformations){
		Set<Clustering> s = new HashSet<>();
		
		for(ArrayList<TNode> selNodes : optimalTransformations){
			Clustering cnew = new Clustering(initClustering);
//			if(selNodes.size()==1)
//				cnew.changeClusterOfNode(selNodes.get(0).getNodeId(), selNodes.get(0).getClusterId(), true);
//			else
			cnew.changeClusterOfMultipleNodes(selNodes);
			//System.out.println(cnew.toString());
			s.add(cnew);
		}
		//System.out.println(s.size());
		return(s);
	}
	
	
}
