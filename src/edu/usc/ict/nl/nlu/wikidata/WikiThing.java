package edu.usc.ict.nl.nlu.wikidata;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONObject;

import edu.usc.ict.nl.util.StringUtils;

public class WikiThing {

	private TYPE type;
	private long id=-1;
	public enum TYPE {ITEM,PROPERTY};
	
	private static final Pattern thingName=Pattern.compile("^(P|Q)([0-9]+)$");
	
	public WikiThing(long id,TYPE type) {
		this.id=id;
		this.type=type;
	}
	
	public WikiThing(String string) throws Exception {
		Matcher m=thingName.matcher(string);
		if (m.matches()) {
			this.id=Long.parseLong(m.group(2));
			String t=m.group(1);
			if (t.equals("P")) this.type=TYPE.PROPERTY;
			else this.type=TYPE.ITEM;
		} else throw new Exception ("unandled string case: "+string);
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
	
	@Override
	public String toString() {
		return toString("en",true);
	}
	
	public String toString(String lang,boolean longForm) {
		String base=(isEntity()?"Q":"P")+id;
		if (longForm) {
			JSONObject content=Wikidata.getWikidataContentForSpecificEntityOnly(lang,base);
			String desc=Wikidata.getDescriptionForContent(content,lang);
			String label=Wikidata.getLabelsForContent(content,lang);
			desc=StringUtils.cleanupSpaces(desc);
			label=StringUtils.cleanupSpaces(label);
			if (!StringUtils.isEmptyString(label)||!StringUtils.isEmptyString(desc)) return base+": "+label+" ("+desc+")";
		}
		return base;
	}

}
