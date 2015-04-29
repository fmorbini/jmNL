package edu.usc.ict.nl.bus.events;


public class NLGEvent extends Event {

	/**
	 * 
	 * @param name: the text of the utterance to generate
	 * @param sid: the session id
	 * @param payload: the original DMSpeakEvent that generated this NLGEvent
	 */
	public NLGEvent(String name, Long sid, DMSpeakEvent payload) {
		super(name, sid, payload);
	}

	@Override
	public DMSpeakEvent getPayload() {
		return (DMSpeakEvent) super.getPayload();
	}
	
	public String getDMEventName() {
		return getPayload().getName();
	}
	
	public NLGEvent clone(NLGEvent sourceEvent) {	
		return new NLGEvent(sourceEvent.getName(), sourceEvent.getSessionID(), sourceEvent.getPayload());
	}
}
