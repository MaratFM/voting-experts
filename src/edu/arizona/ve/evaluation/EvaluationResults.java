package edu.arizona.ve.evaluation;

import java.io.PrintStream;

import edu.arizona.ve.util.NF;

public class EvaluationResults {
	public int actualChunkCount;
	public int estimatedChunkCount;
	
	public double boundaryPrecision;
	public double boundaryRecall;
	public double chunkPrecision;
	public double chunkRecall;
	
	public double boundaryF1() {
		return F1(boundaryPrecision, boundaryRecall);
	}
	
	public double chunkF1() {
		return F1(chunkPrecision, chunkRecall);
	}
	
	public double F1(double precision, double recall) {
		return 2 * ((precision * recall) / (precision + recall));
	}
	
   public void printResults() {
	   printResults(System.out);
   }
   
   public void printResults(PrintStream out) {
	   out.println("BP\tBR\tBF\tWP\tWR\tWF");
	   out.print(NF.format(boundaryPrecision) + "\t" + NF.format(boundaryRecall) + "\t" + NF.format(boundaryF1()) + "\t");
	   out.print(NF.format(chunkPrecision) + "\t" + NF.format(chunkRecall) + "\t" + NF.format(chunkF1()));
	   out.println();
	   out.println("LATEX:");
	   out.println(NF.format(boundaryPrecision) + " & " + NF.format(boundaryRecall) + " & " + NF.format(boundaryF1()) + 
			   	   " & " + NF.format(chunkPrecision) + " & " + NF.format(chunkRecall) + " & " + NF.format(chunkF1()) + " \\\\ ");
	   out.println();
   }
}
