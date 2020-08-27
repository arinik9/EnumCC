package myUtils;

import java.util.LinkedList;

public class DirectedGraph extends AbstractGraph {

    LinkedList<Integer>[] inAdjListArray; 

    
    // constructor 
    public DirectedGraph(int V) { 
        super(V);
        
        inAdjListArray = new LinkedList[V]; 
        
        // Create a new list for each vertex 
        // such that adjacent nodes can be stored 
  
        for(int i = 0; i < V ; i++){ 
        	inAdjListArray[i] = new LinkedList<Integer>(); 
        } 
    } 
    
    // Adds an edge to an undirected graph 
    public void addEdge(int src, int dest) {
    	if(!checkEdgeIfExists(src, dest)){
	        // Add an edge from src to dest. 
	        adjListArray[src].add(dest); // outgoing
	        inAdjListArray[dest].add(src); // incoming
    	}
    }

    public LinkedList<Integer> getInNeighbors(Integer nodeId) {
		return inAdjListArray[nodeId];
	}
    
    public LinkedList<Integer> getOutNeighbors(Integer nodeId) {
		return adjListArray[nodeId];
	}
    
	@Override
	public void displayGraph() {
		for(int v=0; v<V; v++){
			System.out.println("node: " + v + " => in neighbors: [" + getInNeighbors(v).toString() + "]");
			System.out.println("node: " + v + " => out neighbors: [" + getOutNeighbors(v).toString() + "]");
		}
	}
	
}
