package edu.usc.ict.nl.nlg.lf.match;

import java.util.Collection;
import java.util.Stack;

import edu.usc.ict.nl.nlg.lf.WFF;
import edu.usc.ict.nl.util.graph.Node;

public class ArgumentOfOtherMatcher implements LiteralMatcher {

	private Node entireLF;

	public ArgumentOfOtherMatcher(Node entireLF) {
		this.entireLF=entireLF;
	}
	
	@Override
	public boolean matches(Object nlu) {
		return false;
	}

	@Override
	public boolean matches(Node lfNode) {
		try {
			return isThisNodeArgumentOfAnotherLiteral(lfNode, entireLF);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	/**
	 *  
	 * @param n a {@link Node} object representing a literal
	 * @param lf the entire LF including inferences represented as a graph that shows argument relations.
	 * @return true is n has an ancestor that is a literal (i.e. has a name that is not a number and not empty)
	 * @throws Exception 
	 */
	private boolean isThisNodeArgumentOfAnotherLiteral(Node n,Node lf) throws Exception {
		String name=n.getName();
		Node lfn=lf.getNodeNamed(name);
		if (lfn!=null) {
			Stack<Node> s=new Stack<Node>();
			s.push(lfn);
			while(!s.isEmpty()) {
				Node x=s.pop();
				Collection<Node> ps = x.getParents();
				if (ps!=null) {
					for(Node p:ps) {
						if (p instanceof WFF && ((WFF)p).isLiteral()) return true;
						else s.push(p);
					}
				}
			}
		}
		return false;
	}
}
