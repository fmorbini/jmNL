package edu.usc.ict.nl.dm.reward.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.bus.events.Event;
import edu.usc.ict.nl.bus.events.NLUEvent;
import edu.usc.ict.nl.bus.events.changes.DMStateChangeEvent;
import edu.usc.ict.nl.bus.events.changes.StateChange;
import edu.usc.ict.nl.dm.reward.RewardDM;
import edu.usc.ict.nl.dm.reward.SpeakingTracker;
import edu.usc.ict.nl.dm.reward.SwapoutReason;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.kb.TrivialDialogueKB;
import edu.usc.ict.nl.nlu.ChartNLUOutput;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.Triple;
import edu.usc.ict.nl.util.graph.Edge;

public class DialogueAction {
	
	/*
	 *  PAUSED is when the action is waiting for a system line to complete
	 *  RE_ENTERED is when the action is being woken up from the dormant queue
	 *  JUST_CREATED as it says
	 *  RUNNING it's the normal state
	 *  INVALID marks an action that cannot be executed  
	 */
	private static enum ActionState {JUST_CREATED,RUNNING,RE_ENTERED,PAUSED,INVALID};
	private static enum SAYState {SAYING,SAID,INTERRUPTED};
	
	private EvalContext context=null;
	
	// is an instantiated operator that is being executed.
	private ActiveStates activeStates;//LinkedHashSet<DialogueOperatorNode> activeStates;
	private Map<DialogueOperatorNodeTransition,SAYState> sayStateTracker;
	private RewardDM dm;
	private ActionState state=ActionState.JUST_CREATED;
	/**
	 * this contains the reason for the latest time this action has been swapped out.
	 */
	private SwapoutReason swapoutReason=null;
	private DialogueOperatorEntranceTransition entranceCondition;
	private DialogueKB internalKB;
	private DialogueKB localVarKB;
	private double rewardAlreadyGot=0;
	
	public RewardDM getDM() {return dm;}
	
	public DialogueKB getInternalKB() {
		assert((localVarKB==null) || (internalKB==localVarKB));
		return internalKB;
	}
	public DialogueKB getLocalVarKB() {return localVarKB;}
	public EvalContext getContext() {
		// returns the context making sure that its KB is set to the
		// local KB of this action.
		return context.setInformationState(getInternalKB());
	}
	
	public DialogueAction() {
		activeStates=new ActiveStates();
	}
	
	public DialogueAction(DialogueOperatorEntranceTransition ec, RewardDM rewardDM) {
		this();
		this.entranceCondition=ec;
		this.dm=rewardDM;
		sayStateTracker=new HashMap<DialogueOperatorNodeTransition, DialogueAction.SAYState>();
		context=new EvalContext(ec.getOperator());
		setAsCreated();
	}
	public DialogueOperatorEntranceTransition getEntranceTransition() {return entranceCondition;}

	public void setSwapoutReason(SwapoutReason swapoutReason) {
		this.swapoutReason = swapoutReason;
	}
	public SwapoutReason getSwapoutReason() {
		return swapoutReason;
	}
	
	public void setAsCreated() {
		dm.getLogger().info("setting action "+getOperator().getName()+" as created.");
		if (isInInvalidState()) dm.getLogger().error("FAILED to set state as action is in INVALID state.");
		else state=ActionState.JUST_CREATED;
	}
	public void setAsRunning() {
		dm.getLogger().info("setting action "+getOperator().getName()+" as running.");
		if (isInInvalidState()) dm.getLogger().error("FAILED to set state as action is in INVALID state.");
		else state=ActionState.RUNNING;
	}
	public void setAsPaused(String reason) throws Exception {
		dm.getLogger().info("setting action "+getOperator().getName()+" as paused. Reason: "+reason);
		if (isInInvalidState()) dm.getLogger().error("FAILED to set state as action is in INVALID state.");
		else {
			state=ActionState.PAUSED;
		}
	}
	public void setAsReentered(DialogueOperatorEntranceTransition ec) {
		dm.getLogger().info("setting action "+getOperator().getName()+" as re-entered.");
		if (isInInvalidState()) dm.getLogger().error("FAILED to set state as action is in INVALID state.");
		else {
			state=ActionState.RE_ENTERED;
			entranceCondition=ec;
			resetTimerEventsInCurrentState();
			resettingInterruptionInformation();
		}
	}
	private void resettingInterruptionInformation() {
		dm.getLogger().info("Resetting interruption information.");
		setSwapoutReason(null);
		sayStateTracker.clear();
	}

