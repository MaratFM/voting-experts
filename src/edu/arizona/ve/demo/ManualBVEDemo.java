package edu.arizona.ve.demo;

import edu.arizona.ve.api.Engine;
import edu.arizona.ve.api.Segmentation;
import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.corpus.Corpus.CorpusType;
import edu.arizona.ve.evaluation.EvaluationResults;
import edu.arizona.ve.evaluation.Evaluator;

public class ManualBVEDemo {

	/**
	 * A simple example of how to use BVE with manually-specified parameters.
	 * @param args
	 */
	public static void main(String[] args) {
//		Engine.DEBUG = true;
//		Engine.EVALUATE = true;
		
		// Load the corpus 
		Corpus corpus = Corpus.autoLoad("bloom73", CorpusType.LETTER, true);
		
		// Create a segmentation engine, with a trie depth of 6
		Engine engine = new Engine(corpus, 7);
		
		// Segment using BVE with manually-specified parameters
		Segmentation segmentation = engine.voteBVE(6, 3, true, false);
		
		// Evaluate (compute precision, recall, F-measure, etc.) 
		EvaluationResults results = Evaluator.evaluate(segmentation, corpus);
		
		// Print the important evaluation metrics
		results.printResults();
	}

}
