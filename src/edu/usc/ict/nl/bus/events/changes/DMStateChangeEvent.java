package edu.usc.ict.nl.bus.events.changes;

import edu.usc.ict.nl.bus.events.DMGeneratedEvent;
import edu.usc.ict.nl.bus.events.Event;


public class DMStateChangeEvent extends DMGeneratedEvent {

	public DMStateChangeEvent(Event se, Long sid, StateChange c) {
		super(se,(c!=null)?c.getName():null, sid, c);
	}
	
	@Override
	public StateChange getPayload() {
		return (StateChange) super.getPayload();
	}
	
	public DMStateChangeEvent clone(DMStateChangeEvent sourceEvent) {
		return new DMStateChangeEvent(sourceEvent.getSourceEvent(),sourceEvent.getSessionID(),sourceEvent.getPayload());
	}

}
