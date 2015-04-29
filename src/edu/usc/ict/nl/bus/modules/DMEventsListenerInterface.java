package edu.usc.ict.nl.bus.modules;

import java.util.concurrent.LinkedBlockingQueue;

import edu.usc.ict.nl.bus.events.DMGeneratedEvent;
import edu.usc.ict.nl.bus.events.Event;
import edu.usc.ict.nl.bus.events.NLGEvent;

public interface DMEventsListenerInterface {
	public void handleDMResponseEvent(DMGeneratedEvent ev) throws Exception;
	public boolean isResource(Long sessionID,NLGEvent ev) throws Exception;
	public void setHoldProcessingOfResponseEvents(boolean hold);
	public boolean getHoldProcessingOfResponseEvents();
	public LinkedBlockingQueue<Event> getUnprocessedResponseEvents(Long sid);
	public void clearHeldEvents(Long sid);
	// get the DM that generates the events this interface receives
	public DM getPolicyDMForSession(Long sid) throws Exception;
	public NLGInterface getNlg(Long sessionID) throws Exception;
	public NLUInterface getNlu(Long sessionID) throws Exception;
}
