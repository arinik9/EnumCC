package rns;

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
import java.util.List;

import rns.AbstractEnumeration;
import rns.EnumerationBFS;
import rns.EnumerationBFSIncremental;
import myUtils.Clustering;


/**
* The RNSCC method/program aims at enumerating optimal solutions from an existing one:
* 	 iteratively 1-Edit, 2-edit, 3-Edit, etc. until no new solution found.
* <p>
* 
*
*/
public class RNS {

	
	static String tempFile = "temp.txt";
	
	/**
	 * 
	 * Input parameters:
	 * <ul>
	 * <li> inputFilePath (String): Input file path. </li>
	 * <li> outDir (String): Output directory path. Default ".", i.e. the current directory). </li>
	 * <li> initMembershipFilePath (String): The membership file path, from which the RNS starts. </li>
	 * <li> allPreviousResultsFilePath (String): The file path which stores the paths of the all already-discovered solutions. </li>
	 * <li> maxNbEdit (Integer): the maximum value of the edit distance to consider in edit operations. </li>
	 * <li> tilim (Integer): time limit in seconds. </li>
	 * <li> solLim (Integer): the maximum number of optimal solutions to limit. </li>
	 * <li> nbThread (Integer): number of threads. </li>
	 * <li> isBruteForce (boolean): Default false. Whether we apply the MVMO property or not.
	 * 								 Actually this parameter is not well named.
	 * 								 This option is used for our experiments in Section 8.1. </li>
	 * <li> isIncrementalEditBFS (booelan): Default false. When it is true, we apply 
	 * 									all possible d-edit operations with d \in {1,2,..,d} to an unprocessed solution,
	 * 									then pass to another solution. Otherwise, we apply d-edit operation
	 * 									with d=1 to all unprocessed solutions, then we increase d and apply again.
	 * 									This option is used for our experiments in Section 8.1. </li>
	 * </ul>
	 * 

	 * 
	 * Example:
	 * <pre>
	 * {@code
	 * ant clean jar
	 *     ant -v -buildfile build-rns.xml -DinitMembershipFilePath="out/net/membership0.txt" 
	 *     	-DallPreviousResultsFilePath="out/net/allResults.txt" -DinputFilePath="$inputFilePath"
	 *      -DoutDir="$outDir" -DmaxNbEdit=3 -Dtilim=3600 -DsolLim=3000 -DnbThread=1 
	 *      -DisBruteForce=false -DisIncrementalEditBFS=false run
	 * }
	 * </pre>
	 * 
	 * 
	 * @param args  (Not used in this program. Instead, user parameters are obtained
	 * 	 through ant properties. See the buil.xml for more details).
	 * 
	 * @throws FileNotFoundException.
	 */
	public static void main(String[] args) throws FileNotFoundException {
		String initMembershipFilePath = "";
		
		int tilim = -1;
		String inputFilePath = "";
		String outputDirPath = ".";
		String allPreviousResultsFilePath = "";
		int maxNbEdit = 1;
		int nbThread = 1;
		int solLim = -1;
		boolean isBruteForce = false;
		boolean isIncrementalEditBFS = false; // for benchmark, use isIncrementalEditBFS = true
		

		System.out.println("___");
		if( !System.getProperty("initMembershipFilePath").equals("${initMembershipFilePath}") )
			initMembershipFilePath = System.getProperty("initMembershipFilePath");
		
		if( !System.getProperty("allPreviousResultsFilePath").equals("${allPreviousResultsFilePath}") )
			allPreviousResultsFilePath = System.getProperty("allPreviousResultsFilePath");
		
		if( !System.getProperty("inputFilePath").equals("${inputFilePath}") )
			inputFilePath = System.getProperty("inputFilePath");
		else {
			System.out.println("inputFilePath is not specified. Exit");
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
		
		if(!System.getProperty("solLim").equals("${solLim}") )
			solLim = Integer.parseInt(System.getProperty("solLim"));
		
		if( !System.getProperty("isBruteForce").equals("${isBruteForce}") )
			isBruteForce = Boolean.valueOf(System.getProperty("isBruteForce"));
		
		if( !System.getProperty("isIncrementalEditBFS").equals("${isIncrementalEditBFS}") )
			isIncrementalEditBFS = Boolean.valueOf(System.getProperty("isIncrementalEditBFS"));
		
		System.out.println("===============================================");
		System.out.println("initMembershipFilePath: " + initMembershipFilePath);
		System.out.println("allPreviousResultsFilePath: " + allPreviousResultsFilePath);
		System.out.println("inputFilePath: " + inputFilePath);
		System.out.println("outputDirPath: " + outputDirPath);
		System.out.println("maxNbEdit: " + maxNbEdit);
		System.out.println("tilim: " + tilim + "s");
		System.out.println("solLim: " + solLim);
		System.out.println("isBruteForce: " + isBruteForce);
		System.out.println("isIncrementalEditBFS: " + isIncrementalEditBFS);
		System.out.println("===============================================");
		

		new File(outputDirPath).mkdirs();
		
//		try {
//			String startSolutionFileName="membership0.txt";
//			Files.copy(new File(startSolutionDirPath+"/"+startSolutionFileName).toPath(), new File(outputDirPath+"/"+startSolutionFileName).toPath(), 
//					StandardCopyOption.REPLACE_EXISTING);
//		} catch (IOException e1) {
//			e1.printStackTrace();
//		}
		
		double[][] adjMat = createAdjMatrixFromInput(inputFilePath);

		
		AbstractEnumeration e;
		if(!isIncrementalEditBFS)
			e = new EnumerationBFS(adjMat, tilim, solLim, nbThread, maxNbEdit, isBruteForce);
		else
			e = new EnumerationBFSIncremental(adjMat, tilim, solLim, nbThread, maxNbEdit, isBruteForce); 

						
		MyExactPopulateHeuristicOnePass heuristic = new MyExactPopulateHeuristicOnePass(adjMat.length, adjMat, maxNbEdit,
				 outputDirPath, initMembershipFilePath, allPreviousResultsFilePath);
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
