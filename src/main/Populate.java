package main;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;

import enumeration.AbstractEnumeration;
import enumeration.EnumerationBFS;
import heuristic.MyExactPopulateHeuristic;
import heuristic.MyHybridHeuristic;
import myUtils.Clustering;
import myUtils.UndirectedGraph;


/**
* The EnumCC program aims at enumerating optimal solutions: iteratively 1-Edit, 3-edit, 3-Edit, etc.
* <p>
* 
*
*/
public class Populate {

	
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
		String outputDirPath = ".";
		String startSolutionDirPath = "";
		int maxNbEdit = 1;
		int nbThread = 1;

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
		if(!System.getProperty("tilim").equals("${tilim}") )
			tilim = Integer.parseInt(System.getProperty("tilim"));
		
		if(!System.getProperty("maxNbEdit").equals("${maxNbEdit}") )
			maxNbEdit = Integer.parseInt(System.getProperty("maxNbEdit"));

		if(!System.getProperty("nbThread").equals("${nbThread}") )
			nbThread = Integer.parseInt(System.getProperty("nbThread"));
		
		System.out.println("===============================================");
		System.out.println("CPLEX_BIN_PATH: " + CPLEX_BIN_PATH);
		System.out.println("inputDirPath: " + inputDirPath);
		System.out.println("graphFileName: " + graphFileName);
		System.out.println("startSolutionDirPath: " + startSolutionDirPath);
		System.out.println("outputDirPath: " + outputDirPath);
		System.out.println("maxNbEdit: " + maxNbEdit);
		System.out.println("tilim: " + tilim + "s");
		System.out.println("nbThread: " + nbThread);
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

		
		
		// ==> only Edit1, Edit2 and Edit3
		AbstractEnumeration e = new EnumerationBFS(adjMat, tilim, nbThread, maxNbEdit); 
						
		MyExactPopulateHeuristic heuristic = new MyExactPopulateHeuristic(adjMat.length, adjMat, maxNbEdit, inputDirPath, graphFileName, outputDirPath,
				tilim, CPLEX_BIN_PATH);
		heuristic.run(e);
		

		System.out.println("end");
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
