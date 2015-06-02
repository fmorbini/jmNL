package edu.usc.ict.nl.nlg.lf.time;

import edu.usc.ict.nl.util.graph.Node;

public class Query {
	/**
	 * returns true if n1 is after n2
	 * @param n1
	 * @param n2
	 * @return
	 */
	public static boolean after(Node n1, Node n2) {
		Integer d=n2.getDistanceTo(n1);
		return d!=null && d>=0;
	}
	public static boolean before(Node n1, Node n2) {
		Integer d=n1.getDistanceTo(n2);
		return d!=null && d>=0;
	}
}
