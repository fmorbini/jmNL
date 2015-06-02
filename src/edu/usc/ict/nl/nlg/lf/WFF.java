package edu.usc.ict.nl.nlg.lf;

import java.util.List;

import edu.usc.ict.nl.nlg.lf.utils.NLUUtils;
import edu.usc.ict.nl.util.graph.Node;

public class WFF extends Node {
	private Object nlu=null;
	public enum TYPE {CONJ,NEG,SIMPLE,CNST}
	public WFF(TYPE t) {
		this.type=t;
		switch (type) {
		case CONJ:
			setName("AND");
			break;
		case NEG:
			setName("NOT");
			break;
		}
	}
	public WFF(TYPE t,String n) {
		this(t);
		setName(n);
	}
	private TYPE type=null;
	private List<WFF> arguments;
	
	public boolean isLiteral() {
		return isNegation() || type==TYPE.SIMPLE; 
	}
	public boolean isNegation() {
		return type==TYPE.NEG; 
	}
	public Object getParsedNLUObject(boolean normalizePrimes) {
		if (nlu!=null) return nlu;
		else {
			nlu=NLUUtils.parse(getName(), normalizePrimes,false);
			return nlu;
		}
	}
	
	public void setType(TYPE type) {
		this.type = type;
	}
}
