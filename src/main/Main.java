package main;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import callback.lazy_callback.LazyCBCycle;
import cplex.Cplex;
import formulation.AbstractFormulation;
import formulation.Edge;
import formulation.FormulationEdge;
import formulation.FormulationVertex;
import formulation.MyParam;
import formulation.MyParam.Transitivity;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.CplexStatus;
import ilog.cplex.IloCplex.IntParam;
import ilog.cplex.IloCplex.UnknownObjectException;
import myUtils.Clustering;
import variable.VariableLister.VariableListerException;


/**
* 
* Some references:
* <ul>
* <li> Cartwright D, Harary F (1956) Structural balance:
* 	 a generalization of Heider’s theory. Psychol Rev 63:277-293 <\li>
* <li> Heider F (1946) Attitudes and cognitive organization. J Psychol 21:107-112 <\li>
* <li> N. Arınık & R. Figueiredo & V. Labatut. Efficient Enumeration of Correlation Clustering Optimal Solution Space (submitted), Journal of Global Optmization, 2021. <\li>
* <li> N. Arınık, *Multiplicity in the Partitioning of Signed Graphs*. PhD thesis in Avignon Université (2021). <\li>
* <\lu>
*/
public class Main {

	
	static String tempFile = "temp.txt";
	
	/**
	 * 
	 * EnumCC is an optimal solution space enumeration method for the CC problem.
	 * It relies on two essential tasks: recurrent neighborhood search and jumping onto an undiscovered solution.
	 
	 * In the first step, instead of directly "jumping" onto undiscovered optimal solutions one by one
	 * 	 like in the sequential approach, it discovers the recurrent neighborhood of the current optimal solution P 
	 * 	 with the hope of discovering new optimal solutions. The recurrent neighborhood of an optimal solution P,
	 *  represents the set of optimal solutions, reached directly or indirectly from P depending on 
	 *  the maximum distance parameter 'maxNbEdit'. Whether a new solution is found or not through RNSCC, 
	 *  the jumping process into a new solution P is performed (or the verification of the completeness of the solution space)
	 *  
	 *  EnumCC performs the enumeration process based on two different formulation types: 1) decision variables defined on vertex-pair (Fv: "vertex" formulation type) or 2) edge (Fe: "edge" formulation type).
	 *  If we denote "n" by the number of vertices in the graph and "m" by the number of edges, 
	 * 		there are (n*(n-1)/2) variables in Fv, whereas there are m variables in Fe.
	 *  So, which one would be preferable over the other one? We have not answered this question yet. 
	 *  However, some small experiments I conducted show that the Fv formulation type is in general more efficient, especially if EnumCC needs to perfomr a large number of "jumps".
	 *  Because, the Fe formulation type performs these jumps through lazy callback, and this can worsen the performance.
	 *  
	 *  Another remark of performance issue concerns the "triangleIneqReducedForm" parameter in the Fv formulation. See the description of this parameter for more information.
	 *
	 * 
	 * Input parameters:
	 * <ul>
	 * <li> formulationType (String): ILP formulation type. Either "vertex" for Fv or "edge" for Fe. </li>
	 * <li> inFile (String): Input file path. </li>
	 * <li> outDir (String): Output directory path. Default ".", i.e. the current directory). </li>
	 * <li> initMembershipFilePath (String): The membership file path, from which the RNS starts. </li>
	 * <li> java.library.path (String): The Cplex java library path. </li>
	 * <li> maxNbEdit (Integer): the maximum value of the edit distance to consider in edit operations. </li>
	 * <li> tilim (Integer): time limit in seconds. </li>
	 * <li> solLim (Integer): the maximum number of optimal solutions to limit. </li>
	 * <li> nbThread (Integer): number of threads. </li>
	 * <li> JAR_filepath_RNSCC (String): The file path for the RNSCC executable method  </li>
	 * <li> LPFilePath (String): The file path pointing to the ILP model of the given signed graph 
	 * 							(produced by the functionality 'exportModel' in Cplex) </li>
	 * <li> triangleIneqReducedForm (boolean): Used only for the Fv formulation type. When it is set to true, this indicates that 
	 * 		"LPFilePath" contains the reduced number ("non-redundant") of triangle inequalities in the formulation. 
	 * 		It is important to know this information, because the way the partition information is extracted from an optimal solution is slightly different.
	 * 		This extraction step is explained in the work of Miyauchi et al.: 
	            A. Miyauchi, T. Sonobe, and N. Sukegawa, « Exact Clustering via Integer Programming and Maximum Satisfiability », in: AAAI Conference on Artificial Intelligence 32.1 (2018).
	        Default value is false, which keeps the whole set of triangle constraints. See Chapter 2 in my Phd thesis.
	        It is worth noting that removing redundant triangle inequalities from the ILP formulation can be very beneficial for complete or very dense signed graphs, when enumerating all optimal solutions. 
            However, removing such inequalities from the ILP formulation in sparse signed graphs can substantially worsen the performance of this optimization process.
             This is because Cplex can find many optimal solutions which actually correspond to a same optimal partition.
             This last point is briefly mentioned in Chapter 5 in my PhD thesis, but it needs to be investigated thoroughly in a follow-up work. </li>
	 * <li> lazyCB (boolean): Used only for the Fe formulation type. True if adding lazily triangle constraints (i.e. lazy callback approach) is used during the branching phase when trying to find a new optimal solution. Default false. </li>
	 * <li> userCutCB (boolean): True if adding user cuts during the branching phase is desired, when trying to find a new optimal solution.
	 * 		 Based on our experiments, we can say that it does not yield any advantage, and it might even slow down the optimization process. Default false. </li>
	 * </ul>
	 * 
	 * 
	 * Example:
	 * <pre>
	 * {@code
	 * 
	 * ant -v -buildfile build.xml -DinFile="in/""$name" -DoutDir="out/""$modifiedName" 
	 * 		-DmaxNbEdit=3 -DinitMembershipFilePath="$initMembershipFilePath" -DLPFilePath="$LPFilePath"
	 *  	-DJAR_filepath_RNSCC="RNSCC.jar" -DnbThread=4 -Dtilim=-1 -DsolLim=50000 run
	 * }
	 * </pre>
	 * 
	 * @param args  (Not used in this program. Instead, user parameters are obtained
	 * 	 through ant properties. See the build.xml for more details).
	 * @throws VariableListerException 
	 * @throws IOException 
	 * 
	 * @throws FileNotFoundException.
	 * @throws UnknownObjectException. 
	 * @throws IloException.
	 */
	public static void main(String[] args) throws UnknownObjectException, IloException, IOException {
		System.out.println("!!===============================================");

		String inputFilePath = "";
		String outputDirPath = ".";
		String initMembershipFilePath = "";
		String LPFilePath = "";
		int maxNbEdit = 3; // by default
		String JAR_filepath_RNSCC = "";
		int nbThread = 1;
		long tilim = -1;
		long remainingTime = -1;
		long startTime;
		long enumTime = 0;
		int solLim = -1;
		int nbSols = 1; // init
		int remainingNbSols = -1;
		boolean isBruteForce = false; // init
		
		boolean triangleIneqReducedForm = false;
		boolean lazyCB = false;
		boolean userCutCB = false;
		String formulationType = "vertex";

		System.out.println("===============================================");

		if( !System.getProperty("inFile").equals("${inFile}") )
			inputFilePath = System.getProperty("inFile");
		else {
			System.out.println("input file is not specified. Exit");
			return;
		}

		
		if( !System.getProperty("formulationType").equals("${formulationType}") )
			formulationType = System.getProperty("formulationType");
		else {
			System.out.println("formulationType is not specified. Exit");
			return;
		}
		
		if( !System.getProperty("JAR_filepath_RNSCC").equals("${JAR_filepath_RNSCC}") )
			JAR_filepath_RNSCC = System.getProperty("JAR_filepath_RNSCC");
		else {
			System.out.println("JAR_filepath_RNSCC file is not specified. Exit");
			return;
		}
		
		if( !System.getProperty("outDir").equals("${outDir}") )
			outputDirPath = System.getProperty("outDir");
	

		System.out.println(System.getProperty("initMembershipFilePath"));
		if( !System.getProperty("initMembershipFilePath").equals("${initMembershipFilePath}") ) // it is not useful
			initMembershipFilePath = System.getProperty("initMembershipFilePath");
		else {
			System.out.println("initMembershipFilePath file is not specified.");
		}
		
		if( !System.getProperty("LPFilePath").equals("${LPFilePath}") )
			LPFilePath = System.getProperty("LPFilePath");
		else {
			System.out.println("LPFilePath file is not specified.");
		}

		if( !System.getProperty("maxNbEdit").equals("${maxNbEdit}") )
			maxNbEdit = Integer.parseInt(System.getProperty("maxNbEdit"));
		
		if( !System.getProperty("nbThread").equals("${nbThread}") )
			nbThread = Integer.parseInt(System.getProperty("nbThread"));
		
		if( !System.getProperty("tilim").equals("${tilim}") )
			tilim = Long.parseLong(System.getProperty("tilim"));
		
		if( !System.getProperty("solLim").equals("${solLim}") )
			solLim = Integer.parseInt(System.getProperty("solLim"));
		
		if(!System.getProperty("lazyCB").equals("${lazyCB}") )
			lazyCB = Boolean.valueOf(System.getProperty("lazyCB"));

		if( !System.getProperty("userCutCB").equals("${userCutCB}") )
			userCutCB = Boolean.valueOf(System.getProperty("userCutCB"));

		if( formulationType.equals("vertex") &&  !System.getProperty("triangleIneqReducedForm").equals("${triangleIneqReducedForm}") )
			triangleIneqReducedForm = Boolean.valueOf(System.getProperty("triangleIneqReducedForm"));
		
		
		
		System.out.println("===============================================");
		System.out.println("formulationType: " + formulationType);
		System.out.println("inputFilePath: " + inputFilePath);
		System.out.println("outputDirPath: " + outputDirPath);
		System.out.println("initMembershipFilePath: " + initMembershipFilePath);
		System.out.println("LPFilePath: " + LPFilePath);
		System.out.println("maxNbEdit: " + maxNbEdit);
		System.out.println("JAR_filepath_RNSCC: " + JAR_filepath_RNSCC);
		System.out.println("nbThread: " + nbThread);
		System.out.println("lazyCB: " + lazyCB);
		System.out.println("userCutCB: " + userCutCB);
		System.out.println("tilim: " + tilim);
		System.out.println("solLim: " + solLim);
		System.out.println("triangleIneqReducedForm: " + triangleIneqReducedForm); // affects only the task of retrieving membership info
		System.out.println("===============================================");
		
		// ------------------------------------------------
		// if this is true, this means we will perform Miyauchi's filtering for triangle constraints.
		boolean isWithoutZeroLink = triangleIneqReducedForm;
		int n = createTempFileFromInput(inputFilePath, isWithoutZeroLink); // Remark: we do not use the produced temp file, just need "n"
		
		ArrayList<int[]> allPrevEdgeVarsList = new ArrayList<>();
		ArrayList<int[]> prevEdgeVarsList = new ArrayList<>();
		

		double[][] adjMat = createAdjMatrixFromInput(inputFilePath);
		
		// -----------------------------------------
		
		remainingTime = tilim;
		System.out.println("remainingTime: " + remainingTime);
		remainingNbSols = solLim;
		System.out.println("remaining number of solutions: " + remainingNbSols);
		
		
		String clusterings_LB_AssocFileName = "assoc.txt";
		int passCounter = 0;
		
		
		
		// =================================================================		
		// STEP 0: init cplex and formulation
		// =================================================================
		
		boolean statusReadLPModelFromFile = false;
		
		
		MyParam myp = null;
		Cplex cplex = new Cplex(); // start
		cplex.setParam(IntParam.ClockType, 2);
		
		
		if(!LPFilePath.equals("")){ 
			System.out.println("laod LP");			
			cplex.iloCplex.importModel(LPFilePath);
		}
		
		
		// note that when LPFilePath.equals("")=FALSE, we load directly the model from file, 
		//    so the choice of 'Triangle' and that of "triangleIneqReducedForm" do not affect it
		if(lazyCB)
			myp = new MyParam(inputFilePath, cplex, Transitivity.USE_LAZY, userCutCB, lazyCB, nbThread, LPFilePath, triangleIneqReducedForm);
		else
			myp = new MyParam(inputFilePath, cplex, Transitivity.USE, userCutCB, lazyCB, nbThread, LPFilePath, triangleIneqReducedForm);
		

		myp.useCplexPrimalDual = true;
		myp.useCplexAutoCuts = true;
		myp.tilim = tilim;
		myp.userCutInBB = userCutCB;
		
		AbstractFormulation p = null;
		if(formulationType.equals("vertex"))
			p = new FormulationVertex(myp); // LPFilePath.equals("")=FALSE, we will just load variables
		else if(formulationType.equals("edge")) {
			p = new FormulationEdge(myp);
			if(!LPFilePath.equals("")) // we force lazy callback, when reading ILP model from LP file 
				p.getCplex().use(new LazyCBCycle(p, 500));
		}
		
		
	    int[] initMembership = readMembership(initMembershipFilePath, n);
	    Clustering c_init = new Clustering(initMembership, 0);
	    c_init.computeImbalance(adjMat);
	    
		//double optimalObjectiveValue = p.getObjectiveValue(c_init); // I commented, because when we perform Miyauchi's filtering for triangle constraints, this may cause a problem.
		double optimalObjectiveValue = c_init.getImbalance();
		
		p.createOptimalityConstraint(optimalObjectiveValue);
		System.out.println("imb: " + optimalObjectiveValue);
		
		p.getCplex().setParam(IloCplex.Param.Threads, nbThread);
		// to stop at the first feasible solution (no need to prove optimality, since we know them already):
		p.getCplex().setParam(IloCplex.Param.MIP.Limits.Solutions, 1);
		////p.getCplex().setParam(IloCplex.Param.Advance.FPHeur, 2);
        p.getCplex().setParam(IloCplex.Param.Emphasis.MIP, 1);
		//p.getCplex().setParam(IloCplex.Param.Advance.FeasOptMode, 1); // TODO make this input parameter
	    
	    
		
		// =================================================================		
		// STEP 1
		// =================================================================
		// create an empty file where all the file paths of the results will be stored step by step
		String allPreviousResultsFilePath = outputDirPath+"/allResults.txt";
		File file = new File(allPreviousResultsFilePath);
	    file.createNewFile();
	    
		file = new File(outputDirPath+"/" + clusterings_LB_AssocFileName);
	    file.createNewFile();
	    
	    String _initMembershipFilePath = initMembershipFilePath;
	    initMembershipFilePath = outputDirPath + "/" + "membership0.txt";
		try {
			Files.copy(new File(_initMembershipFilePath).toPath(), new File(initMembershipFilePath).toPath(), 
					StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		

		ArrayList<Clustering> allCurrentClusterings = new ArrayList<>();

		while(true) {

		    if(tilim > 0 && remainingTime<=0)
		    	break;
			
			// =================================================================
		    // STEP 2
			// =================================================================
		    //String JAR_filepath_EnumCC = "lib/EnumPopulateCCOnePass.jar";
		    //int nbThread = 6;
			startTime = System.currentTimeMillis();
			
		    initMembershipFilePath = outputDirPath + "/membership"+passCounter+".txt";
			
			passCounter++;
		    String outputPassDirPath = outputDirPath + "/" + passCounter ;
		    
			List<String> cmdArgsRNSCC = buildRNSCCCommand(JAR_filepath_RNSCC, inputFilePath, outputPassDirPath, 
					initMembershipFilePath, allPreviousResultsFilePath, maxNbEdit, remainingTime, remainingNbSols,
					isBruteForce, nbThread, false);
			String cmdEnumCC = cmdArgsRNSCC.stream()
				      .collect(Collectors.joining(" "));
			runCommand(cmdEnumCC);
		    
			if(tilim > 0) { // if time limit is provided by user
				enumTime = (System.currentTimeMillis()-startTime)/1000;
				remainingTime = remainingTime-enumTime;
				System.out.println("remainingTime: " + remainingTime);
			}
			
			ArrayList<Clustering> prevClusterings = LoadPreviousClusterings(n,outputPassDirPath,clusterings_LB_AssocFileName);
			allCurrentClusterings.addAll(prevClusterings);
			nbSols += prevClusterings.size();
		    System.out.println("current number of optimal solutions: "+allCurrentClusterings.size());
			if(solLim > 0){
				remainingNbSols = solLim - nbSols;
			}
		    
					
			// ==========================================================================
			for(Clustering c : prevClusterings){
				//c.computeImbalance(adjMat);
				//System.out.println("imb: " + c.getImbalance());
				allPrevEdgeVarsList.add(c.retreiveEdgeVars(p.getEdges()));
				prevEdgeVarsList.add(c.retreiveEdgeVars(p.getEdges()));
			}
			// ==========================================================================
			
			// p.getCplex().iloCplex.exportModel(outputDirPath+"/"+"strengthedModel2_"+formulationType+".lp");
			
			 if((tilim > 0 && remainingTime<=0) || (solLim > 0 && remainingNbSols<=0))
			    	break;
		 
//		if((tilim<0 || (tilim > 0 && remainingTime>0)) && (solLim<0 || (solLim > 0 && remainingNbSols>0))){
	    
			// =================================================================
			// STEP 3
			// =================================================================
			
			System.out.println("=== STEP 3 =====");

			startTime = System.currentTimeMillis();
						
			boolean alreadyVisited = true;
			while(alreadyVisited){
				String logpath = outputDirPath + "/" + "jump-log" + passCounter + ".txt";
				p.setLogPath(logpath);
				
				if(tilim > 0)
					p.getCplex().setParam(IloCplex.Param.TimeLimit, remainingTime);
		
				////p.getCplex().setParam(IloCplex.Param.MIP.Strategy.VariableSelect, 3);
				////p.getCplex().setParam(IloCplex.Param.MIP.Strategy.VariableSelect, -1); // when a custom Branch callback is used, use this line
				////p.getCplex().iloCplex.setParam(IloCplex.Param.Preprocessing.Dual, 1);
				////p.getCplex().iloCplex.setParam(IloCplex.Param.MIP.Strategy.Probe, 3);
				////p.getCplex().iloCplex.setParam(IloCplex.Param.MIP.Limits.RepairTries, -1);
				if(passCounter==1) // only at the first iteration
					p.getCplex().iloCplex.setParam(IloCplex.IntParam.RootAlg, 3); // network
				else // for the remaining iterations
				    p.getCplex().iloCplex.setParam(IloCplex.IntParam.RootAlg, 2); // dual
				
				System.out.println("trying to find a new solution with CPLEX. Waiting ...");
				p.solve(prevEdgeVarsList);
				System.out.println("status: " + p.getCplex().getCplexStatus());
				writeStringIntoFile(outputDirPath + "/jump-status"+passCounter+".txt", p.getCplex().getCplexStatus()+"");
				
				enumTime = (System.currentTimeMillis()-startTime)/1000;
				String execTimeFilename = outputDirPath + "/" + "jump-exec-time" + passCounter + ".txt";
				writeDoubleIntoFile(execTimeFilename, enumTime);
				
				if(tilim > 0) { // if time limit is provided by user
					remainingTime = remainingTime-enumTime;
					System.out.println("remainingTime: " + remainingTime);
				
					if(remainingTime<=0)
				    	break;
				}
				
				if(p.getCplex().getCplexStatus() == CplexStatus.Optimal || p.getCplex().getCplexStatus() == CplexStatus.SolLim){
					if(!isWithoutZeroLink)
						p.retreiveClusters();
					else
						p.retreiveClusters(-1);
					
	//				nbSols += 1;
	//				//p.writeClusters(outputDirPath + "/membership"+passCounter+".txt");
	//				p.writeMembershipIntoFile(outputDirPath, "membership"+passCounter+".txt");
	//				System.out.println("status: " + p.getCplex().getCplexStatus());
					
					Clustering currClustering = new Clustering(p.retreiveMembership(), -1);
					currClustering.computeImbalance(adjMat);
					System.out.println(currClustering);
					alreadyVisited = isAlreadyVisitedSolution(currClustering, allCurrentClusterings);
					if(alreadyVisited){
						System.out.println("!!!! ALREADY VISITED OPT SOL. RUN AGAIN !!!!");
						
						int[] currEdgeVars = p.retreiveEdgeVariables();
						//p.displayEdgeVariables(6);
						prevEdgeVarsList.clear();
						prevEdgeVarsList.add(currEdgeVars);
						allPrevEdgeVarsList.add(currEdgeVars);
					}
					else {
							nbSols += 1;
							//p.writeClusters(outputDirPath + "/membership"+passCounter+".txt");
							p.writeMembershipIntoFile(outputDirPath, "membership"+passCounter+".txt");
							System.out.println("status: " + p.getCplex().getCplexStatus());
						}
				
				} else // CplexStatus.Infeasible
					return; // quit the program
				
			}
			
		}
		
		cplex.end(); // end
		
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
	 * This method reads input graph file, then stocks it as weighted adjacency matrix, 
	 * finally writes the graph in lower triangle format into a temp file.
	 * Actually, we do not use the produced temp file in the program, we need just "n".
	 * 
	 * @param filename  input graph filename
	 * @return 
	 */
	private static int createTempFileFromInput(String fileName, boolean isWithoutZeroLink) {
	//private static int createTempFileFromInput(String fileName) {
		double NEG_EPSILON = -0.000001; 
		double[][] weightedAdjMatrix = null;
		int n=-1;
		
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
		}
		// end =================================================================

		// =====================================================================
		// When isWithoutZeroLink=true, Turn zero link into negative link for reduced form of triangle inequalities
		// =====================================================================
		if(n>0 && isWithoutZeroLink){
			for(int i=0; i<(n-1); i++){
				 for(int j=i+1; j<n; j++){
					 if(weightedAdjMatrix[i][j] == 0.0){
						 weightedAdjMatrix[i][j] = NEG_EPSILON;
						 weightedAdjMatrix[j][i] = NEG_EPSILON;
					 }
				 }
			}
		}

		// =====================================================================
		// write into temp file (in lower triangle format)
		// =====================================================================
		if(weightedAdjMatrix != null){
			 try{
			     FileWriter fw = new FileWriter(tempFile, false);
			     BufferedWriter output = new BufferedWriter(fw);

			     for(int i = 1 ; i < weightedAdjMatrix.length ; ++i){
			    	 String s = "";
			    	 
			    	 for(int j = 0 ; j < i ; ++j) // for each line, iterate over columns
			    		 s += weightedAdjMatrix[i][j] + " ";

			    	 s += "\n";
			    	 output.write(s);
			    	 output.flush();
			     }
			     
			     output.close();
			 }
			 catch(IOException ioe){
			     System.out.print("Erreur in reading input file: ");
			     ioe.printStackTrace();
			 }

		}
		// end =================================================================

		return(n);
	}
	
	
	
	
	
	
	private static boolean isAlreadyVisitedSolution(Clustering c_new, ArrayList<Clustering> allCurrentClusterings){
		int i=0;
		for(Clustering c : allCurrentClusterings){
			if(c_new.equals(c)) {
				System.out.println("redundant solution found at index : " + i);
				System.out.println(c.getClustersInArrayFormat());
//				System.out.println(c.retreiveEdgeVars().toString());
				return(true);
			}
		}
		return(false);
	}
	
	
	
	private static void writeDoubleIntoFile(String filename, double value){
		try{
		     FileWriter fw = new FileWriter(filename, false);
		     BufferedWriter output = new BufferedWriter(fw);

	    	 String s = value+"";
	    	 output.write(s);
	    	 output.flush();
		     output.close();
		 }
		 catch(IOException ioe){
		     System.out.print("Erreur in reading input file: ");
		     ioe.printStackTrace();
		 }
	}
	
	private static void writeStringIntoFile(String filename, String s){
		try{
		     FileWriter fw = new FileWriter(filename, false);
		     BufferedWriter output = new BufferedWriter(fw);

	    	 output.write(s);
	    	 output.flush();
		     output.close();
		 }
		 catch(IOException ioe){
		     System.out.print("Erreur in reading input file: ");
		     ioe.printStackTrace();
		 }
	}
	
	/**
	 * This method reads a clustering result file.
	 * 
	 * @param filename  input clustering filename
	 * @param n: nb node in the graph
	 * @return 
	 */
	private static ArrayList<Clustering> LoadPreviousClusterings(int n, String inputDirPath, String clusterings_LB_AssocFileName) {
		ArrayList<Clustering> clusterings = new ArrayList<Clustering>();
		
		//System.out.println(inputDirPath+"/"+clusterings_LB_AssocFileName);
		try{
			  InputStream  ips=new FileInputStream(inputDirPath+"/"+clusterings_LB_AssocFileName);
			  InputStreamReader ipsr=new InputStreamReader(ips);
			  BufferedReader   br=new
			  BufferedReader(ipsr);
			  String line;

			  /* For all the lines */
			  while ((line=br.readLine())!=null){
				  String[] items = line.split(":");
				  int diversityLowerBound = Integer.parseInt(items[0]);
				  String clusteringFilePath = items[1];
				  
				  //int[] membership = readClusteringExCCResult(inputDirPath+"/"+clusteringFileName, n);
			      int[] membership = readMembership(clusteringFilePath, n);
			      Clustering c = new Clustering(membership, -1);
			      clusterings.add(c);
			  }
			  br.close();
			  
			}catch(Exception e){
			  System.out.println(e.toString());
			}
		
		return(clusterings);
	}
	

	/**
	 * read a solution from file
	 * 
	 */
	public static int[] readMembership(String fileName, int n){
		//String fileName = "membership" + id_ + ".txt";
		//String filepath = inputDirPath + "/" + fileName;
		int[] membership_ = new int[n];
		
		try{
			InputStream  ips = new FileInputStream(fileName);
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
	
	
	
	public static int getNbLinesInFile(String filepath) throws IOException{
		BufferedReader reader = new BufferedReader(new FileReader(filepath));
		int lines = 0;
		while (reader.readLine() != null) lines++;
		reader.close();
		return(lines);
	}
	
	
	 public static TreeSet<ArrayList<Integer>> getMIPStartSolutionInArrayFormat(int[] membership){
	    	int n = membership.length;
	    	int nbCluster=0;
			for(int i=0; i<n; i++){
				if(membership[i]>nbCluster)
					nbCluster = membership[i];
			}
			
			TreeSet<ArrayList<Integer>> orderedClusters = new TreeSet<ArrayList<Integer>>(
					new Comparator<ArrayList<Integer>>(){
						// descending order by array size
						@Override
						public int compare(ArrayList<Integer> o1, ArrayList<Integer> o2) {
							int value=-1;
							if(o1.size() < o2.size())
								value = 1;
//							else if(o1.size() < o2.size())
//									value = -1;
							return value;
						}
					}
			);

			
	    	ArrayList<ArrayList<Integer>> clusters = new ArrayList<ArrayList<Integer>>(nbCluster);
			for(int i=1; i<=nbCluster; i++) // for each cluster
				clusters.add(new ArrayList<Integer>());
			for(int i=0; i<n; i++) // for each node
				clusters.get(membership[i]-1).add(i); // membership array has values starting from 1
			
			for(int i=1; i<=nbCluster; i++){ // for each cluster
				ArrayList<Integer> newCluster = clusters.get(i-1);
				orderedClusters.add(newCluster);
			}
			

			return(orderedClusters);
	    }
	 
	 
	    
	 	
	 	public static List<String> buildRNSCCCommand(String JAR_filepath, String inputFilePath,
	 			String outDir, String initMembershipFilePath, String allPreviousResultsFilePath, int maxNbEdit,
	 			long tilim, int solLim, boolean isBruteForce, int nbThread, boolean isIncrementalEditBFS
	 			){
			List<String> cmdArgsDistCC = new ArrayList<>();
			cmdArgsDistCC.add("java");
			cmdArgsDistCC.add("-DinputFilePath="+ inputFilePath);
			cmdArgsDistCC.add("-DoutDir=" + outDir);
			cmdArgsDistCC.add("-DinitMembershipFilePath=" + initMembershipFilePath);
			cmdArgsDistCC.add("-DallPreviousResultsFilePath=" + allPreviousResultsFilePath);
			cmdArgsDistCC.add("-DmaxNbEdit=" + maxNbEdit);
			cmdArgsDistCC.add("-Dtilim=" + tilim);
			cmdArgsDistCC.add("-DsolLim=" + solLim);
			cmdArgsDistCC.add("-DisBruteForce=" + isBruteForce);
			cmdArgsDistCC.add("-DnbThread=" + nbThread);
			cmdArgsDistCC.add("-DisIncrementalEditBFS=" + isIncrementalEditBFS);
			cmdArgsDistCC.add("-jar " + JAR_filepath);
			
			return(cmdArgsDistCC);
		}
		
		
	 	public static void runCommand(String cmd){
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
		
}
