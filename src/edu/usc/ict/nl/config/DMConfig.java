package edu.usc.ict.nl.config;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.scxml.model.CustomAction;

import edu.usc.ict.nl.config.NLBusConfig.RunningMode;
import edu.usc.ict.nl.dm.fsm.SCXMLDisableState;
import edu.usc.ict.nl.dm.fsm.SCXMLEnableState;

public class DMConfig extends NLConfig {
	// DM specific
	private String dmClass;
	private float timerInterval;
	private int waitForUserReplyTimeout;
	private String timerEvent;
	private String initialPolicyFileName,specialEntitiesFileName;
	private String unhandledEventName,forcedIgnoreEventName,loginEventName,loopEventName;
	protected boolean systemEventsHaveDuration;
	private boolean preferUserInitiatedActions=true;
	private List<String> valueTrackers=null;
	// case sensitive/insensitive
	private boolean caseSensitive=false;

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
	private int maxSearchLevels=10;

	private boolean dmVhGenerating=false;
	private boolean dmVhListening=false;

	private String visualizerConfig=null,visualizerClass=null;

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
	public String getInitialPolicyFileName() {return initialPolicyFileName;}
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

	public boolean getDmVhGenerating() {return dmVhGenerating;}
	public void setDmVhGenerating(boolean s) {this.dmVhGenerating=s;}
	public boolean getDmVhListening() {return dmVhListening;}
	public void setDmVhListening(boolean s) {this.dmVhListening=s;}

	public boolean getApproximatedForwardSearch() {return approximatedForwardSearch;}
	public void setApproximatedForwardSearch(boolean a) {this.approximatedForwardSearch=a;}
	public int getMaxSearchLevels() {
		return maxSearchLevels;
	}
	public void setMaxSearchLevels(int maxSearchLevels) {
		this.maxSearchLevels = maxSearchLevels;
	}

	public String getVisualizerConfig() {return visualizerConfig;}
	public void setVisualizerConfig(String a) {this.visualizerConfig=a;}
	public String getVisualizerClass() {return visualizerClass;}
	public void setVisualizerClass(String a) {this.visualizerClass=a;}

	public boolean getCaseSensitive() {
		return caseSensitive;
	}
	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	public List<String> getValueTrackers() {
		return valueTrackers;
	}
	public void setValueTrackers(List<String> valueTrackers) {
		this.valueTrackers = valueTrackers;
	}

	@Override
	public DMConfig getDmConfigNC() {
		return this;
	}
	@Override
	public NLUConfig getNluConfigNC() {
		return getNlBusConfigNC().getNluConfigNC();
	}
	@Override
	public NLGConfig getNlgConfigNC() {
		return getNlBusConfigNC().getNlgConfigNC();
	}
	
	public String getDMContentRoot() {return (nlBusConfig!=null)?nlBusConfig.getCharacterContentRoot()+File.separator+"dm"+File.separator:"";}
	
	public DMConfig cloneObject() {
		DMConfig ret=null;
		try {
			// get all methods for which we have a getter and a setter.
			Constructor<? extends DMConfig> constructor = this.getClass().getConstructor();
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
		} catch (Exception e) {e.printStackTrace();}
		return ret;
	}

	public static final DMConfig WIN_EXE_CONFIG=new DMConfig();
	static{
	}
}
