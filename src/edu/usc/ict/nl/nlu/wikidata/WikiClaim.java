package edu.usc.ict.nl.nlu.wikidata;

import edu.usc.ict.nl.nlu.wikidata.dumps.LuceneWikidataSearch;

public class WikiClaim {

	private String subject,property,object;

	public WikiClaim(String subject,String property, String object) {
		this.subject=subject;
		this.property=property;
		this.object=object;
	}
	
	@Override
	public String toString() {
		return "("+subject+" "+property+" "+object+")";
	}

	public String getObject() {
		return object;
	}
	public String getSubject() {
		return subject;
	}
	public String getProperty() {
		return property;
	}

	public String toString(LuceneWikidataSearch searchItems, LuceneWikidataSearch searchProperties) {
		String as=searchItems.getLabelForId(subject);
		String ao=searchItems.getLabelForId(object);
		String ap=searchProperties.getLabelForId(property);
		return "("+as.replaceAll("[\\s]+", "_")+" "+ap.replaceAll("[\\s]+", "_")+" "+ao.replaceAll("[\\s]+", "_")+")";
	}
}
