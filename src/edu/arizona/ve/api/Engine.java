package edu.arizona.ve.api;

import java.util.List;
import java.util.Vector;

import edu.arizona.ve.algorithm.VotingExperts;
import edu.arizona.ve.api.Segmentation.Direction;
import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.corpus.Corpus.CorpusType;
import edu.arizona.ve.corpus.CorpusWriter;
import edu.arizona.ve.evaluation.EvaluationResults;
import edu.arizona.ve.evaluation.Evaluator;
import edu.arizona.ve.mdl.MDL;
import edu.arizona.ve.trie.Trie;
import edu.arizona.ve.util.Utils;

/**
* @author Daniel Hewlett
*/
public class Engine {

	public static boolean EVAL_FORWARD = false;
	public static boolean EVAL_BACKWARD = false;
	public static boolean EVALUATE = false;
	
	public static boolean DEBUG = false;
	
	int trieDepth;

	// The corpus 
	Corpus corpus;
	
	// The tries
	Trie forwardTrie;
	Trie backwardTrie;

	// The knowledge trie for BVE
	Trie forwardKnowledgeTrie;
	
	// Segmentations
	Segmentation forwardSegmentation = null;
	Segmentation backwardSegmentation = null;
	Segmentation partialSegmentation = null;
	Segmentation bidiSegmentation = null;
	
	// TODO: Implement a more elegant way to test the transfer condition
	VotingExperts pve;

	public Engine(Corpus corpus, int trieDepth) {
		this.trieDepth = trieDepth;
		this.corpus = corpus;
		initTries();
	}

	public void setCorpus(Corpus c) {
		corpus = c;
	}
	
	private void initTries() {
		forwardTrie = corpus.makeForwardTrie(trieDepth);
		backwardTrie = corpus.makeBackwardTrie(trieDepth);
	}
	
	public Trie getForwardKnowledgeTrie() {
		return forwardKnowledgeTrie;
	}
		
	public List<String> getForwardCorpus() {
		return corpus.getCleanChars();
	}
	
	public Corpus getCorpus() {
		return corpus;
	}
	
	public Trie getForwardTrie() {
		return forwardTrie;
	}

	public Trie getBackwardTrie() {
		return backwardTrie;
	}

	public Segmentation voteBVE(int window, int minThreshold, boolean useLocalMax, boolean bidiBVE) {

		int startThreshold = window * (bidiBVE ? 3 : 2); // window + thresholdOffset 
		
		// Experimenting with dummy corpus 0 this just means the first time the knowledge expert won't do anything
		List<String> forwardKnowledgeCorpus = corpus.getCleanChars(); //new ArrayList<String>(bidiInput.getCleanChars());
		forwardKnowledgeTrie = new Trie();
		Trie.addAll(forwardKnowledgeTrie, forwardKnowledgeCorpus, trieDepth+1);
		forwardKnowledgeTrie.generateStatistics();

		partialSegmentation = votePartial(window, startThreshold, useLocalMax, bidiBVE);
		CorpusWriter.writeCorpus(corpus.getName()  + "-partial-1.txt", corpus, partialSegmentation);
		
		if (DEBUG)
			evaluate();
		
		bidiSegmentation = null; // stop the printing of the bidi array, we're done with it
		
		boolean[] lastSegmentation = partialSegmentation.cutPoints;
		
		int i = 1;
		int cyclesAtLevel = 0;
		for (int threshold = startThreshold - 1; threshold >= minThreshold; ) {
			Corpus partialInputRest = new Corpus();
			partialInputRest.naiveLoad("output/" + corpus.getName()  + "-partial-" + i + ".txt", corpus.getType());
			
			forwardKnowledgeCorpus = partialInputRest.getCleanChars();
			forwardKnowledgeTrie = new Trie();
			Trie.addAll(forwardKnowledgeTrie, forwardKnowledgeCorpus, trieDepth+1);
			forwardKnowledgeTrie.generateStatistics();
			
			partialSegmentation = votePartial(window, threshold, useLocalMax, bidiBVE);
			CorpusWriter.writeCorpus(corpus.getName()  + "-partial-" + (i+1) + ".txt", corpus, partialSegmentation);
			
			if (DEBUG)
				evaluate();
			
			i++;

			threshold--;
			
//			if (Arrays.equals(partialSegmentation.cutPoints, lastSegmentation) || cyclesAtLevel > 6) {
//				threshold--;
//				cyclesAtLevel = 0;
//			} else {
//				cyclesAtLevel++;
//			}
			lastSegmentation = partialSegmentation.cutPoints;
		}
		
		//Trie.extractWords(forwardKnowledgeTrie);
		
		return partialSegmentation;
	}
	
