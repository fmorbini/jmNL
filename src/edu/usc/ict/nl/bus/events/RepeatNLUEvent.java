package edu.usc.ict.nl.bus.events;

public class RepeatNLUEvent extends NLUEvent {

	public RepeatNLUEvent(NLUEvent sourceEvent) {
		super(sourceEvent.getName(),sourceEvent.getSessionID(),sourceEvent.getPayload());
	}

}
