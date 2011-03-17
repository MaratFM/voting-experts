package edu.arizona.ve.algorithm.optimize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.arizona.ve.corpus.Corpus;

public class LocalOptimizer {
	
	/************************************************************
	 * Algorithm 2 from (Zhikov et al., 2010). Compresses local token co-occurences.
	 * Greedily merge and/or split local instances of tokens to
	 * minimize the description length.
	 * @param c The corpus.
	 * @param initCuts The initial segmentation.
	 * @param scores The boundary scores (used to guide the
	 * search order), where higher scores constitute more likely
	 * boundaries.
	 * @return The resulting segmentation.
	 */
	public static boolean[] optimize(Corpus c, boolean[] initCuts, double[] scores) {
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
	
}
