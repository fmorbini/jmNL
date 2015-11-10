package edu.usc.ict.nl.dm.reward;

import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.bus.events.Event;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.bus.events.SystemUtteranceDoneEvent;
import edu.usc.ict.nl.bus.events.SystemUtteranceInterruptedEvent;
import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.bus.modules.NLGInterface;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.config.NLGConfig;
import edu.usc.ict.nl.dm.reward.model.DialogueAction;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorNodeTransition;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;

public class SpeakingTracker {

	private static DialogueOperatorEffect setSpeackingAsTrue,setSpeackingAsFalse;
	static {
		try{
			setSpeackingAsTrue=DialogueOperatorEffect.createAssignment(NLBusBase.systemSpeakingStateVarName, true);
			setSpeackingAsFalse=DialogueOperatorEffect.createAssignment(NLBusBase.systemSpeakingStateVarName, false);
		} catch (Exception e) {e.printStackTrace();}
	}

	private boolean waitForSystemDone=false;
	private DialogueActionSpeakingTransitionWaiting speakingActionState=null;
	private final DM dm;
	private Timer tt;
	private Map<String,TimerTask> tasks = new ConcurrentHashMap<String,TimerTask>();
	private Event interruptionSourceEvent=null;
	
	public SpeakingTracker(DM dm, Timer timer) throws Exception {
		NLBusConfig config=dm.getConfiguration();
		this.waitForSystemDone=config.getSystemEventsHaveDuration();
		this.dm=dm;
		this.tt=timer;
	}

