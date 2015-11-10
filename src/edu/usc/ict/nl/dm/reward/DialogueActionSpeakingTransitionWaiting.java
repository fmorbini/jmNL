package edu.usc.ict.nl.dm.reward;

import org.apache.log4j.Logger;

import edu.usc.ict.nl.bus.events.Event;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.dm.reward.model.DialogueAction;
import edu.usc.ict.nl.dm.reward.model.DialogueOperator;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorNodeTransition;
import edu.usc.ict.nl.util.StringUtils;

public class DialogueActionSpeakingTransitionWaiting {
	private DialogueAction waitingAction=null; //this is the action waiting for a certain speech to be done (it usually is the same action that started the speech) 
	private DialogueOperatorNodeTransition waitingTransition=null;//same as above but for the particular transition in the action that is waiting.
	private Event sourceEvent=null;//the trigger for the speech or waiting
	private NLGEvent speakingThis=null;//the speech act name should be equal to speakingSA. Is the NLGEvent that is spoken
	private String waitingSA=null;//before the nlg event is received
	private Logger logger;
	private Float durationOfThingSpoken=0f;
	private Float durationOfAudioPortionOfThingSpoken=0f;
	private Long timeStartedSpeaking=null;

	public DialogueActionSpeakingTransitionWaiting(DialogueAction speakingAction,DialogueOperatorNodeTransition speakingTransition, String speakingSA,Event sourceEvent, Logger logger) {
		this.waitingAction=speakingAction;
		this.waitingTransition=speakingTransition;
		this.sourceEvent=sourceEvent;
		this.waitingSA=speakingSA;
		this.logger=logger;
	}
	public void setWaitingState(DialogueAction speakingAction, DialogueOperatorNodeTransition speakingTransition, String speakingThisSA) {
		this.waitingAction=speakingAction;
		this.waitingTransition=speakingTransition;
		this.waitingSA=speakingThisSA;
	}
	public DialogueOperatorNodeTransition getTransition() {return waitingTransition;}
	public String getSpeechAct() {
		if (speakingThis!=null) return speakingThis.getDMEventName();
		else if (!StringUtils.isEmptyString(waitingSA)) return waitingSA;
		else return null;
	}
	public NLGEvent getNLGEvent() { return speakingThis; }
	public boolean isPaused() { return waitingAction!=null?waitingAction.isPaused():false;}
	public boolean isSpeaking() { return waitingAction!=null?waitingAction.isTransitionBeingSpoken(waitingTransition):false;}
	public DialogueAction getAction() {return waitingAction;}
	public DialogueOperator getOperator() {return waitingAction!=null?waitingAction.getOperator():null;}
	public boolean byThisAction(DialogueAction a) {
		return waitingAction!=null?waitingAction.equals(a):false;
	}
	public boolean byThisTransition(DialogueOperatorNodeTransition tr) {
		return waitingTransition!=null?waitingTransition==tr:false;
	}
	public Event getSourceEvent() {
		return sourceEvent;
	}
	
	@Override
	public String toString() {
		return "Action "+waitingAction+" is waiting at transition "+waitingTransition+" for event "+speakingThis+" speech act "+waitingSA+", to be done. Source event: "+sourceEvent;
	}
	public boolean setSpeakingEvent(NLGEvent ev) {
		if (ev!=null) {
			if (speakingThis==null && (waitingSA==null || waitingSA.equalsIgnoreCase(ev.getDMEventName()))) {
				speakingThis=ev;
				waitingSA=ev.getDMEventName();
				return true;
			} else {
				logger.error("waiting action state has already a speaking event and is different from given nlg event!");
				logger.error(" nlg event: "+ev);
				logger.error(" state: "+this);
			}
		} else {
			logger.warn("null event given to set nlg event for waiting action state.");
		}
		return false;
	}
	public Float getExpectedLengthOfSpeech() {return durationOfThingSpoken;}
	public Float getSpokenFraction() {
		if (durationOfAudioPortionOfThingSpoken!=null && timeStartedSpeaking!=null && durationOfAudioPortionOfThingSpoken>=0 && timeStartedSpeaking>0) {
			float p=(durationOfAudioPortionOfThingSpoken>0)?((float)(System.currentTimeMillis()-timeStartedSpeaking))/(durationOfAudioPortionOfThingSpoken*1000f):1;
			if (p>1) p=1;
			return p;
		}
		return null;
	}
	public void setExpectedLengthOfSpeech(Float durationOfThisDMEvent) {
		this.durationOfThingSpoken=durationOfThisDMEvent;
		this.durationOfAudioPortionOfThingSpoken=durationOfThisDMEvent;
	}
	public void setStartOfSpeech(long ms) {
		this.timeStartedSpeaking=ms;
	}
}
