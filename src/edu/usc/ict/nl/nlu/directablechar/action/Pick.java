package edu.usc.ict.nl.nlu.directablechar.action;

import java.util.List;

public class Pick extends Action {
	public Pick(List<String> args) {
		this.name="PICK";
		this.arguments=args;
	}
	
	@Override
	public boolean good() {
		return allResolved();
	}
	
}