	public List<Segmentation> voteBVEMDL(int window, boolean useLocalMax, boolean bidiBVE) {
		
		int startThreshold = (window * (bidiBVE ? 3 : 2)) - 1; // window + thresholdOffset 
		
		// Experimenting with dummy corpus 0 this just means the first time the knowledge expert won't do anything
		List<String> forwardKnowledgeCorpus = corpus.getCleanChars(); //new ArrayList<String>(bidiInput.getCleanChars());
		forwardKnowledgeTrie = new Trie();
		Trie.addAll(forwardKnowledgeTrie, forwardKnowledgeCorpus, trieDepth+1);
		forwardKnowledgeTrie.generateStatistics();

		partialSegmentation = votePartial(window, startThreshold, useLocalMax, bidiBVE);
		CorpusWriter.writeCorpus(corpus.getName()  + "-partial-" + startThreshold + ".txt", corpus, partialSegmentation);
		
		if (DEBUG)
			evaluate();
		
		bidiSegmentation = null; // stop the printing of the bidi array, we're done with it
		
		// For now, let's segment with all thresholds and pick the best
		
		double minDL = Double.MAX_VALUE;
		Segmentation bestSegmentation = null;
		Vector<Segmentation> segmentations = new Vector<Segmentation>();
		
		for (int threshold = startThreshold - 1; threshold >= 0; threshold-- ) {
			Corpus partialInputRest = new Corpus();
			partialInputRest.naiveLoad("output/" + corpus.getName()  + "-partial-" + (threshold+1) + ".txt", corpus.getType());
			
			forwardKnowledgeCorpus = partialInputRest.getCleanChars();
			forwardKnowledgeTrie = new Trie();
			Trie.addAll(forwardKnowledgeTrie, forwardKnowledgeCorpus, trieDepth+1);
			forwardKnowledgeTrie.generateStatistics();
			
			partialSegmentation = votePartial(window, threshold, useLocalMax, bidiBVE);
			CorpusWriter.writeCorpus(corpus.getName()  + "-partial-" + threshold + ".txt", corpus, partialSegmentation);

//			System.out.println("THRESHOLD: " + threshold + "\tDL: " + partialSegmentation.descriptionLength);
			if (partialSegmentation.descriptionLength < minDL) {
				minDL = partialSegmentation.descriptionLength;
				bestSegmentation = partialSegmentation;
			}
			
			segmentations.add(partialSegmentation);
			
			if (DEBUG) {
				evaluate();
				System.out.println();
			}
		}
		
		//Trie.extractWords(forwardKnowledgeTrie);
		
		partialSegmentation = bestSegmentation;
		
		return segmentations;
		
//		return bestSegmentation;
	}
	
	// TODO: What is the difference between this and voteKnowledgeTransfer
	public Segmentation voteTransfer(Corpus newCorpus, int window, int threshold, boolean useLocalMax) {
		setCorpus(newCorpus);
		return votePartial(window, threshold, useLocalMax, false);
	}
	
	public Segmentation voteForward(int windowSize, int threshold, boolean useLocalMax) {
		VotingExperts ve = VotingExperts.makeForwardVE(corpus, forwardTrie, windowSize, threshold);
	    ve.runAlgorithm(useLocalMax);
	    
	    Segmentation s = new Segmentation(windowSize, threshold);
	    s.cutPoints = Utils.makeArray(ve.getCutPoints());
	    s.localMax = useLocalMax;
	    s.descriptionLength = MDL.computeDescriptionLength(corpus, s.cutPoints);

	    forwardSegmentation = s;
	    
	    return s;
	}
	
