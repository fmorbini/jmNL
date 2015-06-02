package edu.usc.ict.nl.nlg.lf.match;

import edu.usc.ict.nl.util.graph.Node;

public class NotMatcher implements LiteralMatcher {

	private LiteralMatcher matcher;

	public NotMatcher(LiteralMatcher toBeNegated) {
		this.matcher=toBeNegated;
	}
	
	@Override
	public boolean matches(Object nlu) {
		return !matcher.matches(nlu);
	}

	@Override
	public boolean matches(Node lfNode) {
		return !matcher.matches(lfNode);
	}

}
