package edu.arizona.ve.algorithm.optimize;

import java.util.List;

import edu.arizona.ve.util.NF;

public class LexiconType implements Comparable<LexiconType> {
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