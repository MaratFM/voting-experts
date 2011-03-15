package edu.arizona.ve.algorithm;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import edu.arizona.ve.algorithm.EntropyMDL.SimulationResult.SimulationType;
import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.corpus.Corpus.CorpusType;
import edu.arizona.ve.evaluation.EvaluationResults;
import edu.arizona.ve.evaluation.Evaluator;
import edu.arizona.ve.mdl.MDL;
import edu.arizona.ve.trie.Trie;
import edu.arizona.ve.util.NF;
import edu.arizona.ve.util.Stats;

/**
 * 
 * @author Anh Tran
 * 
 * Implementation of the unsupervised word segmentation algorithm (Ent-MDL)
 * by Zhikov, Takamua, and Okumura (2010).
 * 
 */
public class EntropyMDL {

	Trie _forwardTrie, _backwardTrie;
	Corpus _corpus;
	int _maxLen;
	double[] _entropy;
	boolean[] _cutPoints1, _cutPoints2, _cutPoints3;
	double _mdl;
	Model _model;
	

	public EntropyMDL(Corpus corpus, int maxLen) {
		// Init class variables
		_corpus = corpus;
		_maxLen = maxLen;
		
		// Build tries
		_forwardTrie = corpus.makeForwardTrie(maxLen+1);
		_backwardTrie = corpus.makeBackwardTrie(maxLen+1);	
	}
	
	
	
	public boolean[] runAlgorithm() {

		// Calculate the branching entropies
		_entropy = getBranchingEntropy(_corpus, _forwardTrie, _backwardTrie);
		
		// Generate initial hypothesis using branching entropy as 'goodness' scores
		_cutPoints1 = algorithm1(_corpus, _entropy);

		System.out.println("\nAlgorithm 1:");
		Evaluator.evaluate(getCutPoints1(), _corpus.getCutPoints()).printResults();
		System.out.println("MDL: " + getMDL1());

		// Compress local token co-occurences
		_cutPoints2 = Arrays.copyOf(_cutPoints1, _cutPoints1.length);
		_cutPoints2 = algorithm2(_corpus, _cutPoints2, _entropy);

		System.out.println("\n\nAlgorithm 2:");
		Evaluator.evaluate(getCutPoints2(), _corpus.getCutPoints()).printResults();
		System.out.println("MDL: " + getMDL2());
		
		// Lexicon clean-up
		_cutPoints3 = Arrays.copyOf(_cutPoints2, _cutPoints2.length);
		_cutPoints3 = algorithm3(_corpus, _cutPoints3);

		System.out.println("\n\nAlgorithm 3:");
		Evaluator.evaluate(getCutPoints3(), _corpus.getCutPoints()).printResults();
		System.out.println("MDL: " + getMDL3());
		
		return _cutPoints3;
	}
	
	
	
	

	public boolean[] getCutPoints1() {
		return _cutPoints1;
	}

	public boolean[] getCutPoints2() {
		return _cutPoints2;
	}
	
	
	public boolean[] getCutPoints3() {
		return _cutPoints3;
	}
	
	public double getMDL1() {
		return MDL.computeDescriptionLength(_corpus, _cutPoints1);
	}
	
	public double getMDL2(){
		return MDL.computeDescriptionLength(_corpus, _cutPoints2);
	}
	
	public double getMDL3() {
		return MDL.computeDescriptionLength(_corpus, _cutPoints3);
	}
	
	
	
	
	
	/************************************************************
	 * Calculates the accumulated branching entropy at each
	 * boundary in the corpus (going both forward & backward).
	 * @param c The corpus.
	 * @param f The forward trie of the corpus.
	 * @param b The backward trie of the corpus.
	 * @return Array of branching entropy at each boundary
	 * position in the corpus.
	 */
	public double[] getBranchingEntropy(Corpus c, Trie f, Trie b) {
		double[] H = new double[c.getCutPoints().length];
		for (int i = 0; i < H.length; i++) {
			H[i] = 0.;
		}
		
		for (int i = 1; i <= _maxLen; i++) {
			accumulateEntropy(H, i, c, f, b);
		}
		
		return H;
	}
	
	private double entropy(int m, int n, List<String> chars, Trie trie) {
		List<String> subList = chars.subList(m, n);
		return trie.getEntropy(subList);
	}
	
