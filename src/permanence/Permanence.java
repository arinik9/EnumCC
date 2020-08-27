package permanence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import myUtils.Clustering;
import myUtils.MyCGraph;

// source: Li et al. Community Detection in Complicated Network based on the Multi-view Weighted Signed Permanence. 2016
public class Permanence {
	Clustering c;
	double[][] adjMat;
	MyCGraph g;
	int n;
	ArrayList<ArrayList<Integer>> clusters;
	final int MAX_CLUSTER_SIZE_FOR_HIGH_PENALIZATION = 4;
	final double HIGH_PENALIZATION_VALUE = 1.5;
	
	public Permanence(double[][] adjMat_, MyCGraph g_) {
		c = g_.c;
		g = g_;
		adjMat = adjMat_;
		n = g.n;
		clusters = c.getClustersInArrayFormat();
	}
	
	// returns sorted array
	public PermanenceData[] computePermananceScores(){
		PermanenceData[] results = new PermanenceData[this.n];
		
		double[] signedCluCoefs = computeSignedClusteringCoefs();
		double[] Ivalues = computeWeightedInternalDegreeSum();
		double[] EmaxValues = computeEmaxValues(); // Emax values are not transformed, as opposed to Li et al.
		double[] SintValues = computeSintValues();
		double[] SextValues = computeSextValues();
		
		int[] itsClusterSize = new int[this.n];
		for(ArrayList<Integer> cluster : clusters){
			for(int i : cluster)
				itsClusterSize[i] = cluster.size();
		}
		
		for(int i=0; i<this.n; i++){
			double penalizationTerm = HIGH_PENALIZATION_VALUE;
			if(itsClusterSize[i]>MAX_CLUSTER_SIZE_FOR_HIGH_PENALIZATION)
				penalizationTerm = (1-signedCluCoefs[i]);
			double D = SintValues[i] + SextValues[i];
			double score = (float) (Ivalues[i]-EmaxValues[i])/D - penalizationTerm;
			results[i] = new PermanenceData(i, score);
		}
		
//		System.out.println("signedCluCoefs!!");
//		List<Double> list1 = new ArrayList<Double>();
//		for(Double a:signedCluCoefs) {
//	         list1.add(a);
//	      }
//		System.out.println(list1);
////
//		System.out.println("Perm scores!!");
//		List<Double> list2 = new ArrayList<Double>();
//		for(PermanenceData a : results) {
//	         list2.add(a.getPermScore());
//	      }
//		System.out.println(list2);
		
		Arrays.sort(results);
		return(results);
	}
	
	
	// Li et al. 2016. Community Detection in Complicated Network based on the Multi-view Weighted Signed Permanence
//	public double[] computePermananceScores(){
//		double[] scores = new double[this.n];
//		
//		double[] signedCluCoefs = computeSignedClusteringCoefs();
//		double[] Ivalues = computeWeightedInternalDegreeSum();
//		double[] EmaxValues = computeEmaxValues();
//		double[] EmaxTransfValues = new double[EmaxValues.length];
//		for(int i=0; i<EmaxValues.length; i++)
//			EmaxTransfValues[i] = computeMappingForEmax(EmaxValues[i]);
//			
//		double[] SintValues = computeSintValues();
//		double[] SextValues = computeSextValues();
//		
//		for(int i=0; i<this.n; i++){
//			double penalizationTerm = (1-signedCluCoefs[i]);
//			double pullOffTerm = 0.0;
//			double D = SintValues[i] + SextValues[i];
//			double internalStrength = (float) Ivalues[i]/D;
//			if(EmaxValues[i] != 0.0)
//				pullOffTerm = (float) 1/EmaxTransfValues[i];
//			
//			scores[i] = (float) internalStrength*pullOffTerm - penalizationTerm;
//		}
//		
//		return(scores);
//	}
	
	
	public ArrayList<Integer> getNeighbors(int nodeId, ArrayList<Integer> currCluster){
		ArrayList<Integer> neighs = new ArrayList<>();
		for(Integer otherNodeId : currCluster){
			if(nodeId!=otherNodeId && adjMat[nodeId][otherNodeId]!=0.0)
				neighs.add(otherNodeId);
		}
		return(neighs);
	}
	
	
	
	public double[] computeSignedClusteringCoefs(){
		double[] coefs = new double[this.n];
		
		for(int currClusterId=1; currClusterId<=this.clusters.size(); currClusterId++){
			ArrayList<Integer> currCluster = this.clusters.get(currClusterId-1);
			
			for(Integer nodeId : currCluster){ // for each node of this cluster
				coefs[nodeId] = computeSignedClusteringCoefForNode(nodeId, currCluster);
			}
		}
		return(coefs);
	}

	
	
	public double computeSignedClusteringCoefForNode(int nodeId, ArrayList<Integer> currCluster){
		double result = 0.0;
		ArrayList<Integer> neighs = getNeighbors(nodeId, currCluster);
		double nominator = 0.0;
	    double denominator = 0.0;
			    
		if(neighs.size()>1){
			for(int i=0; i<(neighs.size()-1); i++){
				for(int j=i+1; j<neighs.size(); j++){
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
		
		for(int currClusterId=1; currClusterId<=this.clusters.size(); currClusterId++){
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
				targetClusterIds[nodeId] = findTargetClusterIdWithEmaxForNode(nodeId, currClusterId);// it returns -1 for an empty cluster
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
//				if(candTargetFitness>targetFitness && candTargetFitness>0){
				if(candTargetFitness>targetFitness){
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
				double w = 0;
				w = this.g.weightSumInClusters[nodeId][targetClusterId-1];
				EmaxValues[nodeId] = w;
//				EmaxValues[nodeId] = computeMappingForEmax(w);
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
//				if(targetClusterId!=-1){
					ArrayList<Integer> targetCluster = this.clusters.get(targetClusterId-1);
					ArrayList<Integer> neighs = getNeighbors(nodeId, targetCluster);
					for(Integer neighId : neighs){
						SextValues[nodeId] += Math.abs(this.adjMat[nodeId][neighId]);
					}
//				} else {
//					SextValues[nodeId] = 0.0;
//				}
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
				ArrayList<Integer> neighs = getNeighbors(nodeId, currCluster);
				for(Integer neighId : neighs){
					SintValues[nodeId] += Math.abs(this.adjMat[nodeId][neighId]);
				}
			}
		}
		return(SintValues);
	}
	
	// ==================================
	
	public class PermanenceData implements Comparable<PermanenceData> {
		int nodeId;
		double permScore;
		
		public PermanenceData(int nodeId_, double permScore_){
			nodeId = nodeId_;
			permScore = permScore_;
		}
		
		public int getNodeId(){
			return(nodeId);
		}
		
		public double getPermScore(){
			return(permScore);
		}

		@Override
		public int compareTo(PermanenceData o) {
			double res = this.permScore - o.permScore;
			if(res<0)
				return(-1);
			else if(res>0)
				return(1);
			return 0;
		}

	}
}
