package edu.arizona.ve.algorithm.auto;

import edu.arizona.ve.algorithm.PhonemeToMorpheme;
import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.mdl.MDL;
import edu.arizona.ve.trie.Trie;

public class AutoPtM implements AutoSegmenter {

	boolean[] _lastCutPoints = null;
	double[] _lastScores = null;
	
	@Override
	public void runAlgorithm(Corpus corpus, Trie forwardTrie, Trie backwardTrie, int windowSize) {
		double minDL = Double.POSITIVE_INFINITY;
		
		for (double threshold = 0.0; threshold < 2.0; threshold += 0.1) {
			PhonemeToMorpheme ptm = new PhonemeToMorpheme(forwardTrie, backwardTrie, corpus, windowSize);
			boolean[] cutPoints = ptm.runAlgorithm(threshold);
			double dl = MDL.computeDescriptionLength(corpus, cutPoints);
			
			if (dl < minDL) {
				minDL = dl;
				_lastCutPoints = cutPoints;
				_lastScores = ptm.getScores();
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
		return "PtM";
	}
}
