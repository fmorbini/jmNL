package edu.usc.ict.nl.dm.reward;

import java.awt.Rectangle;
import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Logger;

import edu.usc.ict.nl.audio.util.Audio;
import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.bus.NLBusInterface;
import edu.usc.ict.nl.bus.events.DMInternalEvent;
import edu.usc.ict.nl.bus.events.DMInterruptionRequest;
import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.Event;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.bus.events.NLUEvent;
import edu.usc.ict.nl.bus.events.SystemUtteranceDoneEvent;
import edu.usc.ict.nl.bus.events.SystemUtteranceLengthEvent;
import edu.usc.ict.nl.bus.events.changes.VarChange;
import edu.usc.ict.nl.bus.events.changes.DMVarChangeEvent;
import edu.usc.ict.nl.bus.events.changes.DMVarChangesEvent;
import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.bus.modules.DMEventsListenerInterface;
import edu.usc.ict.nl.bus.modules.NLG;
import edu.usc.ict.nl.bus.modules.NLGInterface;
import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.dm.reward.model.DialogueAction;
import edu.usc.ict.nl.dm.reward.model.DialogueAction.ActiveStates;
import edu.usc.ict.nl.dm.reward.model.DialogueOperator;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEntranceTransition;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorNode;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorNodesChain;
import edu.usc.ict.nl.dm.reward.model.RewardPolicy;
import edu.usc.ict.nl.dm.reward.model.TimemarksTracker;
import edu.usc.ict.nl.dm.reward.model.RewardPolicy.OpType;
import edu.usc.ict.nl.dm.reward.possibilityGraph.OperatorHistoryNode;
import edu.usc.ict.nl.dm.reward.possibilityGraph.PossibleIS;
import edu.usc.ict.nl.dm.reward.possibilityGraph.PossibleTransition;
import edu.usc.ict.nl.dm.reward.trackers.SystemFinishedSpeakingTracker;
import edu.usc.ict.nl.dm.reward.trackers.ValueTracker;
import edu.usc.ict.nl.dm.visualizer.DMVisualizerI;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.kb.TrivialDialogueKB;
import edu.usc.ict.nl.kb.VariableProperties;
import edu.usc.ict.nl.kb.VariableProperties.PROPERTY;
import edu.usc.ict.nl.nlu.ChartNLUOutput;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.NumberUtils;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.Triple;
import edu.usc.ict.nl.util.graph.Edge;

public class RewardDM extends DM {
	
	// keeps track if the current DM session has reached a final state.
	private boolean done=false;

	private static int MAX_SEARCH_LEVELS;
	private static long MAX_SEARCH_TIME = 300;
	private static final int highSpeedTimerDelay=200;
	private static int MAX_ITERATIONS;


	private DMInternalEvent unhandledUserEvent=null,forcedIgnoreUserEvent=null;

	protected SpeakingTracker speakingTracker;
	protected TimemarksTracker timemarkTracker=null;
	public SpeakingTracker getSpeakingTracker() {return speakingTracker;}
	private List<ValueTracker> trackers=null;
	private SystemFinishedSpeakingTracker systemFinishedSpeakingTracker=null;
	
	private OperatorHistoryNode historyOfExecutedOperators=null;

	private DialogueAction activeAction=null;
	protected DormantActions dormantActions;
	private DormantActions dormantDaemonActions;
	public DormantActions getDormantActions(OpType type) throws Exception {
		switch (type) {
		case NORMAL:
			return dormantActions;
		case DAEMON:
			return dormantDaemonActions;
		default:
			throw new Exception("unsupported type.");
		}
	}
	public boolean isThisActionDormant(DialogueAction a) throws Exception {
		if (a!=null) {
			DormantActions das = getDormantActions((a.getOperator().isDaemon())?OpType.DAEMON:OpType.NORMAL);
			if (das!=null) {
				return das.isThisDormant(a.getOperator());
			}
		}
		return false;
	}
	public void setActiveAction(DialogueAction a) throws Exception {
		// if overwriting the current action, and the current action doesn't become dormant, make it invalid so that it doesn't get executed.
		if (a!=activeAction && activeAction!=null) {
			if (!dormantActions.isThisDormant(activeAction.getOperator())) {
				activeAction.setAsInvalid("was active action but overwritten by "+a+" and not dormant");
			}
		}
		activeAction=a;
		updateActiveAndDormantVariables();
		updateHistoryOfExecutedOperators(getCurrentActiveAction());
	}
	public void updateHistoryOfExecutedOperators(DialogueAction aa) {
		//remove all temporary nodes attached to the current history node.
		if (historyOfExecutedOperators!=null && historyOfExecutedOperators.hasChildren()) {
			historyOfExecutedOperators.clearOutgoingEdges();
		}
		if (aa!=null) {
			DialogueOperator no=aa.getOperator();
			boolean add=false,modified=true;
			if (historyOfExecutedOperators!=null) {
				DialogueOperator last=historyOfExecutedOperators.getOperator();
				add=(last!=no);
				modified=(historyOfExecutedOperators.getDone()!=aa.getDone()); 
			}
			if (modified) {
				try {
					if (add) {
						historyOfExecutedOperators=new OperatorHistoryNode(aa,historyOfExecutedOperators);
					} else {
						historyOfExecutedOperators.update(aa);
					}
					context.setExecutedOperatorsHistory(historyOfExecutedOperators);
					logger.info("Executed: "+getExecutedOperatorsHistory().printChain());
				} catch (Exception e) {logger.error("Error while updating the history of executed operators: ",e);}
			}
		}
	}
	public OperatorHistoryNode getExecutedOperatorsHistory() throws Exception {
		return historyOfExecutedOperators;
	}
	public DialogueAction getCurrentActiveAction() throws Exception {
		DialogueAction currentAction = activeAction;
		if (currentAction!=null && currentAction.getDone()) {
			logger.warn("auto popping of action '"+currentAction+"' while getting current action because it's done.");
			setActiveAction(null);
			currentAction=activeAction;
		}
		return currentAction;
	}
	
	protected EvalContext context;
	protected RewardPolicy dp=null;
	public RewardPolicy getPolicy() {return dp;}
	
	private int eventCounter=0;
	
	private Timer timerEventThread=null,highSpeedTimerThread=null;
	public Timer getTimerThread() {return timerEventThread;}
	
	private boolean isTimerEvent(Event ev) {
		if (timerEventThread!=null) {
			if (ev!=null) {
				return ev.isTimerEvent(this);
			}
			return false;
		} else return false;
	}
	private boolean isExternalUserEvent(Event ev) {
		return ev!=null && ev.isExternalUserEvent(this);
	}
	private boolean isUserEvent(Event ev) {
		return ev!=null && ev.isUserEvent();
	}
	private boolean isLoginEvent(Event ev) {
		return ev!=null && ev.isLoginEvent(this);
	}

	private synchronized void runDaemons(Event ev) throws Exception {
		Collection<DialogueOperator> ops = dp.getOperators(OpType.DAEMON);
		if (ops!=null) {
			if ((dormantDaemonActions!=null) && dormantDaemonActions.getDormantOperators()!=null && !dormantDaemonActions.getDormantOperators().isEmpty()) {
				if (logger.isDebugEnabled()) logger.info("dormant daemon actions '"+((dormantDaemonActions!=null)?dormantDaemonActions.getDormantOperators():null)+"'");
			}
			EvalContext context=getContext();
			List<DialogueOperatorEntranceTransition> ecs = getOperatorsThatCanBeStartedByThisEvent(context, ev, OpType.DAEMON);
			List<DialogueOperatorEntranceTransition> sysecs = getOperatorsThatSupportSystemInitiative(context, dormantDaemonActions, OpType.DAEMON);
			if (sysecs!=null) {
				if (ecs!=null) ecs.addAll(sysecs);
				else ecs=sysecs;
			}

			if (dormantDaemonActions!=null && ecs!=null) {
				Set<DialogueOperator> dops = dormantDaemonActions.getDormantOperators();
				Iterator<DialogueOperatorEntranceTransition> it=ecs.iterator();
				while(it.hasNext()) {
					DialogueOperatorEntranceTransition ec=it.next();
					DialogueOperator op=ec.getOperator();
					if (dops.contains(op) && !ec.isReEntrable()) it.remove();
				}
			}
			if (logger.isDebugEnabled() && ecs!=null) {
				logger.debug("Daemon entrance conditions ecs (only the first for each operator will be executed):");
				for(DialogueOperatorEntranceTransition ec:ecs) {
					logger.debug(" "+ec);
				}
			}
			if (ecs!=null) {
				Set<DialogueOperator> executedOps=new HashSet<DialogueOperator>();
				for(DialogueOperatorEntranceTransition ec:ecs) {
					DialogueOperator op=ec.getOperator();
					assert(op.isDaemon());
					if (executedOps.contains(op)) continue;
					else executedOps.add(op);

					if (ec.isReEntrable()) {
						if (dormantDaemonActions.isThisDormant(op)) {
							if (logger.isDebugEnabled()) logger.info("Restarting daemon operator with re-entrance option: "+ec);
							DialogueAction a=dormantDaemonActions.getDormantActionOf(op);
							a.setAsReentered(ec);
							a.execute(ev, getRootInformationState());
						} else {
							logger.error("Selected reentrance condition for non-dormant daemon operator: "+op.getName());
						}
					} else {
						if (logger.isDebugEnabled()) logger.info("New execution of daemon operator with entrance condition: "+ec);
						DialogueAction a=new DialogueAction(ec, this);
						a.execute(ev, getRootInformationState());
					}
				}
			}
			
		}
	}
	
	private final Semaphore eventLock=new Semaphore(1);
	@Override
	public synchronized List<Event> handleEvent(Event ev) {
		logger.info("=> received event '"+ev+"' ("+(ev!=null?ev.getClass().getCanonicalName():null)+")");
		if (!getPauseEventProcessing()) {
			super.handleEvent(ev);
			try {
				if (ev.isEmptyNLUEvent(this)) {
					logger.info("Empty NLU event received, ignoring.");
				} else {
					if (ev instanceof NLUEvent && getConfiguration().getUserAlwaysInterrupts()) {
						logger.info("/ Interrupting current speaking action (speaking="+getSpeakingTracker().isSpeaking()+") as received user event and global interruption policy is enabled.");
						interruptCurrentlySpeakingAction(ev);
						logger.info("\\ Done interruption. (speaking="+getSpeakingTracker().isSpeaking()+")Continue processsing trigger event '"+ev+"' ("+(ev!=null?ev.getClass().getCanonicalName():null)+")");
					}
					updateInformationStateWithEvent(ev);
					runForwardInferenceInTheseDormantActionsLocalKBs(getDormantActions(OpType.DAEMON));
					runForwardInferenceInTheseDormantActionsLocalKBs(getDormantActions(OpType.NORMAL));
					if (ev instanceof SystemUtteranceDoneEvent) {
						handleDoneEvent((SystemUtteranceDoneEvent)ev);
					} else if (ev instanceof SystemUtteranceLengthEvent) {
						handleLengthEvent((SystemUtteranceLengthEvent)ev);
					} else if (ev instanceof DMSpeakEvent) {
						// update info state covers this case
					} else if (ev instanceof NLGEvent) {
						// update info state covers this case
					} else if (ev instanceof DMInternalEvent || ev instanceof NLUEvent) {
						boolean lockResult=eventLock.tryAcquire();
						if (!lockResult) {
							Exception e=new Exception("recursive call of DM event handler.");;
							logger.error("Recursive call of event handler method.", e);
							throw e;
						}
						// NLU and timer events
						try {
							handleDefaultEvent(ev);
						} catch (Exception e) {
							logger.error(e);
							e.printStackTrace();
						}
						eventLock.release();
					} else {
						logger.error("Unhandled event type: "+ev.getClass().getCanonicalName());
					}
				}
			} catch (Exception e) {
				logger.error(e);
				e.printStackTrace();
			}
	
			if (visualizer!=null) visualizer.updatedKB();
			
			//LinkedBlockingQueue<Event> collectedEvents = getMessageBus().getUnprocessedResponseEvents(getSessionID());
			//if (collectedEvents!=null && !collectedEvents.isEmpty()) return new ArrayList<Event>(collectedEvents);
			//else return null;
		} else {
			logger.info("Event received but processing paused: "+ev);
		}
		return null;
	}
	
