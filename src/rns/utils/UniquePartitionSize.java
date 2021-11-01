package rns.utils;

import java.util.ArrayList;
import java.util.List;

public class UniquePartitionSize {
	
	private static void helper(List<int[]> sizes, List<int[]> inputs, int n, int r, int index) {
		
		for(int[] input : inputs){ // input: {2,1,1}
			List<int[]> queue = new ArrayList<>();
			//System.out.println(input.toString());
			boolean ok = true;
			while(sum(input)<(n-1)){
				input[index]++;
				
				if(index==0)
					queue.add(input.clone());
				if(index>0 && input[index-1]>input[index])
					queue.add(input.clone());
				else if(index>0 && input[index-1]<=input[index])
					ok = false;
			}
			
			if(ok){
				input[index]++;
				if(!checkIfEqualPartition(input, n, r))
					sizes.add(input.clone());
			}
			
			if(queue.size()>0 && (index+1)<r)
				helper(sizes, queue, n, r, index+1);
		}
		
	}
	
	
	static int sum(int[] input){
		int result = 0;
		for(int i=0; i<input.length; i++)
			result += input[i];
		return(result);
	}
	
	
	static boolean checkIfEqualPartition(int[] input, int n, int r){
		int val = n/r;
		boolean equal = true;
		for(int i=0; i<input.length; i++)
			if(input[i]!=val){
				equal=false;
				break;
			}
		return(equal);
	}
	
	
	/*
	 * input: n=5, r=3
	 * 
	 * output:
	 * [3, 1, 1]
	 * [2, 2, 1]
	 * [1, 3, 1]
	 * [1, 2, 2]
	 * [1, 1, 3]
	 * [2, 1, 2]
	 */
	public static List<int[]> generate(int n, int r) {
		int[] input = new int[r];
		for(int i=0;i<r;i++)
		input[i] = 1; // input = {1,1,1} if n=3

		List<int[]> sizes = new ArrayList<>();
		if(sum(input) < n){
			List<int[]> queue = new ArrayList<>();
			queue.add(input.clone());
			helper(sizes, queue, n, r, 0);
		}
		
		// handle the equal partition case
		if(n%r == 0){
			int val = n/r;
			for(int i=0;i<r;i++)
				input[i] = val;
			sizes.add(input);
		}
		return sizes;
	}
}
