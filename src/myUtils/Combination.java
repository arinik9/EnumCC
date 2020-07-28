package myUtils;

import java.util.ArrayList;
import java.util.List;

public class Combination { 
	  
	private static void helper(List<int[]> combinations, int input[], int data[], int start, int end, int index) {
	    if (index == data.length) {
	        int[] combination = new int[data.length];
	        for(int i=0;i<data.length;i++)
	        	combination[i] = input[data[i]];
	        combinations.add(combination);
	        return;
	    } else if (start <= end) {
	        data[index] = start;
	        helper(combinations, input, data, start + 1, end, index + 1);
	        helper(combinations, input, data, start + 1, end, index);
	    }
	}
	
	public static List<int[]> generate(int[] input, int r) {
		int n = input.length;
	    List<int[]> combinations = new ArrayList<>();
	    helper(combinations, input, new int[r], 0, n-1, 0);
	    return combinations;
	}
	
	public static List<int[]> generate(List<Integer> input_, int r) {
		int[] input = new int[input_.size()];
		for(int i=0;i<input_.size();i++)
			input[i] = input_.get(i);
		List<int[]> combinations = generate(input, r);
		return combinations;
	}
	
	public static List<int[]> generate(int n, int r) {
		int[] input = new int[n];
		for(int i=1;i<=n;i++)
			input[i] = i;
		List<int[]> combinations = generate(input, r);
		return combinations;
	}
	
}