	private void accumulateEntropy(double[] H, int winLen,
			Corpus c, Trie fTrie, Trie bTrie) {
		List<String> fChars = c.getCleanChars();
		List<String> bChars = c.getReverseCorpus().getCleanChars();
		
		double fh, bh;
		int m = 0, n = m + winLen;
		while (n <= H.length) {
			fh = entropy(m,n,fChars,fTrie);
			bh = entropy(m,n,bChars,bTrie);
			H[n-1] += fh;
			H[H.length-n] += bh;
			
			m = m + 1;
			n = n + 1;
		}
	}
	/************************************************************/
	
	
	
	
	
	/************************************************************
	 * Algorithm 1. Generates an initial hypothesis by finding
	 * the optimal threshold for some set of boundary scores.
	 * The optimal threshold is found via a greedy search using
	 * the minimum description length as the evaluation
	 * method.
	 * @param c The corpus.
	 * @param scores The boundary scores, where higher scores
	 * constitute more likely boundaries.
	 * @return A segmentation based on the optimal threshold.
	 */
	public boolean[] algorithm1(Corpus c, double[] scores) {
		// Sort scores into thresholds
		assert(c.getCutPoints().length == scores.length);
		double[] thresholds = Arrays.copyOf(scores, scores.length);
		Arrays.sort(thresholds);
		
		// Seed initial hypothesis at median threshold
		int tempPos, pos = thresholds.length / 2;
		int step = thresholds.length / 4;
		int dir = 1; // ascending
		double tempDL, mdl = simulateSegmentation(c, scores, thresholds[pos]);
		
		// Binary search for better threshold
		while (step > 0) {
			tempPos = pos + dir*step;
			tempDL = simulateSegmentation(c, scores, thresholds[tempPos]);
			if (tempDL < mdl) {
				mdl = tempDL;
				pos = tempPos; 
				step /= 2;
				continue;
			}
			dir *= -1;
			tempPos = pos + dir*step;
			tempDL = simulateSegmentation(c, scores, thresholds[tempPos]);
			if (tempDL < mdl) {
				mdl = tempDL;
				pos = tempPos; 
				step /= 2;
				continue;
			}
			dir *= -1;
			step /= 2;
		}
		
		boolean[] initCuts = new boolean[scores.length];
		segmentByThreshold(initCuts, scores, thresholds[pos]);
		return initCuts;
	}
	
	private double simulateSegmentation(Corpus c, double[] scores, double threshold) {
		boolean[] cuts = new boolean[scores.length];
		segmentByThreshold(cuts, scores, threshold);
		return MDL.computeDescriptionLength(c, cuts);
	}
	
	private void segmentByThreshold(boolean[] cuts, double[] scores, double threshold) {
		for (int i = 0; i < scores.length; i++) {
			if (scores[i] > threshold)
				cuts[i] = true;
		}
	}
	/************************************************************/
	
	
	
	
	
	/************************************************************
	 * Algorithm 2. Compresses local token co-occurences.
	 * Greedily merge and/or split local instances of tokens to
	 * minimize the description length.
	 * @param c The corpus.
	 * @param initCuts The initial segmentation.
	 * @param scores The boundary scores (used to guide the
	 * search order), where higher scores constitute more likely
	 * boundaries.
	 * @return The resulting segmentation.
	 */
	public boolean[] algorithm2(Corpus c, boolean[] initCuts, double[] scores) {
		// Sort positions based on scores
		assert(initCuts.length == scores.length);
		Score[] path = new Score[scores.length];
		for (int i = 0; i < path.length; i++) {
			path[i] = new Score(i, scores[i]);
		}
		Arrays.sort(path);
		
		// DL of initial model
		boolean[] cuts = Arrays.copyOf(initCuts, initCuts.length);
		Model model = new Model(c, cuts);
		double mdl = model.getMDL();
		
		int pos, counter = 0;
		boolean change = false;
		double tempDL = 0;
		SimulationResult sim;
		List<String> leftToken, rightToken, longToken;
		while (true) {
			counter++;
			change = false;
			for (int i = path.length-1; i >= 0; i--) {
				pos = path[i].pos;
				if (model.cutPoints[pos] == false) {
					leftToken = model.getLeftToken(pos);
					rightToken = model.getRightToken(pos);
					longToken = new ArrayList<String>(leftToken);
					longToken.addAll(rightToken);
					sim = model.simulateLocalSplit(pos, leftToken, rightToken, longToken);
					tempDL = sim.getMDL();
					if (tempDL < mdl){
						change = true;
						model.acceptSimulationResult(sim);
						mdl = model.getMDL();
					}
				}
			}

			mdl = model.reComputeMDL();
			
			for (int i = 0; i < path.length; i++) {
				pos = path[i].pos;
				if (model.cutPoints[pos] == true) {
					leftToken = model.getLeftToken(pos);
					rightToken = model.getRightToken(pos);
					longToken = new ArrayList<String>(leftToken);
					longToken.addAll(rightToken);
					sim = model.simulateLocalMerge(pos, leftToken, rightToken, longToken);
					tempDL = sim.getMDL();
					if (tempDL < mdl){
						change = true;
						model.acceptSimulationResult(sim);
						mdl = model.getMDL();
					}
				}
			}

			mdl = model.reComputeMDL();
			
			// Exit if no change is evident in the model
			if (!change /*|| counter > 50*/) {
				break;
			}
		}
		
		model.integrityCheck();		// sanity check
		
		return model.cutPoints;
	}
	/************************************************************/
	
	
	
	
	
