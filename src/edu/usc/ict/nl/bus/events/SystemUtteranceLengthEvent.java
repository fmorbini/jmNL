package edu.usc.ict.nl.bus.events;


public class SystemUtteranceLengthEvent extends Event {

	public SystemUtteranceLengthEvent(String name, Long sid, Float length) {
		super(name, sid, length);
	}

	@Override
	public Float getPayload() {
		return (Float) super.getPayload();
	}
	
	public SystemUtteranceLengthEvent clone(SystemUtteranceLengthEvent sourceEvent) {
		return new SystemUtteranceLengthEvent(sourceEvent.getName(),sourceEvent.getSessionID(),sourceEvent.getPayload()); 
	}

}
