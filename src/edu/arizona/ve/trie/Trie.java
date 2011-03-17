package edu.arizona.ve.trie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.arizona.ve.corpus.Corpus;
import edu.arizona.ve.util.NF;
import edu.arizona.ve.util.Utils;
/**
 *
 * @author  Wesley Kerr, Daniel Hewlett
 */
public class Trie {
	
	// Special members for root node
	public int maxDepthSeen = 0;
	private HashMap<Integer,StatNode> statistics = null; 
	
	// All nodes have these members
	public List<String> prefix;
	public HashMap<String,Trie> children;
	
	public double  freq;
	public double  stdFreq;

	public double  boundaryEntropy;
	public double  stdBoundaryEntropy;
	
	public double  probability = -1;
	public double  internalEntropy;
	public double  stdInternalEntropy;

	public int depth = -1;

	/** Creates a new instance of Trie */
	public Trie() {
		children = new HashMap<String,Trie>();
	}

	/**
	 * process the statistics for the entire tree.
	 * Requires touching all of the nodes within the tree.
	 * Depth level traversal.
	 */
	public void generateStatistics() {
		statistics = new HashMap<Integer,StatNode>();

		for (int i = 1; i <= maxDepthSeen; ++i) {
			statistics.put(new Integer(i), new StatNode());
		}

		fillStatistics(statistics);

		for (int i = 1; i <= maxDepthSeen; ++i) {
			StatNode s = (StatNode) statistics.get(new Integer(i));
			s.calculate();
		}
		standardize(statistics);
	}

	/**
	 * this just displays the stats for for a given depth
	 * @param depth
	 */
	public void printStats(int depth) {
		if (depth != -1) {
			StatNode s = (StatNode) statistics.get(new Integer(depth));
			s.print(depth);
			return;
		}

		for (int i = 1; i <= maxDepthSeen; ++i) {
			StatNode s = (StatNode) statistics.get(new Integer(i));
			s.print(i);
		}
	}	

	public StatNode getStatNode(int depth) {
		return statistics.get(depth);
	}	
	
	/**
	 * wrapper function so that we can just call the insert
	 * on the sequence and it will keep track of what was
	 * inserted.
	 * @param sequence
	 * @param count
	 */
	public Trie put(List<String> sequence, double count)  { 
		maxDepthSeen = Math.max(maxDepthSeen, sequence.size());
		return put(new LinkedList<String>(), sequence, count);
	}
	
	/**
	 * 
	 * @param prefix
	 * @param suffix
	 * @param count
	 */
	public Trie put(List<String> prefix, List<String> suffix, double count) { 
		this.prefix = prefix;
		this.freq += count;
		
		//System.out.println("Pre " + prefix + " Suf " + suffix + " Freq " + freq);
		if (suffix != null && suffix.size() > 0) { 
			String obj = suffix.get(0);
			Trie t = children.get(obj);
			if (t == null) { 
				t = new Trie();
				children.put(obj, t);
			}

			List<String> newPrefix = new ArrayList<String>(prefix);
			newPrefix.add(obj);
			
			List<String> newSuffix = new ArrayList<String>(suffix.subList(1, suffix.size()));
			return t.put(newPrefix, newSuffix, count);
		} else { 
			return this;
		}
	}

	protected void calculateEntropy() {
		double f = 0;
		double sum = 0;

		if (children.size() == 0) {
			boundaryEntropy = 0;
			return;
		}

		for (Trie t : children.values()) { 
			sum += t.freq;
		}

		boundaryEntropy  = 0;
		if (sum > 0) {
			double norm = 1.0 / sum;
			for (Trie t : children.values()) { 
				f = norm * t.freq;
				boundaryEntropy += ( -f * Math.log(f));
			}
		}
	}
	
	// this will get called on the root node, passing total frequency down the tree
	protected void calculateInternalEntropy() {
		if (probability == -1) { // this means we are the root node
			for (Trie child : children.values()) {
				child.probHelper(freq);
			}
		} // else do nothing, it's already there
	}
	
	protected void probHelper(double totalFrequency) {
		probability = freq / totalFrequency;
		internalEntropy = -Math.log(probability);
		
		for (Trie child : children.values()) {
			child.probHelper(totalFrequency);
		}
	}

	public void fillStatistics(HashMap<Integer,StatNode> stats) {
		calculateInternalEntropy();
		calculateEntropy();
		if (prefix != null && !prefix.isEmpty()) {
			StatNode stat = (StatNode) stats.get(new Integer(prefix.size()));
			stat.frequencies.add(new Double(freq));
			stat.surprisals.add(internalEntropy);
			stat.boundaryEntropies.add(new Double(boundaryEntropy));
		}

		if (children.size() == 0) return;

		for (Trie t : children.values()) { 
			t.fillStatistics(stats);
		}
	}

