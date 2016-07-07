package edu.usc.ict.nl.bus.protocols;

import java.net.URL;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.usc.ict.nl.bus.ExternalListenerInterface;
import edu.usc.ict.nl.bus.NLBus;
import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.bus.events.DMInterruptionRequest;
import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.bus.events.NLUEvent;
import edu.usc.ict.nl.bus.events.TextUtteranceEvent;
import edu.usc.ict.nl.bus.special_variables.SpecialEntitiesRepository;
import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.utils.LogConfig;

public abstract class Protocol implements ExternalListenerInterface {
	protected static final Logger logger = Logger.getLogger(Protocol.class.getName());
	static {
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );
	}

	protected NLBus bus;
	protected NLBusConfig config;
	protected SpecialEntitiesRepository svs;

	public Protocol(NLBus bus) {
		logger.info("Starting protocol: "+this.getClass());
		this.bus=bus;
		this.config = bus.getConfiguration();
	}
	
	public List<SpecialVar> getSpecialVariables() throws Exception {
		if (svs!=null)  return svs.getAllSpecialVariables();
		return null;
	}

	public abstract void kill();
	
	@Override
	public void handleDMInterruptionRequestEvent(DMInterruptionRequest ev) throws Exception {
	}
	@Override
	public void handleDMSpeakEvent(DMSpeakEvent ev) throws Exception {
	}
	@Override
	public void handleNLGEvent(Long sessionID, NLGEvent nlgOutput) throws Exception {
	}
	@Override
	public void handleNLUEvent(Long sessionId, NLUEvent selectedUserSpeechAct) throws Exception {
	}
	@Override
	public void handleTextUtteranceEvent(Long sessionId, TextUtteranceEvent ev) throws Exception {
	}
	@Override
	public Long startSession(String characterName,Long sid){
		return null;
	}
	@Override
	public void terminateSession(Long sid) {
	}

	public boolean canDetectUtteranceCompleted() {
		return false;
	}
	
}
