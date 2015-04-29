package edu.usc.ict.nl.kb;

import java.util.HashMap;
import java.util.Map;

public class VariableProperties {
	public enum PROPERTY {READONLY,HIDDEN,PERSISTENT};
	private Map<PROPERTY,Boolean> ps;
	public static final VariableProperties defaultProperties=new VariableProperties();
	static {
		for(PROPERTY p:PROPERTY.values()) {
			defaultProperties.setProperty(p, getDefault(p));
		}
	}
	public static boolean getDefault(PROPERTY p) {
		switch (p) {
		case READONLY: return false;
		case HIDDEN: return false;
		case PERSISTENT: return true;
		}
		return false;
	}
	public static boolean isDefault(PROPERTY p,boolean v) {
		return (getDefault(p)==v);
	}
	
	public void setProperty(PROPERTY p,boolean v) {
		if (!isDefault(p, v)) {
			if (ps==null) ps=new HashMap<VariableProperties.PROPERTY, Boolean>();
			ps.put(p, v);
		}
	}
	
	public boolean getProperty(PROPERTY p) {
		Boolean ret;
		if (ps!=null && ((ret=ps.get(p))!=null)) {
			return ret;
		} else return getDefault(p);
	}
}
