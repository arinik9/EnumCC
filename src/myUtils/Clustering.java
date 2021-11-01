package myUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import rns.utils.TNode;



public class Clustering {
	long id;
	public int n;
	int nbCluster = 0;
	public int[] membership; // partition information array
	static String MEMBERSHIP_FILENAME = "membership";
	public int[] clusterSizes;
	double imbalance = 0.0;
	long parentClusteringId;
	int nbEditParent;
	
	public Clustering(int[] membership_, long id_){
		id = id_;
		n = membership_.length;
		membership = new int[n];
		
		int nbCluster_=0;
		for(int i=0; i<n; i++){
			membership[i] = membership_[i];
			
			if(membership[i]>nbCluster_){
				nbCluster_ = membership[i];
			}
		}
		nbCluster = nbCluster_;
		
		calculateClusterSizes();
	}
	
	
	public Clustering(String outDirPath, long id_, int n){
		this(readMembership(outDirPath, id_, n),id_);
	}
	
	
	// will be used for deep copy
		public Clustering(Clustering c){
			this(c, -1);
		}
	
	
	// will be used for deep copy
	public Clustering(Clustering c, long id_){
		id = id_;
		nbCluster = c.nbCluster;
		n = c.membership.length;
		membership = new int[n];
		for(int i=0; i<n; i++){
			membership[i] = c.membership[i];
		}
		calculateClusterSizes();
	}
	
	public long getId(){
		return(id);
	}
	
	public void setId(long id_){
		id = id_;
	}
	
	
	public void calculateClusterSizes(){
		clusterSizes = new int[nbCluster+n]; // +n because we can create new clusters
		// +n for new clusters (normally it is sufficient to add k new clusters, where k is the number of moving vertices)
		for(int i=1; i<=(nbCluster+n); i++)
			clusterSizes[i-1]=0;
		
		for(int i=0; i<n; i++){
			if(membership[i] != -1) // except nodes already removed
				clusterSizes[membership[i]-1]++;
		}
	}
	
    public int[] getClusterSizes(){
    	return(this.clusterSizes);
    }

    	
	public int getNbCluster(){
		return(this.nbCluster);
	}
	
	public double getImbalance(){
		return(this.imbalance);
	}
	
	public int getNbEditParent(){
		return(nbEditParent);
	}
	
	public void setNbEditParent(int nbEdit){
		nbEditParent = nbEdit;
	}
	
	
	public long getParentClusteringId(){
		return(parentClusteringId);
	}
	
	public void setParentClusteringId(long id){
		parentClusteringId = id;
	}
	
    
	public ArrayList<ArrayList<Integer>> getClustersInArrayFormat(){
    	ArrayList<ArrayList<Integer>> clusters = new ArrayList<ArrayList<Integer>>(nbCluster);
		
		for(int i=1; i<=nbCluster; i++) // for each cluster
			clusters.add(new ArrayList<Integer>());
		
		for(int i=0; i<n; i++) // for each node
			if(membership[i]!=-1)  // except nodes already removed
				clusters.get(membership[i]-1).add(i); // membership array has values starting from 1

		return(clusters);
    }
    
	
	public void removeElementWithoutRemovingItsCluster(int u){
		membership[u] = -1;
		calculateClusterSizes();
	}
	
	public void changeClusterOfNode(int u, int clusterId_, boolean renumber){
		
		// --------------------------------------------------------	
		// TODO a small trick here: this.targetClusterId == 0, this means that the node will create its single node cluster
		int clusterId = clusterId_;
		if(clusterId_ == 0)
			clusterId = getNbCluster(); // the last cluster in init clustering is intentionaly empty for this situation
		// --------------------------------------------------------	
		int[] oldMem = new int[membership.length];
		for(int i=0; i<membership.length; i++)
			oldMem[i] = membership[i];
		
		int oldClusterId = membership[u];

		if(clusterSizes[clusterId-1] == 0){
			nbCluster++;
		}
		
		membership[u] = clusterId; // cluster ids start from 1
		clusterSizes[clusterId-1]++;
		clusterSizes[oldClusterId-1]--;
		
		if(renumber){
			if(clusterSizes[oldClusterId-1] == 0){
				nbCluster--;
			
				// renumber membership array
				for(int i=0; i<n; i++){
					if(membership[i]>oldClusterId)
						membership[i]--;
				}
				
			
			}
		}
		
		calculateClusterSizes(); // depends on 'nbCluster'
	}	
	
	
	public void changeClusterOfMultipleNodes(ArrayList<TNode> nodes){
//		if(nodes.size()==1)
//			changeClusterOfNode(nodes.get(0).getNodeId(), nodes.get(0).getTargetClusterId(), true);
//		else {
//		

		for(TNode node : nodes){
			changeClusterOfNode(node.getNodeId(), node.getTargetClusterId(), false);
		}
		
		int nbClusterToDelete = 0;
		ArrayList<Integer> oldClusterIds = new ArrayList<>();
		for(int i=1; i<=(clusterSizes.length-n); i++){ // do not iterate over the last n items, since they are dedicated to a new cluster
			if(clusterSizes[i-1] == 0){
				nbClusterToDelete += 1;
				oldClusterIds.add(i);
			}
		}
		
		
		if(nbClusterToDelete != 0){
			nbCluster = nbCluster - nbClusterToDelete;
			//example => input: [[1,2], [3,4], [5,6], [7,8], [9,10], [11,12]
			// output: [[1,2,3,4], [], [5,6], [7,8,9,10], [], [11,12]

			for(int i=0; i<n; i++){// for each node
				
				int decrementCounter= 0;
				for(int j=0; j<nbClusterToDelete; j++){
					if(membership[i]>oldClusterIds.get(j))
						decrementCounter += 1;
				}
				if(decrementCounter>0)
					membership[i] = membership[i] - decrementCounter;
			}
			
			calculateClusterSizes();// depends on 'nbCluster'
		}
//		}
	}
	
	
	public void computeImbalance(double[][] adjMat){
		// bug with : [[0, 1, 2, 4, 6, 7, 10, 11, 12, 16], [5, 8, 9, 13, 14, 15],
		//		[41], [17, 19, 20, 21, 27, 32, 33, 42, 48], [18, 22, 28, 29], [24, 25, 30], [26, 31, 39, 43, 45, 46, 47],
		//			[34, 35, 36, 37, 38, 44, 49], [3, 23, 40], []]


		double imbalance = 0.0;
		ArrayList<ArrayList<Integer>> clusters = getClustersInArrayFormat();
		// System.out.println(clusters);
		// we know by construction that node ids in each cluster is sorted in ascending order
		
		// count the misplaced link weights inside the clusters
		for(ArrayList<Integer> cluster : clusters){
			for(Integer nodeId1 : cluster){
				for(Integer nodeId2 : cluster){
					if(nodeId1 < nodeId2){
						if(adjMat[nodeId1][nodeId2]<0)
							imbalance += -adjMat[nodeId1][nodeId2]; // since it is a negative weight, negate it instead of abs()
					}
				}
			}
		}
		
		// count the misplaced link weights between the clusters
		for(ArrayList<Integer> cluster1 : clusters){
			for(ArrayList<Integer> cluster2 : clusters){
				
				if(cluster1.get(0) != cluster2.get(0)){ // if two arrays are identical, then the fist element should be the same
					for(Integer nodeId1 : cluster1){
						for(Integer nodeId2 : cluster2){
							if(nodeId1 < nodeId2){
								if(adjMat[nodeId1][nodeId2]>0)
									imbalance += adjMat[nodeId1][nodeId2];
							}
						}
					}
				}
			}
		}
		
		this.imbalance = imbalance;
	}
	
	
	
	
	
