package edu.arizona.ve.algorithm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.experts.BackwardEntropyExpert;
import edu.arizona.ve.experts.BackwardPhonemeToMorphemeExpert;
import edu.arizona.ve.experts.ChunkinessExpert;
import edu.arizona.ve.experts.Expert;
import edu.arizona.ve.experts.ForwardEntropyExpert;
import edu.arizona.ve.experts.KnowledgeExpert;
import edu.arizona.ve.experts.MorphemeExpert;
import edu.arizona.ve.experts.PhonemeToMorphemeExpert;
import edu.arizona.ve.experts.SurprisalExpert;
import edu.arizona.ve.trie.Trie;

/**
 * @author Daniel Hewlett
 * A java implementation of the Voting Experts algorithm. Can be used and 
 * customized directly or through the static factory methods (makeXxxVE). 
 */
public class VotingExperts {
	
	// Locals
	protected List<String> _corpus;
	protected List<Boolean> _cutPoints;
	
	protected int _windowSize;
	protected int _threshold;
	
	protected HashMap<Expert,Integer> _experts = new HashMap<Expert,Integer>();
	protected int[] _vote;
	
	// Constructors
	public VotingExperts(Corpus c, int windowSize, int threshold) {
		_corpus = c.getCleanChars();
		_windowSize = windowSize;
		_threshold = threshold;
	}
	
	public VotingExperts(List<String> c, int windowSize, int threshold) {
		_corpus = c;
		_windowSize = windowSize;
		_threshold = threshold;
	}
	
	public void addExpert(Expert expert, int weight) {
		_experts.put(expert, weight);
	}
	
	// The Algorithm
	public void runAlgorithm(boolean uselocalMax) { 
		int numCutPoints = _corpus.size() - 1;

		_vote = new int[numCutPoints];
		
		for (int i = 0; i <= numCutPoints - _windowSize + 1; ++i) {
			List<String> subSequence = Collections.unmodifiableList(_corpus.subList(i, i + _windowSize));

			for (Expert expert : _experts.keySet()) {
				boolean[] votePoints = expert.segment(subSequence);
				for (int j = 0; j < votePoints.length; j++) {
					if (votePoints[j]) {
						int index = j + i - 1;
						if (index > 0 && index < numCutPoints) {
							_vote[index] += _experts.get(expert);
						}
					}
				}
			}
		}

		// cutPoints will be either true or false depending on whether
		// you choose to cut there (what else would that mean?)		
		makeCutPoints(numCutPoints, uselocalMax);
	}
	
	public void makeCutPoints(int numCutPoints, boolean useLocalMax) {
		_cutPoints = new ArrayList<Boolean>();
		for (int i = 0; i < numCutPoints; ++i) { 
			if (_vote[i] > _threshold) {
				if (!useLocalMax) { // automatically add the cut, don't check against neighbors
					_cutPoints.add(true);
				} else {
					if (i + 1 == numCutPoints) { // can't check next location because we're at the end
						if (_vote[i] > _vote[i-1]) {
							_cutPoints.add(true);
						} else {
							_cutPoints.add(false);
						}
					} else if (i == 0) { // can't check previous location because we're at the beginning
						if (_vote[i] > _vote[i+1]) {
							_cutPoints.add(true);
						} else {
							_cutPoints.add(false);
						}
					} else if (_vote[i] > _vote[i-1] && _vote[i] > _vote[i+1]) { // in the middle we can check both sides
						_cutPoints.add(true);
					} else { // local max checks failed, so it's not a boundary
						_cutPoints.add(false);
					}
				}
			} else {
				_cutPoints.add(false);
			}
		}
		
		// sanity check
		if (_cutPoints.size() != _corpus.size() - 1) {
			System.out.println("ERROR: VE produced the wrong number of cut points");
		}
	}

	// Getters and setters
	public void setCorpus(List<String> newCorpus) {
		_corpus = newCorpus;
	}

	public List<Boolean> getCutPoints() {
		return _cutPoints;
	}

	public int[] getVotes() {
		return _vote;
	}
	
	public String getVoteString(int length) {
		String result = new String();
		
		for (int i = 0; i < length; i++) {
			result += _corpus.get(i);
			if (i < _vote.length)
				result += _vote[i];	
		}
		
		return result;
	}
	
	public String getSegmentedString(int length, int threshold) {
		String result = new String();
		
		for (int i = 0; i < length; i++) {
			result += _corpus.get(i);
			if (i < _vote.length) {
				if (_vote[i] > threshold) { 
					result += "|";	
				}
			}
		}
		
		return result;
	}
	
