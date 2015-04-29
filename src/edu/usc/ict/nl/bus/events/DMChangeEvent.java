package edu.usc.ict.nl.bus.events;

import edu.usc.ict.nl.kb.Change;


public class DMChangeEvent extends DMGeneratedEvent {

	public DMChangeEvent(Event se, Long sid, Change c) {
		super(se,(c!=null)?c.getName():null, sid, c);
	}
	
	@Override
	public Change getPayload() {
		return (Change) super.getPayload();
	}
	
	public DMChangeEvent clone(DMChangeEvent sourceEvent) {
		return new DMChangeEvent(sourceEvent.getSourceEvent(),sourceEvent.getSessionID(),sourceEvent.getPayload());
	}

}
