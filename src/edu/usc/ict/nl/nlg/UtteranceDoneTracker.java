package edu.usc.ict.nl.nlg;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;

import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.util.StringUtils;

public class UtteranceDoneTracker {
	private Map<String,NLGEvent> activeMessages;
	protected Logger logger=null;
	private Map<String,Boolean> receivedVrSpoke;
	protected Semaphore lock=new Semaphore(1);

	public UtteranceDoneTracker(Logger logger) {
		this.logger=logger;
		activeMessages=new HashMap<String, NLGEvent>();
		receivedVrSpoke=new HashMap<>();
	}

	public void storeActiveMessage(String id, NLGEvent nlg) {
		activeMessages.put(id, nlg);
		receivedVrSpoke.put(nlg.getDMEventName(), false);
	}
	
	public String getSpeechActIDFromVRMessageID(String vrMessageID) {
		NLGEvent nlg = activeMessages.get(vrMessageID);
		if (nlg!=null) {
			DMSpeakEvent ev=nlg.getPayload();
			if (ev!=null) return ev.getName();
		}
		return null;
	}

	public void completeVRMessageWithID(String vrMessageID) {
		activeMessages.remove(vrMessageID);
	}

	public void receivedVrSpoke(String sa) throws InterruptedException {
		if (!StringUtils.isEmptyString(sa)) {
			lock.acquire();
			receivedVrSpoke.put(sa, true);
			lock.release();
		}
	}

	public boolean canSpeechactBeEnded(String sa) throws InterruptedException {
		if (!StringUtils.isEmptyString(sa)) {
			lock.acquire();
			Boolean r=receivedVrSpoke.get(sa);
			lock.release();
			if (r==null) logger.error("Speech act "+sa+" had no vrspoke tracker set.");
			return r!=null && r;
		}
		return false;
	}

}
