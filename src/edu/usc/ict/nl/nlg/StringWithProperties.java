package edu.usc.ict.nl.nlg;

import java.util.HashMap;
import java.util.Map;

import edu.usc.ict.nl.util.StringUtils;

public class StringWithProperties {

	private String text;
	private Map<String,Object> properties;
	
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public void setProperty(String pname,Object pvalue) {
		if (!StringUtils.isEmptyString(pname)) {
			if (properties==null) properties=new HashMap<String,Object>();
			properties.put(pname, pvalue);
		}
	}
	public Object getProperty(String pname) {
		if (!StringUtils.isEmptyString(pname) && properties!=null) return properties.get(pname);
		return null;
	}
	
	@Override
	public String toString() {
		return getText();
	}

}
