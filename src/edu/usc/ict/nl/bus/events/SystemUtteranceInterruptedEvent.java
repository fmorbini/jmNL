package edu.usc.ict.nl.bus.events;



public class SystemUtteranceInterruptedEvent extends SystemUtteranceDoneEvent {
	private Event sourceEvent;

	public SystemUtteranceInterruptedEvent(String name, Long sid,Event sourceEvent) {
		super(name, sid);
		this.sourceEvent=sourceEvent;
	}
	
	public Event getSourceEvent() {return sourceEvent;}
	public void setSourceEvent(Event sourceEvent) {
		this.sourceEvent = sourceEvent;
	}
}
