package edu.arizona.ve.experts;

import java.util.List;

import edu.arizona.ve.trie.Trie;

public class PhonemeToMorphemeExpert extends Expert {

	public PhonemeToMorphemeExpert(Trie trie) {
		super(trie);
	}

	@Override
	public boolean[] segment(List<String> segment) {
		boolean[] cuts = new boolean[segment.size() + 1];
		double[] scores = new double[segment.size() + 1]; 
		
		for (int i = 1; i < segment.size(); i++) {
			List<String> seg1 = segment.subList(0, i);
			List<String> seg2 = segment.subList(0, i+1);
			
			double diff = _trie.getEntropy(seg2) - _trie.getEntropy(seg1);
			scores[i+1] = diff;
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
