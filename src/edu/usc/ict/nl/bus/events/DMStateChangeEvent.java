package edu.usc.ict.nl.bus.events;

import edu.usc.ict.nl.kb.Change;


public class DMStateChangeEvent extends DMGeneratedEvent {

	public DMStateChangeEvent(Event se, Long sid, Change c) {
		super(se,(c!=null)?c.getName():null, sid, c);
	}
	
	@Override
	public Change getPayload() {
		return (Change) super.getPayload();
	}
	
	public DMStateChangeEvent clone(DMStateChangeEvent sourceEvent) {
		return new DMStateChangeEvent(sourceEvent.getSourceEvent(),sourceEvent.getSessionID(),sourceEvent.getPayload());
	}

}
