package edu.usc.ict.nl.bus;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.Event;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.bus.modules.NLG;
import edu.usc.ict.nl.bus.modules.NLGInterface;
import edu.usc.ict.nl.bus.modules.NLUInterface;
import edu.usc.ict.nl.bus.protocols.Protocol;
import edu.usc.ict.nl.bus.special_variables.SpecialEntitiesRepository;
import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.config.NLConfig;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.VariableProperties;
import edu.usc.ict.nl.kb.VariableProperties.PROPERTY;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.utils.LogConfig;

/**
 * 
 * This class contains all methods that are not directly implementing event handling or communication related functions.
 * Events and communications between the three main modules this bus class is intended to connect: NLU, DM and NLG. 
 * 
 * @author morbini
 *
 */
public abstract class NLBusBase implements NLBusInterface {
	
	protected Map<Long,SpecialEntitiesRepository> session2specialVars=null;
	
	public static final Logger logger = Logger.getLogger(NLBusBase.class.getName());
	static {
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );
	}

	protected NLBusConfig configuration;
	public NLBusConfig getConfiguration() {return configuration;}
	public void setConfiguration(NLBusConfig c) {this.configuration=c;}
		
	protected static ClassPathXmlApplicationContext context;

	private DM dm;
	public DM getDM() {return dm;}
	protected void setDM(DM dm) {this.dm=dm;}
	
	protected List<ExternalListenerInterface> listeners=null;
	protected List<Protocol> protocols=null;
	@Override
	public void addBusListener(ExternalListenerInterface i) {
		if (i!=null) {
			if (this.listeners==null) this.listeners=new ArrayList<ExternalListenerInterface>();
			this.listeners.add(i);
		}
	}
	protected boolean hasListeners() {return listeners!=null && !listeners.isEmpty();}
	protected List<ExternalListenerInterface> getListeners() {return listeners;}
	protected boolean hasProtocols() {return protocols!=null && !protocols.isEmpty();}
	protected List<Protocol> getProtocols() {return protocols;}
	public void addProtocol(Protocol p) {
		if (p!=null) {
			if (this.protocols==null) this.protocols=new ArrayList<Protocol>();
			this.protocols.add(p);
		}
	}


	// PER SESSION INFORMATION
	protected Map<Long,String> session2User = null;
	protected Map<Long, Boolean> session2Ignore = null;
	protected Map<Long, ReferenceToVirtualCharacter> session2Character = null;
	protected ConcurrentHashMap<Long, Set<Long>> session2HandledEvents = null;
	protected HashMap<Long,NLUInterface> session2NLU=null;
	protected HashMap<Long,DM> session2PolicyDM=null;
	protected HashMap<Long,NLG> session2NLG=null;
	protected HashMap<String,NLBusConfig> character2Config=null;
	// key: character name, value: unparsed POLICY associated with it
	protected HashMap<String,String> character2unparsedPolicy = null;
	// key: character name, value: already parsed POLICY network associated with it
	protected HashMap<String,Object> character2parsedPolicy=null;
	// stores timestamps for various objects in each session. Used for randomly selecting and rpeferring earlier used things.
	private static Map<Long,Map<Integer,Long>> session2ContentTimestamps = null;
	
	// EXECUTE MODES
	public boolean isInExecuteMode() {return getConfiguration().isInExecuteMode();}
	public boolean isLoadBalacingActive() {return getConfiguration().getIsLoadBalancing();}

	//##############################################################################
	//    setting and getting the character associated to a particular session
	//     each session is a user talking to a particular character whose policy
	//     has been correctly parsed and validated
	//##############################################################################
	public void setCharacter4Session(Long sid,String name,Object payload) {
		session2Character.put(sid, new ReferenceToVirtualCharacter(name,payload));
	}
	public void setCharacter4Session(Long sid,String name) {
		session2Character.put(sid, new ReferenceToVirtualCharacter(name));
	}
	@Override
	public String getCharacterName4Session(Long sid) {
		ReferenceToVirtualCharacter c = getCharacter4Session(sid);
		if (c!=null) return c.getName();
		else return null;
	}
	public ReferenceToVirtualCharacter getCharacter4Session(Long sid) {return session2Character.get(sid);}

	/**
	 * Picks one {@link HasAnnotations} object of the given list of replies 
	 * @param sessionId Session ID
	 * @param replies List of hasAnnotations which are returned 
	 * to the user by the system
	 * @return The {@link HasAnnotations} object with the oldest timestamp
	 * if the given list contains multiple objects, the exact object if the
	 * given list contains exactly one object, null if the given list is empty
	 */
	public static Object pickEarliestUsedOrStillUnused(Long sessionId, Collection replies) throws Exception {
		if (replies == null || replies.isEmpty()) 
			return null;
		if (replies.size() == 1)  
			return replies.iterator().next();

		Map<Integer, Long> timestampsById = session2ContentTimestamps.get(sessionId);
		if (timestampsById == null) {
			timestampsById = new HashMap<Integer, Long>();
			session2ContentTimestamps.put(sessionId, timestampsById);
		}
		Long oldestTimestamp = null;
		Object oldestAction = null;
		Long currentTime = new Date().getTime();
		List<Object> orderedReplies = new ArrayList<Object>(replies);
		//add randomness to selection process from candidate utterances 
		Collections.shuffle(orderedReplies);
		for (Object reply : orderedReplies) {
			Integer id = reply.hashCode();
			Long timestamp = timestampsById.get(id);
			if (timestamp == null) {
				timestampsById.put(id, currentTime);
				return reply;
			}
			if ((oldestTimestamp == null) || (oldestTimestamp > timestamp)) {
				oldestTimestamp = timestamp;
				oldestAction = reply;
			}
		}
		timestampsById.put(oldestAction.hashCode(),currentTime);
		return oldestAction;
	}

	//##############################################################################
	// deals with dialog sessions with a particular user (start/stop/cleanup) 
	//##############################################################################
	
	public NLUOutput getNLUforLoginEvent(Long sessionId, DM dm,
			NLUInterface nlu) throws Exception {
		String text=getConfiguration().getLoginEventName();
		List<NLUOutput> userSpeechActs = nlu.getNLUOutputFake(new String[]{"1 "+text}, null);
		NLUOutput selectedUserSpeechAct=dm.selectNLUOutput(text,sessionId, userSpeechActs);
		return selectedUserSpeechAct;
	}
	
	public synchronized void setDialogSession2User(Long sessionID,String user) {
		if (!existDialogSession(sessionID)) {
			logger.warn(" session "+sessionID+" set to user "+session2User.get(sessionID)+" and now being switched to user "+user);
		}
		session2User.put(sessionID, user);
	}
	public String getDialogSessionUser(Long sessionID) {return session2User.get(sessionID);}
	public boolean existDialogSession(Long sessionID) {
		return session2User.containsKey(sessionID);
	}
	public List<Long> getTerminatedSessions(Set<Long> activeSessionsIDs) {
		List<Long> terminatedSessions = new ArrayList<Long>();
		for(Long sessionId : session2PolicyDM.keySet()) {
			DM dm=session2PolicyDM.get(sessionId);
			if (dm.isSessionDone() || (activeSessionsIDs!=null && !activeSessionsIDs.contains(sessionId))) {
				terminatedSessions.add(sessionId);
			}
		}
		return terminatedSessions;
	}
	public Set<Long> getSessions() {
		return session2PolicyDM.keySet();
	}
	public Boolean sessionReadyForTermination(Long sessionId) {
		if (session2PolicyDM.containsKey(sessionId)) {
			DM dm = session2PolicyDM.get(sessionId);
			return dm.isSessionDone();
		} else
			return false;
	}
	public Long startSession(String characterName) {
		Long sid=new Long(999);
		setCharacter4Session(sid,characterName);
		loadSpecialVariablesForSession(sid);
		return sid;
	}
	@Override
	public void saveInformationStateForSession(Long sid,File dumpFile) throws Exception {
		DM dm=getPolicyDMForSession(sid,false);
		if (dm!=null) {
			DialogueKB is = dm.getInformationState();
			if (is!=null) {
				is.dumpKB(dumpFile);
			}
		}
	}
	public File getNewInformationStateFileName(Long sid) throws Exception {
		File sessionDump=null;
		DM dm=getPolicyDMForSession(sid,false);
		if (dm!=null) {
			NLBusConfig dmConfig = dm.getConfiguration();
			String oldDefaultCharacter = dmConfig.getDefaultCharacter();
			String characterName=getCharacterName4Session(sid);
			dmConfig.setDefaultCharacter(characterName);
			String d=dmConfig.getPausedSessionsRoot();
			dmConfig.setDefaultCharacter(oldDefaultCharacter);
			File sessionDir=new File(d+sid+File.separator);
			sessionDir.mkdirs();
			sessionDump=File.createTempFile(sid+"-", ".is", sessionDir);
		}
		return sessionDump;
	}
	@Override
	public void loadInformationStateForSession(Long sid, File fis) throws Exception {
		DM dm=getPolicyDMForSession(sid,false);
		if (dm!=null) {
			DialogueKB is = dm.getInformationState();
			if (is!=null) {
				is.readFromFile(fis);
			}
		}
	}
	public void cleanSessions() {
		for (Long sid : getTerminatedSessions(null)) {
			terminateSession(sid, true);
		}
	}
	public synchronized void terminateSession(Long sessionId, Boolean endInteraction) {
		if (sessionId!=null) {
			DM dm=null;
			try {
				dm=getPolicyDMForSession(sessionId);
			} catch (Exception e) {
				logger.warn("no dm available for session: "+sessionId+"  (probably it's already been terminated).");
			}
			if (dm!=null) dm.kill();
			try {
				killNlu(sessionId);
			} catch (Exception e) {
				logger.warn("exception killing NLU for: "+sessionId+"  (probably it's already been terminated).");
			}
			session2User.remove(sessionId);
			session2Character.remove(sessionId);
			session2PolicyDM.remove(sessionId);
			session2Ignore.remove(sessionId);
			session2specialVars.remove(sessionId);
			Set<Long> handledEvents = session2HandledEvents.get(sessionId);
			if (handledEvents != null)
				handledEvents.clear();
			session2HandledEvents.remove(sessionId);
			logger.info("REMOVED terminated session: "+sessionId);
		}
	}
	public synchronized boolean isThisEventNewForThisSession(Long eventID,Long sessionID) {
		Set<Long> handledEvents=session2HandledEvents.get(sessionID);
		if (!session2HandledEvents.containsKey(sessionID)) {
			session2HandledEvents.put(sessionID,handledEvents=new ConcurrentSkipListSet<Long>());
		}
		//only allow one handler thread to process any given streamevent
		return handledEvents.add(eventID);
	}
	
	
	//##############################################################################
	// library functions to find/compile/validate a dialog policy
	//##############################################################################
	public HashMap<String, String> findAvailablePolicies(String basePoliciesURL) throws Exception {
		logger.info("DIALOGUE POLICIES DIR: " + basePoliciesURL.toString());
		URL policiesDirURL;
		policiesDirURL = ClassLoader.getSystemResource(basePoliciesURL);
		if (policiesDirURL == null)
			policiesDirURL = new File(basePoliciesURL).toURI().toURL();
	
		URI fileURI=policiesDirURL.toURI();
		//URI fileURI=new File()).toURI();
		File characterRoot=new File(fileURI);
		if (!characterRoot.isDirectory()) {
			policiesDirURL = Thread.currentThread().getContextClassLoader().getResource(basePoliciesURL);
			if (policiesDirURL != null)
				characterRoot = new File(policiesDirURL.toURI());
		}
		
		NLBusConfig config=getConfiguration();
		
		String defaultCharacterName=config.getDefaultCharacter();
		if (characterRoot.isDirectory()) {
			for (File file:characterRoot.listFiles()) {
				String characterName=file.getName();
				config.setDefaultCharacter(characterName);
				String name=config.getInitialPolicyFileName();
				File fileForName=new File(name);
				if (doesCharacterExist(config, characterName)) {
					logger.info("Adding POLICY network for characher '"+characterName+"'");
					character2unparsedPolicy.put(characterName, name);
				} else {
					logger.warn("Failed to add POLICY for character '"+characterName+"' because initial file not found and/or character not found.");
					logger.warn("initial policy file: '"+new File(config.getInitialPolicyFileName()).getAbsolutePath()+"'");
				}
			}
		} else throw new Exception("Error: POLICY root must be a directory.");
		config.setDefaultCharacter(defaultCharacterName);
		return character2unparsedPolicy;
	}
	
	public boolean doesCharacterExist(NLBusConfig config,String characterName) {
		String name=config.getInitialPolicyFileName();
		File fileForName=new File(name);
		return (fileForName!=null && fileForName.exists());
	}
	
	public HashMap<String, Object> parseAvailablePolicies(HashMap<String, String> unparsedPolicies) {
		character2parsedPolicy.clear();
		Set<String> toBeRemoved=null;
		for (Entry<String, String> characterAndPolicyURL:unparsedPolicies.entrySet()) {
			String characterName=characterAndPolicyURL.getKey();
			String policyURL=characterAndPolicyURL.getValue();
			try {
				Object parsedDialoguePolicy=getDM().parseDialoguePolicy(policyURL);
				character2parsedPolicy.put(characterName, parsedDialoguePolicy);
			} catch (Exception e) {
				logger.error("Error while parsing policy for character: "+characterName,e);
				logger.error("REMOVING policy.");
				if (toBeRemoved==null) toBeRemoved=new HashSet<String>();
				toBeRemoved.add(characterName);
			}
		}
		if (toBeRemoved!=null) {
			for(String characterName:toBeRemoved) {
				character2parsedPolicy.remove(characterName);
				character2unparsedPolicy.remove(characterName);
			}
		}
		return character2parsedPolicy;
	}
	protected void validateAvailablePolicies(HashMap<String, Object> charactersNames2ParsedPolicy) throws Exception {
		if (charactersNames2ParsedPolicy!=null) {
			NLBusConfig config=getConfiguration();
			NLBusConfig.RunningMode mode=config.getRunningMode();
			try {
				for(String cn:charactersNames2ParsedPolicy.keySet()) {
					Long sid=startSession(cn);
					if (sid!=null) {
						DM dm=getPolicyDMForSession(sid);
						dm.setPauseEventProcessing(true);
						dm.validatePolicy(this);
						dm.kill();
						terminateSession(sid, true);
					}
				}
			} catch (Exception e) {
				config.setRunningMode(mode);
				throw e;
			}
			config.setRunningMode(mode);
		}
	}
	@Override
	public void refreshPolicyForCharacter(String characterName) throws Exception {
		NLBusConfig config=getConfiguration();
		config.setDefaultCharacter(characterName);
		String name=config.getInitialPolicyFileName();
		character2unparsedPolicy.put(characterName, name);
		Object parsedDialoguePolicy=getDM().parseDialoguePolicy(name);
		character2parsedPolicy.put(characterName, parsedDialoguePolicy);
	}
	public void removePolicyForCharacter(String characterName) {
		if (character2unparsedPolicy.containsKey(characterName))
			character2unparsedPolicy.remove(characterName);
		if (character2parsedPolicy.containsKey(characterName))
				character2parsedPolicy.remove(characterName);
	}
	public Map<String,String> getAvailableCharacterNames() {return character2unparsedPolicy;}
		
	public boolean isCharacterPolicyActive(String characterName) {
		return character2parsedPolicy.containsKey(characterName);
	}
	
	public Set<String> getActiveCharacterPolicies() {return character2parsedPolicy.keySet();}
	
	
	
	//##############################################################################
	//    Method to deal with storing received DM events 
	//##############################################################################
	protected HashMap<Long,LinkedBlockingQueue<Event>> session2UnprocessedDMResponses;
	protected boolean holdResponses=false;
	@Override
	public void setHoldProcessingOfResponseEvents(boolean hold) {holdResponses=hold;}
	@Override
	public boolean getHoldProcessingOfResponseEvents() {return holdResponses;}
	@Override
	public LinkedBlockingQueue<Event> getUnprocessedResponseEvents(Long sid) {return session2UnprocessedDMResponses.get(sid);}
	@Override
	public void clearHeldEvents(Long sid) {
		Queue<Event> queue = getUnprocessedResponseEvents(sid);
		if (queue!=null) queue.clear();
	}
	private final Semaphore eventLock=new Semaphore(1);
	public List<NLGEvent> processHeldDMEvents(Long sessionId) throws Exception {
		eventLock.acquire();
		try {
			List<NLGEvent> vcuResponses = new ArrayList<NLGEvent>();
			Queue<Event> systemSpeechActs = getUnprocessedResponseEvents(sessionId);
			if ((systemSpeechActs!=null) && (systemSpeechActs.size() != 0)) {
				DM dm=getPolicyDMForSession(sessionId);
				for (Event dmr : systemSpeechActs) {
					if (dmr instanceof DMSpeakEvent) {
						NLGEvent nlgResult = getNlg(sessionId).doNLG(sessionId, (DMSpeakEvent) dmr,false);
						dm.logEventInChatLog(dmr);
						if (nlgResult!=null) vcuResponses.add(nlgResult);
					}
				}
			}
			clearHeldEvents(sessionId);
			eventLock.release();
			return vcuResponses;
		} catch (Exception e) {
			eventLock.release();
			throw e;
		}
	}

	//##############################################################################
	//    GET/CREATE a DM for a specific session
	//##############################################################################
	public synchronized DM getPolicyDMForSession(Long sid) throws Exception {
		return getPolicyDMForSession(sid, true);
	}
	public synchronized DM getPolicyDMForSession(Long sid,boolean createIfNotThereAlready) throws Exception {
		DM policyDM = session2PolicyDM.get(sid);
		String characterName = getCharacterName4Session(sid);
		if (characterName==null) throw new Exception("Session '"+sid+"' has no character associated.");
		if (policyDM == null) {
			if (createIfNotThereAlready) {
				policyDM=createDMPolicyForCharacter(characterName, sid);
				session2PolicyDM.put(sid,policyDM);
			}
		}
		return policyDM;
	}
	protected DM createDMPolicyForCharacter(String characterName,Long sid) throws Exception {
		logger.info("Starting DM session ("+sid+") for character: "+characterName);
		if (StringUtils.isEmptyString(characterName)) throw new Exception("Request for creating DM instance for character with empty name.");
		Object parsedDialoguePolicy = character2parsedPolicy.get(characterName);
		DM dm=getDM();
		DM policyDM=null;
		if (parsedDialoguePolicy == null) {				
			String unparsedPolicy = character2unparsedPolicy.get(characterName);
			if (StringUtils.isEmptyString(unparsedPolicy)) throw new Exception("Found character with no POLICY associated: '"+characterName+"'");
			logger.warn("SLOW initialization of POLICY for session: "+sid+" (parsing of file).");
			parsedDialoguePolicy=dm.parseDialoguePolicy(unparsedPolicy);
			character2parsedPolicy.put(characterName, parsedDialoguePolicy);
			policyDM=getDM().createPolicyDM(parsedDialoguePolicy,sid,this);
		} else {
			logger.info("QUICK initialization of POLICY for session: "+sid+".");
			policyDM=dm.createPolicyDM(parsedDialoguePolicy,sid,this);
		}
		return policyDM;
	}
	//##############################################################################
	//  GET/CREATE NLU for a specific session
	//##############################################################################
	@Override
	public synchronized NLUInterface getNlu(Long sid) throws Exception {
		NLUInterface nlu=session2NLU.get(sid);
		if (nlu!=null) return nlu;
		else {
			String characterName = getCharacterName4Session(sid);
			NLUConfig config=getNLUConfigurationForCharacter(characterName);
			nlu=(NLUInterface) createSubcomponent(config,config.getNluClass());
			logger.info("Starting NEW NLU for session "+sid+" for character "+characterName+" with nlu class: "+config.getNluClass());
			session2NLU.put(sid, nlu);
			return nlu;
		}
	}
	public void killNlu(Long sid) throws Exception {
		NLUInterface nlu=session2NLU.get(sid);
		if (nlu!=null) {
			session2NLU.remove(sid);
			nlu.kill();
		}
	}
	
	//##############################################################################
	//  GET/CREATE NLG for a specific session
	//##############################################################################
	@Override
	public synchronized NLGInterface getNlg(Long sid) throws Exception {
		return getNlg(sid, true);
	}
	public synchronized NLGInterface getNlg(Long sid,boolean createIfNotThereAlready) throws Exception {
		NLG nlg=session2NLG.get(sid);
		String characterName = getCharacterName4Session(sid);
		boolean characterOK=nlg==null || nlg.getConfiguration().getDefaultCharacter().equals(characterName);
		if (!characterOK) logger.error("NLG for session "+sid+" associated to character '"+nlg.getConfiguration().getDefaultCharacter()+"' but that session is for character '"+characterName+"'.");
		if (nlg!=null && characterOK) return nlg;
		else if (characterOK || createIfNotThereAlready) {
			NLBusConfig config=(NLBusConfig) getConfiguration().clone();
			config.setDefaultCharacter(characterName);
			nlg=(NLG) createSubcomponent(config,config.getNlgClass());
			nlg.setNLModule(this);
			session2NLG.put(sid, nlg);
			return nlg;
		}
		return null;
	}
	
	//##############################################################################
	//  methods used to create all modules that are configured using an NLConfig object
	//##############################################################################
	public static Object createSubcomponent(NLConfig config, String nluClassName) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		if (config!=null) {
			Class cc = Class.forName(nluClassName);
			Class configClass=config.getClass();
			while(configClass!=null) {
				try {
					Constructor nluconstructor = cc.getConstructor(configClass);
					return nluconstructor.newInstance(config);
				} catch (NoSuchMethodException e) {
					configClass=configClass.getSuperclass();
				}
			}
		}
		return null;
	}
	protected NLBusConfig getConfigurationForCharacter(String characterName) throws CloneNotSupportedException {
		NLBusConfig config=character2Config.get(characterName);
		if (config==null) {
			config=(NLBusConfig) getConfiguration().clone();
			config.setDefaultCharacter(characterName);
			character2Config.put(characterName, config);
		}
		return config;
	}
	protected NLUConfig getNLUConfigurationForCharacter(String characterName) throws CloneNotSupportedException {
		return getConfigurationForCharacter(characterName).nluConfig;
	}

	@Override
	public List<SpecialVar> getSpecialVariables(Long sessionId) throws Exception {
		SpecialEntitiesRepository svs = session2specialVars.get(sessionId);
		return (svs!=null)?svs.getAllSpecialVariables():null;
	}

	public static final String userSpeakingStateVarName="nowSpeaking";
	public static final String lengthOfLastThingUserSaidVarName="userSpokeForSeconds";
	public static final String lengthOfLastUserTurnVarName="userTurnSeconds";
	public static final String systemSpeakingStateVarName="systemNowSpeaking";
	public static final String systemSpeakingCompletionVarName="systemFractionSpoke";
	public static final String timeSinceLastUserActionVariableName="timeSinceLastUserAction";
	public static final String timeSinceLastSystemActionVariableName="timeSinceLastSystemAction";
	public static final String counterConsecutiveUnhandledUserActionsVariableName="consecutiveUnhandledUserActions";
	public static final String counterConsecutiveUnhandledUserActionsSinceLastSystemActionVariableName="consecutiveUnhandledUserActionsInTurn";
	public static final String timeSinceLastActionVariableName="timeSinceLastAction";
	public static final String timeSinceLastResourceVariableName="timeSinceLastResource";
	public static final String timeSinceStartVariableName="timeSinceStart";
	public static final String lastEventVariableName="event";
	public static final String hasUserSaidSomethingVariableName="lastUserSpeechAct";
	public static final String lastNonNullOperatorVariableName="lastNonNullSubdialog";
	public static final String lastSystemSayVariableName="systemEvent";
	public static final String timerIntervalVariableName="timerInterval";
	public static final String dmVariableName="dmInstance";
	public static final String activeActionVariableName="activeAction";
	public static final String dormantActionsVariableName="dormantActions";
	public static final String preferFormsVariableName="preferForms";
	public static final String tmpEventVariableName="tmpEvent";
	public static final String userEventsHistory="uEventsHistory";
	public static final String lastUserText="lastUserUtterance";

	
	public void loadSpecialVariablesForSession(Long sid) {
		SpecialEntitiesRepository svs = session2specialVars.get(sid);
		if (svs==null) session2specialVars.put(sid, svs=new SpecialEntitiesRepository(getConfiguration()));
		new SpecialVar(svs,userSpeakingStateVarName,"Boolean flag that if true indicates that the user is speacking","false",Boolean.class,
				VariableProperties.getDefault(PROPERTY.HIDDEN),VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,lengthOfLastThingUserSaidVarName,"Number of seconds the user has spoken last.","0",Number.class,
				VariableProperties.getDefault(PROPERTY.HIDDEN),VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,lengthOfLastUserTurnVarName,"Number of seconds the user has spoken since the last system intervention.","0",Number.class,
				VariableProperties.getDefault(PROPERTY.HIDDEN),VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,systemSpeakingStateVarName,"Boolean flag that if true indicates that the system is speacking","false",Boolean.class,
				VariableProperties.getDefault(PROPERTY.HIDDEN),VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,systemSpeakingCompletionVarName,"fraction of the system utterance being said that has been currently spoken. Updated at each timer interval.","1",Number.class,
				VariableProperties.getDefault(PROPERTY.HIDDEN),VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,timeSinceLastUserActionVariableName,"Time in seconds since the last thing said by the user.","0",Number.class,
				VariableProperties.getDefault(PROPERTY.HIDDEN),VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,timeSinceLastSystemActionVariableName,"Time in seconds since the last thing said by the system.","0",Number.class,
				VariableProperties.getDefault(PROPERTY.HIDDEN),VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,counterConsecutiveUnhandledUserActionsVariableName,"Number of consecutive user actions for which the system had no direct response (handler) across the entire dialogue.","0",Number.class,
				VariableProperties.getDefault(PROPERTY.HIDDEN),VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,counterConsecutiveUnhandledUserActionsSinceLastSystemActionVariableName,"Number of consecutive user actions to which the system didn't have an handler within the same user turn.","0",Number.class,
				VariableProperties.getDefault(PROPERTY.HIDDEN),VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,timeSinceLastActionVariableName,"Time in seconds since anyone said something (user or system).","0",Number.class,
				VariableProperties.getDefault(PROPERTY.HIDDEN),VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,timeSinceLastResourceVariableName,"Time in seconds since the last resource link/video was given.","0",Number.class,
				VariableProperties.getDefault(PROPERTY.HIDDEN),VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,timeSinceStartVariableName,"Time in seconds since the login event.","null",Number.class,
				VariableProperties.getDefault(PROPERTY.HIDDEN),VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,lastEventVariableName,"Name of last speech act received by the system. After the search is done, it contains the single speech act dealt by the selected network.",null,String.class,
				VariableProperties.getDefault(PROPERTY.HIDDEN),VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,hasUserSaidSomethingVariableName,"Name of last speech act received by the system. Not affected by search selection.",null,String.class,
				VariableProperties.getDefault(PROPERTY.HIDDEN),VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,lastNonNullOperatorVariableName,"Name of last sub-dialog executed by the system.",null,String.class,
				VariableProperties.getDefault(PROPERTY.HIDDEN),VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,lastSystemSayVariableName,"Name of the speech act last said by the system.",null,String.class,
				VariableProperties.getDefault(PROPERTY.HIDDEN),VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,timerIntervalVariableName,"Time in seconds between 2 consecutive timer events.","1",Number.class,
				VariableProperties.getDefault(PROPERTY.HIDDEN),VariableProperties.getDefault(PROPERTY.READONLY),false);
		// special variables that should be hidden from the suer using the dialog editor application.
		new SpecialVar(svs,dmVariableName,"DM instance.",null,null,true,VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,activeActionVariableName,"String representation of the current active action.",null,String.class,
				true,VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,dormantActionsVariableName,"Current List of dormant actions.",null,String.class,
				true,VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,preferFormsVariableName,"If true and a form is available for the current system speech act, the form will be selected by the NLG.","true",Boolean.class,
				VariableProperties.getDefault(PROPERTY.HIDDEN),VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,tmpEventVariableName,"Variable used to store the input event that generated one of the internal events (e.g. unhandled, ignore and loop).",null,String.class,
				VariableProperties.getDefault(PROPERTY.HIDDEN),VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,userEventsHistory,"Stores a list of lists of NLUEvents received by the character. The first is the most recent (stack). When a system event is received the next incoming user events are collected in a new list element.",null,Deque.class,
				true,VariableProperties.getDefault(PROPERTY.READONLY),false);
		new SpecialVar(svs,lastUserText,"Stores the last text the user said, as received by the DM.",null,String.class,
				VariableProperties.getDefault(PROPERTY.HIDDEN),VariableProperties.getDefault(PROPERTY.READONLY),false);
		
		if (hasProtocols()) {
			for(Protocol p:getProtocols()) {
				if (p!=null) {
					try {
						List<SpecialVar> vars = p.getSpecialVariables();
						if(vars!=null) {
							for(SpecialVar sv:vars) {
								svs.addSpecialVariable(sv);
							}
						}
					} catch (Exception e) {
						logger.error("Error while adding special variable from Protocol: "+p, e);
					}
				}
			}
		}
	}
	//##############################################################################
	//  methods used to create Protocol object
	//##############################################################################
	public static Protocol createProtocol(NLBus bus, String className) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		if (bus!=null) {
			Class cc = Class.forName(className);
			Class busClass=bus.getClass();
			Constructor constructor = cc.getConstructor(busClass);
			return (Protocol) constructor.newInstance(bus);
		}
		return null;
	}
	
	public NLBusBase() throws Exception {
		session2specialVars=new HashMap<Long,SpecialEntitiesRepository>();
		session2User=new ConcurrentHashMap<Long, String>();
		session2Character = new HashMap<Long, ReferenceToVirtualCharacter>();
		character2unparsedPolicy = new HashMap<String, String>();
		character2parsedPolicy=new HashMap<String, Object>();
		session2NLU=new HashMap<Long, NLUInterface>();
		session2NLG=new HashMap<Long, NLG>();
		session2PolicyDM=new HashMap<Long, DM>();
		character2Config=new HashMap<String, NLBusConfig>();
		session2Ignore=new HashMap<Long,Boolean>();
		session2HandledEvents = new ConcurrentHashMap<Long,Set<Long>>();
		session2UnprocessedDMResponses=new HashMap<Long, LinkedBlockingQueue<Event>>();
		session2ContentTimestamps=new HashMap<Long, Map<Integer,Long>>();
	}
}
