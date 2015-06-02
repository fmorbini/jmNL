package edu.usc.ict.nl.nlg.lf;

import java.util.List;
import java.util.Map;

import org.w3c.tools.sexpr.Symbol;

import edu.usc.ict.nl.nlg.lf.utils.NLUGraphUtils;
import edu.usc.ict.nl.util.graph.Node;

public class NLG2Data {

	public List<Predication> clauses;
	public Node logicForm;
	public Map<String, Symbol> equalities;
	public Map<Predication, Node> explanations;
	public Map<String,Object> eventToLiteral;

	public NLG2Data(List<Predication> independentLiteralsOrder, Node lf,Map<String, Symbol> groupsOfEqualities,Map<Predication, Node> associatedExplanations) {
		this.clauses=independentLiteralsOrder;
		this.logicForm=lf;
		this.equalities=groupsOfEqualities;
		this.explanations=associatedExplanations;
		this.eventToLiteral=NLUGraphUtils.getEventsFromLF(lf);
	}

}
