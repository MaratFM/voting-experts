package edu.arizona.ve.experts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.arizona.ve.trie.Trie;

public class BackwardPhonemeToMorphemeExpert extends Expert {

	public BackwardPhonemeToMorphemeExpert(Trie backwardTrie) {
		super(backwardTrie);
	}

	@Override
	public boolean[] segment(List<String> segment) {
		boolean[] cuts = new boolean[segment.size() + 1];
		double[] scores = new double[segment.size() + 1]; 
		
		List<String> seg1, rev1;
		List<String> seg2, rev2;
		for (int i = segment.size()-1; i >= 1; i--) {
			seg1 = segment.subList(i, segment.size());
			seg2 = segment.subList(i-1, segment.size());
			
			rev1 = new ArrayList<String>(seg1);
			Collections.reverse(rev1);
			
			rev2 = new ArrayList<String>(seg2);
			Collections.reverse(rev2);
			
			// Backward
			scores[i-1] = _trie.getEntropy(rev2) - _trie.getEntropy(rev1);
		}
		
		double max = Double.NEGATIVE_INFINITY;
		int maxIndex = 0;
		for (int i = 1; i < scores.length; i++) {
			if (scores[i] > max) {
				maxIndex = i;
				max = scores[i];
			}
		}
		
		cuts[maxIndex] = true;
		
		return cuts;
	}

	@Override
	public double[] getScores() {
		// TODO Auto-generated method stub
		return null;
	}

}
