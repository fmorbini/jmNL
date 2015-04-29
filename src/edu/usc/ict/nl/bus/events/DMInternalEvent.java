package edu.usc.ict.nl.bus.events;


public class DMInternalEvent extends DMGeneratedEvent {

	public static final Event INIT = new DMInternalEvent("init", null);

	public DMInternalEvent(Event sourceEvent, String event, Long sid, Object payload) {
		super(sourceEvent, event, sid, payload);
	}
	public DMInternalEvent(String event, Long sid) {
		this(null, event, sid, null);
	}
	
	public DMInternalEvent clone(DMInternalEvent sourceEvent) {
		return new DMInternalEvent(sourceEvent.getSourceEvent(),sourceEvent.getName(),sourceEvent.getSessionID(),sourceEvent.getPayload());
	}

}
