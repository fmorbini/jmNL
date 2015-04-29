package edu.usc.ict.nl.dm.fsm.scxml;

import java.io.Serializable;

import org.apache.commons.scxml.SCXMLListener;
import org.apache.commons.scxml.TriggerEvent;
import org.apache.commons.scxml.model.Transition;
import org.apache.commons.scxml.model.TransitionTarget;

public class SCXMLListenerUnhandledEvents implements SCXMLListener,Serializable {

	private static final long serialVersionUID = 1L;
	private String unhandledEventResponse;
	private SystemEventDispatcher systemEventdispatcher;
	private String unhandledEvent;

	public SCXMLListenerUnhandledEvents(SystemEventDispatcher sed,String uer) {
		this.systemEventdispatcher=sed;
		this.unhandledEventResponse=uer;
	}
	
	public String getUnhandledEvent() {
		return unhandledEvent;
	}
	public void resetUnhandledEvent() {
		unhandledEvent=null;
	}
	
	@Override
	public void onEntry(TransitionTarget arg0) {
	}

	@Override
	public void onExit(TransitionTarget arg0) {
	}

	@Override
	public void onTransition(TransitionTarget arg0, TransitionTarget arg1,Transition arg2) {
	}

	@Override
	public void onUnusedEvent(TriggerEvent te) {
		unhandledEvent=te.getName();
		System.out.println("WARNING, this event was not handled: "+te+".");
		systemEventdispatcher.internalSend(unhandledEventResponse, null);
	}

}
