package edu.usc.ict.nl.nlu.directablechar.action;

import java.util.List;

public class Goto extends Action {
	public Goto(List<String> args) {
		this.name="GOTO";
		this.arguments=args;
	}
	
	@Override
	public boolean good() {
		return allResolved();
	}
	
}
