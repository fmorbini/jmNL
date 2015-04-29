package edu.usc.ict.nl.kb;

import edu.usc.ict.nl.util.Triple;

public class Change extends Triple<String,Object,Object> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public Change(String name, Object oldValue, Object newValue) {
		super(name,oldValue,newValue);
	}
	
	public Object getOldValue() {return getSecond();}
	public Object getNewValue() {return getThird();}
	public String getName() {return getFirst();}

}
