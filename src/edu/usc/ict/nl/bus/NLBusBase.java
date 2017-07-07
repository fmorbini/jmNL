package edu.usc.ict.nl.bus;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.beans.BeansException;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.Event;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.bus.modules.NLGInterface;
import edu.usc.ict.nl.bus.modules.NLUInterface;
import edu.usc.ict.nl.bus.protocols.Protocol;
import edu.usc.ict.nl.bus.special_variables.SpecialEntitiesRepository;
import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.config.DMConfig;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.config.NLConfig;
import edu.usc.ict.nl.config.NLGConfig;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.ne.NamedEntityExtractorI;
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

	protected Map<String,SpecialEntitiesRepository> character2specialVars=null;

	public static final Logger logger = Logger.getLogger(NLBusBase.class.getName());
	static {
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );
	}

	protected NLBusConfig configuration;
	public NLBusConfig getConfiguration() {return configuration;}
	public void setConfiguration(NLBusConfig c) {
		c.fixLinkings();
		this.configuration=c;
	}

	protected static ClassPathXmlApplicationContext context;

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
	@Override
	public List<Protocol> getProtocols() {return protocols;}
	public void addProtocol(Protocol p) {
		if (p!=null) {
			if (this.protocols==null) this.protocols=new ArrayList<Protocol>();
			this.protocols.add(p);
		}
	}

	@Override
	public boolean canDetectUtteranceCompleted() {
		List<Protocol> ps = getProtocols();
		if (ps!=null) {
			for(Protocol p:ps) {
				if (p.canDetectUtteranceCompleted()) return true;
			}
		}
		return false;
	}

	// PER SESSION INFORMATION
	protected Map<Long,String> session2User = null;
	protected Map<Long, Boolean> session2Ignore = null;
	protected Map<Long, ReferenceToVirtualCharacter> session2Character = null;
	protected ConcurrentHashMap<Long, Set<Long>> session2HandledEvents = null;
	protected Map<Long,NLUInterface> session2NLU=null;
	protected Map<Long,NLGInterface> session2NLG=null;
	protected Map<String,NLGInterface> character2NLG=null;
	protected Map<String,DM> character2DM=null;
	protected Map<Long,DM> session2PolicyDM=null;
	protected Map<String,Object> character2parsedPolicy=null;
	protected Map<String,NLBusConfig> character2Config=null;
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

	public Long getNewSessionNumber() {
		if (session2Character.isEmpty()) return 1l;
		else {
			ArrayList<Long> sids = new ArrayList<>(session2Character.keySet());
			Collections.sort(sids);
			Long last=sids.get(sids.size()-1);
			while(session2Character.containsKey(last)) last+=1;
			return last;
		}
	}

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
		String text=dm.getConfiguration().getLoginEventName();
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
		return session2Character.containsKey(sessionID);
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
	@Override
	public synchronized Long startSession(String characterName,Long sid) {
		try {
			if (sid!=null && getDM(sid, false)!=null) {
				terminateSession(sid);
			}
		} catch (Exception e) {}
		if (sid==null) sid=getNewSessionNumber();
		setCharacter4Session(sid,characterName);
		getSpecialVariables(characterName, true);
		try {
			getDM(sid,true);
		} catch (Exception e) {logger.error("Error when creating dm while starting session "+sid+" for character "+characterName+".",e);}
		if (hasListeners()) {
			for(ExternalListenerInterface l:getListeners()) {
				l.startSession(characterName,sid);
			}
		}
		return sid;
	}
	@Override
	public void saveInformationStateForSession(Long sid,boolean save) throws Exception {
		DM dm=getDM(sid,false);
		if (dm!=null) {
			DialogueKB is = dm.getInformationState();
			if (is!=null) {
				is.setValueOfVariable(NLBusBase.timeLastSessionVarName,DialogueKBFormula.create(Math.round(System.currentTimeMillis()/1000)+"", null),ACCESSTYPE.AUTO_OVERWRITEAUTO);
				if (save) {
					File dump=getNewInformationStateFileName(sid);
					is.dumpKB(dump);
				}
			}
		}
	}
	public File getNewInformationStateFileName(Long sid) throws Exception {
		File sessionDump=null;
		DM dm=getDM(sid,false);
		if (dm!=null) {
			String d=dm.getConfiguration().getNlBusConfigNC().getPausedSessionsRoot();
			File sessionDir=new File(d+sid+File.separator);
			sessionDir.mkdirs();
			sessionDump=File.createTempFile("infostate-"+sid+"-", ".is", sessionDir);
		}
		return sessionDump;
	}
	@Override
	public void loadInformationStateForSession(Long sid, Collection<DialogueOperatorEffect> content) throws Exception {
		if (content!=null && !content.isEmpty()) {
			DM dm=getDM(sid,false);
			if (dm!=null) {
				DialogueKB is = dm.getInformationState();
				if (is!=null) {
					is.storeAll(content, ACCESSTYPE.AUTO_OVERWRITEAUTO, true);
				}
			}
		}
	}
	@Override
	public void loadInformationStateForSession(Long sid, File fis) throws Exception {
		if (fis!=null && fis.exists()) {
			DM dm=getDM(sid,false);
			if (dm!=null) {
				DialogueKB is = dm.getInformationState();
				if (is!=null) {
					Collection<DialogueOperatorEffect> content = is.readFromFile(fis);
					if (content!=null && !content.isEmpty()) {
						is.storeAll(content, ACCESSTYPE.AUTO_OVERWRITEAUTO, true);
					}
				}
			}
		}
	}
	public void cleanSessions() {
		for (Long sid : getTerminatedSessions(null)) {
			terminateSession(sid);
		}
	}
	@Override
	public synchronized void terminateSession(Long sessionId) {
		if (sessionId!=null) {
			DM dm=null;
			try {
				dm=getDM(sessionId,false);
			} catch (Exception e) {
				logger.warn("no dm available for session: "+sessionId+"  (probably it's already been terminated).");
			}
			if (dm!=null) {
				try {
					saveInformationStateForSession(sessionId, true);
				} catch (Exception e) {
					logger.warn("error while saving state of: "+sessionId+"  (during termination event).",e);
				}
				session2PolicyDM.remove(sessionId);
				if (!dm.isSessionDone()) dm.kill();
			}

			try {
				killNlu(sessionId);
			} catch (Exception e) {
				logger.warn("exception killing NLU for: "+sessionId+"  (probably it's already been terminated).");
			}
			try {
				killNlg(sessionId);
			} catch (Exception e) {
				logger.warn("exception killing NLG for: "+sessionId+"  (probably it's already been terminated).");
			}
			session2PolicyDM.remove(sessionId);
			session2User.remove(sessionId);
			session2Character.remove(sessionId);
			session2Ignore.remove(sessionId);
			character2specialVars.remove(sessionId);
			Set<Long> handledEvents = session2HandledEvents.get(sessionId);
			if (handledEvents != null) handledEvents.clear();
			session2HandledEvents.remove(sessionId);
			if (hasListeners()) {
				for(ExternalListenerInterface l:getListeners()) {
					l.terminateSession(sessionId);
				}
			}
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
	
	
	/*public String getPausedSessionsRoot(String contentRoot,String characterName) {return getCharacterContentRoot(contentRoot,characterName)+File.separator+"pausedSessions"+File.separator;}
	public String getDMContentRoot(String contentRoot,String characterName) {return getCharacterContentRoot(contentRoot,characterName)+File.separator+"dm"+File.separator;}
	public String getNLUContentRoot(String contentRoot,String characterName) {
		NLUConfig nluConfig;
		try {
			nluConfig = getNLUConfigurationForCharacter(characterName);
			String forcedContentRoot=nluConfig.getForcedNLUContentRoot();
			if (!StringUtils.isEmptyString(forcedContentRoot)) return forcedContentRoot+File.separator;
			else return getCharacterContentRoot(contentRoot,characterName)+File.separator+nluConfig.getNluDir()+File.separator;
		} catch (CloneNotSupportedException e) {
			logger.error(e);
		}
		return null;
	}*/

	public Set<String> findAvailableCharacters(String contentRoot) throws Exception {
		Set<String> ret=null;
		logger.info("DIALOGUE POLICIES DIR: " + contentRoot.toString()+". finding possible character names...");
		URL policiesDirURL;
		policiesDirURL = ClassLoader.getSystemResource(contentRoot);
		if (policiesDirURL == null)
			policiesDirURL = new File(contentRoot).toURI().toURL();

		URI fileURI=policiesDirURL.toURI();
		File characterRoot=new File(fileURI);
		if (!characterRoot.isDirectory()) {
			policiesDirURL = Thread.currentThread().getContextClassLoader().getResource(contentRoot);
			if (policiesDirURL != null)
				characterRoot = new File(policiesDirURL.toURI());
		}

		if (characterRoot.isDirectory()) {
			for (File file:characterRoot.listFiles()) {
				String characterName=file.getName();
				String name=contentRoot+File.separator+characterName+File.separator;
				File chFolder=new File(name);
				if (chFolder.isDirectory()) {
					logger.info(" Adding possible characher name: '"+characterName+"'");
					if (ret==null) ret=new HashSet<>();
					ret.add(characterName);
				}
			}
		} else throw new Exception("Error: POLICY root must be a directory.");
		return ret;
	}

	@Override
	public void refreshPolicyForCharacter(String characterName) throws Exception {
		DM dm=getDMForCharacter(characterName);
		parsePolicyForCharacter(dm);
	}
	public void removePolicyForCharacter(String characterName) {
		if (character2parsedPolicy.containsKey(characterName)) {
			character2parsedPolicy.remove(characterName);
		}
		if (character2DM.containsKey(characterName)) {
			DM dm=character2DM.get(characterName);
			if (dm!=null) dm.kill();
			character2DM.remove(characterName);
		}
	}
	public Set<String> getAvailableCharacterNames() {return character2DM.keySet();}
	public boolean isCharacterPolicyActive(String characterName) {return character2parsedPolicy.containsKey(characterName);}

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
				DM dm=getDM(sessionId);
				for (Event dmr : systemSpeechActs) {
					if (dmr instanceof DMSpeakEvent) {
						NLGEvent nlgResult = getNlg(sessionId).doNLG(sessionId, (DMSpeakEvent) dmr,null,false);
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
	//  GET/CREATE NLU for a specific session
	//##############################################################################
	@Override
	public synchronized NLUInterface getNlu(Long sid) throws Exception {
		NLUInterface nlu=session2NLU.get(sid);
		if (nlu!=null) return nlu;
		else {
			String characterName = getCharacterName4Session(sid);
			NLUConfig config=getNLUConfigurationForCharacter(characterName);
			if (config!=null) {
				logger.info("Starting NEW NLU for session "+sid+" for character "+characterName+" with nlu class: "+config.getNluClass());
				nlu=(NLUInterface) createSubcomponent(config,config.getNluClass());
				session2NLU.put(sid, nlu);
			} else {
				logger.error("No NLU configuration for character: "+characterName+". already termianted?");
			}
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
	//  GET/CREATE DM for a specific session
	//##############################################################################
	public synchronized Map<String,DM> startDMs(Collection<String> characters) {
		character2DM.clear();
		if (characters!=null) {
			logger.info(characters.size()+" character(s) to parse.");
			int i=1;
			for(String c:characters) {
				try {
					logger.info("loading "+i+" of "+characters.size());
					startDMForCharacter(c);
					i++;
				} catch (Exception e) {
					logger.error("error while starting DM",e);
				}
			}
		}
		return character2DM;
	}
	public synchronized DM getDM(Long sid,boolean createIfNotThereAlready) {
		DM dm=session2PolicyDM.get(sid);
		if (dm!=null) return dm;
		else if (createIfNotThereAlready) {
			String characterName = getCharacterName4Session(sid);
			dm=getDMForCharacter(characterName);
			Object policy=character2parsedPolicy.get(characterName);
			if (policy!=null) {
				try {
					dm=dm.createPolicyDM(policy, sid, this);
					logger.info("Using DM for session "+sid+" for character "+characterName+" with dm class: "+dm.getClass().getName());
					session2PolicyDM.put(sid, dm);
					return dm;
				} catch (Exception e) {
					logger.error("Error while starting a new DM for character: "+characterName,e);
				}
			} else {
				logger.warn("NULL policy for character: "+characterName+". Cannot start a session DM.");
			}
		}
		return null;
	}
	@Override
	public synchronized DM getDM(Long sid) {
		return getDM(sid,true);
	}
	private synchronized DM getDMForCharacter(String characterName) {
		DM dm=character2DM.get(characterName);
		if (dm==null) startDMForCharacter(characterName);
		return dm;
	}
	private void startDMForCharacter(final String characterName) {
		try {
			if (!StringUtils.isEmptyString(characterName)) {
				logger.info("starting new template DM for character: "+characterName);
				DMConfig dmConfig = getDMConfigurationForCharacter(characterName);
				DM dm=(DM) createSubcomponent(dmConfig,dmConfig.getDmClass());
				deleteOldPolicyIfThere(characterName);
				parsePolicyForCharacter(dm);
				character2DM.put(characterName, dm);
				logger.info("DONE starting template DM for character: "+characterName);
			}
		} catch (Exception e) {
			logger.error("error starting template dm for character: "+characterName,e);
		}
	}
	private void deleteOldPolicyIfThere(String characterName) {
		if (character2DM.containsKey(characterName) || character2parsedPolicy.containsKey(characterName)) {
			removePolicyForCharacter(characterName);
			logger.warn("Removed old policy and dm for character "+characterName+" before it gets replaced with new one.");
		}
	}
	private void parsePolicyForCharacter(DM dm) throws Exception {
		if (dm!=null) {
			DMConfig dmConfig = dm.getConfiguration();
			String characterName=dmConfig.getNlBusConfigNC().getCharacter();
			String policyLocation=dmConfig.getDMContentRoot()+File.separator+dmConfig.getInitialPolicyFileName();
			Object policy = dm.parseDialoguePolicy(policyLocation);
			logger.info("DONE parsing DM policy for character: "+characterName);
			character2parsedPolicy.put(characterName, policy);
		}
	}
	public void killDM(Long sid) throws Exception {
		DM dm=session2PolicyDM.get(sid);
		if (dm!=null) {
			session2PolicyDM.remove(sid);
			dm.kill();
		}
	}

	//##############################################################################
	//  GET/CREATE NLG for a specific session
	//##############################################################################

	public synchronized Map<String,NLGInterface> startNLGs(Collection<String> characters) {
		character2NLG.clear();
		if (characters!=null) {
			for(String c:characters) {
				try {
					startNLGForCharacter(c,true);
				} catch (Exception e) {
					logger.error("error while starting NLG",e);
				}
			}
		}
		return character2NLG;
	}

	@Override
	public synchronized NLGInterface getNlg(Long sid) throws Exception {
		return getNlg(sid, true);
	}
	public synchronized NLGInterface getNlg(Long sid,boolean createIfNotThereAlready) throws Exception {
		NLGInterface nlg=session2NLG.get(sid);
		if (nlg!=null) return nlg;
		else if (createIfNotThereAlready) {
			String characterName = getCharacterName4Session(sid);
			nlg=getNLGForCharacter(characterName);
			logger.info("Using NLG for session "+sid+" for character "+characterName+" with nlg class: "+nlg.getClass().getName());
			nlg.setNLModule(this);
			session2NLG.put(sid, nlg);
			return nlg;
		}
		return null;
	}
	private synchronized NLGInterface getNLGForCharacter(String characterName) throws Exception {
		NLGInterface nlg=null;
		int count=0;
		while (nlg==null) {
			nlg = character2NLG.get(characterName);
			if (nlg==null) {
				startNLGForCharacter(characterName,true);
			}
			else break;
			if (count>5) {
				logger.error("attempted "+count+" times to start nlg and failed.");
				break;
			}
		}
		character2NLG.put(characterName,null);
		startNLGForCharacter(characterName,false); // build a new one for the character. don't wait.
		return nlg;
	}
	private void startNLGForCharacter(final String characterName,boolean wait) throws Exception {
		//System.out.println(Arrays.toString(Thread.currentThread().getStackTrace()));
		Thread t=new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (!StringUtils.isEmptyString(characterName)) {
						logger.info("starting new NLG for character: "+characterName);
						NLGConfig nlgConfig = getNLGConfigurationForCharacter(characterName);
						NLGInterface nlg=(NLGInterface) createSubcomponent(nlgConfig,nlgConfig.getNlgClass());
						nlg.setNLModule(NLBusBase.this);
						character2NLG.put(characterName, nlg);
						logger.info("DONE starting new NLG for character: "+characterName);
					}
				} catch (Exception e) {
					logger.error("error starting nlg for character: "+characterName,e);
				}
			}
		});
		t.start();
		if (wait) t.join();
	}
	public void killNlg(Long sid) throws Exception {
		NLGInterface nlg=session2NLG.get(sid);
		if (nlg!=null) {
			session2NLG.remove(sid);
			nlg.kill();
		}
	}

	//##############################################################################
	//  methods used to create all modules that are configured using an NLConfig object
	//##############################################################################
	public static Object createSubcomponent(NLConfig config, String nluClassName) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		if (config!=null) {
			if (nluClassName!=null) {
				Class cc = Class.forName(nluClassName);
				Class configClass=config.getClass();
				while(configClass!=null) {
					try {
						Constructor nluconstructor = cc.getConstructor(configClass);
						return nluconstructor.newInstance(config);
					} catch (NoSuchMethodException e) {
						configClass=configClass.getSuperclass();
					} catch (InvocationTargetException e) {
						throw new RuntimeException("Not enough training data!");
					}
				}
			}
		}
		return null;
	}
	protected NLBusConfig getConfigurationForCharacter(String characterName) throws CloneNotSupportedException {
		if (!StringUtils.isEmptyString(characterName)) {
			logger.info("getting configuration for character: "+characterName);
			NLBusConfig config=character2Config.get(characterName);
			if (config==null) {
				config=(NLBusConfig) getConfiguration().clone();
				NLBusConfig pc=getPersonalizedNLConfigurationForCharacter(characterName);
				if (pc!=null) {
					if (pc.getNluConfigNC()!=null) config.setNluConfig(pc.getNluConfigNC());
					if (pc.getNlgConfigNC()!=null) config.setNlgConfig(pc.getNlgConfigNC());
					if (pc.getDmConfigNC()!=null) config.setDmConfig(pc.getDmConfigNC());
				}
				if (config.getNluConfigNC()!=null) config.getNluConfigNC().setNlBusConfig(config);
				if (config.getNlgConfigNC()!=null) config.getNlgConfigNC().setNlBusConfig(config);
				if (config.getDmConfigNC()!=null) config.getDmConfigNC().setNlBusConfig(config);
				config.setCharacter(characterName);
				character2Config.put(characterName, config);
			}
			return config;
		}
		return null;
	}
	public NLBusConfig getPersonalizedNLConfigurationForCharacter(String characterName) {
		File contentRoot=new File(getConfiguration().getContentRoot());
		String characterDirectoryName=contentRoot.getName();
		String classpathPersonalizedConfigName=characterDirectoryName+File.separator+characterName+File.separator+"NLConfig.xml";
		File personalizedConfigFile=new File(contentRoot.getParentFile(),classpathPersonalizedConfigName);
		AbstractXmlApplicationContext context=null;
		if (personalizedConfigFile.exists()) {
			try {
				context = new FileSystemXmlApplicationContext(personalizedConfigFile.getAbsolutePath());
			} catch (BeansException e) {
				logger.error("error while getting personalized configuration file in filesystem: ",e);
			}
		}
		if (context==null) {
			try {
				context = new ClassPathXmlApplicationContext(new String[] {classpathPersonalizedConfigName});
			} catch (BeansException e) {
				Throwable cause=e.getRootCause();
				if (cause==null || !(cause instanceof FileNotFoundException)) {
					logger.error("error while getting personalized configuration file in classpath: ",e);
				} else {
					logger.warn("personalized config not found in classpath for: "+characterName);
				}
			}
		}
		if (context!=null) {
			logger.info("creating NL configuration from file: "+classpathPersonalizedConfigName);
			String[] beans = context.getBeanDefinitionNames();
			if (beans!=null && beans.length>0) {
				for (String b:beans) {
					if (b.equals(characterName)) {
						logger.info("startign bean: "+b);
						NLBusConfig nlModule = (NLBusConfig) context.getBean(b);
						return nlModule;
					}
				}
			} else {
				logger.error("no bean named "+characterName+" found. Not starting anything: "+Arrays.toString(beans));
			}
		} else {
			logger.info("no character specific configuration, using default");
		}
		return null;
	}

	protected NLUConfig getNLUConfigurationForCharacter(String characterName) throws CloneNotSupportedException {
		NLBusConfig busConfig = getConfigurationForCharacter(characterName);
		return busConfig!=null?busConfig.getNluConfigNC():null;
	}
	protected DMConfig getDMConfigurationForCharacter(String characterName) throws CloneNotSupportedException {
		NLBusConfig busConfig = getConfigurationForCharacter(characterName);
		return busConfig!=null?busConfig.getDmConfigNC():null;
	}
	protected NLGConfig getNLGConfigurationForCharacter(String characterName) throws CloneNotSupportedException {
		NLBusConfig busConfig = getConfigurationForCharacter(characterName);
		return busConfig!=null?busConfig.getNlgConfigNC():null;
	}

	@Override
	public List<SpecialVar> getSpecialVariables(Long sessionId) {
		return getSpecialVariables(getCharacterName4Session(sessionId), true);
	}
	@Override
	public List<SpecialVar> getSpecialVariables(String characterName, boolean createIfNotThere) {
		SpecialEntitiesRepository svs = character2specialVars.get(characterName);
		if (svs==null && createIfNotThere) {
			svs=getSpecialVariablesForCharacterName(characterName);
			character2specialVars.put(characterName, svs);
		}
		return (svs!=null)?svs.getAllSpecialVariables():null;
	}
	public SpecialEntitiesRepository getSpecialVariablesForCharacterName(String characterName) {
		SpecialEntitiesRepository svs=new SpecialEntitiesRepository(getConfiguration());
		new SpecialVar(svs,userSpeakingStateVarName,"Boolean flag that if true indicates that the user is speacking","false",Boolean.class,
				null,null,null);
		new SpecialVar(svs,lengthOfLastThingUserSaidVarName,"Number of seconds the user has spoken last.","0",Number.class,
				null,null,null);
		new SpecialVar(svs,lengthOfLastUserTurnVarName,"Number of seconds the user has spoken since the last system intervention.","0",Number.class,
				null,null,null);
		new SpecialVar(svs,systemSpeakingStateVarName,"Boolean flag that if true indicates that the system is speacking","false",Boolean.class,
				null,null,null);
		new SpecialVar(svs,systemSpeakingCompletionVarName,"fraction of the system utterance being said that has been currently spoken. Updated at each timer interval.","1",Number.class,
				null,null,null);
		new SpecialVar(svs,timeSinceLastUserActionVariableName,"Time in seconds since the last thing said by the user.","0",Number.class,
				null,null,null);
		new SpecialVar(svs,timeSinceLastSystemActionVariableName,"Time in seconds since the last thing said by the system.","0",Number.class,
				null,null,null);
		new SpecialVar(svs,counterConsecutiveUnhandledUserActionsVariableName,"Number of consecutive user actions for which the system had no direct response (handler) across the entire dialogue.","0",Number.class,
				null,null,null);
		new SpecialVar(svs,counterConsecutiveUnhandledUserActionsSinceLastSystemActionVariableName,"Number of consecutive user actions to which the system didn't have an handler within the same user turn.","0",Number.class,
				null,null,null);
		new SpecialVar(svs,timeSinceLastActionVariableName,"Time in seconds since anyone said something (user or system).","0",Number.class,
				null,null,null);
		new SpecialVar(svs,timeLastSessionVarName,"Time in seconds since 1970 of the end of last session (when information state was saved).","null",Number.class,
				null,null,true);
		new SpecialVar(svs,timeSinceLastResourceVariableName,"Time in seconds since the last resource link/video was given.","0",Number.class,
				null,null,null);
		new SpecialVar(svs,timeSinceStartVariableName,"Time in seconds since the login event.","null",Number.class,
				null,null,null);
		new SpecialVar(svs,lastEventVariableName,"Name of last speech act received by the system. After the search is done, it contains the single speech act dealt by the selected network.",null,String.class,
				null,null,null);
		new SpecialVar(svs,hasUserSaidSomethingVariableName,"Name of last speech act received by the system. Not affected by search selection.",null,String.class,
				null,null,true);
		new SpecialVar(svs,lastNonNullOperatorVariableName,"Name of last sub-dialog executed by the system.",null,String.class,
				null,null,true);
		new SpecialVar(svs,lastSystemSayVariableName,"Name of the speech act last said by the system.",null,String.class,
				null,null,null);
		new SpecialVar(svs,timerIntervalVariableName,"Time in seconds between 2 consecutive timer events.","1",Number.class,
				null,null,null);
		// special variables that should be hidden from the suer using the dialog editor application.
		new SpecialVar(svs,dmVariableName,"DM instance.",null,null,true,null,null);
		new SpecialVar(svs,activeActionVariableName,"String representation of the current active action.",null,String.class,
				true,null,null);
		new SpecialVar(svs,dormantActionsVariableName,"Current List of dormant actions.",null,String.class,
				true,null,null);
		new SpecialVar(svs,preferFormsVariableName,"If true and a form is available for the current system speech act, the form will be selected by the NLG.","true",Boolean.class,
				null,null,true);
		new SpecialVar(svs,tmpEventVariableName,"Variable used to store the input event that generated one of the internal events (e.g. unhandled, ignore and loop).",null,String.class,
				null,null,null);
		new SpecialVar(svs,userEventsHistory,"Stores a list of lists of NLUEvents received by the character. The first is the most recent (stack). When a system event is received the next incoming user events are collected in a new list element.",null,Deque.class,
				true,null,null);
		new SpecialVar(svs,lastUserText,"Stores the last text the user said, as received by the DM.",null,String.class,
				null,null,true);

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

		try {
			NLUConfig config=getNLUConfigurationForCharacter(characterName);
			List<NamedEntityExtractorI> nes = config.getNluNamedEntityExtractors(PreprocessingType.RUN);
			if(nes!=null) {
				for(NamedEntityExtractorI ne:nes) {
					List<SpecialVar> vs = ne.getSpecialVariables();
					if (vs!=null) {
						for(SpecialVar v:vs) svs.addSpecialVariable(v);
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error while adding special variable from named entity extractors.", e);
		}

		return svs;
	}

	public static final String userSpeakingStateVarName="nowSpeaking";
	public static final String lengthOfLastThingUserSaidVarName="userSpokeForSeconds";
	public static final String lengthOfLastUserTurnVarName="userTurnSeconds";
	public static final String systemSpeakingStateVarName="systemNowSpeaking";
	public static final String systemSpeakingCompletionVarName="systemFractionSpoke";
	public static final String counterConsecutiveUnhandledUserActionsVariableName="consecutiveUnhandledUserActions";
	public static final String counterConsecutiveUnhandledUserActionsSinceLastSystemActionVariableName="consecutiveUnhandledUserActionsInTurn";
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
	public static final String timeSinceLastUserActionVariableName="timeSinceLastUserAction";
	public static final String timeSinceLastSystemActionVariableName="timeSinceLastSystemAction";
	public static final String timeSinceLastActionVariableName="timeSinceLastAction";
	public static final String timeLastSessionVarName="timeOfLastSession";
	public static final String timeSinceLastResourceVariableName="timeSinceLastResource";
	public static final String timeSinceStartVariableName="timeSinceStart";

	//##############################################################################
	//  methods used to create Protocol object
	//##############################################################################
	public static Protocol createProtocol(NLBus bus, String className) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		if (bus!=null) {
			Class cc = Class.forName(className);
			Class busClass=bus.getClass();
			while(busClass!=null) {
				try {
					Constructor constructor = cc.getConstructor(busClass);
					return (Protocol) constructor.newInstance(bus);
				} catch (NoSuchMethodException e) {
					busClass=busClass.getSuperclass();
				}
			}
		}
		return null;
	}

	public NLBusBase() throws Exception {
		character2specialVars=new HashMap<String,SpecialEntitiesRepository>();
		session2User=new ConcurrentHashMap<Long, String>();
		session2Character = new HashMap<Long, ReferenceToVirtualCharacter>();
		session2NLU=new HashMap<Long, NLUInterface>();
		session2NLG=new HashMap<Long, NLGInterface>();
		character2NLG=new HashMap<>();
		character2DM=new HashMap<>();
		character2parsedPolicy=new HashMap<>();
		session2PolicyDM=new HashMap<Long, DM>();
		character2Config=new HashMap<String, NLBusConfig>();
		session2Ignore=new HashMap<Long,Boolean>();
		session2HandledEvents = new ConcurrentHashMap<Long,Set<Long>>();
		session2UnprocessedDMResponses=new HashMap<Long, LinkedBlockingQueue<Event>>();
		session2ContentTimestamps=new HashMap<Long, Map<Integer,Long>>();
	}
}
