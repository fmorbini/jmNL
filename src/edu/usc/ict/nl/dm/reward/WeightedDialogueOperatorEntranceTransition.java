package edu.usc.ict.nl.dm.reward;

import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEntranceTransition;

public class WeightedDialogueOperatorEntranceTransition {
	
	Float expectedReward=null;
	DialogueOperatorEntranceTransition ec=null;
	
	public WeightedDialogueOperatorEntranceTransition() {
	}
	
	public WeightedDialogueOperatorEntranceTransition(DialogueOperatorEntranceTransition tr,Float reward) {
		this.ec=tr;
		this.expectedReward=reward;
	}
	
	public Float getExpectedReward() {
		return expectedReward;
	}
	
	public DialogueOperatorEntranceTransition getEntranceCondition() {
		return ec;
	}
	public void setEntranceCondition(DialogueOperatorEntranceTransition ec) {this.ec=ec;}
	
	@Override
	public String toString() {
		return "weigthed entrance condition: <"+ec+"> with expected reward: "+expectedReward;
	}
}
