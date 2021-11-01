package rns;

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
import rns.EditDistance;
import rns.AbstractEnumeration;
import java.util.Set;
import java.util.HashSet;

public class MyExactPopulateHeuristicOnePass {

	String clusterings_LB_AssocFileName = "assoc.txt";
	
	int n;
	double[][] adjMat;
	int tilim = 3600;
	String outputDirPath = ".";
	int maxNbEdit = 1;
	String initMembershipFilePath = "";
	String allPreviousResultsFilePath = "";
	
	int NB_THREAD = 6;
	
	Set<Clustering> discoveredClusterings;
	ArrayList<String> discoveredClusteringFilePaths;
	
	
	public MyExactPopulateHeuristicOnePass(int n_, double[][] adjMat_, int maxNbEdit_,
			String outputDirPath_, String initMembershipFilePath_, String allPreviousResultsFilePath_)
	{
		n = n_;
		adjMat = adjMat_;
		maxNbEdit = maxNbEdit_;
		//startSolutionDirPath = startSolutionDirPath_;
		outputDirPath = outputDirPath_;
		initMembershipFilePath = initMembershipFilePath_;
		allPreviousResultsFilePath = allPreviousResultsFilePath_;
		
		discoveredClusterings = new HashSet<Clustering>();
		discoveredClusteringFilePaths = new ArrayList<>();
	}
	
	
	// TODO control time limit in each component
	public void run(AbstractEnumeration e){
		//discoveredClusterings.clear();
		//discoveredClusteringFilePaths.clear();
		long startTime = System.currentTimeMillis();
		
		
		
		discoveredClusterings = readAllPreviousMemberships(allPreviousResultsFilePath);
		
		// ===========================================================================================================
		ArrayList<Integer> LBs = new ArrayList<Integer>();
			System.out.println("-------------------------");
		
		// ===================================================
		// 1) enumerate other optimal clusterings based on 1-Edit, ... 'maxNbEdit', starting from the reference/seed clustering
		// ===================================================

		int[] membership = readMembership(initMembershipFilePath);

		Clustering c1 = new Clustering(membership, -1);
		c1.computeImbalance(adjMat);
		System.out.println(c1);
		System.out.println("------------------");

		
		
		Clustering currRefClustering = new Clustering(membership, -1);
		currRefClustering.computeImbalance(adjMat);
		System.out.println("imbalance: " + currRefClustering.getImbalance());
		
		// ===================
		e.reset();
		new File(outputDirPath).mkdirs();
		e.enumerate(currRefClustering, outputDirPath, discoveredClusterings);
		// ===================		
		
		

		// ===================================================
		// 2) 
		// ===================================================
		if(e.foundClusterings.size()>0){				
			
			for(Clustering c : e.foundClusterings){
				String resultFileName = "membership"+c.getId()+".txt";
				discoveredClusteringFilePaths.add(outputDirPath+"/"+resultFileName);
				discoveredClusterings.add(c);
				LBs.add(1);
			}
			
		} else { // at least use the info of the init solution
			discoveredClusterings.add(currRefClustering);
			discoveredClusteringFilePaths.add(initMembershipFilePath);
			LBs.add(1); //+1 because we want to see a new solution
		}
		
		boolean append = false;
		writeClusterings_LB_AssocFileName(outputDirPath, clusterings_LB_AssocFileName, discoveredClusteringFilePaths, LBs, append);		
		appendClusteringFilePathsIntoFile(allPreviousResultsFilePath, discoveredClusteringFilePaths);
//		append = true;
//		writeClusterings_LB_AssocFileName(outputDirPath+"/..", clusterings_LB_AssocFileName, discoveredClusteringFilePaths, LBs, append);		

		long endTime = System.currentTimeMillis();
		double execTime = (float) (endTime-startTime)/1000;
		//System.out.println("execution time (in MyExactPopulateHeuristicOnePass): " + execTime + "s");
	}
	

	
	private void appendClusteringFilePathsIntoFile(String outFilePath, ArrayList<String> clusteringFileNames){
		String content = "";
		for(int i=0; i<clusteringFileNames.size(); i++){ // for each node
			if(!content.equals(""))
				content += "\n";
			content += clusteringFileNames.get(i);
		}
			
		try{
			InputStream  ips = new FileInputStream(outFilePath);
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br = new BufferedReader(ipsr);
			String line;
			int counter = 0;
	        while ((line=br.readLine())!=null){
	        	counter++;
	        }
	        
			 BufferedWriter writer = new BufferedWriter(new FileWriter(outFilePath, true)); // append = true
			 if(counter!=0)
				 content = "\n" + content;
			 writer.write(content);
			 writer.close();
		 } catch(IOException ioe){
		     System.out.print("Erreur in writing output file: ");
		     ioe.printStackTrace();
		 }
	}
	
	private void writeClusterings_LB_AssocFileName(String outputDirPath, String outFileName, 
			ArrayList<String> clusteringFileNames, ArrayList<Integer> LBs, boolean append)
	{
		String filepath = outputDirPath + "/" + outFileName;
		
		String content = "";
		for(int i=0; i<clusteringFileNames.size(); i++){ // for each node
			if(!content.equals(""))
				content += "\n";
			content += LBs.get(i)+":"+clusteringFileNames.get(i);
		}
			
		try{
//			InputStream  ips = new FileInputStream(filepath);
//			InputStreamReader ipsr=new InputStreamReader(ips);
//			BufferedReader br = new BufferedReader(ipsr);
//			String line;
//			int counter = 0;
//	        while ((line=br.readLine())!=null){
//	        	counter++;
//	        }
//	        if(counter!=0)
//				 content = "\n" + content;
	        
			 BufferedWriter writer = new BufferedWriter(new FileWriter(filepath, append));
			 writer.write(content);
			 writer.close();
		 } catch(IOException ioe){
		     System.out.print("Erreur in writing output file: ");
		     ioe.printStackTrace();
		 }
	}
	
	
	/**
	 * read a solution from file
	 * 
	 */
	public Set<Clustering> readAllPreviousMemberships(String filepath){
		Set<Clustering> allMemberships = new HashSet<Clustering>();
        System.out.println(filepath);

		try{
			InputStream  ips = new FileInputStream(filepath);
			InputStreamReader ipsr=new InputStreamReader(ips);
			BufferedReader br = new BufferedReader(ipsr);
			String line;
			
            while ((line=br.readLine())!=null){
                System.out.println(line);
                Clustering c = new Clustering(readMembership(line), -1);
                allMemberships.add(c);
            }
			br.close();
			
		}catch(Exception e){
		  System.out.println(e.toString());
		  return(null);
		}
		
		return(allMemberships);
	}
	
	

	/**
	 * read a solution from file
	 * 
	 */
	public int[] readMembership(String inputDirPath, long id_){
		String fileName = "membership" + id_ + ".txt";
		String filepath = inputDirPath + "/" + fileName;
		return(readMembership(filepath));
	}
	
	/**
	 * read a solution from file
	 * 
	 */
	public int[] readMembership(String filepath){
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
	
	
	
	
}
