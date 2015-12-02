package edu.usc.ict.nl.dm.fsm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.scxml.Context;
import org.apache.commons.scxml.ErrorReporter;
import org.apache.commons.scxml.Evaluator;
import org.apache.commons.scxml.EventDispatcher;
import org.apache.commons.scxml.SCXMLExecutor;
import org.apache.commons.scxml.env.SimpleErrorReporter;
import org.apache.commons.scxml.env.jexl.JexlContext;
import org.apache.commons.scxml.env.jexl.JexlEvaluator;
import org.apache.commons.scxml.model.SCXML;

import edu.usc.ict.nl.bus.NLBusInterface;
import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.Event;
import edu.usc.ict.nl.bus.events.SystemUtteranceDoneEvent;
import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.config.DMConfig;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.dm.fsm.advicer.NoSendExecutorSCXMLAdvice;
import edu.usc.ict.nl.dm.fsm.scxml.SCXMLListenerUnhandledEvents;
import edu.usc.ict.nl.dm.fsm.scxml.SCXMLRunner;
import edu.usc.ict.nl.dm.fsm.scxml.SystemEventDispatcher;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.util.StringUtils;

/**
 * @author morbini
 *
 */
public class FSMdm extends SCXMLRunner {

	public FSMdm(DMConfig config) {
		super(config);
	}
	public FSMdm(Long sessionID, SCXML preparsedFSM,Context initialContext,
			Evaluator ev, EventDispatcher ed, ErrorReporter er,
			DMConfig config) throws Exception {
		super(sessionID, preparsedFSM, initialContext, ev, ed, er,config);
	}

	@Override
	public List<Event> handleEvent(Event ev) {
		if (!getPauseEventProcessing()) {
			super.handleEvent(ev);
			if (ev instanceof SystemUtteranceDoneEvent)
				return null;
			else if (ev instanceof DMSpeakEvent)
				try {
					updateInformationStateWithEvent(ev);
				} catch (Exception e) {
					e.printStackTrace();
				}
			else if (ev instanceof Event) {
				NLUOutput internalEvent=(NLUOutput) ev.getPayload();
				String speechActID = internalEvent.getId();
				Object payload = internalEvent.getPayload();
				sendEvent(speechActID, payload);
			}
		}
		return null;
	}

	@Override
	public Map<NLUOutput, List<List<String>>> getPossibleSystemResponsesForThesePossibleInputs(
			List<NLUOutput> userInputs) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NLUOutput> getHandledUserEventsInCurrentState(
			List<NLUOutput> userInputs) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Initializes the DM context and returns it
	 * @return Context with all initialization variables set
	 */
	public Context initializeContext() {
		HashSet<String> disabledNotUnderstandingSpeechActs = new  HashSet<String>();
		disabledNotUnderstandingSpeechActs.add("question.reason-to-visit");
		disabledNotUnderstandingSpeechActs.add("assertion.introduce-wait-loop");
		disabledNotUnderstandingSpeechActs.add("question.reason-to-visit.after-first");		
		
		Stack<String> activeStates = new Stack<String>();
		activeStates.push("main");

		HashMap<String, Object> initVars = new HashMap<String, Object>();
		initVars.put("disabledNotUnderstanding", disabledNotUnderstandingSpeechActs);
		initVars.put("stackCurrentActiveState", activeStates);
		//initVars.put("infoStateTypes", new InfoStateManager());
		

		return new JexlContext(initVars);
	}

	@Override
	public SCXML parseDialoguePolicy(String policyURL) {
		try {
			return parseSCXMLFile(policyURL);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public DM createPolicyDM(Object preparsedDialoguePolicy, Long sid,
			NLBusInterface listener) throws Exception {
		DMConfig config=getConfiguration();
		Context initialContext = initializeContext();
		FSMdm scxml = new FSMdm(sid,
				(SCXML)preparsedDialoguePolicy,initialContext,
				new JexlEvaluator(),
				new SystemEventDispatcher(sid,listener),
				new SimpleErrorReporter(),config);
		scxml.setMessageBus(listener);
		scxml.addListner(new SCXMLListenerUnhandledEvents((SystemEventDispatcher)scxml.getEventDispatcher(), config.getUnhandledEventName()));
		//scxml.addListner(new SCXMLListenerDebugger("127.0.0.1", 5555));
		//scxml.setAdvicer(new ExecutorSCXMLAdvice(scxml));
		scxml.setAdvicer(new NoSendExecutorSCXMLAdvice(scxml));
		return scxml;
	}
	@Override
	public boolean isSessionDone() {
		SCXMLExecutor exe = getExecutor();
		return ((exe == null) || (exe.getCurrentStatus() == null) || exe.getCurrentStatus().isFinal());
	}
	
	@Override
	public NLUOutput selectNLUOutput(String text,Long sessionId,
			List<NLUOutput> userSpeechActs) throws Exception {
		Map<String, Set<String>> mapping = getAdvicer().getAdviceForWizardGivenTheseUserEvents(userSpeechActs);
		DMConfig config=getConfiguration();
		String unhandledEvent=config.getUnhandledEventName();
		for(NLUOutput nluResult : userSpeechActs) {
			if (StringUtils.isEmptyString(text)) text=nluResult.getText();
			String speechAct = nluResult.getId();
			Set<String> replies = mapping.get(speechAct);
			if (replies != null && !replies.contains(unhandledEvent)) {
				logger.info(" highest probability, handled user event is: "+speechAct);
				return nluResult;
			}
		}
		logger.info(" all events are unhandled, send the unhandled event: "+config.getUnhandledEventName());
		return new NLUOutput(text,unhandledEvent, 1f, unhandledEvent);
	}
	
	/**
	 * Sets context variables to remember last 
	 * determined system speech act
	 * @param context SCXML context
	 * @param replySpeechAct System speech act 
	 * which represents the reply to the user
	 */
	public void updateInformationStateWithEvent(Event ev) throws Exception {
		if (ev!=null && ev instanceof DMSpeakEvent) {
			String replySpeechAct=ev.getName();
			DialogueKB context = getInformationState();
			List<String> trivialSA=getConfiguration().getTrivialSystemSpeechActs();
			if (!trivialSA.contains(replySpeechAct)) context.setValueOfVariable("lastSystemSpeechAct", replySpeechAct,ACCESSTYPE.THIS_OVERWRITETHIS);
			String value = (replySpeechAct.startsWith("question.") ? "yes" : "no");
			context.setValueOfVariable("lastSystemSpeechActWasQuestion", value,ACCESSTYPE.THIS_OVERWRITETHIS);
			logger.info("LAST NOT TRIVIAL SYSTEM SA IS: "+context.get("lastSystemSpeechAct")+" and is last system sa a question? "+context.get("lastSystemSpeechActWasQuestion"));
		}
	}
}
