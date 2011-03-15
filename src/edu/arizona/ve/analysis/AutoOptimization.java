package edu.arizona.ve.analysis;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.arizona.ve.algorithm.EntropyMDL;
import edu.arizona.ve.api.AutoEngine;
import edu.arizona.ve.api.Engine;
import edu.arizona.ve.api.Segmentation;
import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.corpus.Corpus.CorpusType;
import edu.arizona.ve.evaluation.EvaluationResults;
import edu.arizona.ve.evaluation.Evaluator;
import edu.arizona.ve.mdl.MDL;
import edu.arizona.ve.util.NF;

public class AutoOptimization {

	public static void main(String[] args) {
		Engine.EVALUATE = true;
		
		List<Corpus> corpora = new ArrayList<Corpus>();
		
		corpora.add(Corpus.autoLoad("br87", CorpusType.LETTER, true));
//		corpora.add(Corpus.autoLoad("bloom73", CorpusType.LETTER, true));
//		corpora.add(Corpus.autoLoad("chinese-gw", CorpusType.LETTER, true));
//		corpora.add(Corpus.autoLoad("thai-novel-short", CorpusType.LETTER, true));
//		
//		corpora.add(Corpus.autoLoad("caesar", CorpusType.LETTER, false));
//		corpora.add(Corpus.autoLoad("gray", CorpusType.LETTER, false));
//		corpora.add(Corpus.autoLoad("switchboard", CorpusType.LETTER, false));
//		corpora.add(Corpus.autoLoad("orwell-short", CorpusType.LETTER, false));
//		corpora.add(Corpus.autoLoad("twain", CorpusType.LETTER, false));
//		corpora.add(Corpus.autoLoad("zarathustra", CorpusType.LETTER, false));
		
		for (Corpus c : corpora) {
			System.out.println("\nCORPUS: " + c.getName());
			runExperiment(c);
		}
		
	}
	
	
	public static void runExperiment(Corpus corpus) {

		Segmentation segmentation = AutoEngine.autoBVE(corpus);
//		Segmentation segmentation = AutoEngine.autoVE(corpus);
		
		boolean[] cutPointsBVE = Arrays.copyOf(segmentation.cutPoints, segmentation.cutPoints.length);
		double[] scores = new double[segmentation.votes.length];
		for (int i = 0; i < segmentation.votes.length; i++) {
			scores[i] = segmentation.votes[i];
		}
		assert(cutPointsBVE.length == scores.length);

		EntropyMDL optimizer = new EntropyMDL(corpus, segmentation.windowSize);
		boolean[] cutPointsBVE2 = optimizer.algorithm2(corpus, cutPointsBVE, scores);
		boolean[] cutPointsBVE3 = optimizer.algorithm3(corpus, cutPointsBVE);
		boolean[] cutPointsBVE23 = optimizer.algorithm3(corpus, cutPointsBVE2);

		assert(!Arrays.equals(cutPointsBVE, cutPointsBVE2));
		assert(!Arrays.equals(cutPointsBVE, cutPointsBVE3));
		assert(!Arrays.equals(cutPointsBVE2, cutPointsBVE23));
		
		EvaluationResults resultsBVE = Evaluator.evaluate(cutPointsBVE, corpus.getCutPoints());
		System.out.println("BVE: " + resultsBVE.boundaryF1());
		EvaluationResults resultsBVE2 = Evaluator.evaluate(cutPointsBVE2, corpus.getCutPoints());
		System.out.println("BVE+Opt2: " + resultsBVE2.boundaryF1());
		EvaluationResults resultsBVE23 = Evaluator.evaluate(cutPointsBVE23, corpus.getCutPoints());
		System.out.println("BVE+Opt2&3: " + resultsBVE23.boundaryF1());
		EvaluationResults resultsBVE3 = Evaluator.evaluate(cutPointsBVE3, corpus.getCutPoints());
		System.out.println("BVE+Opt3: " + resultsBVE3.boundaryF1());
		
		try {
	        String wd = System.getProperty("user.dir");
			String path = wd + "/" + "results/" + Corpus.getFolder(corpus.getType())
				+ "/BVEOptimized-" + corpus.getName() + ".txt";

			BufferedWriter out = new BufferedWriter(new FileWriter(path));

			out.write("***************** WinLen: " + segmentation.windowSize + " *****************\n\n");
			
			out.write("- BVE -\n");
			out.write("MDL: " + MDL.computeDescriptionLength(corpus, cutPointsBVE) + "\n");
			out.write("B-F: " + resultsBVE.boundaryF1() + "\n");
		    out.write(NF.format(resultsBVE.boundaryPrecision) + "\t"
		    		+ NF.format(resultsBVE.boundaryRecall) + "\t"
		    		+ NF.format(resultsBVE.boundaryF1()) + "\t");
		    out.write(NF.format(resultsBVE.chunkPrecision) + "\t"
		    		+ NF.format(resultsBVE.chunkRecall) + "\t"
		    		+ NF.format(resultsBVE.chunkF1()) + "\n");
		    out.write("\n");

			out.write("- BVE + Optimization 2 -\n");
			out.write("MDL: " + MDL.computeDescriptionLength(corpus, cutPointsBVE2) + "\n");
			out.write("B-F: " + resultsBVE2.boundaryF1() + "\n");
		    out.write(NF.format(resultsBVE2.boundaryPrecision) + "\t"
		    		+ NF.format(resultsBVE2.boundaryRecall) + "\t"
		    		+ NF.format(resultsBVE2.boundaryF1()) + "\t");
		    out.write(NF.format(resultsBVE2.chunkPrecision) + "\t"
		    		+ NF.format(resultsBVE2.chunkRecall) + "\t"
		    		+ NF.format(resultsBVE2.chunkF1()) + "\n");
		    out.write("\n");

			out.write("- BVE + Optimization 2 & 3 -\n");
			out.write("MDL: " + MDL.computeDescriptionLength(corpus, cutPointsBVE23) + "\n");
			out.write("B-F: " + resultsBVE23.boundaryF1() + "\n");
		    out.write(NF.format(resultsBVE23.boundaryPrecision) + "\t"
		    		+ NF.format(resultsBVE23.boundaryRecall) + "\t"
		    		+ NF.format(resultsBVE23.boundaryF1()) + "\t");
		    out.write(NF.format(resultsBVE23.chunkPrecision) + "\t"
		    		+ NF.format(resultsBVE23.chunkRecall) + "\t"
		    		+ NF.format(resultsBVE23.chunkF1()) + "\n");
		    out.write("\n");

			out.write("- BVE + Optimization 3 -\n");
			out.write("MDL: " + MDL.computeDescriptionLength(corpus, cutPointsBVE3) + "\n");
			out.write("B-F: " + resultsBVE3.boundaryF1() + "\n");
		    out.write(NF.format(resultsBVE3.boundaryPrecision) + "\t"
		    		+ NF.format(resultsBVE3.boundaryRecall) + "\t"
		    		+ NF.format(resultsBVE3.boundaryF1()) + "\t");
		    out.write(NF.format(resultsBVE3.chunkPrecision) + "\t"
		    		+ NF.format(resultsBVE3.chunkRecall) + "\t"
		    		+ NF.format(resultsBVE3.chunkF1()) + "\n");
		    out.write("\n");
			
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
