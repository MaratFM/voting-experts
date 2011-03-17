package edu.arizona.ve.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.Collection;

import edu.arizona.ve.corpus.Corpus;
/**
*
* @author  Daniel Hewlett
*/
public class Utils {
	public static List<String> toCharList(String s) {
		ArrayList<String> result = new ArrayList<String>();
		for (Character c : s.toCharArray()) {
			result.add(c.toString());
		}
		return result;
	}
	
	public static List<String> toStringList(List<List<String>> segments) {
		Vector<String> result = new Vector<String>();
		
		for (List<String> s : segments) {
			String currWord = new String();
			for (@SuppressWarnings("unused") String letter : s) {
				currWord += s;
			}
			
			result.add(currWord);
		}
		
		return result;
	}
	
	public static String fromList(List<String> list) {
		StringBuffer result = new StringBuffer();
		
		for (String s : list) {
			result.append(s);
		}
		
		return result.toString();
	}

	public static boolean[] makeArray(List<Boolean> list) {
		boolean[] b = new boolean[list.size()];
		for (int i = 0; i < list.size(); i++) 
			b[i] = list.get(i);
		return b;
	}
	
	public static String getCutString(List<String> corpus, boolean[] cutPoints, int length) {
		String result = new String();
		
		for (int i = 0; i < length; i++) {
			result += corpus.get(i);
			if (i < cutPoints.length) {
				if (cutPoints[i]) {
					result += "|";
				} else {
					result += "-";
				}
			}
		}
		
		return result;
	}
	
	public static Corpus makeWords(List<String> knowledgeCorpus) {
        Corpus c = new Corpus();
        StringBuffer sb = new StringBuffer();
        for(String letter : knowledgeCorpus){
            if(letter.equals("*")){
                c.getCleanChars().add(sb.toString());
                c.getSegmentedChars().add(sb.toString());
                sb = new StringBuffer();
            }
            else{
                sb.append(letter);
            }
        }
        if(sb.toString().length() > 0){
            c.getCleanChars().add(sb.toString());
            c.getSegmentedChars().add(sb.toString());
        }
        return c;
    }
	
	public static boolean[] makeLetterCutPoints(Corpus c, boolean[] cutPoints){
        Vector<Boolean> cutList = new Vector<Boolean>();
        for (int i = 0; i < cutPoints.length; i++) {
            String word = c.getCleanChars().get(i);
            for (int j = 0; j < word.length() - 1; j++) {
                cutList.add(false);
            }
            if (cutPoints[i]) {
                cutList.add(true);
            } else {
                cutList.add(false);
            }
        }
        String word = c.getCleanChars().get(c.getCleanChars().size()-1);
        for (int j = 0; j < word.length() - 1; j++) {
            cutList.add(false);
        }
    
//        cutList.remove(cutList.size()-1);
        
        return makeArray(cutList);
    }
	
	public static boolean[] combineUnion(boolean[] forward, boolean[] backward) {
		if (forward.length != backward.length)
			System.out.println("ERROR in COMBINE!");
		
	    boolean[] reversed = new boolean[backward.length];
	    for (int i = 0; i < backward.length; i++) {
	    	reversed[backward.length - i - 1] = backward[i];
	    }	
	    
	    boolean[] bidiCutArray = new boolean[forward.length];
	    for (int i = 0; i < forward.length; i++) 
	    	bidiCutArray[i] = forward[i] || reversed[i];
	    
	    return bidiCutArray;
	}
	
	public static boolean[] simpleUnion(boolean[] first, boolean[] second) {
		boolean[] union = new boolean[first.length];
	    for (int i = 0; i < first.length; i++) 
	    	union[i] = first[i] || second[i];
	    
	    return union;
	}
	
	public static String join(String[] strings, String delimiter) {
		return join(Arrays.asList(strings), delimiter);
	}
	
	public static <T> String join(Collection<T> objs, String delimiter) {
		if (objs == null || objs.isEmpty()) {
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		Iterator<T> iter = objs.iterator();
		sb.append(iter.next());
		
		while (iter.hasNext()) {
			sb.append(delimiter);
			sb.append(iter.next());
		}
		
		return sb.toString();
	}

	public static String getRawInput(String prompt) {
		String input = null;
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		
		try {
			System.out.print(prompt);
			input = reader.readLine();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return input;
	}
	
	public static<T> List<T> reverseCopy(List<T> list) {
		ArrayList<T> reversed = new ArrayList<T>(list);
		Collections.reverse(reversed);
		return reversed;
	}
	
	public static double[] toDoubleArray(int[] ints) {
		double[] doubles = new double[ints.length];
		for (int i = 0; i < ints.length; i++) {
			doubles[i] = ints[i];
		}
		return doubles;
	}
}
