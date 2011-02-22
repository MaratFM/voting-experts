package edu.arizona.ve.api;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.corpus.CorpusWriter;
import edu.arizona.ve.evaluation.EvaluationResults;
import edu.arizona.ve.evaluation.Evaluator;
import edu.arizona.ve.util.NF;
import edu.arizona.ve.util.Stats;

/**
* @author  Daniel Hewlett
*/
public class AutoEngine {

	// For convenience, hard-coded maxWindow = 9 (larger windows can result in much longer running time)
	public static Segmentation autoVE(Corpus c) {
		return autoVE(c, 9);
	}
	
	public static Segmentation autoVE(Corpus c, int maxWindow) {
		Vector<Segmentation> segmentations = new Vector<Segmentation>();
		
		for (int window = 2; window <= maxWindow; window++) {
			Engine engine = new Engine(c, window+1);
			segmentations.addAll(engine.voteAllThresholds(window, 0, window));
		}
		
		Collections.sort(segmentations);
		Segmentation bestSegmentation = segmentations.get(0);

		Vector<Double> scores = new Vector<Double>();
		for (Segmentation seg : segmentations) {
			EvaluationResults results = Evaluator.evaluate(seg.cutPoints, c.getCutPoints());
			scores.add(results.boundaryF1());
		}
		
		double maxBF = Stats.max(scores);
		double minBF = Stats.min(scores);
		double meanBF = Stats.mean(scores);
		double stdDev = Stats.stDev(scores);
		
		EvaluationResults results = Evaluator.evaluate(bestSegmentation, c);
		double mdlBF = results.boundaryF1();
		double percentOfBest = mdlBF / maxBF;
		
//		System.out.println(bestSegmentation);
		
		System.out.println("MDL REPORT: ");
		System.out.println(c.getName() + 
				"   Mean: " + NF.format(meanBF) + 
				"   StDev: " + NF.format(stdDev) + 
				"   Min: " + NF.format(minBF) + 
				"   Max: " + NF.format(maxBF) + 
				"   MDL: " + NF.format(mdlBF) + 
				"   % of Best: " + NF.format(percentOfBest));
		
		System.out.println("DL: " + NF.format(bestSegmentation.descriptionLength));
		System.out.println("& " + NF.format(minBF) + " & " + NF.format(maxBF) + " & " + NF.format(meanBF) + 
				   		" & " + NF.format(stdDev) + " & " + NF.format(mdlBF) + " & " + NF.format(percentOfBest) + " \\\\ \\hline");
		
		
		return bestSegmentation;
	}
	
	// For convenience, hard-coded maxWindow = 8 (larger windows can result in much longer running time)
	public static Segmentation autoBVE(Corpus c) {
		int maxWindow = 8; // save some time
		return autoBVE(c, maxWindow);
	}
	
	public static Segmentation autoBVE(Corpus c, int maxWindow) {
		double minDL = Double.MAX_VALUE;
		Segmentation bestSegmentation = null;
		Vector<Segmentation> segmentations = new Vector<Segmentation>();
		Vector<Double> scores = new Vector<Double>();
		
		int minWindow = 2;
		
		Engine engine = new Engine(c, maxWindow + 1);
		
		// Local max on
		for (int window = minWindow; window <= maxWindow; window++) {
			segmentations.addAll(engine.voteBVEMDL(window, true, false));
		}
		
		// Local max off
		for (int window = minWindow; window <= maxWindow; window++) {
			segmentations.addAll(engine.voteBVEMDL(window, false, false));
		}
		
		// TODO: The above code can be made more efficient, or at least parallelized
		
		for (Segmentation s : segmentations) {
			EvaluationResults results = Evaluator.evaluate(s, c);
			scores.add(results.boundaryF1());
			
			if (s.descriptionLength < minDL) {
				minDL = s.descriptionLength;
				bestSegmentation = s;
			}
		}
		
		double maxBF = Stats.max(scores);
		double minBF = Stats.min(scores);
		double meanBF = Stats.mean(scores);
		double stdDev = Stats.stDev(scores);
		
		EvaluationResults results = Evaluator.evaluate(bestSegmentation, c);
		double mdlBF = results.boundaryF1();
		double percentOfBest = mdlBF / maxBF;
		
//		System.out.println(bestSegmentation);
		
		System.out.println("MDL REPORT: ");
		System.out.println(c.getName() + 
				"   Mean: " + NF.format(meanBF) + 
				"   StDev: " + NF.format(stdDev) + 
				"   Min: " + NF.format(minBF) + 
				"   Max: " + NF.format(maxBF) + 
				"   MDL: " + NF.format(mdlBF) + 
				"   % of Best: " + NF.format(percentOfBest));
		
		System.out.println("DL: " + NF.format(bestSegmentation.descriptionLength));
		System.out.println("& " + NF.format(minBF) + " & " + NF.format(maxBF) + " & " + NF.format(meanBF) + 
				   		" & " + NF.format(stdDev) + " & " + NF.format(mdlBF) + " & " + NF.format(percentOfBest) + " \\\\ \\hline");
		
		return bestSegmentation;
	}
	
	// For convenience, hard-coded maxWindow = 8 (larger windows can result in much longer running time)
	public static Segmentation autoTransfer(Corpus train, Corpus test) {
		int maxWindow = 8; // save some time
		return autoTransfer(train, test, maxWindow);
	}
	
	public static Segmentation autoTransfer(Corpus train, Corpus test, int maxWindow) {
		double minDL = Double.MAX_VALUE;
		Segmentation bestSegmentation = null;
		Vector<Segmentation> segmentations = new Vector<Segmentation>();
		Vector<Double> scores = new Vector<Double>();
		
		int minWindow = 2;
		
		Engine engine = new Engine(train, maxWindow + 1);
		
		// TODO: Pull out most of this code into a helper function for code reuse
		
		// Local max on
		for (int window = minWindow; window <= maxWindow; window++) {
			segmentations.addAll(engine.voteBVEMDL(window, true, false));
		}
		
		// Local max off
		for (int window = minWindow; window <= maxWindow; window++) {
			segmentations.addAll(engine.voteBVEMDL(window, false, false));
		}
		
		for (Segmentation s : segmentations) {
			EvaluationResults results = Evaluator.evaluate(s, train);
			scores.add(results.boundaryF1());
			
			if (s.descriptionLength < minDL) {
				minDL = s.descriptionLength;
				bestSegmentation = s;
			}
		}
		
		System.out.println("TRAIN (" + bestSegmentation + "):");
		EvaluationResults results = Evaluator.evaluate(bestSegmentation, train);
		results.printResults();
		
		// We need to get back the right knowledge trie
		engine.voteBVE(bestSegmentation.windowSize, bestSegmentation.threshold, bestSegmentation.localMax, false);
		
//		Engine.DEBUG = true;
		
		// Segment the second corpus with the knowledge trie and parameters from the old corpus
		Segmentation transfer = engine.voteTransfer(test, bestSegmentation.windowSize,
														  bestSegmentation.threshold, 
														  bestSegmentation.localMax);

		EvaluationResults transferResults = Evaluator.evaluate(transfer.cutPoints, test.getCutPoints());
		System.out.println("TRANSFER:");
		transferResults.printResults();
		
		CorpusWriter.writeCorpus("transfer-test.txt", test, transfer);
		
		return bestSegmentation;
	}
}