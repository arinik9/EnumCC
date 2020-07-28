package heuristic;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import myUtils.ArrayOperations;
import myUtils.Clustering;
import myUtils.Combination;
import myUtils.EditDistance;
import myUtils.MyCGraph;
import enumeration.AbstractEnumeration;

import mincut.StoerWagnerGlobalMincut;
import myUtils.EdgeWeightedGraph;

public class MySplitMergeHeuristic {

	int n;
	double[][] adjMat;
	int tilim = 3600;
	String inputDirPath = "";
	String graphFileName = "";
	String outputDirPath = ".";
	int maxNbEdit = 1;
	
	int nbSeedSolutionCounter;
	int passCounter;
	
	String JAR_filepath_DistCC = "lib/DistCC.jar";
	int NB_THREAD = 6;
	
	ArrayList<Clustering> discoveredClusterings;
	ArrayList<String> discoveredClusteringFilePaths;
	
	
	
	
	public MySplitMergeHeuristic(int n_, double[][] adjMat_, int maxNbEdit_, String inputDirPath_, String graphFileName_,
			String outputDirPath_, int tilim_)
	{
		n = n_;
		adjMat = adjMat_;
		maxNbEdit = maxNbEdit_;
		inputDirPath = inputDirPath_;
		graphFileName = graphFileName_;
		//startSolutionDirPath = startSolutionDirPath_;
		outputDirPath = outputDirPath_;
		tilim = tilim_;
		
		nbSeedSolutionCounter = 0;
		passCounter = 0;
		
		discoveredClusterings = new ArrayList<>();
		discoveredClusteringFilePaths = new ArrayList<>();

	}
	
	
	// TODO control time limit in each component
	public void run(AbstractEnumeration e){
		discoveredClusterings.clear();
		discoveredClusteringFilePaths.clear();
		long startTime = System.currentTimeMillis();
		
//		String JAR_filepath_ExCC = "lib/ExCC.jar";
//		String inputFilePath = inputDirPath + "/" + graphFileName;
//		List<String> cmdArgsExCC = buildExCCCommand(JAR_filepath_ExCC, CPLEX_BIN_PATH, inputFilePath, outputDirPath, 600, true, false, n/3, false, false, false);
//		String cmdExCC = cmdArgsExCC.stream()
//			      .collect(Collectors.joining(" "));
//		runCommand(cmdExCC);
//		
//		File oldfile = new File(outputDirPath+"/"+"ExCC-result.txt");
//		oldfile.renameTo(new File(outputDirPath+"/"+"sol"+nbSeedSolutionCounter+".txt"));
//		oldfile = new File(outputDirPath+"/"+"logcplex.txt");
//		oldfile.delete();
//		oldfile = new File(outputDirPath+"/"+"log.txt");
//		oldfile.delete();
		
		File oldfile; // = new File(outputDirPath+"/"+"ExCC-result.txt");

		
//		ArrayList<Clustering> clusterings = new ArrayList<Clustering>();
//		for(int i=0; i<63; i++){
//			int[] membership = readMembership("out/net-all-networks", i, adjMat.length);
//			Clustering c = new Clustering(membership, -1);
//			c.computeImbalance(adjMat);
//			clusterings.add(c);
//		}
		
		HashSet<Clustering> nextClusterings = new HashSet<>();
		int[] membership = readMembership(outputDirPath,nbSeedSolutionCounter);
		Clustering c1 = new Clustering(membership, -1);
		nextClusterings.add(c1);
		
		
		// ===========================================================================================================
		while(nextClusterings.size()>0){
			passCounter++;
			System.out.println("-------------------------");
			System.out.println("passCounter: " + passCounter);
			
//			if(passCounter==2)
//				break;
		
			// ===================================================
			// 1) enumerate other optimal clusterings based on 1-Edit, ... 'maxNbEdit', starting from the reference/seed clustering
			// ===================================================
			
			Iterator<Clustering> iter = nextClusterings.iterator();
			Clustering currRefClustering = iter.next();
			currRefClustering.computeImbalance(adjMat);
			System.out.println(currRefClustering);
			iter.remove(); // remove the first element
			System.out.println("imbalance: " + currRefClustering.getImbalance());
			String clusteringResultFileName = "membership"+nbSeedSolutionCounter+".txt";
			currRefClustering.writeMembership(outputDirPath, clusteringResultFileName);
			

			
			// ===================
			e.reset();
			String passOutputDirPath = outputDirPath+"/"+passCounter;
			new File(passOutputDirPath).mkdirs();
			e.enumerate(currRefClustering, passOutputDirPath);
			// ===================
			
			
			
			

			// ===================================================
			// 2) 
			// ===================================================
			if(e.foundClusterings.size()>0){
				
				for(Clustering c : e.foundClusterings){
					discoveredClusterings.add(c);
				}
				
			} else { // at least use the info ofthe init solution
				discoveredClusterings.add(currRefClustering);
			}
			

			
			// ===================================================
			// 3) try to find diverse clusterings based on merge/split
			// ===================================================
			ArrayList<Clustering> currDiverseClusterings = new ArrayList<>();
//			int[][] distMatrix = calculateEditDistanceMatrix(discoveredClusterings);
//			ArrayList<Clustering> currDiverseSeed = chooseDiverseSeed(distMatrix, discoveredClusterings);
			
			
//			for(Clustering tempp : e.foundClusterings){
//				tempp.computeImbalance(adjMat);
//				System.out.println("e.foundClusterings  " + tempp);
//			}
			
			// TODO: merge
			ArrayList<Clustering> newClusterings1 = generateDiverseClusteringsWithMergeOperation(e.foundClusterings);
			//ArrayList<Clustering> newClusterings1 = generateDiverseClusteringsWithMergeOperation(currDiverseSeed);
			for(Clustering cnew1 : newClusterings1){
				if(!currDiverseClusterings.contains(cnew1))
					currDiverseClusterings.add(cnew1);
			}
			
			
			// TODO: split
			ArrayList<Clustering> newClusterings2 = generateDiverseClusteringsWithSplitOperation(e.foundClusterings);
			//ArrayList<Clustering> newClusterings2 = generateDiverseClusteringsWithSplitOperation(currDiverseSeed);
			for(Clustering cnew2 : newClusterings2){
				if(!currDiverseClusterings.contains(cnew2))
					currDiverseClusterings.add(cnew2);
			}
			
			
			// ===================================================
			// 4) try to find diverse clusterings based on metaheuristics => perturb the existing ones
			// ===================================================
			// TODO
			
			
			
//			// ===================================================
			// 5) check if any optimal solution is found. If no solution exists, then stop. Otherwise, continue with the new one(s)
			// ===================================================
			if(currDiverseClusterings.size()>0) {
				System.out.println("!!!! Yes, some solutions exist !!!! => "+discoveredClusterings.size());

				boolean notAnyDifferent = true;
				for(Clustering cand : currDiverseClusterings){ // choose only 1 clustering for the next step
					if(!discoveredClusterings.contains(cand)){
						cand.computeImbalance(adjMat);
						System.out.println(cand);
						nextClusterings.add(cand);
						notAnyDifferent = false;
						break;
					}
				}
				if(notAnyDifferent)
					break;
				
				nbSeedSolutionCounter++;

			} else {
				// No Solution Exist
				System.out.println("No Solution Exist! Quiting ... ");
				break;
			}
			
		}
		
		
		long endTime = System.currentTimeMillis();
		double execTime = (float) (endTime-startTime)/1000;
		System.out.println("execution time: " + execTime + "s");
	}
	
	
	
	
	
	
	
	
	private void displayNodeFitnessValueForClusters(MyCGraph g, int nodeId, int nbClusters){
		System.out.println("node id:"+nodeId + ", nb clusters:"+nbClusters);
		String content = "";
		for(int cluId=1; cluId<nbClusters; cluId++){
			content += "(cluId:"+cluId+", f:"+ g.weightSumInClusters[nodeId][cluId-1]+"), ";
		}
		content += "(cluId:"+nbClusters+", f:"+ g.weightSumInClusters[nodeId][nbClusters-1]+")";
		System.out.println(content);
	}
	
	
	private boolean isNodePlacementStationary(MyCGraph g, int nodeId, int currClusterId, int nbClusters){
		double currFitnessValue = g.weightSumInClusters[nodeId][currClusterId-1];
		for(int cluId=1; cluId<=nbClusters; cluId++){
			if(g.weightSumInClusters[nodeId][cluId-1]>currFitnessValue)
				return(false);
		}
		return(true);
	}
	
	
	private int[][] calculateEditDistanceMatrix(ArrayList<Clustering> clusterings){
		int m = clusterings.size();
		int[][] mtrx = new int[m][m];
		for(int i=0; i<m; i++){
			mtrx[i][i] = 0;
			//System.out.println(i);
			for(int j=0; j<i; j++){
				EditDistance eDist = new EditDistance();
				int nbEdit = eDist.calculateEditDistance(clusterings.get(i), clusterings.get(j));
				mtrx[i][j] = nbEdit;
				mtrx[j][i] = nbEdit;
			}
		}
		
		return(mtrx);
	}
	
	
	
