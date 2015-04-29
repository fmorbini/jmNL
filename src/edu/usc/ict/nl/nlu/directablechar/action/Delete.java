package edu.usc.ict.nl.nlu.directablechar.action;

import java.util.List;

public class Delete extends Action {
	public Delete(List<String> args) {
		this.name="DELETE";
		this.arguments=args;
	}
	
	@Override
	public boolean good() {
		return allResolved();
	}
}
