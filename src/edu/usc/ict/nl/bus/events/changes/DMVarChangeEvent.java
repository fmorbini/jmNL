package edu.usc.ict.nl.bus.events.changes;

import edu.usc.ict.nl.bus.events.DMGeneratedEvent;
import edu.usc.ict.nl.bus.events.Event;


public class DMVarChangeEvent extends DMGeneratedEvent {

	public DMVarChangeEvent(Event se, Long sid, VarChange c) {
		super(se,(c!=null)?c.getName():null, sid, c);
	}
	
	@Override
	public VarChange getPayload() {
		return (VarChange) super.getPayload();
	}
	
	public DMVarChangeEvent clone(DMVarChangeEvent sourceEvent) {
		return new DMVarChangeEvent(sourceEvent.getSourceEvent(),sourceEvent.getSessionID(),sourceEvent.getPayload());
	}

}
