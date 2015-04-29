package edu.usc.ict.nl.bus.events;

import edu.usc.ict.nl.kb.DialogueKBInterface;

public class DMSpeakEvent extends DMGeneratedEvent {

	private DialogueKBInterface localInformationState=null;

	public DMSpeakEvent(Event sourceEvent,String name, Long sid, Object payload,DialogueKBInterface customKB) {
		super(sourceEvent,name, sid, payload);
		this.localInformationState=customKB;
	}

	public DialogueKBInterface getLocalInformationState() {return localInformationState;}
	public void setLocalInformationState(
			DialogueKBInterface localInformationState) {
		this.localInformationState = localInformationState;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	public DMSpeakEvent clone(DMSpeakEvent sourceEvent) {
		return new DMSpeakEvent(sourceEvent.getSourceEvent(),sourceEvent.getName(),sourceEvent.getSessionID(),sourceEvent.getPayload(),sourceEvent.getLocalInformationState());
	}

}
