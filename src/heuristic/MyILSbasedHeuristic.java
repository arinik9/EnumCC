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

public class MyILSbasedHeuristic {

	int n;
	double[][] adjMat;
	int tilim = 3600;
	String inputDirPath = "";
	String graphFileName = "";
	String outputDirPath = ".";
	int maxNbEdit = 1;
	
	int nbSeedSolutionCounter;
	int passCounter;
	
	String ILSExecutablePath = "";
	int nbIterILS = 1;
	
	String JAR_filepath_DistCC = "lib/DistCC.jar";
	int NB_THREAD = 6;
	
	ArrayList<Clustering> discoveredClusterings;
	ArrayList<String> discoveredClusteringFilePaths;
	
	
	
	public MyILSbasedHeuristic(String ILSExecutablePath_, int nbIterILS_, int n_, double[][] adjMat_, int maxNbEdit_, String inputDirPath_, String graphFileName_,
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
		
		ILSExecutablePath = ILSExecutablePath_;
		nbIterILS = nbIterILS_;
		
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
		Clustering c0 = new Clustering(membership, -1);
		nextClusterings.add(c0);
		c0.computeImbalance(adjMat);
		double optImbValue = c0.getImbalance();
		
		
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
			// 3) try to find diverse clusterings based on metaheuristics => perturb the existing ones
			// ===================================================
			
			ArrayList<Clustering> currDiverseClusterings = new ArrayList<>();
			
			
			try {
				Files.copy(new File(inputDirPath+"/"+graphFileName).toPath(), new File(outputDirPath+"/"+graphFileName).toPath(), 
						StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
		
			for(int iterNo=1; iterNo<nbIterILS; iterNo++){
				
				// e.foundClusterings
				// choose a random solution
				
				String IterILSOutputDirPath = outputDirPath+"/"+"ILS"+iterNo;
				new File(IterILSOutputDirPath).mkdirs();
				
				String initPartitionFilePath = outputDirPath+"/"+clusteringResultFileName;
				boolean initPartitionsFromFileForAllIters = true;
				int tilim = 60;
				int neighborhoodSize = 1;
				double alpha = 0.4;
				int perturbationLevelMax = 30;
				List<String> cmdArgsILSCC = buildILSCCCommand(ILSExecutablePath, outputDirPath, graphFileName, IterILSOutputDirPath,
						10, tilim, alpha, neighborhoodSize, perturbationLevelMax,
						initPartitionsFromFileForAllIters, initPartitionFilePath); // TODO those last 2 params are not used for now
				String cmdILSCC = cmdArgsILSCC.stream()
					      .collect(Collectors.joining(" "));
				System.out.println(cmdILSCC);
				runCommand(cmdILSCC);
				
				File[] directories = new File(IterILSOutputDirPath+"/"+graphFileName).listFiles(File::isDirectory);
				String tempDirPath = directories[0].getAbsolutePath();
				String resultFilePath = tempDirPath+"/"+"cc-result.txt";
				
				try {
					Files.copy(new File(resultFilePath).toPath(), new File(IterILSOutputDirPath+"/"+"cc-result.txt").toPath(), 
							StandardCopyOption.REPLACE_EXISTING);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				File[] files = new File(tempDirPath).listFiles();
				for (File f: files) f.delete();
				new File(tempDirPath).delete();
				new File(IterILSOutputDirPath+"/"+graphFileName).delete();
				
				int[] resultMemberhsip = readClusteringILSCCResult(IterILSOutputDirPath+"/"+"cc-result.txt", n);
				new File(resultFilePath).delete();
				Clustering cnew = new Clustering(resultMemberhsip, -1);
				cnew.computeImbalance(adjMat);
				//System.out.println(cnew);
				new File(IterILSOutputDirPath).delete();
				files = new File("./logs").listFiles();
				for (File f: files) f.delete();
				new File("./logs").delete();
				
				if(optImbValue == cnew.getImbalance())
					currDiverseClusterings.add(cnew);
			}
			

			
			
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
	

	
	
	


	
	
	/**
	 * This method reads a clustering result file.
	 * 
	 * @param filename  input clustering filename
	 * @param n: nb node in the graph
	 * @return 
	 */
	private int[] readClusteringILSCCResult(String fileName, int n) {
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
				  String[] items = line.split(": \\[ ");
				  if(items.length > 1){
					  String line2 = items[1].replaceAll(" \\]" , "");
					  String[] items2 = line2.split(" ");
					  for(int i=0; i<items2.length; i++){
						  int id = Integer.parseInt(items2[i]);
						  membership[id] = clusterIdCounter;
					  }
					  clusterIdCounter++;
				  }
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
	
	
	private void runCommand(String cmd){
		Process p;
		try {
			String line;
			p = Runtime.getRuntime().exec(cmd);
			System.out.println(cmd+"\nWaiting ...");
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			  while ((line = input.readLine()) != null) {
			    //System.out.println(line); //==> if you want to see console output, decomment this line
			}
			input.close();
			//int exitVal = p.waitFor();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private List<String> buildILSCCCommand(String ILSExecutablePath, String inputDirPath, String graphFileName,
			String outDir, int nbIterILS, int tilim, double alpha, int neighborhoodSize, int perturbationLevelMax,
			boolean initPartitionsFromFileForAllIters, String initPartitionFilePath) {

		
		String inputFilepath = inputDirPath+"/"+graphFileName;
		
		List<String> cmdArgsILSCC = new ArrayList<>();
		cmdArgsILSCC.add(ILSExecutablePath);
		cmdArgsILSCC.add("--vns false");
		cmdArgsILSCC.add("--rcc false");
		cmdArgsILSCC.add("--alpha " + alpha);
		cmdArgsILSCC.add("--iterations " + nbIterILS);
		cmdArgsILSCC.add("--time-limit " + tilim);
		// see if neighborhood_size=2 is time consuming
		cmdArgsILSCC.add("--neighborhood_size " + neighborhoodSize); // up to 'r' vertices in P which are moving from one cluster into another
		cmdArgsILSCC.add("--input-file "+ inputFilepath);
		cmdArgsILSCC.add("--output-folder "+ outDir);
		cmdArgsILSCC.add("--gain-function-type 0");
		cmdArgsILSCC.add("--strategy ILS");
		cmdArgsILSCC.add("--perturbationLevelMax "+perturbationLevelMax); // default value is 30

//		cmdArgsILSCC.add("--init-partition-file "+initPartitionFilePath); // default value is 30
//		cmdArgsILSCC.add("--initPartitionsFromFileForAllIters "+initPartitionsFromFileForAllIters); // default value is 30
		
		return(cmdArgsILSCC);
	}
	
	
}
