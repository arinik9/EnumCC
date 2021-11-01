package rns.utils;

import java.util.ArrayList;
import java.util.List;

public class Combination2 {

	public static List<ArrayList<Integer>> generate(ArrayList<List<Integer>> valuesList) {
		//int r = valuesList.size();
		
		// [1, ..,  ..], [2, .., ..] ==> [1, 3,  ...], [2, 3, ..], [1, 4,  ...], [2, 4, ..]
		List<ArrayList<Integer>> collector = new ArrayList<ArrayList<Integer>>();
		
		
		for(List<Integer> values : valuesList){ // possible target cluster ids for for each node
			
			if(collector.size()>0){
				ArrayList<ArrayList<Integer>> collectorCopy = new ArrayList<ArrayList<Integer>>();
				for(ArrayList<Integer> arr : collector){
					collectorCopy.add(new ArrayList<Integer>(arr));
				}
				collector.clear();
				
				for(Integer val : values){
					ArrayList<ArrayList<Integer>> collector2 = new ArrayList<ArrayList<Integer>>();

					for(ArrayList<Integer> currVal : collectorCopy){
						ArrayList<Integer> l = new ArrayList<>(currVal);
						l.add(val);
						collector2.add(l);
					}
					collector.addAll(collector2);
				}
				
				
			} else {
				
				ArrayList<ArrayList<Integer>> collector2 = new ArrayList<ArrayList<Integer>>();
				for(Integer val : values){
					ArrayList<Integer> l = new ArrayList<>();
					l.add(val);
					collector2.add(l);
				}
				collector.addAll(collector2);
			}
		}

		return collector;
	}
	
}
