package edu.arizona.ve.experts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import edu.arizona.ve.trie.Trie;

public class ChunkinessExpert extends Expert {

	Trie _backwardTrie;
	boolean _reversed = false; 
	
	public ChunkinessExpert(Trie trie, Trie backwardTrie) {
		super(trie);
		_backwardTrie = backwardTrie;
	}
	
	public ChunkinessExpert(Trie trie) {
		super(trie);
		throw new RuntimeException("YOU MUST USE A BACKWARD TRIE");
	}
	
	public void reverse() {
		_reversed = true;
	}
	
	public double computeForwardChunkiness(double hf, double hb, double surp) {
		return hf - surp; 
	}
	
	public double computeBackwardChunkiness(double hf, double hb, double surp) {
		return hb - surp;
	}
	
	public double computeForwardChunkiness(List<String> s) {
		double surp = _trie.getStdIntEntropy(s);
		double hf = _trie.getStdEntropy(s);
		List<String> rev = new ArrayList<String>(s);
		Collections.reverse(rev);
		double hb = _backwardTrie.getStdEntropy(rev);
		
		return computeForwardChunkiness(hf, hb, surp);
	}
	
	public double computeBackwardChunkiness(List<String> s) {
		double surp = _trie.getStdIntEntropy(s);
		double hf = _trie.getStdEntropy(s);
		List<String> rev = new ArrayList<String>(s);
		Collections.reverse(rev);
		double hb = _backwardTrie.getStdEntropy(rev);
		
		return computeBackwardChunkiness(hf, hb, surp);
	}
	
	@Override
	public boolean[] segment(List<String> segment) {
		int cutSize = segment.size() + 1;
		
		double[] fScore = new double[cutSize];
		Arrays.fill(fScore, Double.NEGATIVE_INFINITY);
		boolean[] votes = new boolean[cutSize];
		
		List<String> seg1, seg2;
		for (int i = 0; i <= segment.size(); ++i) {
			seg1 = segment.subList(0,i);
			seg2 = segment.subList(i, segment.size());
			
			if (seg1.isEmpty()) {
				fScore[i] = computeBackwardChunkiness(seg2);
			} else if (seg2.isEmpty()) {
				fScore[i] = computeForwardChunkiness(seg1);
			} else {
				fScore[i] = computeForwardChunkiness(seg1);
				fScore[i] += computeBackwardChunkiness(seg2);
			}
		}

		int cutF = 0;
		double maxF = Double.NEGATIVE_INFINITY;
		
		for (int i = 0; i <= segment.size(); ++i) {
			if (fScore[i] > maxF) {
				maxF = fScore[i];
				cutF = i;
			}
		}

		votes[cutF] = true;
		
		return votes;
	}

	@Override
	public double[] getScores() {
		// TODO Auto-generated method stub
		return null;
	}

}
