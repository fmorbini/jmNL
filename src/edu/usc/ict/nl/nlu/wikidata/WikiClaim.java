package edu.usc.ict.nl.nlu.wikidata;

public class WikiClaim {

	private String subject,property,object;

	public WikiClaim(String subject,String property, String object) {
		this.subject=subject;
		this.property=property;
		this.object=object;
	}
	
	@Override
	public String toString() {
		return "("+property+" "+subject+" "+object+")";
	}
}
