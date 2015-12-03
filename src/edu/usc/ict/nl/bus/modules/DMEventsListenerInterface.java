package edu.usc.ict.nl.bus.modules;

import java.util.concurrent.LinkedBlockingQueue;

import edu.usc.ict.nl.bus.events.DMGeneratedEvent;
import edu.usc.ict.nl.bus.events.Event;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.bus.events.changes.DMStateChangeEvent;
import edu.usc.ict.nl.bus.events.changes.DMVarChangeEvent;
import edu.usc.ict.nl.bus.events.changes.DMVarChangesEvent;

public interface DMEventsListenerInterface {
	public void handleDMResponseEvent(DMGeneratedEvent ev) throws Exception;
	public boolean isResource(Long sessionID,NLGEvent ev) throws Exception;
	public void setHoldProcessingOfResponseEvents(boolean hold);
	public boolean getHoldProcessingOfResponseEvents();
	public LinkedBlockingQueue<Event> getUnprocessedResponseEvents(Long sid);
	public void clearHeldEvents(Long sid);
	public NLUInterface getNlu(Long sessionID) throws Exception;
	// get the DM that generates the events this interface receives
	public DM getDM(Long sessionID) throws Exception;
	public NLGInterface getNlg(Long sessionID) throws Exception;
	void handleDMChangeEvent(DMVarChangeEvent ev);
	void handleDMChangesEvent(DMVarChangesEvent ev);
	void handleDMStateChangeEvent(DMStateChangeEvent ev);
}
