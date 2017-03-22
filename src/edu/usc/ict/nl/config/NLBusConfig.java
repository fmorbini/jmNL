package edu.usc.ict.nl.config;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
	
	//SimSenSei specific, directory where BML, sound, and viseme files are located
	private String filestoreRoot;
	
	//vh setup
	private String vhSpeaker=null,vhTopic=null,vhServer=null,vhComponentId=null;
	private String vhOtherSpeaker=null;
	private boolean useVrExpressOnly=false;
	private boolean useVrSpeakOnly=false;
	
	private boolean validatePolicies=true;
	private boolean validatePoliciesOnStartup=true;

	// vh toolkit
	protected boolean isVRexpressBasicNLG() {
		return nlgConfig.getNlgClass().equals(VRexpressBasicNLG.class.getCanonicalName());
	}
	
	//set the root of where to find content stuff (immediate subdirectories of this are the characters)
	private String contentRoot;
	
	private String internalDmClass4VhMsgWrapper;
	
	// feedback config
	private String feedbackInputForm;
	
	private List<String> protocols=null;
	

	
	private String character=null;

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
	
	/** content Root directory */
	public String getContentRoot() {return contentRoot;}
	public void setContentRoot(String root) {this.contentRoot = root;}

	public String getCharacterContentRoot() {
		String ch=getCharacter();
		return new File(getContentRoot(),ch!=null?ch:"null").getAbsolutePath();
	}
	public String getPausedSessionsRoot() {return getCharacterContentRoot()+File.separator+"pausedSessions"+File.separator;}
	public String getDMContentRoot() {return getCharacterContentRoot()+File.separator+"dm"+File.separator;}
	public String getTargetDialoguesRoot() {return getDMContentRoot()+File.separator+"target dialogues"+File.separator;}
	public String getXLSXContentRoot() {return getCharacterContentRoot()+File.separator+"content"+File.separator;}
	

	/** dm wrapper */
	public String getInternalDmClass4VhMsgWrapper() {return internalDmClass4VhMsgWrapper;}
	public void setInternalDmClass4VhMsgWrapper(String c) {internalDmClass4VhMsgWrapper=c;}
	
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
	
	public String getCharacter() {return character;}
	public void setCharacter(String cn) {this.character = cn;}

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

	public void setUseVrExpressOnly(Boolean s) {this.useVrExpressOnly=s;}
	public void setUseVrSpeakOnly(Boolean s) {this.useVrSpeakOnly=s;}
	public boolean getUseVrExpressOnly() {return this.useVrExpressOnly;}
	public boolean getUseVrSpeakOnly() {return this.useVrSpeakOnly;}
	
	public static enum RunningMode {EXE,ADVICER,AUTHORING};
	public boolean isInExecuteMode() { return getRunningMode()==RunningMode.EXE; }
	public boolean isInAdvicerMode() { return getRunningMode()==RunningMode.ADVICER; }
	public boolean isInAuthoringMode() { return getRunningMode()==RunningMode.AUTHORING; }

	public String getFeedbackInputform() {return feedbackInputForm;}
	public void setFeedbackInputForm(String i) {this.feedbackInputForm=i;}
	

	public boolean getNluVhListening() {return nluConfig.getNluVhListening();}
	public boolean getNluVhGenerating() {return nluConfig.getNluVhGenerating();}
	public boolean getDmVhListening() {return dmConfig.getDmVhListening();}
	public boolean getNlgVhListening() {return nlgConfig.getNlgVhListening();}
	public boolean getDmVhGenerating() {return dmConfig.getDmVhGenerating();}
	public boolean getNlgVhGenerating() {return nlgConfig.getNlgVhGenerating();}

	public String getLowConfidenceEvent() {return nluConfig.getLowConfidenceEvent();}

	
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
	
	public boolean getValidatePolicies() {return validatePolicies;}
	public void setValidatePolicies(boolean validatePolicies) {
		this.validatePolicies = validatePolicies;
	}	
	public boolean isValidatePoliciesOnStartup() { return validatePoliciesOnStartup; } 
	public void setValidatePoliciesOnStartup(boolean validatePoliciesOnStartup) { this.validatePoliciesOnStartup = validatePoliciesOnStartup; }
	
	// sample config used to run mxnlu during testing
	public static final NLBusConfig WIN_EXE_CONFIG=new NLBusConfig();
	static{
		WIN_EXE_CONFIG.setRunningMode(RunningMode.EXE);
		WIN_EXE_CONFIG.setIsLoadBalancing(false);
		WIN_EXE_CONFIG.setContentRoot("resources/characters/");
		WIN_EXE_CONFIG.nluConfig=NLUConfig.WIN_EXE_CONFIG;
		WIN_EXE_CONFIG.nlgConfig=NLGConfig.WIN_EXE_CONFIG;
		//WIN_EXE_CONFIG.setSystemUtterances("system-utterances.xlsx");
		//WIN_EXE_CONFIG.setSystemForms("forms.xlsx");
	}

	public static void main(String[] args) {
		List<String> list = WIN_EXE_CONFIG.getAllConfigurationFields();
		Collections.sort(list);
		System.out.println(list);
	}
	
}
