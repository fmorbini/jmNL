package edu.usc.ict.nl.nlu.wikidata;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.graph.Node;

public class WikiThing extends Node {

	private TYPE type;
	private String label;
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
	public void setLabel(String label) {
		this.label = label;
	}
	public String getLabel() {
		return label;
	}
	@Override
	public String toString() {
		return getName()+":"+getLabel();
	}
	
	public String toString(WikiLanguage lang,boolean longForm) {
		String base=getName();
		if (longForm && (isEntity() || isProperty())) {
			JSONObject content=Wikidata.getWikidataContentForSpecificEntityOnly(lang,base);
			String desc=Wikidata.getDescriptionForContent(content,lang);
			String label=getLabel()!=null?getLabel():Wikidata.getLabelsForContent(content,lang);
			desc=StringUtils.cleanupSpaces(desc);
			label=StringUtils.cleanupSpaces(label);
			if (!StringUtils.isEmptyString(label)||!StringUtils.isEmptyString(desc)) return base+": "+label+" ("+desc+")";
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

}
