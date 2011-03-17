package edu.arizona.ve.corpus;

import edu.arizona.ve.util.NF;

public class CorpusPrinter {

	public static String printScores(Corpus c, double[] scores, int length) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0 ; i < length; i++) {
			sb.append(c.getCleanChars().get(i));
			sb.append('[');
			sb.append(NF.format(scores[i]));
			sb.append(']');
		}
		return sb.toString();
	}
	
	public static String printVotes(Corpus c, int[] votes, int length) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0 ; i < length; i++) {
			sb.append(c.getCleanChars().get(i));
			sb.append('[');
			sb.append(votes[i]);
			sb.append(']');
		}
		return sb.toString();
	}
	
	public static String printCuts(Corpus c, boolean[] votes, int length) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0 ; i < length; i++) {
			sb.append(c.getCleanChars().get(i));
			if (votes[i]) {
				sb.append('|');
			}
		}
		return sb.toString();
	}
}
