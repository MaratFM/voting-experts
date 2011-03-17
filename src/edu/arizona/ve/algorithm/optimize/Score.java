package edu.arizona.ve.algorithm.optimize;

import edu.arizona.ve.util.NF;

public class Score implements Comparable<Score> {
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