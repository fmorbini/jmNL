package edu.usc.ict.nl.dm.fsm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.scxml.model.OnEntry;
import org.apache.commons.scxml.model.Send;
import org.apache.commons.scxml.model.TransitionTarget;

import edu.usc.ict.nl.dm.fsm.advicer.BaseGraphSCXMLAdvice;
import edu.usc.ict.nl.dm.fsm.scxml.SCXMLRunner;

public class GraphSCXMLAdvice extends BaseGraphSCXMLAdvice {

	public static String cleanString(String in) {
		return in.replaceAll("^[\\s']", "").replaceAll("[\\s']$", "");
	}
	public GraphSCXMLAdvice(SCXMLRunner scxml, String[] reportingStates) throws Exception {
		super(scxml, reportingStates);
	}

	@Override
	public Set<String> returnOnlySystemEvents(TransitionTarget s) {		
		if (s!=null) {
			HashSet<String> ret=new HashSet<String>();
			OnEntry entry = s.getOnEntry();
			List actions=entry.getActions();
			for(Object a:actions) {
				if (a instanceof Send) {
					Send action=(Send) a;
					String event=action.getEvent();
					String target=action.getTarget();
					String type=action.getType();
					if ((event!=null) && (target!=null) && (type!=null) && (event.length()>0) && type.equals("'systemSpeechAct'") && target.equals("'dispatcher'")) {						
						ret.add(cleanString(event));
					}
				}
			}
			return ret;
		} else return null;
	}
	@Override
	public Set<String> returnOnlySystemEventsFromPreferredStates(TransitionTarget s) {
		if ((s!=null) && (reportingStates.containsKey(s))) {
			HashSet<String> ret=new HashSet<String>();
			OnEntry entry = s.getOnEntry();
			List actions=entry.getActions();
			for(Object a:actions) {
				if (a instanceof Send) {
					Send action=(Send) a;
					String event=action.getEvent();
					String target=action.getTarget();
					String type=action.getType();
					if ((event!=null) && (target!=null) && (type!=null) && (event.length()>0) && type.equals("'systemSpeechAct'") && target.equals("'dispatcher'")) {
						ret.add(cleanString(event));
					}
				}
			}
			return ret;
		} else return null;
	}
}
