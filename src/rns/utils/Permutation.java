package rns.utils;

import java.util.ArrayList;
import java.util.List;

public class Permutation {

	public static void helper(List<int[]> permutations, int start, int[] input) {
	    if (start == input.length) {
	        //System.out.println(input);
	        int[] perm = input.clone();
	        permutations.add(perm);
	        return;
	    }
	    for (int i = start; i < input.length; i++) {
	        // swapping
	        int temp = input[i];
	        input[i] = input[start];
	        input[start] = temp;
	        // swap(input[i], input[start]);

	        helper(permutations, start + 1, input);
	        // swap(input[i],input[start]);

	        int temp2 = input[i];
	        input[i] = input[start];
	        input[start] = temp2;
	    }
	}
	
	public static List<int[]> permute(int[] input) {
	    List<int[]> permutations = new ArrayList<>();
	    helper(permutations, 0, input);
	    return permutations;
	}
	
}
