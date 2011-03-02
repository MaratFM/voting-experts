package edu.arizona.ve.analysis;

import edu.arizona.ve.algorithm.PhonemeToMorpheme;
import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.corpus.Corpus.CorpusType;
import edu.arizona.ve.evaluation.Evaluator;
import edu.arizona.ve.mdl.MDL;
import edu.arizona.ve.trie.Trie;
import edu.arizona.ve.util.NF;

public class AutoPtM {

	public static void main(String[] args) {
//		Corpus c = Corpus.autoLoad("latin-morph", "case");
//		Corpus c = Corpus.autoLoad("latin", "word");
//		Corpus c = Corpus.autoLoad("caesar", "nocase");
//		Corpus c = Corpus.autoLoad("orwell-short", CorpusType.LETTER, false);
//		Corpus c = Corpus.autoLoad("br87", CorpusType.LETTER, true);
		Corpus c = Corpus.autoLoad("thai-novel-short", CorpusType.LETTER, true);
//		Corpus c = Corpus.autoLoad("switchboard", CorpusType.LETTER, false);
		
		int maxLen = 8;
		Trie f = Trie.buildTrie(c, maxLen+1);
		Trie b = Trie.buildTrie(c.getReverseCorpus(), maxLen+1);
		
		double minDL = Double.POSITIVE_INFINITY;
		boolean[] mdlSegmentation = null;
		
		for (double threshold = 0.0; threshold < 2.0; threshold += 0.05) {
			PhonemeToMorpheme ptm = new PhonemeToMorpheme(f, b, c, maxLen);
			ptm.runAlgorithm(threshold);
			
			boolean[] cutPoints = ptm.getCutPoints();
			double dl = MDL.computeDescriptionLength(c, cutPoints);
			
			if (dl < minDL) {
				minDL = dl;
				mdlSegmentation = cutPoints;
			}
			
			System.out.println("THRESHOLD: " + NF.format(threshold) + " DL: " + NF.format(dl));
			Evaluator.evaluate(ptm.getCutPoints(), c.getCutPoints()).printResults();
			System.out.println();
		}
		
		System.out.println("\n\n********************************************");
		System.out.println("DL: " + NF.format(minDL));
		Evaluator.evaluate(mdlSegmentation, c.getCutPoints()).printResults();
		
//		CorpusWriter.writeCorpus(c, ptm._cutPoints);
	}

}
