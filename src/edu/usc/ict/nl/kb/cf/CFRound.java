package edu.usc.ict.nl.kb.cf;

import java.util.Collection;

import edu.usc.ict.nl.config.DMConfig;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;

public class CFRound implements CustomFunctionInterface {

	private static final String getNameFromClass() {
		return CFRound.class.getSimpleName().toLowerCase().substring(2);
	}
	private static final String name=getNameFromClass();
	@Override
	public String getName() {return name;}

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args!=null && args.size()==1);
	}

	@Override
	public Number eval(DialogueKBFormula f, DialogueKBInterface is,boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		DialogueKBFormula arg1=f.getArg(1);
		if (arg1!=null) {
			Object ea=is.evaluate(arg1,context);
			if (ea!=null && ea instanceof Number) {
				Float cea=((Number)ea).floatValue();
				return Math.round(cea);
			}
		}
		return null;
	}


	@Override
	public boolean test() throws Exception {
		TestRewardDM dm=new TestRewardDM(DMConfig.WIN_EXE_CONFIG);
		DialogueKB is=dm.getInformationState();
		is.store(DialogueOperatorEffect.parse("assign(a,11)"), ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		Number r=eval(DialogueKBFormula.parse(getName()+"(a)"),is,false,null);
		if (!r.equals(11)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"(2.3)"),is,false,null);
		if (!r.equals(2)) return false;
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFRound f = new CFRound();
		if (!f.test()) throw new Exception("failed test");
	}
}
