package edu.arizona.ve.corpus;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import edu.arizona.ve.trie.Trie;
import edu.arizona.ve.util.Utils;

/**
*
* @author  Daniel Hewlett
*/
public class Corpus {

	public enum CorpusType { LETTER, WORD, SYLLABLE };
	
	private List<String> cleanChars = new ArrayList<String>();
	private List<String> segChars = new ArrayList<String>();
	
	private List<List<String>> segments; // TODO: this should probably go away
	
	private boolean[] cutPoints;
	private String name = "CORPUS";
	
	CorpusType type = CorpusType.LETTER;
	boolean casePreserved = false; // only meaningful for letter type

	public static final String BOUNDARY = "*";
	
	public static boolean isBoundary(String s) {
		return s.startsWith(BOUNDARY) || s.equals("|");
	}
	
	public static boolean isBoundary(Character c) {
		return c.equals(BOUNDARY.charAt(0));
	}
	
	public static String getFolder(String filetype) {
		if (filetype.equals("downcase") || 
			filetype.equals("downcase_spaces") ||
			filetype.equals("preserve_case") ||
			filetype.equals("naive")) {
			return "letter";
		} else {
			return filetype;
		}
	}
	
	public static String getFolder(CorpusType type) {
		switch (type) {
		case LETTER: return "letter";
		case SYLLABLE: return "syllable";
		case WORD: return "word";
		}
		return null;
	}
	
	// Assumes you don't want to preserve spaces
	public static Corpus autoLoad(String name, CorpusType type, boolean preserveCase) {
		return autoLoad(name, type, preserveCase, false);
	}
	
	// New version with safer parameters
	public static Corpus autoLoad(String name, CorpusType type, boolean preserveCase, boolean preserveSpaces) {
		Corpus cl = new Corpus();

        String wd = System.getProperty("user.dir");

		String path = wd + "/" + "input/" + getFolder(type) + "/" + name + ".txt";

		cl.name = name;
		cl.type = type;
		
		switch (type) {
		
		case LETTER:
			cl.casePreserved = preserveCase;
			if (preserveCase) {
				if (preserveSpaces) {
					cl.loadWithSpaces(path);
				} else {
					cl.load(path, false);
				}
			} else {
				if (preserveSpaces) {
					cl.loadLowercaseWithSpaces(path);
				} else {
					cl.load(path, true);
				}
			}
			break;

		case SYLLABLE:
			cl.loadSyllables(path);
			break;
		
		case WORD:
			cl.loadWords(path);
			break;
		}
		
		return cl;
	}
	
	// input is of the form "br87", "case"
	public static Corpus autoLoad(String file, String type) {
		Corpus cl = new Corpus();

        String wd = System.getProperty("user.dir");

		String path = wd + "/" + "input/" + getFolder(type) + "/" + file + ".txt";

		cl.name = file;
		
		if (type.equals("preserve_case")) {
			cl.load(path, false);
			cl.type = CorpusType.LETTER;
			cl.casePreserved = true;
		} else if (type.equals("downcase")) {
			cl.load(path, true);
			cl.type = CorpusType.LETTER;
			cl.casePreserved = false;
		} else if (type.equals("naive")) {
			cl.type = CorpusType.LETTER;
			cl.loadWithSpaces(path);
			cl.casePreserved = true;
		} else if (type.equals("syllable")) {
			cl.loadSyllables(path);
			cl.type = CorpusType.SYLLABLE;
		} else if (type.equals("word")) {
			cl.loadWords(path);
			cl.type = CorpusType.WORD;
		} else if (type.equals("downcase_spaces")) {
			cl.loadLowercaseWithSpaces(path);
			cl.type = CorpusType.LETTER;
			cl.casePreserved = false;
		} else if (type.equals("sent")) {
			System.out.println("AUTOLOAD ERROR: sent TYPE NOT YET SUPPORTED");
		} else {
			System.out.println("AUTOLOAD ERROR: INVALID FILE");
			return null;
		}

		return cl;
	}

	public static Corpus fromList(List<String> list) {
		Corpus c = new Corpus();
		c.loadList(list);
		return c;
	}
	
	// Only removes whitespace and *
	protected String wsClean(String file) {
		StringBuffer buf = new StringBuffer();
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			while (in.ready()) { 
				char c = (char) in.read();
				if (!Character.isWhitespace(c) && c != '*') {
					buf.append(c);
				} else {
					buf.append(' ');
				}
			}
			in.close();
		} catch (Exception e) {
			System.out.println("ERROR - " + e.getMessage());
			e.printStackTrace();
		}