	public Set<List<String>> getLexicon() {
		Set<List<String>> segments = new HashSet<List<String>>();
		List<String> segment = new ArrayList<String>();
		for (int i = 0; i < _corpus.size(); i++) {
			if (i < _cutPoints.size() && _cutPoints.get(i)) {
				segment.add(_corpus.get(i));
				segments.add(segment);
				segment = new ArrayList<String>();
			} else {
				segment.add(_corpus.get(i));
			}
		}
		
		if (segment.size() > 0) {
			segments.add(segment); 
		}
		
		return segments;
	}
	
	public void setThreshold(int threshold) {
		_threshold = threshold;
	}
	
	// Convenience Functions (Factory)
	public static VotingExperts makeForwardVE(Corpus c, Trie t, int windowSize, int threshold) {
		VotingExperts ve = new VotingExperts(c, windowSize, threshold);
		ve.addExpert(new SurprisalExpert(t), 1);
		ve.addExpert(new ForwardEntropyExpert(t), 1);
		return ve;
	}
	
	public static VotingExperts makeBackwardVE(Corpus c, Trie forward, Trie backward, int windowSize, int threshold) {
		VotingExperts ve = new VotingExperts(c, windowSize, threshold);
		ve.addExpert(new SurprisalExpert(forward), 1);
		ve.addExpert(new BackwardEntropyExpert(backward), 1);
		return ve;
	}
	
	public static VotingExperts makeBidiVE(Corpus c, Trie forward, Trie backward, int windowSize, int threshold) {
		VotingExperts ve = new VotingExperts(c, windowSize, threshold);
		ve.addExpert(new SurprisalExpert(forward), 1);
		ve.addExpert(new ForwardEntropyExpert(forward), 1);
		ve.addExpert(new BackwardEntropyExpert(backward), 1);
		return ve;
	}
	
	public static VotingExperts makeMorphemeVE(Corpus c, Trie forward, Trie backward, int windowSize, int threshold) {
		VotingExperts ve = new VotingExperts(c, windowSize, threshold);
		ve.addExpert(new SurprisalExpert(forward), 1);
		ve.addExpert(new ForwardEntropyExpert(forward), 1);
		ve.addExpert(new BackwardEntropyExpert(backward), 1);
		ve.addExpert(new MorphemeExpert(forward, backward), 1);
		return ve;
	}
	
	public static VotingExperts makeBVE(Corpus c, Trie forward, Trie knowledgeTrie, int windowSize, int threshold) {
		VotingExperts ve = new VotingExperts(c, windowSize, threshold);
		ve.addExpert(new SurprisalExpert(forward), 1);
		ve.addExpert(new ForwardEntropyExpert(forward), 1);
		ve.addExpert(new KnowledgeExpert(knowledgeTrie), 8);
		return ve;
	}
	
	public static VotingExperts makeBidiBVE(Corpus c, Trie forward, Trie backward, Trie knowledgeTrie, int windowSize, int threshold) {
		VotingExperts ve = new VotingExperts(c, windowSize, threshold);
		ve.addExpert(new SurprisalExpert(forward), 1);
		ve.addExpert(new ForwardEntropyExpert(forward), 1);
		ve.addExpert(new BackwardEntropyExpert(backward), 1);
		ve.addExpert(new KnowledgeExpert(knowledgeTrie), 8);
		return ve;
	}

	public static VotingExperts makeChunkVE(Corpus c, Trie forward, Trie backward, int windowSize, int threshold) {
		VotingExperts ve = new VotingExperts(c, windowSize, threshold);
		ve.addExpert(new ChunkinessExpert(forward, backward), 1);
		
		return ve;
	}
	
	public static VotingExperts makeOmniVE(Corpus c, Trie forward, Trie backward, int windowSize, int threshold) {
		VotingExperts ve = new VotingExperts(c, windowSize, threshold);
		ve.addExpert(new SurprisalExpert(forward), 1);
		ve.addExpert(new ForwardEntropyExpert(forward), 1);
		ve.addExpert(new BackwardEntropyExpert(backward), 1);
		ve.addExpert(new ChunkinessExpert(forward, backward), 1);
		ve.addExpert(new PhonemeToMorphemeExpert(forward), 1);
		ve.addExpert(new BackwardPhonemeToMorphemeExpert(backward), 1);
		return ve;
	}
	
}
