package edu.arizona.ve.experts;

import java.util.List;

import edu.arizona.ve.trie.Trie;


/**
*
* @author  Daniel Hewlett
*/
public abstract class Expert {
	
	protected Trie _trie;
	
	protected double[] _scores;
	
	public Expert(Trie trie) {
		_trie = trie;
	}
	
	public abstract boolean[] segment(List<String> subSequence);
	
	// Returns the score array for the last window.
	// To be called immediately after segment
	public abstract double[] getScores();
}
