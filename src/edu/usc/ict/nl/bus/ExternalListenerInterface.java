package edu.usc.ict.nl.bus;

import edu.usc.ict.nl.bus.events.DMInterruptionRequest;
import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.bus.events.NLUEvent;

public interface ExternalListenerInterface {
	public void handleDMInterruptionRequestEvent(DMInterruptionRequest ev) throws Exception;

	public void handleTextUtteranceEvent(Long sessionId, String text) throws Exception;
	public void handleNLUEvent(Long sessionId,NLUEvent selectedUserSpeechAct) throws Exception;
	public void handleDMSpeakEvent(DMSpeakEvent ev) throws Exception;
	public void handleNLGEvent(Long sessionID, NLGEvent nlgOutput) throws Exception;
}
