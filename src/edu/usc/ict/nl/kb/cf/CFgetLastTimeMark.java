package edu.usc.ict.nl.kb.cf;

import java.util.Collection;

import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.config.DMConfig;
import edu.usc.ict.nl.dm.reward.RewardDM;
import edu.usc.ict.nl.dm.reward.model.DialogueAction;
import edu.usc.ict.nl.dm.reward.model.DialogueOperator;
import edu.usc.ict.nl.dm.reward.model.TimemarksTracker;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;

public class CFgetLastTimeMark implements CustomFunctionInterface {

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args!=null && (args.size()>=1 && args.size()<=2));
	}

	@Override
	public Long eval(DialogueKBFormula f, DialogueKBInterface is,
			boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		RewardDM dm=(RewardDM) is.getValueOfVariable(NLBusBase.dmVariableName,ACCESSTYPE.AUTO_OVERWRITEAUTO,context);
		DialogueKBFormula arg1 = (DialogueKBFormula) f.getArg(1);
		DialogueKBFormula arg2 = (DialogueKBFormula) f.getArg(2);
		Object typeArg=is.evaluate(arg1,context);
		Object parArg=is.evaluate(arg2,context);
		TimemarksTracker.TYPES type=null;
		String par=null;
		if (typeArg instanceof String) type=TimemarksTracker.TYPES.valueOf(DialogueKBFormula.getStringValue((String)typeArg));
		if (parArg instanceof String) par=DialogueKBFormula.getStringValue((String)parArg);
		if (dm!=null && type!=null && context!=null) {
			TimemarksTracker tt = dm.getTimemarkTracker();
			DialogueOperator op = context.getFormulaOperator();
			if (tt!=null && op!=null) {
				Long r=tt.getLastTimeMark(op.getName(), type, par);
				return r;
			}
		}
		return null;
	}
	
	private static final String getNameFromClass() {
		return CFgetLastTimeMark.class.getSimpleName().toLowerCase().substring(2);
	}
	private static final String name=getNameFromClass();
	@Override
	public String getName() {return name;}
	
	@Override
	public boolean test() throws Exception {
		TestRewardDM dm=new TestRewardDM(DMConfig.WIN_EXE_CONFIG);
		DialogueAction aa = dm.getCurrentActiveAction();
		TimemarksTracker tt = dm.getTimemarkTracker();
		tt.setMark(aa.getOperator().getName(), TimemarksTracker.TYPES.SAY, "test");
		Thread.sleep(200);
		tt.setMark(aa.getOperator().getName(), TimemarksTracker.TYPES.DONE,null);
		Thread.sleep(300);
		tt.setMark(aa.getOperator().getName(), TimemarksTracker.TYPES.ENTER,null);
		TestDialogueOperator op = (TestDialogueOperator) aa.getOperator();
		EvalContext context=new EvalContext();
		Long r=eval(DialogueKBFormula.parse(getName()+"('DONE')"),dm.getInformationState(),false,context);
		if (r!=null) return false;
		context.setFormulaOperator(op);
		r=eval(DialogueKBFormula.parse(getName()+"('SAY','test')"),dm.getInformationState(),false,context);
		if (r==null) return false;
		r=eval(DialogueKBFormula.parse(getName()+"('DONE')"),dm.getInformationState(),false,context);
		if (r==null) return false;
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFgetLastTimeMark f = new CFgetLastTimeMark();
		if (!f.test()) throw new Exception("failed test");
	}
}
