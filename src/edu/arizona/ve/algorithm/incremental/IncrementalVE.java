package edu.arizona.ve.algorithm.incremental;

import java.util.Collections;
import java.util.List;

import edu.arizona.ve.algorithm.Incremental;
import edu.arizona.ve.algorithm.VotingExperts;
import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.trie.Trie;
import edu.arizona.ve.util.Utils;

/**
*
* @author  Daniel Hewlett
* An incremental (sentence-at-a-time) version of VE. Used for comparison with 
* MBDP-1 algorithm in Hewlett and Cohen (2009).
*/
public class IncrementalVE implements IncrementalAlgorithm {
	Trie fTrie, bTrie;
	int trieDepth;
	int windowSize = 4;
	int threshold = 3;
	int n = 0;
	int wait = 0; // who cares for VE? This would only matter for BVE
	
	public IncrementalVE() {
		fTrie = new Trie();
		bTrie = new Trie();
		trieDepth = windowSize + 1;
	}
	
	public String segment(String utterance) {
		if (n < wait) { n++; return utterance; } // can't segment the first utterance 
		
		Corpus cl = new Corpus();
		cl.loadString(utterance);
		
		VotingExperts ve = VotingExperts.makeBidiVE(cl, fTrie, bTrie, windowSize, threshold);
		ve.runAlgorithm(false);
		
		String segmented = new String();
		for (List<String> seg : cl.getSegments(Utils.makeArray(ve.getCutPoints()))) {
			for (String c : seg) { segmented += c; }
			segmented += " ";
		}
		segmented = segmented.trim();
//		System.out.println(segmented);
//		System.out.println(ve.getSegments());
		
		n++;
		
		return segmented;
	}
	
	public void commit(String segmented) {	
		Trie.addAll(fTrie, Incremental.split(segmented.replaceAll(" ", "")), trieDepth);
		fTrie.generateStatistics();
		
		List<String> letters = Incremental.split(segmented.replaceAll(" ", ""));
		Collections.reverse(letters);
		Trie.addAll(bTrie, letters, trieDepth);
		bTrie.generateStatistics();
	}
}
