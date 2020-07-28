package weka;

import java.util.ArrayList;

import weka.clusterers.SimpleKMeans;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class Kmeans {
	// https://waikato.github.io/weka-wiki/documentation/
	
	SimpleKMeans kmeans;
	double[] centroids;
	
	public Kmeans(){
		kmeans = new SimpleKMeans();
	}

	// k-means with single dimensional data
	public int[] run(double[] scores, int k){
		this.centroids = new double[k];
		
		Attribute attr1 = new Attribute("attr1");                               
        ArrayList<Attribute> attrList = new ArrayList<Attribute>();             
        attrList.add(attr1);                

        Instances dataset = new Instances("permanence", attrList, 0);
        for(int i=0; i<scores.length; i++){
        	double[] val = new double[] { scores[i]};
        	Instance instance = new DenseInstance(1.0, val);
        	instance.setDataset(dataset);
        	dataset.add(instance);  
        }
        
        int assignments[];
        try {
        	this.kmeans.setPreserveInstancesOrder(true);
        	this.kmeans.setNumClusters(k);
        	this.kmeans.setSeed(k);
        	this.kmeans.setDontReplaceMissingValues(true);
        	this.kmeans.buildClusterer(dataset);
        	this.kmeans.setMaxIterations(10);                                    
            Instances instances = this.kmeans.getClusterCentroids();
            int nbFoundClusters = this.kmeans.getNumClusters();
            if(nbFoundClusters == k){ // TODO I do not know the source of this error
	            for(int i=0; i<k; i++)
	            	this.centroids[i] = instances.get(i).value(0); // we do 'value(0)', since there is 1 attribute
	            assignments = this.kmeans.getAssignments();
	//            int x=0;
	//            for(int assignment : assignments) {
	//                System.out.println("data :" + dataset.get(x) + "instance idx: " + x + " centroid value: " + instances.get(assignment) + ", assign:" + assignment);
	//                x++;
	//            }
	            return(assignments);
            }
         } catch(Exception e){
        	 System.err.println("Error: " + e + ", nb cluster: " + this.kmeans.getNumClusters());
         }
        
        return(null);
	}
	
	
	public double[] getCentroids(){
		return(this.centroids);
	}
}
