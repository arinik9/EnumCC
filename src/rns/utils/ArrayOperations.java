package rns.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

public class ArrayOperations {

	
	// Function to return the first occurence of a value in a given array
    public static int whichIndex(int[] arr, int val) 
    { 
    	for(int i=0; i<arr.length; i++){
    		if(arr[i] == val)
    			return(i);
    	}
    	return(-1);
    }
    
    
	// Function to ...
    public static int[] seq(int startIndex, int endIndex) 
    { 
    	int nb = (endIndex-startIndex+1);
    	int[] arr = new int[nb];
    	for(int i=0; i<nb; i++){
    		arr[i]=startIndex+i;
    	}
    	return(arr);
    }
    
    
 // Function to remove the element 
    public static int[] rep(int val, int times) 
    { 
    	int[] arr = new int[times];
    	for(int i=0; i<times; i++){
    		arr[i]=val;
    	}
    	return(arr);
    }
    
    
    // Function to remove the element 
    public static int[] assignSameValue(int[] arr, int[] indexes, int val) 
    { 
    	for(int i=0; i<indexes.length; i++){
    		arr[indexes[i]]=val;
    	}
    	return(arr);
    }
    
    // Function to remove the element 
    public static boolean isIdentical(int[] arr1, int[] arr2) 
    { 
    	if(arr1.length != arr2.length)
    		return(false);
    	
    	for(int i=0; i<arr1.length; i++){
    		if(arr1[i] != arr2[i])
    			return(false);
    	}
    	return(true);
    }
    
    
	// Function to remove the element 
    public static int[] removeTheElement(int[] arr, 
                                          int index) 
    { 
  
        // If the array is empty 
        // or the index is not in array range 
        // return the original array 
        if (arr == null
            || index < 0
            || index >= arr.length) { 
  
            return arr; 
        } 
  
        // Create another array of size one less 
        int[] anotherArray = new int[arr.length - 1]; 
  
        // Copy the elements except the index 
        // from original array to the other array 
        for (int i = 0, k = 0; i < arr.length; i++) { 
  
            // if the index is 
            // the removal element index 
            if (i == index) { 
                continue; 
            } 
  
            // if the index is not 
            // the removal element index 
            anotherArray[k++] = arr[i]; 
        } 
  
        // return the resultant array 
        return anotherArray; 
    } 
    
    
 // Function to remove the element 
    public static int[] removeMultipleElements(int[] arr, int[] indexes) 
    { 
    	Arrays.sort(indexes);
    	
    	for(int i=(indexes.length-1); i>=0; i--){
    		arr = removeTheElement(arr, indexes[i]);
    	}
    	return(arr);
    }
    
    
    
    
    
    // Function to merge
    public static int[] mergeMultipleArrays(ArrayList<int[]> arrArrays) 
    { 
    	int[] concan = arrArrays.get(0);
    	for(int i=1; i<arrArrays.size(); i++){
    		concan = IntStream.concat(IntStream.of(concan), IntStream.of(arrArrays.get(i))).toArray();
    	}
    	
    	return(concan);
    }
    
    
    // Function to merge
    public static int[] mergeArrayAndArrayList(ArrayList<Integer> arraylist, int[] array) 
    { 
    	int len = array.length + arraylist.size();
    	int[] newArray = new int[len - 1]; 
    	
    	int i;
    	for(i=0; i<arraylist.size(); i++){
    		newArray[i] = arraylist.get(i);
    	}
    	
    	i++;
    	for(int j=0; j<array.length; j++){
    		newArray[i++] = array[j];
    	}
    	
    	return(newArray);
    }
    
    
}
