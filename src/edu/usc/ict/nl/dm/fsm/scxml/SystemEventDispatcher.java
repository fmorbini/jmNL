package edu.usc.ict.nl.dm.fsm.scxml;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.scxml.EventDispatcher;

import edu.usc.ict.nl.bus.events.DMChangeEvent;
import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.Event;
import edu.usc.ict.nl.bus.modules.DMEventsListenerInterface;
import edu.usc.ict.nl.kb.Change;


public class SystemEventDispatcher implements EventDispatcher, Serializable {

	private static final long serialVersionUID = 1L;
	transient private DMEventsListenerInterface listener;
	transient private Long sessionID;

	public SystemEventDispatcher(Long sid,DMEventsListenerInterface l) {
		this.sessionID=sid;
		this.listener=l;
	}
	
	@Override
	public void cancel(String sendId) {
	}

	public void internalSend(String event,Map params) {
		System.out.println("received this internal event "+event+" with parameters: "+params);
		if (listener!=null) try {listener.handleDMResponseEvent(new DMSpeakEvent(null,event,sessionID,params,null));} catch (Exception e) {e.printStackTrace();}
	}
	
	@Override
	public void send(String sendId, String target, String type, String event,
			Map params, Object hints, long delay, List externalNodes) {
		if (target.equals("dispatcher")) {
			if (type.equals("systemSpeechAct")) {
				System.out.println("received this system speech act "+event+" with parameters: "+params);
				if (listener!=null) try {listener.handleDMResponseEvent(new DMSpeakEvent(null,event,sessionID,params,null));} catch (Exception e) {e.printStackTrace();}
			} else if (type.equals("changeEvent")) {
				System.out.println("received this change event "+event+" with parameters: "+params);
				if (listener!=null) try {listener.handleDMResponseEvent(new DMChangeEvent(null,sessionID,new Change(event, null, params)));} catch (Exception e) {e.printStackTrace();}
			}
		}
	}

	public boolean setHoldingOfEventsAs(boolean newSetup) {
		if (listener!=null) {
			boolean currentSetup=listener.getHoldProcessingOfResponseEvents();
			listener.setHoldProcessingOfResponseEvents(newSetup);
			return currentSetup;
		} else return false;
	}

	public void clearHeldEvents() {
		if (listener!=null) listener.clearHeldEvents(sessionID);
	}

	public Queue<Event> getHeldEvents() {
		if (listener!=null) return listener.getUnprocessedResponseEvents(sessionID);
		else return null;
	}

}