	private void runForwardInferenceInTheseDormantActionsLocalKBs(DormantActions dacsContainer) throws Exception {
		if (dacsContainer!=null) {
			Collection<DialogueAction> dacs = dacsContainer.getDormantActions();
			if (dacs!=null && !dacs.isEmpty()) {
				for(DialogueAction a:dacs) {
					DialogueKBInterface lkb = a.getLocalVarKB();
					if (lkb!=null) {
						logger.info("running forward inference in Dormant action local KB ("+lkb+") of operator: "+a.getOperator());
						lkb.runForwardInference(ACCESSTYPE.AUTO_OVERWRITEAUTO);
					}
				}
			}
		}
	}
	
	public void interruptCurrentlySpeakingAction(Event sourceEvent) {
		try {
			NLGEvent ev=getSpeakingTracker().getCurrentlySpeackingEvent();
			if (ev!=null) {
				logger.info("executing interruption of event: "+ev);
				getMessageBus().handleDMResponseEvent(new DMInterruptionRequest(sourceEvent, getSessionID(), ev));
			} else {
				logger.warn("not executing interruption as the system speacking tracker says that there is nothing to be interrupted.");
			}
		} catch (Exception e) {
			logger.error("Error while interrupting current action: ",e);
		}
	}

	synchronized private void handleDoneEvent(SystemUtteranceDoneEvent ev) throws Exception {
		logger.info("====>received done event: "+ev);
		getSpeakingTracker().finishedSpeakingThis(ev);
	}
	synchronized private void handleLengthEvent(SystemUtteranceLengthEvent ev) throws Exception {
		String event=ev.getName();
		Float duration=ev.getPayload();
		logger.info("====>received length event: "+ev+" = "+duration);
		if (duration==null) {
			try {
				DMEventsListenerInterface mb = getMessageBus();
				NLGInterface nlg = mb.getNlg(ev.getSessionID());
				String sa=nlg.getAudioFileName4SA(ev.getName());
				if (sa!=null) {
					duration=Audio.getWavLength(new File(sa));
				}
			} catch (Exception e) {
				logger.warn("Error getting duration for SystemUtteranceLengthEvent.",e);
			}
		}
		if (duration!=null) getSpeakingTracker().gotLengthForThisEvent(event,duration);
	}
	private List<DialogueAction> getPausedActions() throws Exception {
		DormantActions das = getDormantActions(OpType.NORMAL);
		DialogueAction currentAction = getCurrentActiveAction();
		List<DialogueAction> ret=null;
		if (das!=null) {
			for(DialogueAction a:das.getDormantActions()) {
				if (a.isPaused()) {
					if (ret==null) ret=new ArrayList<DialogueAction>();
					ret.add(a);
				}
			}
		}
		if (currentAction!=null && currentAction.isPaused()) {
			if (ret==null) ret=new ArrayList<DialogueAction>();
			ret.add(currentAction);
		}
		return ret;
	}
	private void handleDefaultEvent(Event ev) throws Exception {
		NLUOutput sa=(NLUOutput) ev.getPayload();
		DialogueAction currentAction=null;
		logger.info("=================> "+(eventCounter++)+" received event '"+ev+"' with payload: "+((sa!=null)?sa.getPayload():null));

		cleanupDormantActions();
		updateActiveAndDormantVariables();
		runDaemons(ev);
		
		// contains the input event. If no operator handled that event it stays like the input event.
		// Otherwise it's the exact event handled (in case the input is a multi nlu or chart nlu output).
		Event userInputEvent=ev; 
		
		int iterations=0;
		while (!isSessionDone() && iterations<MAX_ITERATIONS) {
			iterations++;
			cleanupDormantActions();
			updateActiveAndDormantVariables();
			currentAction=getCurrentActiveAction();

			logger.info(" current action '"+currentAction+"'");
			logger.info(" dormant actions '"+((dormantActions!=null)?dormantActions.getDormantOperators():null)+"'");
			runForwardInferenceInTheseDormantActionsLocalKBs(getDormantActions(OpType.DAEMON));
			runForwardInferenceInTheseDormantActionsLocalKBs(getDormantActions(OpType.NORMAL));
			
			if (currentAction!=null && currentAction.isPaused() && !ev.isUserEvent() && getSpeakingTracker().isSpeaking()) {
				logger.info("Input event is not a user event, current action is paused and speaking => delay running search for new action.");
				break;
			} else {
				// select action and pick exact event handled (the nlu event can contain multiple simple events, select one among them).
				Event handledEvent=runForwardSearchForEventAndActivateResult(ev);
				
				getInformationState().store(DialogueOperatorEffect.createAssignment(NLBusBase.lastEventVariableName, DialogueKBFormula.create("'"+handledEvent+"'", null)),ACCESSTYPE.AUTO_OVERWRITEAUTO,true);
				// update the event variable with the handled event (null if input event is ignored)
				if (handledEvent!=null) userInputEvent=handledEvent;
				// this line is here to store the variable attached to the default event to remember which user event generated the default event.
				if (iterations==1) updateISwithNLUvariablesFromEvent(getRootInformationState(),userInputEvent); 
	
				cleanupDormantActions();
				updateActiveAndDormantVariables();
				currentAction=getCurrentActiveAction();
				if (currentAction!=null) {
					assert(!dormantActions.isThisDormant(currentAction.getOperator()));
					if (handledEvent!=null) logger.info("  action '"+currentAction+"' will consume the event '"+handledEvent+"'");
					else logger.info("  no events, continuing action: "+currentAction.getOperator());
					currentAction.execute(handledEvent, getRootInformationState());
					if (currentAction.getDone()) {
						logger.info("   action '"+currentAction+"' reached end. Popping.");
						setActiveAction(null);
					} else if (isThisActionDormant(currentAction)) {
						logger.info("   action '"+currentAction+"' is dormant.");
					} else break;
				} else {
					break;
				}
				ev=null;
			}
		}
		if (iterations>=MAX_ITERATIONS) logger.warn("possible loop in event handler. Forced exit because reached "+iterations+" iteration count for one input event.");
		addStateToStateTracker(userInputEvent,getCurrentActiveAction());
		int loops=isThereADialogloop();
		if (loops>1) {
			logger.warn("POSSIBLE DIALOG LOOP: "+loops);
			logger.warn("\n"+stateTracker);
			if (!StringUtils.isEmptyString(loopEvent)) {
				logger.info("sending loop event: "+loopEvent);
				TimerTask task = new TimerTask() {
					@Override
					public void run() {
						handleEvent(new DMInternalEvent(loopEvent, getSessionID()));
					}
				};
				getTimerThread().schedule(task, 1);
			} else {
				logger.warn("loop event not configured, doing nothing.");
			}
		}
	}
	
	private Event runForwardSearchForEventAndActivateResult(Event ev) throws Exception {
		return sendEventToCurrentActionOrToSearchOrUserProvidedSearchResult(ev, null);
	}
	private Event sendEventToCurrentActionOrToSearchOrUserProvidedSearchResult(Event ev,FoundDialogueOperatorEntranceTransition op_mode) throws Exception {
		DialogueAction currentAction=getCurrentActiveAction();
		Event handledEvent=null; // contains the event that the selected action handles.
		if ((currentAction==null) || ((ev!=null) && !currentAction.handles(ev))) {
			logger.info("  current action '"+currentAction+"' cannot handle event '"+ev+"'. Searching for other action.");

			op_mode=(op_mode!=null)?op_mode:selectAndPushBestOperatorForGivenEvent(ev,currentAction);
			if (op_mode!=null) {
				DialogueOperatorEntranceTransition ec=op_mode.getEntranceCondition();
				SearchTermination mode=op_mode.getSearchTermination();
				if (ec==null) throw new Exception("non NULL search ending mode but null entrance condition.");
				if (mode!=null && isIgnoreCurrentEvent(mode)) ev=null;
				Event hev=null;
				if (ev!=null && ec!=null && (hev=ec.handlesWhich(ev))!=null) {
					handledEvent=hev;
					logger.info("  selected entrance condition handles event '"+handledEvent+"'.");
				}
			}
		} else if (currentAction!=null && ev!=null) {
			handledEvent=currentAction.handlesWhich(ev);
			logger.info("  the current action handles event '"+handledEvent+"'.");
		}

		return handledEvent;
	}
	
