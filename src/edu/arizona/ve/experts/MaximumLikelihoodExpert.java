package edu.arizona.ve.experts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.trie.Trie;

public class MaximumLikelihoodExpert extends Expert {

	HashMap<List<String>, int[]> _data;
	
	public MaximumLikelihoodExpert(Trie trie) {
		super(trie);
		throw new RuntimeException("USE THE OTHER CONSTRUCTOR!");
	}
	
	public MaximumLikelihoodExpert(Corpus c, boolean[] myCuts, int windowSize) {
		super(null);
		
		HashMap<List<String>,HashMap<List<Boolean>,Integer>> hashMapData = fillHashMapData(c, myCuts, windowSize);
		_data = getMagicExpertData(hashMapData);
	}

	 //used to roll or fold all data into a usable form for the magic experts
    public static HashMap<List<String>, int[]> getMagicExpertData(HashMap<List<String>, HashMap<List<Boolean>, Integer>> m) {
    	Set<List<String>> keys = m.keySet();
    	HashMap<List<String>, int[]> data = new HashMap<List<String>, int[]>();
    	
    	for(List<String> i : keys) {
    		HashMap<List<Boolean>, Integer> temp = m.get(i);
    		Set<List<Boolean>> keys2 = temp.keySet();
    		int[] temp2 = new int[i.size() + 1];
    		for(List<Boolean> j : keys2) {
    			for(int z=0;z<j.size();z++) {
    				if(j.get(z))
    					temp2[z] += temp.get(j);
    			}
    		}
    		data.put(i, temp2);
    	}
    	return data;
    }
    
    //put data from corpus into usable form for error analysis
    public static HashMap<List<String>, HashMap<List<Boolean>, Integer>> fillHashMapData(Corpus corpus, boolean[] myCuts, int windowSize) {
    	ArrayList<Boolean> cuts = new ArrayList<Boolean>();
        cuts.add(true);
        for (int i = 0; i < myCuts.length; i++) 
                cuts.add(myCuts[i]);
        cuts.add(true);
    	
    	int val = 0;
    	HashMap<List<String>, HashMap<List<Boolean>, Integer>> m = new HashMap<List<String>, HashMap<List<Boolean>, Integer>>();
    	
    	for (int i = 0; i <= corpus.getCleanChars().size() - windowSize; i++) {
    		
            List<String> subSeq = Collections.unmodifiableList(corpus.getCleanChars().subList(i, i + windowSize));
            List<Boolean> actualCuts = Collections.unmodifiableList(cuts.subList(i, i + windowSize + 1));
            
        	HashMap<List<Boolean>, Integer> temp = new HashMap<List<Boolean>, Integer>();
            temp.put(actualCuts, 1);
            
            if(m.containsKey(subSeq)) {
            	temp = m.get(subSeq);
            	if(temp.containsKey(actualCuts)) {
            		val = temp.get(actualCuts);
            		temp.put(actualCuts, ++val);
            		//m.put(subSeq, temp);
            	}
            	else {
            		temp.put(actualCuts, 1);
            		//m.put(subSeq, temp);
            	}
            }
            else
            	m.put(subSeq, temp);
    	}
    	return m;
    }
	
	public boolean[] segment(List<String> segment) {
		
        int[] ta = _data.get(segment);
		int cutSize = segment.size() + 1;
		boolean[] votes = new boolean[cutSize];
		
		int max = 0, vote = 0;
		
		for (int i = 1; i < ta.length; ++i) {
			if(ta[i] > max) {
				max = ta[i];
				vote = i;
			}
		}

		if (max > 0)
			votes[vote] = true;
		
		return votes;
	}

	@Override
	public double[] getScores() {
		// TODO Auto-generated method stub
		return null;
	}
}
