package edu.arizona.ve.experts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.arizona.ve.trie.Trie;

/**
*
* @author  Daniel Hewlett
*/
public class BackwardEntropyExpert extends Expert {

	// Make sure trie is a backward trie!
	public BackwardEntropyExpert(Trie backwardTrie) {
		super(backwardTrie);
	}

	@Override
	public boolean[] segment(List<String> segment) {
		int cutSize = segment.size() + 1;
		
		double[] scoreBEnt = new double[cutSize];
		boolean[] votes = new boolean[cutSize];
		
		List<String> seg, rev;
		for (int i = 0; i < segment.size(); ++i) {
			seg = segment.subList(i, segment.size());
	
			rev = new ArrayList<String>(seg);
			Collections.reverse(rev);
			
			// Backward
			scoreBEnt[i] = _trie.getStdEntropy(rev);
		}

		double maxBEnt  = scoreBEnt[1];

		int cutEnt = 0;

		for (int i = 0; i < cutSize; ++i) {
			if (scoreBEnt[i] >= maxBEnt) {
				maxBEnt = scoreBEnt[i];
				cutEnt = i;
			}            
		}

		votes[cutEnt] = true;
		
		return votes;
	}

	@Override
	public double[] getScores() {
		// TODO Auto-generated method stub
		return null;
	}

}
