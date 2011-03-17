package edu.arizona.ve.algorithm;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.arizona.ve.algorithm.auto.EntropyMDL;
import edu.arizona.ve.algorithm.optimize.GlobalOptimizer;
import edu.arizona.ve.algorithm.optimize.LocalOptimizer;
import edu.arizona.ve.algorithm.optimize.Model;
import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.corpus.Corpus.CorpusType;
import edu.arizona.ve.evaluation.EvaluationResults;
import edu.arizona.ve.evaluation.Evaluator;
import edu.arizona.ve.mdl.MDL;
import edu.arizona.ve.trie.Trie;
import edu.arizona.ve.util.NF;
import edu.arizona.ve.util.Stats;

/**
 * 
 * @author Anh Tran
 * 
 * Implementation of the unsupervised word segmentation algorithm (Ent-MDL)
 * by Zhikov, Takamura, and Okumura (2010).
 * 
 */
public class Zhikov {

	Trie _forwardTrie, _backwardTrie;
	Corpus _corpus;
	int _maxLen;
	boolean[] _cutPoints1, _cutPoints2, _cutPoints3;
	double[] _entropy;
	double _mdl;
	Model _model;
	

	public Zhikov(Corpus corpus, int maxLen) {
		// Init class variables
		_corpus = corpus;
		_maxLen = maxLen;
		
		// Build tries
		_forwardTrie = corpus.makeForwardTrie(maxLen+1);
		_backwardTrie = corpus.makeBackwardTrie(maxLen+1);	
	}
	
	public boolean[] runAlgorithm() {

		EntropyMDL emdl = new EntropyMDL();
		emdl.runAlgorithm(_corpus, _forwardTrie, _backwardTrie, _maxLen);
		
		_cutPoints1 = emdl.getCutPoints();
		_entropy = emdl.getScores();

		System.out.println("\nAlgorithm 1:");
		Evaluator.evaluate(getCutPoints1(), _corpus.getCutPoints()).printResults();
		System.out.println("MDL: " + getMDL1());

		// Compress local token co-occurences
		_cutPoints2 = Arrays.copyOf(_cutPoints1, _cutPoints1.length);
		_cutPoints2 = LocalOptimizer.optimize(_corpus, _cutPoints2, _entropy);

		System.out.println("\n\nAlgorithm 2:");
		Evaluator.evaluate(getCutPoints2(), _corpus.getCutPoints()).printResults();
		System.out.println("MDL: " + getMDL2());
		
		// Lexicon clean-up
		_cutPoints3 = Arrays.copyOf(_cutPoints2, _cutPoints2.length);
		_cutPoints3 = GlobalOptimizer.optimize(_corpus, _cutPoints3);

		System.out.println("\n\nAlgorithm 3:");
		Evaluator.evaluate(getCutPoints3(), _corpus.getCutPoints()).printResults();
		System.out.println("MDL: " + getMDL3());
		
		return _cutPoints3;
	}
	
	public boolean[] getCutPoints1() {
		return _cutPoints1;
	}

	public boolean[] getCutPoints2() {
		return _cutPoints2;
	}
	
	public boolean[] getCutPoints3() {
		return _cutPoints3;
	}
	
	public double getMDL1() {
		return MDL.computeDescriptionLength(_corpus, _cutPoints1);
	}
	
	public double getMDL2(){
		return MDL.computeDescriptionLength(_corpus, _cutPoints2);
	}
	
	public double getMDL3() {
		return MDL.computeDescriptionLength(_corpus, _cutPoints3);
	}
	
