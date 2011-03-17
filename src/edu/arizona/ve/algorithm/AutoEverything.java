package edu.arizona.ve.algorithm;

import java.util.Collections;
import java.util.List;

import com.google.common.collect.Lists;

import edu.arizona.ve.algorithm.auto.AutoBVE;
import edu.arizona.ve.algorithm.auto.AutoPtM;
import edu.arizona.ve.algorithm.auto.AutoSegmenter;
import edu.arizona.ve.algorithm.auto.AutoVE;
import edu.arizona.ve.algorithm.auto.EntropyMDL;
import edu.arizona.ve.algorithm.optimize.GlobalOptimizer;
import edu.arizona.ve.algorithm.optimize.LocalOptimizer;
import edu.arizona.ve.api.Segmentation;
import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.corpus.Corpus.CorpusType;
import edu.arizona.ve.evaluation.Evaluator;
import edu.arizona.ve.mdl.MDL;
import edu.arizona.ve.trie.Trie;

public class AutoEverything {

	public static Segmentation autoSegment(AutoSegmenter algorithm, Corpus c, Trie forwardTrie, Trie backwardTrie) {
		int minWindow = 3;
		int maxWindow = 7;
		
		List<Segmentation> segmentations = Lists.newArrayList();
		for (int w = minWindow; w <= maxWindow; w++) {
			algorithm.runAlgorithm(c, forwardTrie, backwardTrie, w);
			boolean[] cutPoints = algorithm.getCutPoints();
			
			Segmentation s = new Segmentation();
			s.cutPoints = cutPoints;
			s.descriptionLength = MDL.computeDescriptionLength(c, cutPoints);
			s.windowSize = w; // why not
			segmentations.add(s);
			
//			System.out.println("\n*****************************\nWINDOW SIZE: " + w);
//			System.out.println(s.descriptionLength);
//			Evaluator.evaluate(cutPoints, c.getCutPoints()).printResults();
		}
		
		// Sort into ascending order by DL
		Collections.sort(segmentations);
		Segmentation mdl = segmentations.get(0);
		
		System.out.println("\n\nResults for " + algorithm.getName() + ":");
		Evaluator.evaluate(mdl.cutPoints, c.getCutPoints()).printResults();
		
		boolean[] local = optimizeLocally(algorithm, mdl.cutPoints, c);
		System.out.println("LOCAL ONLY");
		Evaluator.evaluate(local, c.getCutPoints()).printResults();

		boolean[] globalOnly = GlobalOptimizer.optimize(c, mdl.cutPoints);
		System.out.println("GLOBAL ONLY");
		Evaluator.evaluate(globalOnly, c.getCutPoints()).printResults();

		boolean[] localToGlobal = GlobalOptimizer.optimize(c, local);
		System.out.println("LOCAL then GLOBAL");
		Evaluator.evaluate(localToGlobal, c.getCutPoints()).printResults();
		
		return segmentations.get(0);
	}
	
	public static boolean[] optimizeLocally(AutoSegmenter algorithm, boolean[] cutPoints, Corpus c) {
		boolean[] optimized = LocalOptimizer.optimize(c, cutPoints, algorithm.getScores());
		return optimized;
	}
	
	public static boolean[] optimizeGlobally(boolean[] cutPoints, Corpus c) {
		boolean[] optimized = GlobalOptimizer.optimize(c, cutPoints);
		
		System.out.println("GLOBAL OPTIMIZATION");
		Evaluator.evaluate(optimized, c.getCutPoints()).printResults();
		
		return optimized;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
//		Corpus c = Corpus.autoLoad("gray", CorpusType.LETTER, false);
		Corpus c = Corpus.autoLoad("br87", CorpusType.LETTER, true);
//		Corpus c = Corpus.autoLoad("thai-novel-short", CorpusType.LETTER, true);
		
		Trie forwardTrie = c.makeForwardTrie(8);
		Trie backwardTrie = c.makeBackwardTrie(8);

		AutoSegmenter ve = new AutoVE();
		AutoSegmenter bve = new AutoBVE();
		AutoSegmenter ptm = new AutoPtM();
		AutoSegmenter emdl = new EntropyMDL();
		
		autoSegment(emdl, c, forwardTrie, backwardTrie);
		autoSegment(ve, c, forwardTrie, backwardTrie);
		autoSegment(bve, c, forwardTrie, backwardTrie);
		autoSegment(ptm, c, forwardTrie, backwardTrie);
	}
}
