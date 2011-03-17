package edu.arizona.ve.algorithm.incremental;

import java.util.Collections;
import java.util.List;

import edu.arizona.ve.algorithm.Incremental;
import edu.arizona.ve.algorithm.PhonemeToMorpheme;
import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.trie.Trie;

public class IncrementalPtM implements IncrementalAlgorithm {
	Trie fTrie, bTrie;
	int trieDepth;
	int windowSize = 6;
	double threshold = 0.75;
	
	public IncrementalPtM() {
		fTrie = new Trie();
		bTrie = new Trie();
		trieDepth = windowSize + 1;
	}
	
	@Override
	public String segment(String utterance) {
		Corpus cl = new Corpus();
		cl.loadString(utterance);
		
		PhonemeToMorpheme ptm = new PhonemeToMorpheme(fTrie, bTrie, cl, windowSize);
		ptm.runAlgorithm(threshold);
		
		String segmented = new String();
		for (List<String> seg : cl.getSegments(ptm.getCutPoints())) {
			for (String c : seg) { segmented += c; }
			segmented += " ";
		}
		segmented = segmented.trim();
		
		return segmented;
	}

	@Override
	public void commit(String segmented) {
		Trie.addAll(fTrie, Incremental.split(segmented.replaceAll(" ", "")), trieDepth);
		fTrie.generateStatistics();
		
		List<String> letters = Incremental.split(segmented.replaceAll(" ", ""));
		Collections.reverse(letters);
		Trie.addAll(bTrie, letters, trieDepth);
		bTrie.generateStatistics();
	}

}
