package edu.usc.ict.nl.bus.events;


public class PMLEvent extends UserEvent {

	public PMLEvent(String name, Long sid, Object payload) {
		super(name, sid, payload);
	}
	
	public PMLEvent clone(PMLEvent sourceEvent) {
		return new PMLEvent(sourceEvent.getName(), sourceEvent.getSessionID(), sourceEvent.getPayload());
	}

}
