package edu.arizona.ve.algorithm;

import java.util.List;

import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.corpus.Corpus.CorpusType;
import edu.arizona.ve.corpus.CorpusPrinter;
import edu.arizona.ve.evaluation.Evaluator;
import edu.arizona.ve.trie.Trie;
import edu.arizona.ve.util.NF;
import edu.arizona.ve.util.Utils;

/**
*
* @author  Daniel Hewlett
* The Phoneme to Morpheme algorithm of Tanaka-Ishii and Jin (2006).
*/
public class PhonemeToMorpheme {
	Trie _forwardTrie, _backwardTrie;
	Corpus _corpus;
	private boolean[] _cutPoints;
	int _maxLen;
	
	double[] _scores = null;
	
	public PhonemeToMorpheme(Trie forwardTrie, Trie backwardTrie, Corpus corpus, int maxLen) {
		_forwardTrie = forwardTrie;
		_backwardTrie = backwardTrie;
		_corpus = corpus;
		setCutPoints(new boolean[_corpus.getCutPoints().length]);
		_maxLen = maxLen;
	}
	
	public boolean[] runAlgorithm(double threshold) {
		_scores = new double[_corpus.getCutPoints().length];
		
		boolean[] forward = runForward(threshold);
		boolean[] backward = runBackward(threshold);
		boolean[] combined = Utils.combineUnion(forward, backward);
		setCutPoints(combined);
		return getCutPoints();
	}
	
	private double hf(int m, int n) {
		List<String> subList = _corpus.getCleanChars().subList(m, n+1);
		return _forwardTrie.getEntropy(subList);
	}
	
	private double hb(int m, int n, List<String> revChars) {
		List<String> subList = revChars.subList(m, n+1);
		return _backwardTrie.getEntropy(subList);
	}
	
	private boolean[] runForward(double threshold) {
		boolean[] cuts = new boolean[getCutPoints().length];
		
		int m = 0, n = 1;
		
		while (n < cuts.length) {
			double diff = hf(m,n) - hf(m,n-1);
			if (diff > threshold) {
				cuts[n] = true;
			}
//			_scores[n] = Math.max(_scores[n], diff);
			_scores[n] += diff;
			
			if (n > m + _maxLen) {
				m = m + 1;
				n = m + 1;
			} else {
				n = n + 1;
			}
		}
		
		return cuts;
	}
	
	// To use Utils.combine, this should return a reversed set of cutPoints
	private boolean[] runBackward(double threshold) {
		boolean[] cuts = new boolean[getCutPoints().length];
		
		List<String> revChars = _corpus.getReverseCorpus().getCleanChars();
		
		int m = 0, n = 1;
		
		while (n < cuts.length) {
			double diff = hb(m,n,revChars) - hb(m,n-1,revChars);
			if (diff > threshold) {
				cuts[n] = true;
			}
			
			int index = cuts.length-n+1;
			if (index < cuts.length)
				_scores[index] += _scores[index];
//				_scores[index] = Math.max(_scores[index], diff);
			
			if (n > m + _maxLen) {
				m = m + 1;
				n = m + 1;
			} else {
				n = n + 1;
			}
		}
				
		return cuts; 
	}
	
	public void setCutPoints(boolean[] _cutPoints) {
		this._cutPoints = _cutPoints;
	}

	public boolean[] getCutPoints() {
		return _cutPoints;
	}
	
	public double[] getScores() {
		return _scores;
	}
	
	
	
	public static void main(String[] args) {
//		Corpus c = Corpus.autoLoad("latin-morph", "case");
//		Corpus c = Corpus.autoLoad("latin", "word");
//		Corpus c = Corpus.autoLoad("caesar", "nocase");
//		Corpus c = Corpus.autoLoad("orwell-short", CorpusType.LETTER, false);
		Corpus c = Corpus.autoLoad("br87", CorpusType.LETTER, true);
		int maxLen = 7;
		Trie f = Trie.buildTrie(c, maxLen+1);
		Trie b = Trie.buildTrie(c.getReverseCorpus(), maxLen+1);
		
//		Corpus morph = Corpus.autoLoad("inuktitut-morph", "case");
		
		for (double threshold = 0.0; threshold < 1.0; threshold += 0.05) {
			PhonemeToMorpheme ptm = new PhonemeToMorpheme(f, b, c, maxLen);
			ptm.runAlgorithm(threshold);
			
			System.out.println("THRESHOLD: " + NF.format(threshold));
			System.out.println(CorpusPrinter.printScores(c, ptm.getScores(), 100));
			System.out.println(CorpusPrinter.printCuts(c, ptm.getCutPoints(), 100));
			
			Evaluator.evaluate(ptm.getCutPoints(), c.getCutPoints()).printResults();
			//e.printResults();
		}
		
//		CorpusWriter.writeCorpus(c, ptm._cutPoints);
	}
}