	public void setAsInvalid(String reason) {
		//System.out.println(Arrays.toString(Thread.currentThread().getStackTrace()));
		dm.getLogger().info("setting action '"+getOperator().getName()+"' as INVALID because: "+reason+".");
		state=ActionState.INVALID;
	}
	public boolean isInInvalidState() {return state==ActionState.INVALID;}
	public boolean isPaused() {return state==ActionState.PAUSED;}
	public boolean isJustCreated() {return state==ActionState.JUST_CREATED;}
	public ActiveStates getActiveStates() {return activeStates;}

	/*public double reEvaluateExpectedReward(DialogueKBInterface informationState) throws Exception {
		HashSet<Pair<List<Object>, LinkedHashSet<DialogueOperatorEffect>>> effectsSets = getPossibleSetsOfEffects();
		PossibleIS root=new PossibleIS(informationState);

		NetworkTransition fakeRootEntranceCondition=new NetworkTransition();
		fakeRootEntranceCondition.setOperator(getOperator());
		dm.computePossibilityGraph(root,effectsSets,fakeRootEntranceCondition);
		
		root.toGDLGraph("test-current.gdl");
		HashMap<NetworkTransition, Double> rewards = dm.computeExpectedRewardsForPossibilities(root);
		if ((rewards==null) || (rewards.size()>1) || rewards.isEmpty()) throw new Exception("Error while re-evaluating curretn action: "+rewards);
		//return rewardAlreadyGot+rewards.values().iterator().next();
		return rewards.values().iterator().next();
	}*/

	public List<DialogueOperatorNodesChain> getPossibleSetsOfEffects() throws Exception {
		List<DialogueOperatorNodesChain> effectsSets=null;
		DialogueOperator op=getOperator();
		for(DialogueOperatorNode state:activeStates) {
			List<DialogueOperatorEffect> effs = state.getEffects();
			state.setEffects(null);
			List<DialogueOperatorNodesChain> chains_effects = op.traverseOperatorAndCollectEffects(state);
			if (chains_effects!=null) {
				if (effectsSets==null) effectsSets=new ArrayList<DialogueOperatorNodesChain>();
				effectsSets.addAll(chains_effects);
			}
			state.setEffects(effs);
		}
		return effectsSets;
	}
	
	// returns true if one of the active states is a final state.
	public boolean getDone() { return activeStates.getDone(); }

	public boolean isFinal() {
		return entranceCondition.getOperator().isFinal();
	}

	public void execute(Event sourceEvent,DialogueKB initIS) throws Exception {
		DialogueOperator op=entranceCondition.getOperator();
		if (activeStates==null) throw new Exception("Action with NULL active states.");
		String evName=(sourceEvent!=null)?sourceEvent.getName():null;

		/* if it's the first execution:
		 *  if the event is null, then execute the initial state.
		 *  else activate the first handler for that event
		 * else:
		 *  use the active states only to process the input event. If event is null or event not handled return exception
		 */
		switch (state) {
		case INVALID:
			dm.getLogger().warn("call to execute action: "+getOperator().getName()+" but action is in INVALID state.");
			break;
		case PAUSED:
			dm.getLogger().warn("call to execute action: "+getOperator().getName()+" while action waiting for complete event.");
			break;
		case JUST_CREATED:
			Map<String, DialogueKBFormula> localVars = op.getLocalVars();
			if (localVars!=null && !localVars.isEmpty()) {
				if (localVarKB==null) localVarKB=new TrivialDialogueKB(initIS);
				for(String varName:localVars.keySet()) {
					DialogueKBFormula varValue = localVars.get(varName);
					Object result = localVarKB.evaluate(varValue,null);
					DialogueOperatorEffect effect=DialogueOperatorEffect.createAssignment(varName, result);
					localVarKB.store(effect, ACCESSTYPE.AUTO_OVERWRITETHIS, false);
					//if (result==null) localVarKB.setValueOfVariable(varName, varValue);
					//else localVarKB.setValueOfVariable(varName, DialogueKBFormula.parse(result.toString()));
				}
			}
			if (localVarKB!=null) {
				localVarKB.setParent(initIS);
				internalKB=localVarKB;
			} else internalKB=(DialogueKB)initIS;
		case RE_ENTERED:
			TimemarksTracker tt = dm.getTimemarkTracker();
			if (tt!=null) tt.setMark(getOperator().getName(),TimemarksTracker.TYPES.ENTER,null);
			assert((getInternalKB()==initIS) || ((getLocalVarKB()!=null) && (getInternalKB().getParent()==initIS)));
			activeStates.clear();

			if (entranceCondition!=null) {
				activeStates.transition(null,(DialogueOperatorNode) entranceCondition.getSource());
				if ((evName!=null) && entranceCondition.isEventGoodForTransition(evName)) evName=null;
				takeTransition(entranceCondition, context.setInformationState(internalKB),sourceEvent,STOP.NOTHING,true);
			}
			if (!isPaused() && !dm.isThisActionDormant(this)) setAsRunning();
			else break;
		case RUNNING:
			// check active states for the first executable transition
			for(DialogueOperatorNode state:activeStates) {
				List<Edge> transitions=state.getOutgoingEdges();
				if (transitions!=null) {
					for(Edge e:transitions) {
						DialogueOperatorNodeTransition tr=(DialogueOperatorNodeTransition) e;
						boolean eventGoodForTransition=tr.isEventGoodForTransition(evName);
						if (eventGoodForTransition) {
							boolean conditionGoodForTransition=tr.isConditionSatisfiedInCurrentIS(context.setInformationState(internalKB));
							if (conditionGoodForTransition) { 
								if (evName!=null) evName=null;
								takeTransition(tr,context.setInformationState(internalKB),sourceEvent,STOP.NOTHING,true);
								break;
							} else if (tr.doesConsume()) break;
						}
					}
				}
			}
			break;
		default:
			throw new Exception("Unknown actions state.");
		}
	}
	
