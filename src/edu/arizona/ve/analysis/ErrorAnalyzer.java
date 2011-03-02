package edu.arizona.ve.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.corpus.Corpus.CorpusType;
import edu.arizona.ve.experts.ChunkinessExpert;
import edu.arizona.ve.experts.ForwardEntropyExpert;
import edu.arizona.ve.experts.MorphemeExpert;
import edu.arizona.ve.experts.SurprisalExpert;
import edu.arizona.ve.trie.Trie;

public class ErrorAnalyzer {

	public static boolean scoreWindow(List<Boolean> actual, boolean[] ve) {
//		System.out.println(actual);
//		System.out.println(Arrays.toString(ve));
//		
//		"a".charAt(0);
		
		for (int i = 0; i < actual.size(); i++) {
			if (actual.get(i) != ve[i])
				return false;
		}
		
		return true;
	}
	
	public static boolean scorePrecision(List<Boolean> actual, boolean[] ve) {
		for (int i = 0; i < actual.size(); i++) {
			if (ve[i] && !actual.get(i))
				return false;
		}
		
		return true;
	}
	
	public static void analyzeErrors(Corpus corpus, int windowSize) {
		System.out.println("========================================================");
		System.out.println("Performing Error Analysis for Window Size " + windowSize);
		
		HashMap<List<String>, HashMap<List<Boolean>, Integer>> m = new HashMap<List<String>, HashMap<List<Boolean>, Integer>>();
		
		Trie trie = corpus.makeForwardTrie(windowSize + 1);
		Trie backwardTrie = corpus.makeBackwardTrie(windowSize + 1);
		
		ForwardEntropyExpert fe = new ForwardEntropyExpert(trie);
//		BackwardEntropyExpert be = new BackwardEntropyExpert(backwardTrie);
//		BackwardPhonemeToMorphemeExpert be = new BackwardPhonemeToMorphemeExpert(backwardTrie);
		MorphemeExpert be = new MorphemeExpert(trie, backwardTrie);
		SurprisalExpert se = new SurprisalExpert(trie);
		ChunkinessExpert ce = new ChunkinessExpert(trie, backwardTrie);
//		MaximumLikelihoodExpert me = new MaximumLikelihoodExpert()
		
		ArrayList<Boolean> cuts = new ArrayList<Boolean>(corpus.getCutPoints().length + 2);
		cuts.add(true);
		for (int i = 0; i < corpus.getCutPoints().length; i++) 
			cuts.add(corpus.getCutPoints()[i]);
		cuts.add(true);
		
		int totalWindows = 0;
		int errorWindows = 0;
		int zeroWindows = 0;
		int multWindows = 0;
//		int veErrorWindows = 0; 
		int seErrorWindows = 0, feErrorWindows = 0, beErrorWindows = 0, ceErrorWindows = 0, cePrecisionWindows = 0, sePrecisionWindows = 0;
		int bePrecisionWindows = 0;
		int agreeWindows = 0;
		int fePrecisionWindows = 0;
		int totalVoteChances = 0;
		int errors = 0;
		for (int i = 0; i <= corpus.getCleanChars().size() - windowSize; i++) {
			List<String> subSeq = Collections.unmodifiableList(corpus.getCleanChars().subList(i, i + windowSize));
			List<Boolean> actualCuts = Collections.unmodifiableList(cuts.subList(i, i + windowSize + 1));
			
			int val = 0;
            HashMap<List<Boolean>, Integer> temp = new HashMap<List<Boolean>, Integer>();
            temp.put(actualCuts, 1);
            //HashMap<List<String>, HashMap<List<Boolean>, Integer>> temp2;
            //temp2.put(subSeq, temp);
            
            if(m.containsKey(subSeq)) {
            	temp = m.get(subSeq);
            	if(temp.containsKey(actualCuts)) {
            		val = temp.get(actualCuts);
            		temp.put(actualCuts, ++val);
            		//val = m.get(subSeq);
            		//m.put(subSeq, ++val);
            	}
            	else
            		temp.put(actualCuts, 1);
            }
            else {
            	m.put(subSeq, temp);
            }
			
			int numCutPoints = countCutPoints(actualCuts);
			errors += Math.abs(numCutPoints - 1);
			errorWindows += (numCutPoints == 1 ? 0 : 1);
			
			if (numCutPoints == 0) { zeroWindows++; }
			if (numCutPoints > 1) { multWindows++; }
			
			boolean[] feCuts = fe.segment(subSeq);
			if (!scoreWindow(actualCuts, feCuts))
				feErrorWindows++;
			if (scorePrecision(actualCuts, feCuts)) 
				fePrecisionWindows++;
			
			boolean[] beCuts = be.segment(subSeq);
			if (!scoreWindow(actualCuts, beCuts))
				beErrorWindows++;
			if (scorePrecision(actualCuts, beCuts)) 
				bePrecisionWindows++;
//			
			boolean[] seCuts = se.segment(subSeq);
			if (!scoreWindow(actualCuts, seCuts))
				seErrorWindows++;
			if (scorePrecision(actualCuts, seCuts)) 
				sePrecisionWindows++;
			
			
//			boolean correct = scoreWindow(actualCuts, Utils.simpleUnion(Utils.simpleUnion(feCuts, seCuts), beCuts));
//			if (!correct) { veErrorWindows++; };
			
			boolean[] ceCuts = ce.segment(subSeq);
            if (!scoreWindow(actualCuts, ceCuts))
                ceErrorWindows++;
            if (scorePrecision(actualCuts, ceCuts))
                cePrecisionWindows++;
			
            if (Arrays.equals(beCuts, feCuts)) {
            	agreeWindows++;
            }
            
			totalWindows++;
			totalVoteChances += actualCuts.size();
//			System.out.println(subSeq + " " + actualCuts);
		}
		
		int myTotal = countHashValues(m);
        int totalHashErrors = countHashErrors(m);
        System.out.println(">> Total best case: " + totalHashErrors);
        System.out.println(">> Errors are = " + (totalWindows - totalHashErrors));
        System.out.println(">> Forced Errors: " + (totalWindows - totalHashErrors) + " (" + ((double) (totalWindows - totalHashErrors) / totalWindows) * 100 + "%)");
        System.out.println(">> My total is: " + myTotal);
		
		System.out.println("Windows: " + totalWindows);
		// This one isn't exactly right
//		System.out.println("VE Error Windows: " + veErrorWindows + " (" + ((double) veErrorWindows / totalWindows) * 100 + "%)");
//		System.out.println("Entropy Error Windows: " + feErrorWindows + " (" + ((double) feErrorWindows / totalWindows) * 100 + "%)");
		System.out.println("F Entropy Prec. Windows: " + fePrecisionWindows + " (" + ((double) fePrecisionWindows / totalWindows) * 100 + "%)");
		System.out.println("B Entropy Prec. Windows: " + bePrecisionWindows + " (" + ((double) bePrecisionWindows / totalWindows) * 100 + "%)");

		System.out.println("Surprisal Prec. Windows: " + sePrecisionWindows + " (" + ((double) sePrecisionWindows / totalWindows) * 100 + "%)");

		System.out.println("Agreement Windows: " + agreeWindows + " (" + ((double) agreeWindows / totalWindows) * 100 + "%)");
		
//		System.out.println("Chunk Error Windows: " + ceErrorWindows + " (" + ((double) ceErrorWindows / totalWindows) * 100 + "%)");
		System.out.println("Chunk Prec. Windows: " + cePrecisionWindows + " (" + ((double) cePrecisionWindows / totalWindows) * 100 + "%)");
		
		
//		System.out.println("Surp. Error Windows: " + seErrorWindows + " (" + ((double) seErrorWindows / totalWindows) * 100 + "%)");
//		System.out.println("Forced Error Windows: " + errorWindows + " (" + ((double) errorWindows / totalWindows) * 100 + "%)");
//		System.out.println("  Zero Cuts: " + zeroWindows + " (" + ((double) zeroWindows / errorWindows) * 100 + "%)");
//		System.out.println("  Multiple Cuts: " + multWindows + " (" + ((double) multWindows / errorWindows) * 100 + "%)");
//		System.out.println(zeroWindows + " " + multWindows);
//		System.out.println("Vote Chances: " + totalVoteChances);
//		System.out.println("Errors: " + errors + "(" + ((double) errors / totalVoteChances) * 100 + "%)");
	}
	
