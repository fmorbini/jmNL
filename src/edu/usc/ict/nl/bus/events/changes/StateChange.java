package edu.usc.ict.nl.bus.events.changes;

import edu.usc.ict.nl.util.Triple;

public class StateChange extends Triple<String,Object,Object> implements Change {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public StateChange(String name,Object oldValue, Object newValue) {
		super(name,oldValue,newValue);
	}
	
	public Object getOldValue() {return getSecond();}
	public Object getNewValue() {return getThird();}
	public String getName() {return getFirst();}

}
