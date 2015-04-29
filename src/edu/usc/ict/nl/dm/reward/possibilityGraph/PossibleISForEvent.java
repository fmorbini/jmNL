package edu.usc.ict.nl.dm.reward.possibilityGraph;

import edu.usc.ict.nl.bus.events.Event;
import edu.usc.ict.nl.dm.reward.DormantActions;
import edu.usc.ict.nl.dm.reward.model.DialogueOperator;
import edu.usc.ict.nl.kb.DialogueKB;

public class PossibleISForEvent extends PossibleIS {
	public PossibleISForEvent(Event ev, DialogueOperator op, Type type, String name,
			DialogueKB is, DormantActions dormantActions) {
		super(op, type, name, is, dormantActions);
		this.inputEvent=ev;
	}

	Event inputEvent=null;

	public Event getInputEvent() {return inputEvent;}
}