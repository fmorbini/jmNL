package edu.usc.ict.nl.dm.fsm.advicer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.scxml.SCXMLExecutor;
import org.apache.commons.scxml.TriggerEvent;

import edu.usc.ict.nl.dm.fsm.scxml.SCXMLRunner;
import edu.usc.ict.nl.nlu.NLUOutput;

public class NoSendExecutorSCXMLAdvice extends ExecutorSCXMLAdvice {

	public NoSendExecutorSCXMLAdvice(SCXMLRunner scxml) throws Exception {
		super(scxml);
	}
	
	@Override
	public void applyThisWiwardSelectedEvent(String se) {
	}

	private static Set<String> dummyReplies=new HashSet<String>();
	static{dummyReplies.add("dummy");}
	
	@Override
	public HashMap<String, Set<String>> getAdviceForWizardGivenTheseUserEvents(List<NLUOutput> userEvents) throws Exception {
		if ((userEvents==null) || (userEvents.isEmpty())) return lastAdviceProvided;
		
		HashMap<String, Set<String>> advices=new HashMap<String, Set<String>>();
		SCXMLExecutor exe = scxml.getExecutor();

		for(NLUOutput ue:userEvents) {
			TriggerEvent[] ues=new TriggerEvent[]{new TriggerEvent(ue.getId(), TriggerEvent.SIGNAL_EVENT, ue.getPayload())};
			List<TriggerEvent> unusedUE=exe.fakeTriggerEvents(ues);
		
			String unusedEventName=(unusedUE==null)?null:unusedUE.get(0).getName();

			String ueName=ue.getId();
			if ((unusedEventName==null) || !unusedEventName.equals(ueName)) advices.put(ueName, dummyReplies);
		}
		
		lastAdviceProvided=advices;
		
		return advices;
	}
}
