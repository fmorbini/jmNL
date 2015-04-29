package edu.usc.ict.nl.config;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.scxml.model.CustomAction;

import edu.usc.ict.nl.dm.fsm.SCXMLDisableState;
import edu.usc.ict.nl.dm.fsm.SCXMLEnableState;
import edu.usc.ict.nl.nlg.VRexpressBasicNLG;
import edu.usc.ict.nl.util.StringUtils;

public class NLBusConfig extends NLConfig {
	public static final String DM_INTERNAL_ID = "dialogManager";
	public static final String SIMCOACH_INTERNAL_ID = "simcoach";

	// generic
	private RunningMode mode=null;
	private boolean loadBalancing = true;
	private boolean alwaysPreferForms;
	
	//SimSenSei specific, directory where BML, sound, and viseme files are located
	private String filestoreRoot;
	
	//vh setup
	private String vhSpeaker=null,vhTopic=null,vhServer=null,vhComponentId=null;
	private String vhOtherSpeaker=null;
	private boolean dmVhGenerating=false,nlgVhGenerating=false;
	private boolean dmVhListening=false,nlgVhListening=false;
	private boolean pmlListening=false;
	private boolean minatListening=false;
	
	// vh toolkit
	protected boolean isVRexpressBasicNLG() {
		return getNlgClass().equals(VRexpressBasicNLG.class.getCanonicalName());
	}
	
	//set the root of where to find content stuff (immediate subdirectories of this are the characters)
	private String contentRoot;
	
	private String internalDmClass4VhMsgWrapper;
	
	// feedback config
	private String feedbackInputForm;
	
	// Stemmer
	private String stemmerClass;
	
	// DM specific
	private String dmClass;
	private float timerInterval;
	private int waitForUserReplyTimeout;
	private String timerEvent;
	private String initialPolicyFileName,specialEntitiesFileName;
	private String unhandledEventName,forcedIgnoreEventName,loginEventName,loopEventName;
	private String systemUtterancesFile,systemResourcesFile,systemFormsFile,nvbFile=null; 
	protected boolean systemEventsHaveDuration;
	private boolean preferUserInitiatedActions=true;
	private List<String> valueTrackers=null;
	// case sensitive/insensitive
	private boolean caseSensitive=false;
	
	private List<String> protocols=null;
	
	// related to turn taking
	protected boolean skipUnhandledWhileSystemSpeaking=false;
	protected Float spokenFractionForSaid=null;
	protected boolean userAlwaysInterrupts=false;
	
	// FSM DM specific
	private boolean staticURLs;
	private List<CustomAction> scxmlCustomAction=Arrays.asList(
			new CustomAction("http://simcoach/custom","enable", SCXMLEnableState.class),
			new CustomAction("http://simcoach/custom","disable", SCXMLDisableState.class));
	private List<String> trivialSAs=Arrays.asList("signal-non-understanding", "answer.dont-know");
	private String policiesDirectory;
	// REWARD DM specific
	private boolean approximatedForwardSearch=false;
	
	// NLG specific
	private String nlgClass;
	private boolean strictNLG=false;
	private boolean allowEmptyNLGOutput=true;
	private boolean displayFormAnswerInNlg=true;
	
	private String visualizerConfig=null,visualizerClass=null;

	
	private String defaultCharacter=null;

	public NLBusConfig() {}

	public NLBusConfig cloneObject() {
		NLBusConfig ret=null;
		try {
			// get all methods for which we have a getter and a setter.
			Constructor<? extends NLBusConfig> constructor = this.getClass().getConstructor();
			ret=constructor.newInstance();
			Method[] publicMethods = getClass().getMethods();
			if (publicMethods!=null) {
				Map<String,Method> mTable=new HashMap<String, Method>();
				for(Method m:publicMethods) mTable.put(m.getName(),m);
				filterMethodsLeavingOnlyGettersAndSetters(mTable);
				for(String m:mTable.keySet()) {
					if (isGetter(m)) {
						Method getter=mTable.get(m);
						Method setter=mTable.get(getSetter(m));
						if (getter!=null && setter!=null) {
							Object v=getter.invoke(this);
							setter.invoke(ret, v);
						}
					}
				}
			}
			/*
			Constructor<? extends NLConfig> constructor = this.getClass().getConstructor();
			ret=constructor.newInstance();
			Class C = getClass();
			Stack<Class> fromRootToThis=new Stack<Class>();
			fromRootToThis.push(C);
			while ((C = C.getSuperclass()) != null) fromRootToThis.push(C);
			while(!fromRootToThis.isEmpty()) {
				C=fromRootToThis.pop();
				Field[] fields=C.getDeclaredFields();
				if (fields!=null) {
					for(Field f:fields) {
						try {
							Object value=f.get(this);
							System.out.println("setting field: "+f.getName()+" from "+f.get(ret)+" to "+value);
							f.set(ret, value);
						} catch (IllegalAccessException e) {e.printStackTrace()}
					}
				}
			}
		*/
		} catch (Exception e) {e.printStackTrace();}
		return ret;
	}
	
