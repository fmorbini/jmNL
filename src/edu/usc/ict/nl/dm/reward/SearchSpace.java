package edu.usc.ict.nl.dm.reward;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.bus.events.Event;
import edu.usc.ict.nl.dm.reward.RewardDM.TERMINATION_CAUSE;
import edu.usc.ict.nl.dm.reward.model.DialogueAction;
import edu.usc.ict.nl.dm.reward.model.DialogueOperator;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEntranceTransition;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorNode;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorNodesChain;
import edu.usc.ict.nl.dm.reward.model.RewardPolicy.OpType;
import edu.usc.ict.nl.dm.reward.possibilityGraph.PossibleIS;
import edu.usc.ict.nl.dm.reward.possibilityGraph.PossibleISForEvent;
import edu.usc.ict.nl.dm.reward.possibilityGraph.PossibleTransition;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.Rational;
import edu.usc.ict.nl.util.Triple;
import edu.usc.ict.nl.util.graph.GraphElement;

public class SearchSpace {
	public PossibleIS rootIgnore,rootKeep;
	public PossibleISForEvent[] rootHandle;
	private List<PossibleIS> possibilities;
	private Pair<TERMINATION_CAUSE, Integer> endState=null;
	private DialogueAction internalCurrentAction=null;
	private DormantActions internalDormantActions=null;
	private RewardDM dm=null;
	protected Logger logger = null;
	private boolean approximateForwardSearch=false;

	public SearchSpace(RewardDM dm, DialogueAction currentAction,Event... evs) throws Exception {
		this.dm=dm;
		this.logger=dm.getLogger();
		this.approximateForwardSearch=dm.getConfiguration().getApproximatedForwardSearch();
		internalDormantActions=dm.getDormantActions(OpType.NORMAL);
		internalCurrentAction=currentAction;

		rootIgnore=new PossibleIS(null,PossibleIS.Type.ROOT," IGNORE ",dm.getRootInformationState(),internalDormantActions);
		HashMap<String, PossibleIS> ccDB = new HashMap<String, PossibleIS>();
		rootIgnore.setContentCheckDB(ccDB);
		dm.getExecutedOperatorsHistory().addEdgeTo(rootIgnore, false, false);
		List<DialogueOperatorEntranceTransition> ecs=dm.getOperatorsThatCanBeStartedByThisEvent(rootIgnore,null,internalCurrentAction,OpType.NORMAL);
		possibilities = addAllTheseECsTo(ecs, rootIgnore, internalCurrentAction);


		rootKeep=null;
		if (internalCurrentAction!=null) {
			List<DialogueOperatorNodesChain> effectsSets = internalCurrentAction.getPossibleSetsOfEffects();
			DialogueOperator op=internalCurrentAction.getOperator();
			float remainingReward=dm.getRemainingRewardForSetOfEffects(effectsSets,op,internalCurrentAction.getInternalKB());
			boolean nonGoalEffectsRemaining=areThereNonGoalEffectsIn(effectsSets);
			logger.info("   KEEP: remaining reward: "+remainingReward+" has effects? "+nonGoalEffectsRemaining);
			if (nonGoalEffectsRemaining || remainingReward>0) {
				DialogueOperatorEntranceTransition fakeRootEntranceCondition=new DialogueOperatorEntranceTransition();fakeRootEntranceCondition.setOperator(internalCurrentAction.getOperator());
				rootKeep=new PossibleIS(null,PossibleIS.Type.ROOT," KEEP ",internalCurrentAction.getInternalKB(),internalDormantActions);
				PossibleIS rootKeepEC=new PossibleIS(op,PossibleIS.Type.EC,"continue: "+op+"\n"+FunctionalLibrary.printCollection(internalCurrentAction.getActiveStates()," [","] ",", "), internalCurrentAction.getInternalKB(), internalDormantActions);
				rootKeep.setContentCheckDB(ccDB);
				rootKeep.addEdgeTo(rootKeepEC, fakeRootEntranceCondition, null);
				List<PossibleIS> ssKeep = addAllThesePossibleEffectsSetsTo(effectsSets, fakeRootEntranceCondition, rootKeepEC);
				if (possibilities==null) possibilities = ssKeep;
				else if (ssKeep!=null) possibilities.addAll(ssKeep);
				dm.getExecutedOperatorsHistory().addEdgeTo(rootKeep, false, false);
			} else {
				logger.info("   KEEP: current action has no effects or rewards. Nothing to keep.");
			}
		}
		
		rootHandle=null;
		if (evs!=null && evs.length>0) {
			rootHandle=new PossibleISForEvent[evs.length];
			for (int i=0;i<evs.length;i++) {
				rootHandle[i]=new PossibleISForEvent(evs[i],null,PossibleIS.Type.ROOT," HANDLE ",dm.getRootInformationState(),internalDormantActions);
				rootHandle[i].setContentCheckDB(ccDB);
				dm.getExecutedOperatorsHistory().addEdgeTo(rootHandle[i], false, false);
				ecs=dm.getOperatorsThatCanBeStartedByThisEvent(rootHandle[i],evs[i],internalCurrentAction,OpType.NORMAL);
				List<PossibleIS> ssHandle = addAllTheseECsTo(ecs, rootHandle[i], internalCurrentAction);
				if (possibilities==null) possibilities = ssHandle;
				else if (ssHandle!=null) possibilities.addAll(ssHandle);
			}
		}

	}

