package edu.arizona.ve.experts;

import java.util.ArrayList;
import java.util.List;

import edu.arizona.ve.trie.Trie;

public class MarkovExpert extends Expert {

	// NOTE: This expert is not finished!
	
	/**
	 * Please make sure to provide a forward knowledge trie!
	 * @param trie
	 */
	public MarkovExpert(Trie trie) {
		super(trie);
	}

	// Fitness of inserting a boundary at location c in window w
//	public double fitness(int c, List<String> w) {
//		double freqBadCut = freqBadCut(c, w);
//		
//		freqSuffix(c, w) - freqBadCut - _trie.getStatNode(w.size());
//	}
	
	// What are the bounds on c expected by this? It seems that the paper may be 1-based?
	public double freqBadCut(int c, List<String> w) {
		double firstTerm = 0;
		for (int i = 2; i < c; i++) {
			for (int j = c + 1; j < w.size() - 1; j++) {
				ArrayList<String> bounded = new ArrayList<String>(w.subList(i, j));
				bounded.add(0, "*");
				bounded.add("*");
				firstTerm += _trie.getFreq(bounded);
			}
		}
		
		double secondTerm = 0;
		for (int i = 2; i < w.size(); i++) {
			ArrayList<String> bounded = new ArrayList<String>(w.subList(i, w.size()));
			bounded.add(0, "*");
			firstTerm += _trie.getFreq(bounded);
		}
		
		double thirdTerm = 0;
		for (int j = c + 1; j < w.size() - 1; j++) {
			ArrayList<String> bounded = new ArrayList<String>(w.subList(1, j));
			bounded.add("*");
			firstTerm += _trie.getFreq(bounded);
		}
		
		double fourthTerm = _trie.getFreq(w);
		
		return firstTerm + secondTerm + thirdTerm + fourthTerm;
	}
	
	public double freqSuffix(int c, List<String> w) {
		List<String> seg1 = w.subList(0, c);

		ArrayList<String> segK1 = new ArrayList<String>(seg1); 
		segK1.add("*");
		return _trie.getFreq(segK1);
	}
	
	public double freqPrefix(int c, List<String> w) {
		List<String> seg2 = w.subList(c, w.size());
		
		ArrayList<String> segK2 = new ArrayList<String>(seg2);
		segK2.add(0, "*");
		return _trie.getFreq(segK2);
	}
	
	@Override
	public boolean[] segment(List<String> segment) {
		int cutSize = segment.size() + 1;
		
		boolean[] votes = new boolean[cutSize];
		double[] scorefKnowledge = new double[segment.size()+1];
		for (int i = 0; i < scorefKnowledge.length; i++) {
			scorefKnowledge[i] = Double.POSITIVE_INFINITY;
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
			scorefKnowledge[i] = _trie.getStdIntEntropy(segK1);
			
			ArrayList<String> segK2 = new ArrayList<String>(seg2);
			segK2.add(0, "*");
			scorefKnowledge[i] += _trie.getStdIntEntropy(segK2);
			
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
		for (int i = 0; i < scorefKnowledge.length; ++i) {
			if (scorefKnowledge[i] < minfKnowledgeEnt) {
				minfKnowledgeEnt = scorefKnowledge[i];
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
		// TODO Auto-generated method stub
		return null;
	}

}
