package edu.usc.ict.nl.dm.reward.possibilityGraph;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import edu.usc.ict.nl.dm.reward.model.DialogueAction;
import edu.usc.ict.nl.dm.reward.model.DialogueOperator;
import edu.usc.ict.nl.util.graph.Node;

public class OperatorHistoryNode extends Node {
	private DialogueOperator op=null;
	private boolean done=false;
	
	public OperatorHistoryNode() {
	}
	private OperatorHistoryNode(DialogueAction action) {
		this((action!=null)?action.getOperator():null,
				(action!=null)?action.getDone():false);
	}
	protected OperatorHistoryNode(DialogueOperator op, boolean isDone) {
		set(op,isDone);
	}
	private void set(DialogueOperator op, boolean isDone) {
		setOperator(op);
		// if this history node was not already done, update it with the done status of the provided action.
		if (!getDone()) setDone(isDone);
	}
	
	/**
	 * creates a new child node if action is a new action (compared to this (i.e. the parent)
	 * if action is done and this is the same action (i.e. the same operator) but not done, then it simply update the state to done
	 * it return the most recent history node (new or same as this) is any changes have been done.
	 * @param action
	 * @return
	 * @throws Exception
	 */
	public OperatorHistoryNode update(DialogueAction action) throws Exception {
		if (action!=null && action.getOperator()!=null) {
			DialogueOperator no = action.getOperator();
			DialogueOperator thiso = getOperator();
			OperatorHistoryNode newhn=this;
			if (thiso!=no && thiso!=null) {
				newhn = new OperatorHistoryNode(action);
				this.addEdgeTo(newhn, true, true);
			} else if (action.getDone()!=getDone() || thiso==null) {
				set(no, action.getDone());
			}
			return newhn;
		}
		return null;
	}
	
	public DialogueOperator getOperator() {return this.op;}
	private void setOperator(DialogueOperator op) {
		this.op = op;
		if (op!=null) setName(op.toString(true));
	}
	
	public OperatorHistoryNode findOperator(String name) throws Exception {
		return findOperator(name, false);
	}
	public OperatorHistoryNode findOperator(String name,boolean completed) throws Exception {
		Set<OperatorHistoryNode> visited=new HashSet<OperatorHistoryNode>();
		LinkedList<OperatorHistoryNode> q=new LinkedList<OperatorHistoryNode>();
		q.push(this);
		while(!q.isEmpty()) {
			OperatorHistoryNode n=q.pop();
			if (n.op!=null && n.op.getName().equals(name) && (!completed || n.getDone())) return this;
			else {
				Collection<OperatorHistoryNode> parents = n.getParents();
				if (parents!=null) {
					for(OperatorHistoryNode p:parents) {
						if (!visited.contains(p)) {
							visited.add(p);
							q.add(p);
						}
					}
				}
			}
		}
		return null;
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
