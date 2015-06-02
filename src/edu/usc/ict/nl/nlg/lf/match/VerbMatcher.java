package edu.usc.ict.nl.nlg.lf.match;

import edu.usc.ict.nl.nlg.lf.Lexicon;
import edu.usc.ict.nl.nlg.lf.WFF;
import edu.usc.ict.nl.nlg.lf.utils.NLUUtils;
import edu.usc.ict.nl.util.graph.Node;

public class VerbMatcher implements LiteralMatcher {

	private Lexicon lex;

	public VerbMatcher(Lexicon lex) {
		this.lex=lex;
	}
	
	@Override
	public boolean matches(Object nlu) {
		if (nlu!=null) {
			String pname=NLUUtils.getPredicateName(nlu);
			return NLUUtils.isVerb(pname, lex);
		}
		return false;
	}

	@Override
	public boolean matches(Node lfNode) {
		if (lfNode!=null && lfNode instanceof WFF) {
			return matches(((WFF)lfNode).getParsedNLUObject(false));
		}
		return false;
	}
	
}
