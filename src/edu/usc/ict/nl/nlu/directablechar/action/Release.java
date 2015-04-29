package edu.usc.ict.nl.nlu.directablechar.action;

import java.util.List;

public class Release extends Action {
	public Release(List<String> args) {
		this.name="RELEASE";
		this.arguments=args;
	}
	
	@Override
	public boolean good() {
		return allResolved();
	}
	
}
