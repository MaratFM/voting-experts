package edu.arizona.ve.algorithm.incremental;

public interface IncrementalAlgorithm {
	
	public String segment(String utterance);
	
	public void commit(String segmented);
	
}