	/************************************************************
	 * Algorithm 3. A lexicon clean-up procedure.
	 * Try to improve the initial segmentation by globally
	 * splitting and merging different lexicon types to lower
	 * the description length. 
	 * @param c The corpus.
	 * @param initCuts The initial segmentation.
	 * @return The resulting segmentation.
	 */
	public boolean[] algorithm3(Corpus c, boolean[] initCuts) {
		// DL of initial model
		boolean[] cuts = Arrays.copyOf(initCuts, initCuts.length);
		Model model = new Model(c, cuts);
		double mdl = model.getMDL();
		
		// Get initial list of lexicon types
		int pos, step, dir, counter = 0;
		boolean change = false;
		double tempDL = 0;
		List<String> word;
		LexiconType[] types;
		SimulationResult sim;
		List<String> leftType, rightType, longType;
		while(true) {
			counter++;
			change = false;
			types = model.getLexiconTypesByCost();
			for (int i = 0; i < types.length; i++) {
				word = types[i].word;
				if (word.size() == 1)
					continue;
				pos = (word.size()-1) / 2;
				step = 1; dir = -1;
				// for pos = middle to both ends of word
				while (pos >= 0 && pos < word.size()-1) {
					longType = word;
					leftType = word.subList(0, pos+1);
					rightType = word.subList(pos+1, word.size());
					sim = model.simulateGlobalSplit(leftType, rightType, longType);
					tempDL = sim.getMDL();
					if (tempDL < mdl) {
						change = true;
						model.acceptSimulationResult(sim);
						mdl = model.getMDL();
						break;
					}
					pos += step * dir;
					step++; dir *= -1;
				}
			}

			mdl = model.reComputeMDL();
			
			if (change)
				types = model.getLexiconTypesByCost();
			
			for (int i = types.length-1; i >= 0; i--) {
				word = types[i].word;
				if (word.size() == 1)
					continue;
				pos = (word.size()-1) / 2;
				step = 1; dir = -1;
				// for pos = middle to both ends of word
				while (pos >= 0 && pos < word.size()-1) {
					longType = word;
					leftType = word.subList(0, pos+1);
					rightType = word.subList(pos+1, word.size());
					if (model.lexicon.containsKey(leftType) && model.lexicon.containsKey(rightType)) {
						sim = model.simulateGlobalMerge(leftType, rightType, longType);
						tempDL = sim.getMDL();
						if (tempDL < mdl) {
							change = true;
							model.acceptSimulationResult(sim);
							mdl = model.getMDL();
							break;
						}
					}
					pos += step * dir;
					step++; dir *= -1;
				}
			}

			mdl = model.reComputeMDL();
			
			// Exit if no change is evident in the model
			if (!change /*|| counter > 50*/) {
				break;
			}
		}

		model.integrityCheck();		// sanity check
		
		return model.cutPoints;
	}
	/************************************************************/
	
	
	
	
	
	public static class Score implements Comparable<Score> {
		public double score;
		public int pos;
		
		public Score(int pos, double score) {
			this.score = score;
			this.pos = pos;
		}
		
	    public int compareTo(Score s) {
	    	double diff = this.score - s.score;
	        if (diff > 0)
	        	return 1;
	        else if (diff < 0)
	        	return -1;
	        return 0;
	    }
	    
	    public String toString() {
	    	return NF.format(score);
	    }
	}
	
