package edu.arizona.ve.algorithm.auto;

import edu.arizona.ve.algorithm.VotingExperts;
import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.mdl.MDL;
import edu.arizona.ve.trie.Trie;
import edu.arizona.ve.util.Utils;

public class AutoBVE implements AutoSegmenter {

	boolean[] _cutPoints = null;
	double[] _scores = null;
	
	@Override
	public void runAlgorithm(Corpus corpus, Trie forwardTrie, Trie backwardTrie, int windowSize) {
		int startThreshold = windowSize * 2;
		double mdl = Double.POSITIVE_INFINITY;
		
		boolean[] maxOn = null, maxOff = null;
		Trie knowledgeTrieOn = null, knowledgeTrieOff = null;
		
		int[] mdlVotes = null;
		
		for (int threshold = startThreshold; threshold >= 0; threshold-- ) {
			
			if (threshold == startThreshold) {
				// Initial k-trie is empty
				knowledgeTrieOn = corpus.makeForwardTrie(windowSize);
				knowledgeTrieOff = corpus.makeForwardTrie(windowSize);
			} else {
				 knowledgeTrieOn = corpus.makeKnowledgeTrie(windowSize, maxOn);
				 knowledgeTrieOff = corpus.makeKnowledgeTrie(windowSize, maxOff);
			}
			
			VotingExperts bveOn = VotingExperts.makeBVE(corpus, forwardTrie, knowledgeTrieOn, windowSize, threshold);
			VotingExperts bveOff = VotingExperts.makeBVE(corpus, forwardTrie, knowledgeTrieOff, windowSize, threshold);
			
			// First set of scores
			int[] votesOn = bveOn.vote();
			int[] votesOff = bveOff.vote();
			
			maxOn = bveOn.makeCutPoints(true);
			double maxOnDL = MDL.computeDescriptionLength(corpus, maxOn);
			if (maxOnDL < mdl) {
				_cutPoints = maxOn;
				_scores = bveOn.getScores();
				mdlVotes = votesOn;
				mdl = maxOnDL;
			}
			
			maxOff = bveOff.makeCutPoints(false);
			double maxOffDL = MDL.computeDescriptionLength(corpus, maxOff);
			if (maxOffDL < mdl) {
				_cutPoints = maxOff;
				_scores = bveOff.getScores();
				mdlVotes = votesOff;
				mdl = maxOffDL;
			}
		}
		
//		_scores = Utils.toDoubleArray(mdlVotes);
	}

	@Override
	public boolean[] getCutPoints() {
		return _cutPoints;
	}

	@Override
	public double[] getScores() {
		return _scores;
	}

	@Override
	public String getName() {
		return "BVE";
	}
}
