package edu.usc.ict.nl.parser;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GrammarRule {
	public enum GrammarRuleType {NORMAL,RE};
	GrammarRuleType type=GrammarRuleType.NORMAL;
	int n;				// the rule number
	double prob;		// the probability of the rule
	String lhs;			// the left hand side
	String[] rhs;		// the right hand side
	SemanticRule sem;
	Pattern sp=Pattern.compile("^\\{(.+)\\}$");
	Pattern nlhs=Pattern.compile("^<(.+)>$");
	Pattern relhs=Pattern.compile("^\\|(.+)\\|$");
		
	public GrammarRuleType getType() {
		return type;
	}

	public GrammarRule(String[] tok, int num, ChartParser chartParser) throws Exception {
		Matcher m;
		lhs = tok[1];
		m=nlhs.matcher(lhs);
		if (m.matches()) {
			type=GrammarRuleType.NORMAL;
		}
		else {
			m=relhs.matcher(lhs);
			if (m.matches()) {
				type=GrammarRuleType.RE;
			} else {
				throw new Exception("LHS is neither <.*> nor |.*|.");
			}
		}
		n = num;
		int length=tok.length;
		m = sp.matcher(tok[length-1]);
		int numRight;
		if (m.matches()) {
			sem=new SemanticRule(m.group(1),chartParser.getSemanticRulesContainer());
			numRight=length-3;
		} else {
			sem=null;
			numRight=length-2;
		}
		if (type.equals(GrammarRuleType.RE) && (numRight>1)) throw new Exception("A regular expression LHS can have only 1 RHS.");
		rhs = new String[numRight];
		for(int i = 0; i < numRight; i++) {					
			rhs[i] = tok[i+2];
		}
	}

	public String toString() {
		return lhs+"("+prob+")<-"+Arrays.asList(rhs)+((sem!=null)?"{"+sem.getRule()+"}":"");
	}
}