	public static class LexiconType implements Comparable<LexiconType> {
		public double cost;
		public List<String> word;
		
		public LexiconType(List<String> word, double cost) {
			this.cost = cost;
			this.word = word;
		}
		
	    public int compareTo(LexiconType t) {
	    	double diff = this.cost - t.cost;
	        if (diff > 0)
	        	return 1;
	        else if (diff < 0)
	        	return -1;
	        return 0;
	    }
	    
	    public String toString() {
	    	return word.toString() + "=" +NF.format(cost);
	    }
	}
	
	
	public static class SimulationResult {
		
		public SimulationType type;
		public List<String> leftToken, rightToken, longToken;
		public HashMap<List<String>,Integer> lexiconMod;
		public HashMap<String,Integer> lettersMod;
		public List<Integer> pos;
		public double[] T;
		public double accumulatedLetters;
		public double accumulatedWords;
		public double lexiconSize;
		public int ops;
		
		public enum SimulationType {
			LOCAL_SPLIT,
			LOCAL_MERGE,
			GLOBAL_SPLIT,
			GLOBAL_MERGE
		};
		
		public SimulationResult(SimulationType t, List<String> leftWord,
				List<String> rightWord, List<String> longWord) {
			type = t;
			leftToken = leftWord;
			rightToken = rightWord;
			longToken = longWord;
			lexiconMod = new HashMap<List<String>, Integer>();
			lettersMod = new HashMap<String, Integer>();
			pos = new ArrayList<Integer>();
			ops = 0;
		}
		
		public void loadInitialModel(Model initModel) {
			T = new double[5];
			T[0] = initModel.T[0];
			T[1] = initModel.T[1];
			T[2] = initModel.T[2];
			T[3] = initModel.T[3];
			T[4] = initModel.T[4];
			accumulatedLetters = initModel.accumulatedLetters;
			accumulatedWords = initModel.accumulatedWords;
			lexiconSize = initModel.lexicon.size();
		}
		
		public void incrementWord(List<String> word) {
			if (lexiconMod.containsKey(word))
				lexiconMod.put(word, lexiconMod.get(word)+1);
			else
				lexiconMod.put(word, 1);
		}
		
		public void decrementWord(List<String> word) {
			if (lexiconMod.containsKey(word))
				lexiconMod.put(word, lexiconMod.get(word)-1);
			else
				lexiconMod.put(word, -1);
		}
		
		public double getMDL() {
			return T[0] + T[1] + T[2] + T[3] + T[4];
		}
	}

	
	public static class Model {
		
		public Corpus corpus;
		public boolean[] cutPoints;
		public HashMap<List<String>, Integer> lexicon;
		public double accumulatedWords;
		public HashMap<String, Integer> letters;
		public double accumulatedLetters;
		public double[] T;
		
		public Model(Corpus c, boolean[] cuts) {
			this.corpus = c;
			this.cutPoints = Arrays.copyOf(cuts, cuts.length);
			this.lexicon = new HashMap<List<String>, Integer>();
			this.accumulatedWords = 0;
			this.letters = new HashMap<String, Integer>();
			this.accumulatedLetters = 0;
			
			// Construct lexicon and letters models
			List<List<String>> segments = corpus.getSegments(cutPoints);
			for (List<String> w : segments) {
				addWord(w);
			}
			
			// Calculate MDL components
			T = calculateMDL();
		}

		private double[] calculateMDL() {
			double T1 = 0;
			for (List<String> word : this.lexicon.keySet()) {
				T1 -= this.lexicon.get(word) * Stats.log(this.lexicon.get(word));
			}
			double T2 = this.accumulatedWords * Stats.log(this.accumulatedWords);
			double T3 = 0;
			for (String letter : this.letters.keySet()) {
				T3 -= this.letters.get(letter) * Stats.log(this.letters.get(letter));
			}
			double T4 = this.accumulatedLetters * Stats.log(this.accumulatedLetters);
			double T5 = ((this.lexicon.size() - 1.0) / 2.0) * Stats.log(this.accumulatedWords);
			return new double[] {T1, T2, T3, T4, T5};
		}
		
		public double getCalculatedMDL() {
			double[] temp = calculateMDL();
			return temp[0]+temp[1]+temp[2]+temp[3]+temp[4];
		}
		
		public double reComputeMDL() {
			T = calculateMDL();
			return getMDL();
		}
		
		public double getMDL() {
			return T[0] + T[1] + T[2] + T[3] + T[4];
		}

