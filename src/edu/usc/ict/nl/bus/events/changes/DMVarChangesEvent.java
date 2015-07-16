package edu.usc.ict.nl.bus.events.changes;

import java.util.Collection;

import edu.usc.ict.nl.bus.events.DMGeneratedEvent;
import edu.usc.ict.nl.bus.events.Event;


public class DMVarChangesEvent extends DMGeneratedEvent {

	public DMVarChangesEvent(Event se, Long sid, Collection<VarChange> cs) {
		super(se,"set-of-changes", sid, cs);
	}
	
	@Override
	public Collection<VarChange> getPayload() {
		return (Collection) super.getPayload();
	}
	
	public DMVarChangesEvent clone(DMVarChangesEvent sourceEvent) {
		return new DMVarChangesEvent(sourceEvent.getSourceEvent(),sourceEvent.getSessionID(),sourceEvent.getPayload());
	}

}