	private boolean isGetter(String name) { return name.startsWith("get"); }
	private String getSetter(String name) { return name.replaceFirst("get", "set"); }
	private void filterMethodsLeavingOnlyGettersAndSetters(Map<String, Method> mTable) {
		if (mTable!=null) {
			List<String> toBeRemoved=null;
			for(String mName:mTable.keySet()) {
				if (mName!=null && isGetter(mName)) {
					String sName=getSetter(mName);
					if (!mTable.containsKey(sName)) {
						if (toBeRemoved==null) toBeRemoved=new ArrayList<String>();
						toBeRemoved.add(mName);
					}
				}
			}
			if (toBeRemoved!=null) for(String k:toBeRemoved) mTable.remove(k);
		}
	}

	public String chatLog;
	public boolean isLoggingEventsInChatLog = true;
	
	//chat specific
	public boolean displayNluOutputChat=true;
	public float zoomFactorChat=1;
	public boolean allowNluTraining=true;
	
	/** Smartbody media file root, directory where BML, sound, and viseme files are located */
	public String getFileRoot() { return this.filestoreRoot; }
	public void setFileRoot(String fileRoot) { this.filestoreRoot = fileRoot; }
	/** Running Mode */
	public void setRunningMode(RunningMode mode) {this.mode=mode;}
	public RunningMode getRunningMode() {return mode;}
	/** Load-balancing Mode */
	public final boolean getIsLoadBalancing() { return loadBalancing; }
	public final void setIsLoadBalancing(boolean loadBalancing) { this.loadBalancing = loadBalancing; }
	/** Prefer Forms Mode */
	public final boolean getAlwaysPreferForms() { return alwaysPreferForms; }
	public final void setAlwaysPreferForms(boolean status) { this.alwaysPreferForms = status; }
	
	/** content Root directory */
	public String getContentRoot() {return contentRoot;}
	public void setContentRoot(String root) {this.contentRoot = root;}

	public String getCharacterContentRoot() {return new File(getContentRoot(),getDefaultCharacter()).getAbsolutePath();}

