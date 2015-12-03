package edu.usc.ict.nl.kb.cf;

import java.util.Collection;

import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.config.DMConfig;
import edu.usc.ict.nl.dm.reward.RewardDM;
import edu.usc.ict.nl.dm.reward.SpeakingTracker;
import edu.usc.ict.nl.dm.reward.model.DialogueAction;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorNodeTransition;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;

public class CFisInterruptible implements CustomFunctionInterface {

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args==null || (args!=null && args.size()==0));
	}

	@Override
	public Boolean eval(DialogueKBFormula f, DialogueKBInterface is,
			boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		RewardDM dm=(RewardDM) is.getValueOfVariable(NLBusBase.dmVariableName,ACCESSTYPE.AUTO_OVERWRITEAUTO,context);
		SpeakingTracker st = dm.getSpeakingTracker();
		DialogueOperatorNodeTransition tr = st.getWaitingTransition();
		if (tr!=null) return tr.isInterruptible();
		return false;
	}
	
	private static final String name="isInterruptible".toLowerCase();
	@Override
	public String getName() {return name;}
	
	@Override
	public boolean test() throws Exception {
		TestRewardDM dm=new TestRewardDM(DMConfig.WIN_EXE_CONFIG);
		TestDialogueOperator op = (TestDialogueOperator) dm.getCurrentActiveAction().getOperator();
		DialogueOperatorNodeTransition tr=new DialogueOperatorNodeTransition();
		tr.setEvent("test", null);
		SpeakingTracker tracker = dm.getSpeakingTracker();
		DialogueAction aa = dm.getCurrentActiveAction();
		tracker.setSpeakingTransition(aa, tr, "test.sa",null);
		Boolean r=eval(DialogueKBFormula.parse(getName()+"()"),dm.getInformationState(),false,null);
		if (!r.equals(false)) return false;
		tr.setInterruptible(true);
		r=eval(DialogueKBFormula.parse(getName()+"()"),dm.getInformationState(),false,null);
		if (!r.equals(true)) return false;
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFisInterruptible f = new CFisInterruptible();
		if (!f.test()) throw new Exception("failed test");
	}
}
