package edu.usc.ict.nl.dm.reward.possibilityGraph;

import edu.usc.ict.nl.dm.reward.model.DialogueOperator;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEntranceTransition;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorNodesChain;
import edu.usc.ict.nl.util.graph.Edge;

public class PossibleTransition extends Edge {
	private DialogueOperatorNodesChain path;
	private DialogueOperatorEntranceTransition entraceCondition;
	private double reward=0;

	public PossibleTransition(DialogueOperatorEntranceTransition ec,DialogueOperatorNodesChain path) {
		this.path=path;
		this.entraceCondition=ec;
	}

	public DialogueOperatorEntranceTransition getEntranceCondition() {return entraceCondition;}
	public DialogueOperator getOperator() {return entraceCondition.getOperator();}
	public DialogueOperatorNodesChain getPath() {return path;}
	
	@Override
	public String toString() {
		return toString(true);
	}
	public String toString(boolean shortForm) {
		if (shortForm) return getID()+"("+reward+")";//+": "+entraceCondition+"("+reward+")";
		else return "["+getSource()+"-"+entraceCondition+"("+reward+")"+"->"+getTarget()+"]";
	}

	public void setReward(double reward) { this.reward=reward;}
	public double getReward() {return reward;}
}
