package edu.usc.ict.nl.bus.events;

import java.util.Collection;

import edu.usc.ict.nl.kb.Change;


public class DMChangesEvent extends DMGeneratedEvent {

	public DMChangesEvent(Event se, Long sid, Collection<Change> cs) {
		super(se,"set-of-changes", sid, cs);
	}
	
	@Override
	public Collection<Change> getPayload() {
		return (Collection) super.getPayload();
	}
	
	public DMChangesEvent clone(DMChangesEvent sourceEvent) {
		return new DMChangesEvent(sourceEvent.getSourceEvent(),sourceEvent.getSessionID(),sourceEvent.getPayload());
	}

}