	public static void runExperiment(Corpus c) {
		Zhikov tpbe;
		double bestMDL = Double.MAX_VALUE, bestF = Double.MIN_VALUE;
		int bestMDLWin = -1;
		EvaluationResults mdlResults = null;

		List<Double> scores = new ArrayList<Double>();
		
        String wd = System.getProperty("user.dir");
		String path = wd + "/" + "results/" + Corpus.getFolder(c.getType())
			+ "/EntMDL-" + c.getName() + ".txt";
		
		try {
			EvaluationResults eval1, eval2, eval3;
			
			PrintStream out = new PrintStream(path);
			
			for (int maxLen = 3; maxLen <= 8; maxLen++) {

				System.out.println("\nWIN LENGTH: " + maxLen);
				
				tpbe = new Zhikov(c, maxLen);
				tpbe.runAlgorithm();
				
				out.print("***************** WinLen: " + maxLen + " *****************\n\n");
				eval1 = Evaluator.evaluate(tpbe.getCutPoints1(), c.getCutPoints());
				out.print("- Algorithm 1 -\n");
				out.print("MDL: " + tpbe.getMDL1() + "\n");
				out.print("B-F: " + eval1.boundaryF1() + "\n");
				eval1.printResults(out);

				eval2 = Evaluator.evaluate(tpbe.getCutPoints2(), c.getCutPoints());
				out.print("- Algorithm 2 -\n");
				out.print("MDL: " + tpbe.getMDL2() + "\n");
				out.print("B-F: " + eval2.boundaryF1() + "\n");
				eval2.printResults(out);

				eval3 = Evaluator.evaluate(tpbe.getCutPoints3(), c.getCutPoints());
				out.println("- Algorithm 3 -");
				out.println("MDL: " + tpbe.getMDL3());
				out.println("B-F: " + eval3.boundaryF1());
			    eval3.printResults(out);
			    out.println("\n");
			    
			    if (tpbe.getMDL3() < bestMDL) {
			    	bestMDL = tpbe.getMDL3();
			    	bestMDLWin = maxLen;
			    	mdlResults = eval3;
			    }

			    scores.add(eval3.boundaryF1());
			}
			
			out.println("****************** SUMMARY ******************\n");
//			out.write("- Best BF -\n");
//			out.write("Win: " + bestFWin + "\n");
//			out.write("B-F: " + bestF + "\n\n");
//			out.write("- Best MDL -\n");
//			out.write("Win: " + bestMDLWin + "\n");
//			out.write("MDL: " + bestMDL + "\n\n");
			
			double maxBF = Stats.max(scores);
			double minBF = Stats.min(scores);
			double meanBF = Stats.mean(scores);
			double stdDev = Stats.stDev(scores);
			
			double mdlBF = mdlResults.boundaryF1();
			double percentOfBest = mdlBF / maxBF;
			
			out.println("MDL REPORT: ");
			out.println(c.getName() + 
					"   Mean: " + NF.format(meanBF) + 
					"   StDev: " + NF.format(stdDev) + 
					"   Min: " + NF.format(minBF) + 
					"   Max: " + NF.format(maxBF) + 
					"   MDL: " + NF.format(mdlBF) + 
					"   % of Best: " + NF.format(percentOfBest));
			
			out.println("DL: " + NF.format(bestMDL));
			out.println("LATEX:");
			out.println("& " + NF.format(minBF) + " & " + NF.format(maxBF) + " & " + NF.format(meanBF) + 
				   		" & " + NF.format(stdDev) + " & " + NF.format(mdlBF) + " & " + NF.format(percentOfBest) + " \\\\ \\hline");
			
			out.println();
			mdlResults.printResults(out);
			
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	

	public static void main(String[] args) {
		List<Corpus> corpora = new ArrayList<Corpus>();
		
		corpora.add(Corpus.autoLoad("br87", CorpusType.LETTER, true));
//		corpora.add(Corpus.autoLoad("bloom73", CorpusType.LETTER, true));
//		corpora.add(Corpus.autoLoad("chinese-gw", CorpusType.LETTER, true));
//		corpora.add(Corpus.autoLoad("thai-novel-short", CorpusType.LETTER, true));
		
//		corpora.add(Corpus.autoLoad("caesar", CorpusType.LETTER, false));
//		corpora.add(Corpus.autoLoad("gray", CorpusType.LETTER, false));
//		corpora.add(Corpus.autoLoad("switchboard", CorpusType.LETTER, false));
//		corpora.add(Corpus.autoLoad("orwell-short", CorpusType.LETTER, false));
//		corpora.add(Corpus.autoLoad("twain", CorpusType.LETTER, false));
//		corpora.add(Corpus.autoLoad("zarathustra", CorpusType.LETTER, false));
		
		for (Corpus c : corpora) {
			System.out.println("\nCORPUS: " + c.getName());
			Zhikov.runExperiment(c);
		}
	}

}