	public boolean isTransitionBeingSpoken(DialogueOperatorNodeTransition tr) {
		if (sayStateTracker.containsKey(tr)) {
			SAYState state=sayStateTracker.get(tr);
			return (state==SAYState.SAYING);
		} else return false;
	}
	private boolean hasTransitionAlreadyBeenSaid(DialogueOperatorNodeTransition tr) {
		if (sayStateTracker.containsKey(tr)) {
			SAYState state=sayStateTracker.get(tr);
			return (state==SAYState.SAID);
		} else return false;
	}
	private boolean hasTransitionAlreadyBeenAttempted(DialogueOperatorNodeTransition tr) {
		if (sayStateTracker.containsKey(tr)) {
			SAYState state=sayStateTracker.get(tr);
			return state!=null;
		} else return false;
	}
	private boolean hasTransitionBeenInterrupted(DialogueOperatorNodeTransition tr) {
		if (sayStateTracker.containsKey(tr)) {
			SAYState state=sayStateTracker.get(tr);
			return state!=null && state==SAYState.INTERRUPTED;
		} else return false;
	}
	
	public void setTransitionAsSaid(DialogueOperatorNodeTransition tr) {
		if (tr.isSayTransition() || tr.isWaitTransition()) {
			if (sayStateTracker.containsKey(tr)) sayStateTracker.put(tr, SAYState.SAID);
			else dm.getLogger().error("attempted to set not currently speaking transition to SAID.");
		}
	}
	public void setTransitionAsInterrupted(DialogueOperatorNodeTransition tr) throws Exception {
		if (tr.isSayTransition() || tr.isWaitTransition()) {
			if (sayStateTracker.containsKey(tr)) sayStateTracker.put(tr, SAYState.INTERRUPTED);
			else dm.getLogger().error("attempted to set not currently speaking transition to INTERRUPTED.");
		}
	}
	public void setTransitionAsSaying(DialogueOperatorNodeTransition tr,Event sourceEvent) {
		dm.getLogger().info("Setting say transition: "+tr+". if possible: issay="+tr.isSayTransition()+" getsay="+tr.getSay()+".");
		boolean isSayTransition=tr.isSayTransition();
		if (isSayTransition || tr.isWaitTransition()) sayStateTracker.put(tr, SAYState.SAYING);
		if (isSayTransition) {
			String sa=tr.getSay();
			if (!StringUtils.isEmptyString(sa)) {
				dm.getSpeakingTracker().setSpeakingTransition(this,tr,sa,sourceEvent);
			} else {
				dm.getLogger().error("!!not setting waiting transition in tracker as say event is null!!");
			}
		}
	}
	public void setTransitionAsWaitingForCurrentSpeach(DialogueOperatorNodeTransition tr,Event sourceEvent) {
		String sa=dm.getSpeakingTracker().getCurrentlySpeackingSA();
		if (!StringUtils.isEmptyString(sa)) {
			dm.getSpeakingTracker().setSpeakingTransition(this,tr,sa,sourceEvent);
		} else {
			dm.getLogger().error("!!not setting waiting transition in tracker as current event event is null!!");
		}
	}
	
