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
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.dm.reward.model.DialogueAction;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorNodeTransition;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;

public class SpeakingTracker {

	private boolean waitForSystemDone=false;
	private NLGEvent speakingThis=null;
	private Float durationOfThingSpoken=0f;
	private Float durationOfAudioPortionOfThingSpoken=0f;
	private Long timeStartedSpeaking=null;
	private DialogueAction waitingAction=null;
	private Pair<DialogueOperatorNodeTransition,Event> waitingTransitionWithState=null;
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
		if (waitForSystemDone && speakingThis!=null) {
			try {
				Float duration=durationOfThingSpoken;
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
		if (durationOfAudioPortionOfThingSpoken!=null && timeStartedSpeaking!=null && durationOfAudioPortionOfThingSpoken>=0 && timeStartedSpeaking>0) {
			float p=(durationOfAudioPortionOfThingSpoken>0)?((float)(System.currentTimeMillis()-timeStartedSpeaking))/(durationOfAudioPortionOfThingSpoken*1000f):1;
			if (p>1) p=1;
			return p;
		}
		return null;
	}
	private static DialogueOperatorEffect setSpeackingAsTrue,setSpeackingAsFalse;
	static {
		try{
			setSpeackingAsTrue=DialogueOperatorEffect.createAssignment(NLBusBase.systemSpeakingStateVarName, true);
			setSpeackingAsFalse=DialogueOperatorEffect.createAssignment(NLBusBase.systemSpeakingStateVarName, false);
		} catch (Exception e) {e.printStackTrace();}
	}
	public void setSpeaking(NLGEvent ev) throws Exception {
		speakingThis=ev;
		durationOfThingSpoken=dm.getMessageBus().getNlg(dm.getSessionID()).getDurationOfThisDMEvent(dm.getSessionID(), speakingThis);
		durationOfAudioPortionOfThingSpoken=durationOfThingSpoken;
		final String say=getSpeakingSpeechAct();
		dm.getLogger().info("got duration for event: "+say+" as: "+durationOfThingSpoken);
		if (isSpeaking()) {
			timeStartedSpeaking=System.currentTimeMillis();
			DialogueKBInterface is = dm.getInformationState();
			is.store(setSpeackingAsTrue, ACCESSTYPE.AUTO_OVERWRITEAUTO,true);

			Timer timer = getTimer();
			Float duration = durationOfThingSpoken;
			if (duration==null || duration<=0) duration=30f;
			else duration*=1.2f; // increase the backup event duration by 20%
			if (tasks.containsKey(say)) {
				dm.getLogger().warn("NOT setting up BACKUP animation complete sender for '"+say+"' because one already present.");
				dm.getLogger().warn(" -> ms until execution: "+(System.currentTimeMillis()-tasks.get(say).scheduledExecutionTime()));
			} else {
				long ms=Math.round(duration*1000);
				dm.getLogger().info("Setting up BACKUP animation complete sender for '"+say+"' with duration: "+duration);
				TimerTask task = new TimerTask() {
					@Override
					public void run() {
						dm.getLogger().warn("sending BACKUP animation complete event for: '"+say+"'");
						dm.handleEvent(new SystemUtteranceDoneEvent(say, dm.getSessionID()));
					}
				};
				tasks.put(say, task);
				timer.schedule(task,ms);
			}
		}
	}
	public Timer getTimer() {return tt;}
	public DialogueOperatorNodeTransition getWaitingTransition() {return (waitingTransitionWithState!=null)?waitingTransitionWithState.getFirst():null;}
	public void setWaitingTransition(DialogueAction waitingAction,DialogueOperatorNodeTransition waitingTransition, Event sourceEvent) {
		Logger logger = dm.getLogger();
		if (hasAlreadyAWaitingAction()) {
			if (this.waitingAction!=waitingAction) {
				logger.warn(" speaking tracker: has already a waiting action: "+this.waitingAction.getOperator().getName());
				logger.warn(" speaking tracker: setting old waiting action as running. Replacing waiting action with: "+waitingAction.getOperator().getName());
				this.waitingAction.setAsRunning();
			} else if (waitingTransition!=this.waitingTransitionWithState.getFirst()) {
				logger.warn(" speaking tracker: has already the action waiting but setting a different transition: "+waitingTransition);
			}
		}
		this.waitingAction=waitingAction;
		this.waitingTransitionWithState=new Pair<DialogueOperatorNodeTransition,Event>(waitingTransition,sourceEvent);
	}
	private boolean hasAlreadyAWaitingAction() {return waitingAction!=null && waitingTransitionWithState!=null;}

	private String getSpeakingSpeechAct() {
		if (speakingThis!=null) {
			return speakingThis.getDMEventName();
		}
		return null;
	}
	public NLGEvent getCurrentlySpeackingEvent() {return speakingThis;}
	
	public void finishedSpeakingThis(Event evSaid) throws Exception {
		String thingSaid=evSaid.getName();
		String spokenSpeechAct=getSpeakingSpeechAct();
		if (tasks.containsKey(thingSaid))
			tasks.remove(thingSaid).cancel();
		if (!StringUtils.isEmptyString(spokenSpeechAct) && hasAlreadyAWaitingAction()) {
			Float f=getSpokenFraction();
			Float th=dm.getConfiguration().getSpokenFractionForSaid();
			if (f != null && th!=null && f<th) {
				dm.getLogger().info("setting action '"+waitingAction+"' as interrupted. Percentage completed: "+f+" th="+th);
				waitingAction.setTransitionAsInterrupted(waitingTransitionWithState.getFirst());
			} else {
				dm.getLogger().info("setting action '"+waitingAction+"' as said. Percentage completed: "+f+" th="+th);
				waitingAction.setTransitionAsSaid(waitingTransitionWithState.getFirst());
			}
			if (waitingAction.isPaused()) {
				if (!spokenSpeechAct.equals(thingSaid)) {
					dm.getLogger().error("no match between waiting event: '"+spokenSpeechAct+"' and received done event. Doing NOTHING.");
				} else {
					dm.getLogger().info("MATCH between waiting event: '"+spokenSpeechAct+"' and received done event. AWAKENING action '"+waitingAction.getOperator().getName()+"'");
					DialogueAction actionToBeAwoken=waitingAction;
					DialogueOperatorNodeTransition transitionToBeTaken=waitingTransitionWithState.getFirst();
					Event sourceEvent=waitingTransitionWithState.getSecond();
					resetTrackerState();
					if (transitionToBeTaken==null) {
						dm.getLogger().error("Waiting action has no waiting transiton. Don't know what to do.");
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
			} else if (waitingAction.isInInvalidState()) {
				dm.getLogger().warn(" action '"+waitingAction.getOperator().getName()+"' is in an INVALID state.");
				resetTrackerState();
			} else {
				dm.getLogger().warn(" action '"+waitingAction.getOperator().getName()+"' is not anymore waiting.");
				resetTrackerState();
			}
		} else {
			if (!hasAlreadyAWaitingAction())
				dm.getLogger().info("Received an animation complete for "+thingSaid+" but no waiting action.");
			else
				dm.getLogger().error("Incorrect speaking tracker state: "+speakingThis+" action: "+waitingAction+" paused while taking transiton: "+waitingTransitionWithState);
			resetTrackerState();
		}
	}
	public void gotLengthForThisEvent(String thingSaid,Float length) {
		if (getSpeakingSpeechAct()!=null && getSpeakingSpeechAct().equals(thingSaid)) {
			timeStartedSpeaking=System.currentTimeMillis();
			durationOfAudioPortionOfThingSpoken=length;
		}
	}

	public Event getInterruptionSourceEvent() {return interruptionSourceEvent;}
	
	private void resetTrackerState() {
		dm.getLogger().info("resetting speaking tracker state.");
		waitingAction=null;
		waitingTransitionWithState=null;
		speakingThis=null;
		durationOfThingSpoken=null;
		durationOfAudioPortionOfThingSpoken=null;
		timeStartedSpeaking=null;
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
