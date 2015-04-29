package edu.usc.ict.nl.dm.reward;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Set;

import edu.usc.ict.nl.dm.reward.model.DialogueAction;
import edu.usc.ict.nl.dm.reward.model.DialogueOperator;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEntranceTransition;

public class DormantActions {

	private LinkedHashMap<DialogueOperator,DialogueAction> dormantActions;
	private HashMap<DialogueOperator,Long> action2DormantSince;

	public DormantActions(DormantActions source) {
		if (source!=null) {
			if (source.dormantActions!=null) this.dormantActions=new LinkedHashMap<DialogueOperator, DialogueAction>(source.dormantActions);
			if (source.action2DormantSince!=null) this.action2DormantSince=new HashMap<DialogueOperator,Long>(source.action2DormantSince);
		}
		if (dormantActions==null) this.dormantActions=new LinkedHashMap<DialogueOperator, DialogueAction>();
		if (action2DormantSince==null) this.action2DormantSince=new HashMap<DialogueOperator,Long>();
	}
	public DormantActions() {
		this.dormantActions=new LinkedHashMap<DialogueOperator, DialogueAction>();
		this.action2DormantSince=new HashMap<DialogueOperator,Long>();
	}

	// get operators or actions
	public Set<DialogueOperator> getDormantOperators() {
		return dormantActions.keySet();
	}
	public Collection<DialogueAction> getDormantActions() {
		return dormantActions.values();
	}
	public DialogueAction getDormantActionOf(DialogueOperator op) {
		return dormantActions.get(op);
	}

	public boolean isThisDormant(DialogueOperator operator) {
		return dormantActions.containsKey(operator);
	}

	public void wakeUpThisOperator(DialogueOperatorEntranceTransition selectedEntranceCondition,DialogueAction currentAction,RewardDM dm) throws Exception {
		DialogueOperator selectedDormantOperator = selectedEntranceCondition.getOperator();
		DialogueAction selectedDormantAction=getDormantActionOf(selectedDormantOperator);
		removeDormantOperator(selectedDormantOperator,true);
		dm.getLogger().info("   selected to reactivate this dormant action: "+selectedDormantAction);
		selectedDormantAction.setAsReentered(selectedEntranceCondition);
		if (currentAction!=null) addAction(currentAction,SwapoutReason.SEARCH);
		dm.setActiveAction(selectedDormantAction);
	}
	
	// remove operator, add action
	public void removeDormantOperator(DialogueOperator op,boolean noStateChanges) {
		DialogueAction action=getDormantActionOf(op);
		if (action!=null) {
			resetDormantTimeOf(action);
			dormantActions.remove(op);
			action2DormantSince.remove(op);
			if (!noStateChanges) action.setAsInvalid("removed from dormant list (no re-activation)");
		}
	}
	private void resetDormantTimeOf(DialogueAction action) {
		if (action!=null) {
			DialogueOperator op=action.getOperator();
			action2DormantSince.put(op,-1l);
		}
	}
	private void setDormantTimeOf(DialogueAction action) {
		if (action!=null) {
			DialogueOperator op=action.getOperator();
			action2DormantSince.put(op,System.currentTimeMillis());
		}
	}
	public long getDormantTimeOf(DialogueAction action) {
		if (action!=null) {
			DialogueOperator op=action.getOperator();
			return action2DormantSince.get(op);
		}
		return -1;
	}
	public boolean shouldThisActionBeForgotten(DialogueAction action) {
		if (action!=null) {
			long dormantSinceTime=getDormantTimeOf(action);
			if (dormantSinceTime>0) {
				DialogueOperator op = action.getOperator();
				long deltaTime=System.currentTimeMillis()-dormantSinceTime;
				return op.shouldItBeForgotten(deltaTime,action.getInternalKB());
			}
		}
		return false;
	}

	public void addAction(DialogueAction action,SwapoutReason reason) {
		action.setSwapoutReason(reason);
		dormantActions.put(action.getOperator(), action);
		setDormantTimeOf(action);
	}

	public boolean isEmpty() {
		return dormantActions==null || dormantActions.isEmpty();
	}
	
	@Override
	public String toString() {
		return dormantActions.toString();
	}
}

