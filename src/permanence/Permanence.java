package permanence;

import java.util.ArrayList;

import myUtils.Clustering;
import myUtils.MyCGraph;

public class Permanence {
	Clustering c;
	double[][] adjMat;
	MyCGraph g;
	int n;
	ArrayList<ArrayList<Integer>> clusters;

	
	public Permanence(double[][] adjMat_, MyCGraph g_) {
		c = g_.c;
		g = g_;
		adjMat = adjMat_;
		n = g.n;
		clusters = c.getClustersInArrayFormat();
	}
	
	public double[] computePermananceScores(){
		double[] scores = new double[this.n];
		
		double[] signedCluCoefs = computeSignedClusteringCoefs();
		double[] Ivalues = computeWeightedInternalDegreeSum();
		double[] EmaxValues = computeEmaxValues();
		double[] SintValues = computeSintValues();
		double[] SextValues = computeSextValues();
		
		for(int i=0; i<this.n; i++){
			double penalizationTerm = (1-signedCluCoefs[i]);
			double pullOffTerm = 0.0;
			double D = SintValues[i] + SextValues[i];
			double internalStrength = (float) Ivalues[i]/D;
			if(EmaxValues[i] != 0.0)
				pullOffTerm = (float) 1/EmaxValues[i];
			
			scores[i] = (float) internalStrength*pullOffTerm - penalizationTerm;
		}
		return(scores);
	}
	
	
	public ArrayList<Integer> getInternalNeighbors(int nodeId, ArrayList<Integer> currCluster){
		ArrayList<Integer> neighs = new ArrayList<>();
		for(Integer otherNodeId : currCluster){
			if(nodeId!=otherNodeId && adjMat[nodeId][otherNodeId]!=0.0)
				neighs.add(otherNodeId);
		}
		return(neighs);
	}
	
	
	
	public double[] computeSignedClusteringCoefs(){
		double[] coefs = new double[this.n];
		
		for(int currClusterId=1; currClusterId<this.clusters.size(); currClusterId++){
			ArrayList<Integer> currCluster = this.clusters.get(currClusterId-1);
			
			for(Integer nodeId : currCluster){ // for each node of this cluster
				coefs[nodeId] = computeSignedClusteringCoefForNode(nodeId, currCluster);
			}
		}
		return(coefs);
	}

	
	
	public double computeSignedClusteringCoefForNode(int nodeId, ArrayList<Integer> currCluster){
		double result = 0.0;
		ArrayList<Integer> neighs = getInternalNeighbors(nodeId, currCluster);
		double nominator = 0.0;
	    double denominator = 0.0;
			    
		if(neighs.size()>1){
			for(int i=0; i<(neighs.size()-1); i++){
				for(int j=i; i<neighs.size(); i++){
					int neighNodeId1 = neighs.get(i);
					int neighNodeId2 = neighs.get(j);
					
					nominator += this.adjMat[nodeId][neighNodeId1]*this.adjMat[nodeId][neighNodeId2]*this.adjMat[neighNodeId1][neighNodeId2];
	                denominator += Math.abs(this.adjMat[nodeId][neighNodeId1]*this.adjMat[nodeId][neighNodeId2]);
				}
			}
			result = (float) nominator/denominator;
		}
		return(result);
	}

	
	
	public double[] computeWeightedInternalDegreeSum(){
		double[] results = new double[this.n];
		
		for(int currClusterId=1; currClusterId<this.clusters.size(); currClusterId++){
			ArrayList<Integer> currCluster = this.clusters.get(currClusterId-1);
			
			for(Integer nodeId : currCluster){ // for each node of this cluster
				results[nodeId] = this.g.weightSumInClusters[nodeId][currClusterId-1];
			}
		}
		return(results);
	}
	
	
	
	public int[] findTargetClusterIdsWithEmax(){
		int[] targetClusterIds = new int[this.n];
		
		for(int currClusterId=1; currClusterId<=this.clusters.size(); currClusterId++){
			ArrayList<Integer> currCluster = this.clusters.get(currClusterId-1);
			
			for(Integer nodeId : currCluster){ // for each node of this cluster
				targetClusterIds[nodeId] = findTargetClusterIdWithEmaxForNode(nodeId, currClusterId);
			}
		}
		return(targetClusterIds);
	}
	
	
	public int findTargetClusterIdWithEmaxForNode(int nodeId, int currClusterId){
		int targetClusterId = -1;
		double targetFitness = -Double.MAX_VALUE;
		
		for(int candTargetClusterId=1; candTargetClusterId<=this.clusters.size(); candTargetClusterId++){
			if(candTargetClusterId != currClusterId){
				double candTargetFitness = this.g.weightSumInClusters[nodeId][candTargetClusterId-1];
				if(candTargetFitness > targetFitness){
					targetFitness = candTargetFitness;
					targetClusterId = candTargetClusterId;
				}
			}
		}
		return(targetClusterId);
	}
	
	
	public double computeMappingForEmax(double EmaxValue){
		if(EmaxValue == 0.0)
			return(1.0);
		else if(EmaxValue<0)
			return(Math.pow(2, EmaxValue)+1);
		return(EmaxValue);
	}
	
	
	public double[] computeEmaxValues(){
		double[] EmaxValues = new double[this.n];
		int[] targetClusterIds = findTargetClusterIdsWithEmax();
		
		for(int currClusterId=1; currClusterId<=this.clusters.size(); currClusterId++){
			ArrayList<Integer> currCluster = this.clusters.get(currClusterId-1);
			
			for(Integer nodeId : currCluster){ // for each node of this cluster
				int targetClusterId = targetClusterIds[nodeId];
				EmaxValues[nodeId] = computeMappingForEmax(this.g.weightSumInClusters[nodeId][targetClusterId-1]);
			}
		}
		return(EmaxValues);
	}

	
	public double[] computeSextValues(){
		double[] SextValues = new double[this.n];
		int[] targetClusterIds = findTargetClusterIdsWithEmax();
		
		for(int currClusterId=1; currClusterId<=this.clusters.size(); currClusterId++){
			ArrayList<Integer> currCluster = this.clusters.get(currClusterId-1);
			
			for(Integer nodeId : currCluster){ // for each node of this cluster
				int targetClusterId = targetClusterIds[nodeId];
				SextValues[nodeId] = computeMappingForEmax(this.g.absWeightSumInClusters[nodeId][targetClusterId-1]);
			}
		}
		return(SextValues);
	}
	
	
	public double[] computeSintValues(){
		double[] SintValues = new double[this.n];
		for(int i=0; i<this.n; i++)
			SintValues[i] = 0.0;
		
		for(int currClusterId=1; currClusterId<=this.clusters.size(); currClusterId++){
			ArrayList<Integer> currCluster = this.clusters.get(currClusterId-1);
			
			for(Integer nodeId : currCluster){ // for each node of this cluster
				ArrayList<Integer> neighs = getInternalNeighbors(nodeId, currCluster);
				for(Integer neighId : neighs){
					SintValues[nodeId] += Math.abs(this.adjMat[nodeId][neighId]);
				}
			}
		}
		return(SintValues);
	}
	
}
