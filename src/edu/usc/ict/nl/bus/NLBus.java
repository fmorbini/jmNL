package edu.usc.ict.nl.bus;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import javax.xml.bind.JAXBContext;

import org.apache.log4j.PropertyConfigurator;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.usc.ict.nl.bus.events.DMGeneratedEvent;
import edu.usc.ict.nl.bus.events.DMInterruptionRequest;
import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.Event;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.bus.events.NLUEvent;
import edu.usc.ict.nl.bus.events.changes.DMVarChangeEvent;
import edu.usc.ict.nl.bus.events.changes.DMVarChangesEvent;
import edu.usc.ict.nl.bus.events.changes.DMStateChangeEvent;
import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.bus.modules.NLGInterface;
import edu.usc.ict.nl.bus.modules.NLUInterface;
import edu.usc.ict.nl.bus.protocols.Protocol;
import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.pml.PMLStateKeeper;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.utils.LogConfig;
import edu.usc.ict.nl.vhmsg.VHBridge;
import edu.usc.ict.nl.vhmsg.VHBridge.VRPerception;
import edu.usc.ict.nl.vhmsg.VHBridgewithMinat;
import edu.usc.ict.nl.vhmsg.VHBridgewithMinat.Minat;
import edu.usc.ict.nl.vhmsg.VHBridgewithMinat.Minat.Decision;
import edu.usc.ict.perception.pml.Pml;
import edu.usc.ict.vhmsg.MessageListener;

/**
 * This class contains all methods required to process events and communicate between the three main NL sub-modules: NLU, DM and NLG.
 * Put all other methods in the NLBusBuse class 
 *  
 * @author morbini
 * 
 */
public class NLBus extends NLBusBase {

	protected static String defaultSpringConfigFile = "dialogueManagerConfig.xml";
	
	protected static NLBus _instance;
	private Boolean started=null;

	// each DM is associated to a particular policy. There is 1-to-1 correspondence between characters and policies.
	// each session is associated to a different instance of a DM (each with a different instance of a policy).

	/** singleton scope controlled via spring initialization */
	public static final NLBus getInstance() {
		return _instance;
	}
	
	private long startTime=0l;
	@Override
	public float getTimeUserHasBeenSpeaking() {
		if (startTime<=0) return 0;
		else return ((float)(System.currentTimeMillis()-startTime))/1000.0f;
	}
	@Override
	synchronized public void setSpeakingStateVarForSessionAs(Long sessionId,Boolean state) throws Exception {
		if (getCharacterName4Session(sessionId)!=null) {
			DM dm=getPolicyDMForSession(sessionId,false);
			if (dm!=null) {
				DialogueKBInterface informationState = dm.getInformationState();
				if (informationState!=null) {
					Boolean currentState = (Boolean)informationState.evaluate(informationState.getValueOfVariable(userSpeakingStateVarName, ACCESSTYPE.AUTO_OVERWRITEAUTO,null),null);
					informationState.setValueOfVariable(userSpeakingStateVarName, state,ACCESSTYPE.AUTO_OVERWRITEAUTO);
					if (currentState==null || currentState!=state) {

						Float seconds=null;
						if (state && startTime<=0) startTime=System.currentTimeMillis();
						if (!state && startTime>0) {
							seconds=((float)(System.currentTimeMillis()-startTime))/1000.0f;
							startTime=0;
						}
						
						if (state) {
							informationState.setValueOfVariable(lengthOfLastThingUserSaidVarName,0,ACCESSTYPE.AUTO_OVERWRITEAUTO);
						} else {
							informationState.setValueOfVariable(lengthOfLastThingUserSaidVarName, seconds,ACCESSTYPE.AUTO_OVERWRITEAUTO);
							logger.warn("User last utterance length="+seconds+" [s]");
							DialogueOperatorEffect incrementLengthOfLastUserTurnVar=DialogueOperatorEffect.createIncrementForVariable(lengthOfLastUserTurnVarName,seconds);
							informationState.store(incrementLengthOfLastUserTurnVar, ACCESSTYPE.AUTO_OVERWRITEAUTO, true);
							logger.warn("User turn length="+informationState.getValueOfVariable(lengthOfLastUserTurnVarName,ACCESSTYPE.AUTO_OVERWRITEAUTO,null)+" [s]");
						}
					}
				}
			}
		}
	}

	
	@Override
	public boolean isResource(Long sessionID,NLGEvent ev) throws Exception {
		NLGInterface nlg = getNlg(sessionID);
		return nlg.isResource(sessionID, ev);
	}
	//##############################################################################
	//    Implementing DM event receiving interface 
	//##############################################################################
	@Override
	public void handleDMResponseEvent(DMGeneratedEvent ev) throws Exception {
		if (ev instanceof DMSpeakEvent) {
			handleDMSpeakEvent((DMSpeakEvent) ev);
		} else if (ev instanceof DMVarChangeEvent) {
			handleDMChangeEvent((DMVarChangeEvent) ev);
		} else if (ev instanceof DMVarChangesEvent) {
			handleDMChangesEvent((DMVarChangesEvent) ev);
		} else if (ev instanceof DMInterruptionRequest) {
			handleDMInterruptionRequestEvent((DMInterruptionRequest) ev);
		} else if (ev instanceof DMStateChangeEvent) {
			handleDMStateChangeEvent((DMStateChangeEvent) ev);
		} else {
			throw new Exception("Unhandled type of DM response event: "+ev.getClass());
		}
	}
	
