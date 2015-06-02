package edu.usc.ict.nl.nlg.lf.time;

import java.util.ArrayList;
import java.util.List;

import edu.usc.ict.nl.nlg.lf.Predication;
import edu.usc.ict.nl.util.graph.Node;

public class EventNode extends Node {

	List<Predication> ps=null;   
	
	public EventNode(String id) {
		super(id);
	}
	
	public void addPredication(Object p) {
		if (ps==null) ps=new ArrayList<Predication>();
		ps.add(new Predication(p));
	}

	@Override
	public String gdlText() {
		return "node: { shape: "+getShape()+" title: \""+getID()+"\" label: \""+toString()+"\" info1: \""+toString(true)+"\"}\n";
	}

	public String toString() {
		return toString(false);
	}
	public String toString(boolean more) {
		if (more) {
			StringBuffer ret=new StringBuffer();
			ret.append(super.toString());
			if(ps!=null && !ps.isEmpty()) {
				ret.append(":");
				for(Predication p:ps) {
					ret.append(" |"+p.toString()+"|");
				}
			}
			return ret.toString();
		}
		else return super.toString();
	}
	
	
	public List<Predication> getPredications() {
		return ps;
	}
	public void setPredications(List<Predication> ps) {
		this.ps = ps;
	}
}
