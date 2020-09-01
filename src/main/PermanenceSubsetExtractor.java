package main;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import myUtils.Clustering;
import myUtils.DirectedGraph;
import myUtils.MyCGraph;
import permanence.MovingDependance;

public class PermanenceSubsetExtractor {

	public static void main(String[] args) {
		
		String inputDirPath = "";
		String graphFileName = "";
		String inputMembership = "";
		int nbEdit = 1;
		int subsetMaxSize = 12;
		int maxNbDistinctDeltaFitnessForPossibleTargetClusters = 3;

		
		if( !System.getProperty("inputDirPath").equals("${inputDirPath}") )
			inputDirPath = System.getProperty("inputDirPath");
		else {
			System.out.println("inputDirPath is not specified. Exit");
			return;
		}

		if( !System.getProperty("graphFileName").equals("${graphFileName}") )
			graphFileName = System.getProperty("graphFileName");
		else {
			System.out.println("graphFileName is not specified. Exit");
			return;
		}
		
		if( !System.getProperty("inputMembership").equals("${inputMembership}") )
			inputMembership = System.getProperty("inputMembership");
		else {
			System.out.println("inputMembership is not specified. Exit");
			return;
		}
		
		if(!System.getProperty("nbEdit").equals("${nbEdit}") )
			nbEdit = Integer.parseInt(System.getProperty("nbEdit"));

		if(!System.getProperty("subsetMaxSize").equals("${subsetMaxSize}") )
			subsetMaxSize = Integer.parseInt(System.getProperty("subsetMaxSize"));

		if(!System.getProperty("maxNbDistinctDeltaFitnessForPossibleTargetClusters").equals("${maxNbDistinctDeltaFitnessForPossibleTargetClusters}") )
			maxNbDistinctDeltaFitnessForPossibleTargetClusters = Integer.parseInt(System.getProperty("maxNbDistinctDeltaFitnessForPossibleTargetClusters"));
		
//		System.out.println("===============================================");
//		System.out.println("inputDirPath: " + inputDirPath);
//		System.out.println("graphFileName: " + graphFileName);
//		System.out.println("startSolutionDirPath: " + startSolutionDirPath);
//		System.out.println("startSolutionId: " + startSolutionId);
//		System.out.println("nbEdit: " + nbEdit);
//		System.out.println("subsetMaxSize: " + subsetMaxSize);
//		System.out.println("maxNbDistinctDeltaFitnessForPossibleTargetClusters: " + maxNbDistinctDeltaFitnessForPossibleTargetClusters);
//		System.out.println("===============================================");
		
		double[][] adjMat = createAdjMatrixFromInput(inputDirPath + "/" + graphFileName);

		// TODO actually, we need to check if the file exists
		
		String[] items = inputMembership.replaceAll("\\[", "").replaceAll("\\]", "").replaceAll("\\s", "").split(",");
		int[] membership = new int[items.length];
		for (int i = 0; i < items.length; i++) {
		    try {
		    	membership[i] = Integer.parseInt(items[i]);
		    } catch (NumberFormatException nfe) {
		        //NOTE: write something here if you need to recover from formatting errors
		    };
		}
		Clustering initClustering = new Clustering(membership, -1);

		MyCGraph g = new MyCGraph(adjMat.length, initClustering);
		g.fillInNodeToWeightedDegreeSum(adjMat);
		
		ArrayList<ArrayList<Integer>> subsetList = new ArrayList<ArrayList<Integer>>();
		for(int levelNo=0; levelNo<nbEdit; levelNo++){
            System.out.print("level:"+levelNo+";");
			MovingDependance movDep = new MovingDependance(g, adjMat, initClustering, nbEdit, levelNo, 
					subsetMaxSize, maxNbDistinctDeltaFitnessForPossibleTargetClusters);
			DirectedGraph diG = movDep.buildMovingDependenceGraph();
            
            String content = "[";
            for(int i=1; i<adjMat.length; i++)
                content += movDep.possibleTargetClusterIdListArray[i-1]+", ";
            content += movDep.possibleTargetClusterIdListArray[adjMat.length-1]+"]";

            System.out.print("possibleTargetClusterIds:"+content+";");
            System.out.print("subsets:"+movDep.prepareResult(diG, levelNo));
			//subsetList.addAll(movDep.prepareResult(diG, levelNo));

            if(levelNo<(nbEdit-1))
                System.out.print("&");
		}
		//System.out.print(subsetList);
		
		//System.out.println("end");
	}

	
	
	
	
	/**
	 * This method reads input graph file, then stocks it as weighted adjacency matrix, 
	 * finally writes the graph in lower triangle format into a temp file.
	 * 
	 * @param filename  input graph filename
	 * @return 
	 */
	//private static double[][] createAdjMatrixFromInput(String fileName, boolean isReducedTriangleConstraints) {
	private static double[][] createAdjMatrixFromInput(String fileName) {
		
		  double[][] weightedAdjMatrix = null;
		  int n = -1;
		// =====================================================================
		// read input graph file
		// =====================================================================
		try{
		  InputStream  ips=new FileInputStream(fileName);
		  InputStreamReader ipsr=new InputStreamReader(ips);
		  BufferedReader   br=new
		  BufferedReader(ipsr);
		  String ligne;
		  
		  ligne = br.readLine();
		  
		  /* Get the number of nodes from the first line */
		  n = Integer.parseInt(ligne.split("\t")[0]);
		  

		  weightedAdjMatrix = new double[n][n];
		  if(weightedAdjMatrix[0][0] != 0.0d)
			  System.out.println("Main: Error default value of doubles");
		  
		  /* For all the other lines */
		  while ((ligne=br.readLine())!=null){
			  
			  String[] split = ligne.split("\t");
			  
			  if(split.length >= 3){
				  int i = Integer.parseInt(split[0]);
				  int j = Integer.parseInt(split[1]);
				  double v = Double.parseDouble(split[2]);
				  weightedAdjMatrix[i][j] = v;
				  weightedAdjMatrix[j][i] = v;
			  }
			  else
				  System.err.println("All the lines of the input file must contain three values" 
						+ " separated by tabulations"
						+ "(except the first one which contains two values).\n"
				  		+ "Current line: " + ligne);
		  }
		  br.close();
		}catch(Exception e){
		  System.out.println(e.toString());
		  n = -1;
		}
		// end =================================================================
		
		return(weightedAdjMatrix);
	}
	
	
	/**
	 * read a solution from file
	 * 
	 */
	public static int[] readMembership(String inputDirPath, long id_, int n){
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
