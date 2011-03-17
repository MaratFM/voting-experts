package edu.arizona.ve.algorithm.auto;

import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.trie.Trie;

public interface AutoSegmenter {

	/** 
	 * Run the algorithm for the given window size specified, 
	 * solving for any other parameters automatically.
	 */
	public void runAlgorithm(Corpus corpus, Trie forwardTrie, Trie backwardTrie, int windowSize);
	
	public boolean[] getCutPoints(); 
	
	/** 
	 * One of the optimization methods relies on a scoring function
	 * to bias optimization. This score should increase with the likelihood that
	 * a boundary is present at a given location.
	 */
	public double[] getScores(); 
	
	public String getName();
	
}