	public void standardize(HashMap<Integer,StatNode> stats) {
		if (prefix != null && !prefix.isEmpty()) {
			StatNode stat = (StatNode) stats.get(new Integer(prefix.size()));
			stdFreq = (stat.stdDevFreq == 0) ? 0 : (freq - stat.meanFreq) / stat.stdDevFreq;
			stdInternalEntropy = (stat.stdDevIntEnt == 0) ? 0 : (internalEntropy - stat.meanIntEnt) / stat.stdDevIntEnt;
			stdBoundaryEntropy = (stat.stdDevEnt == 0) ? 0 : (boundaryEntropy - stat.meanEnt) / stat.stdDevEnt;
		}

		if (children.size() == 0) return;

		for (Trie t : children.values()) {
			t.standardize(stats);
		}
	}

	public HashMap<String,Integer> getWords() {
		HashMap<String, Integer> result = new HashMap<String,Integer>();
		
		for (Map.Entry<String,Trie> e : children.entrySet() ) {
			String child = e.getKey();
			Trie subTrie = e.getValue();
			
			if (child.equals("*")) {
				result.put(Utils.fromList(subTrie.prefix), (int) subTrie.freq);
			} else {
				result.putAll(subTrie.getWords());
			}
		}
		
		return result;
	}
	
	/**
	 * get the children of the specified sequence of objects
	 * @param sequence
	 * @return
	 */
	public HashMap<String,Trie> getChildren(List<String> sequence) {
		if (sequence == null || sequence.size() == 0) {
			return children;
		}

		String child = sequence.get(0);
		if (!children.containsKey(child))
			return new HashMap<String,Trie>();

		Trie t = children.get(child);
		List<String> suffix = sequence.subList(1, sequence.size());
		return t.getChildren(suffix);
	}

	public double getFreq(List<String> sequence) {
		if (sequence == null || sequence.size() == 0) 
			return freq;

		String child = sequence.get(0);
		if (!children.containsKey(child))
			return 0;

		Trie t = children.get(child);
		List<String> suffix = sequence.subList(1,sequence.size());
		return t.getFreq(suffix);
	}
	
	/**
	 * returns the standardized frequence for the sequence
	 * @param word
	 * @return
	 */
	public double getStdFreq(List<String> sequence) {
		if (sequence == null || sequence.size() == 0) 
			return stdFreq;

		String child = sequence.get(0);
		if (!children.containsKey(child))
			return 0;

		Trie t = children.get(child);
		List<String> suffix = sequence.subList(1,sequence.size());
		return t.getStdFreq(suffix);
	}

	public double getStdIntEntropy(List<String> sequence) {
		if (sequence == null || sequence.isEmpty()) 
			return stdInternalEntropy;
		
		String child = sequence.get(0);
		if (!children.containsKey(child))
			return Double.POSITIVE_INFINITY;   
						// NB: zero is OK here because the stdIntEnropy is always negative. Really?
						// NO NO NO the STANDARD entropy is not always negative - it's a standard normal dist
						// ... dumbass!

		Trie t = children.get(child);
		List<String> suffix = sequence.subList(1,sequence.size());
		return t.getStdIntEntropy(suffix);
	}
	
	/**
	 * return the standard entropy for the sequence
	 * @param word
	 * @return
	 */
	public double getStdEntropy(List<String> sequence) {
		if (sequence == null || sequence.size() == 0) 
			return stdBoundaryEntropy;

		String child = sequence.get(0);
		if (!children.containsKey(child))
			return 0;

		Trie t = children.get(child);
		List<String> suffix = sequence.subList(1,sequence.size());
		return t.getStdEntropy(suffix);
	}

	/**
	 * return the standard entropy for the sequence
	 * @param word
	 * @return
	 */
	public double getEntropy(List<String> sequence) {
		if (sequence == null || sequence.size() == 0)
			return boundaryEntropy;

		String child = sequence.get(0);
		if (!children.containsKey(child))
			return 0;

		Trie t = children.get(child);
		List<String> suffix = sequence.subList(1,sequence.size());
		return t.getEntropy(suffix);
	}


	/**
	 * return the frequency information for the sequence
	 * @param sequence
	 * @return
	 */
	public List<Double> getCountList(List<String> sequence) { 
		if (sequence == null || sequence.size() == 0) {
			List<Double> l = new LinkedList<Double>();
			l.add(new Double(freq));
			return l;
		}

		String child = sequence.get(0);
		if (!children.containsKey(child))
			return null;

		Trie t = children.get(child);
		List<String> suffix = sequence.subList(1,sequence.size());
		List<Double> l = t.getCountList(suffix);
		if (l == null) 
			return null;

		if (!prefix.equals(""))
			l.add(0, new Double(freq));
		return l;
	}



