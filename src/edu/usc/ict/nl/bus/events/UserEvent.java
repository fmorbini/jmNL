package edu.usc.ict.nl.bus.events;


public abstract class UserEvent extends Event {

	public UserEvent(String name, Long sid, Object payload) {
		super(name, sid, payload);
	}
}
