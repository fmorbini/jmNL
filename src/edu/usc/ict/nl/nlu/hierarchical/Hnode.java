package edu.usc.ict.nl.nlu.hierarchical;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Stack;

import edu.usc.ict.nl.util.graph.Node;

public class Hnode extends Node {
	private boolean hasTD=false;
	public Hnode(String name) {super(name);}
	public void setHasTrainingData(boolean htd) {this.hasTD = htd;}
	public boolean getHasTrainingData() {return hasTD;}
	public HashSet<Node> getFirstDescendantsWithTrainingData() throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
		HashSet<Node>ret=new HashSet<Node>();
		Stack<Hnode> s=new Stack<Hnode>();
		s.push(this);
		while (!s.isEmpty()) {
			Hnode c=s.pop();
			if (c.getHasTrainingData()) ret.add(c);
			else {
				Collection children = c.getImmediateChildren();
				if (children!=null) s.addAll(children);
			}
		}
		return ret;
	}
	
	@Override
	public String gdlText() {
		return "node: { color: "+((getHasTrainingData())?"red":"white")+" title: \""+getID()+"\" label: \""+toString()+"\"}\n";
	}
}