	/** 
	 * this is called when an interruption is received (i.e. an interrupted system done event is received). This happens when
	 * the system was saying a line (or an action with a duration) and during that time an interruption was received (either by
	 * the automatic global interruption policy (config file) or by a custom interruption policy in the dialog policy.  
	 * @param tr
	 * @param sourceEvent
	 * @throws Exception
	 */
	public void resumeExeFromInterruption(DialogueOperatorNodeTransition tr,Event sourceEvent) throws Exception {
		dm.getLogger().info("Starting execution of action from interruption.");
		takeTransition(tr, context.setInformationState(internalKB), sourceEvent,STOP.SYSTEM,true);
	}
	/**
	 * compared to the above, this is executed when a normal system done event is received. 
	 * @param tr
	 * @param sourceEvent
	 * @throws Exception
	 */
	public void resumeExeFromFinishedSystemAction(DialogueOperatorNodeTransition tr,Event sourceEvent) throws Exception {
		takeTransition(tr, context.setInformationState(internalKB), sourceEvent, STOP.NOTHING, true);
	}
	private static enum STOP {SYSTEM,USER,NOTHING};
	public void takeTransition(final DialogueOperatorNodeTransition tr,EvalContext context,final Event sourceEvent,STOP stopCondition,boolean root) throws Exception {
		final Logger logger = dm.getLogger();
		if (root) logger.info("Starting execution of action. Stop criterion: "+stopCondition);
		DialogueOperatorNode startState = (DialogueOperatorNode) tr.getSource();
		DialogueOperatorNode endState = (DialogueOperatorNode) tr.getTarget();

		if (hasTransitionBeenInterrupted(tr)) {
			logger.info("swapping out '"+this.getOperator().getName()+"' because it has been interrupted at: "+tr);
			((DialogueOperatorNode)tr.getTarget()).doSwapOut(this,new SwapoutReason(tr));
			return;
		}

		/*
		 * boolean say=tr is a say transition and not said already and it will say something given the current information state;
		 * if say and events have durations
		 *  then
		 *   if the system is speaking?
		 *    then
		 *     pause action
		 *    else
		 *     mark as saying
		 *     execute transition
		 *     pause the action
		 *     (ext)==> when finished-speaking received mark as said and awaken action
		 *  else (tr is not a say transition or already said or it is one but information state says it's not going to say anything)
		 *   if (state of tr is unknown (said/saying/interrupted)
		 *    then
		 *     just execute
		 *   update states
		 *   ...
		 */
		
		boolean sayTransitionToBeSaid=tr.isSayTransition() && tr.willSay(context) && !hasTransitionAlreadyBeenSaid(tr);
		boolean eventsHaveDurations=dm.getConfiguration().getSystemEventsHaveDuration();
		if (sayTransitionToBeSaid && stopCondition!=null && stopCondition==STOP.SYSTEM && !root) {
			logger.info("Stopping execution of action because reached a system line.");
			return;
		}
		if (sayTransitionToBeSaid && eventsHaveDurations) {
			setAsPaused("Action is going to say: "+tr);
			boolean isSpeaking=dm.getSpeakingTracker().isSpeaking();
			if (!isSpeaking) {
				logger.info("system not speaking. Continuing execution of: "+tr);
				setTransitionAsSaying(tr,sourceEvent);
				tr.execute(this,context,sourceEvent);
			} else {
				setTransitionAsWaitingForCurrentSpeach(tr, sourceEvent);
				logger.info("system speaking. Not continuing execution of: "+tr);
			}
		} else if (tr.isWaitTransition() && !hasTransitionAlreadyBeenSaid(tr)) {
			final RewardDM dm = getDM();
			DialogueOperator op=getOperator();
			logger.info("Operator '"+op+"' will pause for: '"+tr.getDelay()+"' seconds.");
			setAsPaused("because a wait transition is being executed.");
			SpeakingTracker st = dm.getSpeakingTracker();
			Timer timer = st.getTimer();
			long ms=Math.round(tr.delay*1000f);
			logger.info("Setting up timer event for wait transition.");
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					logger.warn("sending end of wait timer: '"+tr+"'");
					try {
						setTransitionAsSaid(tr);
						resumeExeFromFinishedSystemAction(tr, sourceEvent);
					} catch (Exception e) {
						dm.getLogger().error(e);
					}
				}
			};
			timer.schedule(task,ms);
			setTransitionAsSaying(tr,sourceEvent);
		} else {
			if (!hasTransitionAlreadyBeenAttempted(tr)) {
				tr.execute(this, context, sourceEvent);
			}
			if (!activeStates.contains(startState)) {
				logger.error("takeTransition called on transition not from an active state.\n" +
						"transition from: "+startState+" to "+endState+"\n"+
						"but active states are: "+activeStates);
			}
			activeStates.transition(startState,endState);
			if (endState!=startState) resetTimerEventsInCurrentState();
			if (endState.isFinal()) {
				// if i reached a final state (not a final operator) then set the DONE time mark.
				TimemarksTracker tt = dm.getTimemarkTracker();
				if (tt!=null) tt.setMark(getOperator().getName(),TimemarksTracker.TYPES.DONE,null);
			}

			rewardAlreadyGot+=endState.execute(this,context,sourceEvent);
			
			if (endState.isFinal() && isFinal()) {
				dm.setDone(true);
				logger.info("   reached final state of action '"+this+"' that is a final action. Put DM in DONE state.");
			}

			if (!endState.isSwapOut()) {
				List<Edge> transitions = endState.getOutgoingEdges();
				if (transitions!=null && !transitions.isEmpty()) {
					DialogueOperatorNodeTransition first = (DialogueOperatorNodeTransition)transitions.get(0);
					if (first.isSayTransition()) {
						List<DialogueOperatorNodeTransition> possibilities=getOperator().pickPossibleSayTransitions(transitions,context);
						
						DialogueOperatorNodeTransition trc=(DialogueOperatorNodeTransition)NLBusBase.pickEarliestUsedOrStillUnused(dm.getSessionID(),possibilities);
						
						takeTransition(trc, context,sourceEvent,stopCondition,false);
					} else if (first.isNoEventTransition() || first.isWaitTransition()) {
						// check and take the first one in order that is executable
						for(Edge e:transitions) {
							DialogueOperatorNodeTransition trc=(DialogueOperatorNodeTransition) e;
							if (trc.isExecutableInCurrentIS(null, context)) {
								takeTransition(trc, context,sourceEvent,stopCondition,false);
								break;
							}
						}
					}
				}
			}
		}
	}

	public boolean handles(Event ev) throws Exception {
		Event result=handlesWhich(ev);
		return (result!=null);
	}
	public Event handlesWhich(Event ev) throws Exception {
		if (ev!=null && !StringUtils.isEmptyString(ev.getName())) {
			NLUOutput sa=(NLUOutput) ev.getPayload();
			if (sa==null) {
				if (handles(ev.getName())) return ev;
				else return null;
			} else if (sa instanceof ChartNLUOutput) {
				List<Triple<Integer, Integer, NLUOutput>> portions = ((ChartNLUOutput)sa).getPortions();
				if (portions!=null) {
					for(Triple<Integer, Integer, NLUOutput> portion:portions) {
						NLUOutput nlu = portion.getThird();
						if (handles(nlu.getId())) return new NLUEvent(nlu, ev.getSessionID());
					}
				}
				return null;
			} else {
				if (handles(sa.getId())) return ev;
				else return null;
			}
		} else return null;
	}
	public boolean handles(String evName) throws Exception {
		if (!StringUtils.isEmptyString(evName)) {
			// first check the active states
			if ((activeStates!=null) && !activeStates.isEmpty()) {
				for(DialogueOperatorNode state:activeStates) {
					List<Edge> transitions=state.getOutgoingEdges();
					if (transitions!=null) {
						for(Edge e:transitions) {
							DialogueOperatorNodeTransition tr=(DialogueOperatorNodeTransition) e;
							if (tr.isEventGoodForTransition(evName)) return true;
						}
					}
				}
			} else {
				DialogueOperator op=entranceCondition.getOperator();
				// if no active states, then check for the entrance conditions
				List<DialogueOperatorEntranceTransition> entranceConditions=op.getUserTriggerableTransitionsForEvent(evName);
				return ((entranceConditions!=null) && !entranceConditions.isEmpty());
			}
		}
		return false;
	}
	/*public boolean handles(String evName,DialogueKBInterface is) throws Exception {
		if (!StringUtils.isEmptyString(evName)) {
			// first check the active states
			if ((activeStates!=null) && !activeStates.isEmpty()) {
				for(DialogueOperatorNode state:activeStates) {
					List<Edge> transitions=state.getOutgoingEdges();
					if (transitions!=null) {
						for(Edge e:transitions) {
							DialogueOperatorNodeTransition tr=(DialogueOperatorNodeTransition) e;
							if (tr.isExecutableInCurrentIS(evName,is)) return true;
						}
					}
				}
			} else {
				DialogueOperator op=entranceCondition.getOperator();
				// if no active states, then check for the entrance conditions
				Set<NetworkTransition> entranceConditions=op.getUserTriggerableTransitionsForEvent(evName);
				if (entranceConditions!=null) {
					for(NetworkTransition tr:entranceConditions) {
						if (tr.isExecutableInCurrentIS(evName,is)) return true;
					}
				}
			}
		}
		return false;
	}*/

	@Override
	public String toString() {
		return toString(true);
	}
	public String toString(boolean shortForm) {
		if (shortForm)
			return "{Action("+state+"): "+entranceCondition.getOperator()+" --"+activeStates+"--}";
		else {
			String ret="<action name=\""+getOperator().getName()+"\">";
			if (activeStates!=null) {
				for(DialogueOperatorNode as:activeStates) {
					ret+="<state id=\""+as.getName()+"\"/>";
				}
			}
			DialogueKBInterface localVars = getLocalVarKB();
			if (localVars!=null) {
				try {
					Collection<DialogueOperatorEffect> effs = localVars.dumpKB();
					if (effs!=null && !effs.isEmpty()) {
						List<DialogueOperatorEffect> effList=new ArrayList<DialogueOperatorEffect>(effs);
						Collections.sort(effList);
						for(DialogueOperatorEffect eff:effList) {
							if (eff.isAssignment()) {
								ret+=eff.toString(false);
							} else throw new Exception("invalid type of effect created by KB dump.");
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}
			ret+="</action>";
			return ret;
		}
	}
	
	private int timerEventsInCurrentState=0; 
	public boolean isWaitingForUser() {
		ActiveStates as = getActiveStates();
		if (as!=null) {
			for(DialogueOperatorNode a:as) {
				return a.isWaitingForUser();
			}
		}
		return false;
	}
	public int incrementTimerEventsInCurrentState() {
		return timerEventsInCurrentState++;
	}
	public int getTimerEventsInCurrentState() {return timerEventsInCurrentState;}
	public void resetTimerEventsInCurrentState() {
		timerEventsInCurrentState=0;
	}
	
	public DialogueOperator getOperator() {
		return entranceCondition.getOperator();
	}

	public class ActiveStates implements Iterable<DialogueOperatorNode> {

		/**
		 * can be null if this active state object is a fake one (i.e. created for simulation).
		 */
		private LinkedHashSet<DialogueOperatorNode> activeStates=null;

		public ActiveStates() {
			this.activeStates=new LinkedHashSet<DialogueOperatorNode>();
		}
		
		public void transition(DialogueOperatorNode remove,DialogueOperatorNode add) {
			sendEnteringEvent(remove, add);
			activeStates.remove(remove);
			activeStates.add(add);
			getDM().updateHistoryOfExecutedOperators(DialogueAction.this);
		}
		
		public boolean getDone() {
			for(DialogueOperatorNode state:activeStates) if(state.isFinal()) return true;
			return false;
		}
		
		public void sendEnteringEvent(DialogueOperatorNode leave,DialogueOperatorNode enter) {
			if (DialogueAction.this!=null) {
				if (enter!=null && enter.getReportStateChange()) {
					RewardDM dm=getDM();
					try {
						String leaveName=(leave!=null)?leave.getName():null;
						String enterName=(enter!=null)?enter.getName():null;
						dm.getMessageBus().handleDMResponseEvent(new DMStateChangeEvent(null, dm.getSessionID(), new StateChange(getOperator().getName(), leaveName, enterName)));
					} catch (Exception e) {
						dm.getLogger().error("while sending the state change event("+leave+"->"+enter+"): "+e);
					}
				}
			}
		}

		@Override
		public Iterator<DialogueOperatorNode> iterator() {
			return activeStates.iterator();
		}

		public boolean isEmpty() {
			return activeStates.isEmpty();
		}

		public boolean contains(DialogueOperatorNode state) {
			return activeStates.contains(state);
		}

		public void clear() {
			activeStates.clear();
		}
	}

}
