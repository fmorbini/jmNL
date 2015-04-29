package edu.usc.ict.nl.pml;

import java.util.HashMap;
import java.util.Map;

public class PMLVar {
	public static final String FREQUENCY_SUFFIX="DMFrequency";
	private int currentIndex;
	private Object[] history;
	private String baseName;
	private Object value=null;
	private Object keyForGetFrequency=null;
	private boolean changed=false;

	public PMLVar(String name,int window) {
		this.baseName=name;
		this.history=(window>0)?new Object[window]:null;
		this.currentIndex=0;
	}
	public void setKeyForFrequency(Object key) {this.keyForGetFrequency=key;}
	
	public boolean hasChanged() {return changed;}
	public void setValue(Object value) {
		Object oldValue=this.value;
		boolean vC=different(oldValue,value),hC=false;
		this.value=value;
		if (hasHistory()) {
			hC=different(history[currentIndex],value);
			history[currentIndex]=value;
			currentIndex=((currentIndex+1)%history.length);
		}
		changed=vC || hC;
	}
	private boolean different(Object oldValue, Object newValue) {
		return ((oldValue==null && newValue!=null) || (oldValue!=null && !oldValue.equals(newValue)));
	}
	public Float getFrequency(boolean redistributeNullWeight) {return getFrequencyForKey(keyForGetFrequency, redistributeNullWeight);}
	public Float getFrequency() {return getFrequency(true);}
	public Float getFrequencyForKey(Object key,boolean redistributeNullWeight) {
		Map<Object, Float> freqMap = computeFrequency();
		if (freqMap!=null) {
			Float f = freqMap.get(key);
			Float nullF=freqMap.get(null);
			if (f!=null) {
				if (nullF!=null) {
					int nonNullSize=freqMap.keySet().size()-1;
					assert(nonNullSize>=1);
					return f+(nullF/(float)nonNullSize);
				} else return f;
			} else return 0f;
		}
		return null;
	}

	private Map<Object,Float> computeFrequency() {
		Map<Object,Float> ret=null;
		if (hasHistory()) {
			if (ret==null) ret=new HashMap<Object, Float>();
			for(Object v:history) {
				Float f=ret.get(v);
				if (f==null) ret.put(v,1f);
				else ret.put(v,f+1);
			}
			for(Object k:ret.keySet()) ret.put(k, ret.get(k)/(float)history.length);
		}
		return ret;
	}

	public String getName() {return baseName;}
	public Object getValue() {return value;}
	public boolean hasHistory() {return history!=null;}
	public String getFrequencyVarName() {return baseName+FREQUENCY_SUFFIX;}
}