	private boolean areThereNonGoalEffectsIn(List<DialogueOperatorNodesChain> effectsSets) {
		if (effectsSets!=null) {
			for(DialogueOperatorNodesChain chainEl:effectsSets) {
				if (!chainEl.isEmpty()) {
					for(Triple<GraphElement, Rational, List<DialogueOperatorEffect>> nodeEl:chainEl.getChain()) {
						List<DialogueOperatorEffect> effects=nodeEl.getThird();
						if (effects!=null) {
							for(DialogueOperatorEffect e:effects) {
								if (!e.isGoalAchievement()) return true;
							}
						}
					}
				}
			}
			return false;
		} else return false;
	}
	
	Pair<TERMINATION_CAUSE, Integer> runForwardSearch() throws Exception {
		this.endState=doForwardSearch(possibilities);
		return this.endState;
	}
	WeightedDialogueOperatorEntranceTransition computeRewardForIgnoringEvent() throws Exception {
		WeightedDialogueOperatorEntranceTransition ec = dm.getBestEntranceCondition4Operator(rootIgnore);
		if (logger.getLevel()==Level.DEBUG) rootIgnore.toGDLGraph("drop-ignore.gdl");
		if (logger.isInfoEnabled()) logger.info("   IGNORE: "+((endState!=null)?endState.getFirst():null)+" level reached: "+((endState!=null)?endState.getSecond():null)+" result: "+ec);
		return ec;
	}
	WeightedDialogueOperatorEntranceTransition[] computeRewardsForHandlingEvents() throws Exception {
		if (rootHandle!=null && rootHandle.length>0) {
			WeightedDialogueOperatorEntranceTransition[] ecs=new WeightedDialogueOperatorEntranceTransition[rootHandle.length];
			for(int i=0;i<rootHandle.length;i++) {
				ecs[i] = dm.getBestEntranceCondition4Operator(rootHandle[i]);
				Event ev=rootHandle[i].getInputEvent();
				
				if (logger.isDebugEnabled()) rootHandle[i].toGDLGraph("drop-handle-"+"(event_index="+i+")-"+ev.getName().replace('|', '_')+".gdl");
				if (logger.isInfoEnabled()) logger.info("   HANDLE[event_index="+i+"]("+ev.getName()+"): "+((endState!=null)?endState.getFirst():null)+" level reached: "+((endState!=null)?endState.getSecond():null)+" result: "+ecs[i]);
			}
			return ecs;
		} else {
			logger.info("   HANDLE: NO handlers");
		}
		return null;
	}
	WeightedDialogueOperatorEntranceTransition computeRewardForHandlingEvent() throws Exception {
		WeightedDialogueOperatorEntranceTransition[] ecs=computeRewardsForHandlingEvents();
		WeightedDialogueOperatorEntranceTransition ret=null;
		if (ecs!=null) {
			for(WeightedDialogueOperatorEntranceTransition ec:ecs) {
				if (ret==null || ret.getExpectedReward()<ec.getExpectedReward()) ret=ec;
			}
		}
		return ret;
	}
	WeightedDialogueOperatorEntranceTransition computeRewardForContinuingCurrentAction() throws Exception {
		if (rootKeep!=null) {
			WeightedDialogueOperatorEntranceTransition ec = dm.getBestEntranceCondition4Operator(rootKeep);
			// need to undu the step loss because there is no entrance condition here even if its placed there to build
			// a valid search space
			/*if (ec!=null) {
				Float reward=ec.getSecond();
				if (reward!=null) {
					float stepLoss=dp.getStepLoss();
					reward/=stepLoss;
					ec.setSecond(reward);
				}
			}*/
			if (logger.getLevel()==Level.DEBUG) rootKeep.toGDLGraph("continuing.gdl");
			logger.info("   KEEP: "+((endState!=null)?endState.getFirst():null)+" level reached: "+((endState!=null)?endState.getSecond():null)+" result: "+ec);
			return ec;
		} else {
			logger.info("   KEEP: nothing to keep.");
			return null;
		}
	}
	private List<PossibleIS> addAllTheseECsTo(List<DialogueOperatorEntranceTransition> ecs,PossibleIS rootPis,DialogueAction currentAction) throws Exception {
		List<PossibleIS> possibilities=null;
		if (ecs!=null) {
			possibilities=new ArrayList<PossibleIS>();
			for(DialogueOperatorEntranceTransition ec:ecs) {
				DialogueOperator op=ec.getOperator();
				DialogueOperatorNode state=ec.getTarget();
				
				DialogueKB rootIS=rootPis.getValue();
				PossibleIS newRootPis;
				DialogueKB newRootIS=rootIS;
				if (ec.isReEntrable()) {
					DormantActions currentDormantActions = rootPis.getDormantOperators();
					DialogueAction currentDormantAction=currentDormantActions.getDormantActionOf(ec.getOperator());
					assert(currentDormantAction!=null);
					DialogueKB dormantActionLocalVars=currentDormantAction.getLocalVarKB();
					if (dormantActionLocalVars!=null) {
						newRootIS = rootIS.storeAll(dormantActionLocalVars.dumpKB(),ACCESSTYPE.AUTO_NEW,true);
						if (newRootIS==null) newRootIS=rootIS;
					}
				}
				newRootPis=new PossibleIS(ec.getOperator(),PossibleIS.Type.EC,"for "+ec.getOperator().getName()+" ["+ec.toString(true)+"]", newRootIS, rootPis.getDormantOperators());
				
				newRootPis.removeDormantAction(ec.getOperator());
				newRootPis.addDormantAction(currentAction);
				rootPis.addEdgeTo(newRootPis,ec,null);

	
	
				List<DialogueOperatorNodesChain> effectsSets = op.getEffectsSetsForStartingState(state);
				if (approximateForwardSearch) {
					float reward = computeExpectedApxRewardForChains(effectsSets,rootIS);
					PossibleIS newPis=new PossibleIS(op,PossibleIS.Type.CHAIN," approximation ", rootIS, newRootPis.getDormantOperators());
					newPis.setChainReward(reward);
					newPis.setWeight(Rational.one);
					newRootPis.addEdgeTo(newPis, ec, null);
					possibilities.add(newPis);
				} else {
					if (effectsSets!=null) {
						for(DialogueOperatorNodesChain chain_effects:effectsSets) {
							PossibleIS newPis=newRootPis.simulateExecutionOfChain(ec,chain_effects);
							possibilities.add(newPis);
							newPis.addToRecord();
						}
					}
				}
			}
		}
		return possibilities;
	}