	public int[] retreiveEdgeVars(Set<formulation.Edge> set){
		int[] edgeVars = new int[set.size()];

		/* For each edge */
		int k=0;
		for(formulation.Edge e : set) {
			// System.out.println("i:"+e.getSource()+ ", j:"+e.getDest());
			if(this.membership[e.getSource()] == this.membership[e.getDest()])
				edgeVars[k++] = 1;
			else
				edgeVars[k++] = 0;
		}
		// System.out.println("===================================");
		
		return(edgeVars);
	}


	
	
	
//	@Override
//	public int hashCode() {
//		final int prime = 31;
//		int result = 1;
//		result = prime * result + Arrays.hashCode(clusterSizes);
//		result = prime * result + n;
//		result = prime * result + nbCluster;
//		return result;
//	}

	
	// Overriding equals() to compare two Complex objects 
    @Override
    public boolean equals(Object o) { 
        // If the object is compared with itself then return true   
        if (o == this) { 
            return true; 
        } 
  
        /* Check if o is an instance of Complex or not "null instance of [type]" also returns false */
        if (!(o instanceof Clustering)) { 
            return false; 
        } 
          
        // type cast o to Complex so that we can compare data members  
        Clustering c = (Clustering) o; 

        // Compare the data members and return accordingly
        boolean equal = true;
        
        if(getNbCluster() != c.getNbCluster())
        	equal = false;
        else{
        	Map<Integer, Integer> assoc = new HashMap<>();
        	
	        for(int i=0; i<n; i++){ // for each node
	        	if(!assoc.containsKey(c.membership[i])) // fill in the hash map for the cluster ids that are just discovered
	        		assoc.put(c.membership[i], membership[i]); // ex: 3 --> 1, this means there is an association between cluster ids 3 and 1 
	        	else if(assoc.containsKey(c.membership[i])) {
	        		if(assoc.get(c.membership[i]) != membership[i]){ // if the association is not always maintained, this shows "different clusterings"
						equal = false;
						break;
					}
	        	}
			}
        }
        return equal; 
    } 
    
    
    
    
    /**
	 * Write a solution into file
	 * 
	 */
	public void writeMembership(String outputDirPath){
		String fileName = MEMBERSHIP_FILENAME + id + ".txt";
		writeMembership(outputDirPath, fileName);
	}
	
	
	
	/**
	 * Write a solution into file
	 * 
	 */
	public void writeMembership(String outputDirPath, String fileName){
		String filepath = outputDirPath + "/" + fileName;
		
		String content = "";
		for(int i=0; i<n; i++){ // for each node
			if(!content.equals(""))
				content += "\n";
			content += membership[i];
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
	
	
    /**
	 * read a solution from file
	 * 
	 */
	public static int[] readMembership(String inputDirPath, long id_, int n){
		String fileName = MEMBERSHIP_FILENAME + id_ + ".txt";
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
	
	
	
	@Override
    public String toString() { 
		ArrayList<ArrayList<Integer>> clusters = getClustersInArrayFormat();
		
		String content = "("+getId()+") ";
		for(int i=1; i<=nbCluster; i++){
			if(clusterSizes[i-1]>0){
				content += "Cluster" + i+": "; 
				ArrayList<Integer> clu = clusters.get(i-1);
				content += clu.toString();
				content += "\n";
			}
		}
		content += "imbalance: " + getImbalance() + "\n";
		
        return String.format(content); 
    }



	
}
