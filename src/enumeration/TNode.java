package enumeration;

import java.util.ArrayList;
import java.util.function.Predicate;

import myUtils.MyCGraph;

public class TNode {

	int nodeId;
	int clusterId;
	int targetIndex; // this is used to know which nodes will be in the same target cluster
	int targetClusterId;
	double currFitness;
	double targetFitness;
	ArrayList<Predicate<Integer>> notEqualToConstraints;
	Predicate<Integer> equalToConstraint;

	
	public TNode(int nodeId_, int clusterId_, int targetIndex_, int targetClusterId_, ArrayList<Integer> notEqualToValues){
		this.nodeId = nodeId_;
		this.clusterId = clusterId_;
		this.targetIndex = targetIndex_;
		this.targetClusterId = targetClusterId_;
		notEqualToConstraints = new ArrayList<>();
		for(Integer val : notEqualToValues){
			notEqualToConstraints.add(n -> n != val);
		}
	}
	
	public TNode(int nodeId_, int clusterId_, int targetIndex_, ArrayList<Integer> notEqualToValues){
		this.nodeId = nodeId_;
		this.clusterId = clusterId_;
		this.targetIndex = targetIndex_;
		this.targetClusterId = -1;
		notEqualToConstraints = new ArrayList<>();
		for(Integer val : notEqualToValues){
			notEqualToConstraints.add(n -> n != val);
		}
	}
	
	
	public TNode(TNode ncopy){
		this.nodeId = ncopy.nodeId;
		this.clusterId = ncopy.clusterId;
		this.targetIndex = ncopy.targetIndex;
		this.targetClusterId = ncopy.targetClusterId;
		notEqualToConstraints = new ArrayList<>();
		for(Predicate<Integer> p : ncopy.notEqualToConstraints){
			notEqualToConstraints.add(p);
		}
		
		this.currFitness = ncopy.currFitness;
		this.targetFitness = ncopy.targetFitness;
		this.targetIndex = ncopy.targetIndex;
	}  
	
	public void addNotEqualToConstraints(ArrayList<Integer> notEqualToValues){
		for(Integer val : notEqualToValues){
			notEqualToConstraints.add(n -> n != val);
		}
	}
    
	
	public void substractFromCurrNodeFitness(double value){
		currFitness -= value;
	}
	
	public void substractFromTargetNodeFitness(double value){
		targetFitness -= value;
	}
	
	// neighbors: the nodes sharing the same cluster with this.nodeId
	public void computeCurrNodeFitness(MyCGraph g, double[][] adjMat){ // ArrayList<TNode> neighbors
		currFitness = g.weightSumInClusters[this.nodeId][this.clusterId-1];
//		for(TNode neigh : neighbors){
//			substractFromCurrNodeFitness(adjMat[this.nodeId][neigh.nodeId]);
//		}
	}
	
	// neighbors: the nodes sharing the same cluster with this.nodeId
	public void computeTargetNodeFitness(MyCGraph g, double[][] adjMat){ // ArrayList<TNode> neighbors
		// TODO a small trick here: this.targetClusterId == 0, this means that the node will create its single node cluster
		int targetClusterId = this.targetClusterId;
		if(this.targetClusterId == 0)
			targetClusterId = g.c.getNbCluster(); // object 'c' is the init clustering, and the last cluster in init clustering is intentionaly empty for this situation
		targetFitness = g.weightSumInClusters[this.nodeId][targetClusterId-1];
//		for(TNode neigh : neighbors){
//			substractFromTargetNodeFitness(adjMat[this.nodeId][neigh.nodeId]);
//		}
	}
	
	public double calculateDeltaFitness(){
		return(this.targetFitness-this.currFitness);
	}

	public int getNodeId() {
		return nodeId;
	}

	public void setNodeId(int nodeId) {
		this.nodeId = nodeId;
	}

	public int getClusterId() {
		return clusterId;
	}

	public void setClusterId(int clusterId) {
		this.clusterId = clusterId;
	}
	
	public int getTargetIndex() {
		return targetIndex;
	}
	
	public void setTargetIndex(int targetIndex) {
		this.targetIndex = targetIndex;
	}

	public int getTargetClusterId() {
		return targetClusterId;
	}

	public void setTargetClusterId(int targetClusterId) {
		this.targetClusterId = targetClusterId;
	}

	public double getCurrFitness() {
		return currFitness;
	}

	public void setCurrFitness(double currFitness) {
		this.currFitness = currFitness;
	}

	public double getTargetFitness() {
		return targetFitness;
	}

	public void setTargetFitness(double targetFitness) {
		this.targetFitness = targetFitness;
	}
	
	
	@Override
    public String toString() { 
		String content = "(nodeId:" + nodeId + ", clusterId:" + clusterId + ", targetClusterId:" + targetClusterId + ", targetIndex:" + targetIndex + ")";
		// String content = "(nodeId:" + nodeId + ", delta:" + calculateDeltaFitness() + ", targetClusterId:" + targetClusterId + ")";
		//String content = "(nodeId:" + nodeId + ", currFitness:" + currFitness + ", targetFitness:" + targetFitness + ", clusterId:" + clusterId + ", targetClusterId:" + targetClusterId + ")";

        return String.format(content); 
    }
	
}
