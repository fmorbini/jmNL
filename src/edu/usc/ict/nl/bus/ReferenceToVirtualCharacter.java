package edu.usc.ict.nl.bus;

public class ReferenceToVirtualCharacter {
	private String name=null;
	private Object payload=null;

	public ReferenceToVirtualCharacter(String n) {
		this.name=n;
	}
	public ReferenceToVirtualCharacter(String n,Object p) {
		this.name=n;
		this.payload=p;
	}

	public String getName() { return name;}
	public Object getPayload() {return payload;}
}
