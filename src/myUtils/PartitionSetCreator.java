package myUtils;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PartitionSetCreator<T> {

    private Set<Set<Set<T>>> parts;//the partitions that are created
    private Set<Set<T>> pow;//the power set of the input set
    private Set<T> base;//the base set

    /**
     * The main method is just for testing and can be deleted.
     */
    public static void main(String[] args) {
        //test using an empty set = []
        Set<Integer> baseSet = new HashSet<Integer>();
        PartitionSetCreator<Integer> partSetCreatorEmpty = new PartitionSetCreator<Integer>(baseSet);
        Set<Set<Set<Integer>>> partitionSetsEmpty = partSetCreatorEmpty.findAllPartitions();
        System.out.println("BaseSet: " + baseSet);
        System.out.println("Result:  " + partitionSetsEmpty);
        System.out.println("Base-Size: " + baseSet.size() + " Result-Size: " + partitionSetsEmpty.size());

        //test using base set = [1]
        baseSet.add(1);
        PartitionSetCreator<Integer> partSetCreator = new PartitionSetCreator<Integer>(baseSet);
        Set<Set<Set<Integer>>> partitionSets = partSetCreator.findAllPartitions();
        System.out.println("BaseSet: " + baseSet);
        System.out.println("Result:  " + partitionSets);
        System.out.println("Base-Size: " + baseSet.size() + " Result-Size: " + partitionSets.size());

        //test using base set = [1, 2]
        baseSet.add(2);
        PartitionSetCreator<Integer> partSetCreator2 = new PartitionSetCreator<Integer>(baseSet);
        Set<Set<Set<Integer>>> partitionSets2 = partSetCreator2.findAllPartitions();
        System.out.println("BaseSet: " + baseSet);
        System.out.println("Result:  " + partitionSets2);
        System.out.println("Base-Size: " + baseSet.size() + " Result-Size: " + partitionSets2.size());

        //another test using base set = [1, 2, 3]
        baseSet.add(3);
        PartitionSetCreator<Integer> partSetCreator3 = new PartitionSetCreator<Integer>(baseSet);
        Set<Set<Set<Integer>>> partitionSets3 = partSetCreator3.findAllPartitions();
        System.out.println("BaseSet: " + baseSet);
        System.out.println("Result:  " + partitionSets3);
        System.out.println("Base-Size: " + baseSet.size() + " Result-Size: " + partitionSets3.size());

        //another test using base set = [1, 2, 3, 4]
        baseSet.add(4);
        PartitionSetCreator<Integer> partSetCreator4 = new PartitionSetCreator<Integer>(baseSet);
        Set<Set<Set<Integer>>> partitionSets4 = partSetCreator4.findAllPartitions();

        System.out.println("BaseSet: " + baseSet);
        System.out.println("Result:  " + partitionSets4);
        System.out.println("Base-Size: " + baseSet.size() + " Result-Size: " + partitionSets4.size());
    }

    public PartitionSetCreator(Set<T> base) {
        this.base = base;
        this.pow = powerSet(base);
        if (pow.size() > 1) {
            //remove the empty set if it's not the only entry in the power set
            pow.remove(new HashSet<T>());           
        }
        this.parts = new HashSet<Set<Set<T>>>();
    }

    /**
     * Calculation is in this method.
     */
    public Set<Set<Set<T>>> findAllPartitions() {
        //find part sets for every entry in the power set
        for (Set<T> set : pow) {
            Set<Set<T>> current = new HashSet<Set<T>>();
            current.add(set);
            findPartSets(current);
        }

        //return all partitions that were found
        return parts;
    }

    /**
     * Finds all partition sets for the given input and adds them to parts (global variable).
     */
    private void findPartSets(Set<Set<T>> current) {
        int maxLen = base.size() - deepSize(current);
        if (maxLen == 0) {
            //the current partition is full -> add it to parts
            parts.add(current);
            //no more can be added to current -> stop the recursion
            return;
        }
        else {
            //for all possible lengths
            for (int i = 1; i <= maxLen; i++) {
                //for every entry in the power set
                for (Set<T> set : pow) {
                    if (set.size() == i) {
                        //the set from the power set has the searched length
                        if (!anyInDeepSet(set, current)) {
                            //none of set is in current
                            Set<Set<T>> next = new HashSet<Set<T>>();
                            next.addAll(current);
                            next.add(set);
                            //next = current + set
                            findPartSets(next);
                        }
                    }
                }
            }
        }
    }

    /**
     * Creates a power set from the base set.
     */
    private Set<Set<T>> powerSet(Set<T> base) {
        Set<Set<T>> pset = new HashSet<Set<T>>();
        if (base.isEmpty()) {
            pset.add(new HashSet<T>());
            return pset;
        }
        List<T> list = new ArrayList<T>(base);
        T head = list.get(0);
        Set<T> rest = new HashSet<T>(list.subList(1, list.size()));
        for (Set<T> set : powerSet(rest)) {
            Set<T> newSet = new HashSet<T>();
            newSet.add(head);
            newSet.addAll(set);
            pset.add(newSet);
            pset.add(set);
        }

        return pset;
    }

    /**
     * The summed up size of all sub-sets
     */
    private int deepSize(Set<Set<T>> set) {
        int deepSize = 0;
        for (Set<T> s : set) {
            deepSize += s.size();
        }
        return deepSize;
    }

    /**
     * Checks whether any of set is in any of the sub-sets of current
     */
    private boolean anyInDeepSet(Set<T> set, Set<Set<T>> current) {
        boolean containing = false;

        for (Set<T> s : current) {
            for (T item : set) {
                containing |= s.contains(item);
            }
        }

        return containing;
    }
}