	private float computeExpectedApxRewardForChains(List<DialogueOperatorNodesChain> chains,DialogueKBInterface is) throws Exception {
		float reward=0;
		for(DialogueOperatorNodesChain chain:chains) {
			List<DialogueOperatorEffect> effects = chain.getCompressedEffects().getEffects();
			if (effects!=null && !effects.isEmpty()) {
				for(DialogueOperatorEffect eff:effects) {
					if (eff.isGoalAchievement()) {
						String goalName = DialogueOperatorEffect.buildVarNameForGoal(eff.getGoalName());
						Object val = is.evaluate(is.getValueOfVariable(goalName,ACCESSTYPE.AUTO_OVERWRITEAUTO,null),null);
						if (val!=null && val instanceof Float) {
							//float reward = eff.evaluateGoalValueIn(is);
							reward+=((Float)val)*chain.getWeight();
						}
					}
				}
			}
		}
		return reward;
	}

	private List<PossibleIS> addAllThesePossibleEffectsSetsTo(List<DialogueOperatorNodesChain> effectsSets,DialogueOperatorEntranceTransition fakeRootEntranceCondition,PossibleIS root) throws Exception {
		List<PossibleIS> possibilities=new ArrayList<PossibleIS>();
		
		if (approximateForwardSearch) {
			HashSet<DialogueOperatorEntranceTransition> allECS=new HashSet<DialogueOperatorEntranceTransition>();
			for (DialogueOperatorNodesChain chain:effectsSets) {
				PossibleIS newPis=root.simulateExecutionOfChain(fakeRootEntranceCondition,chain);
				List<DialogueOperatorEntranceTransition> ecs=dm.getOperatorsThatCanBeStartedByThisEvent(newPis,null,null,OpType.NORMAL);
				if (ecs!=null) allECS.addAll(ecs);
			}
			root.removeAllOutgoingEdges();
			float reward = computeExpectedApxRewardForChains(effectsSets,root.getValue());
			PossibleIS newPis=new PossibleIS(null,PossibleIS.Type.CHAIN," approximation ", root.getValue(), root.getDormantOperators());
			newPis.setChainReward(reward);
			newPis.setWeight(Rational.one);
			root.addEdgeTo(newPis, fakeRootEntranceCondition, null);
			for(DialogueOperatorEntranceTransition ec:allECS) {

				DialogueKB currentIS=root.getValue();
				PossibleIS newCurrentPis=new PossibleIS(ec.getOperator(),PossibleIS.Type.EC,"for "+ec.getOperator().getName()+" ["+ec.toString(true)+"]", currentIS, root.getDormantOperators());
				newCurrentPis.removeDormantAction(ec.getOperator());
				newPis.addEdgeTo(newCurrentPis, ec, null);

				DialogueOperator op=ec.getOperator();
				DialogueOperatorNode state=ec.getTarget();
				List<DialogueOperatorNodesChain> effectsSets2 = op.getEffectsSetsForStartingState(state);

				reward = computeExpectedApxRewardForChains(effectsSets2,newCurrentPis.getValue());
				PossibleIS newPis2=new PossibleIS(op,PossibleIS.Type.CHAIN," approximation ", newCurrentPis.getValue(), newCurrentPis.getDormantOperators());
				newPis2.setChainReward(reward);
				newPis2.setWeight(Rational.one);
				newCurrentPis.addEdgeTo(newPis2, ec, null);
				possibilities.add(newPis2);
			}
		} else {
			for(DialogueOperatorNodesChain chain_effects:effectsSets) {
				PossibleIS newPis=root.simulateExecutionOfChain(fakeRootEntranceCondition,chain_effects);
				possibilities.add(newPis);
				newPis.addToRecord();
			}
		}
		return possibilities;
	}
	private Pair<TERMINATION_CAUSE,Integer> doForwardSearch(List<PossibleIS> poss) throws Exception {
		TERMINATION_CAUSE endCause=TERMINATION_CAUSE.NORMAL;
		long startTime=System.currentTimeMillis();
		int levels=1;
		PossibleIS levelMarker=null;
		Queue<PossibleIS> possibilities=(poss!=null)?new LinkedList<PossibleIS>(poss):new LinkedList<PossibleIS>();

		while((endCause=dm.isTimeToEndSearch(possibilities,levels,startTime))==null) {
			PossibleIS currentPis=possibilities.poll();
			//System.out.println(currentPis);
			DialogueKBInterface is = currentPis.getValue();
			DialogueKBInterface newIS=is.store(DialogueOperatorEffect.createAssignment(DialogueKBFormula.create(NLBusBase.lastEventVariableName, null), null, false), ACCESSTYPE.AUTO_NEW, true);
			if (newIS!=null) currentPis.setValue(newIS);
			
			List<DialogueOperatorEntranceTransition> ecs;
			if (approximateForwardSearch) {
				PossibleTransition parentPT = currentPis.getSingleIncomingTransition();
				DialogueOperatorEntranceTransition parentEC = parentPT.getEntranceCondition();
				Set<DialogueOperatorEntranceTransition> rawECS = dm.getPolicy().getSetOfEntranceOptionsThatCanBeEnabledByHavingExecutedThisEntranceCondition(parentEC);
				Set<DialogueOperatorEntranceTransition> filteredECS=filterSetOfEntranceConditionsWithAncestors(rawECS,currentPis);
				ecs=(filteredECS!=null)?new ArrayList<DialogueOperatorEntranceTransition>(filteredECS):new ArrayList<DialogueOperatorEntranceTransition>();
			} else {
				ecs=dm.getOperatorsThatCanBeStartedByThisEvent(currentPis,null,null,OpType.NORMAL);
			}
			if (ecs!=null) {
				for(DialogueOperatorEntranceTransition ec:ecs) {

					DialogueKB currentIS=currentPis.getValue();
					PossibleIS newCurrentPis=new PossibleIS(ec.getOperator(),PossibleIS.Type.EC,"for "+ec.getOperator().getName()+" ["+ec.toString(true)+"]", currentIS, currentPis.getDormantOperators());
					newCurrentPis.removeDormantAction(ec.getOperator());
					currentPis.addEdgeTo(newCurrentPis, ec, null);

					DialogueOperator op=ec.getOperator();
					DialogueOperatorNode state=ec.getTarget();
					List<DialogueOperatorNodesChain> effectsSets = op.getEffectsSetsForStartingState(state);
					if (approximateForwardSearch) {
						float reward = computeExpectedApxRewardForChains(effectsSets,newCurrentPis.getValue());
						PossibleIS newPis=new PossibleIS(op,PossibleIS.Type.CHAIN," approximation ", newCurrentPis.getValue(), newCurrentPis.getDormantOperators());
						newPis.setChainReward(reward);
						newPis.setWeight(Rational.one);
						newCurrentPis.addEdgeTo(newPis, ec, null);
						possibilities.add(newPis);
					} else {
						for(DialogueOperatorNodesChain chain_effects:effectsSets) {
							PossibleIS newPis=newCurrentPis.simulateExecutionOfChain(ec,chain_effects);
							if (!newPis.tryToMergeStopIfLoop()) {
								possibilities.add(newPis);
								if (levelMarker==null) levelMarker=newPis;
							}
						}
					}
				}
			}
			if (possibilities.peek()==levelMarker) {
				levels++;
				levelMarker=null;
			}
		}
		return new Pair<TERMINATION_CAUSE, Integer>(endCause,levels);
	}
	
	private Set<DialogueOperatorEntranceTransition> filterSetOfEntranceConditionsWithAncestors(Set<DialogueOperatorEntranceTransition> rawECS,PossibleIS currentPis) throws Exception {
		if (rawECS!=null) {
			if (currentPis!=null) {
				HashSet<DialogueOperatorEntranceTransition> visitedECS=new HashSet<DialogueOperatorEntranceTransition>();
				PossibleTransition it=null;
				while ((it = currentPis.getSingleIncomingTransition())!=null) {
					DialogueOperatorEntranceTransition ec = it.getEntranceCondition();
					visitedECS.add(ec);
					currentPis=(PossibleIS) it.getSource();
				}
				HashSet<DialogueOperatorEntranceTransition>ret=new HashSet<DialogueOperatorEntranceTransition>(rawECS);
				ret.removeAll(visitedECS);
				return ret;
			} else return rawECS;
		} else return null;
	}
}
