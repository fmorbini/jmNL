package edu.usc.ict.nl.bus.events;


public abstract class DMGeneratedEvent extends Event {

	private Event sourceEvent;
	
	public DMGeneratedEvent(Event sourceEvent,String event, Long sid, Object payload) {
		super(event,sid,payload);
		this.sourceEvent=sourceEvent;
	}
	
	public Event getSourceEvent() {return sourceEvent;}
	public void setSourceEvent(Event sourceEvent) {
		this.sourceEvent = sourceEvent;
	}
}
