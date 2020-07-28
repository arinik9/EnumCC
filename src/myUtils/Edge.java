package myUtils;


/**
 * ID of an edge i,j (with i < j)
 * @author Nejat ARINIK
 *
 */
public class Edge {
	int i;
	int j;
	double weight;
	int hashcode;

	// it is used in Enumeration
	public Edge(int i, int j, double weight_){
		if(i<j){
			this.i=i;
			this.j=j;
		} else {
			this.i=j;
			this.j=i;
		}
		this.weight = weight_;
		
		// TODO Is this hash code is good enough? it is ok up to 1000 nodes
		//hashcode = 1000 * this.i + this.j;
	}
	
	// it is used in primal heuristic
	public Edge(int i, int j){
		if(i<j){
			this.i=i;
			this.j=j;
		} else {
			this.i=j;
			this.j=i;
		}
		
		// TODO Is this hash code is good enough? it is ok up to 1000 nodes
		hashcode = 1000 * this.i + this.j;
	}
	
	
    /**
     * Returns either endpoint of this edge.
     *
     * @return either endpoint of this edge
     */
    public int either() {
        return i;
    }
	
	
	public int getOtherVertexId(int id){
		if(id == i)
			return(j);
		else if(id == j)
			return(i);
		else throw new IllegalArgumentException("Illegal endpoint");
	}

	
	@Override
	public int hashCode(){	
		return hashcode;
	}

	
	@Override
	public boolean equals(Object o){

		if (this==o)
			return true;
		if (o instanceof Edge) {
			Edge e = (Edge)o;
			return (this.i == e.i && this.j == e.j);
		}
		return false;
	}
	
	public int getSource() {
		return this.i;
	}
	
	public int getDest() {
		return this.j;
	}
	
	public double getweight() {
		return this.weight;
	}
	

	@Override
    public String toString() { 
		return("(i:"+getSource()+", j:"+getDest()+", w:"+getweight()+")");
	}
}