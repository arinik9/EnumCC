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
import java.util.List;
import java.util.stream.Collectors;

import myUtils.Clustering;
import myUtils.EditDistance;
import enumeration.AbstractEnumeration;

public class MyExactPopulateHeuristic {

	String CPLEX_BIN_PATH = "/opt/ibm/ILOG/CPLEX_Studio128/cplex/bin/x86-64_linux";
	String clusterings_LB_AssocFileName = "assoc.txt";
	
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
	
	
	public MyExactPopulateHeuristic(int n_, double[][] adjMat_, int maxNbEdit_, String inputDirPath_, String graphFileName_,
			String outputDirPath_, int tilim_, String cplexBinPath_)
	{
		n = n_;
		adjMat = adjMat_;
		maxNbEdit = maxNbEdit_;
		inputDirPath = inputDirPath_;
		graphFileName = graphFileName_;
		//startSolutionDirPath = startSolutionDirPath_;
		outputDirPath = outputDirPath_;
		tilim = tilim_;
		CPLEX_BIN_PATH = cplexBinPath_;
		
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
		
//		int[] membership11 = readMembership("/home/nejat/eclipse/workspace-neon/TEMP/out/net-error-maxNbEdit=3/All", 7, adjMat.length);
//		Clustering c11 = new Clustering(membership11, -1);
//		int[] membership33 = readMembership("/home/nejat/eclipse/workspace-neon/TEMP/out/net-error-maxNbEdit=3/All", 12, adjMat.length);
//		Clustering c33 = new Clustering(membership33, -1);
//		EditDistance ed = new EditDistance();
//		int d11 = ed.calculateEditDistance(c11, c33);
		
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

//////		int[] membership11 = readMembership("/home/nejat/eclipse/workspace-neon/TEMP/out/net", 0, adjMat.length);
//////		Clustering c11 = new Clustering(membership11, -1);
//////		int[] membership22 = readMembership("/home/nejat/eclipse/workspace-neon/TEMP/out/net", 1, adjMat.length);
//////		Clustering c22 = new Clustering(membership22, -1);
//		int[] membership33 = readMembership("/home/nejat/eclipse/workspace-neon/TEMP/out/net", 1, adjMat.length);
//		Clustering c33 = new Clustering(membership33, -1);
////////		EditDistance ed = new EditDistance();
////////		int d11 = ed.calculateEditDistance(c11, c22);
////////		int d22 = ed.calculateEditDistance(c11, c33);
////////		int d33 = ed.calculateEditDistance(c22, c33);
//////
//////		
////		int[] membership44 = readMembership("/home/nejat/eclipse/workspace-neon/TEMP/out/net", 12, adjMat.length);
////		Clustering c44 = new Clustering(membership44, -1);
////		EditDistance ed = new EditDistance();
////		int d11 = ed.calculateEditDistance(c33, c44);
////		
//		ArrayList<Clustering> clusterings = new ArrayList<Clustering>();
//		for(int i=0; i<26; i++){
//			int[] membership = readMembership("/home/nejat/eclipse/workspace-neon/TEMP/out/net/All", i, adjMat.length);
//			Clustering c = new Clustering(membership, -1);
//			c.computeImbalance(adjMat);
//			System.out.println(c);
//			clusterings.add(c);
//		}
//////////		
//////////		ArrayList<Clustering> clusterings2 = new ArrayList<Clustering>();
//////////		for(int i=0; i<19; i++){
//////////			int[] membership = readMembership("/home/nejat/eclipse/workspace-neon/EnumCC/out/net/1", i, adjMat.length);
//////////			Clustering c = new Clustering(membership, -1);
//////////			c.computeImbalance(adjMat);
//////////			clusterings2.add(c);
//////////		}
//////////		
//		int minI = -1;
//		int minD = 700;
//		System.out.println("!!!!!  AMK !!!!!");
//		Clustering issue = null;
//		//for(Clustering c1 : clusterings2){
//			int counter = -1;
//			for(Clustering c2 : clusterings){
//				counter += 1;
//				EditDistance ed = new EditDistance();
//				int d11 = ed.calculateEditDistance(c33, c2);
//				System.out.println("c:"+counter+" => "+d11);
//
//				if(d11<minD){
//					minD = d11;
//					issue = c2;
//					minI = counter;
//				}
//			}
//			//if(!ok){
//				System.out.println("ISSUEE !!!!!!!!!!!");
//			//}
//		//}


		// ===========================================================================================================
		ArrayList<Integer> LBs = new ArrayList<Integer>();
		while(true){
			passCounter++;
			System.out.println("-------------------------");
			System.out.println("passCounter: " + passCounter);
			
//			if(passCounter==2)
//				break;
		
			// ===================================================
			// 1) enumerate other optimal clusterings based on 1-Edit, ... 'maxNbEdit', starting from the reference/seed clustering
			// ===================================================
			String clusteringResultFileName = "membership"+nbSeedSolutionCounter+".txt";
			String clusteringResultFilePath = outputDirPath+"/"+clusteringResultFileName;

//			int[] membership = readClusteringExCCResult(clusteringResultFilePath, n);
			int[] membership = readMembership(outputDirPath,nbSeedSolutionCounter);
			Clustering c1 = new Clustering(membership, -1);
			c1.computeImbalance(adjMat);
			System.out.println(c1);
			System.out.println("------------------");
			
			
//			for(int i=0; i<63; i++){
//				if(c1.equals(clusterings.get(i))){
//					System.out.println("Curr Clustering Id in solution space: "+i);
//					break;
//				}
//			}
			

//			int[] membership2 = readMembership(outputDirPath+"/../net-all-networks",19);
//			Clustering c2 = new Clustering(membership2, -1);
//			System.out.println(c2);
//			
//			int[] membership3 = readMembership(outputDirPath+"/../net-all-networks",28);
//			Clustering c3 = new Clustering(membership3, -1);
//			System.out.println(c3);

			
			
			Clustering currRefClustering = new Clustering(membership, -1);
			currRefClustering.computeImbalance(adjMat);
			System.out.println("imbalance: " + currRefClustering.getImbalance());
//			discoveredClusterings.add(currRefClustering);
//			discoveredClusteringFilePaths.add(clusteringResultFilePath);
//			LBs.add(1); //+1 because we want to see a new solution
			
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
				
//				for(Clustering c : e.foundClusterings){
//					for(int i=0; i<63; i++){
//						if(c.equals(clusterings.get(i))){
//							System.out.println("!!! new found Clustering Id in solution space: "+i);
//							break;
//						}
//					}
//				}
				
				
				for(Clustering c : e.foundClusterings){
					String resultFileName = "membership"+c.getId()+".txt";
					discoveredClusteringFilePaths.add(passOutputDirPath+"/"+resultFileName);
					discoveredClusterings.add(c);
					LBs.add(1);
				}
				
			} else { // at least use the info ofthe init solution
				discoveredClusterings.add(currRefClustering);
				discoveredClusteringFilePaths.add(clusteringResultFilePath);
				LBs.add(1); //+1 because we want to see a new solution
			}
			
			writeClusterings_LB_AssocFileName(outputDirPath, clusterings_LB_AssocFileName, discoveredClusteringFilePaths, LBs);
			
			

			
			// ===================================================
			// 3) run DistCC based on already found seed clusterings
			// ===================================================
			try {
				Files.copy(new File(inputDirPath+"/"+graphFileName).toPath(), new File(outputDirPath+"/"+graphFileName).toPath(), 
						StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
			List<String> cmdArgsDistCC = buildDistCCCommand(JAR_filepath_DistCC, CPLEX_BIN_PATH, outputDirPath, graphFileName, clusterings_LB_AssocFileName, outputDirPath,
					-1, true, false, n, false, false); // no time limit, since the aim is to cover all solution space
			String cmdDistCC = cmdArgsDistCC.stream()
				      .collect(Collectors.joining(" "));
			runCommand(cmdDistCC);
	
			
			// ===================================================
			// 4) check if DistCC finds any optimal solution. If no solution exists, then stop. Otherwise, continue with the new one
			// ===================================================
			// TODO: another error handler: cplex may yield a clustering file where each is a single node cluster
			oldfile = new File(outputDirPath+"/"+"ExCC-result.txt");
			if(oldfile.exists() && !oldfile.isDirectory()) {
				nbSeedSolutionCounter++;

                membership = readClusteringExCCResult(outputDirPath+"/"+"ExCC-result.txt", n);
			    Clustering c = new Clustering(membership, nbSeedSolutionCounter);
                c.writeMembership(outputDirPath);
                System.out.println("DistCC result:"+c);
                oldfile.delete();

				//oldfile.renameTo(new File(outputDirPath+"/"+"sol"+nbSeedSolutionCounter+".txt"));
				oldfile = new File(outputDirPath+"/"+"logcplex.txt");
				//oldfile.delete();
				oldfile = new File(outputDirPath+"/"+"log.txt");
				oldfile.delete();
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
	
	
//	private int computeBinaryHammingDistance(Clustering c1, Clustering c2){
//		int dist = 0;
//		
//		for(int i=1; i<n; i++){
//			for(int j=0; j<i; j++){
//				if(c1.membership[i]==c1.membership[j] && c2.membership[i]!=c2.membership[j])
//					dist++;
//				else if(c1.membership[i]!=c1.membership[j] && c2.membership[i]==c2.membership[j])
//					dist++;
//			}
//		}
//		
//		return(dist);
//	}
//	
//	
//	
//	private Clustering findSolutionInCenterOfSolutionClass(Set<Clustering> foundClusterings){
//		Clustering clusteringInCenter = null;
//		int minOfMaxDistances = Integer.MAX_VALUE;
//		
//		int counter=0;
//		Iterator<Clustering> itr = foundClusterings.iterator(); // for each solution, find its max dist towards other solutions		
//		while(itr.hasNext()){
//			//System.out.println("counter:" + counter);
//
//			Clustering c = itr.next();
//			int maxDist = getMaxEditDistanceBetweenOtherSolutions(c, foundClusterings, counter);
//			if(maxDist<minOfMaxDistances){
//				minOfMaxDistances = maxDist;
//				clusteringInCenter = c;
//			}
//			
//			counter++;
//		}
//		
//		return(clusteringInCenter);
//	}
//	
//	
//	
//	private int getMaxEditDistanceBetweenOtherSolutions(Clustering refClustering, Set<Clustering> foundClusterings, int parCounter){
//		int maxDist = 0;
//		
//		Iterator<Clustering> itr = foundClusterings.iterator(); // current enumerated clusterings		
//		int counter = 0;
//		while(itr.hasNext()){
//			//System.out.println("counter2:" + counter);
//			//System.out.println("refClustering:" + refClustering);
//
//				
//			Clustering c = itr.next();
//			//System.out.println("curr Clustering:" + c);
//
//			EditDistance eDist = new EditDistance();
//			int dist = eDist.calculateEditDistance(refClustering, c);
//			
//			if(dist > maxDist)
//				maxDist = dist;
//			//System.out.println("--------------");
//			counter++;
//		}
//		
//		return(maxDist);
//	}
	
	
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
	
	
	
	
	
	private void writeClusterings_LB_AssocFileName(String outputDirPath, String outFileName, ArrayList<String> clusteringFileNames, ArrayList<Integer> LBs){
		String filepath = outputDirPath + "/" + outFileName;
		
		String content = "";
		for(int i=0; i<clusteringFileNames.size(); i++){ // for each node
			if(!content.equals(""))
				content += "\n";
			content += LBs.get(i)+":"+clusteringFileNames.get(i);
		}
			
		try{
			 BufferedWriter writer = new BufferedWriter(new FileWriter(filepath));
			 writer.write(content);
			 writer.close();
		 } catch(IOException ioe){
		     System.out.print("Erreur in writing output file: ");
		     ioe.printStackTrace();
		 }
	}
	
	
	
	
	
//	private List<String> buildExCCCommand(String JAR_filepath_ExCC, String CplexBinPath, String inFilePath, String outDir, int tilim, boolean isCP, boolean lazyInBB,
//			int MaxTimeForRelaxationImprovement, boolean userCutInBB, boolean isReducedTriangleConstraints, boolean enumAll){
//		List<String> cmdArgsExCC = new ArrayList<>();
//		cmdArgsExCC.add("java");
//		cmdArgsExCC.add("-Djava.library.path=" + CplexBinPath);
//		cmdArgsExCC.add("-DinFile="+ inFilePath);
//		cmdArgsExCC.add("-DoutDir=" + outDir);
//		cmdArgsExCC.add("-Dcp=" + isCP);
//		cmdArgsExCC.add("-Dtilim=" + tilim);
//		cmdArgsExCC.add("-DlazyInBB=" + lazyInBB);
//		
//		cmdArgsExCC.add("-DMaxTimeForRelaxationImprovement=" + MaxTimeForRelaxationImprovement);
//		cmdArgsExCC.add("-DuserCutInBB=" + userCutInBB);
//		cmdArgsExCC.add("-DisReducedTriangleConstraints=" + isReducedTriangleConstraints);
//		cmdArgsExCC.add("-DenumAll=" + enumAll);
//		cmdArgsExCC.add("-jar " + JAR_filepath_ExCC);
//
//		return(cmdArgsExCC);
//	}
	
	
	private List<String> buildDistCCCommand(String JAR_filepath_DistCC, String CplexBinPath, String inputDirPath, String graphFileName, String clusterings_LB_AssocFileName,
			String outDir, int tilim, boolean isCP, boolean lazyInBB, int MaxTimeForRelaxationImprovement, boolean userCutInBB,
			boolean isReducedTriangleConstraints){
		List<String> cmdArgsDistCC = new ArrayList<>();
		cmdArgsDistCC.add("java");
		cmdArgsDistCC.add("-Djava.library.path=" + CplexBinPath);
		cmdArgsDistCC.add("-DinputDirPath="+ inputDirPath);
		cmdArgsDistCC.add("-DgraphFileName="+ graphFileName);
		cmdArgsDistCC.add("-Dclusterings_LB_AssocFileName="+ clusterings_LB_AssocFileName);
		cmdArgsDistCC.add("-DoutDir=" + outDir);
		cmdArgsDistCC.add("-Dcp=" + isCP);
		cmdArgsDistCC.add("-Dtilim=" + tilim);
		cmdArgsDistCC.add("-DlazyInBB=" + lazyInBB);
		cmdArgsDistCC.add("-DMaxTimeForRelaxationImprovement=" + MaxTimeForRelaxationImprovement);
		cmdArgsDistCC.add("-DuserCutInBB=" + userCutInBB);
		cmdArgsDistCC.add("-DisReducedTriangleConstraints=" + isReducedTriangleConstraints);
		cmdArgsDistCC.add("-jar " + JAR_filepath_DistCC);
		
		return(cmdArgsDistCC);
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
