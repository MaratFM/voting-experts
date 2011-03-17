package edu.arizona.ve.experts;

import java.util.List;

import edu.arizona.ve.trie.Trie;

/**
*
* @author  Daniel Hewlett
*/
public class SurprisalExpert extends Expert {

	// This is the "Internal Entropy" expert
	public SurprisalExpert(Trie trie) {
		super(trie);
	}

	public boolean[] segment(List<String> segment) {
		int cutSize = segment.size() + 1;
		
		_scores = new double[cutSize];
		boolean[] votes = new boolean[cutSize];
		
		List<String> seg1, seg2;
		for (int i = 1; i < segment.size(); ++i) {
			seg1 = segment.subList(0,i);
			seg2 = segment.subList(i, segment.size());
			
			_scores[i] = _trie.getStdIntEntropy(seg1) + _trie.getStdIntEntropy(seg2);
		}

		_scores[segment.size()] = _trie.getStdIntEntropy(segment);

		int cutFreq = 1;
		double min = Double.MAX_VALUE;
		for (int i = 1; i <= segment.size(); ++i) {
			if (_scores[i] <= min) {
				min = _scores[i];
				cutFreq = i;
			}
		}

		votes[cutFreq] = true;
		
		return votes;
	}

	@Override
	public double[] getScores() {
		double[] posScores = new double[_scores.length];
		for (int i = 0; i < posScores.length; i++) {
			posScores[i] = -_scores[i];
		}
		return posScores;
	}

}
