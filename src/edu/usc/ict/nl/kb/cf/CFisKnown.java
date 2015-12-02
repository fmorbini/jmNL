package edu.usc.ict.nl.kb.cf;

import java.util.Collection;

import edu.usc.ict.nl.config.DMConfig;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;

public class CFisKnown implements CustomFunctionInterface {

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args!=null && args.size()==1);
	}

	@Override
	public Boolean eval(DialogueKBFormula f, DialogueKBInterface is,
			boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		// returns true is the argument has a value different from NULL, otherwise returns false.
		DialogueKBFormula arg = (DialogueKBFormula) f.getFirstChild();
		Object result=is.evaluate(arg,context);
		if (result!=null) return true;
		else return false;
	}

	private static final String name="known".toLowerCase();
	@Override
	public String getName() {return name;}
	
	@Override
	public boolean test() throws Exception {
		TestRewardDM dm=new TestRewardDM(DMConfig.WIN_EXE_CONFIG);
		DialogueKB is=dm.getInformationState();
		is.store(DialogueOperatorEffect.parse("assign(a,2)"), ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		Boolean r=eval(DialogueKBFormula.parse(getName()+"(a)"),is,false,null);
		if (!r.equals(true)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"(b)"),is,false,null);
		if (!r.equals(false)) return false;
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFisKnown f = new CFisKnown();
		if (!f.test()) throw new Exception("failed test");
	}
}