	// for now, pick only 2 diverse clusterings
	private ArrayList<Clustering> chooseDiverseSeed(int[][] distMatrix, ArrayList<Clustering> clusterings){
		ArrayList<Clustering> diverseSeed = new ArrayList<Clustering>();
		int m = clusterings.size();
		int maxDist = 0;
		
		// find the most distant clustering pair
		// maybe later we can use VI to distinguish a set of the most distant clusterings
		// also, we can later find more than 2 diverse clusterings
		for(int i=1; i<m; i++){
			for(int j=0; j<i; j++){
				if(distMatrix[i][j]>maxDist){
					maxDist = distMatrix[i][j];
					diverseSeed.clear();
					diverseSeed.add(clusterings.get(i));
					diverseSeed.add(clusterings.get(j));
				}
			}
		}
		
		return(diverseSeed);
	}
	
	
	
	
	// recursive level hesaba katayim mi?
	private ArrayList<Clustering> generateDiverseClusteringsWithSplitOperation(Set<Clustering> seedDiverseclusterings){
		ArrayList<Clustering> newDiverseOptimalClusterings = new ArrayList<>();
		ArrayList<Clustering> newCandidateClusterings = new ArrayList<>();
		int nbMinimalClusterSizeForSplit = 8; // TODO t be adjusted
		
		for(Clustering c : seedDiverseclusterings){
			//System.out.println("------------------");
			//System.out.println(c);
			ArrayList<ArrayList<Integer>> clusters = c.getClustersInArrayFormat();
			int nbClusters = c.getNbCluster();
			
			MyCGraph cgraph = new MyCGraph(n, c);
			cgraph.fillInNodeToWeightedDegreeSum(adjMat);
			
			for(ArrayList<Integer> clusterNodeIds : clusters){ // for each cluster
				
				if(clusterNodeIds.size()>=nbMinimalClusterSizeForSplit){
			
					EdgeWeightedGraph G = new EdgeWeightedGraph(adjMat, clusterNodeIds, true);
					StoerWagnerGlobalMincut mc = new StoerWagnerGlobalMincut(G);
					//System.out.println("Min cut weight = " + mc.weight());
					ArrayList<Integer> S = new ArrayList<>();
					ArrayList<Integer> T = new ArrayList<>();
				    for (int i=0; i<clusterNodeIds.size(); i++) {
					    if (mc.cut(i)) S.add(clusterNodeIds.get(i));
					    else T.add(clusterNodeIds.get(i));
					}
				    
				    // we want that the cluster to be splited be of less size, compared to the other cluster
				    ArrayList<Integer> temp;
				    if(S.size()>T.size()) {
				    	temp = new ArrayList<>(T);
				    	T = new ArrayList<>(S);
				    	S = new ArrayList<>(temp);
			    	}
				    
				    
				    if(S.size()>3){
					    // compute sum edge weight between the node sets S and T
					    double sum = 0.0;
					    for(int s : S)
					    	for(int t : T)
					    		sum += adjMat[s][t];
					    //System.out.println("sum in split: " + sum);
					    
					    
					    // if sum > 0, this means that the nb pos links > the nb neg links
					    if(sum == 0){ // note that what we do here is if the new clustering is also optimal by looking at the change in fitness
						    Clustering cnew = new Clustering(c);
						    for(Integer v : S){ // for each of the cut nodes
						    	int nodeClusterId = c.membership[v];
						    	//displayNodeFitnessValueForClusters(cgraph, v, nbClusters);
						    	//System.out.println("is node placement optimal: " + isNodePlacementStationary(cgraph, v, nodeClusterId, nbClusters));
						    	boolean renumber = false; // since we will create a new cluster, no need to renumbering
						    	int newClusterId = c.getNbCluster()+1; // the last cluster is empty for a new cluster
						    	cnew.changeClusterOfNode(v, newClusterId, renumber);
						    }
						    newCandidateClusterings.add(cnew);
					    }
				    
				    }
			    
				}
			    
			}
			
		}
		
		
		// TODO a generic method for split and merge, which tries to find opt sol with edit ops
		// input  : newCandidateClusterings
		// output : newDiverseOptimalClusterings
		newDiverseOptimalClusterings.addAll(newCandidateClusterings); // TODO this part of the method can be improved later

		
		return(newDiverseOptimalClusterings);
	}
	

	
	// recursive level hesaba katayim mi?
	// simdilik 2 cluster merge ediyoruz
	private ArrayList<Clustering> generateDiverseClusteringsWithMergeOperation(Set<Clustering> seedDiverseclusterings){
		ArrayList<Clustering> newDiverseOptimalClusterings = new ArrayList<>();
		ArrayList<Clustering> newCandidateClusterings = new ArrayList<>();
		int nbMinimalNodeForMerge = 5; // both clusters have to be of size at least 5 => TODO this number can be adjusted
		
		for(Clustering c : seedDiverseclusterings){
			//System.out.println("------------------");
			//System.out.println(c);
			ArrayList<ArrayList<Integer>> clusters = c.getClustersInArrayFormat();
			int nbClusters = c.getNbCluster();
			
			MyCGraph cgraph = new MyCGraph(n, c);
			cgraph.fillInNodeToWeightedDegreeSum(adjMat);

			int[] clusterIds = ArrayOperations.seq(1, nbClusters);
			int nbClusterToMerge = 2;
			List<int[]> combClusterIds = Combination.generate(clusterIds, nbClusterToMerge);
			
			for(int[] clusterIdPair : combClusterIds){
				int clusterId1 = clusterIdPair[0];
				int clusterId2 = clusterIdPair[1];
				int minSize = clusters.get(clusterId1-1).size();
				if(minSize>clusters.get(clusterId2-1).size())
					minSize = clusters.get(clusterId2-1).size();
				
				if(minSize>=nbMinimalNodeForMerge){
				    // compute sum edge weight between the two clusters
				    double sum = 0.0;
				    for(int s : clusters.get(clusterId1-1))
				    	for(int t : clusters.get(clusterId2-1))
				    		sum += adjMat[s][t];
				    //System.out.println("clusterId1:"+clusterId1+", clusterId2:"+clusterId2+" => sum in merge:" + sum);
				    
					// if sum > 0, this means that the nb pos links > the nb neg links
				    if(sum == 0){ // note that what we do here is if the new clustering is also optimal by looking at the change in fitness
						Clustering cnew = new Clustering(c);
						// place the nodes of clusterId1 into clusterId2
					    for(Integer v : clusters.get(clusterId1-1)){ // for each of the cut nodes
					    	boolean renumber = true; // since we will create a new cluster, no need to renumbering
					    	cnew.changeClusterOfNode(v, clusterId2, renumber);
					    }
					    newCandidateClusterings.add(cnew);
					}
			    
				}
				
			}
		
		}
		
		
		
		
		// TODO a generic method for split and merge, which tries to find opt sol with edit ops
		// input  : newCandidateClusterings
		// output : newDiverseOptimalClusterings
		newDiverseOptimalClusterings.addAll(newCandidateClusterings); // TODO this part of the method can be improved later
		
		return(newDiverseOptimalClusterings);
	}

	
	