	public static int countCutPoints(List<Boolean> cuts) {
		int count = 0;
		for (boolean b : cuts) {
			if (b) { count++; }
		}
		return count;
	}
	
    public static int countHashValues(HashMap<List<String>, HashMap<List<Boolean>, Integer>> m) {
    	int count = 0;
    	Set<List<String>> keys = m.keySet();
    	
    	for(List<String> i : keys) {
    		HashMap<List<Boolean>, Integer> temp = m.get(i);
    		Set<List<Boolean>> keys2 = temp.keySet();
    		for(List<Boolean> j : keys2) {
    			int v = (Integer)temp.get(j);
    			count += v;
    		}
    	}
    	return count;
    }
    
    public static int countHashErrors(HashMap<List<String>, HashMap<List<Boolean>, Integer>> m) {
    	int total = 0;
    	Set<List<String>> keys = m.keySet();
    	int v = 0;
    	int max = 0;
    	
    	for(List<String> i : keys) {
    		HashMap<List<Boolean>, Integer> temp = m.get(i);
    		Set<List<Boolean>> keys2 = temp.keySet();
    		for(List<Boolean> j : keys2) {
    			v = (Integer)temp.get(j);
    			if(v > max)
    				max = v;
    		}
    		total += max;
    		max = 0;
    	}
    	return total;
    }
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Corpus corpus = Corpus.autoLoad("br87", CorpusType.LETTER, true);
//		Corpus corpus = Corpus.autoLoad("orwell-short", CorpusType.LETTER, false);
		for (int windowSize = 4; windowSize <= 7; windowSize++) {
			analyzeErrors(corpus, windowSize);
		}
	}
}
