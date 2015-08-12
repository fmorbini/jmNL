package edu.usc.ict.nl.nlu.wikidata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.graph.Node;

public class WikiThing extends Node implements Comparable<WikiThing> {

	private TYPE type;
	private List<String> labels;
	private String desc=null;
	private long id=-1;
	public enum TYPE {ITEM,PROPERTY,CONSTANT};
	
	private static final Pattern thingName=Pattern.compile("^(P|Q)([0-9]+)$");
	
	public WikiThing(long id,TYPE type) {
		this.id=id;
		this.type=type;
		setName((isEntity()?"Q":"P")+id);
	}
	
	public WikiThing(String string) throws Exception {
		Matcher m=thingName.matcher(string);
		if (m.matches()) {
			this.id=Long.parseLong(m.group(2));
			String t=m.group(1);
			if (t.equals("P")) this.type=TYPE.PROPERTY;
			else this.type=TYPE.ITEM;
			setName((isEntity()?"Q":"P")+id);
		} else {
			this.type=TYPE.CONSTANT;
			setName(string);
		}
	}

	public long getId() {
		return id;
	}
	
	public boolean isEntity() {
		return type!=null && type==TYPE.ITEM;
	}
	public boolean isProperty() {
		return type!=null && type==TYPE.PROPERTY;
	}
	public void setLabels(List<String> label) {
		this.labels = label;
	}
	public void addLabel(String label) {
		if (!StringUtils.isEmptyString(label)) {
			if (labels==null) labels=new ArrayList<String>();
			labels.add(label);
		}
	}
	public List<String> getLabels() {
		return labels;
	}
	public String getDesc() {
		return desc;
	}
	public void setDesc(String desc) {
		this.desc = desc;
	}
	@Override
	public String toString() {
		return getName()+":"+getLabels();
	}
	
	public String toString(WikiLanguage lang,boolean longForm) {
		String base=getName();
		if (longForm && (isEntity() || isProperty())) {
			JSONObject content=Wikidata.getWikidataContentForSpecificEntityOnly(lang,base);
			String desc=(getDesc()!=null)?getDesc():Wikidata.getDescriptionForContent(content,lang);
			List<String> labels=getLabels()!=null?getLabels():Wikidata.getLabelsForContent(content,lang);
			desc=StringUtils.cleanupSpaces(desc);
			String ret=base;
			if (!StringUtils.isEmptyString(desc)) ret+=" ("+desc+")";
			if (labels!=null) {
				boolean first=true;
				for(String l:labels) {
					if (!StringUtils.isEmptyString(l)) ret+=(first?": ":", ")+l;
					first=false;
				}
			}
		}
		return base;
	}
	
	@Override
	public String gdlText() {
		try {
			return "node: { shape: "+getShape()+" title: \""+getID()+"\" label: \""+toString()+"\" info1: \""+toString(WikiLanguage.get("en"),true)+"\"}\n";
		} catch (Exception e) {
			return "node: { shape: "+getShape()+" title: \""+getID()+"\" label: \""+toString()+"\" info1: \""+Arrays.toString(e.getStackTrace())+"\"}\n";
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj!=null && obj instanceof WikiThing) {
			return ((WikiThing)obj).getName().equals(getName()); 
		} else return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	@Override
	public int compareTo(WikiThing o) {
		return getName().compareTo(o.getName());
	}
}
