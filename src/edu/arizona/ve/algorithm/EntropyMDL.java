package edu.arizona.ve.algorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import edu.arizona.ve.algorithm.EntropyMDL.SimulationResult.SimulationType;
import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.corpus.Corpus.CorpusType;
import edu.arizona.ve.evaluation.Evaluator;
import edu.arizona.ve.mdl.MDL;
import edu.arizona.ve.trie.Trie;
import edu.arizona.ve.util.NF;
import edu.arizona.ve.util.Stats;

/**
*
* @author  Anh Tran
* Unsupervised Word Segmentation algorithm using Branching Entropy & MDL
* by Zhikov, Takamua, and Okumura (2010).
*/
public class EntropyMDL {

	Trie _forwardTrie, _backwardTrie;
	Corpus _corpus;
	int _maxLen;
	BranchEntropy[] _branchEntropy;
	boolean[] _initialCutpoints;
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
	
	
	public void runAlgorithm() {
		// Calculate the branching entropies
		_branchEntropy = calcBranchingEntropy(_corpus, _forwardTrie, _backwardTrie);
		
		// Generate initial hypothesis
		_initialCutpoints = new boolean[_branchEntropy.length];
		double mdl = algorithm1(_corpus, _branchEntropy, _initialCutpoints);

		Evaluator.evaluate(_initialCutpoints, _corpus.getCutPoints()).printResults();
		System.out.println(mdl);
		System.out.println(Arrays.toString(Arrays.copyOf(_branchEntropy, 100)));
		System.out.println(_corpus.getCleanChars().subList(0, 100));
		System.out.println(Arrays.toString(Arrays.copyOf(_initialCutpoints, 100)));

		// Compress local token co-occurences
		_model = new Model(_corpus, _initialCutpoints);
		mdl = algorithm2(_model, _branchEntropy);

		Evaluator.evaluate(_model.cutPoints, _corpus.getCutPoints()).printResults();
		System.out.println(mdl);
		System.out.println(_corpus.getCleanChars().subList(0, 100));
		System.out.println(Arrays.toString(Arrays.copyOf(_model.cutPoints, 100)));
	}
	
	
	
	
	
	/************************************************************
	 * Algorithm 3. A lexicon clean-up procedure
	 */
	private double algorithm3(Model model) {
		
		return 0;
	}
	
	
	
	
	
	/************************************************************
	 * Algorithm 2. Compresses local token co-occurences
	 */
	private double algorithm2(Model model, BranchEntropy[] H) {		
		// Sort positions on entropy
		BranchEntropy[] path = Arrays.copyOf(H, H.length);
		Arrays.sort(path);
		
		// DL of initial model
		double mdl = model.getMDL();
		
		int pos, counter = 0;
		boolean change = false;
		double tempDL = 0;
		List<String> leftToken, rightToken, longToken;
		SimulationResult sim;
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
						
//						System.out.println(mdl);
//						System.out.println(model.calculateMDL());
//						System.out.println(MDL.computeDescriptionLength(model.corpus, model.cutPoints));
					}
				}
			}
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
						
