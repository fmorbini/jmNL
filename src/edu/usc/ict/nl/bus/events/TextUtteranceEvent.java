package edu.usc.ict.nl.bus.events;

public class TextUtteranceEvent extends UserEvent {

	String speaker;
	
	public TextUtteranceEvent(String text, Long sid, String speaker) {
		super(text, sid, null);
		this.speaker=speaker;
	}
	
	public String getSpeaker() {
		return speaker;
	}

	/**
	 * @return returns the text content of this message
	 */
	public String getText() {
		return getName();
	}

}
