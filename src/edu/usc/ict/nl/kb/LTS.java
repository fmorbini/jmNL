package edu.usc.ict.nl.kb;

import java.util.HashMap;

public class LTS {
	private static int nextID=0;
	public LTS(){
		this.id=nextID++;
	}
	public LTS(DialogueKBFormula f) {
		this.f=f;
		this.completeF=f;
	}
	public LTS(DialogueKBFormula f, DialogueKBFormula completeF) {
		this.f=f;
		this.completeF=completeF;
	}
	DialogueKBFormula f=null; // is the formula correspondent to this element
	DialogueKBFormula completeF=null; // is the complete formula (if f is the last argument, completeF is the parent) 
	HashMap<DialogueKBFormula, LTS> children=null;
	private int id;
}
