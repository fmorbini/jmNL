package edu.usc.ict.nl.dm.reward.possibilityGraph;

import edu.usc.ict.nl.dm.reward.model.DialogueAction;
import edu.usc.ict.nl.dm.reward.model.DialogueOperator;
import edu.usc.ict.nl.util.graph.Node;

public class OperatorHistoryNode extends Node {
	private DialogueOperator op=null;
	private boolean done=false;
	
	/**
	 * returns a new history node with one parent link to the history node passed as argument.
	 * @param historyOfExecutedOperators
	 * @throws Exception 
	 */
	public OperatorHistoryNode(DialogueAction action,OperatorHistoryNode historyOfExecutedOperators) throws Exception {
		this(action);
		setName((op!=null)?op.getName():null);
		if (historyOfExecutedOperators!=null) historyOfExecutedOperators.addEdgeTo(this, true, true);
	}
	protected OperatorHistoryNode(DialogueAction action) {
		this((action!=null)?action.getOperator():null,
				(action!=null)?action.getDone():false);
	}
	protected OperatorHistoryNode(DialogueOperator op, boolean isDone) {
		update(op,isDone);
	}
	
	public void update(DialogueAction action) {
		update((action!=null)?action.getOperator():null,
				(action!=null)?action.getDone():false);
	}
	public void update(DialogueOperator op, boolean isDone) {
		setOperator(op);
		// if this history node was not already done, update it with the done status of the provided action.
		if (!getDone()) setDone(isDone);
	}
	
	public DialogueOperator getOperator() {return this.op;}
	private void setOperator(DialogueOperator op) {
		this.op = op;
	}
	
	public OperatorHistoryNode findOperator(String name) throws Exception {
		return findOperator(name, false);
	}
	public OperatorHistoryNode findOperator(String name,boolean completed) throws Exception {
		if (op!=null && op.getName().equals(name) && (!completed || getDone())) return this;
		else {
			OperatorHistoryNode parent=(OperatorHistoryNode) getSingleParent();
			if (parent!=null) return parent.findOperator(name,completed);
			else return null;
		}
	}

	public String printChain() throws Exception {
		StringBuffer out=new StringBuffer();
		Node n=this;
		while(n!=null) {
			boolean done=((OperatorHistoryNode)n).getDone();
			out.append("["+n.toString()+(done?"]":"")+"<-");
			n=n.getSingleParent();
		}
		return out.toString();
	}
	
	public boolean getDone() { return done; }
	private void setDone(boolean done) { this.done=done; }
}
