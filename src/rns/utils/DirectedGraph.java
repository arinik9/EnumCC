package rns.utils;

import java.util.ArrayList;
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
//		ArrayList<Integer> myList = new ArrayList<Integer>();
//		myList.add(0);
//		myList.add(1);
//		myList.add(9);
//		myList.add(2);
//		myList.add(13);

		for(int v=0; v<V; v++){
//		for(int v : myList){
			System.out.println("node: " + v + " => in neighbors: [" + getInNeighbors(v).toString() + "]");
			System.out.println("node: " + v + " => out neighbors: [" + getOutNeighbors(v).toString() + "]");
		}
	}
	
}
