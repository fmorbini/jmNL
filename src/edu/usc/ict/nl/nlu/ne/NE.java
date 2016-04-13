package edu.usc.ict.nl.nlu.ne;

import java.util.ArrayList;
import java.util.List;

public class NE {
	private String matchedString;
	private int start,end;
	//private Object value;
	//private String varName;
	private String type;
	private NamedEntityExtractorI extractor;
	private List<Var> variables=null;
	
	public class Var {
		String vname;
		Object value;
		public Var(String vname,Object value) {
			this.value=value;
			this.vname=vname;
		}
		public Object getValue() {
			return value;
		}
		public String getName() {
			return vname;
		}
		@Override
		public String toString() {
			return getName()+"="+getValue();
		}
	}
	
	//public static final String INFOSTATE="informationStateNamedEntity";
	
	/**
	 * 
	 * @param varName
	 * @param value
	 * @param type the NE type (usually the name of the extractor, like number)
	 * @param start
	 * @param end
	 * @param match
	 * @param ext
	 */
	public NE(String varName,Object value,String type,int start,int end,String match,NamedEntityExtractorI ext) {
		this.type=type;
		this.start=start;
		this.end=end;
		this.matchedString=match;
		this.extractor=ext;
		addVariable(varName, value);
	}
	
	public void addVariable(String vname,Object value) {
		if (variables==null) variables=new ArrayList<>();
		variables.add(new Var(vname, value));
	}
	/*
	public NE(String varName,Object value, NamedEntityExtractorI ext) {
		this(varName,value,INFOSTATE,-1,-1,null,ext);
	}

	public boolean isInfoState() {
		return getType()==INFOSTATE;
	}
	public Object getValue() {
		return value;
	}
	public String getVarName() {
		return varName;
	}
	*/
	public List<Var> getVariables() {
		return variables;
	}
	public int getStart() {
		return start;
	}
	public int getEnd() {
		return end;
	}
	
	public String getType() {
		return type;
	}
	
	public String getMatchedString() {
		return matchedString;
	}
	
	@Override
	public String toString() {
		String variables=getVariables()!=null?getVariables().toString():"";
		return "<ne><vars>"+variables+"</vars><start="+getStart()+"><end="+getEnd()+"><string="+getMatchedString()+"></ne>";
	}

	public NamedEntityExtractorI getExtractor() {
		return extractor;
	}
}
