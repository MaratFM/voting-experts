package edu.arizona.ve.algorithm;

import java.util.List;

import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.corpus.Corpus.CorpusType;
import edu.arizona.ve.evaluation.Evaluator;
import edu.arizona.ve.trie.Trie;
import edu.arizona.ve.util.NF;
import edu.arizona.ve.util.Utils;

/**
*
* @author  Anh Tran
* Unsupervised Word Segmentation algorithm with Branching Entropy & MDL
* by Zhikov, Takamua, and Okumura (2010).
*/
public class TwoPartBranchingEntropy {

	Trie _forwardTrie, _backwardTrie;
	Corpus _corpus;
	private boolean[] _cutPoints;
	int _maxLen;
	

	public TwoPartBranchingEntropy(Corpus corpus, int maxLen) {
		// Init class variables
		_corpus = corpus;
		_cutPoints = new boolean[_corpus.getCutPoints().length];
		_maxLen = maxLen;
		
		// Build tries
		_forwardTrie = Trie.buildTrie(corpus, maxLen+1);
		_backwardTrie = Trie.buildTrie(corpus.getReverseCorpus(), maxLen+1);	
	}
	
	public void runAlgorithm() {
	}
	
	
	
	
	public Double[] calcBranchingEntropy(Corpus c, Trie forward, Trie backward) {
		Double[] H = new Double[getCutPoints().length];
		for (int i = _maxLen; i <= _maxLen; i++) {
			accumulateEntropies(H, i, forward, backward);
		}
		
		return H;
	}
	
	private double entropy(int m, int n, List<String> chars, Trie trie) {
		List<String> subList = chars.subList(m, n+1);
		return trie.getEntropy(subList);
	}
	
	private void accumulateEntropies(Double[] H, int winLen, Trie fTrie, Trie bTrie) {
		List<String> fChars = _corpus.getCleanChars();
		List<String> bChars = _corpus.getReverseCorpus().getCleanChars();
		
		double fh, bh;
		int m = 0, n = m + winLen;
		while (n < H.length) {
			fh = entropy(m,n,fChars,fTrie) - entropy(m,n-1,fChars,fTrie);
			bh = entropy(m,n,bChars,bTrie) - entropy(m,n-1,bChars,bTrie);
			H[n] += fh;
			H[H.length-(n+1)] += bh;
			
			m = m + 1;
			n = n + 1;
		}
	}
	
	
	

	public boolean[] getCutPoints() {
		return _cutPoints;
	}
	
	
	
	public static void main(String[] args) {
//		Corpus c = Corpus.autoLoad("latin-morph", "case");
//		Corpus c = Corpus.autoLoad("latin", "word");
//		Corpus c = Corpus.autoLoad("caesar", "nocase");
//		Corpus c = Corpus.autoLoad("orwell-short", CorpusType.LETTER, false);
		Corpus c = Corpus.autoLoad("br87", CorpusType.LETTER, true);
		int maxLen = 7;
		
		TwoPartBranchingEntropy tpbe = new TwoPartBranchingEntropy(c, maxLen);
		Evaluator.evaluate(tpbe.getCutPoints(), c.getCutPoints()).printResults();
	}

}
