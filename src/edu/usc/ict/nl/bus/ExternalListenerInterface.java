package edu.usc.ict.nl.bus;

import edu.usc.ict.nl.bus.events.DMInterruptionRequest;
import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.bus.events.NLUEvent;
import edu.usc.ict.nl.bus.events.TextUtteranceEvent;

public interface ExternalListenerInterface {
	public void handleDMInterruptionRequestEvent(DMInterruptionRequest ev) throws Exception;

	public void handleTextUtteranceEvent(Long sessionId, TextUtteranceEvent ev) throws Exception;
	public void handleNLUEvent(Long sessionId,NLUEvent selectedUserSpeechAct) throws Exception;
	public void handleDMSpeakEvent(DMSpeakEvent ev) throws Exception;
	public void handleNLGEvent(Long sessionID, NLGEvent nlgOutput) throws Exception;
	/**
	 * 
	 * @param characterName the name of the character to be loaded in this session
	 * @param sid if not null that sid will be terminated and a new sid with same id will be created (this makes the function equivalent to a restart)
	 * @return the id of the session created (equal to the input sid parameter if not null).
	 */
	public Long startSession(String characterName,Long sid);
	public void terminateSession(Long sid);
}
