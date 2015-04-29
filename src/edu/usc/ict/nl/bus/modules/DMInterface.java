package edu.usc.ict.nl.bus.modules;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.bus.NLBusInterface;
import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.Event;
import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.nlu.NLUOutput;

public interface DMInterface {
	
	/**
	 * @param ev
	 * @return set of DM answers
	 * handler called whenever a NLU output is available and needs to be processed by the DM, returns the set of DM answers
	 */
	public List<Event> handleEvent(Event ev);
	public boolean getPauseEventProcessing();
	public void setPauseEventProcessing(boolean p);
	
	/* returns the current information state */
	public DialogueKB getInformationState();
	/* returns the set of possible answers given the set of NLU outputs */
	public 	Map<NLUOutput, List<List<String>>> getPossibleSystemResponsesForThesePossibleInputs(List<NLUOutput> userInputs) throws Exception;
	/* returns the subset of nlu output that are handeld in the current DM state */
	public 	List<NLUOutput> getHandledUserEventsInCurrentState(List<NLUOutput> userInputs) throws Exception;
	// method used to parse the dialogue policy pointed by the given file
	public Object parseDialoguePolicy(String policyURL) throws Exception;
	// validate a parsed policy
	public void validatePolicy(NLBusBase nlModule) throws Exception;
	// returns all possible system speak events (the argument is optional)
	public List<DMSpeakEvent> getAllPossibleSystemLines() throws Exception;
	public List<DMSpeakEvent> getAllAvailableSystemLines() throws Exception;
	// creates a DM associated to the given policy
	public DM createPolicyDM(Object parsedDialoguePolicy, Long sid,NLBusInterface listener) throws Exception;
	// returns true if the given session is done
	public boolean isSessionDone();
	public Long getSessionID();
	public void setSessionID(long sessionID);
	public String getPersonalSessionID();
	public void setPersonalSessionID(String pid);

	public Set<String> getIDActiveStates() throws Exception;
	public void resetActiveStatesTo(Set<String> activeStateIds) throws Exception;

	public NLUOutput selectNLUOutput(String text,Long sessionId,List<NLUOutput> userSpeechActs) throws Exception;
	
	public void kill();
	public void logEventInChatLog(Event ev);
	public File getCurrentChatLogFile();
	
	public boolean isWaitingForUser();
	
	public Logger getLogger();
}
