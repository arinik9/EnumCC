package myUtils;

import java.util.ArrayList;
import java.util.LinkedList;

public class UndirectedGraph extends AbstractGraph {

    // constructor 
    public UndirectedGraph(int V) { 
        super(V);
    } 
    
    // Adds an edge to an undirected graph 
    public void addEdge(int src, int dest) {
    	if(!checkEdgeIfExists(src, dest)){
	        // Add an edge from src to dest. 
	        adjListArray[src].add(dest); 
	  
	        // Since graph is undirected, add an edge from dest 
	        // to src also 
	        adjListArray[dest].add(src);
    	}
    }


    public LinkedList<Integer> getNeighbors(Integer nodeId) {
		return adjListArray[nodeId];
	}

    
	@Override
	public void displayGraph() {
		for(int v=0; v<V; v++){
			System.out.println("node: " + v + " => neighbors: [" + getNeighbors(v).toString() + "]");
		}
	}
    
    
    
      
//    // Driver program to test above 
//    public static void main(String[] args){ 
//        // Create a graph given in the above diagram  
//        UndirectedGraph g = new UndirectedGraph(5); // 5 vertices numbered from 0 to 4  
//          
//        g.addEdge(1, 0);  
//        g.addEdge(2, 3);  
//        g.addEdge(3, 4); 
//        System.out.println("Following are connected components"); 
//        g.connectedComponents(); 
//    } 
}     
