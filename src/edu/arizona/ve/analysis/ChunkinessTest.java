package edu.arizona.ve.analysis;

import java.util.List;

import edu.arizona.ve.algorithm.VotingExperts;
import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.corpus.Corpus.CorpusType;
import edu.arizona.ve.evaluation.EvaluationResults;
import edu.arizona.ve.evaluation.Evaluator;
import edu.arizona.ve.trie.Trie;
import edu.arizona.ve.util.Utils;

public class ChunkinessTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
//		Corpus corpus = Corpus.autoLoad("orwell-short", CorpusType.LETTER, false);
		Corpus corpus = Corpus.autoLoad("br87", CorpusType.LETTER, true);
		
		int windowSize = 5;
//		int refThreshold = 0; 
		int refThreshold = 2;
		int chunkThreshold = 0; //Math.max(0, refThreshold-3);
		
		Trie forward = corpus.makeForwardTrie(windowSize + 1);
		Trie backward = corpus.makeBackwardTrie(windowSize + 1);
		VotingExperts ve = VotingExperts.makeChunkVE(corpus, forward, backward, windowSize, chunkThreshold);
		
		ve.runAlgorithm(false);
		boolean[] veCuts = ve.getCutPoints();
		
		System.out.println("Chunkiness Expert:");
		EvaluationResults results = Evaluator.evaluate(veCuts, corpus.getCutPoints());
		results.printResults();
		
		// OmniVE for Reference 
		VotingExperts ref = VotingExperts.makeOmniVE(corpus, forward, backward, windowSize, refThreshold);
		ref.runAlgorithm(true);
		boolean[] refCuts = ref.getCutPoints();
		
		System.out.println("\nReference BidiVE:");
		EvaluationResults refResults = Evaluator.evaluate(refCuts, corpus.getCutPoints());
		refResults.printResults();
	}

}
