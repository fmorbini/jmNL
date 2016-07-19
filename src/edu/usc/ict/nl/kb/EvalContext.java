package edu.usc.ict.nl.kb;

import edu.usc.ict.nl.dm.reward.model.DialogueOperator;
import edu.usc.ict.nl.dm.reward.possibilityGraph.OperatorHistoryNode;

public class EvalContext {
	/**
	 * this is the operator that contains a specific formula (to which this context is associated)
	 */
	private DialogueOperator operator=null;
	/**
	 * this one contains the list of information states used in the evaluation
	 */
	private DialogueKB[] lowestUsed=null;
	
	/**
	 * contains the history of executed operators. The content itself is the last executed operator.
	 * The one before will be reached by following the single incoming edge into this node.
	 */
	private OperatorHistoryNode historyOfExecutedOperators=null;
		
	/** information state to be used for evaluations in this context
	 * 
	 */
	private DialogueKB is=null;
	
	public EvalContext(DialogueKB is, DialogueOperator o) {
		setInformationState(is);
		setFormulaOperator(o);
	}
	public DialogueKB[] getLowestUsed() {
		return lowestUsed;
	}
	public void setLowestUsed(DialogueKB[] lowestUsed) {
		this.lowestUsed = lowestUsed;
	}
	public void updateLowestKBUsed(DialogueKB thisVarKB) {
		if (lowestUsed!=null && lowestUsed.length==1) {
			DialogueKB previousLowest=lowestUsed[0];
			if (thisVarKB!=null && thisVarKB!=previousLowest) {
				if (previousLowest==null || thisVarKB.findThisKBInHierarchy(previousLowest)!=null) lowestUsed[0]=thisVarKB;
			}
		}
	}
	public DialogueOperator getFormulaOperator() {return operator;}
	public void setFormulaOperator(DialogueOperator op) {this.operator=op;}
	
	public OperatorHistoryNode getExecutedOperatorsHistory() {
		return historyOfExecutedOperators;
	}
	public void setExecutedOperatorsHistory(OperatorHistoryNode h) {
		this.historyOfExecutedOperators =h;
	}
	public DialogueKB getInformationState() {
		return is;
	}
	public EvalContext setInformationState(DialogueKB is) {
		this.is=is;
		return this;
	}

}