	public void levelOrder() {
		LinkedList<Trie> l = new LinkedList<Trie>();
		l.add(this);
		while (l.size() > 0) {
			Trie curr = l.removeFirst();
			curr.printData("");

			for (Trie t : curr.children.values()) { 
				l.addLast(t);
			}
		}
	}
	
	public void setFrequency(double freq) { 
		this.freq = freq;
	}
	
	
	// STATIC METHODS (old style)
	
	/**
	 * given a set of tokens and a window size make sure to 
	 * add all of the sets of tokens with the window size
	 * @param tokens
	 * @param windowSize
	 */
	public static void addAll(Trie root, List<String> tokens, int windowSize) {
		root.depth = windowSize;
		
		for (int i = 0; i < tokens.size(); ++i) { 
			List<String> tmp = null;
			if (i+windowSize > tokens.size()) { 
				tmp = tokens.subList(i, tokens.size());
			} else { 
				tmp = tokens.subList(i, i+windowSize);
			}
			
			root.put(tmp, 1);
		}		
	}
	
	public static Trie buildTrie(Corpus c, int depth) {
		return buildTrie(c.getCleanChars(), depth);
	}
	
	public static Trie buildTrie(List<String> tokens, int depth) {
		Trie root = new Trie();
		addAll(root,tokens,depth);
		root.generateStatistics();
		return root;
	}
	
	public static Trie buildBackwardTrie(Corpus c, int depth) {
		return buildBackwardTrie(c.getCleanChars(), depth);
	}
	
	public static Trie buildBackwardTrie(List<String> tokens, int depth) {
		ArrayList<String> backwardCorpus = new ArrayList<String>(tokens);
		Collections.reverse(backwardCorpus);
		return buildTrie(backwardCorpus, depth);
	}
	
	public double getIntEntropy(List<String> sequence) {
		if (sequence == null || sequence.size() == 0)
			return internalEntropy;

		String child = sequence.get(0);
		if (!children.containsKey(child))
			return 0;

		Trie t = children.get(child);
		List<String> suffix = sequence.subList(1,sequence.size());
		return t.getIntEntropy(suffix);
	}
	
	
	
	
	
	
	////////////////////////////////////////////
	// Printing
	
	/** 
	 * print information about the tree
	 * @param path
	 * @param tab
	 * @param nf
	 */
	public void print(String path, String tab) {
		if (!children.containsKey(path))
			return;

		Trie t = children.get(path);
		t.display("", true);
	}
	
	/**
	 * print information for sequence (stored in trie)
	 * @param word
	 * @param nf
	 */
	public void printSequence(List<String> sequence) {
		if (sequence != null && sequence.size() > 0) {
			String child = sequence.get(0);

			if (!children.containsKey(child))
				return;

			Trie t = children.get(child);
			t.printSequence(sequence.subList(1, sequence.size()));
		} else {
			printData("");
		}
	}

	/**
	 * perform the actually printing
	 * @param tab
	 * @param nf
	 */
	protected void printData(String tab) {
		System.out.print(tab);
		for (String t : prefix) 
			System.out.print(t + " ");
		System.out.print("Freq: " + NF.format(freq) + " ");
		System.out.print("StdFreq: " + NF.format(stdFreq) + " ");
		System.out.print("Entropy: " + NF.format(boundaryEntropy) + " ");
		System.out.print("StdEntropy: " + NF.format(stdBoundaryEntropy) + " ");
		System.out.print("IntEntropy: " + NF.format(internalEntropy) + " ");
		System.out.print("StdIntEntropy: " + NF.format(stdInternalEntropy) + " ");
		System.out.println();
	}

	/**
	 * decide what to print
	 * @param tab
	 * @param nf
	 * @param all
	 */
	public void display(String tab, boolean all) {
		printData(tab);

		if (children.size() == 0 || !all) return;

		printChildren(tab, all);
	}

	protected void printChildren(String tab, boolean all) {
		for (Trie t : children.values()) { 
			t.display(tab + " ", all);
		}
	}

	/**
	 * 
	 * @param word
	 * @param nf
	 * @param all
	 */
	public void printExtensions(List<String> sequence, boolean all) {
		if (sequence == null || sequence.size() == 0) {
			printChildren("", all);
			return;
		}

		String child = sequence.get(0);
		if (!children.containsKey(child))
			return;

		Trie t = children.get(child);
		t.printExtensions(sequence.subList(1, sequence.size()), all);

	}
	
}