	@Override
	public void handleDMChangeEvent(DMVarChangeEvent ev) {
	}
	@Override
	public void handleDMChangesEvent(DMVarChangesEvent ev) {
	}
	@Override
	public void handleDMStateChangeEvent(DMStateChangeEvent ev) {
	}

	//##############################################################################
	//    Implementing interface for processing external events
	//##############################################################################
	@Override
	public void handleDMSpeakEvent(DMSpeakEvent ev) throws Exception {
		Long sessionID=ev.getSessionID();
		DM dm=getPolicyDMForSession(sessionID);
		dm.handleEvent(ev);
		
		if (holdResponses) {
			LinkedBlockingQueue<Event> queue = getUnprocessedResponseEvents(sessionID);
			if (queue==null) session2UnprocessedDMResponses.put(sessionID,queue=new LinkedBlockingQueue<Event>());
			queue.add(ev);
		} else {
			if (!getConfiguration().getNlgVhListening()) {
				NLGEvent nlgOutput = getNlg(sessionID).doNLG(sessionID, (DMSpeakEvent) ev,false);
				handleNLGEvent(sessionID, nlgOutput);
			}
			if (hasListeners()) {
				for(ExternalListenerInterface l:getListeners()) {
					l.handleDMSpeakEvent(ev);
				}
			}
			if (hasProtocols()) {
				for(Protocol p:getProtocols()) {
					p.handleDMSpeakEvent(ev);
				}
			}
		}
	}

	@Override
	public void handleDMInterruptionRequestEvent(DMInterruptionRequest ev) throws Exception {
		Long sessionID=ev.getSessionID();
		
		if (holdResponses) {
			LinkedBlockingQueue<Event> queue = getUnprocessedResponseEvents(sessionID);
			if (queue==null) session2UnprocessedDMResponses.put(sessionID,queue=new LinkedBlockingQueue<Event>());
			queue.add(ev);
		} else {
			if (hasListeners()) {
				for(ExternalListenerInterface l:getListeners()) {
					l.handleDMInterruptionRequestEvent(ev);
				}
			}
			NLGInterface nlg = getNlg(sessionID);
			nlg.interrupt(ev);
		}
	}
	
