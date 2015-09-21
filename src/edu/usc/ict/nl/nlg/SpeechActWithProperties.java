package edu.usc.ict.nl.nlg;

import java.util.HashMap;
import java.util.Map;

import edu.usc.ict.nl.bus.modules.NLG;
import edu.usc.ict.nl.util.StringUtils;

public class SpeechActWithProperties {

	private String text;
	private int row;
	private String speechact;
	private Map<String,String> properties;

	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public void setRow(int rownum) {
		row=rownum;
	}
	public void setSA(String sa) {
		this.speechact=sa;
	}
	public String getSA() {
		return speechact;
	}
	public int getRow() {
		return row;
	}
	public void setProperty(String pname,String pvalue) {
		if (!StringUtils.isEmptyString(pname)) {
			pname=normalize(pname);
			switch (pname) {
			case NLG.PROPERTY_ROW:
				row=Integer.parseInt(pvalue);
				break;
			case NLG.PROPERTY_SA:
				speechact=pvalue;
				break;
			default:
				if (properties==null) properties=new HashMap<String,String>();
				properties.put(pname, pvalue);
			}
		}
	}

	private String normalize(String pname) {
		return (pname!=null)?pname.toLowerCase():pname; 
	}
	
	public String getProperty(String pname) {
		if (!StringUtils.isEmptyString(pname)) {
			pname=normalize(pname);
			switch (pname) {
			case NLG.PROPERTY_ROW:
				return row+"";
			case NLG.PROPERTY_SA:
				return speechact;
			default:
				if (properties!=null) return properties.get(pname);
			}
		}
		return null;
	}

	public Boolean isUsed() {
		Boolean r=Boolean.parseBoolean(getProperty(NLG.PROPERTY_USED));
		return r;
	}
	
	@Override
	public String toString() {
		return toString(false);
	}
	public String toString(boolean longform) {
		if (longform) {
			Boolean used=isUsed();
			return (used!=null?(used?"* ":""):"")+getRow()+","+getSA()+": "+getText();
		} else return getText();
	}

}
