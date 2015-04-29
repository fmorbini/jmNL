package edu.usc.ict.nl.kb.cf;

import java.util.Collection;

import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.dm.reward.DormantActions;
import edu.usc.ict.nl.dm.reward.RewardDM;
import edu.usc.ict.nl.dm.reward.SwapoutReason;
import edu.usc.ict.nl.dm.reward.model.DialogueAction;
import edu.usc.ict.nl.dm.reward.model.DialogueOperator;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorNode;
import edu.usc.ict.nl.dm.reward.model.RewardPolicy.OpType;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;

public class CFhasBeenInterrupted implements CustomFunctionInterface {

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args==null || (args!=null && args.size()==0));
	}

	@Override
	public Boolean eval(DialogueKBFormula f, DialogueKBInterface is,
			boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		// returns true is the argument has a value different from NULL, otherwise returns false.
		RewardDM dm=(RewardDM) is.getValueOfVariable(NLBusBase.dmVariableName,ACCESSTYPE.AUTO_OVERWRITEAUTO,context);
		DormantActions das=(dm!=null)?dm.getDormantActions(OpType.NORMAL):null;
		DialogueOperator op=(context!=null)?context.getFormulaOperator():null;
		if (das!=null && op!=null) {
			DialogueAction da = das.getDormantActionOf(op);
			SwapoutReason reason = da.getSwapoutReason();
			if (reason!=null) return reason.isInterruptionReason();
		}
		return false;
	}
	
	private static final String name="hasBeenInterrupted".toLowerCase();
	@Override
	public String getName() {return name;}
	
	@Override
	public boolean test() throws Exception {
		TestRewardDM dm=new TestRewardDM(NLBusConfig.WIN_EXE_CONFIG);
		DialogueAction aa = dm.getCurrentActiveAction();
		TestDialogueOperator op = (TestDialogueOperator) aa.getOperator();
		DialogueOperatorNode.doSwapOut(aa, new SwapoutReason(aa.getEntranceTransition()));
		EvalContext context=new EvalContext();
		Boolean r=eval(DialogueKBFormula.parse(getName()+"()"),dm.getInformationState(),false,context);
		if (!r.equals(false)) return false;
		context.setFormulaOperator(op);
		r=eval(DialogueKBFormula.parse(getName()+"()"),dm.getInformationState(),false,context);
		if (!r.equals(true)) return false;
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFhasBeenInterrupted f = new CFhasBeenInterrupted();
		if (!f.test()) throw new Exception("failed test");
	}
}
