package edu.arizona.ve.algorithm.optimize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import edu.arizona.ve.algorithm.optimize.SimulationResult.SimulationType;
import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.util.Stats;

public class Model {
	
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