	public String getDMContentRoot() {return getCharacterContentRoot()+File.separator+"dm"+File.separator;}
	public String getPausedSessionsRoot() {return getCharacterContentRoot()+File.separator+"pausedSessions"+File.separator;}
	public String getTargetDialoguesRoot() {return getDMContentRoot()+File.separator+"target dialogues"+File.separator;}
	public String getXLSXContentRoot() {return getCharacterContentRoot()+File.separator+"content"+File.separator;}
	/** DM interval between timer events in seconds */
	public float getTimerInterval() {return timerInterval;}
	public void setTimerInterval(float t) {this.timerInterval = t;}
	public int getWaitForUserReplyTimeout() {return this.waitForUserReplyTimeout;}
	public void setWaitForUserReplyTimeout(int w) {this.waitForUserReplyTimeout=w;}
	/** timer event name */
	public String getTimerEvent() {return timerEvent;}
	public void setTimerEvent(String e) {this.timerEvent=e;}
	/** SCXML DM, setup that all imported urls are static (that is pointing to files instead of dynamic web services */
	/** sets whether system events have non-0 duration */
	public void setSystemEventsHaveDuration(boolean s) {this.systemEventsHaveDuration=s;}
	public boolean getSystemEventsHaveDuration() {return this.systemEventsHaveDuration;}
	public void setSkipUnhandledWhileSystemSpeaking(boolean s) {this.skipUnhandledWhileSystemSpeaking=s;}
	public boolean getSkipUnhandledWhileSystemSpeaking() {return this.skipUnhandledWhileSystemSpeaking;}
	public Float getSpokenFractionForSaid() {return spokenFractionForSaid;}
	public void setSpokenFractionForSaid(Float spokenFractionForSaid) {this.spokenFractionForSaid = spokenFractionForSaid;}
	public boolean getUserAlwaysInterrupts() {return userAlwaysInterrupts;}
	public void setUserAlwaysInterrupts(boolean b) {this.userAlwaysInterrupts=b;}
	public void setPreferUserInitiatedActions(boolean s) {this.preferUserInitiatedActions=s;}
	public boolean getPreferUserInitiatedActions() {return this.preferUserInitiatedActions;}
	public boolean getStaticURLs() {return staticURLs;}
	public void setStaticURLs(boolean s) {this.staticURLs=s;}
	/** file name containing a policy */
	public String getInitialPolicyFileName() {return getDMContentRoot()+initialPolicyFileName;}
	public void setInitialPolicyFileName(String fn) {this.initialPolicyFileName=removeAbsolutePath(fn);}
	/** file that contains the definitions of the DM specific special variables */
	public String getSpecialVariablesFileName() {return specialEntitiesFileName;}
	public void setSpecialVariablesFileName(String specialEntitiesFileName) {this.specialEntitiesFileName = specialEntitiesFileName;}
	/** scxml dm custom action */
	public List<CustomAction> getScxmlCustomActions() {return scxmlCustomAction;}
	//public void setScxmlCustomActions(List<CustomAction> cas) {this.scxmlCustomAction=cas;}
	/** dm name of unhandled event */
	public String getUnhandledEventName() {return unhandledEventName;}
	public void setUnhandledEventName(String name){this.unhandledEventName=name;}
	/** dm name of force-ignore event */
	public String getForcedIgnoreEventName() {return forcedIgnoreEventName;}
	public void setForcedIgnoreEventName(String name){this.forcedIgnoreEventName=name;}
	/** dm name of login event */
	public String getLoginEventName() {return loginEventName;}
	public void setLoginEventName(String name){this.loginEventName=name;}
	/** dm name of loop event */
	public String getLoopEventName() {return loopEventName;}
	public void setLoopEventName(String name){this.loopEventName=name;}
	/** scxml dm, list of trivial sa to not be used to update the info state */
	public List<String> getTrivialSystemSpeechActs() {return trivialSAs;}
	public void setTrivialSystemSpeechActs(List<String> sas){this.trivialSAs=sas;}
	/** nlu and dm class to be used to create nlu and dm instances */
	public String getDmClass() {return dmClass;}
	public void setDmClass(String dm) {this.dmClass=dm;}
	public String getNlgClass() {return nlgClass;}
	public void setNlgClass(String nlg) {this.nlgClass=nlg;}
	/** dm wrapper */
	public String getInternalDmClass4VhMsgWrapper() {return internalDmClass4VhMsgWrapper;}
	public void setInternalDmClass4VhMsgWrapper(String c) {internalDmClass4VhMsgWrapper=c;}
	
	public List<String> getValueTrackers() {
		return valueTrackers;
	}
	public void setValueTrackers(List<String> valueTrackers) {
		this.valueTrackers = valueTrackers;
	}
	/** name of the file used to log the conversations tried in the chat window */
	public String getChatLog() {return chatLog;}
	public void setChatLog(String c) {this.chatLog=c;}
	public boolean getLoggingEventsInChatLog() { return this.isLoggingEventsInChatLog; }
	public void setLoggingEventsInChatLog(boolean logging) { this.isLoggingEventsInChatLog = logging; }
	public boolean getDisplayNluOutputChat() {return this.displayNluOutputChat;}
	public void setDisplayNluOutputChat(boolean o) {this.displayNluOutputChat=o;}
	public float getZoomFactorChat() {return this.zoomFactorChat;}
	public void setZoomFactorChat(float z) {this.zoomFactorChat=z;}
	public boolean getAllowNluTraining() {return this.allowNluTraining;}
	public void setAllowNluTraining(boolean s) {this.allowNluTraining=s;}
	
	public final boolean getIsStrictNLG() { return strictNLG; }
	public final void setIsStrictNLG(boolean s) { this.strictNLG = s; }
	public boolean getDisplayFormAnswerInNlg() {return displayFormAnswerInNlg;}
	public void setDisplayFormAnswerInNlg(boolean s) {this.displayFormAnswerInNlg=s;}
	public boolean getAllowEmptyNLGOutput() {return allowEmptyNLGOutput;}
	public void setAllowEmptyNLGOutput(boolean allowEmptyNLGOutput) {this.allowEmptyNLGOutput = allowEmptyNLGOutput;}

	public String getDefaultCharacter() {return defaultCharacter;}
	public void setDefaultCharacter(String cn) {this.defaultCharacter = cn;}

	public boolean getApproximatedForwardSearch() {return approximatedForwardSearch;}
	public void setApproximatedForwardSearch(boolean a) {this.approximatedForwardSearch=a;}