	public boolean isSpeaking() {
		if (waitForSystemDone && speakingActionState!=null) {
			try {
				Float duration=speakingActionState.getExpectedLengthOfSpeech();
				if ((duration==null) || (duration>0)) return true;
				else return false;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return false;
		} else return false;
	}
	/**
	 * 
	 * @return a number between 0 and 1 that indicates the portion of the amount of time allocated for this utterance that has passed.
	 */
	public Float getSpokenFraction() {
		if (speakingActionState!=null) return speakingActionState.getSpokenFraction();
		else return null;
	}

	public void setSpeaking(NLGEvent ev) throws Exception {
		final Logger logger = dm.getLogger();
		if (speakingActionState.setSpeakingEvent(ev)) {
			NLGInterface nlg = dm.getMessageBus().getNlg(dm.getSessionID());
			NLGConfig nlgConfig=nlg.getConfiguration();
			speakingActionState.setExpectedLengthOfSpeech(nlg.getDurationOfThisDMEvent(dm.getSessionID(), ev));
			final String say=getSpeakingSpeechAct();
			logger.info("got duration for event: "+say+" as: "+speakingActionState.getExpectedLengthOfSpeech());
			if (isSpeaking()) {
				speakingActionState.setStartOfSpeech(System.currentTimeMillis());
				DialogueKBInterface is = dm.getInformationState();
				is.store(setSpeackingAsTrue, ACCESSTYPE.AUTO_OVERWRITEAUTO,true);

				Timer timer = getTimer();
				Float duration = speakingActionState.getExpectedLengthOfSpeech();
				boolean busTracksUtterancesDuration=dm.getMessageBus().canDetectUtteranceCompleted();
				if (duration==null || duration<=0 || busTracksUtterancesDuration) {
					if (busTracksUtterancesDuration) dm.getLogger().warn("setting buckup timer duration to default as bus tracks utterances.");
					duration=nlgConfig.getDefaultDuration();
				}
				else duration*=1.2f; // increase the backup event duration by 20%
				if (tasks.containsKey(say)) {
					logger.warn("NOT setting up BACKUP animation complete sender for '"+say+"' because one already present.");
					logger.warn(" -> ms until execution: "+(System.currentTimeMillis()-tasks.get(say).scheduledExecutionTime()));
				} else {
					long ms=Math.round(duration*1000);
					logger.info("Setting up BACKUP animation complete sender for '"+say+"' with duration: "+duration);
					TimerTask task = new TimerTask() {
						@Override
						public void run() {
							logger.warn("sending BACKUP animation complete event for: '"+say+"'");
							dm.handleEvent(new SystemUtteranceDoneEvent(say, dm.getSessionID()));
						}
					};
					tasks.put(say, task);
					timer.schedule(task,ms);
				}
			}
		} else {
			logger.error("invalid tracker state (while setting thing spoken): "+speakingActionState);
			resetTrackerState();
		}
	}
	public Timer getTimer() {return tt;}
	public DialogueOperatorNodeTransition getWaitingTransition() {return (speakingActionState!=null)?speakingActionState.getTransition():null;}
	public void setSpeakingTransition(DialogueAction speakingAction,DialogueOperatorNodeTransition speakingTransition, String speakingThisSA,Event sourceEvent) {
		Logger logger = dm.getLogger();
		if (hasAlreadyAWaitingAction()) {
			if (!speakingActionState.byThisAction(speakingAction)) {
				logger.warn(" speaking tracker: has already a waiting action: "+speakingActionState.getOperator());
				logger.warn(" speaking tracker: setting old waiting action as running. Replacing waiting action with: "+speakingAction.getOperator().getName());
				speakingActionState.getAction().setAsRunning();
				speakingActionState.setWaitingState(speakingAction,speakingTransition,speakingThisSA);
			} else if (!speakingActionState.byThisTransition(speakingTransition)) {
				logger.warn(" speaking tracker: has already the action waiting but setting a different transition: "+speakingTransition);
				speakingActionState.setWaitingState(speakingAction,speakingTransition,speakingThisSA);
			}
		} else {
			speakingActionState=new DialogueActionSpeakingTransitionWaiting(speakingAction,speakingTransition,speakingThisSA,sourceEvent,logger);
		}
	}
	private boolean hasAlreadyAWaitingAction() {return speakingActionState!=null;}

	private String getSpeakingSpeechAct() {return speakingActionState!=null?speakingActionState.getSpeechAct():null;}
	public NLGEvent getCurrentlySpeackingEvent() {return speakingActionState.getNLGEvent();}
	public String getCurrentlySpeackingSA() {return speakingActionState.getSpeechAct();}
	
	public void finishedSpeakingThis(Event evSaid) throws Exception {
		String thingSaid=evSaid.getName();
		String spokenSpeechAct=getSpeakingSpeechAct();
		if (tasks.containsKey(thingSaid))
			tasks.remove(thingSaid).cancel();
		Logger logger = dm.getLogger();
		if (!StringUtils.isEmptyString(spokenSpeechAct) && hasAlreadyAWaitingAction()) {
			if (speakingActionState.isPaused()) {
				if (!spokenSpeechAct.equals(thingSaid)) {
					logger.error("no match between waiting event: '"+spokenSpeechAct+"' and received done event. Doing NOTHING.");
				} else {
					logger.info("MATCH between waiting event: '"+spokenSpeechAct+"' and received done event.");
					
					if (speakingActionState.isSpeaking()) {
						Float f=getSpokenFraction();
						Float th=dm.getConfiguration().getSpokenFractionForSaid();
						DialogueAction waitingAction=speakingActionState.getAction();
						if (f != null && th!=null && f<th) {
							logger.info("setting action '"+waitingAction+"' as interrupted. Percentage completed: "+f+" th="+th);
							waitingAction.setTransitionAsInterrupted(speakingActionState.getTransition());
						} else {
							logger.info("setting action '"+waitingAction+"' as said. Percentage completed: "+f+" th="+th);
							waitingAction.setTransitionAsSaid(speakingActionState.getTransition());
						}
					}
					
					logger.info("AWAKENING action '"+speakingActionState.getOperator()+"'");

					DialogueAction actionToBeAwoken=speakingActionState.getAction();
					DialogueOperatorNodeTransition transitionToBeTaken=speakingActionState.getTransition();
					Event sourceEvent=speakingActionState.getSourceEvent();
					resetTrackerState();
					if (transitionToBeTaken==null) {
						logger.error("Waiting action has no waiting transiton. Don't know what to do.");
					} else {
						actionToBeAwoken.setAsRunning();
						if (evSaid instanceof SystemUtteranceInterruptedEvent) {
							interruptionSourceEvent=((SystemUtteranceInterruptedEvent) evSaid).getSourceEvent();
							actionToBeAwoken.resumeExeFromInterruption(transitionToBeTaken,sourceEvent);
						} else {
							actionToBeAwoken.resumeExeFromFinishedSystemAction(transitionToBeTaken,sourceEvent);
						}
					}
				}
			} else if (speakingActionState.getAction().isInInvalidState()) {
				logger.warn(" action '"+speakingActionState.getOperator()+"' is in an INVALID state.");
				resetTrackerState();
			} else {
				logger.warn(" action '"+speakingActionState.getOperator()+"' is not anymore waiting.");
				resetTrackerState();
			}
		} else {
			if (!hasAlreadyAWaitingAction()) {
				logger.info("Received an animation complete for "+thingSaid+" but no waiting action.");
			} else {
				logger.error("Incorrect speaking tracker state:\n "+speakingActionState);
			}
			resetTrackerState();
		}
	}
	public void gotLengthForThisEvent(String thingSaid,Float length) {
		if (getSpeakingSpeechAct()!=null && getSpeakingSpeechAct().equals(thingSaid)) {
			speakingActionState.setStartOfSpeech(System.currentTimeMillis());
			speakingActionState.setExpectedLengthOfSpeech(length);
		}
	}

	public Event getInterruptionSourceEvent() {return interruptionSourceEvent;}
	
	private void resetTrackerState() {
		dm.getLogger().info("resetting speaking tracker state.");
		speakingActionState=null;
		interruptionSourceEvent=null;
		try {
			DialogueKBInterface is = dm.getInformationState();
			is.setValueOfVariable(NLBusBase.systemSpeakingCompletionVarName, 1f,ACCESSTYPE.AUTO_OVERWRITEAUTO);
			is.store(setSpeackingAsFalse, ACCESSTYPE.AUTO_OVERWRITEAUTO,true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
