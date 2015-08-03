package edu.usc.ict.nl.nlu.wikidata;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		return toString(true);
	}
	
	public String toString(boolean longForm) {
		String base=(isEntity()?"Q":"P")+id;
		String desc=Wikidata.getLabelsForWikidataId(base);
		desc=StringUtils.cleanupSpaces(desc);
		if (!StringUtils.isEmptyString(desc)) return base+": "+desc;
		return base;
	}

}
