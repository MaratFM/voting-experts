package edu.arizona.ve.api;



public class Segmentation implements Comparable<Segmentation> {
	public enum Direction { Forward, Backward, BiDirectional };

	public int windowSize;
	public int threshold;
	public boolean localMax = true;
	public boolean[] cutPoints;
	public double descriptionLength;
	public Direction direction = Direction.Forward;
	public int[] votes = null;
	
	public Segmentation() {}
	
	public Segmentation(int window, int threshold) {
		this.windowSize = window;
		this.threshold = threshold;
	}
	
	public int compareTo(Segmentation other) {
		if (descriptionLength == other.descriptionLength) {
			return 0;
		} else if (descriptionLength < other.descriptionLength) {
			return -1;
		} else {
			return 1;
		}
	}
	
	@Override
	public String toString() {
		return windowSize + "," + threshold + "," + localMax;
	}
}