package edu.usc.ict.nl.parser;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.parser.ChartParser.Item;
import edu.usc.ict.nl.parser.GrammarRule.GrammarRuleType;

/**
 * Simple grammar class, used by chartParser in the NLU
 * @author sagae
 *
 */
public class Grammar {
	// Simple class for grammar rules

	GrammarRule[] rules;		// the rules
	int nrules;					// number of rules in the grammar
	int MAXRULENUM = 250000;	// maximum number of rules

	// the following hashmaps index the rules by lhs and first symbol of rhs.
	// this is useful for the chart parsing algorithm
	HashMap<String, Vector<Integer> > gralhs = new HashMap<String, Vector<Integer> >();
	HashMap<String, Vector<Integer> > grarhs = new HashMap<String, Vector<Integer> >();
	HashMap<Pattern,Integer> terminalRE=new HashMap<Pattern, Integer>();

	public Vector<Integer> getGrammarRulesMatchingThisRHS(Item it) {
		Vector<Integer> ret=new Vector<Integer>();
		String rhs=it.getString();
		if (it.isTerminal()) {
			for (Pattern p:terminalRE.keySet()) {
				Matcher m=p.matcher(rhs);
				if (m.matches()) {					
					ret.add(terminalRE.get(p));
				}
			}
		}
		Vector<Integer> normalRules=grarhs.get(rhs);
		if (normalRules!=null) ret.addAll(normalRules);
		return ret;
	}
	
	public Grammar(File fname, ChartParser chartParser) throws Exception {
		loadGrammar(fname,chartParser);
	}

	// loads the grammar in file fname (a text file)
	int loadGrammar(File fname, ChartParser chartParser) throws Exception {
		rules = new GrammarRule[MAXRULENUM];
		nrules = 0;  		// number of rules in the grammar so far

		// Open the grammar file
		FileInputStream fstream = new FileInputStream(fname);

		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));

		String strLine;

		//Read File Line By Line
		while ((strLine = br.readLine()) != null)   {				
			if ((strLine.length()>0) && (strLine.charAt(0)!='#')) {
				// split at white spaces
				String[] tok = strLine.split("\\s+");

				// the format is "prob lhs rhs1 rhs2..."
				// if we have less than 3 tokens, this is not a rule
				if(tok.length < 3) {
					continue;
				}

				// add the grammar rule
				GrammarRule rule=new GrammarRule(tok,nrules,chartParser);
				if (rule.sem==null) throw new Exception("Invalid rule: "+rule+". No semantics.");
				rules[nrules]=rule;

				if (rule.getType().equals(GrammarRuleType.RE)) {
					try {
						terminalRE.put(Pattern.compile(rule.rhs[0]), nrules);
					} catch(Exception e) {e.printStackTrace();}
				}
				
				// adds the rule number to the hashmaps.
				Vector<Integer> tmp=gralhs.get(tok[1]);
				if(tmp==null) gralhs.put(rule.lhs, tmp=new Vector<Integer>());
				tmp.add(nrules);

				tmp=grarhs.get(rule.rhs[0]);
				if (tmp==null) grarhs.put(rule.rhs[0], tmp=new Vector<Integer>());
				tmp.add(nrules);

				// increase the rule counter
				nrules++;

				// if we have too many rules, return -1
				if(nrules > MAXRULENUM) throw new Exception("too many rules ("+MAXRULENUM+")");
			}
		}

		//Close the input stream
		in.close();

		return 0;
	}
}

