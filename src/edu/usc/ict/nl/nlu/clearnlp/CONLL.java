package edu.usc.ict.nl.nlu.clearnlp;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.emory.mathcs.nlp.component.template.node.NLPNode;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.graph.Edge;
import edu.usc.ict.nl.util.graph.GraphElement;
import edu.usc.ict.nl.util.graph.Node;

public class CONLL extends Node {
	private Map<Integer,CONLL> dictionary;
	private String lemma;
	private String word;
	private int position;
	
	public CONLL() {
		super();
	}

	public CONLL(BufferedReader conll) throws Exception {
		this();
		setWord("root");
		setPosition(0);
		dictionary=new HashMap<Integer, CONLL>();
		dictionary.put(0, this);
		String line;
		while((line=conll.readLine())!=null) {
			String[] parts=line.split("[\\t]+",8);
			if (parts.length>=7) {
				int id=Integer.parseInt(parts[0]);
				String word=StringUtils.cleanupSpaces(parts[1]);
				String lemma=StringUtils.cleanupSpaces(parts[2]);
				Integer parent=Integer.parseInt(parts[5]);
				String edge=StringUtils.cleanupSpaces(parts[6]);
				updateTree(id,word,lemma,parent,null,null,edge);
			}
		}
	}
	public CONLL(NLPNode[] tree) throws Exception {
		this();
		setWord("root");
		dictionary=new HashMap<Integer, CONLL>();
		dictionary.put(0, this);
		if (tree!=null) {
			for(NLPNode n:tree) {
				NLPNode p=n.getDependencyHead();
				String label = n.getDependencyLabel();
				if (p!=null) {
					updateTree(n.getID(), n.getWordForm(),n.getLemma(), p.getID(), p.getWordForm(),p.getLemma(), label);
				}
			}
		}
	}
	
	public void setPosition(int position) {
		this.position = position;
	}
	public int getPosition() {
		return position;
	}

	private void updateTree(int position, String word,String lemma,Integer parentPosition, String parentWord,String parentLemma,String edge) throws Exception {
		CONLL n=updateNode(position,word,lemma);
		CONLL p=updateNode(parentPosition,parentWord,parentLemma);
		//dictionary.put(position, n);
		//dictionary.put(parent, p);
		Edge e=new DepEdge();
		e.setSource(p);
		e.setTarget(n);
		e.setConsume(edge);
		p.addEdge(e, false, false);
	}

	public List<DepEdge> getAllEdgesNamed(String edgeName) {
		List<DepEdge> ret=null;
		Set<GraphElement> visited=new HashSet<GraphElement>();
		LinkedList<Node> q=new LinkedList<Node>(); // breath first
		q.add(this);
		while (!q.isEmpty()) {
			Node c=q.poll();
			if (!visited.contains(c)) {
				visited.add(c);
				Collection<Edge> oes = c.getEdges();
				if (oes!=null) {
					for(Edge oe:oes) {
						if (!visited.contains(oe)) {
							visited.add(oe);
							String label=oe.getConsume();
							if (label!=null && label.equals(edgeName)) {
								if (ret==null) ret=new ArrayList<DepEdge>();
								ret.add((DepEdge) oe);
							}
							Node target=oe.getOtherSide(c);
							if (target!=null) {
								q.addLast(target);
							}
						}
					}
				}
			}
		}
		return ret;
	}


	public void setWord(String word) {
		this.word = word;
	}
	public void setLemma(String lemma) {
		this.lemma = lemma;
	}
	public String getWord() {
		return word;
	}
	public String getLemma() {
		return lemma;
	}
	
	private CONLL updateNode(int position, String word,String lemma) {
		CONLL n=null;
		if (dictionary!=null) {
			n=dictionary.get(position);
			if (n==null) {
				n=new CONLL();
				n.setPosition(position);
				dictionary.put(position, n);
			}
			if (word!=null) n.setWord(word);
			if (lemma!=null) n.setLemma(lemma);
		}
		return n;
	}
	
	@Override
	public String getName() {
		return getWord()+"("+getLemma()+")";
	}
}
