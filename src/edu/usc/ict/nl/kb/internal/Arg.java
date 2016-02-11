package edu.usc.ict.nl.kb.internal;

public class Arg {
	private int pos=0;
	private Object value;
	
	public Arg(int pos,Object v) {
		setPos(pos);
		setValue(v);
	}
	
	public Object getValue() {
		return value;
	}
	public void setValue(Object value) {
		this.value = value;
	}
	public int getPos() {
		return pos;
	}
	public void setPos(int pos) {
		this.pos = pos;
	}
	
	@Override
	public String toString() {
		return getPos()+":"+getValue();
	}
}
