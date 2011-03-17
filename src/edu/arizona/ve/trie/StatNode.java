/*
 * StatNode.java
 *
 * Created on November 11, 2005, 11:48 AM
 */
package edu.arizona.ve.trie;

import java.util.ArrayList;

import edu.arizona.ve.util.NF;
import edu.arizona.ve.util.Printer;

/**
 *
 * @author  Wesley Kerr
 */
public class StatNode {
   
   public ArrayList<Double> frequencies;
   public ArrayList<Double> surprisals;
   public ArrayList<Double> boundaryEntropies;
   
   public double n;

   public double meanFreq;
   public double varFreq;
   public double stdDevFreq;
   
   public double meanIntEnt;
   public double varIntEnt;
   public double stdDevIntEnt;
   
   public double meanEnt;
   public double varEnt;
   public double stdDevEnt;

   /** Creates a new instance of StatNode */
   public StatNode() {
      frequencies = new ArrayList<Double>();
      boundaryEntropies = new ArrayList<Double>();
      surprisals = new ArrayList<Double>();
   }
   
   public void calculate() {
      n = frequencies.size();
      findMean();
      findVar();
   }
   
   protected void findMean() {
      meanFreq = 0;
      meanEnt = 0;
      meanIntEnt = 0;
      
      for (int i = 0; i < n; ++i) {
         meanFreq += ((Double) frequencies.get(i)).doubleValue();
         meanIntEnt += surprisals.get(i).doubleValue();
         meanEnt += ((Double) boundaryEntropies.get(i)).doubleValue();
      }
      
      meanIntEnt /= n;
      meanFreq /= n;
      meanEnt /= n;
   }
   
   protected void findVar() {
      varFreq = 0;
      varEnt = 0;
      varIntEnt = 0;
      
      for (int i = 0; i < n; ++i) {
         double freq = ((Double) frequencies.get(i)).doubleValue();
         varFreq += (freq - meanFreq)*(freq - meanFreq);
         
         double intEnt = surprisals.get(i).doubleValue();
         varIntEnt += Math.pow(intEnt - meanIntEnt, 2);
         
         double ent  = ((Double) boundaryEntropies.get(i)).doubleValue();
         varEnt += (ent - meanEnt)*(ent - meanEnt);
      }
      varFreq /= n;
      varIntEnt /= n;
      varEnt /= n;
      
      stdDevFreq = Math.sqrt(varFreq);
      stdDevIntEnt = Math.sqrt(varIntEnt);
      stdDevEnt = Math.sqrt(varEnt);
   }
   
   public void print(int depth) {
      System.out.print("length: " + Printer.pad(depth+"", 3) + " ");
      System.out.print("number: " + Printer.pad(n+"", 8) + " ");
      System.out.print("freq [" + Printer.pad(NF.format(meanFreq),11)   + "," + 
                                  Printer.pad(NF.format(varFreq),18)    + "," + 
                                  Printer.pad(NF.format(stdDevFreq),11) + "] ");
      System.out.print("int ent [" + Printer.pad(NF.format(meanIntEnt),5)   + "," + 
              Printer.pad(NF.format(varIntEnt),5)    + "," + 
              Printer.pad(NF.format(stdDevIntEnt),5) + "] ");

      System.out.print("ent [" + Printer.pad(NF.format(meanEnt),5)   + "," + 
                                 Printer.pad(NF.format(varEnt),5)    + "," + 
                                 Printer.pad(NF.format(stdDevEnt),5) + "] ");
      System.out.println();
   }
}
