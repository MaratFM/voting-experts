package edu.arizona.ve.algorithm.incremental;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

import edu.arizona.ve.algorithm.Incremental;
import edu.arizona.ve.algorithm.VotingExperts;
import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.experts.BackwardEntropyExpert;
import edu.arizona.ve.experts.ForwardEntropyExpert;
import edu.arizona.ve.experts.KnowledgeExpert;
import edu.arizona.ve.experts.SurprisalExpert;
import edu.arizona.ve.trie.Trie;
import edu.arizona.ve.util.Utils;

/**
*
* @author  Daniel Hewlett
* An incremental (sentence-at-a-time) version of BVE. Used for comparison with 
* MBDP-1 algorithm in Hewlett and Cohen (2009).
*/
public class IncrementalBVE implements IncrementalAlgorithm {
	VotingExperts ve;
	Trie fTrie, bTrie, kTrie;
	int trieDepth;
	int windowSize = 4; 
	int threshold = 6;
	int n = 0;
	int wait = 0; // 500;
	
	public IncrementalBVE() {
		fTrie = new Trie();
		bTrie = new Trie();
		kTrie = new Trie();
		trieDepth = windowSize + 1;
	}
	
	public String segment(String utterance) {
		if (n < wait) { n++; return utterance; } // can't segment the first utterance, plus don't start the bootstrapping too early 
		
		Corpus cl = new Corpus();
		cl.loadString(utterance);
		
//		ve = VotingExperts.makeBidiBVE(cl, fTrie, bTrie, kTrie, windowSize, threshold);
		VotingExperts ve = new VotingExperts(cl, windowSize, threshold);
		ve.addExpert(new SurprisalExpert(fTrie), 1);
		ve.addExpert(new ForwardEntropyExpert(fTrie), 1);
		ve.addExpert(new BackwardEntropyExpert(bTrie), 1);
		ve.addExpert(new KnowledgeExpert(kTrie), 3);
		
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
		
		String seg = segmented.replaceAll(" ", "*"); // Shit. Remember that * is in the phoneme encoding that Brent uses
		List<String> f = new Vector<String>(Incremental.split(seg));
		f.add(0, "*"); // Beginning of utterance
		f.add("*"); // End of utterance
		Trie.addAll(kTrie, f, trieDepth);
		kTrie.generateStatistics();
	}
}
