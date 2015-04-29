package edu.usc.ict.nl.bus.events;

public class DMInterruptionRequest extends DMGeneratedEvent {

	public DMInterruptionRequest(Event sourceEvent, Long sid,NLGEvent payload) {
		super(sourceEvent, "interrupt", sid, payload);
	}

	@Override
	public NLGEvent getPayload() {
		return (NLGEvent) super.getPayload();
	}
	
	public DMInterruptionRequest clone(DMInterruptionRequest sourceEvent) {
		return new DMInterruptionRequest(sourceEvent.getSourceEvent(),sourceEvent.getSessionID(),sourceEvent.getPayload());
	}
	
}