//						System.out.println(mdl);
//						System.out.println(model.calculateMDL());
//						System.out.println(MDL.computeDescriptionLength(model.corpus, model.cutPoints));
					}
				}
			}
			
			// Exit if no change is evident in the model
			if (!change /*|| counter > 100*/) {
				break;
			}
			
			System.out.println(mdl);
		}
		
		System.out.println(counter);
		
		return mdl;
	}
	
	
	
	
	
	/************************************************************
	 * Algorithm 1. Generates initial hypothesis
	 */
	private double algorithm1(Corpus c, BranchEntropy[] H, boolean[] cuts) {
		// Sort branching entropies into thresholds
		BranchEntropy[] thresholds = Arrays.copyOf(H, H.length);
		Arrays.sort(thresholds);
		
		// Seed initial hypothesis at median
		int tempPos, pos = thresholds.length / 2;
		int step = thresholds.length / 4;
		int dir = 1; // ascending
		double tempDL, mdl = simulateSegmentation(c, H, thresholds[pos].h);
		
		// Binary search for better threshold
		while (step > 0) {
			tempPos = pos + dir*step;
			tempDL = simulateSegmentation(c, H, thresholds[tempPos].h);
			if (tempDL < mdl) {
				mdl = tempDL;
				pos = tempPos; 
				step /= 2;
				continue;
			}
			dir *= -1;
			tempPos = pos + dir*step;
			tempDL = simulateSegmentation(c, H, thresholds[tempPos].h);
			if (tempDL < mdl) {
				mdl = tempDL;
				pos = tempPos; 
				step /= 2;
				continue;
			}
			dir *= -1;
			step /= 2;
		}
		
		segmentByThreshold(cuts, H, thresholds[pos].h);
		return MDL.computeDescriptionLength(c, cuts);
	}
	
	private double simulateSegmentation(Corpus c, BranchEntropy[] H, double threshold) {
		boolean[] cuts = new boolean[H.length];
		segmentByThreshold(cuts, H, threshold);
		return MDL.computeDescriptionLength(c, cuts);
	}
	
	private void segmentByThreshold(boolean[] cuts, BranchEntropy[] H, double threshold) {
		for (int i = 0; i < H.length; i++) {
			if (H[i].h > threshold)
				cuts[H[i].pos] = true;
		}
	}
	
	
	
	
	

	/************************************************************
	 * Pre-processing: Calculates the branching entropy.
	 */
	private BranchEntropy[] calcBranchingEntropy(Corpus c, Trie f, Trie b) {
		BranchEntropy[] H = new BranchEntropy[c.getCutPoints().length];
		for (int i = 0; i < H.length; i++)
			H[i] = new BranchEntropy(0., i);
		
		for (int i = 1; i <= _maxLen; i++) {
			accumulateEntropies(H, i, c, f, b);
		}
		
		return H;
	}
	
	private double entropy(int m, int n, List<String> chars, Trie trie) {
		List<String> subList = chars.subList(m, n);
		return trie.getEntropy(subList);
	}
	
	private void accumulateEntropies(BranchEntropy[] H, int winLen,
			Corpus c, Trie fTrie, Trie bTrie) {
		List<String> fChars = c.getCleanChars();
		List<String> bChars = c.getReverseCorpus().getCleanChars();
		
		double fh, bh;
		int m = 0, n = m + winLen;
		while (n <= H.length) {
			fh = entropy(m,n,fChars,fTrie);
			bh = entropy(m,n,bChars,bTrie);
			H[n-1].h += fh;
			H[H.length-n].h += bh;
			
			m = m + 1;
			n = n + 1;
		}
	}
	
	
	
	public static void main(String[] args) {
//		Corpus c = Corpus.autoLoad("latin-morph", "case");
//		Corpus c = Corpus.autoLoad("latin", "word");
//		Corpus c = Corpus.autoLoad("caesar", "nocase");
//		Corpus c = Corpus.autoLoad("orwell-short", CorpusType.LETTER, false);
		Corpus c = Corpus.autoLoad("br87", CorpusType.LETTER, true);
		int maxLen = 3;
		
		EntropyMDL tpbe = new EntropyMDL(c, maxLen);
		tpbe.runAlgorithm();
	}
	
	
	public static class BranchEntropy implements Comparable<BranchEntropy> {
		public double h;
		public int pos;
		
		public BranchEntropy(double entropy, int pos) {
			this.h = entropy;
			this.pos = pos;
		}
		
	    public int compareTo(BranchEntropy be) {
	    	double diff = this.h - be.h;
	        if (diff > 0)
	        	return 1;
	        else if (diff < 0)
	        	return -1;
	        return 0;
	    }
	    
	    public String toString() {
	    	return NF.format(h);
	    }
	}
	
	
	public static class SimulationResult {
		
		public SimulationType type;
		public HashMap<List<String>,Integer> lexiconMod;
		HashMap<String,Integer> lettersMod;
		public List<Integer> pos;
		public double[] T;
		public int accumulatedLetters;
		public int accumulatedWords;
		public int lexiconSize;
		public int ops;
		
		public enum SimulationType {
			LOCAL_SPLIT,
			LOCAL_MERGE,
			GLOBAL_SPLIT,
			GLOBAL_MERGE
		};
		
		public SimulationResult(SimulationType t, Model initModel) {
			type = t;
			lexiconMod = new HashMap<List<String>, Integer>();
			lettersMod = new HashMap<String, Integer>();
			pos = new ArrayList<Integer>();
			T = new double[5];
			T[0] = initModel.T[0];
			T[1] = initModel.T[1];
			T[2] = initModel.T[2];
			T[3] = initModel.T[3];
			T[4] = initModel.T[4];
			accumulatedLetters = initModel.accumulatedLetters;
			accumulatedWords = initModel.accumulatedWords;
			lexiconSize = initModel.lexicon.size();
			ops = 0;
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
		public int accumulatedWords;
		public HashMap<String, Integer> letters;
		public int accumulatedLetters;
		public double[] T;
		
		public Model(Corpus c, boolean[] cuts) {
			this.corpus = c;
			this.cutPoints = cuts;
			this.lexicon = new HashMap<List<String>, Integer>();
			this.accumulatedWords = 0;
			this.letters = new HashMap<String, Integer>();
			this.accumulatedLetters = 0;
			
			// Construct lexicon and letters models
			List<List<String>> segments = c.getSegments(cuts);
			for (List<String> w : segments) {
				addWord(w);
			}
			
			// Calculate MDL components
			T = new double[5];
			calculateMDL();
		}
		
		public double calculateMDL() {
			this.T[0] = 0;
			for (List<String> word : this.lexicon.keySet()) {
				this.T[0] -= this.lexicon.get(word) * Stats.log(this.lexicon.get(word));
			}
			this.T[1] = this.accumulatedWords * Stats.log(this.accumulatedWords);
			this.T[2] = 0;
			for (String letter : this.letters.keySet()) {
				this.T[2] -= this.letters.get(letter) * Stats.log(this.letters.get(letter));
			}
			this.T[3] = this.accumulatedLetters * Stats.log(this.accumulatedLetters);
			this.T[4] = ((this.lexicon.size() - 1.0) / 2.0) * Stats.log(this.accumulatedWords);
			return getMDL();
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
		
		public void simulateLexiconModification(SimulationResult sim) {
			
			// Update t1
			int wCount;
			for (List<String> w : sim.lexiconMod.keySet()) {
				sim.accumulatedWords += sim.lexiconMod.get(w);
				if (lexicon.containsKey(w)) {
					wCount = lexicon.get(w);
					sim.T[0] += wCount * Stats.log(wCount);
					wCount += sim.lexiconMod.get(w);
					sim.T[0] -= wCount * Stats.log(wCount);
					assert(wCount >= 0);
					if (wCount == 0) {
						// Simulate removing a word from the lexicon
						sim.lexiconSize--;
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
					sim.T[0] -= wCount * Stats.log(wCount);
					
					// Simulate adding a word to the lexicon
					sim.lexiconSize++;
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
					sim.T[2] += lCount * Stats.log(lCount);
				} else {
					lCount = 0;
				}
				lCount += sim.lettersMod.get(l);
				sim.T[2] -= lCount * Stats.log(lCount);
				assert(lCount >= 0);
			}
			
			// Update t4
			sim.T[3] = sim.accumulatedLetters * Stats.log(sim.accumulatedLetters);

			// Update t5
			sim.T[4] = 0.5 * (sim.lexiconSize - 1) * Stats.log(sim.accumulatedWords);
		}

		public SimulationResult simulateLocalSplit(int pos, List<String> leftToken,
				List<String> rightToken,List<String> longToken) {
			assert(leftToken != longToken && rightToken != longToken);
			
			// Create new simulation
			SimulationResult sim = new SimulationResult(SimulationType.LOCAL_SPLIT, this);
			
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
			SimulationResult sim = new SimulationResult(SimulationType.LOCAL_MERGE, this);
			
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
		
		public void acceptSimulationResult(SimulationResult sim) {
			
			if (sim.type == SimulationType.GLOBAL_SPLIT) {
				// Need to actually find the split points on global split
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
			T[0] = sim.T[0]; T[1] = sim.T[1]; T[2] = sim.T[2]; T[3] = sim.T[3]; T[4] = sim.T[4];
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
		
		public double getMDL() {
			return T[0] + T[1] + T[2] + T[3] + T[4];
		}
	}
}
