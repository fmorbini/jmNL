package edu.usc.ict.nl.dm.fsm.advicer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.scxml.model.Transition;
import org.apache.commons.scxml.model.TransitionTarget;

import edu.usc.ict.nl.dm.fsm.scxml.SCXMLRunner;
import edu.usc.ict.nl.nlu.NLUOutput;

public abstract class SCXMLAdvice {
	protected Set<TransitionTarget> currentStates;
	protected Collection<TransitionTarget> allStates;
	protected SCXMLRunner scxml;

	public SCXMLAdvice(SCXMLRunner scxml) {
		this.scxml=scxml;
		allStates=scxml.getAllStates();
		currentStates=scxml.getActiveStates();
	}
	
	public Set<TransitionTarget> getCurrentStates() {
		return currentStates;
	}
	public HashMap<String, Set<String>> getAdviceForWizardGivenTheseUserEvents(List<NLUOutput> userSpeechActs) throws Exception {
		return null;
	}
	public void applyThisWiwardSelectedEvent(String event) throws Exception {		
	}
	
	public HashSet<String> getExpectedUserEvents() {
		HashSet<String> ret=new HashSet<String>();
		for(TransitionTarget s:currentStates) {
			List transitions = s.getTransitionsList();
			for (Object t:transitions) {
				Transition tr=(Transition) t;
				String event=tr.getEvent();
				if ((event!=null) && (event.length()>0)) ret.add(event);
			}
		}
		return ret;
	}
	
	public String printStates(Collection<TransitionTarget> trs) {
		if (trs==null) trs=currentStates;
		String out="[";
		for(TransitionTarget tr:trs) {
			out+=" "+tr.getId();
		}
		out+="]";
		return out;
	}
}
