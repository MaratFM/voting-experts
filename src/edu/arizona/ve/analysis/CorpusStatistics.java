package edu.arizona.ve.analysis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.corpus.Corpus.CorpusType;
import edu.arizona.ve.mdl.MDL;
import edu.arizona.ve.util.NF;

public class CorpusStatistics {

	/**
	 * A quick way to view relevant statistics for a corpus.
	 */
	public static void main(String[] args) {
		Corpus c = Corpus.autoLoad("thai-novel-short", CorpusType.LETTER, true);
		
		List<String> words = c.getSegments();
		
		Set<String> lexicon = new HashSet<String>(words);
		
		Set<String> alphabet = new HashSet<String>(c.getCleanChars());
		
		System.out.println("Length (Letter): " + c.getCleanChars().size());
		System.out.println("Alphabet Size:   " + alphabet.size());
		System.out.println("Length (Word):   " + words.size());
		System.out.println("Unique Words:    " + lexicon.size());
		System.out.println("DL:              " + NF.format(MDL.computeDescriptionLength(c, 
															  c.getCutPoints())));
		
	}

}
