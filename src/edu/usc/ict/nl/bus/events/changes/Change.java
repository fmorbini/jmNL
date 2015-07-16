package edu.usc.ict.nl.bus.events.changes;

public interface Change {
	public Object getOldValue();
	public Object getNewValue();
	public String getName();

}
