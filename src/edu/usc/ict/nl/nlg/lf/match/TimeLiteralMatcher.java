package edu.usc.ict.nl.nlg.lf.match;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.nlg.lf.WFF;
import edu.usc.ict.nl.nlg.lf.utils.NLUUtils;
import edu.usc.ict.nl.util.graph.Node;

public class TimeLiteralMatcher implements LiteralMatcher {

	public static final Pattern parPattern=Pattern.compile("[pP][aA][rR](')?");
	public static final Pattern seqPattern=Pattern.compile("[sS][eE][qQ](')?");
	
	@Override
	public boolean matches(Object nlu) {
		return NLUUtils.isPredicationNamed(nlu, parPattern) || NLUUtils.isPredicationNamed(nlu, seqPattern);
	}

	@Override
	public boolean matches(Node lfNode) {
		if (lfNode!=null && lfNode instanceof WFF) {
			Object nlu=((WFF)lfNode).getParsedNLUObject(false);
			return matches(nlu);
		}
		return false;
	}
	private static final Pattern timeP=Pattern.compile("[\\s]*\\([\\s]*(seq|par)(')?[\\s]+.+");
	/**
	 * removes from the list of nodes those that are literals with name par or seq.
	 * @param obs
	 * @return
	 */
	public static List<Node> removeTimingPredicates(List<Node> obs) {
		List<Node> ret=null;
		if (obs!=null) {
			for(Node ob:obs) {
				String name=ob.getName();
				Matcher m=timeP.matcher(name);
				if (!m.matches()) {
					if (ret==null) ret=new ArrayList<Node>();
					ret.add(ob);
				}
			}
		}
		return ret;
	}
	
}