		public void addWord(List<String> word) {
			if (lexicon.containsKey(word)) {
				lexicon.put(word, lexicon.get(word) + 1);
			} else {
				lexicon.put(word, 1);
				this.accumulatedLetters += word.size();
				for (String letter : word) {
					if (this.letters.containsKey(letter)) {
						this.letters.put(letter, this.letters.get(letter) + 1);
					} else {
						this.letters.put(letter, 1);
					}
				}
			}		
			this.accumulatedWords += 1;
		}
		
		public void removeWord(List<String> word) {
			if (lexicon.containsKey(word)) {
				assert(lexicon.get(word) > 0);
				lexicon.put(word, lexicon.get(word) - 1);
				if (lexicon.get(word) == 0) {
					lexicon.remove(word);
					this.accumulatedLetters -= word.size();
					for (String letter : word) {
						letters.put(letter, letters.get(letter) - 1);
						if (letters.get(letter) == 0)
							letters.remove(letter);
					}
				}
			}
			this.accumulatedWords -= 1;
		}

		
		public List<String> getLeftToken(int pos) {
			int leftStart = pos;
			while (leftStart > 0) {
				leftStart -= 1;
				if (cutPoints[leftStart] == true) {
					leftStart += 1;
					break;
				}
			}
			return corpus.getCleanChars().subList(leftStart, pos+1);
		}
		
		public List<String> getRightToken(int pos) {
			int rightEnd = pos+1;
			while (rightEnd < cutPoints.length) {
				if (cutPoints[rightEnd] == true) {
					break;
				}
				rightEnd += 1;
			}
			return corpus.getCleanChars().subList(pos+1, rightEnd+1);
		}
		
		public void acceptSimulationResult(SimulationResult sim) {
			
			// For global split, we'll need to find all the positions where a split
			// since we've put it off during simulation is required.
			if (sim.type == SimulationType.GLOBAL_SPLIT) {
				assert(sim.pos.size() == 0);
				// Find all occurrences of longType
				List<Integer> indices = findInstancesInCorpus(sim.longToken);
				int n = sim.longToken.size();
				nextIndex:
				for (int i : indices) {
					// Make sure the sequence is bounded at both ends
					if ((i > 0 && !cutPoints[i-1]) || (i-1+n < cutPoints.length  && !cutPoints[i-1+n]))
						continue;
					
					// Make sure internal sequence is continuous
					for (int j = i; j < i-1+n; j++) {
						if (cutPoints[j])
							continue nextIndex;
					}
					
					// Finally found a valid long word, find the split point in that word.
					sim.pos.add(i+sim.leftToken.size()-1);
				}
			}
			
			assert(sim.ops == sim.pos.size());	// sanity check
			
			boolean boundaryType = (sim.type == SimulationType.GLOBAL_SPLIT || sim.type == SimulationType.LOCAL_SPLIT);
			for (int i = 0; i < sim.pos.size(); i++) {
				cutPoints[sim.pos.get(i)] = boundaryType;
			}
			
			// Modify lexicon
			int modCount;
			for (List<String> w : sim.lexiconMod.keySet()) {
				modCount = sim.lexiconMod.get(w);
				if (modCount > 0) {
					for (int i = 0; i < modCount; i++) {
						addWord(w);
					}
				} else if (modCount < 0) {
					for (int i = 0; i > modCount; i--) {
						removeWord(w);
					}
				}
			}
			
			// Accept new MDL components
			T[0] = sim.T[0]; T[1] = sim.T[1];
			T[2] = sim.T[2]; T[3] = sim.T[3];
			T[4] = sim.T[4];
		}
		
