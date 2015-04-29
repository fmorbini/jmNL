package edu.usc.ict.nl.nlu.directablechar.action;

import java.util.List;
import java.util.Map;

public class Create extends Action {
	public Create(List<String> args) {
		this.name="CREATE";
		this.arguments=args;
	}
	
	@Override
	public boolean good() {
		return !allResolved();
	}
	
	@Override
	public Map<String, Object> getPayload() {
		return super.getPayload();
	}
	
	@Override
	public int compareTo(Action o) {
		float rother=o.fractionResolved();
		float rthis=fractionResolved();
		return Math.round(Math.signum(rother-rthis));
	}
}
