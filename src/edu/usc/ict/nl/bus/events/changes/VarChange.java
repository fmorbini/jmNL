package edu.usc.ict.nl.bus.events.changes;

import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.util.Pair;

public class VarChange extends Pair<DialogueOperatorEffect,Object> implements Change {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public VarChange(DialogueOperatorEffect newValueAndVar, Object oldValue) {
		super(newValueAndVar,oldValue);
	}
	
	public Object getOldValue() {return getSecond();}
	public Object getNewValue() {return getFirst().getAssignedExpression();}
	public String getName() {return getFirst().getAssignedVariable().getName();}

}
