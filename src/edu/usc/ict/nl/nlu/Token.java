package edu.usc.ict.nl.nlu;

import edu.usc.ict.nl.nlu.ne.NE;

public class Token implements Comparable<Token> {
	public enum TokenTypes {WORD,NUM,OTHER};
	private String name,original;
	private TokenTypes type;
	private int start,end;
	private NE associatedNamedEntity;

	public Token(String name, TokenTypes type,String original,int start,int end) {
		setName(name);
		setType(type);
		setOriginal(original);
		this.start=start;
		this.end=end;
	}
	public Token(String name, TokenTypes type,String original) {
		this(name, type, original, -1, -1);
	}
	
	public void setAssociatedNamedEntity(NE associatedNamedEntity) {
		this.associatedNamedEntity = associatedNamedEntity;
	}
	public NE getAssociatedNamedEntity() {
		return associatedNamedEntity;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public TokenTypes getType() {
		return type;
	}
	public boolean isType(TokenTypes type) {
		return getType()==type;
	}
	public void setType(TokenTypes type) {
		this.type = type;
	}
	public String getOriginal() {
		return original;
	};
	public void setOriginal(String original) {
		this.original = original;
	}
	public int getStart() {
		return start;
	}
	public int getEnd() {
		return end;
	}
	public void setEnd(int end) {
		this.end = end;
	}
	public void setStart(int start) {
		this.start = start;
	}
	@Override
	public String toString() {
		return "<"+getName()+"("+getOriginal()+"): "+getType()+(getAssociatedNamedEntity()!=null?", NE: "+getAssociatedNamedEntity():"")+">";
	}
	@Override
	public int compareTo(Token o) {
		return start-o.start;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj!=null && obj instanceof Token) {
			return (((Token)obj).start==start && ((Token)obj).end==end && 
					((((Token)obj).name!=null && ((Token)obj).name.equals(name)) ||
							((Token)obj).name==name) &&
					((((Token)obj).original!=null && ((Token)obj).original.equals(original)) ||
							((Token)obj).original==original) &&
					((Token)obj).type==type);
		}
		return super.equals(obj);
	}
	
	public boolean overlaps(Token t2) {
		if (t2!=null) {
			return ((getStart()<=t2.getEnd() && getStart()>=t2.getStart()) || (getEnd()<=t2.getEnd() && getEnd()>=t2.getStart()) || (getStart()<=t2.getStart() && getEnd()>=t2.getEnd()));
		}
		return false;
	}

}
