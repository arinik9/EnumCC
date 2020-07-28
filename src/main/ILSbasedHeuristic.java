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
import enumeration.EnumerationBFS;
import enumeration.EstimateDiameterSolutionClass;
import enumeration.SearchAstar;
import enumeration.SearchForward2;
import heuristic.MyHybridHeuristic;
import heuristic.MyILSbasedHeuristic;
import heuristic.MySplitMergeHeuristic;
import mincut.StoerWagnerGlobalMincut;
import myUtils.Clustering;
import myUtils.EdgeWeightedGraph;
import myUtils.EditDistance;
import myUtils.MyCGraph;
import permanence.Permanence;


/**
* The EnumCC program aims at enumerating optimal solutions: iteratively 1-Edit, 3-edit, 3-Edit, etc.
* <p>
* 
*
*/
public class ILSbasedHeuristic {

	
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
		int tilim = -1;
		String ILSExecutablePath = "";
		int nbIterILS = 1;
		String inputDirPath = "";
		String graphFileName = "";
		String startSolutionDirPath = "";
		String outputDirPath = ".";
		int maxNbEdit = 1;
		
		if( !System.getProperty("ILSExecutablePath").equals("${ILSExecutablePath}") )
			ILSExecutablePath = System.getProperty("ILSExecutablePath");
		else {
			System.out.println("ILSExecutablePath is not specified. Exit");
			return;
		}
		
		if( !System.getProperty("nbIterILS").equals("${nbIterILS}") )
			nbIterILS = Integer.parseInt(System.getProperty("nbIterILS"));
		else {
			System.out.println("nbIterILS is not specified. Exit");
			return;
		}
		
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
		
		if(!System.getProperty("maxNbEdit").equals("${maxNbEdit}") )
			maxNbEdit = Integer.parseInt(System.getProperty("maxNbEdit"));

		
		System.out.println("===============================================");
		System.out.println("ILSExecutablePath: " + ILSExecutablePath);
		System.out.println("nbIterILS: " + nbIterILS);
		System.out.println("inputDirPath: " + inputDirPath);
		System.out.println("graphFileName: " + graphFileName);
		System.out.println("startSolutionDirPath: " + startSolutionDirPath);
		System.out.println("outputDirPath: " + outputDirPath);
		System.out.println("maxNbEdit: " + maxNbEdit);
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

		
//		int[] membership = readMembership(outputDirPath, 0, 28);
//		Clustering c = new Clustering(membership, -1);
//		System.out.println(c);
//		MyCGraph g = new MyCGraph(adjMat.length, c);
//		g.fillInNodeToWeightedDegreeSum(adjMat);
//		Permanence p = new Permanence(adjMat, g);
//		double[] scores = p.computePermananceScores();
//		for(int i=0; i<adjMat.length; i++){
//			System.out.println("permanence score of "+i+": "+scores[i]);
//		}
		
		// ==> only Edit1, Edit2 and Edit3
		int nbThread = 1;
		AbstractEnumeration e = new EnumerationBFS(adjMat, tilim, nbThread, maxNbEdit); 
			
		MyILSbasedHeuristic heuristic = new MyILSbasedHeuristic(ILSExecutablePath, nbIterILS, adjMat.length, adjMat, 
				maxNbEdit, inputDirPath, graphFileName, outputDirPath, tilim);
		heuristic.run(e);

		
		
		
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
