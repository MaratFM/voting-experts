package edu.arizona.ve.algorithm.optimize;

import java.util.Arrays;
import java.util.List;

import edu.arizona.ve.corpus.Corpus;

public class GlobalOptimizer {
	
	/************************************************************
	 * Algorithm 3 from (Zhikov et al., 2010). A lexicon clean-up procedure.
	 * Try to improve the initial segmentation by globally
	 * splitting and merging different lexicon types to lower
	 * the description length. 
	 * @param c The corpus.
	 * @param initCuts The initial segmentation.
	 * @return The resulting segmentation.
	 */
	public static boolean[] optimize(Corpus c, boolean[] initCuts) {
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
}