	public Segmentation voteBackward(int windowSize, int threshold, boolean useLocalMax) {
	    VotingExperts ve = VotingExperts.makeBackwardVE(corpus, forwardTrie, backwardTrie, windowSize, threshold);
	    ve.runAlgorithm(useLocalMax);
	    
	    Segmentation s = new Segmentation(windowSize, threshold);
	    s.cutPoints = Utils.makeArray(ve.getCutPoints());
	    s.localMax = useLocalMax;
	    s.descriptionLength = MDL.computeDescriptionLength(corpus, s.cutPoints);

	    backwardSegmentation = s;
	    
	    System.out.println(ve.getVoteString(100));
	    System.out.println(ve.getSegmentedString(100, threshold));
	    
	    return s;
	}
	
	public List<Segmentation> bidiVoteAllThresholds(int windowSize, int minThreshold, int maxThreshold) {
		Vector<Segmentation> segmentations = new Vector<Segmentation>();
		VotingExperts ve = VotingExperts.makeBidiVE(corpus, forwardTrie, backwardTrie, windowSize, minThreshold);
		for (int t = minThreshold; t < maxThreshold; t++) {
			ve.setThreshold(t);
			
			ve.runAlgorithm(false);
			
			Segmentation maxOff = new Segmentation();
			maxOff.windowSize = windowSize;
			maxOff.threshold = t;
			maxOff.localMax = false;
			maxOff.cutPoints = Utils.makeArray(ve.getCutPoints());
			maxOff.descriptionLength = MDL.computeDescriptionLength(corpus, maxOff.cutPoints);
			segmentations.add(maxOff);
			
			ve.makeCutPoints(ve.getCutPoints().size(), true);
			
			Segmentation maxOn = new Segmentation();
			maxOn.windowSize = windowSize;
			maxOn.threshold = t;
			maxOn.localMax = true;
			maxOn.cutPoints = Utils.makeArray(ve.getCutPoints());
			maxOn.descriptionLength = MDL.computeDescriptionLength(corpus, maxOn.cutPoints);
			
			segmentations.add(maxOn);
		}
		return segmentations;
	}
	
	public List<Segmentation> voteAllThresholds(int windowSize, int minThreshold, int maxThreshold) {
		Vector<Segmentation> segmentations = new Vector<Segmentation>();
		VotingExperts ve = VotingExperts.makeForwardVE(corpus, forwardTrie, windowSize, minThreshold); 
		for (int t = minThreshold; t < maxThreshold; t++) {

			ve.setThreshold(t);
			
			// Local Max OFF
			ve.runAlgorithm(false);
			
			Segmentation maxOff = new Segmentation();
			maxOff.windowSize = windowSize;
			maxOff.threshold = t;
			maxOff.localMax = false;
			maxOff.cutPoints = Utils.makeArray(ve.getCutPoints());
			maxOff.descriptionLength = MDL.computeDescriptionLength(corpus, maxOff.cutPoints);
			segmentations.add(maxOff);
			
			// Local Max ON
			ve.makeCutPoints(ve.getCutPoints().size(), true);
			
			Segmentation maxOn = new Segmentation();
			maxOn.windowSize = windowSize;
			maxOn.threshold = t;
			maxOn.localMax = true;
			maxOn.cutPoints = Utils.makeArray(ve.getCutPoints());
			maxOn.descriptionLength = MDL.computeDescriptionLength(corpus, maxOn.cutPoints);
			
			segmentations.add(maxOn);
		}
		return segmentations;
	}
	
	public Segmentation votePartial(int windowSize, int threshold, boolean useLocalMax, boolean bidi) {
	    VotingExperts pve;
	    if (bidi) {
	    	pve = VotingExperts.makeBidiBVE(corpus, forwardTrie, backwardTrie, forwardKnowledgeTrie, windowSize, threshold);
	    } else {
	    	pve = VotingExperts.makeBVE(corpus, forwardTrie, forwardKnowledgeTrie, windowSize, threshold);
	    }
	    pve.runAlgorithm(useLocalMax);
	    
	    Segmentation s = new Segmentation(windowSize, threshold);
	    s.cutPoints = Utils.makeArray(pve.getCutPoints());
	    s.localMax = useLocalMax;
	    s.descriptionLength = MDL.computeDescriptionLength(corpus, s.cutPoints);
	    
	    partialSegmentation = s;
	    
	    if (DEBUG) {
//	    	System.out.println();
		    System.out.println(pve.getVoteString(100));
		    System.out.println(pve.getSegmentedString(100, threshold));
//		    System.out.println(s.descriptionLength);
	    }
	    
	    return s;
	}
	
