package edu.arizona.ve.trie;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

public class TrieUtils {

	public static void extractWords(Trie t) {
		List<String> begin = Arrays.asList(new String[]{"*"});
		
		HashMap<String,Integer> wordCounts = new HashMap<String,Integer>();
		
		// The sub-trie rooted at "*" - the "lexicon"
		HashMap<String, Trie> lexicon = t.getChildren(begin);
		
		for (Trie subTrie : lexicon.values()) {
			wordCounts.putAll(subTrie.getWords());
		}
		
		class Holder implements Comparable<Holder> {
			String word;
			int count;
			
			public int compareTo(Holder o) {
				return -(new Integer(count).compareTo(o.count));
			}
		}

		Vector<Holder> v = new Vector<Holder>();
		for (Map.Entry<String,Integer> e : wordCounts.entrySet()) {
			Holder h = new Holder(); h.word = e.getKey().substring(1, e.getKey().length()-1); h.count = e.getValue().intValue();
			v.add(h);
		}
		
		Collections.sort(v);
		
		System.out.println("WORDS! at last.");
		try {
			PrintStream out = new PrintStream("lexica/seed.lex");
			
			for (Holder h : v) {
				if (h.count > 50) {
					out.println(h.word + "\t" + h.count);
				}
			}
			
			out.close();
		} catch (FileNotFoundException e1) { e1.printStackTrace(); }
	}
	
}
