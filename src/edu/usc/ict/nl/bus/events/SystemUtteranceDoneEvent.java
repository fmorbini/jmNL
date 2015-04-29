package edu.usc.ict.nl.bus.events;



public class SystemUtteranceDoneEvent extends Event {

	public SystemUtteranceDoneEvent(String name, Long sid) {
		super(name, sid, null);
	}
	
	public SystemUtteranceDoneEvent clone(SystemUtteranceDoneEvent sourceEvent) {
		return new SystemUtteranceDoneEvent(sourceEvent.getName(),sourceEvent.getSessionID());
	}
}
