package edu.usc.ict.nl.nlu.chart;

import java.util.ArrayList;
import java.util.List;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.util.Rational;

public class PartialClassification {
	private String text;
	private int start;
	private int end;
	private List<NLUOutput> result;
	
	public PartialClassification(int start,String text) {
		this.text=text;
		this.setStart(start);
		this.setEnd(start+1);
	}
	public PartialClassification(int start, int end, String text, List<NLUOutput> result) {
		this.text=text;
		this.setStart(start);
		this.setEnd(end);
		this.result=result;
	}
	public PartialClassification() {}
	public PartialClassification reset(int start, int end, String text) {
		this.text=text;
		this.setStart(start);
		this.setEnd(end);
		this.result=null;
		return this;
	}
	public PartialClassification resetFrom(PartialClassification from) {
		this.text=from.text;
		this.start=from.start;
		this.end=from.end;
		this.result=from.result;
		return this;
	}

	public void addWord(String word) {
		text+=" "+word;
		setEnd(getEnd() + 1);
	}
	public boolean classifyWith(NLU nlu) {
		try {
			result=nlu.getNLUOutput(text, null,null);
			return (result!=null) && !result.isEmpty();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	public String toString() {
		return "["+getStart()+" "+getEnd()+"] "+text+" -> "+result;
	}
	@Override
	protected PartialClassification clone() throws CloneNotSupportedException {
		return new PartialClassification(getStart(),getEnd(),text,result);
	}
	public void updateFrom(PartialClassification source) {
		this.setStart(source.getStart());
		this.setEnd(source.getEnd());
		this.text=source.text;
		this.result=source.result;
	}
	public Rational getProbTopResult() {
		if ((result!=null) && !result.isEmpty()) return result.get(0).getProb();
		else return Rational.zero;
	}
	public List<NLUOutput> getResult() {return result;}
	public void setEnd(int end) {this.end = end;}
	public int getEnd() {return end;}
	public void setStart(int start) {this.start = start;}
	public int getStart() {return start;}
	public String getText() {return text;}

	public void clearResult() {
		List<NLUOutput> rs = getResult();
		if (rs!=null) rs.clear();
	}
	public void setTopResult(NLUOutput tr) {
		List<NLUOutput> rs = getResult();
		if (rs!=null) {
			if (rs.isEmpty()) rs.add(tr);
			else rs.set(0, tr);
		} else {
			this.result=new ArrayList<NLUOutput>();
			this.result.add(tr);
		}
	}
	public NLUOutput getTopResult() {if (getResult()!=null && !getResult().isEmpty()) return getResult().get(0); else return null;}
}