	private int isThereADialogloop() {
		if (stateTracker!=null) {
			return stateTracker.containsLoop();
		}
		return -1;
	}
	private void addStateToStateTracker(Event userInput,DialogueAction currentActiveAction) throws Exception {
		if (stateTracker!=null) {
			String id=null;
			if (currentActiveAction!=null) {
				ActiveStates states = currentActiveAction.getActiveStates();
				if (states!=null && !states.isEmpty()) {
					for(DialogueOperatorNode n:states) {
						id=((id==null)?"":id)+n.getName();
					}
				}
			}
			if (!isTimerEvent(userInput) || !stateTracker.isSameLastState(id)) {
				stateTracker.addState(userInput.getName(),id);
			}
		}
	}
	public void runISautoUpdatesWithSpeechAct(Event ev, DialogueKB is) throws Exception {
		if (ev!=null) {
			String evName=ev.getName();
			if (!StringUtils.isEmptyString(evName)) {
				logger.info("starting autoupdate of information state: "+evName);
				if (dp.getISUpdatesMatcher()!=null) {
					Set<List<DialogueOperatorEffect>> effs = dp.getISUpdatesMatcher().match(evName);
					if(logger.isDebugEnabled()) logger.debug(" firing these event driven updates: "+effs);
					if (effs!=null) for(List<DialogueOperatorEffect> eff:effs) {
						if (eff!=null) {
							Collection<VarChange> changes = is.saveAssignmentsAndGetUpdates(ACCESSTYPE.AUTO_OVERWRITEAUTO, true, eff);
							sendVarChangeEventsCausedby(changes, ev);
						}
					}
				}
				logger.info("done autoupdate of information state.");
			}
		}
	}
	public void sendVarChangeEventsCausedby(Collection<VarChange> changes, Event sourceEvent) throws Exception {
		NLBusInterface messageBus = getMessageBus();
		if (changes!=null && !changes.isEmpty() &&  messageBus!=null) {
			long sid=getSessionID();
			if (sourceEvent==DMInternalEvent.INIT) {
				messageBus.handleDMResponseEvent(new DMVarChangesEvent(sourceEvent, sid, changes));
			} else {
				for(VarChange c:changes) {
					messageBus.handleDMResponseEvent(new DMVarChangeEvent(sourceEvent,sid,c));
				}
			}
		} else if (messageBus==null) {
			logger.warn("Failed to send messages as the message bus is NULL.");
		}
	}
	synchronized public void updateInformationStateWithEvent(final Event ev) throws Exception {
		DialogueKB localIS = getInformationState();
		DialogueKB baseIS=getRootInformationState();
		if (ev!=null) {
			logger.info("updating IS("+localIS.getName()+") with event of class "+ev.getClass());
			if (trackers!=null) for(ValueTracker vt:trackers) vt.updateIS();
			if (ev instanceof DMSpeakEvent)	{
				updateUserEventsHistory(ev);
			} else if (ev instanceof NLGEvent) {
				updateSystemSayTracker((NLGEvent) ev);
				localIS.store(DialogueOperatorEffect.createAssignment(NLBusBase.counterConsecutiveUnhandledUserActionsSinceLastSystemActionVariableName,DialogueKBFormula.create("0", null)),ACCESSTYPE.AUTO_OVERWRITEAUTO,true);
				Float value=null;
				try{
					value=Float.parseFloat(localIS.getValueOfVariable(NLBusBase.timeSinceStartVariableName, ACCESSTYPE.AUTO_OVERWRITEAUTO,null).toString());
				} catch (Exception e) {};
				if (value==null || value<0) localIS.store(DialogueOperatorEffect.createAssignment(NLBusBase.timeSinceStartVariableName,DialogueKBFormula.create("0", null)),ACCESSTYPE.AUTO_OVERWRITEAUTO,true);
			} else if (ev instanceof NLUEvent) {
				updateISwithNLUvariablesFromEvent(baseIS,ev);
				updateUserEventsHistory(ev);
				NLUOutput sa=(NLUOutput) ev.getPayload();
				if (sa==null) {
					runISautoUpdatesWithSpeechAct(ev, localIS);
				} else {
					if (sa instanceof ChartNLUOutput) {
						ChartNLUOutput nlu=(ChartNLUOutput)sa;
						List<Triple<Integer, Integer, NLUOutput>> portions = nlu.getPortions();
						if (portions!=null) {
							for(Triple<Integer, Integer, NLUOutput> portion:portions) {
								runISautoUpdatesWithSpeechAct(new NLUEvent(portion.getThird(), ev.getSessionID()), localIS);
							}
						}
					} else {
						runISautoUpdatesWithSpeechAct(ev, localIS);
					}
				}

				if (isLoginEvent(ev)) localIS.store(DialogueOperatorEffect.createAssignment(NLBusBase.timeSinceStartVariableName,DialogueKBFormula.create("0", null)),ACCESSTYPE.AUTO_OVERWRITEAUTO,true);
				if (ev.isExternalUserEvent(this)) localIS.setValueOfVariable(NLBusBase.lastEventVariableName, DialogueKBFormula.create("'"+ev.getName()+"'", null),ACCESSTYPE.AUTO_OVERWRITEAUTO);
				if (ev.isExternalUserEvent(this)) localIS.setValueOfVariable(NLBusBase.hasUserSaidSomethingVariableName,DialogueKBFormula.create("'"+ev.getName()+"'", null),ACCESSTYPE.AUTO_OVERWRITEAUTO);

				localIS.store(DialogueOperatorEffect.createAssignment(NLBusBase.timeSinceLastUserActionVariableName,DialogueKBFormula.create("0", null)),ACCESSTYPE.AUTO_OVERWRITEAUTO,true);
				getMessageBus().setSpeakingStateVarForSessionAs(getSessionID(), false);

				Object thing=localIS.getValueOfVariable(NLBusBase.counterConsecutiveUnhandledUserActionsVariableName,ACCESSTYPE.AUTO_OVERWRITEAUTO,null);
				previousUnhandledCounter=DialogueKBFormula.create((thing!=null)?thing.toString():"null", null);
				localIS.store(DialogueOperatorEffect.createAssignment(NLBusBase.counterConsecutiveUnhandledUserActionsVariableName,DialogueKBFormula.create("0", null)),ACCESSTYPE.AUTO_OVERWRITEAUTO,true);

				Float uv=Float.parseFloat(localIS.getValueOfVariable(NLBusBase.timeSinceLastUserActionVariableName,ACCESSTYPE.AUTO_OVERWRITEAUTO,null).toString());
				Float sv=Float.parseFloat(localIS.getValueOfVariable(NLBusBase.timeSinceLastSystemActionVariableName,ACCESSTYPE.AUTO_OVERWRITEAUTO,null).toString());
				localIS.store(DialogueOperatorEffect.createAssignment(NLBusBase.timeSinceLastActionVariableName,Math.min(uv,sv)),ACCESSTYPE.AUTO_OVERWRITEAUTO,true);

			} else if (isTimerEvent(ev)) {
				DialogueAction currentAction=getCurrentActiveAction();
				if (currentAction!=null) currentAction.incrementTimerEventsInCurrentState();
				
				localIS.store(incrementTimeSinceStart, ACCESSTYPE.AUTO_OVERWRITEAUTO, true);
				localIS.store(incrementTimeSinceLastResource, ACCESSTYPE.AUTO_OVERWRITEAUTO, true);

				Float uv=Float.parseFloat(localIS.getValueOfVariable(NLBusBase.timeSinceLastUserActionVariableName,ACCESSTYPE.AUTO_OVERWRITEAUTO,null).toString());
				Float sv=Float.parseFloat(localIS.getValueOfVariable(NLBusBase.timeSinceLastSystemActionVariableName,ACCESSTYPE.AUTO_OVERWRITEAUTO,null).toString());
				localIS.store(DialogueOperatorEffect.createAssignment(NLBusBase.timeSinceLastActionVariableName,Math.min(uv,sv)),ACCESSTYPE.AUTO_OVERWRITEAUTO,true);
			} else if (ev instanceof SystemUtteranceDoneEvent) {
			} else {
				logger.error("Unhandled input event in :"+ev+"("+((ev!=null)?ev.getClass().getCanonicalName():"")+") in "+this.getClass().getCanonicalName());
			}
			logger.info("Done updating IS with event.");
		}
	}
	protected void updateUserEventsHistory(Event ev) throws Exception {
		if (ev!=null) {
			DialogueKB is=getInformationState();
			Deque<Deque<NLUOutput>> userEventsHistoryContent=(Deque)is.getValueOfVariable(NLBusBase.userEventsHistory,ACCESSTYPE.AUTO_OVERWRITEAUTO,null);
			if (userEventsHistoryContent==null) userEventsHistoryContent=new LinkedList<Deque<NLUOutput>>();
			if (userEventsHistoryContent.isEmpty()) userEventsHistoryContent.push(new LinkedList<NLUOutput>());
			Deque<NLUOutput> lastItem=userEventsHistoryContent.peekFirst();
			if ((ev instanceof DMSpeakEvent || ev instanceof SystemUtteranceDoneEvent) && lastItem!=null && !lastItem.isEmpty()) {
				userEventsHistoryContent.push(new LinkedList<NLUOutput>());
			} else if (ev instanceof NLUEvent && ev.isExternalUserEvent(this)) {
				Float spokenFraction=getSpeakingTracker().getSpokenFraction();
				Deque<NLUOutput> thisIsTheRealLast=null;
				if (spokenFraction!=null && spokenFraction<.7) {
					thisIsTheRealLast=userEventsHistoryContent.pop();
					lastItem=userEventsHistoryContent.peekFirst();
					if (lastItem==null) userEventsHistoryContent.push(lastItem=new LinkedList<NLUOutput>());
				}
				
				NLUOutput sa=(NLUOutput) ev.getPayload();
				if (sa!=null) {
					is.store(DialogueOperatorEffect.createAssignment(NLBusBase.lastUserText,sa.getText()),ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
					lastItem.push(sa);
				}
				
				if (thisIsTheRealLast!=null) {
					userEventsHistoryContent.push(thisIsTheRealLast);
				}
			}
			is.store(DialogueOperatorEffect.createAssignment(NLBusBase.userEventsHistory,userEventsHistoryContent),ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		}
		
	}

	/*private Event pickOutSingleMainEventIfMultipleEvents(Event ev,NLUOutput sa, DialogueAction currentAction, DialogueKBInterface is) throws Exception {
		if (sa instanceof ChartNLUOutput) {
			NLConfig config=getConfiguration();
			ChartNLUOutput nlu=(ChartNLUOutput)sa;
			List<Triple<Integer, Integer, NLUOutput>> portions = nlu.getPortions();
			if (portions!=null) {
				if (portions.size()==1) {
					Triple<Integer, Integer, NLUOutput> pc=portions.get(0);
					NLUOutput pcNLU=pc.getThird();
					ev.setName(pcNLU.getId());
					ev.setPayload(pcNLU);
					runISautoUpdates(ev,informationState);
				} else {
					// filter out events that are unhandled (do stateless updates if available)
					// if the current action handles some events, filter out the rest, except if there are some questions.
					// if there are questions, prefer them over the handled ones.
					// among the remaining events, pick randomly
					List<NLUOutput> handledEvents=new ArrayList<NLUOutput>(),expectedEvents=new ArrayList<NLUOutput>(),questionEvents=new ArrayList<NLUOutput>();
					NLUOutput hasLowConfidenceEvent=null;
					for(Triple<Integer, Integer, NLUOutput> pc:portions) {
						NLUOutput pcNLU=pc.getThird();
						String eventName=pcNLU.getId();
						runISautoUpdates(eventName,is);
						if (!eventName.equals(config.getLowConfidenceEvent())) {
							List<DialogueOperatorEntranceTransition> handlers = getOperatorsThatSupportThisEvent(is, eventName);
							
							if (currentAction!=null && currentAction.handles(eventName)) {
								expectedEvents.add(pcNLU);
							}
							if ((handlers!=null) && !handlers.isEmpty()) {
								handledEvents.add(pcNLU);
							}
							if (NLU.isQuestion(eventName)) {
								questionEvents.add(pcNLU);
							}
						} else {
							hasLowConfidenceEvent=pcNLU;
						}
					}
					if (logger.isInfoEnabled()) {
						logger.info("                    handled events: "+FunctionalLibrary.printCollection(handledEvents, "                    \n", "", "                    \n"));
						logger.info("                    expected events: "+FunctionalLibrary.printCollection(expectedEvents, "                    \n", "", "                    \n"));
						logger.info("                    question events: "+FunctionalLibrary.printCollection(questionEvents, "                    \n", "", "                    \n"));
					}
					NLUOutput pickedEvent=null;
					if (!questionEvents.isEmpty()) {
						pickedEvent=(NLUOutput) FunctionalLibrary.pickRandomElement(questionEvents);
					} else if (!expectedEvents.isEmpty()) {
						pickedEvent=(NLUOutput) FunctionalLibrary.pickRandomElement(expectedEvents);
					} else if (!handledEvents.isEmpty()) {
						pickedEvent=(NLUOutput) FunctionalLibrary.pickRandomElement(handledEvents);
					}
					if (pickedEvent!=null) {
						ev.setName(pickedEvent.getId());
						ev.setPayload(pickedEvent);
					} else {
						Triple<Integer, Integer, NLUOutput> pickedPortion=(Triple<Integer, Integer, NLUOutput>) FunctionalLibrary.pickRandomElement(portions);
						NLUOutput pcNLU=pickedPortion.getThird();
						ev.setName(pcNLU.getId());
						ev.setPayload(pcNLU);
					}
				}
			}
		} else {
			dp.runISautoUpdates(ev.getName(),informationState);
		}
		return ev;
	}*/
	public static DialogueKBFormula lastNonNullOperatorVariableFormula=null;
	static {
		try {
			lastNonNullOperatorVariableFormula=DialogueKBFormula.create(NLBusBase.lastNonNullOperatorVariableName, null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public static DialogueOperatorEffect updateLastNonNullOperatorVariableInWith(DialogueKBInterface is,DialogueOperator op) throws Exception {
		if (is!=null && op!=null) {
			Object value = is.evaluate(lastNonNullOperatorVariableFormula,null);
			boolean needUpdating=false;
			String currentOpName=op.getName();
			if (value!=null && value instanceof String) {
				String prevOpName=(String)value;
				needUpdating=!prevOpName.equals(currentOpName);
			} else needUpdating=true;
			if (needUpdating) return DialogueOperatorEffect.createAssignment(lastNonNullOperatorVariableFormula, "'"+currentOpName+"'");
			else return null;
		} else return null;
	}
	private void updateActiveAndDormantVariables() throws Exception {
		// update the variables that keep track of the current active action and the set of
		// dormant actions
		DialogueKB informationState=getInformationState();
		if (activeAction!=null) {
			informationState.setValueOfVariable(NLBusBase.activeActionVariableName, DialogueKBFormula.create("'"+activeAction.toString(false)+"'", null),ACCESSTYPE.AUTO_OVERWRITEAUTO);
			DialogueOperatorEffect eff = updateLastNonNullOperatorVariableInWith(informationState,activeAction.getOperator());
			if (eff!=null) informationState.store(eff, ACCESSTYPE.AUTO_OVERWRITEAUTO, true);
		} else
			informationState.setValueOfVariable(NLBusBase.activeActionVariableName, DialogueKBFormula.nullFormula,ACCESSTYPE.AUTO_OVERWRITEAUTO);
		String allDormantPrint="";
		if (dormantActions!=null && !dormantActions.isEmpty()) {
			for (DialogueAction a:dormantActions.getDormantActions()) {
				allDormantPrint+=a.toString(false);
			}
		}
		if (!StringUtils.isEmptyString(allDormantPrint))
			informationState.setValueOfVariable(NLBusBase.dormantActionsVariableName, DialogueKBFormula.create("'"+allDormantPrint+"'", null),ACCESSTYPE.AUTO_OVERWRITEAUTO);
		else
			informationState.setValueOfVariable(NLBusBase.dormantActionsVariableName, DialogueKBFormula.nullFormula,ACCESSTYPE.AUTO_OVERWRITEAUTO);
	}
	private void cleanupDormantActions() {
		cleanupTheseDormantActions(dormantActions);
		cleanupTheseDormantActions(dormantDaemonActions);
	}
	private void cleanupTheseDormantActions(DormantActions sacts) {
		ArrayList<DialogueOperator> toBeRemoved=null;
		for(DialogueAction a:sacts.getDormantActions()) {
			DialogueOperator op=a.getOperator();
			if(!op.isReEntrable()) {
				logger.info("Removing dormant action: "+op.getName()+" because has no re-entrance options.");
				if (toBeRemoved==null) toBeRemoved=new ArrayList<DialogueOperator>();
				toBeRemoved.add(op);
			} else if (sacts.shouldThisActionBeForgotten(a)) {
				logger.info("Removing dormant action: "+op.getName()+" because has reached forget time.");
				if (toBeRemoved==null) toBeRemoved=new ArrayList<DialogueOperator>();
				toBeRemoved.add(op);
			} else if (a.isInInvalidState()) {
				logger.info("Removing dormant action: "+op.getName()+" because its state is INVALID.");
				if (toBeRemoved==null) toBeRemoved=new ArrayList<DialogueOperator>();
				toBeRemoved.add(op);
			} else if (a.getDone()) {
				a.setAsInvalid("because the action was dormant but reached a final state");
				if (toBeRemoved==null) toBeRemoved=new ArrayList<DialogueOperator>();
				toBeRemoved.add(op);
			}
		}
		if (toBeRemoved!=null) for(DialogueOperator op:toBeRemoved) sacts.removeDormantOperator(op,false);
	}

	@Override
	public boolean isWaitingForUser() {
		DialogueAction currentAction=null;
		try {
			currentAction = getCurrentActiveAction();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return (currentAction!=null)?currentAction.isWaitingForUser():false;
	}

	/**
	 * 
	 * @param ev the event to handle
	 * @param currentAction the current active action (if non null, the current action is preferred)
	 * @return null means no change, otherwise an entrance condition for the handler of the given event with best immediate reward.
	 * @throws Exception
	 */
	private FoundDialogueOperatorEntranceTransition getBestImmediateHandlerForEvent(Event ev, DialogueAction currentAction) throws Exception {
		if (ev!=null) {
			EvalContext context=getContext();
			Event hev=null;
			if (currentAction!=null && (hev=currentAction.handlesWhich(ev))!=null) {
				DialogueOperatorEntranceTransition fake=DialogueOperatorEntranceTransition.createFakeEntraceConditionForEventAndOperator(hev.getName(),currentAction.getOperator());
				logger.info("Creating fake entrance condition for current action(="+currentAction+") for event: "+hev.getName());
				return new FoundDialogueOperatorEntranceTransition(new WeightedDialogueOperatorEntranceTransition(fake,0f),
						SearchTermination.KEEP_CURRENT_ACTION_HANDLE_EVENT);
			} else {
				List<DialogueOperatorEntranceTransition> ecs = getOperatorsThatCanBeStartedByThisEvent(context, ev.getName(),OpType.NORMAL);
				if (ecs!=null) {
					DialogueOperatorEntranceTransition bestEC=null;
					float bestReward=0;
					for(DialogueOperatorEntranceTransition ec:ecs) {
						DialogueOperator op=ec.getOperator();
						float rewardForHandler=getRemainingRewardForSetOfEffects(
								op.getEffectsSetsForStartingState(ec.getTarget()),
								op, getRootInformationState());
						if (rewardForHandler>bestReward) bestEC=ec;
					}
					return new FoundDialogueOperatorEntranceTransition(new WeightedDialogueOperatorEntranceTransition(bestEC,bestReward), SearchTermination.DROP_CURRENT_ACTION_HANDLE_EVENT);
				}
			}
		}
		return null;
	}
	public static enum SearchTermination {KEEP_CURRENT_ACTION_IGNORE_EVENT,
		KEEP_CURRENT_ACTION_HANDLE_EVENT,
		DROP_CURRENT_ACTION_IGNORE_EVENT,
		DROP_CURRENT_ACTION_HANDLE_EVENT};
		public static final Set<SearchTermination> ALL_TERMINATION_METHODS=new HashSet<RewardDM.SearchTermination>();
	static {
		ALL_TERMINATION_METHODS.add(SearchTermination.KEEP_CURRENT_ACTION_IGNORE_EVENT);
		ALL_TERMINATION_METHODS.add(SearchTermination.KEEP_CURRENT_ACTION_HANDLE_EVENT);
		ALL_TERMINATION_METHODS.add(SearchTermination.DROP_CURRENT_ACTION_IGNORE_EVENT);
		ALL_TERMINATION_METHODS.add(SearchTermination.DROP_CURRENT_ACTION_HANDLE_EVENT);
	}
	public static final Set<SearchTermination> HANDLER_TERMINATION_METHODS=new HashSet<RewardDM.SearchTermination>();
	
	static {
		HANDLER_TERMINATION_METHODS.add(SearchTermination.KEEP_CURRENT_ACTION_HANDLE_EVENT);
		HANDLER_TERMINATION_METHODS.add(SearchTermination.DROP_CURRENT_ACTION_HANDLE_EVENT);
	}
	
	private boolean isChangeDrivenByNonUserEvent(FoundDialogueOperatorEntranceTransition ec,Event ev, DialogueAction currentAction) {
		return (currentAction!=null) &&
			!isUserEvent(ev) && 
			(ec!=null) && (ec.getEntranceCondition()!=null) &&
			(ec.getEntranceCondition().getOperator()!=currentAction.getOperator());
	}
	
	// this method returns the best operator given the current state and event
	// it may chose to ignore the given event and pick some other operator
	private FoundDialogueOperatorEntranceTransition selectAndPushBestOperatorForGivenEvent(Event ev, DialogueAction currentAction) throws Exception {
		assert(activeAction==currentAction);
		logger.info("   Searching for best operator for event: "+ev);
		NextActionSelector br=findBestEntranceCondition4Operator2(ev,currentAction);
		FoundDialogueOperatorEntranceTransition ec = br.getBest();
		logger.info("   FOUND: "+ec);

		// if change happens too soon while waiting for user input, cancel the change.
		if (isChangeDrivenByNonUserEvent(ec, ev, currentAction) && currentAction.isWaitingForUser()) {
			if (isTimerEvent(ev)) {
				NLBusConfig config=getConfiguration();
				float interval=config.getTimerInterval();
				float time=currentAction.getTimerEventsInCurrentState()*interval;
				int th=config.getWaitForUserReplyTimeout();
				if (time<th) {
					logger.info("   OVERWRITE CHANGE DECISION because waiting for user reply and only: "+time+" of "+th+" seconds elapsed.");
					return null; 
				}
			}
		}
		
		// check if we should prefer to the selected entrance condition, handlers for the ignore or unhandled events (if there).
		ec = checkIgnoredAndUnhandledHandlers(ev, currentAction, br);
		
		if (ec!=null && ec.getEntranceCondition().getOperator().canItBeExecutedNow(getInformationState())) {
			// ec here contains the selected entrance condition. Now it's time to enable it.
			updateCurrentActionGivenSelection(ec,currentAction);
		} else if (ec!=null) {
			logger.info("Setting search result to NULL as operator's turn taking condition evaluated to false.");
			ec=null;
		}

		return ec;
	}
	
	private FoundDialogueOperatorEntranceTransition checkIgnoredAndUnhandledHandlers(
			Event ev, DialogueAction currentAction, NextActionSelector br) throws Exception {
		// if current action unchanged and current action not handling current event,
		// then send the unhandled event.
		FoundDialogueOperatorEntranceTransition ec = br.getBest();
		
		if (isUserEvent(ev) && ec!=null && !ec.getEntranceCondition().isEventGoodForTransition(ev.getName())) {
			logger.info("selected action doesn't handle input user event. Looking for ignore/unhandled event.");
			//selectedEntranceCondition==null && isUserEvent(ev)) {
			// if there was an handler but it was ignored, look for best handler for forcedIgnore event instead of unhandled event
			boolean habi=br.handlerOptionAvailableButIgnored();
			if (habi) {
				logger.info("Available handler for input user event. Checking if ignored event configured.");
				if (forcedIgnoreUserEvent==null) logger.warn(" ignored user event even if handler available but forcedIgnored event NOT configured.");
			}
			Event ignoredUserEvent=(habi)?forcedIgnoreUserEvent:unhandledUserEvent;
			if (ignoredUserEvent==null) ignoredUserEvent=unhandledUserEvent; // fall back to unhandled event
			if (ignoredUserEvent!=null) {
				if (ignoredUserEvent==unhandledUserEvent && (getSpeakingTracker().isSpeaking() || getSpeakingTracker().getInterruptionSourceEvent()==ev) && getConfiguration().getSkipUnhandledWhileSystemSpeaking()) {
					logger.info(" ignored user event, skipping looking for an immediate handler for '"+ignoredUserEvent+"' as system configured to skip unhandled event while speaking.");
				} else {
					logger.info(" ignored user event, checking best immediate handler for '"+ignoredUserEvent+"' event.");
					incrementUnhandledEventTracker();
					FoundDialogueOperatorEntranceTransition unhandledEC=getBestImmediateHandlerForEvent(ignoredUserEvent,currentAction);
					if (unhandledEC!=null) {
						//if (ignoredUserEvent==forcedIgnoreUserEvent) System.exit(1);
						ec=unhandledEC;
						logger.info("   FOUND: "+ec);
						String originatingEventName=ev.getName();
						ev.setName(ignoredUserEvent.getName());
						if (ev instanceof NLUEvent) {
							NLUEvent nluEv=(NLUEvent) ev;
							nluEv.setPayload(new NLUOutput("", ignoredUserEvent.getName(), 1, null));
							nluEv.addVariableToPayload(NLBusBase.tmpEventVariableName,DialogueKBFormula.create("'"+originatingEventName+"'", null));
						}
					}
				}
			} else {
				logger.warn(" ignored user event but no configured special events. No special handling.");
			}
		}
		return ec;
	}
	
	private void updateCurrentActionGivenSelection(FoundDialogueOperatorEntranceTransition ec, DialogueAction currentAction) throws Exception {
		if (ec!=null) {
			DialogueOperatorEntranceTransition selectedEntranceCondition=ec.getEntranceCondition();
			// something was selected
			if (selectedEntranceCondition.isReEntrable() && dormantActions.isThisDormant(selectedEntranceCondition.getOperator())) {
				// a re-entrance option was selected
				dormantActions.wakeUpThisOperator(selectedEntranceCondition,currentAction,this);
			} else {
				// a normal entrance condition was selected
				if ((currentAction!=null) && (currentAction.getOperator()!=selectedEntranceCondition.getOperator())) {
					// something is current executing, swap it out for the newly selected one
					dormantActions.addAction(currentAction,SwapoutReason.SEARCH);
					setActiveAction(new DialogueAction(selectedEntranceCondition,this));
				} else {
					assert(currentAction==null || currentAction.getOperator()==selectedEntranceCondition.getOperator());
					// nothing currently executing or need to restart the same action currently executing
					// (e.g. because of specific user speech act) 
					SearchTermination mode=ec.getSearchTermination();
					if ((currentAction==null) || (isDropMode(mode) && selectedEntranceCondition.isCurrentUserInitiatable())) {
						setActiveAction(new DialogueAction(selectedEntranceCondition,this));
					}
				}
			}
		}
	}
	
	public boolean isIgnoreCurrentEvent(SearchTermination mode) {
		return mode==SearchTermination.DROP_CURRENT_ACTION_IGNORE_EVENT || mode==SearchTermination.KEEP_CURRENT_ACTION_IGNORE_EVENT;
	}
	public boolean isDropMode(SearchTermination mode) {
		return (mode==SearchTermination.DROP_CURRENT_ACTION_HANDLE_EVENT) || (mode==SearchTermination.DROP_CURRENT_ACTION_IGNORE_EVENT);
	}
	public boolean isHandleMode(SearchTermination mode) {
		return (mode==SearchTermination.DROP_CURRENT_ACTION_HANDLE_EVENT) || (mode==SearchTermination.KEEP_CURRENT_ACTION_HANDLE_EVENT);
	}
	
	private Map<String,Object> nluVariables=null;

	private DialogueKBFormula previousUnhandledCounter;

	private StateTracker stateTracker;
	private DMVisualizerI visualizer=null;

	private static Rectangle visualizerBounds=null;
	
	private static DialogueOperatorEffect incrementUnhandledCounterVariable,incrementUnhandledInTurnCounterVariable,incrementTimeSinceLastResource,incrementTimeSinceStart;
	static {
		try {
			incrementUnhandledCounterVariable=DialogueOperatorEffect.createIncrementForVariable(NLBusBase.counterConsecutiveUnhandledUserActionsVariableName,1);
			incrementUnhandledInTurnCounterVariable=DialogueOperatorEffect.createIncrementForVariable(NLBusBase.counterConsecutiveUnhandledUserActionsSinceLastSystemActionVariableName,1);
			incrementTimeSinceLastResource=DialogueOperatorEffect.createIncrementForVariable(NLBusBase.timeSinceLastResourceVariableName,
					DialogueKBFormula.create(NLBusBase.timerIntervalVariableName,null));
			incrementTimeSinceStart=DialogueOperatorEffect.createIncrementForVariable(NLBusBase.timeSinceStartVariableName,
					DialogueKBFormula.create(NLBusBase.timerIntervalVariableName,null));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void updateISwithNLUvariablesFromEvent(DialogueKB is,Event ev) throws Exception {
		Object payload=ev.getPayload();
		if ((payload!=null) && (payload instanceof NLUOutput)) {
			NLUOutput sa=(NLUOutput) payload;
			Map<String,Object> thisIterationNLUVariables=new HashMap<String, Object>();
			if (sa instanceof ChartNLUOutput) {
				List<Triple<Integer, Integer, NLUOutput>> portions = ((ChartNLUOutput)sa).getPortions();
				if (portions!=null) {
					for(Triple<Integer, Integer, NLUOutput> portion:portions) {
						NLUOutput nlu = portion.getThird();
						Object nluPayload=nlu.getPayload();
						if (nluPayload!=null) {
							thisIterationNLUVariables.putAll((Map<String, Object>) nluPayload);
						}
					}
				}
			} else {
				thisIterationNLUVariables=(Map<String, Object>) sa.getPayload();
			}
			removeNLUVariableFromIS(is,nluVariables);
			nluVariables=thisIterationNLUVariables;
			setNLUVariablesInIS(is,nluVariables);
		}
	}
	private void incrementUnhandledEventTracker() throws Exception {
		DialogueKB informationState=getInformationState();
		informationState.setValueOfVariable(NLBusBase.counterConsecutiveUnhandledUserActionsVariableName,previousUnhandledCounter,ACCESSTYPE.AUTO_OVERWRITEAUTO);
		informationState.store(incrementUnhandledCounterVariable, ACCESSTYPE.AUTO_OVERWRITEAUTO, true);
		if (logger.isDebugEnabled())
			logger.debug("unhandled counter updated to: "+informationState.getValueOfVariable(NLBusBase.counterConsecutiveUnhandledUserActionsVariableName,ACCESSTYPE.AUTO_OVERWRITEAUTO,null));
		informationState.store(incrementUnhandledInTurnCounterVariable, ACCESSTYPE.AUTO_OVERWRITEAUTO, true);
		if (logger.isDebugEnabled())
			logger.debug("unhandled counter in turn updated to: "+informationState.getValueOfVariable(NLBusBase.counterConsecutiveUnhandledUserActionsSinceLastSystemActionVariableName,ACCESSTYPE.AUTO_OVERWRITEAUTO,null));
	}
	private void setNLUVariablesInIS(DialogueKBInterface is,Map<String, Object> vars) throws Exception {
		if (vars!=null) {
			for(String vName:vars.keySet()) {
				String nvName=vName.toLowerCase();
				is.setValueOfVariable(nvName,vars.get(vName),ACCESSTYPE.AUTO_OVERWRITEAUTO);
			}
		}
	}
	public void updateSystemSayTracker(final NLGEvent ev) throws Exception {
		DialogueKB is=getInformationState();
		final String say=ev.getDMEventName();
		is.setValueOfVariable(NLBusBase.lastSystemSayVariableName, DialogueKBFormula.create("'"+say+"'", null),ACCESSTYPE.AUTO_OVERWRITEAUTO);
		is.setValueOfVariable(NLBusBase.timeSinceLastSystemActionVariableName,DialogueKBFormula.create("0", null),ACCESSTYPE.AUTO_OVERWRITEAUTO);
		is.setValueOfVariable(NLBusBase.timeSinceLastActionVariableName,DialogueKBFormula.create("0", null),ACCESSTYPE.AUTO_OVERWRITEAUTO);
		DMEventsListenerInterface mb = getMessageBus();
		if (mb!=null) {
			if (mb.isResource(getSessionID(), ev)) {
				is.setValueOfVariable(NLBusBase.timeSinceLastResourceVariableName,DialogueKBFormula.create("0", null),ACCESSTYPE.AUTO_OVERWRITEAUTO);
			}
		}

		getSpeakingTracker().setSpeaking(ev);
		if (systemFinishedSpeakingTracker!=null) {
			if (getConfiguration().getSystemEventsHaveDuration()) {
				systemFinishedSpeakingTracker.setter();
			} else {
				systemFinishedSpeakingTracker.touch();
			}
		}
		
		is.setValueOfVariable(NLBusBase.lengthOfLastUserTurnVarName,DialogueKBFormula.create("0", null),ACCESSTYPE.AUTO_OVERWRITEAUTO);
	}

	private void removeNLUVariableFromIS(DialogueKBInterface is,Map<String, Object> vars) throws Exception {
		if (vars!=null) {
			for(String vName:vars.keySet()) {
				vName=vName.toLowerCase();
				is.removeVariable(vName,ACCESSTYPE.AUTO_OVERWRITEAUTO);
			}
		}
	}

	@Override
	public NLUOutput selectNLUOutput(String text,Long sessionId,List<NLUOutput> userSpeechActs) throws Exception {
		if (userSpeechActs!=null && !userSpeechActs.isEmpty()) {
			NLUOutput speechAct = userSpeechActs.get(0);
			logger.info(" highest probability user event is: "+speechAct);
			return speechAct;
		}
		else {
			String lowConfidenceEvent=getConfiguration().getLowConfidenceEvent();
			if (StringUtils.isEmptyString(lowConfidenceEvent)) {
				logger.warn(" no NLU output and LOW confidence event disabled, returning no NLU results.");
				return null;
			} else {
				logger.warn(" no NLU output. adding the low confidence event.");
				return new NLUOutput(text,lowConfidenceEvent, 1f, null);
			}
		}
	}
	
	public List<DialogueOperatorEntranceTransition> getOperatorsThatCanBeStartedByThisEvent(PossibleIS pis, Event ev,DialogueAction currentAction,OpType type) throws Exception {
		List<DialogueOperatorEntranceTransition> ret;
		
		EvalContext context=pis.getEvalContextFromThis();
		
		if (ev==null) {
			ret=getOperatorsThatSupportSystemInitiative(context,pis.getDormantOperators(),type);
			if (currentAction!=null) removeECsThatBelongsToOperator(ret,currentAction.getOperator());
		} else ret=getOperatorsThatCanBeStartedByThisEvent(context, ev,type);
		return ret;
	}
	private void removeECsThatBelongsToOperator(List<DialogueOperatorEntranceTransition> ecs, DialogueOperator op) {
		if (ecs!=null && op!=null) {
			Iterator<DialogueOperatorEntranceTransition> it = ecs.iterator();
			while(it.hasNext()) {
				DialogueOperatorEntranceTransition el = it.next();
				DialogueOperator ecOp = el.getOperator();
				if (ecOp==op) it.remove();
			}
		}
	}
	/*private List<DialogueOperatorEntranceTransition> getOperatorsThatSupportSystemInitiative(PossibleIS pis) throws Exception {
		//logger.debug("    Looking for operators that can be initiated by the system.");
		List<DialogueOperatorEntranceTransition> handlers=null;
		
		Set<DialogueOperator> sis = dp.getSystemInitiatableOperators();
		DormantActions dormantActions = pis.getDormantOperators();
		LinkedHashSet<DialogueOperator> operators = new LinkedHashSet<DialogueOperator>();
		if (sis!=null) operators.addAll(sis);
		if (dormantActions!=null) operators.addAll(dormantActions.getDormantOperators());
		
		DialogueKBInterface is = pis.getValue();
		for(DialogueOperator op:operators) {
			LinkedHashSet<DialogueOperatorEntranceTransition> ecs = (pis.isDormant(op))?op.getReEntranceOptions():op.getEntranceConditions();
			if (ecs!=null) {
				for(DialogueOperatorEntranceTransition ec:ecs) {
					if (ec.isSystemInitiatable()) {
						if (ec.isExecutableInCurrentIS(null,is)) {
							if (handlers==null) handlers=new ArrayList<DialogueOperatorEntranceTransition>();
							handlers.add(ec);
						}
					} else if (ec.isReEntrable()) {
						DialogueAction da = dormantActions.getDormantActionOf(op);
						DialogueKBInterface tis = da.getInternalKB();
						if (ec.isExecutableInCurrentIS(null,tis)) {
							if (handlers==null) handlers=new ArrayList<DialogueOperatorEntranceTransition>();
							handlers.add(ec);
						}
					}
				}
			}
		}
		//if (handlers!=null) logger.debug("     Found system initiative entrance conditions: "+handlers);
		return handlers;
	}*/
	private List<DialogueOperatorEntranceTransition> getOperatorsThatSupportSystemInitiative(EvalContext context,DormantActions dormantActions,OpType type) throws Exception {
		//logger.debug("    Looking for operators that can be initiated by the system.");
		List<DialogueOperatorEntranceTransition> handlers=null;
		
		Set<DialogueOperator> sis = dp.getSystemInitiatableOperators(type);
		LinkedHashSet<DialogueOperator> operators = new LinkedHashSet<DialogueOperator>();
		if (sis!=null) operators.addAll(sis);
		if (dormantActions!=null) operators.addAll(dormantActions.getDormantOperators());
		
		for(DialogueOperator op:operators) {
			LinkedHashSet<DialogueOperatorEntranceTransition> ecs = null;
			if (dormantActions!=null && dormantActions.isThisDormant(op)) {
				ecs=op.getReEntranceOptions();
			} else {
				ecs=op.getEntranceConditions();
			}
			if (ecs!=null) {
				for(DialogueOperatorEntranceTransition ec:ecs) {
					if (ec.isSystemInitiatable()) {
						if (ec.isExecutableInCurrentIS(null,context)) {
							if (handlers==null) handlers=new ArrayList<DialogueOperatorEntranceTransition>();
							handlers.add(ec);
						}
					} else if (ec.isReEntrable()) {
						DialogueAction da = dormantActions.getDormantActionOf(op);
						EvalContext acontext = da.getContext();
						if (ec.isExecutableInCurrentIS(null,acontext)) {
							if (handlers==null) handlers=new ArrayList<DialogueOperatorEntranceTransition>();
							handlers.add(ec);
						}
					}
				}
			}
		}
		//if (handlers!=null) logger.debug("     Found system initiative entrance conditions: "+handlers);
		return handlers;
	}

	private List<DialogueOperatorEntranceTransition> getOperatorsThatCanBeStartedByThisEvent(EvalContext context, Event ev,RewardPolicy.OpType type) throws Exception {
		List<DialogueOperatorEntranceTransition> ret=null;
		if (ev!=null && !StringUtils.isEmptyString(ev.getName())) {
			NLUOutput nlu=(NLUOutput) ev.getPayload();
			if (nlu==null) {
				ret=getOperatorsThatCanBeStartedByThisEvent(context,ev.getName(),type);
			} else if (nlu instanceof ChartNLUOutput) {
				List<Triple<Integer, Integer, NLUOutput>> portions = ((ChartNLUOutput) nlu).getPortions();
				if (portions!=null) {
					for(Triple<Integer, Integer, NLUOutput> portion:portions) {
						List<DialogueOperatorEntranceTransition> thisPortionHandlers=getOperatorsThatCanBeStartedByThisEvent(context,portion.getThird().getId(),type);
						if (thisPortionHandlers!=null) {
							if (ret==null) ret=new ArrayList<DialogueOperatorEntranceTransition>();
							ret.addAll(thisPortionHandlers);
						}
					}
				}
			} else {
				ret=getOperatorsThatCanBeStartedByThisEvent(context,nlu.getId(),type);
			}
		}
		return ret;
	}
	// get the operators that define an entrance condition for the given user event and that have
	// at least one of the handlers that can be executed.
	private List<DialogueOperatorEntranceTransition> getOperatorsThatCanBeStartedByThisEvent(EvalContext context, String evName,RewardPolicy.OpType type) throws Exception {
		List<DialogueOperatorEntranceTransition> handlers = null;
		if (!StringUtils.isEmptyString(evName)) { 
			List<DialogueOperator> possibleOps = dp.getUserTriggerableOperatorsForEvent(evName,type);
			if (possibleOps!=null && !possibleOps.isEmpty()) {
				for(DialogueOperator op:possibleOps) {
					List<DialogueOperatorEntranceTransition> entranceConditions=op.getUserTriggerableTransitionsForEvent(evName);
					if(entranceConditions!=null) {
						for(DialogueOperatorEntranceTransition ec:entranceConditions) {
							if (ec.isExecutableInCurrentIS(evName,context)) {
								// replace the entrance conditions of operators that can be reentered with the re-entrance options.
								/*if (dormantOperators.contains(op)) {
									Set<DialogueOperatorEntranceTransition> recs = op.getReEntranceOptions();
									if ((recs!=null) && !recs.isEmpty()) {
										logger.debug("     Replacing entrance conditions for "+op.getName()+" with re-entrance conditions because it's dormant.");
										if (handlers==null) handlers=new ArrayList<DialogueOperatorEntranceTransition>();
										handlers.addAll(recs);
									}
									else logger.debug("     Removing operator "+op.getName()+" because dormant and not re-enterable.");
								} else {*/
									if (handlers==null) handlers=new ArrayList<DialogueOperatorEntranceTransition>();
									handlers.add(ec);
								//}
								break;
							}
						}
					}
				}
			}
		}
		return handlers;
	}


	private NextActionSelector findBestEntranceCondition4Operator2(Event ev, DialogueAction currentAction) throws Exception {
		NextActionSelector bestSelector=new NextActionSelector(this);

		SearchSpace ss = (ev!=null)?new SearchSpace(this,currentAction,ev):new SearchSpace(this,currentAction);
		Pair<TERMINATION_CAUSE, Integer> endState = ss.runForwardSearch();
		
		WeightedDialogueOperatorEntranceTransition rr = ss.computeRewardForIgnoringEvent();
		if (rr!=null) bestSelector.updateBestWith(rr, SearchTermination.DROP_CURRENT_ACTION_IGNORE_EVENT);

		rr = ss.computeRewardForHandlingEvent();
		if (rr!=null) bestSelector.updateBestWith(rr, SearchTermination.DROP_CURRENT_ACTION_HANDLE_EVENT);
		
		rr = ss.computeRewardForContinuingCurrentAction();
		if (rr!=null) bestSelector.updateBestWith(rr, SearchTermination.KEEP_CURRENT_ACTION_IGNORE_EVENT);

		if (visualizer!=null) {
			List<PossibleTransition> possibilities=new ArrayList<PossibleTransition>();
			if (ss.rootKeep!=null && ss.rootKeep.getOutgoingEdges()!=null)
				possibilities.addAll((List)ss.rootKeep.getOutgoingEdges());
			if (ss.rootIgnore!=null && ss.rootIgnore.getOutgoingEdges()!=null)
				possibilities.addAll((List)ss.rootIgnore.getOutgoingEdges());
			if (ss.rootHandle!=null && ss.rootHandle.length>0){
				for(int i=0;i<ss.rootHandle.length;i++) {
					if (ss.rootHandle[i].getOutgoingEdges()!=null) {
						possibilities.addAll((List)ss.rootHandle[i].getOutgoingEdges());
					}
				}
			}
			Collections.sort(possibilities, new Comparator<PossibleTransition>() {
				@Override
				public int compare(PossibleTransition o1, PossibleTransition o2) {
					return (int) (o2.getReward()-o1.getReward());
				}
			});
			visualizer.addSearchResult(possibilities, null);
		}
		
		Float reward=bestSelector.getBestReward();
		if (reward==null || reward<=0) {
			logger.debug("     no best action or best action has no reward (="+reward+"), ignoring it.");
			bestSelector.setBest(null);
		}
			
		FoundDialogueOperatorEntranceTransition best = bestSelector.getBest();
		logger.debug("    Best action selected: "+best);

		return bestSelector;
	}
	
	private static String loopEvent=null;
	public RewardDM(NLBusConfig config) {
		super(config);
		String loopEvent=getConfiguration().getLoopEventName();
		if (!StringUtils.isEmptyString(loopEvent)) this.loopEvent=loopEvent;
		else this.loopEvent=null;
		stateTracker=new StateTracker();
		MAX_SEARCH_LEVELS=config.getMaxSearchLevels();
		MAX_ITERATIONS=10;
	}
	public RewardDM(long sessionID,RewardPolicy dp,NLBusConfig config,NLBusInterface listener) throws Exception {
		this(config);
		this.setMessageBus(listener);
		this.dormantActions=new DormantActions();
		this.dormantDaemonActions=new DormantActions();
		this.historyOfExecutedOperators=new OperatorHistoryNode(null, null);
		
		this.dp = dp;
		setSessionID(sessionID);
		if (config.getVisualizerConfig()!=null && config.getVisualizerClass()!=null) {
			Class vc=Class.forName(config.getVisualizerClass());
			Constructor c = vc.getConstructor(DM.class,Rectangle.class);
			visualizer=(DMVisualizerI) c.newInstance(this,visualizerBounds);
		}
		
		if (!StringUtils.isEmptyString(config.getUnhandledEventName()))
			unhandledUserEvent=new DMInternalEvent(config.getUnhandledEventName(), sessionID);
		if (!StringUtils.isEmptyString(config.getForcedIgnoreEventName()))
			forcedIgnoreUserEvent=new DMInternalEvent(config.getForcedIgnoreEventName(), sessionID);
		
		//informationState.addTracingFor("donequestion");
		this.context=new EvalContext(new TrivialDialogueKB(this));
		
		DialogueKB informationState=getInformationState();
		initializeInformationState(informationState);
		//Collection map = FunctionalLibrary.map(getMessageBus().getSpecialVariables(sessionID),SpecialVar.class.getMethod("getName"));
		//Set<String> specialVarNames = (map == null) ? null : new HashSet<String>(map);
		Collection<VarChange> changes = informationState.getCurrentValues(null);
		sendVarChangeEventsCausedby(changes, DMInternalEvent.INIT);

		loadGoalValuesInIS(informationState,dp.getGoals());
				
		float timer=config.getTimerInterval();
		highSpeedTimerThread=new Timer("RewardDMSpeedTimer-"+getSessionID());
		timerEventThread = new Timer("RewardDMTimer-"+getSessionID());
		int msTimerDelay=-1;
		if (timer>0) {
			msTimerDelay=Math.round(1000f*timer);
			if (msTimerDelay<=1.2*MAX_SEARCH_TIME) {
				logger.warn("SEARCH TIMEOUT TOO CLOSE TO TIMER INTERVAL: "+MAX_SEARCH_TIME+" vs "+msTimerDelay);
				MAX_SEARCH_TIME=Math.round((float)msTimerDelay*0.9);
				logger.warn("reduced search timeout to: "+MAX_SEARCH_TIME+"[ms].");
			}
			timerEventThread.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					handleEvent(new DMInternalEvent(getConfiguration().getTimerEvent(),getSessionID()));
					if (isSessionDone()) {
						killTimerThreads();
					}
				}
			},msTimerDelay,msTimerDelay);
		}
		highSpeedTimerThread.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (trackers!=null) {
					for(ValueTracker vt:trackers) {
						try {
							vt.setter();
						} catch (Exception e) {
							logger.error(e);
						}
					}
				}
			}
		},10,10);
		if (msTimerDelay>0 && msTimerDelay>2*highSpeedTimerDelay) {
			highSpeedTimerThread.scheduleAtFixedRate(new TimerTask() {
				@Override
				public void run() {
					try {
						runDaemons(null);
					} catch (Exception e) {
						logger.error("Error while running the daemons in timer: ",e);
					}
				}
			},highSpeedTimerDelay,highSpeedTimerDelay);
		} else {
			logger.info("NOT starting high speed thread as default thread runs every: "+msTimerDelay+"[ms] (high speed is: "+highSpeedTimerDelay+"[ms]).");
		}
		
		if (getTimerThread() != null)
			this.speakingTracker=new SpeakingTracker(this,getTimerThread());
		else
			this.speakingTracker=new SpeakingTracker(this,new Timer("SpeakingTrackerTimer-"+getSessionID()));

		List<String> vts=config.getValueTrackers();
		if (vts!=null) {
			for(String vtName:vts) {
				if (trackers==null) trackers=new ArrayList<ValueTracker>();
				ValueTracker vt=createTracker(vtName);
				trackers.add(vt);
				if (vt instanceof SystemFinishedSpeakingTracker)
					systemFinishedSpeakingTracker=(SystemFinishedSpeakingTracker) vt;
			}
		}
		timemarkTracker=new TimemarksTracker(getLogger());
	}

	public ValueTracker createTracker(String className) {
		try {
			if (!StringUtils.isEmptyString(className)) {
				Class cc = Class.forName(className);
				Constructor nluconstructor = cc.getConstructor(this.getClass());
				return (ValueTracker) nluconstructor.newInstance(this);
			}
		} catch (Exception e) {
			logger.error("Error while creating value tracker with class: "+className, e);
		}
		return null;
	}

	/**
	 * initialize the information state with the policy init and the special variables init from the bus.
	 * plus sets the dminstance variable and timer variable.
	 * @param is
	 */
	private void initializeInformationState(DialogueKB is) {
		try {
			loadAllSpecialVariables(is);
			loadInformationStateInitializationFromPolicy(is);
			is.setValueOfVariable(NLBusBase.dmVariableName, this,ACCESSTYPE.AUTO_OVERWRITEAUTO);
			is.setValueOfVariable(NLBusBase.timerIntervalVariableName, getConfiguration().getTimerInterval(),ACCESSTYPE.AUTO_OVERWRITEAUTO);
		} catch (Exception e) {
			logger.error("error initializing information state.",e);
		}
	}
	private void loadInformationStateInitializationFromPolicy(DialogueKB is) {
		try {
			Collection<DialogueOperatorEffect> effs = dp.getISinitialization();
			is.storeAll(effs, ACCESSTYPE.AUTO_OVERWRITEAUTO, true);
			for (DialogueOperatorEffect eff:effs) {
				if (eff.isAssignment()) {
					VariableProperties ps = eff.getAssignmentProperties();
					is.setProperties(eff.getAssignedVariable().getName(), ps);
				}
			}
		} catch (Exception e) {
			logger.error("error loading information state initialization from policy.",e);
		}
	}
	private void loadAllSpecialVariables(DialogueKB is) {
		try {
			List<SpecialVar> variables = getMessageBus().getSpecialVariables(getSessionID());
			if (variables!=null) {
				for(SpecialVar sv:variables) {
					String vName=sv.getName();
					if (!is.hasVariableNamed(vName, ACCESSTYPE.AUTO_OVERWRITEAUTO)) {
						try {
							is.setValueOfVariable(vName, DialogueKBFormula.parse(sv.getValue()), ACCESSTYPE.AUTO_OVERWRITEAUTO);
						} catch (Exception e) {
							logger.error("error loading special variable '"+vName+"'.",e);
						}
					}
					is.setPropertyForVar(vName, PROPERTY.READONLY, sv.isReadOnly());
					is.setPropertyForVar(vName, PROPERTY.HIDDEN, sv.isHidden());
					is.setPropertyForVar(vName, PROPERTY.PERSISTENT, sv.isPersistent());
				}
			}
		} catch (Exception e1) {
			logger.error("Error while getting special variables.", e1);
		}
	}
	
	private void loadGoalValuesInIS(DialogueKBInterface is,HashMap<String, Pair<DialogueKBFormula, String>> goals) throws Exception {
		if(goals!=null) {
			for (String gn:goals.keySet()) {
				Pair<DialogueKBFormula,String> valueAndDesc=goals.get(gn);
				DialogueKBFormula goalValue=valueAndDesc.getFirst();
				if (StringUtils.isEmptyString(gn) || (goalValue==null)) throw new Exception("Invalid goal specification: "+gn+"="+valueAndDesc);
				else {
					String goalVarName=DialogueOperatorEffect.buildVarNameForGoal(gn);
					is.setValueOfVariable(goalVarName, goalValue,ACCESSTYPE.AUTO_OVERWRITEAUTO);
				}
			}
		}
	}

	public enum TERMINATION_CAUSE {NORMAL,TIMEOUT,LEVELS};
	public TERMINATION_CAUSE isTimeToEndSearch(Queue<PossibleIS> possibilities, int levels, long startTime) {
		if (possibilities.isEmpty()) return TERMINATION_CAUSE.NORMAL;
		else if (levels>=MAX_SEARCH_LEVELS) return TERMINATION_CAUSE.LEVELS;
		else if ((System.currentTimeMillis()-startTime)>MAX_SEARCH_TIME) return TERMINATION_CAUSE.TIMEOUT;
		else return null;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////
	// START REWARD CALCULATION
	//////////////////////////////////////////////////////////////////////////////////////////
	
	private float computeNodeReward(boolean isCopyNode,float nodeReward,float nodeWeight,float treeReward,float stepLoss, PossibleIS node) {
		assert((nodeReward>0)?nodeWeight>0:true);
		if (logger.isDebugEnabled()) {
			HashMap<DialogueOperator, Float> contributes = node.getContributes();
			if (contributes==null) node.setContributes(contributes=new HashMap<DialogueOperator, Float>());
			for(DialogueOperator op:contributes.keySet()) {
				Float r=contributes.get(op);
				assert(r!=null);
				contributes.put(op, r*stepLoss);
			}
			{
				DialogueOperator op=node.getOperator();
				if (op!=null) {
					Float r=contributes.get(op);
					contributes.put(op, ((r!=null)?r:0)+nodeReward);
				}
			}
			for(DialogueOperator op:contributes.keySet()) {
				Float r=contributes.get(op);
				assert(r!=null);
				contributes.put(op, r*nodeWeight);
			}
		}
		
		if (isCopyNode) return (nodeReward+treeReward*stepLoss)*nodeWeight;
		else return (nodeReward+treeReward*stepLoss)*nodeWeight;
	}
	private void sumContributionInto(
			HashMap<DialogueOperator, Float> total,
			HashMap<DialogueOperator, Float> child) {
		if (total!=null && child!=null) {
			for(DialogueOperator op:child.keySet()) {
				Float t=total.get(op);
				Float c=child.get(op);
				if (c!=null) total.put(op,((t!=null)?t:0)+c);
			}
		}
	}
	/* compute the reward of a node:
	 * ROOT and CHAIN: take reward of node + max of children (reward of node may be different than 0 only for CHAIN)
	 * EC: take reward of node + sum of children (reward of node should be 0)
	 */
	public float getPossibilityReward(PossibleIS node) throws Exception {
		float treeReward=0,nodeWeight=node.getWeight(),nodeReward=node.getChainReward(),stepLoss=dp.getStepLoss();
		
		List<Edge> possibilities;
		switch (node.getType()) {
		case ROOT:
			assert(nodeReward==0);
		case CHAIN:
			assert(nodeWeight>0);

			boolean isMergedNode=(node.isChain() && (node.getOutgoingEdges()!=null) && 
					(node.getImmediateChildren().size()==1) && ((PossibleIS) node.getFirstChild()).isChain());
			assert(!node.isChain() || FunctionalLibrary.every(node.getImmediateChildren(),PossibleIS.class.getMethod("isEntranceCondition")) ||	isMergedNode);

			if (node.isTreeRewardSet()) return computeNodeReward(isMergedNode,nodeReward,nodeWeight,node.getTreeReward(),stepLoss,node);
			else {
				if (isMergedNode) {
					PossibleIS copyChild=(PossibleIS) node.getFirstChild();
					assert(copyChild!=null);
					getPossibilityReward(copyChild);
					treeReward=copyChild.getTreeReward();
					if (logger.isDebugEnabled()) node.setContributes(copyChild.getContributes());
				} else {
					Float bestChildReward=null;
					possibilities = node.getOutgoingEdges();
					if (possibilities!=null && !possibilities.isEmpty()) {
						for(Edge pst:possibilities) {
							PossibleTransition ctr=(PossibleTransition)pst;
							PossibleIS nextNode=(PossibleIS) ctr.getTarget();
		
							float childReward=getPossibilityReward(nextNode);
							ctr.setReward(childReward);
							if ((bestChildReward==null) || (childReward>bestChildReward)) {
								bestChildReward=childReward;
								if (logger.isDebugEnabled()) node.setContributes(nextNode.getContributes());
							}
						}
					}
					
					treeReward=(bestChildReward==null)?0:bestChildReward;
				}
	
				node.setTreeReward(treeReward);
				return computeNodeReward(isMergedNode,nodeReward, nodeWeight, treeReward, stepLoss,node);
			}
		case EC:
			assert(nodeReward==0);
			assert(FunctionalLibrary.every(node.getImmediateChildren(),PossibleIS.class.getMethod("isChain")));

			if (node.isTreeRewardSet()) return computeNodeReward(false,0,1,node.getTreeReward(),stepLoss,node);
			else {
				float childrenReward=0;
				possibilities = node.getOutgoingEdges();
				if (possibilities!=null && !possibilities.isEmpty()) {
					HashMap<DialogueOperator, Float> childrenContributes = null;
					if (logger.isDebugEnabled()) childrenContributes=new HashMap<DialogueOperator,Float>();

					for(Edge pst:possibilities) {
						PossibleTransition ctr=(PossibleTransition)pst;
						PossibleIS nextNode=(PossibleIS) ctr.getTarget();
						float childReward=getPossibilityReward(nextNode);
						ctr.setReward(childReward);
						childrenReward+=childReward;
						sumContributionInto(childrenContributes,nextNode.getContributes());
					}
					if (logger.isDebugEnabled()) node.setContributes(childrenContributes);
				}
				treeReward=childrenReward;
	
				node.setTreeReward(treeReward);
				return computeNodeReward(false,0, 1, treeReward, stepLoss,node);
			}
		default:
			throw new Exception("Unknown node type.: "+node);
		}
	}

	/** for every possible entrance condition, returns the reward that can be obtained taking that entrance condition.
	 *  An entrance condition may be used in multiple possibilities because from that initial state one may end up in different final states.
	 */
	public WeightedDialogueOperatorEntranceTransition getBestEntranceCondition4Operator(PossibleIS root) throws Exception {
		assert(root.isRoot());
		assert(root.getChainReward()==0);
		Float bestChildReward=null;
		DialogueOperatorEntranceTransition bestEC=null;
		PossibleIS bestPIS=null;
		List<Edge> possibilities = root.getOutgoingEdges();
		if (possibilities!=null && !possibilities.isEmpty()) {
			// randomize order of possibilities to randomize the selection among equally good alternatives.
			Collections.shuffle(possibilities);
			for(Edge pst:possibilities) {
				PossibleTransition ctr=(PossibleTransition)pst;
				PossibleIS nextNode=(PossibleIS) ctr.getTarget();
				assert(nextNode.isEntranceCondition());
				
				DialogueOperatorEntranceTransition ec = ctr.getEntranceCondition();
				float childReward=getPossibilityReward(nextNode);
				ctr.setReward(childReward);
				if ((bestChildReward==null) || (childReward>bestChildReward) || 
						(NumberUtils.roughlyEqualRelative(childReward,bestChildReward,0.05) && bestEC.isReEntrable() && ec.isReEntrable() && (bestEC.getOperator()==ec.getOperator()) && thisReentersNearerToActiveStateThan(ec,bestEC))) {
					bestChildReward=childReward;
					bestEC=ec;
					bestPIS=nextNode;
				}
			}
		}
		if (logger.isDebugEnabled() && bestPIS!=null) {
			// print top level possibilities
			for(Edge pst:possibilities) {
				PossibleTransition ctr=(PossibleTransition)pst;
				DialogueOperatorEntranceTransition ec = ctr.getEntranceCondition();
				logger.debug("   OPTION: "+ec.getOperator().getName()+" with reward "+ctr.getReward());
			}
			// print contributor for the selection of this operator as best operator
			bestPIS.log(logger);
		}
		return new WeightedDialogueOperatorEntranceTransition(bestEC,bestChildReward);
	}
	
	private boolean thisReentersNearerToActiveStateThan(DialogueOperatorEntranceTransition n,DialogueOperatorEntranceTransition o) throws Exception {
		if (n!=null && o!=null && n.getOperator()==o.getOperator()) {
			DialogueOperator op=n.getOperator();
			DialogueAction action = getDormantActions((op.isDaemon())?OpType.DAEMON:OpType.NORMAL).getDormantActionOf(op);
			ActiveStates activeStates = action.getActiveStates();
			if (activeStates!=null) {
				Integer minDistanceNew=null;
				DialogueOperatorNode newTarget = n.getTarget();
				for(DialogueOperatorNode node:activeStates) {
					Integer d=newTarget.getDistanceTo(node);
					if (d!=null && ((minDistanceNew==null) || (d<minDistanceNew))) minDistanceNew=d;
				}
				if (minDistanceNew==null) return false;
				Integer minDistanceOld=null;
				DialogueOperatorNode oldTarget = o.getTarget();
				for(DialogueOperatorNode node:activeStates) {
					Integer d=oldTarget.getDistanceTo(node);
					if (d!=null && ((minDistanceOld==null) || (d<minDistanceOld))) minDistanceOld=d;
				}
				return minDistanceNew<minDistanceOld;
			}
		}
		return false;
	}
	public float getRemainingRewardForSetOfEffects(List<DialogueOperatorNodesChain> effectsSets,DialogueOperator op,DialogueKB is) throws Exception {
		if (effectsSets!=null && !effectsSets.isEmpty()) {
			float reward=0;
			PossibleIS pis = new PossibleIS(op,PossibleIS.Type.ROOT,"tmp", is, null);
			pis.setChainReward(0);
			
			for(DialogueOperatorNodesChain pathAndEffects:effectsSets) {
				Pair<DialogueKB, Float> is_reward = pis.updateISAndgetReward(is,pathAndEffects);
				//System.out.println(is_reward.getSecond());
				reward+=pathAndEffects.getWeight()*is_reward.getSecond();
			}
			return reward;
		} else return 0;
	}
	
	//////////////////////////////////////////////////////////////////////////////////////////
	// END REWARD CALCULATION
	//////////////////////////////////////////////////////////////////////////////////////////
	
	public void setDone(boolean d) {
		this.done=d;
		if (d) {
			killTimerThreads();
			getMessageBus().terminateSession(getSessionID());
		}
	}
	@Override
	public void kill() {
		super.kill();
		if (visualizer!=null) {
			// save visualizer window position and dimensions
			visualizerBounds=visualizer.getBounds();
			visualizer.kill();
		}
		setDone(true);
	}
	public void killTimerThreads() {
		if (speakingTracker.getTimer() != timerEventThread) {
			logger.warn("Terminated timer thread: SpeakingTrackerTimer-"+getSessionID());
			speakingTracker.getTimer().cancel();
		}
		if (timerEventThread!=null) {
			logger.warn("Terminated timer thread: RewardDMTimer-"+getSessionID());
			timerEventThread.cancel();
			timerEventThread = null;
		}
		if (highSpeedTimerThread!=null) {
			logger.warn("Terminated high speed timer thread: RewardDMSpeedTimer-"+getSessionID());
			highSpeedTimerThread.cancel();
			highSpeedTimerThread = null;
		}
	}

	public EvalContext getContext() {
		return context;
	}
	public DialogueKB getRootInformationState() {
		return getContext().getInformationState();
	}
	public DialogueKB getInformationState() {
		try {
			DialogueAction aa = getCurrentActiveAction();
			if (aa!=null) {
				DialogueKB localIS=aa.getInternalKB();
				// localIS can be null if the action has not been executed yet.
				if (localIS==null) return getRootInformationState();
				else return localIS;
			}
			else return getRootInformationState();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getRootInformationState();
	}

	@Override
	public Map<NLUOutput, List<List<String>>> getPossibleSystemResponsesForThesePossibleInputs(List<NLUOutput> userInputs) throws Exception {
		logger.info("ALL NEXT MESSAGES ARE CAUSED BY THIS CALL TO getPossibleSystemResponsesForThesePossibleInputs.");
		//throw new Exception("unsupported");
		Map<NLUOutput, List<List<String>>> mapping=new HashMap<NLUOutput, List<List<String>>>();
		if (userInputs!=null) {
			NLUEvent[] pes=new NLUEvent[userInputs.size()];
			int i=0;
			for(NLUOutput usa:userInputs) {
				pes[i++]=new NLUEvent(usa,getSessionID());
			}
			DialogueAction currentAction=getCurrentActiveAction();
			SearchSpace ss = new SearchSpace(this,currentAction,pes);
			Pair<TERMINATION_CAUSE, Integer> endState = ss.runForwardSearch();
			
			NextActionSelector[] selectors=new NextActionSelector[pes.length];
			WeightedDialogueOperatorEntranceTransition ignoring = ss.computeRewardForIgnoringEvent();
			WeightedDialogueOperatorEntranceTransition[] handling = ss.computeRewardsForHandlingEvents();
			WeightedDialogueOperatorEntranceTransition continuing = ss.computeRewardForContinuingCurrentAction();
			for(i=0;i<pes.length;i++) {
				if (selectors[i]==null) selectors[i]=new NextActionSelector(this);
				selectors[i].updateBestWith(ignoring, SearchTermination.DROP_CURRENT_ACTION_IGNORE_EVENT);
				selectors[i].updateBestWith(handling[i], SearchTermination.DROP_CURRENT_ACTION_HANDLE_EVENT);
				selectors[i].updateBestWith(continuing, SearchTermination.KEEP_CURRENT_ACTION_IGNORE_EVENT);
			}
			
			for(i=0;i<pes.length;i++) {
				NLUOutput usa=pes[i].getPayload();
				FoundDialogueOperatorEntranceTransition best=selectors[i].getBest();
				List<List<String>> systemActions=null;
				
				TrivialDialogueKB tmp=new TrivialDialogueKB(getInformationState());
				FoundDialogueOperatorEntranceTransition fec = checkIgnoredAndUnhandledHandlers(pes[i], currentAction, selectors[i]);
				DialogueOperatorEntranceTransition ec=fec.getEntranceCondition();
				best.setEntranceCondition(ec);
				
				
				NLUEvent handledEvent = (NLUEvent)sendEventToCurrentActionOrToSearchOrUserProvidedSearchResult(pes[i], best);
				updateISwithNLUvariablesFromEvent(tmp,handledEvent!=null?handledEvent:pes[i]);
				tmp.store(DialogueOperatorEffect.createAssignment(NLBusBase.lastEventVariableName, DialogueKBFormula.create("'"+handledEvent+"'", null)),ACCESSTYPE.AUTO_OVERWRITETHIS,true);

				if (currentAction!=null && best.getEntranceCondition().getOperator()==currentAction.getOperator()) {
					systemActions=currentAction.getOperator().simulateAndCollectAllSystemActions(handledEvent,tmp,currentAction.getActiveStates());
				} else {
					DialogueOperator op=ec.getOperator();
					Map<String, DialogueKBFormula> localVars = op.getLocalVars();
					if (localVars!=null && !localVars.isEmpty()) {
						for(String varName:localVars.keySet()) {
							DialogueKBFormula varValue = localVars.get(varName);
							Object result = tmp.evaluate(varValue,null);
							DialogueOperatorEffect effect=DialogueOperatorEffect.createAssignment(varName, result);
							tmp.store(effect, ACCESSTYPE.AUTO_OVERWRITETHIS, false);
						}
					}
					new DialogueAction(ec, this);
					ActiveStates activeStates=new DialogueAction().getActiveStates();
					activeStates.transition(null, ec.getTarget());
					handledEvent=null; // because it's consumed by the entrance condition.
					systemActions=op.simulateAndCollectAllSystemActions(handledEvent,tmp,activeStates);
				}
				
				mapping.put(usa, systemActions);
			}
			
		}
		logger.info("DONE WITH CALL TO getPossibleSystemResponsesForThesePossibleInputs.");
		return mapping;

		/*Map<String, Set<String>> mapping=new HashMap<String, Set<String>>();
		for (NLUOutput usa:userInputs) {
			String speechActID = usa.getId();
			Object payload = usa.getPayload();
			List<Event> systemResponses = handleUserEvent(new Event(speechActID,getSessionID(),payload), true);			
			if ((systemResponses!=null) && !systemResponses.isEmpty()) {
				Set<String> stringSystemResponses=new HashSet<String>();
				for(Event se:systemResponses) {
					String sen=se.getName();
					if (!StringUtils.isEmptyString(sen)) stringSystemResponses.add(sen);
				}
				if (!stringSystemResponses.isEmpty()) mapping.put(speechActID, stringSystemResponses);
			}
		}
		return mapping;*/
	}
	
	@Override
	public List<NLUOutput> getHandledUserEventsInCurrentState(List<NLUOutput> userInputs) throws Exception {
		throw new Exception("unsupported");
	}

	
	@Override
	public RewardPolicy parseDialoguePolicy(String policyURL) throws Exception {
		RewardPolicy p=new RewardPolicy(getConfiguration()).parseDialoguePolicyFile(policyURL);
		return p;
	}
	@Override
	public void validatePolicy(NLBusBase nlModule) throws Exception {
		dp.validate(getSessionID(),nlModule);
	}
	
	@Override
	public RewardDM createPolicyDM(Object preparsedDialoguePolicy,Long sid,NLBusInterface listener) throws Exception {
		RewardDM dm;
		RewardPolicy dp=(RewardPolicy)preparsedDialoguePolicy;
		dm = new RewardDM(sid,dp, getConfiguration(),listener);
		return dm;
	}
	
	@Override
	public boolean isSessionDone() {return done;}

	@Override
	public Set<String> getIDActiveStates() throws Exception {
		throw new Exception("unsupported operation");
	}
	@Override
	public void resetActiveStatesTo(Set<String> activeStateIds) throws Exception {
		throw new Exception("unsupported operation");
	}
	
	@Override
	public List<DMSpeakEvent> getAllPossibleSystemLines() throws Exception {
		RewardPolicy p=getPolicy();
		if (p!=null) return p.getAllPossibleSystemLines();
		return super.getAllPossibleSystemLines();
	}
	@Override
	public void addOperator(String xml) throws Exception {
		DialogueOperator o=new DialogueOperator().parse(xml);
		RewardPolicy dp=getPolicy();
		if (dp!=null) {
			DialogueOperator oo=dp.getOperatorNamed(o.getName(), OpType.ALL);
			if (oo!=null) dp.removeOperator(o.getName());
			dp.addOperator(o);
		} else {
			logger.warn("Ignoring adding operator as policy is null.");
		}
	}
	@Override
	public void removeOperator(String name) throws Exception {
		RewardPolicy dp=getPolicy();
		if (dp!=null) {
			DialogueOperator o=dp.getOperatorNamed(name, OpType.ALL);
			if (o!=null) dp.removeOperator(name);
			else {
				logger.warn("Ignoring removing operator named '"+name+"' as it doesn't exist.");
			}
		} else {
			logger.warn("Ignoring removing operator as policy is null.");
		}
	}
	public TimemarksTracker getTimemarkTracker() {
		return this.timemarkTracker;
	}
}