	public Segmentation voteKnowledgeTransfer(int windowSize, int threshold, boolean useLocalMax) {
		pve.setCorpus(corpus.getCleanChars());
		pve.runAlgorithm(useLocalMax);
	    
		Segmentation s = new Segmentation(windowSize, threshold);
	    s.cutPoints = Utils.makeArray(pve.getCutPoints());
	    s.localMax = useLocalMax;
	    s.descriptionLength = MDL.computeDescriptionLength(corpus, s.cutPoints);
	    
	    return s;
	}	    
	
	public Segmentation voteBidi(int windowSize, int threshold, boolean useLocalMax) {
	    VotingExperts ve = VotingExperts.makeBidiVE(corpus, forwardTrie, backwardTrie, windowSize, threshold);
	    ve.runAlgorithm(useLocalMax);

	    System.out.println(ve.getVoteString(100));
	    System.out.println(ve.getSegmentedString(100, threshold));
	    
	    bidiSegmentation = new Segmentation(windowSize, threshold);
	    bidiSegmentation.direction = Direction.BiDirectional;
	    bidiSegmentation.cutPoints = Utils.makeArray(ve.getCutPoints());
	    bidiSegmentation.localMax = useLocalMax;
	    bidiSegmentation.descriptionLength = MDL.computeDescriptionLength(corpus, bidiSegmentation.cutPoints);

	    return bidiSegmentation;
	}
	
	public Segmentation voteMorpheme(int windowSize, int threshold, boolean useLocalMax) {
	    VotingExperts ve = VotingExperts.makeMorphemeVE(corpus, forwardTrie, backwardTrie, windowSize, threshold);
	    ve.runAlgorithm(useLocalMax);

//	    System.out.println(ve.getVoteString(100));
	    
	    bidiSegmentation = new Segmentation(windowSize, threshold);
	    bidiSegmentation.direction = Direction.BiDirectional;
	    bidiSegmentation.cutPoints = Utils.makeArray(ve.getCutPoints());
	    bidiSegmentation.localMax = useLocalMax;
	    bidiSegmentation.descriptionLength = MDL.computeDescriptionLength(corpus, bidiSegmentation.cutPoints);

	    return bidiSegmentation;
	}
	
	public void evaluate() {
		if (!EVALUATE) {
			return;
		}
		
		if (EVAL_FORWARD) {
		    System.out.println("FORWARD:");
		    EvaluationResults forwardResults = Evaluator.evaluate(forwardSegmentation.cutPoints, corpus.getCutPoints());
		    forwardResults.printResults();
//		    System.out.println("DL: " + forwardSegmentation.descriptionLength);
//		    System.out.println();
		}
	    
		if (EVAL_BACKWARD) {
		    System.out.println("BACKWARD:");
		    Evaluator.evaluate(backwardSegmentation.cutPoints, corpus.getCutPoints()).printResults();
//		    System.out.println("DL: " + backwardSegmentation.descriptionLength);
//		    System.out.println();
		}
	    
	    if (bidiSegmentation != null) {
	    	System.out.println("BIDI:");
	    	Evaluator.evaluate(bidiSegmentation.cutPoints, corpus.getCutPoints()).printResults();
//		    System.out.println("DL: " + bidiSegmentation.descriptionLength);
//		    System.out.println();
	    }
	    
	    if (partialSegmentation != null) {
	    	System.out.println("BOOTSTRAP (" + partialSegmentation.windowSize + "," + partialSegmentation.threshold + "):");
	    	Evaluator.evaluate(partialSegmentation.cutPoints, corpus.getCutPoints()).printResults();
//		    System.out.println("DL: " + partialSegmentation.descriptionLength);
//		    System.out.println();
	    }
	}
	
	// Example of how to use the Engine class
	public static void main(String[] args) {	
		Engine.EVALUATE = true;
		Engine.DEBUG = true;
		
//		Corpus corpus = Corpus.autoLoad("zarathustra", "downcase");
		Corpus corpus = Corpus.autoLoad("br87", CorpusType.LETTER, true);
		
		int window = 6;
		
		Engine e = new Engine(corpus, window+1);
//		e.voteBackward(7, 3, false);
		e.voteBVE(window, 0, false, true);
		e.evaluate();
		
//		Engine.bidiBootstrap(corpus, 7, 14, 3, false);
	}
}
