package edu.arizona.ve.algorithm;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;

import edu.arizona.ve.algorithm.mbdp1.BrentSegmentation;
import edu.arizona.ve.algorithm.mbdp1.Score;
import edu.arizona.ve.algorithm.mbdp1.Segmentation;
import edu.arizona.ve.algorithm.mbdp1.Word;


/**
*
* @author  Daniel Hewlett
* Model-Based Dynamic Programming 1 (MBDP-1) incremental (sentence-at-a-time) segmentation 
* algorithm of Michael Brent (1999).
*/
public class MBDP1 {	
	public static void score(String output, String gold, int scorePeriod) throws Exception {
		Score s = new Score();
		
		BufferedReader out = new BufferedReader(new FileReader(output));
		BufferedReader gs = new BufferedReader(new FileReader(gold));
		
		System.out.println("Pre%\tRec%\tLex%\tn+\tn-\tF%");
		
		String outLine = out.readLine();
		String goldLine = gs.readLine();
		for (int n = 1; outLine != null; n++) {
			Word useq1 = new Word();
			useq1.processLine(outLine);
			Word useq2 = new Word();
			useq2.processLine(goldLine);
			
		    if (useq1.size() > 0 && !useq1.equals(useq2)) {
		    	System.err.println("Something is wrong.  You asked me to score two dissimilar utterances:\n" +
		    			"[" + useq1 + "] and [" + useq2 + "]");
		      System.exit(1);
		    }
		    s.update(useq1, useq2);
		    if (scorePeriod > 0 && n > 0 && n % scorePeriod == 0)
		    	System.out.println(s);
		    
		    outLine = out.readLine();
			goldLine = gs.readLine();
		}
		
		System.out.println(s);
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 *
	 */
	public static void main(String[] args) throws Exception {
		int trainSize = 0, n = 0;

		// NEW
//		String corpus = "input/sent/brown-cmu.txt";
//		String corpus = "input/letter/br87.txt";
		String corpus = "input/letter/br87-cmu-sent.txt";
		
		Segmentation seg = new BrentSegmentation();
//		Segmentation seg = new HybridSegmentation();
//		Segmentation seg = new VotingExpertSegmentation();
		
//		seg.setWordTable(WordTable.fromFile("output/lexica/seed.lex"));
		
		BufferedReader in = new BufferedReader(new FileReader(corpus));
		PrintStream out = new PrintStream("output/mdbp1-output.txt");
		for (String line = in.readLine(); line != null; line = in.readLine()) {
			Word useq = new Word();
			useq.processLine(line);
			
			if (++n <= trainSize) {
				seg.commit(useq);
				System.out.println();
			} else {
				useq.delBoundaries();
				seg.dynSearch(useq); // this does the actual segmentation
				seg.commit(useq);
				System.out.println(n + ": " + useq);
				out.println(useq);
			}
			
//			seg.words.printSorted();
		}
		
//		System.out.println(seg.words);
//		seg.words.printSorted();
		out.close();
		
		score("output/mdbp1-output.txt", corpus, 500);
	}

}



//String defaultPhonemeTable = PhonemeConverter.getPhonemeTable();
//String defaultPhonemeTable = "I V E V & V A V a V O V U V 6 V i V e V 9 V Q V u V o V 7 " +
//    "V 3 V R V # V % V * V ( V ) V p C b C m C t C d C n C k C " +
//    "g C N C f C v C T C D C s C z C S C Z C h C c C G C l C r " +
//    "C L V ~ V M V y C w C W C";
