package edu.usc.ict.nl.nlu.ne;

public class NE {
	private String matchedString;
	private int start,end;
	private Object value;
	private String varName;
	private String type;
	private NamedEntityExtractorI extractor;
	
	public static final String INFOSTATE="informationStateNamedEntity";
	
	public NE(String varName,Object value,String type,int start,int end,String match,NamedEntityExtractorI ext) {
		this.varName=varName;
		this.value=value;
		this.type=type;
		this.start=start;
		this.end=end;
		this.matchedString=match;
		this.extractor=ext;
	}
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
		return getVarName()+"="+getValue()+" ("+getStart()+","+getEnd()+","+getMatchedString()+")";
	}

	public NamedEntityExtractorI getExtractor() {
		return extractor;
	}
}
