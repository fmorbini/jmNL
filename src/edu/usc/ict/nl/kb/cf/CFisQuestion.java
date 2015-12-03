package edu.usc.ict.nl.kb.cf;

import java.util.Collection;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.DMConfig;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.util.StringUtils;

public class CFisQuestion implements CustomFunctionInterface {

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
		String argName=null;
		if (result instanceof String) argName=DialogueKBFormula.getStringValue((String) result);
		if (!StringUtils.isEmptyString(argName)) {
			return NLU.isQuestion(argName);
		} else return false;
	}

	private static final String name="isQuestion".toLowerCase();
	@Override
	public String getName() {return name;}
	
	@Override
	public boolean test() throws Exception {
		TestRewardDM dm=new TestRewardDM(DMConfig.WIN_EXE_CONFIG);
		DialogueKB is=dm.getInformationState();
		is.store(DialogueOperatorEffect.parse("assign(a,'question.a')"), ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		is.store(DialogueOperatorEffect.parse("assign(b,'otherwise')"), ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		Boolean r=eval(DialogueKBFormula.parse(getName()+"(a)"),is,false,null);
		if (!r.equals(true)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"('question.b')"),is,false,null);
		if (!r.equals(true)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"('somethingelse')"),is,false,null);
		if (!r.equals(false)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"(b)"),is,false,null);
		if (!r.equals(false)) return false;
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFisQuestion f = new CFisQuestion();
		if (!f.test()) throw new Exception("failed test");
	}
}
