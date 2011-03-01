package edu.arizona.ve.algorithm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.List;
import java.util.Vector;

import edu.arizona.ve.algorithm.incremental.IncrementalAlgorithm;
import edu.arizona.ve.algorithm.incremental.IncrementalBVE;
import edu.arizona.ve.algorithm.incremental.IncrementalPtM;

public class Incremental {

	public static List<String> split(String s) {
		List<String> result = new Vector<String>();
		for (Character c : s.toCharArray()) {
			result.add(c.toString());
		}
		return result;
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
//		String corpus = "input/brent-corpus.txt";
//		String corpus = "input/CHILDES/brent-data-brent.txt";
		String corpus = "input/letter/br87.txt";

		// TODO: Add parameters to the constructors
		IncrementalAlgorithm exp = new IncrementalPtM();
//		IncrementalAlgorithm exp = new IncrementalVE();
//		IncrementalAlgorithm exp = new IncrementalBVE();
		
		int sentNum = 0;
		
		BufferedReader in = new BufferedReader(new FileReader(corpus));
		PrintStream out = new PrintStream("output/incremental-output.txt");
		for (String line = in.readLine(); line != null; line = in.readLine()) {
			String stripped = line.replaceAll(" ", "");
			String segmented = exp.segment(stripped); // this does the actual segmentation
			
			System.out.println(++sentNum + ": " + segmented);
			out.println(segmented);
			
			if (!segmented.replaceAll(" ", "").equals(stripped))
				System.exit(1);
			
			exp.commit(segmented);
		}
		
		out.close();
		
		MBDP1.score("output/incremental-output.txt", corpus, 500);
	}

}