	/**
	 * This method reads a clustering result file.
	 * 
	 * @param filename  input clustering filename
	 * @param n: nb node in the graph
	 * @return 
	 */
	private int[] readClusteringExCCResult(String fileName, int n) {
	    int[] membership = null;
	    
		try{
			  InputStream  ips=new FileInputStream(fileName);
			  InputStreamReader ipsr=new InputStreamReader(ips);
			  BufferedReader   br=new
			  BufferedReader(ipsr);
			  String line;

			  membership = new int[n];
			  if(membership[0] != 0)
				  System.out.println("Main: Error default value of int");
			  
			  int clusterIdCounter = 1;
			  /* For all the lines */
			  while ((line=br.readLine())!=null){
				  String line2 = line.replaceAll("\\[|\\]" , "");
				  String[] items = line2.split(", ");
				  for(int i=0; i<items.length; i++){
					  int id = Integer.parseInt(items[i]);
					  membership[id] = clusterIdCounter;
				  }
				  clusterIdCounter++;
			  }
			  br.close();
			  
			}catch(Exception e){
			  System.out.println(e.toString());
			}
		
		return(membership);
	}
	
	
	/**
	 * read a solution from file
	 * 
	 */
	public int[] readMembership(String inputDirPath, long id_){
		String fileName = "membership" + id_ + ".txt";
		String filepath = inputDirPath + "/" + fileName;
		int[] membership_ = new int[this.n];
		
		try{
			InputStream  ips = new FileInputStream(filepath);
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br = new BufferedReader(ipsr);
			String line;
			  
			for(int i=0; i<n; i++){ // for each node
				line = br.readLine();
				membership_[i] = Integer.parseInt(line);	
			}
			
			line = br.readLine();
			br.close();
			
			// verify that the file we just read corresponds to a correct nb node
			if(line != null){
				return(null);
			}
		
		}catch(Exception e){
		  System.out.println(e.toString());
		  return(null);
		}
		
		return(membership_);
	}
	
	
	
	
	/**
	 * read a solution from file
	 * 
	 */
	public int[] readMembership(String inputDirPath, long id_, int n){
		String fileName = "membership" + id_ + ".txt";
		String filepath = inputDirPath + "/" + fileName;
		int[] membership_ = new int[n];
		
		try{
			InputStream  ips = new FileInputStream(filepath);
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br = new BufferedReader(ipsr);
			String line;
			  
			for(int i=0; i<n; i++){ // for each node
				line = br.readLine();
				membership_[i] = Integer.parseInt(line);	
			}
			
			line = br.readLine();
			br.close();
			
			// verify that the file we just read corresponds to a correct nb node
			if(line != null){
				return(null);
			}
		
		}catch(Exception e){
		  System.out.println(e.toString());
		  return(null);
		}
		
		return(membership_);
	}
	
	
}
