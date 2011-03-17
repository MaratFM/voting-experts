package edu.arizona.ve.experts;

import java.util.List;

import edu.arizona.ve.trie.Trie;


/**
*
* @author  Daniel Hewlett
*/
public class ForwardEntropyExpert extends Expert {

	public ForwardEntropyExpert(Trie trie) {
		super(trie);
	}

	@Override
	public boolean[] segment(List<String> segment) {
		int cutSize = segment.size() + 1;
		
		_scores = new double[cutSize];
		boolean[] votes = new boolean[cutSize];

		List<String> seg1;
		for (int i = 1; i < cutSize; ++i) {
			seg1 = segment.subList(0,i);
			
			_scores[i]  = _trie.getStdEntropy(seg1);
		}

		_scores[segment.size()]  = _trie.getStdEntropy(segment);

		double maxBEnt = _scores[1];

		int cutEnt = 1;

		for (int i = 1; i < cutSize; ++i) {
			if (_scores[i] >= maxBEnt) {
				maxBEnt = _scores[i];
				cutEnt = i;
			}            
		}

		votes[cutEnt] = true;
		
		return votes;
	}

	@Override
	public double[] getScores() {
		return _scores;
	}

}
