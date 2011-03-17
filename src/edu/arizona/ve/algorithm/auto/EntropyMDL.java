package edu.arizona.ve.algorithm.auto;

import java.util.Arrays;
import java.util.List;

import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.mdl.MDL;
import edu.arizona.ve.trie.Trie;

public class EntropyMDL implements AutoSegmenter {

	boolean[] _lastCuts = null;
	double[] _scores = null;
	
	/*************************************************************************
	 * Algorithm 1 from (Zhikov et al., 2010). Generates an initial hypothesis 
	 * by finding the optimal threshold for some set of boundary scores. 
	 * The optimal threshold is found via a greedy search using the minimum 
	 * description length as the evaluation method.
	 * @param c The corpus.
	 * @param scores The boundary scores, where higher scores
	 * constitute more likely boundaries.
	 * @return A segmentation based on the optimal threshold.
	 */
	@Override
	public void runAlgorithm(Corpus corpus, Trie forwardTrie, Trie backwardTrie, int windowSize) {
		// Compute the combined branching entropy at each possible boundary location
		
		_scores = getBranchingEntropy(corpus, forwardTrie, backwardTrie, windowSize);
		
		// Sort scores into thresholds
		assert(corpus.getCutPoints().length == _scores.length);
		double[] thresholds = Arrays.copyOf(_scores, _scores.length);
		Arrays.sort(thresholds);
		
		// Seed initial hypothesis at median threshold
		int tempPos, pos = thresholds.length / 2;
		int step = thresholds.length / 4;
		int dir = 1; // ascending
		double tempDL, mdl = simulateSegmentation(corpus, _scores, thresholds[pos]);
		
		// Binary search for better threshold
		while (step > 0) {
			tempPos = pos + dir*step;
			tempDL = simulateSegmentation(corpus, _scores, thresholds[tempPos]);
			if (tempDL < mdl) {
				mdl = tempDL;
				pos = tempPos; 
				step /= 2;
				continue;
			}
			dir *= -1;
			tempPos = pos + dir*step;
			tempDL = simulateSegmentation(corpus, _scores, thresholds[tempPos]);
			if (tempDL < mdl) {
				mdl = tempDL;
				pos = tempPos; 
				step /= 2;
				continue;
			}
			dir *= -1;
			step /= 2;
		}
		
		_lastCuts = new boolean[_scores.length];
		segmentByThreshold(_lastCuts, _scores, thresholds[pos]);
	}
	
	@Override
	public boolean[] getCutPoints() {
		return _lastCuts;
	}

	@Override
	public double[] getScores() {
		return _scores;
	}

	@Override
	public String getName() {
		return "EntropyMDL";
	}
	
	///////////////////////////////////
	// Helper Functions
	
	public double[] getBranchingEntropy(Corpus c, Trie f, Trie b, int windowSize) {
		double[] H = new double[c.getCutPoints().length];
		for (int i = 0; i < H.length; i++) {
			H[i] = 0.;
		}
		
		for (int i = 1; i <= windowSize; i++) {
			accumulateEntropy(H, i, c, f, b);
		}
		
		return H;
	}
	
	private void accumulateEntropy(double[] H, int winLen,
			Corpus c, Trie fTrie, Trie bTrie) {
		List<String> fChars = c.getCleanChars();
		List<String> bChars = c.getReverseCorpus().getCleanChars();
		
		double fh, bh;
		int m = 0, n = m + winLen;
		while (n <= H.length) {
			fh = entropy(m,n,fChars,fTrie);
			bh = entropy(m,n,bChars,bTrie);
			H[n-1] += fh;
			H[H.length-n] += bh;
			
			m = m + 1;
			n = n + 1;
		}
	}
	
	private double entropy(int m, int n, List<String> chars, Trie trie) {
		List<String> subList = chars.subList(m, n);
		return trie.getEntropy(subList);
	}
	
	private double simulateSegmentation(Corpus c, double[] scores, double threshold) {
		boolean[] cuts = new boolean[scores.length];
		segmentByThreshold(cuts, scores, threshold);
		return MDL.computeDescriptionLength(c, cuts);
	}
	
	private void segmentByThreshold(boolean[] cuts, double[] scores, double threshold) {
		for (int i = 0; i < scores.length; i++) {
			if (scores[i] > threshold)
				cuts[i] = true;
		}
	}
}
