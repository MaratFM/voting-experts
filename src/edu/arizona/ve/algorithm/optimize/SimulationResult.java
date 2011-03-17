package edu.arizona.ve.algorithm.optimize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SimulationResult {
	
	public SimulationResult.SimulationType type;
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
	
	public SimulationResult(SimulationResult.SimulationType t, List<String> leftWord,
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