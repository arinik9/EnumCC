package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import enumeration.AbstractEnumeration;
import enumeration.EstimateDiameterSolutionClass;
import enumeration.SearchAstar;
import enumeration.SearchForward2;
import heuristic.MyHybridHeuristic;
import mincut.StoerWagnerGlobalMincut;
import myUtils.Clustering;
import myUtils.EdgeWeightedGraph;
import myUtils.EditDistance;


/**
* The EnumCC program aims at enumerating optimal solutions: iteratively 1-Edit, 3-edit, 3-Edit, etc.
* <p>
* 
*
*/
public class HybridHeuristicWithDiameterEstimation {

	
	static String tempFile = "temp.txt";
	
	/**
	 * 
	 * Input parameters:
	 * <ul>
	 * <li> inFile (String): Input file path. </li>
	 * <li> outDir (String): Output directory path. Default "." 
	 * 		(i.e. the current directory). </li>
	 * <li> tilim (Integer): Time limit in seconds. </li>
	 * </ul>
	 * 

	 * 
	 * Example for Pure CPLEX approach:
	 * <pre>
	 * {@code
	 * ant clean jar
	 * ant ant -DinFile=data/signed.G -DoutDir=out/signed -Dcp=false -DenumAll=false run
	 * }
	 * </pre>
	 * 
	 * Example for Cutting Plane approach:
	 * <pre>
	 * {@code
	 * ant clean jar
	 * 
	 * ant ant -DinFile=data/signed.G -DoutDir=out/signed
	 * 	 -Dcp=true -Dtilim=3600 -DlazyInBB=false -DuserCutInBB=false run
	 * }
	 * </pre>
	 * 
	 * @param args  (Not used in this program. Instead, user parameters are obtained
	 * 	 through ant properties. See the buil.xml for more details).
	 * 
	 * @throws FileNotFoundException.
	 */
	public static void main(String[] args) throws FileNotFoundException {
		String CPLEX_BIN_PATH = "/opt/ibm/ILOG/CPLEX_Studio128/cplex/bin/x86-64_linux";
		
		int tilim = -1;
		String inputDirPath = "";
		String graphFileName = "";
		String startSolutionDirPath = "";
		String outputDirPath = ".";
		int maxNbEdit = 1;
		int nbRepetition = 1;
		System.out.println("___");
		System.out.println(System.getProperty("java.library.path"));
		if( !System.getProperty("java.library.path").equals("${java.library.path}") )
			CPLEX_BIN_PATH = System.getProperty("java.library.path");
		
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
		
		if( !System.getProperty("startSolutionDirPath").equals("${startSolutionDirPath}") )
			startSolutionDirPath = System.getProperty("startSolutionDirPath");
		else {
			System.out.println("startSolutionDirPath is not specified. Exit");
			return;
		}
		
		if( !System.getProperty("outDir").equals("${outDir}") )
			outputDirPath = System.getProperty("outDir");
		
		// Those 4 options are  available with cutting plane approach
		if(!System.getProperty("nbRepetition").equals("${nbRepetition}") )
			nbRepetition = Integer.parseInt(System.getProperty("nbRepetition"));
		
		if(!System.getProperty("maxNbEdit").equals("${maxNbEdit}") )
			maxNbEdit = Integer.parseInt(System.getProperty("maxNbEdit"));

		
		System.out.println("===============================================");
		System.out.println("CPLEX_BIN_PATH: " + CPLEX_BIN_PATH);
		System.out.println("inputDirPath: " + inputDirPath);
		System.out.println("graphFileName: " + graphFileName);
		System.out.println("startSolutionDirPath: " + startSolutionDirPath);
		System.out.println("outputDirPath: " + outputDirPath);
		System.out.println("maxNbEdit: " + maxNbEdit);
		System.out.println("nbRepetition: " + nbRepetition);
		System.out.println("===============================================");
		
		
		new File(outputDirPath).mkdirs();
		
		try {
			String startSolutionFileName="membership0.txt";
			Files.copy(new File(startSolutionDirPath+"/"+startSolutionFileName).toPath(), new File(outputDirPath+"/"+startSolutionFileName).toPath(), 
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		double[][] adjMat = createAdjMatrixFromInput(inputDirPath + "/" + graphFileName);
		
		int[] membership1 = readMembership(outputDirPath+"/1", 9, adjMat.length);
//		Clustering c1 = new Clustering(membership1, -1);
//		System.out.println(c1);
//		ArrayList<Integer> clusterNodeIds = c1.getClustersInArrayFormat().get(1); // get the first cluster
//
//		EdgeWeightedGraph G = new EdgeWeightedGraph(adjMat, clusterNodeIds, true);
//		StoerWagnerGlobalMincut mc = new StoerWagnerGlobalMincut(G);
//		System.out.println("---");
//	    for (int i=0; i<clusterNodeIds.size(); i++) {
//		    if (mc.cut(i)) System.out.println(clusterNodeIds.get(i) + " ");
//		}
//	    System.out.println();
//	    System.out.println("Min cut weight = " + mc.weight());
		
		// ================================

		
		
		// ==> only Edit1, Edit2 and Edit3
		AbstractEnumeration e = new EstimateDiameterSolutionClass(adjMat, maxNbEdit, nbRepetition); 
			
		MyHybridHeuristic heuristic = new MyHybridHeuristic(adjMat.length, adjMat, maxNbEdit, inputDirPath, graphFileName, outputDirPath, 
				tilim, CPLEX_BIN_PATH);
		heuristic.run(e);
		
		// ================================

//		int[] membership1 = readMembership(outputDirPath+"/1", 1, adjMat.length);
//		Clustering c1 = new Clustering(membership1, -1);
//		System.out.println(c1);
//
//		int[] membership2 = readMembership(outputDirPath+"/1", 170, adjMat.length);
//		Clustering c2 = new Clustering(membership2, -1);
//		System.out.println(c2);
		
//		int[] membership1 = readMembership(outputDirPath+"/1", 9, adjMat.length);
//		Clustering c1 = new Clustering(membership1, -1);
//		System.out.println(c1);
//
//		int[] membership2 = readMembership(outputDirPath+"/1", 16, adjMat.length);
//		Clustering c2 = new Clustering(membership2, -1);
//		System.out.println(c2);
		
//		EditDistance eDist = new EditDistance();
//		int nbEdit = eDist.calculateEditDistance(c1, c2);
//		System.out.println("nbEdit:"+nbEdit);
//		SearchAstar aStar = new SearchAstar(adjMat, 1, maxNbEdit);
//		aStar.findPathIfExists(c1, c2);
		
		
//		SearchForward2 searchForward2 = new SearchForward2(adjMat, 1, maxNbEdit);
//		searchForward2.findPathForward(c1);
		
		// ================================
		
//		ArrayList<Clustering> clusterings = new ArrayList<Clustering>();
//		for(int i=0; i<1093; i++){
//			int[] membership = readMembership(outputDirPath, i, adjMat.length);
//			Clustering c = new Clustering(membership, -1);
//			clusterings.add(c);
//		}
//		
//		//int[][] mtrx = new int[1093][1093];
//		String content = "";
//		for(int i=0; i<1093; i++){
//			System.out.println(i);
//			for(int j=0; j<1093; j++){
//				EditDistance eDist = new EditDistance();
//				int nbEdit = 0;
//				if(i!=j)
//					nbEdit = eDist.calculateEditDistance(clusterings.get(i), clusterings.get(j));
//				//mtrx[i][j] = nbEdit;
//				//mtrx[j][i] = nbEdit;
//				
//				if(j==0)
//					content += nbEdit;
//				else
//					content += ","+nbEdit;
//			}
//			content += "\n";
//		}
//		
//		
//		try{
//			 BufferedWriter writer = new BufferedWriter(new FileWriter(outputDirPath+"/editDist-matrix.csv"));
//			 writer.write(content);
//			 writer.close();
//		 } catch(IOException ioe){
//		     System.out.print("Erreur in writing output file: ");
//		     ioe.printStackTrace();
//		 }
		
		
		System.out.println("end");
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
		
		
		
//		
//		// 2nd phase =============================================================
//		// put negative epsilon values  when links are missing between nodes
//		if(isReducedTriangleConstraints && weightedAdjMatrix != null && n!=-1){
//			double w = (float) -1/n;
//			for(int i=1; i<weightedAdjMatrix.length; i++){
//				for(int j=0; j<i; j++){
//					//System.out.println("i:"+i+", j:"+j+", w:"+weightedAdjMatrix[i][j]);
//					if(weightedAdjMatrix[i][j] == 0.0d){
//						//System.out.println("i:"+i+", j:"+j+", w:"+weightedAdjMatrix[i][j]);
//						weightedAdjMatrix[i][j] = w;
//						weightedAdjMatrix[j][i] = w;
//					}
//				}
//			}
//		}
//		// end 2nd phase =================================================================
//
//
//		for(int i=1; i<weightedAdjMatrix.length; i++){
//			for(int j=0; j<i; j++){
//				//System.out.println("i:"+i+", j:"+j+", w:"+weightedAdjMatrix[i][j]);
//			}
//		}

		
//		// =====================================================================
//		// write into temp file (in lower triangle format)
//		// =====================================================================
//		if(weightedAdjMatrix != null){
//			 try{
//			     FileWriter fw = new FileWriter(tempFile, false);
//			     BufferedWriter output = new BufferedWriter(fw);
//
//			     for(int i = 1 ; i < weightedAdjMatrix.length ; ++i){
//			    	 String s = "";
//			    	 
//			    	 for(int j = 0 ; j < i ; ++j) // for each line, iterate over columns
//			    		 s += weightedAdjMatrix[i][j] + " ";
//
//			    	 s += "\n";
//			    	 output.write(s);
//			    	 output.flush();
//			     }
//			     
//			     output.close();
//			 }
//			 catch(IOException ioe){
//			     System.out.print("Erreur in reading input file: ");
//			     ioe.printStackTrace();
//			 }
//
//		}
//		// end =================================================================

		return(weightedAdjMatrix);
	}

}
