package edu.arizona.ve.trie;

public class TrieXML {
//	public void toXML(PrintWriter out) { 
//	out.write("<Trie>\n");
//	
//	LinkedList<Trie> l = new LinkedList<Trie>();
//	l.add(this);
//	while (l.size() > 0) { 
//		Trie curr = l.removeFirst();
//		curr.xmlDetail(out);
//		
//		for (Trie t : curr.children.values()) { 
//			l.addLast(t);
//		}
//	}
//	out.write("</Trie>\n");
//	out.flush();
//}
//
//public void xmlDetail(PrintWriter out) { 
//	out.write("  <TrieNode ");
//	out.write("prefix=\"");
//	Printer.printList(prefix, out);
//	out.write("\" ");
//	out.write("freq=\"" + freq + "\" ");
//	out.write("/>\n");
//}

	
//	public static void loadFromFile(final Trie root, BufferedReader in) 
//			throws Exception { 
//		try {
//			SAXParserFactory spf = SAXParserFactory.newInstance();
//			spf.setValidating(false);
//			
//			SAXParser sp = spf.newSAXParser();
//			InputSource input = new InputSource(in);
//			sp.parse(input, new DefaultHandler() { 
//				public void startElement(String uri, String localName, String qName, Attributes a) {
//					if (qName.equals("TrieNode")) { 
//						String s = a.getValue("prefix");
//						double freq = Double.parseDouble(a.getValue("freq"));
//
//						if ("".equals(s)) {
//							root.setFrequency(freq);
//						} else { 
//							List<String> list = new LinkedList<String>();
//							String[] sarray = s.split("\\|");
//							for (String str : sarray) 
//								list.add(str);
//
//							// put in the node and make sure to get it back in order
//							// to set it's frequency
//							root.put(list, 0).setFrequency(freq);
//						}
//					}
//				}
//			});
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
}
