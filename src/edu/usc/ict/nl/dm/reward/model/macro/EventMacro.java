package edu.usc.ict.nl.dm.reward.model.macro;

public class EventMacro extends Macro {
	private String substitution;
	
	public EventMacro(String eventName, String substitution) {
		this.name=eventName;
		this.substitution=substitution;
	}

	public String getSubstitution() {
		return substitution;
	}
}
