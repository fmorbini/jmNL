package edu.usc.ict.nl.nlu.clearnlp;

import java.io.BufferedReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.clearnlp.dependency.DEPArc;
import com.clearnlp.dependency.DEPNode;
import com.clearnlp.dependency.DEPTree;

import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.graph.Edge;
import edu.usc.ict.nl.util.graph.GraphElement;
import edu.usc.ict.nl.util.graph.Node;

public class CONLL extends Node {
	private Map<Integer,Node> dictionary;
	
	public CONLL(BufferedReader conll) throws Exception {
		super();
		//setID(0);
		dictionary=new HashMap<Integer, Node>();
		dictionary.put(getID(), this);
		String line;
		while((line=conll.readLine())!=null) {
			String[] parts=line.split("[\\t]+",8);
			if (parts.length>=7) {
				Integer id=Integer.parseInt(parts[0]);
				Integer parent=Integer.parseInt(parts[5]);
				String edge=StringUtils.cleanupSpaces(parts[6]);
				String word=StringUtils.cleanupSpaces(parts[1]);
				String lemma=StringUtils.cleanupSpaces(parts[2]);
				updateTree(id,word+"("+lemma+")",parent,null,edge);
			}
		}
	}

	private void updateTree(Integer id, String nodeLabel,Integer parent, String parentLabel,String edge) throws Exception {
		Node n=updateNode(id,nodeLabel);
		Node p=updateNode(parent,parentLabel);
		dictionary.put(id, n);
		dictionary.put(parent, p);
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

	public CONLL(DEPTree tree) throws Exception {
		super();
		dictionary=new HashMap<Integer, Node>();
		dictionary.put(getID(), this);
		Iterator<DEPNode> it=tree.iterator();
		while(it.hasNext()) {
			DEPNode n=it.next();
			DEPArc e=n.getHeadArc();
			DEPNode p=e.getNode();
			if (p!=null) {
				updateTree(n.id, n.form+"("+n.lemma+")", p.id, p.form+"("+p.lemma+")", e.getLabel());
			}
		}
	}
	

	private Node updateNode(Integer id) {
		return updateNode(id, null);
	}
	private Node updateNode(Integer id, String name) {
		Node n=null;
		if (dictionary!=null) {
			n=dictionary.get(id);
			if (n!=null) {
				if (name!=null) n.setName(name);
			} else {
				n=new Node();
				if (name!=null) n.setName(name);
				dictionary.put(id, n);
			}
		}
		return n;
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return super.toString();
	}
}
