package edu.usc.ict.nl.dm.reward;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import edu.usc.ict.nl.dm.reward.RewardDM.SearchTermination;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEntranceTransition;
import edu.usc.ict.nl.util.NumberUtils;

class NextActionSelector {
	
	/**
	 * 
	 */
	private final RewardDM rewardDM;

	private List<FoundDialogueOperatorEntranceTransition> history=new ArrayList<FoundDialogueOperatorEntranceTransition>();
	
	private FoundDialogueOperatorEntranceTransition best=null;
	private Logger logger;
	
	public NextActionSelector(RewardDM rewardDM) {
		this.rewardDM = rewardDM;
		best=null;
		logger=rewardDM.getLogger();
	}
	public boolean handlerOptionAvailableButIgnored() {
		FoundDialogueOperatorEntranceTransition br = getBest();
		if (br!=null) {
			SearchTermination brMode=br.getSearchTermination();
			if (rewardDM.isIgnoreCurrentEvent(brMode)) {
				FoundDialogueOperatorEntranceTransition brHandler = getBestAmong(RewardDM.HANDLER_TERMINATION_METHODS);
				if (brHandler!=null) return true;
			}
		}
		return false;
	}
	public void updateBestWith(WeightedDialogueOperatorEntranceTransition w,SearchTermination mode) throws Exception {
		if (w!=null) {
			Float er=w.getExpectedReward();
			if(logger.isDebugEnabled()) logger.debug("     update best: "+mode+" "+er);
			if (mode!=null && er!=null) {
				FoundDialogueOperatorEntranceTransition input=new FoundDialogueOperatorEntranceTransition(w, mode);
				history.add(input);
				if ((this.best==null) || (er>best.getExpectedReward()) || (NumberUtils.roughlyEqualAbsolute(er, best.getExpectedReward(), 0.01) && rewardDM.isHandleMode(mode) && rewardDM.getConfiguration().getPreferUserInitiatedActions())) {
					best=input;
				}
			}
		}
	}
	public FoundDialogueOperatorEntranceTransition getBestAmong(Set<SearchTermination> modes) {
		if ((modes==null) || modes.isEmpty()) return null;
		else {
			FoundDialogueOperatorEntranceTransition bestHandler=null;
			for(FoundDialogueOperatorEntranceTransition c:history) {
				if (c!=null && modes.contains(c.getSearchTermination())) {
					DialogueOperatorEntranceTransition hec = c.getEntranceCondition();
					Float hrew=c.getExpectedReward();
					if (hec!=null && (bestHandler==null || hrew>bestHandler.getExpectedReward())) {
						bestHandler=c;
					}
				}
			}
			return bestHandler;
		}
	}
	public FoundDialogueOperatorEntranceTransition getBest() {
		return best;
	}
	public void setBest(FoundDialogueOperatorEntranceTransition best) {
		this.best = best;
	}
	public Float getBestReward() {return (best!=null)?best.getExpectedReward():null;}
}