package edu.arizona.ve.algorithm.auto;

import edu.arizona.ve.algorithm.VotingExperts;
import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.corpus.CorpusPrinter;
import edu.arizona.ve.mdl.MDL;
import edu.arizona.ve.trie.Trie;

public class AutoVE implements AutoSegmenter {

	boolean[] _lastCutPoints = null;
	double[] _lastScores = null;
	
	@Override
	public void runAlgorithm(Corpus corpus, Trie trie, Trie backwardTrie, int windowSize) {
		// Note: backwardTrie is not used
		
		int minThreshold = 0;
		int maxThreshold = windowSize * 2;
		
		VotingExperts ve = VotingExperts.makeForwardVE(corpus, trie, windowSize, minThreshold);
		
		// We use the VE votes as scores (for now?)
//		int[] voteArray = 
		ve.vote();
//		_lastScores = new double[voteArray.length];
//		for (int i = 0; i < voteArray.length; i++) {
//			_lastScores[i] = voteArray[i];
//		}
		
		double mdl = Double.POSITIVE_INFINITY;
		
		for (int t = minThreshold; t < maxThreshold; t++) {
			ve.setThreshold(t);
			
			boolean[] maxOn = ve.makeCutPoints(true);
			double maxOnDL = MDL.computeDescriptionLength(corpus, maxOn);
			if (maxOnDL < mdl) {
				_lastCutPoints = maxOn;
				_lastScores = ve.getScores();
				mdl = maxOnDL;
			}
			
			boolean[] maxOff = ve.makeCutPoints(false);
			double maxOffDL = MDL.computeDescriptionLength(corpus, maxOff);
			if (maxOffDL < mdl) {
				_lastCutPoints = maxOff;
				_lastScores = ve.getScores();
				mdl = maxOffDL;
			}
		}
		
//		System.out.println(CorpusPrinter.printScores(corpus, _lastScores, 100));
	}

	@Override
	public boolean[] getCutPoints() {
		return _lastCutPoints;
	}

	@Override
	public double[] getScores() {
		return _lastScores;
	}

	@Override
	public String getName() {
		return "VE";
	}

}
