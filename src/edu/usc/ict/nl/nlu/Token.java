package edu.usc.ict.nl.nlu;


public class Token {
	public enum TokenTypes {WORD,NUM,OTHER,O1};
	String name,original;
	TokenTypes type;
	private int start,end;

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
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public TokenTypes getType() {
		return type;
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
	
	@Override
	public String toString() {
		return "["+getName()+"("+getOriginal()+"): "+getType()+"]";
	}
}
