package edu.usc.ict.nl.kb.cf;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.bus.events.Event;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.dm.reward.DormantActions;
import edu.usc.ict.nl.dm.reward.RewardDM;
import edu.usc.ict.nl.dm.reward.SpeakingTracker;
import edu.usc.ict.nl.dm.reward.model.DialogueAction;
import edu.usc.ict.nl.dm.reward.model.DialogueOperator;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEntranceTransition;
import edu.usc.ict.nl.dm.reward.model.RewardPolicy;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.kb.TrivialDialogueKB;

public class TestRewardDM extends RewardDM {

	DialogueOperator op=null;
	DialogueOperatorEntranceTransition ec=null;
	DialogueAction aa=null;
	
	public TestRewardDM(NLBusConfig config) throws Exception {
		super(config);
		logger=Logger.getLogger(TestRewardDM.class.getName());
		logger.setLevel(Level.OFF);
		context=new EvalContext(new TrivialDialogueKB(this));
		op=new TestDialogueOperator();
		ec=new DialogueOperatorEntranceTransition();
		ec.setOperator(op);
		aa = new DialogueAction(ec, this);
		dp=new TestRewardPolicy(config);
		dormantActions=new DormantActions();
		speakingTracker=new SpeakingTracker(this,null);
	
		try {
			getRootInformationState().setValueOfVariable(NLBusBase.dmVariableName, this,ACCESSTYPE.AUTO_OVERWRITEAUTO);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public DialogueKB getInformationState() {return getRootInformationState();}

	@Override
	public DialogueAction getCurrentActiveAction() throws Exception {
		return aa;
	}
	
	@Override
	public RewardPolicy getPolicy() {
		return dp;
	}
	
	@Override
	public void updateUserEventsHistory(Event ev) throws Exception {
		super.updateUserEventsHistory(ev);
	}
}
