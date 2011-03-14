package edu.arizona.ve.analysis;

import java.util.Arrays;

import edu.arizona.ve.algorithm.EntropyMDL;
import edu.arizona.ve.api.AutoEngine;
import edu.arizona.ve.api.Engine;
import edu.arizona.ve.api.Segmentation;
import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.corpus.Corpus.CorpusType;
import edu.arizona.ve.evaluation.EvaluationResults;
import edu.arizona.ve.evaluation.Evaluator;
import edu.arizona.ve.mdl.MDL;

public class AutoOptimization {

	public static void main(String[] args) {
		Engine.EVALUATE = true;
		
//		Corpus corpus = Corpus.autoLoad("thai-articles", CorpusType.LETTER, true);
		Corpus corpus = Corpus.autoLoad("br87", CorpusType.LETTER, true);

//		Segmentation segmentation = new Segmentation();
//		segmentation.cutPoints = corpus.getCutPoints();
//		segmentation.votes = new int[segmentation.cutPoints.length];
//		segmentation.descriptionLength = MDL.computeDescriptionLength(corpus, corpus.getCutPoints());
		
		Segmentation segmentation = AutoEngine.autoBVE(corpus, 5);
//		Segmentation segmentation = AutoEngine.autoVE(corpus);
		
		boolean[] cutPoints1 = Arrays.copyOf(segmentation.cutPoints, segmentation.cutPoints.length);
		
		EvaluationResults results = Evaluator.evaluate(cutPoints1, corpus.getCutPoints());
		System.out.println("\n***********\nPHASE 1:");
		results.printResults();
		System.out.println(segmentation.descriptionLength);
		System.out.println("***********\n");
		
		double[] scores = new double[segmentation.votes.length];
		for (int i = 0; i < segmentation.votes.length; i++) {
			scores[i] = segmentation.votes[i];
		}
		
		EntropyMDL optimizer = new EntropyMDL(corpus, segmentation.windowSize);
		boolean[] cutPoints2 = Arrays.copyOf(cutPoints1, cutPoints1.length); 
		optimizer.algorithm2(corpus, cutPoints2, scores);
//		boolean[] cutPoints2 = segmentation.cutPoints;

		EvaluationResults results2 = Evaluator.evaluate(cutPoints2, corpus.getCutPoints());
		System.out.println("\n***********\nPHASE 2:");
		results2.printResults();
		System.out.println(MDL.computeDescriptionLength(corpus, cutPoints2));
		System.out.println("***********\n");
		
		boolean[] cutPoints3 = Arrays.copyOf(cutPoints2, cutPoints2.length);  
		optimizer.algorithm3(corpus, cutPoints3);

		EvaluationResults results3 = Evaluator.evaluate(cutPoints3, corpus.getCutPoints());
		System.out.println("\n***********\nPHASE 3:");
		results3.printResults();
		System.out.println(MDL.computeDescriptionLength(corpus, cutPoints3));
		System.out.println("***********\n");
		
		assert(!Arrays.equals(cutPoints1, cutPoints2));
		assert(!Arrays.equals(cutPoints2, cutPoints3));
		assert(!Arrays.equals(cutPoints1, cutPoints3));
	}
}
