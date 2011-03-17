package edu.arizona.ve.experts;

import java.util.ArrayList;
import java.util.List;

import edu.arizona.ve.trie.Trie;


/**
*
* @author  Daniel Hewlett
*/
public class KnowledgeExpert extends Expert {

	/**
	 * Please make sure to provide a forward knowledge trie!
	 * @param trie
	 */
	public KnowledgeExpert(Trie trie) {
		super(trie);
	}

	@Override
	public boolean[] segment(List<String> segment) {
		int cutSize = segment.size() + 1;
		
		boolean[] votes = new boolean[cutSize];
		_scores = new double[segment.size()+1];
		for (int i = 0; i < _scores.length; i++) {
			_scores[i] = Double.POSITIVE_INFINITY;
		}
		
		// Special case for beginning 
//		ArrayList<String> segK0 = new ArrayList<String>(segment); 
//		segK0.add(0, "*");
//		scorefKnowledge[0] = _trie.getStdIntEntropy(segK0); 
//		System.out.println(scorefKnowledge[0] + " " + segK0);
		
		List<String> seg1, seg2;
		for (int i = 1; i < segment.size(); ++i) {
			seg1 = segment.subList(0,i);
			seg2 = segment.subList(i, segment.size());
			
			// Prior Knowledge 
			ArrayList<String> segK1 = new ArrayList<String>(seg1); 
			segK1.add("*");
			_scores[i] = _trie.getStdIntEntropy(segK1);
			
			ArrayList<String> segK2 = new ArrayList<String>(seg2);
			segK2.add(0, "*");
			_scores[i] += _trie.getStdIntEntropy(segK2);
			
//			System.out.println(scorefKnowledge[i] + " " + segK1 + " || " + segK2);
		}

		// special case for end
//		ArrayList<String> segK = new ArrayList<String>(segment); 
//		segK.add("*");
//		scorefKnowledge[segment.size()] = _trie.getStdIntEntropy(segK); 
//		System.out.println(scorefKnowledge[segment.size()] + " " + segK);
		
		double minfKnowledgeEnt = Double.POSITIVE_INFINITY;

		int cutfKnowledge = -1;

		// <= ? that should favor pushing the tie to the next item - do we want that?
		for (int i = 0; i < _scores.length; ++i) {
			if (_scores[i] < minfKnowledgeEnt) {
				minfKnowledgeEnt = _scores[i];
				cutfKnowledge = i;
			}
		}

		if (cutfKnowledge != -1) {
			votes[cutfKnowledge] = true;
		} else {
//			System.out.println("Don't know anything about " + segment);
		}
		
		return votes;
	}

	@Override
	public double[] getScores() {
		double[] posScores = new double[_scores.length];
		for (int i = 0; i < posScores.length; i++) {
			if (Double.isInfinite(_scores[i])) {
				posScores[i] = 0;
			} else {
				posScores[i] = -_scores[i];
			}
		}
		return posScores;
	}

}
