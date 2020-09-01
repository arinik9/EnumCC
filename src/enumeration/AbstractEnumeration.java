package enumeration;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import myUtils.Clustering;

public abstract class AbstractEnumeration {

	Clustering initClustering;
	String outDirPath = "";
	double[][] adjMat;
	public Set<Clustering> foundClusterings = new HashSet<Clustering>();
	public long idCounter=0; // I am not sure if it is really needed
	public double execTime;
	int maxNbEdit;
	int bestEditDistance;

	
	public AbstractEnumeration(double[][] adjMat_, int maxNbEdit_)
	{
		//outDirPath = outDirPath_;
		adjMat = adjMat_;
		maxNbEdit = maxNbEdit_;
	}
	
	
	public abstract void reset();

	
	public abstract void enumerate(Clustering initClustering, String passOutputDirPath, Set<Clustering> discoveredClusterings);
	
	
	public void writeIntoFile(String filepath, String content){
		try{
			 BufferedWriter writer = new BufferedWriter(new FileWriter(filepath));
			 writer.write(content);
			 writer.close();
		} catch(IOException ioe){
		     System.out.print("Erreur in writing output file: "+filepath);
		     ioe.printStackTrace();
		}
	}
	
	
	public void writeClusteringIntoFiles(String outputDirPath){
		
		int counter = 0;
		for(Clustering c : this.foundClusterings){
			c.setId(counter++);
			c.writeMembership(outputDirPath);
		}
	}
	
	
	public void writeCommonStatisticsIntoFiles(String outputDirPath){
		
		String filepath = outputDirPath+"/"+"maxNbEdit.txt";
		writeIntoFile(filepath, this.maxNbEdit+"");
		
		// ===============================
		
		filepath = outputDirPath+"/"+"execTime.txt";
		writeIntoFile(filepath, this.execTime+"");
	}
	
}