	/**
	 * Handles text utterance event from the user
	 * @param sessionId Session ID
	 * @param event Event which contains the user utterance
	 * @throws Exception 
	 */
	@Override
	public void handleTextUtteranceEvent(Long sessionId, String text) throws Exception {
		logger.info("Text utterance event received for session "+sessionId+": '"+text+"'");
		if (isInExecuteMode()) {
			if (hasListeners()) {
				for(ExternalListenerInterface l:getListeners()) {
					l.handleTextUtteranceEvent(sessionId,text);
				}
			}
			NLUOutput nluOutput=getNLUOutput(sessionId, text);
			logger.info("Text utterance classified into: '"+nluOutput+"'");
			if (nluOutput!=null) {
				NLUEvent ev = new NLUEvent(nluOutput, sessionId);
				if (!getConfiguration().getDmVhListening()) {
					handleNLUEvent(sessionId,ev);
				}
				if (hasProtocols()) {
					for(Protocol p:getProtocols()) {
						p.handleNLUEvent(sessionId, ev);
					}
				}
			}
		} else {
			throw new Exception("unhanlded");
		}
	}
	@Override
	public NLUOutput getNLUOutput(Long sessionId,String userUtterance) throws Exception {
		NLUInterface nlu=getNlu(sessionId);
		DM dm=getPolicyDMForSession(sessionId,false);
		List<NLUOutput> userSpeechActs = nlu.getNLUOutput(userUtterance, null,null);
		NLUOutput selectedUserSpeechAct=null;
		if (userSpeechActs!=null) {
			selectedUserSpeechAct=dm.selectNLUOutput(userUtterance,sessionId, userSpeechActs);
		}
		return selectedUserSpeechAct;
	}
	@Override
	public void handleNLUEvent(Long sessionId,NLUEvent event) throws Exception {
		if (isInExecuteMode() && event!=null) {
			if (hasListeners()) {
				for(ExternalListenerInterface l:getListeners()) {
					l.handleNLUEvent(sessionId,event);
				}
			}
			DM dm=getPolicyDMForSession(sessionId,false);
			dm.handleEvent(event);
		} else {
			throw new Exception("unhanlded");
		}
	}
	@Override
	public void handleNLGEvent(Long sessionId,NLGEvent event) throws Exception {
		if (isInExecuteMode() && event!=null) {
			DM dm=getPolicyDMForSession(sessionId,false);
			if (dm!=null) dm.handleEvent(event);
			if (hasListeners()) {
				for(ExternalListenerInterface l:getListeners()) {
					l.handleNLGEvent(sessionId,event);
				}
			}
		} else {
			throw new Exception("unhandled (exeMode? "+isInExecuteMode()+", event="+event+")");
		}
	}
	@Override
	public void handleLoginEvent(Long sessionId, String userID) throws Exception {
		DM dm=getPolicyDMForSession(sessionId,true);
		if (dm!=null) {
			NLUInterface nlu=getNlu(sessionId);
			NLUOutput loginNluOutput = getNLUforLoginEvent(sessionId, dm, nlu);
			handleNLUEvent(sessionId, new NLUEvent(loginNluOutput, sessionId));
		}

	}
	

	//##############################################################################
	//  JSVC START/STOP/INIT methods and other static startup methods. 
	//##############################################################################
	public void startup() throws Exception {
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );
		
		started = true;
		logger.info("Running mode: "+getConfiguration().getRunningMode());
			
		NLBusConfig config=getConfiguration();
		switch (config.getRunningMode()) {
		case EXE:
			setHoldProcessingOfResponseEvents(false);
			break;
		case ADVICER:
			setHoldProcessingOfResponseEvents(true);
			break;
		/*case AUTHORING:
			logger.info("Authoring provisioning mode is enabled [EXPERIMENTAL]");
			break;*/
		}


		setDM((DM)createSubcomponent(config,config.getDmClass()));
		
		character2unparsedPolicy=findAvailablePolicies(config.getContentRoot());
		character2parsedPolicy=parseAvailablePolicies(character2unparsedPolicy);
		validateAvailablePolicies(character2parsedPolicy);

		List<String> ps=config.getProtocols();
		if (ps!=null) {
			for(String p:ps) {
				Protocol protocol = createProtocol(this, p);
				addProtocol(protocol);
			}
		}

		logger.info(this.getClass().getCanonicalName()+" Ready.");
	}

	/** 
	 * JSVC daemon tool start method. Starts NLU and DM modules 
	 */
	public static void start() {
		NLBus nl = getInstance();
		if (nl.started == null || !nl.started)
			try {
				nl.startup();
			} catch (Exception e) {
				e.printStackTrace();
			}
	}
	public void shutdown() throws Exception {
		logger.info("Stopping NLU module.");
		for (NLUInterface nlu:session2NLU.values())
			try { nlu.kill(); } catch (Exception e) {}
		logger.info("Stopping DM module.");
		DM dm=getDM();
		if (dm != null)
			try { dm.kill(); } catch (Exception e) {}
		
		if (context!=null) context.destroy();
		if (hasProtocols()) {
			for(Protocol p:getProtocols()) {
				if (p!=null) p.kill();
			}
		}
	}
	/**
	 * JSVC daemon tool stop method. Stops all application threads
	 * @throws Exception 
	 */
	public static void stop() throws Exception {
		NLBus nl = getInstance();
		try {
			nl.shutdown();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/** JSVC Daemon destroy method. Release all objects created in init() */
	public static void destroy() {
	}
	/**
	 * JSVC daemon tool init method.
	 */
	public static void init(String args[]) {
		System.out.println("Initializing NL module configuration.");
		String[] config = (args==null || args.length == 0)?new String[] {defaultSpringConfigFile}:new String[] {args[0]};
		context = new ClassPathXmlApplicationContext(config);		
	}
	
	public NLBus() throws Exception {
		super();
		_instance=this;
	}
	
	public static void main(String[] args) throws Exception {
		init(args);
		start();
		NLBus nl=getInstance();
	}

}
