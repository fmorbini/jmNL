package edu.usc.ict.nl.dm.fsm.advicer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.commons.scxml.EventDispatcher;
import org.apache.commons.scxml.SCXMLListener;

import edu.usc.ict.nl.bus.events.Event;
import edu.usc.ict.nl.dm.fsm.scxml.SCXMLListenerDebugger;
import edu.usc.ict.nl.dm.fsm.scxml.SCXMLRunner;
import edu.usc.ict.nl.dm.fsm.scxml.SystemEventDispatcher;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.util.FunctionalLibrary;

public class ExecutorSCXMLAdvice extends SCXMLAdvice {

	protected SCXMLRunner scxml;

	public ExecutorSCXMLAdvice(SCXMLRunner scxml) throws Exception {
		super(scxml);
		EventDispatcher ed = scxml.getEventDispatcher();
		if (!(ed instanceof SystemEventDispatcher)) throw new Exception("ExecutorSCXMLAdvice requires a dispatcher of type SystemEventDispatcher.");
		this.scxml=scxml;
	}

	private SystemEventDispatcher getSystemEventDispatcher() {
		return (SystemEventDispatcher) scxml.getEventDispatcher();
	}
	
	private HashMap<String,String> system2userMap=new HashMap<String, String>();
	
	@Override
	public void applyThisWiwardSelectedEvent(String se) throws Exception {
		// just send the selected event
		String ue=system2userMap.get(se);
		if (ue!=null) scxml.sendEvent(ue, null);
		else System.out.println("WARNING: trying to update the advicer status with an unexpected system event: "+se);
		currentStates=scxml.getActiveStates();
	}

	protected HashMap<String, Set<String>> lastAdviceProvided=new HashMap<String, Set<String>>();
	
	@Override
	public HashMap<String, Set<String>> getAdviceForWizardGivenTheseUserEvents(List<NLUOutput> userEvents) throws Exception {
		if ((userEvents==null) || (userEvents.isEmpty())) return lastAdviceProvided;
		HashMap<String, Set<String>> advices=new HashMap<String,Set<String>>();
		system2userMap.clear();
		
		// save current network status
		ByteArrayOutputStream container = new ByteArrayOutputStream();

		ArrayList<SCXMLListenerDebugger> ls=removeAllDebuggerListeners();
		scxml.stopAndSaveExecutor(container);
		ByteArrayInputStream content = new ByteArrayInputStream(container.toByteArray());
		
		boolean currentSetup=getSystemEventDispatcher().setHoldingOfEventsAs(true);
		
		// send each possible input event to the network and collect the output (coming from the event dispatcher)
		for (NLUOutput uep:userEvents) {
			// clear list of system events
			getSystemEventDispatcher().clearHeldEvents();
			// send the possible event
			System.out.println("advicer, phase 1: sending event: "+uep.getId()+" with payload: "+uep.getPayload());
			scxml.sendEvent(uep.getId(), uep.getPayload());
			// get the generated system events
			Queue<Event> collectedEvents = getSystemEventDispatcher().getHeldEvents();
			if (collectedEvents!=null) {
				Collection<String> result=FunctionalLibrary.map(collectedEvents, Event.class.getMethod("getName"));
				System.out.println("advicer, phase 2: getting system replies: "+result);
				if (!result.isEmpty()) {
					advices.put(uep.getId(), new HashSet<String>(result));
					for(String se:result) {
						if (system2userMap.get(se)!=null) {
							System.out.println("WARNING: same system event for multiple user events.");
							System.out.println(se+" <- "+system2userMap.get(se)+" and now by: "+uep.getId());
						}
						system2userMap.put(se, uep.getId());
					}
				}
			}
			
			// retract modifications to finite state machine state
			content.reset();
			scxml.reloadAndStartExecutor(content);
			getSystemEventDispatcher().setHoldingOfEventsAs(currentSetup);
			//scxml.getExecutor().setEventdispatcher(ed);
		}
		addAllDebuggerListeners(ls);
		//scxml.getExecutor().setEventdispatcher(ed);

		lastAdviceProvided=advices;
		return advices;
	}

	private void addAllDebuggerListeners(ArrayList<SCXMLListenerDebugger> ls) {
		for (SCXMLListenerDebugger l:ls) {
			scxml.addListner(l);
		}
	}

	private ArrayList<SCXMLListenerDebugger> removeAllDebuggerListeners() {
		ArrayList<SCXMLListenerDebugger> ret=new ArrayList<SCXMLListenerDebugger>();
		Set<SCXMLListener> listeners = scxml.getListeners();
		for (SCXMLListener l:listeners) {
			if (l instanceof SCXMLListenerDebugger) {
				ret.add((SCXMLListenerDebugger) l);
			}
		}
		for(int i=0;i<ret.size();i++) {
			SCXMLListenerDebugger l = ret.get(0);
			scxml.removeListner(l);
		}
		return ret;
	}
}
