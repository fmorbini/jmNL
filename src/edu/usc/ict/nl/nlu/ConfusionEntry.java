package edu.usc.ict.nl.nlu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfusionEntry implements Comparable<ConfusionEntry> {
	private String label;
	private Map<String,List<String>> confusedWith;
	private int totalCount=0;
	public ConfusionEntry(String label) {
		this.setLabel(label);
	}
	public void addResult(String resultLabel, String utterance) {
		setTotalCount(getTotalCount() + 1);
		if (!getLabel().equals(resultLabel)) {
			if (getConfusedWith()==null) setConfusedWith(new HashMap<String, List<String>>());
			List<String> us=getConfusedWith().get(resultLabel);
			if (us==null) getConfusedWith().put(resultLabel, us=new ArrayList<String>());
			us.add(utterance);
		}
	}
	public int getTotalErrors() {
		int totalErrors=0;
		if (getConfusedWith()!=null) for(List<String> us:getConfusedWith().values()) {
			int c=(us!=null)?us.size():0; 
			totalErrors+=c;
		}
		return totalErrors;
	}
	public float getErrorPercentage() {
		return (getTotalCount()!=0)?(float)getTotalErrors()/(float)getTotalCount():0;
	}
	public List<String> getSortedConfusedWithSAs() {
		List<String> ret=null;
		if (getConfusedWith()!=null) {
			ret=new ArrayList<String>(getConfusedWith().keySet());
			Collections.sort(ret, new Comparator<String>() {
				@Override
				public int compare(String a0, String a1) {
					List<String> tmp = getConfusedWith().get(a0);
					int a0c=(tmp!=null)?tmp.size():0;
					tmp=getConfusedWith().get(a1);
					int a1c=(tmp!=null)?tmp.size():0;
					return a1c-a0c;
				}
			});
		}
		return ret;
	}
	@Override
	public int compareTo(ConfusionEntry o) {
		//float thisR=getErrorPercentage(),otherR=o.getErrorPercentage();
		float thisR=getTotalErrors(),otherR=o.getTotalErrors();
		return (thisR<otherR)?1:(thisR==otherR)?0:-1;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public int getTotalCount() {
		return totalCount;
	}
	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}
	public Map<String,List<String>> getConfusedWith() {
		return confusedWith;
	}
	public void setConfusedWith(Map<String,List<String>> confusedWith) {
		this.confusedWith = confusedWith;
	}
}