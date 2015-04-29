package edu.usc.ict.nl.dm.reward;

import edu.usc.ict.nl.dm.reward.RewardDM.SearchTermination;

public class FoundDialogueOperatorEntranceTransition extends
		WeightedDialogueOperatorEntranceTransition {
	
	SearchTermination mode=null;
	
	public FoundDialogueOperatorEntranceTransition(WeightedDialogueOperatorEntranceTransition tr,SearchTermination mode) {
		super(tr.ec, tr.expectedReward);
		this.mode=mode;
	}
	
	public SearchTermination getSearchTermination() {
		return mode;
	}
	
	@Override
	public String toString() {
		return "Found entrance condition: <"+ec+"> with mode: "+mode;
	}
}
