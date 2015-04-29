package edu.usc.ict.nl.nlu.clearnlp;

import edu.usc.ict.nl.util.graph.Edge;

public class DepEdge extends Edge {
	@Override
	public String toGDL() {
		String gdl="edge: {source: \""+getSource().getID()+"\" target: \""+getTarget().getID()+"\" label: \""+getConsume()+"\""+((notDirectional)?"arrowstyle: \"none\"":"")+"}\n";
		return gdl;
	}
}
