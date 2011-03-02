package edu.arizona.ve.mdl;

import java.util.HashMap;
import java.util.List;

import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.corpus.Corpus.CorpusType;
import edu.arizona.ve.util.NF;
import edu.arizona.ve.util.Stats;

public class MDL {

	public static double computeDescriptionLength(Corpus corpus, boolean[] cutPoints) {
//		Set<String> alphabet = trie.getChildren(new ArrayList<String>()).keySet();
		
//		System.out.println("ALPHABET SIZE: " + alphabet.size());
		
//		Vector<Double> charFreq = new Vector<Double>();
//		for (String s : alphabet) {
//			List<String> list = new Vector<String>();
//			list.add(s);
//			double freq = trie.getFreq(list);
//			charFreq.add(freq);
//		}
		
		int totalWords = 0;
		HashMap<List<String>,Integer> lexicon = new HashMap<List<String>,Integer>();
		List<List<String>> segments = corpus.getSegments(cutPoints);
		for (List<String> seg : segments) {
//			System.out.println(seg);
			totalWords++;
			if (lexicon.containsKey(seg)) { lexicon.put(seg, lexicon.get(seg) + 1); } 
			else { lexicon.put(seg, 1);	}
		}
		
		assert (totalWords == segments.size());
		
		// Here's the Argamon cost of the lexicon
//		double b = Stats.entropy(charFreq); // b is the entropy of the character distribution
//		double lexiconCost = 0;
//		for (List<String> word : lexicon.keySet()) {
//			lexiconCost += b * word.size();
//		}
		
		// Here's the Zhikov cost of the lexicon
		HashMap<String, Integer> letterCounts = new HashMap<String, Integer>();
		int total = 0;
		for (List<String> word : lexicon.keySet()) {
			total += word.size();
			for (String letter : word) {
				if (letterCounts.containsKey(letter)) {
					letterCounts.put(letter, letterCounts.get(letter) + 1);
				} else {
					letterCounts.put(letter, 1);
				}
			}
		}
		double totalDouble = (double) total;
		double lexiconCost = 0.0;
		for (String letter : letterCounts.keySet()) {
			double letterCount = (double) letterCounts.get(letter);
			double letterProb = letterCount / totalDouble;
			
			lexiconCost -= letterCount * Stats.log(letterProb);
		}
		
		// Now the corpus encoding cost
		double corpusCost = 0;
		for (List<String> word : lexicon.keySet()) {
			corpusCost += lexicon.get(word) * (Stats.log(lexicon.get(word)) - (Stats.log(totalWords) ));
		}
		corpusCost = -corpusCost;
		
		double totalCost = lexiconCost + corpusCost; 
//		// Parameters...
		totalCost += ((lexicon.size() - 1.0) / 2.0) * Stats.log(segments.size());

		// This term is just a constant for any given corpus, so is not really needed
//		totalCost += ((letterCounts.size() - 1.0) / 2.0) * Stats.log(corpus.getCleanChars().size());
		
//		System.out.println(	"LEX: " + NF.format(lexiconCost) + 
//				" CORP: " + NF.format(corpusCost) + 
//				" DL: " + NF.format(totalCost));
		
		// The total information cost is simply lexiconCost + corpusCost
		return totalCost;
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Corpus c = Corpus.autoLoad("br87", CorpusType.LETTER, true);
		System.out.println(NF.format(MDL.computeDescriptionLength(c, c.getCutPoints())));
	}

}