	public String getVhTopic() {return vhTopic;}
	public void setVhTopic(String t) {this.vhTopic=t;}
	public String getVhSpeaker() {return vhSpeaker;}
	public void setVhSpeaker(String s) {this.vhSpeaker=s;}
	public String getVhOtherSpeaker() {return vhOtherSpeaker;}
	public void setVhOtherSpeaker(String s) {this.vhOtherSpeaker=s;}
	public String getVhServer() {return (vhServer!=null)?vhServer:"localhost";}
	public void setVhServer(String s) {this.vhServer=s;}
	public String getVhComponentId() {return vhComponentId;}
	public void setVhComponentId(String s) {this.vhComponentId=s;}
	public boolean getDmVhListening() {return dmVhListening;}
	public boolean getNlgVhListening() {return nlgVhListening;}
	public void setDmVhListening(boolean s) {this.dmVhListening=s;}
	public void setNlgVhListening(boolean s) {this.nlgVhListening=s;}
	public boolean getDmVhGenerating() {return dmVhGenerating;}
	public boolean getNlgVhGenerating() {return nlgVhGenerating;}
	public void setDmVhGenerating(boolean s) {this.dmVhGenerating=s;}
	public void setNlgVhGenerating(boolean s) {this.nlgVhGenerating=s;}
	public boolean getPmlListening() {return pmlListening;}
	public void setPmlListening(boolean s) {this.pmlListening=s;}
	public boolean getMinatListening() {return minatListening;}
	public void setMinatListening(boolean s) {this.minatListening=s;}
	
	
	public String getVisualizerConfig() {return visualizerConfig;}
	public void setVisualizerConfig(String a) {this.visualizerConfig=a;}
	public String getVisualizerClass() {return visualizerClass;}
	public void setVisualizerClass(String a) {this.visualizerClass=a;}
	
	public static enum RunningMode {EXE,ADVICER,AUTHORING};
	public boolean isInExecuteMode() { return getRunningMode()==RunningMode.EXE; }
	public boolean isInAdvicerMode() { return getRunningMode()==RunningMode.ADVICER; }
	public boolean isInAuthoringMode() { return getRunningMode()==RunningMode.AUTHORING; }

	public String getStemmerClass() {return this.stemmerClass;}
	public void setStemmerClass(String sc) {this.stemmerClass=sc;}
	
	public String getFeedbackInputform() {return feedbackInputForm;}
	public void setFeedbackInputForm(String i) {this.feedbackInputForm=i;}
	
	public String getSystemUtterances() {return getXLSXContentRoot()+systemUtterancesFile;}
	public void setSystemUtterances(String file) {this.systemUtterancesFile = removeAbsolutePath(file);}
	public String getNvbs() {return getXLSXContentRoot()+nvbFile;}
	public void setNvbs(String file) {this.nvbFile = removeAbsolutePath(file);}
	public String getSystemForms() {return getXLSXContentRoot()+systemFormsFile;}
	public void setSystemForms(String file) {this.systemFormsFile = removeAbsolutePath(file);}
	public String getSystemResources() {return getXLSXContentRoot()+systemResourcesFile;}
	public void setSystemResources(String file) {this.systemResourcesFile = removeAbsolutePath(file);}

	public boolean getNluVhListening() {return nluConfig.getNluVhListening();}
	public boolean getNluVhGenerating() {return nluConfig.getNluVhGenerating();}
	public String getLowConfidenceEvent() {return nluConfig.getLowConfidenceEvent();}

	public boolean getCaseSensitive() {
		return caseSensitive;
	}
	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}
	
	public boolean hasVHConfig() {
		String activeMQserver=getVhServer();
		String activeMQtopic=getVhTopic();
		return !StringUtils.isEmptyString(activeMQserver) && !StringUtils.isEmptyString(activeMQtopic);
	}

	public List<String> getProtocols() {
		return protocols;
	}
	public void setProtocols(List<String> protocols) {
		this.protocols = protocols;
	}
	
	// sample config used to run mxnlu during testing
	public static final NLBusConfig WIN_EXE_CONFIG=new NLBusConfig();
	static{
		WIN_EXE_CONFIG.setRunningMode(RunningMode.EXE);
		WIN_EXE_CONFIG.setIsLoadBalancing(false);
		WIN_EXE_CONFIG.setAlwaysPreferForms(false);
		WIN_EXE_CONFIG.setContentRoot("resources/characters/");
		WIN_EXE_CONFIG.setDefaultCharacter("Bill_Ford_PB");
		WIN_EXE_CONFIG.setStemmerClass("edu.usc.ict.nl.stemmer.KStemmer");
		WIN_EXE_CONFIG.nluConfig=NLUConfig.WIN_EXE_CONFIG;
		WIN_EXE_CONFIG.setSystemUtterances("system-utterances.xlsx");
		WIN_EXE_CONFIG.setSystemForms("forms.xlsx");
	}

}
