package edu.arizona.ve.experts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.arizona.ve.trie.Trie;


/**
*
* @author  Daniel Hewlett
*/
public class MorphemeExpert extends Expert {
	Trie _backwardTrie;
	
	public MorphemeExpert(Trie trie) {
		super(trie);
	}

	public MorphemeExpert(Trie fTrie, Trie bTrie) {
		super(fTrie);
		_backwardTrie = bTrie;
	}
	
	@Override
	public boolean[] segment(List<String> segment) {
		int cutSize = segment.size() + 1;
		
		double[] score = new double[cutSize];
		boolean[] votes = new boolean[cutSize];
		
		List<String> sub;
		for (int i = 1; i < cutSize; ++i) {
			sub = segment.subList(0,i);
			
			List<String> rev = new ArrayList<String>(sub);
			Collections.reverse(rev);
			
			score[i] = _trie.getChildren(sub).size() + _backwardTrie.getChildren(rev).size();
		}			

		double maxScore = Double.NEGATIVE_INFINITY;
		int cutPoint = 0;
		double threshold = 37;
		
		for (int i = 2; i < cutSize; i++) {
			if (score[i] >= maxScore) {
				maxScore = score[i];
				cutPoint = i;
			}
		}

		
		if (maxScore > threshold) {
//			List<String> subList = segment.subList(0, cutPoint);
//			if (subList.get(0).equals("u")) {
//				System.out.println(subList);
//			}
			votes[0] = true;
			votes[cutPoint] = true;
		}
			
		return votes;
	}

	@Override
	public double[] getScores() {
		// TODO Auto-generated method stub
		return null;
	}

}
