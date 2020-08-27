package myUtils;

import java.util.ArrayList;
import java.util.LinkedList;

public abstract class AbstractGraph {

	// A user define class to represent a graph. 
    // A graph is an array of adjacency lists. 
    // Size of array will be V (number of vertices 
    // in graph) 
    int V; 
    LinkedList<Integer>[] adjListArray; 
    
    
    // constructor 
    public AbstractGraph(int V) { 
        this.V = V; 
        // define the size of array as 
        // number of vertices 
        adjListArray = new LinkedList[V]; 
  
        // Create a new list for each vertex 
        // such that adjacent nodes can be stored 
  
        for(int i = 0; i < V ; i++){ 
            adjListArray[i] = new LinkedList<Integer>(); 
        } 
    } 
    
    
    abstract public void addEdge(int src, int dest);
      
    
    public boolean checkEdgeIfExists(int src, int dest) {
    	if(adjListArray[src].size()>0){
    		for(int d : adjListArray[src]){
    			if(dest == d)
    				return(true);
    		}
    	}
		return(false);
    }
    
    
    void DFSUtil(int v, boolean[] visited) { 
        // Mark the current node as visited and print it 
        visited[v] = true; 
        //System.out.print(v+" "); 
        // Recur for all the vertices 
        // adjacent to this vertex 
        for (int x : adjListArray[v]) { 
            if(!visited[x]) DFSUtil(x,visited); 
        } 
  
    } 
    
    public boolean isSingleConnectedComponent() { 
        // Mark all the vertices as not visited 
        boolean[] visited = new boolean[V]; 
        int v=0;
        DFSUtil(v,visited);
        
        for(int i=0; i<V; i++)
        	if(visited[i] == false)
        		return(false);
        
        return(true);
    }
    
    abstract public void displayGraph();
    
}