		return buf.toString().trim();
	}

	private String clean(String file) {
		StringBuffer buf = new StringBuffer();
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			while (in.ready()) { 
				char c = (char) in.read();
				if (Character.isLetter(c)) {
					buf.append(c);
				} else {
					buf.append(' ');
				}
			}
			in.close();
		} catch (Exception e) {
			System.out.println("ERROR - " + e.getMessage());
			e.printStackTrace();
		}

		String result = buf.toString().replaceAll("\\s+", " ");
		result = result.trim();
		return result;
	}

	// the*cat*was*in.dig.nant -> [the, *, cat, *, was, *, in, dig, nant]
	// TODO: Probably should remove this as this is basically the same as the word case.
	public void loadSyllables(String file) {
		cleanChars = new ArrayList<String>();
		segChars = new ArrayList<String>();

		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			String corpus = in.readLine();

			for (String word : corpus.split("[*]")) {

				if (word.contains(".")) {
					for (String syllable : word.split("[.]")) {
						cleanChars.add(syllable);
						segChars.add(syllable);
					}
					segChars.add(BOUNDARY);
				} else {
					cleanChars.add(word);
					segChars.add(word);
					segChars.add(BOUNDARY);
				}
			}
		} catch (Exception e) {
			System.out.println("ERROR - " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void loadVerbatim(String file) {
		cleanChars = new ArrayList<String>();
		segChars = new ArrayList<String>();
		ArrayList<Boolean> tempCutPoints = new ArrayList<Boolean>();

		String cleaned = wsClean(file);
		// For checking
		//	   System.out.println(cleaned.substring(0, 100));

		for (int i = 0; i < cleaned.length(); ++i) {
			Character c = cleaned.charAt(i);

			if (!Character.isWhitespace(c)) {
				cleanChars.add(c.toString());
				segChars.add(c.toString());
				tempCutPoints.add(false);
			} else if (!segChars.isEmpty()) { 
				if (!isBoundary(segChars.get(segChars.size()-1))) {
					segChars.add("*");
					tempCutPoints.set(tempCutPoints.size()-1,true);
				}
			}	
		}  

		tempCutPoints.remove(tempCutPoints.size() - 1);
		
		cutPoints = Utils.makeArray(tempCutPoints);
	}

	// NB: this will strip off all numbers as well
	public void load(String file, boolean lowerCase) {
		cleanChars = new ArrayList<String>();
		segChars = new ArrayList<String>();
		ArrayList<Boolean> tempCutPoints = new ArrayList<Boolean>();

		String cleaned;
		if (lowerCase)
			cleaned = clean(file);
		else 
			cleaned = wsClean(file);
		// For checking
//		System.out.println(cleaned.substring(0, 100));

		for (int i = 0; i < cleaned.length(); ++i) {
			Character c = cleaned.charAt(i);

			if (Character.isLetter(c) || (!lowerCase && !(Character.isWhitespace(c) || isBoundary(c)))) {
				if (lowerCase) {
					cleanChars.add(Character.toLowerCase(c) + "");
					segChars.add(Character.toLowerCase(c) + "");
				} else {
					cleanChars.add(c.toString());
					segChars.add(c.toString());
				}

				tempCutPoints.add(false);
			} else if (c.equals(' ') && !segChars.isEmpty()) { 
				if (!isBoundary(segChars.get(segChars.size()-1))) {
					segChars.add(BOUNDARY);
					tempCutPoints.set(tempCutPoints.size()-1,true);
				}
			}	
		}        

		tempCutPoints.remove(tempCutPoints.size() - 1);

		if (tempCutPoints.size() != (cleanChars.size() - 1)) {
			System.out.println("ERROR: Got " + tempCutPoints.size() + " cut points for " + cleanChars.size() + " letters!");
		}
		
		cutPoints = Utils.makeArray(tempCutPoints);
	}
	
	public void loadLowercaseWithSpaces(String file) {
		cleanChars = new ArrayList<String>();
		segChars = new ArrayList<String>();
		ArrayList<String> lines = new ArrayList<String>();
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			while (reader.ready()) {
				lines.add(reader.readLine().trim());
			}
			reader.close();
			
			String input = Utils.join(lines, " ");
			input = input.toLowerCase();
			input = input.replaceAll("[^\\w\\s]", "");
			input = input.replaceAll("\\s+", " ");
			
			for (char c : input.toCharArray()) {
				cleanChars.add(c + "");
				segChars.add(c + "");
			}
			
			cutPoints = new boolean[cleanChars.size()-1];
		} catch (Exception e) {
			System.out.println("ERROR - " + e.getMessage());
			e.printStackTrace();
		}
	}

	public void loadWithSpaces(String file) {
		try {	
			cleanChars = new ArrayList<String>();
			segChars = new ArrayList<String>();
			
			FileReader reader = new FileReader(file);
			for (int c = reader.read(); c != -1; c = reader.read()) {
				cleanChars.add(Character.toString((char) c));
				if (isBoundary((char) c)) {
					segChars.add("*");
					segChars.add(Character.toString((char) c));
					segChars.add("*");
				}
			}
			reader.close();
			
			cutPoints = new boolean[cleanChars.size()-1];
		} catch (Exception e) {
			System.out.println("ERROR - " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	// Preserves boundaries as tokens, you can use this for the sentence boundary information
	// cleanChars = segChars = whatever characters were in the corpus file
	public void naiveLoad(String file, CorpusType ctype) {
		
		try {	
			BufferedReader in = new BufferedReader(new FileReader(file));
			String corpus = in.readLine().trim();
			in.close();

			if (ctype.equals(CorpusType.LETTER)) {
				cleanChars = new ArrayList<String>();
				segChars = new ArrayList<String>();
	
				for (int i = 0; i < corpus.length(); ++i) {
					Character c = corpus.charAt(i);
					cleanChars.add(Character.toString(c));
					segChars.add(Character.toString(c));
				}    
				
			} else if (ctype.equals(CorpusType.WORD)) {
				cleanChars = Arrays.asList(corpus.trim().split("\\s+"));
				segChars = cleanChars; // is this OK?
			}
			
			cutPoints = new boolean[cleanChars.size()-1];
			
//				System.out.println(cleanChars.subList(0, 100));
		} catch (Exception e) {
			System.out.println("ERROR - " + e.getMessage());
			e.printStackTrace();
		}
		
	}

	public void loadWords(String file) {
		StringBuffer buf = new StringBuffer();
		try {
			BufferedReader in = new BufferedReader(new FileReader(file));
			while (in.ready()) { 
				char c = (char) in.read();
				buf.append(c);
			}
			in.close();
		} catch (Exception e) {
			System.out.println("ERROR - " + e.getMessage());
			e.printStackTrace();
		}

		String tempString = buf.toString();
		String[] words = tempString.split("\\s+");

		loadArray(words);
	}

	public void loadArray(String[] input) {
		ArrayList<Boolean> tempCutPoints = new ArrayList<Boolean>();
		
		for (String s : input) {
			if (isBoundary(s)) {
				if (!segChars.get(segChars.size()-1).equals(BOUNDARY)) {
					segChars.add(BOUNDARY);
					tempCutPoints.set(tempCutPoints.size()-1,true);	
				}
			} else {
				cleanChars.add(s);
				segChars.add(s);
				tempCutPoints.add(false);
			}
		}
		
		tempCutPoints.remove(tempCutPoints.size() - 1);
		
		cutPoints = Utils.makeArray(tempCutPoints);
	}

	public void loadList(List<String> input) {
		ArrayList<Boolean> tempCutPoints = new ArrayList<Boolean>();
		
		for (String s : input) {
			if (isBoundary(s)) {
				if (!segChars.get(segChars.size()-1).equals(BOUNDARY)) {
					segChars.add(BOUNDARY);
					tempCutPoints.set(tempCutPoints.size()-1,true);	
				}
			} else {
				cleanChars.add(s);
				segChars.add(s);
				tempCutPoints.add(false);
			}
		}
		
		tempCutPoints.remove(tempCutPoints.size() - 1);
		
		cutPoints = Utils.makeArray(tempCutPoints);
	}
	
	// for testing convenience
	public void loadString(String corpus) {
		cleanChars = new ArrayList<String>();
		segChars = new ArrayList<String>();
		ArrayList<Boolean> tempCutPoints = new ArrayList<Boolean>();

		for (int i = 0; i < corpus.length(); ++i) {
			Character c = corpus.charAt(i);

			if ((isBoundary(c) || c.equals(' ')) && !segChars.isEmpty()) { 
				if (!segChars.get(segChars.size()-1).equals(BOUNDARY)) {
					segChars.add(BOUNDARY);
					tempCutPoints.set(tempCutPoints.size()-1,true);	
				}
			} else {
				cleanChars.add(c + "");
				segChars.add(c + "");
				tempCutPoints.add(false);
			}
		}

		tempCutPoints.remove(tempCutPoints.size() - 1);
		
		cutPoints = Utils.makeArray(tempCutPoints);
	}

	public boolean[] getCutPoints() {
		return cutPoints;
	}
	
	public List<String> getCleanChars() {
		return cleanChars;
	}

	public String getName() {
		return name;
	}
	
	public CorpusType getType() {
		return type;
	}
	
	public void setType(CorpusType t) {
		type = t;
	}
	
	public List<String> getReversed() {
		ArrayList<String> backwardCorpus = new ArrayList<String>(cleanChars);
		Collections.reverse(backwardCorpus);
		return backwardCorpus;
	}

	public List<String> getSegmentedChars() {
		return segChars;
	}

	public List<String> getSegments() {
		ArrayList<String> segs = new ArrayList<String>();
		String segment = new String();
		for (int i = 0; i < cutPoints.length; i++) {
			boolean cut = cutPoints[i];
			if (cut) {
				if (i != 0) { 
					segs.add(segment); 
				}
				segment = new String(cleanChars.get(i+1));
			} else {
				segment = segment.concat(cleanChars.get(i+1));
			}
		}

		segs.add(segment);

		return segs;
	}

	public List<List<String>> getSegments(boolean[] cutPoints) {
		List<List<String>> segments = new ArrayList<List<String>>();
		List<String> segment = new ArrayList<String>();
		for (int i = 0; i < cleanChars.size(); i++) {
			if (i < cutPoints.length && cutPoints[i]) {
				segment.add(cleanChars.get(i));
				segments.add(segment);
				segment = new ArrayList<String>();
			} else {
				segment.add(cleanChars.get(i));
			}
		}
		
		if (segment.size() > 0) {
			segments.add(segment); 
		}
		
		return segments;
	}
	
	public Corpus getReverseCorpus() {
		Corpus rev = new Corpus();
		
		rev.type = type; 
		
		ArrayList<String> backwardCorpus = new ArrayList<String>(getCleanChars());
		Collections.reverse(backwardCorpus);
		rev.cleanChars = backwardCorpus;
		
		ArrayList<String> backwardSegCorpus = new ArrayList<String>(getSegmentedChars());
		Collections.reverse(backwardSegCorpus);
		rev.segChars = backwardSegCorpus;
		
		rev.cutPoints = new boolean[cutPoints.length];
		for (int i = 0; i < cutPoints.length; i++) {
			rev.cutPoints[i] = cutPoints[cutPoints.length-i-1];
		}
		
		return rev;
	}
	
	public Corpus getSubCorpus(int size) {
		List<String> subList = new ArrayList<String>(getSegmentedChars().subList(0, size));
		Corpus subCorpus = Corpus.fromList(subList);
		subCorpus.setType(this.getType());
		return subCorpus;
	}
	
	// The behavior of this function is odd and wrong
	public void loadSegments(String file) {
		try {	
			BufferedReader in = new BufferedReader(new FileReader(file));
			String corpus = in.readLine().trim();
			in.close();

			String[] segStrings = corpus.split("[*]");

			segments = new ArrayList<List<String>>();
			for (String s : segStrings) {
				cleanChars.add(s);
				segChars.add(s);
				ArrayList<String> segment = new ArrayList<String>();
				for (Character c : s.toCharArray()) { segment.add(c.toString()); }
				//			   segment.add("*"); // every segment ends with *
				segments.add(segment);
			}        
			
			cutPoints = new boolean[cleanChars.size()-1];

		} catch (Exception e) {
			System.out.println("ERROR - " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	public Trie makeForwardTrie(int depth) {
		Trie forwardTrie = new Trie();
		Trie.addAll(forwardTrie, getCleanChars(), depth);
		forwardTrie.generateStatistics();
		return forwardTrie;
	}
	
	public Trie makeBackwardTrie(int depth) {
		Trie backwardTrie = new Trie();
		Trie.addAll(backwardTrie, getReversed(), depth);
		backwardTrie.generateStatistics();
		return backwardTrie;
	}
	
	public Trie makeKnowledgeTrie(int depth, boolean[] cuts) {
		// First we make a corpus which contains boundary characters
		ArrayList<String> knowledgeCorpus = new ArrayList<String>(cleanChars.size());
		if (cuts.length != cleanChars.size() - 1) { // Sanity check
			System.out.println("ERROR: Cut point length mismatch in writer");
		}
		
		int numLetters = cleanChars.size();
		
		for (int i = 0; i < numLetters; i++) {
			knowledgeCorpus.add(cleanChars.get(i));

			if (i < cuts.length) {
				if (cuts[i]) {
					knowledgeCorpus.add("*");
				} 
			}
		}

		Trie knowledgeTrie = new Trie();
		Trie.addAll(knowledgeTrie, knowledgeCorpus, depth);
		knowledgeTrie.generateStatistics();
		return knowledgeTrie;
	}
	
	// Note: This is not used for anything right now
	public Trie[] makeKnowledgeTries(int depth, boolean[] cuts) {
		Trie forward = new Trie();
		forward.depth = depth;
		Trie backward = new Trie();
		backward.depth = depth;
		
		List<List<String>> segments = getSegments(cuts);
		
		for (List<String> word : segments) {
			if (word.size() > depth) {
				forward.put(word.subList(0, depth), 1);
				backward.put(Utils.reverseCopy(word).subList(0, depth), 1);
			} else {
				forward.put(word, 1);
				backward.put(Utils.reverseCopy(word), 1);
			}
		}
		
		forward.generateStatistics();
		backward.generateStatistics();
		
		return new Trie[] { forward, backward };
	}
}