		public void simulateLexiconModification(SimulationResult sim) {
			
			// Update t1
			int wCount;
			for (List<String> w : sim.lexiconMod.keySet()) {
				sim.accumulatedWords += sim.lexiconMod.get(w);
				if (lexicon.containsKey(w)) {
					wCount = lexicon.get(w);
					sim.T[0] += (wCount * Stats.log(wCount));
					wCount += sim.lexiconMod.get(w);
					sim.T[0] -= (wCount * Stats.log(wCount));
					assert(wCount >= 0);
					if (wCount == 0) {
						// Simulate removing a word from the lexicon
						sim.lexiconSize -= 1;
						for (String letter : w) {
							if (sim.lettersMod.containsKey(letter)) {
								sim.lettersMod.put(letter, sim.lettersMod.get(letter)-1);
							} else {
								sim.lettersMod.put(letter, -1);
							}
						}
					}
				} else {
					wCount = sim.lexiconMod.get(w);
					assert(wCount > 0);
					sim.T[0] -= (wCount * Stats.log(wCount));
					
					// Simulate adding a word to the lexicon
					sim.lexiconSize += 1;
					for (String letter : w) {
						if (sim.lettersMod.containsKey(letter)) {
							sim.lettersMod.put(letter, sim.lettersMod.get(letter)+1);
						} else {
							sim.lettersMod.put(letter, 1);
						}
					}
				}
			}
			
			// Update t2
			sim.T[1] = sim.accumulatedWords * Stats.log(sim.accumulatedWords);
			
			// Update t3
			int lCount;
			for (String l : sim.lettersMod.keySet()) {
				sim.accumulatedLetters += sim.lettersMod.get(l);
				if (letters.containsKey(l)) {
					lCount = letters.get(l);
					sim.T[2] += (lCount * Stats.log(lCount));
				} else {
					lCount = 0;
				}
				lCount += sim.lettersMod.get(l);
				sim.T[2] -= (lCount * Stats.log(lCount));
				assert(lCount >= 0);
			}
			
			// Update t4
			sim.T[3] = sim.accumulatedLetters * Stats.log(sim.accumulatedLetters);

			// Update t5
			sim.T[4] = ((sim.lexiconSize - 1.0) / 2.0) * Stats.log(sim.accumulatedWords);
		}

		public SimulationResult simulateLocalSplit(int pos, List<String> leftToken,
				List<String> rightToken,List<String> longToken) {
			assert(leftToken != longToken && rightToken != longToken);
			
			// Create new simulation
			SimulationResult sim = new SimulationResult(SimulationType.LOCAL_SPLIT,
					leftToken, rightToken, longToken);
			sim.loadInitialModel(this);
			
			// Decrement long token
			sim.decrementWord(longToken);
			
			// Increment left & right tokens
			sim.incrementWord(leftToken);
			sim.incrementWord(rightToken);
			
			// Add split possition
			sim.pos.add(pos);
			sim.ops++;			// track of number of operations for sanity check
			
			simulateLexiconModification(sim);
			
			return sim;
		}
		
		public SimulationResult simulateLocalMerge(int pos, List<String> leftToken,
				List<String> rightToken, List<String> longToken) {
			assert(leftToken != longToken && rightToken != longToken);

			// Create new simulation
			SimulationResult sim = new SimulationResult(SimulationType.LOCAL_MERGE,
					leftToken, rightToken, longToken);
			sim.loadInitialModel(this);
			
			// Increment long token
			sim.incrementWord(longToken);
			
			// Decrement left & right tokens
			sim.decrementWord(leftToken);
			sim.decrementWord(rightToken);

			// Add merge possition
			sim.pos.add(pos);
			sim.ops++;				// track of number of operations for sanity check
			
			simulateLexiconModification(sim);
			
			return sim;
		}
		
		public SimulationResult simulateGlobalSplit(List<String> leftType,
				List<String> rightType, List<String> longType) {
			assert(leftType != longType && rightType != longType && longType.size() > 1);

			// Create new simulation
			SimulationResult sim = new SimulationResult(SimulationType.GLOBAL_SPLIT,
					leftType, rightType, longType);
			sim.loadInitialModel(this);
			
			// Remove all occurences of longType
			int longCount = this.lexicon.get(longType);
			for (int i = 0; i < longCount; i++) {
				// Decrement counts of long type
				sim.decrementWord(longType);

				// Increment counts of left & right types
				sim.incrementWord(leftType);
				sim.incrementWord(rightType);
				
				sim.ops++;
			}
			
			simulateLexiconModification(sim);
			
			return sim;
		}

		public SimulationResult simulateGlobalMerge(List<String> leftType,
				List<String> rightType, List<String> longType) {
			assert(leftType != longType && rightType != longType && longType.size() > 1);

			// Create new simulation
			SimulationResult sim = new SimulationResult(SimulationType.GLOBAL_MERGE,
					leftType, rightType, longType);
			sim.loadInitialModel(this);
			
			// Find all occurrences of longType
			List<Integer> indices = findInstancesInCorpus(longType);
			
			int split, n = longType.size(), prevTokenIndx = Integer.MIN_VALUE;
			Collections.sort(indices);
			Iterator<Integer> iterator = indices.iterator();
			nextIndex:
			while (iterator.hasNext()) {
				int i = iterator.next();
				
				// If we just performed a merge then the next merge should not use
				// any part of the previously merged word.
				// Example: Merging "|id|id|" into "|idid|" and running into
				// 			a corpus with "|id|id|id|id|"
				if (i >= prevTokenIndx && i < prevTokenIndx + n)
					continue;
				
				// Make sure the sequence is bounded on both ends
				if ((i > 0 && !cutPoints[i-1]) || (i-1+n < cutPoints.length  && !cutPoints[i-1+n]))
					continue;
				
				// Make sure the internal of the sequence is continuous except for exactly
				// 1 boundary separating the left & right tokens
				split = i+leftType.size()-1;
				for (int j = i; j < i-1+n; j++) {
					if ((j == split && !cutPoints[j]) || (j != split && cutPoints[j]))
						continue nextIndex;
				}
				
				prevTokenIndx = i;
				
				// Found valid left & right words, add boundary to remove
				sim.pos.add(split);
				sim.ops++;

				// Increment count of long type
				sim.incrementWord(longType);
				
				// Decrement count left & right types
				sim.decrementWord(leftType);
				sim.decrementWord(rightType);
			}
			
			// Sanity checks
			if (!sim.lexiconMod.containsKey(longType))
				assert(sim.ops == 0);
			else
				assert(sim.ops == sim.lexiconMod.get(longType));
			
			simulateLexiconModification(sim);
			
			return sim;
		}
		
		public LexiconType[] getLexiconTypesByCost() {
			LexiconType[] types = new LexiconType[lexicon.keySet().size()];
			int i = 0;
			double cost = 0;
			for (List<String> w : lexicon.keySet()) {
				cost = 0;
				for (String l : w) {
					cost -= Stats.log( ((double)letters.get(l)) / accumulatedLetters );
				}
				types[i++] = new LexiconType(w, cost);
			}
			Arrays.sort(types);
			return types;
		}
		
		public List<Integer> findInstancesInCorpus(List<String> token) {
			List<Integer> indx = new ArrayList<Integer>();
			List<String> chars = corpus.getCleanChars();
			int max = chars.size() - token.size();
			int j, k, n;
			test:
			for (int i = 0; i <= max; i++) {
				n = token.size();
				j = i; k = 0;
				while (n-- > 0) {
					if (!chars.get(j++).equals(token.get(k++))) {
						continue test;
					}
				}
				indx.add(i);
			}
			return indx;
		}

		public void integrityCheck() {
			HashMap<List<String>,Integer> lex = new HashMap<List<String>, Integer>();
			HashMap<String,Integer> let = new HashMap<String, Integer>();;
			double accumWords = 0, accumLets = 0;
			
			List<List<String>> segments = corpus.getSegments(cutPoints);
			
			for (List<String> word : segments) {
				if (lex.containsKey(word)) {
					lex.put(word, lex.get(word) + 1);
				} else {
					lex.put(word, 1);
					accumLets += word.size();
					for (String letter : word) {
						if (let.containsKey(letter)) {
							let.put(letter, let.get(letter) + 1);
						} else {
							let.put(letter, 1);
						}
					}
				}		
				accumWords += 1;
			}
			
			assert((int)accumWords == (int)this.accumulatedWords);
			assert((int)accumLets == (int)this.accumulatedLetters);
			assert(lex.keySet().size() == lexicon.keySet().size());
			assert(let.keySet().size() == letters.keySet().size());
			for (List<String> w : lex.keySet()) {
				assert(lex.get(w).equals(lexicon.get(w)));
			}
			for (String c : let.keySet()) {
				assert(let.get(c).equals(letters.get(c)));
			}
		}
		
	}
	
	
	
	


	public static void runExperiment(Corpus c) {
		EntropyMDL tpbe;
		EvaluationResults eval1, eval2, eval3;
		double bestMDL = Double.MAX_VALUE, bestF = Double.MIN_VALUE;
		int bestMDLWin = -1, bestFWin = -1;

        String wd = System.getProperty("user.dir");
		String path = wd + "/" + "results/" + Corpus.getFolder(c.getType())
			+ "/EntMDL-" + c.getName() + ".txt";
		
		try {
			
			BufferedWriter out = new BufferedWriter(new FileWriter(path));
			
			for (int maxLen = 3; maxLen <= 8; maxLen++) {

				System.out.println("\nWIN LENGTH: " + maxLen);
				
				tpbe = new EntropyMDL(c, maxLen);
				tpbe.runAlgorithm();
				
				out.write("***************** WinLen: " + maxLen + " *****************\n\n");
				eval1 = Evaluator.evaluate(tpbe.getCutPoints1(), c.getCutPoints());
				out.write("- Algorithm 1 -\n");
				out.write("MDL: " + tpbe.getMDL1() + "\n");
				out.write("B-F: " + eval1.boundaryF1() + "\n");
			    out.write(NF.format(eval1.boundaryPrecision) + "\t"
			    		+ NF.format(eval1.boundaryRecall) + "\t"
			    		+ NF.format(eval1.boundaryF1()) + "\t");
			    out.write(NF.format(eval1.chunkPrecision) + "\t"
			    		+ NF.format(eval1.chunkRecall) + "\t"
			    		+ NF.format(eval1.chunkF1()) + "\n");
			    out.write("\n");

				eval2 = Evaluator.evaluate(tpbe.getCutPoints2(), c.getCutPoints());
				out.write("- Algorithm 2 -\n");
				out.write("MDL: " + tpbe.getMDL2() + "\n");
				out.write("B-F: " + eval2.boundaryF1() + "\n");
			    out.write(NF.format(eval2.boundaryPrecision) + "\t"
			    		+ NF.format(eval2.boundaryRecall) + "\t"
			    		+ NF.format(eval2.boundaryF1()) + "\t");
			    out.write(NF.format(eval2.chunkPrecision) + "\t"
			    		+ NF.format(eval2.chunkRecall) + "\t"
			    		+ NF.format(eval2.chunkF1()) + "\n");
			    out.write("\n");

				eval3 = Evaluator.evaluate(tpbe.getCutPoints3(), c.getCutPoints());
				out.write("- Algorithm 3 -\n");
				out.write("MDL: " + tpbe.getMDL3() + "\n");
				out.write("B-F: " + eval3.boundaryF1() + "\n");
			    out.write(NF.format(eval3.boundaryPrecision) + "\t"
			    		+ NF.format(eval3.boundaryRecall) + "\t"
			    		+ NF.format(eval3.boundaryF1()) + "\t");
			    out.write(NF.format(eval3.chunkPrecision) + "\t"
			    		+ NF.format(eval3.chunkRecall) + "\t"
			    		+ NF.format(eval3.chunkF1()) + "\n");
			    out.write("\n\n\n");
			    
			    if (tpbe.getMDL3() < bestMDL) {
			    	bestMDL = tpbe.getMDL3();
			    	bestMDLWin = maxLen;
			    }

			    if (eval3.boundaryF1() > bestF) {
			    	bestF = eval3.boundaryF1();
			    	bestFWin = maxLen;
			    }
			}
			
			out.write("****************** SUMMARY ******************\n\n");
			out.write("- Best BF -\n");
			out.write("Win: " + bestFWin + "\n");
			out.write("B-F: " + bestF + "\n\n");
			out.write("- Best MDL -\n");
			out.write("Win: " + bestMDLWin + "\n");
			out.write("MDL: " + bestMDL + "\n\n");
			
			out.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	

	public static void main(String[] args) {
		List<Corpus> corpora = new ArrayList<Corpus>();
		
		corpora.add(Corpus.autoLoad("br87", CorpusType.LETTER, true));
//		corpora.add(Corpus.autoLoad("bloom73", CorpusType.LETTER, true));
//		corpora.add(Corpus.autoLoad("chinese-gw", CorpusType.LETTER, true));
//		corpora.add(Corpus.autoLoad("thai-novel-short", CorpusType.LETTER, true));
		
//		corpora.add(Corpus.autoLoad("caesar", CorpusType.LETTER, false));
//		corpora.add(Corpus.autoLoad("gray", CorpusType.LETTER, false));
//		corpora.add(Corpus.autoLoad("switchboard", CorpusType.LETTER, false));
//		corpora.add(Corpus.autoLoad("orwell-short", CorpusType.LETTER, false));
//		corpora.add(Corpus.autoLoad("twain", CorpusType.LETTER, false));
//		corpora.add(Corpus.autoLoad("zarathustra", CorpusType.LETTER, false));
		
		for (Corpus c : corpora) {
			System.out.println("\nCORPUS: " + c.getName());
			EntropyMDL.runExperiment(c);
		}
	}

}
