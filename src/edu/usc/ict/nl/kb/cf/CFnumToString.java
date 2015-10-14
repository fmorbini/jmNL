package edu.usc.ict.nl.kb.cf;

import java.util.Collection;

import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.util.EnglishNumberToWords;
import edu.usc.ict.nl.util.NumberUtils;

public class CFnumToString implements CustomFunctionInterface {

	private static final String getNameFromClass() {
		return CFnumToString.class.getSimpleName().toLowerCase().substring(2);
	}
	private static final String name=getNameFromClass();
	@Override
	public String getName() {return name;}

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args!=null && args.size()==1);
	}

	@Override
	public String eval(DialogueKBFormula f, DialogueKBInterface is,boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		DialogueKBFormula arg1=f.getArg(1);
		DialogueKBFormula arg2 =f.getArg(2);
		Object ordinalArg=is.evaluate(arg2,context);
		boolean ordinal=false;
		if (ordinalArg!=null && ordinalArg instanceof Boolean) ordinal=(Boolean)ordinalArg;
		if (arg1!=null) {
			Object ea=is.evaluate(arg1,context);
			if (ea!=null && ea instanceof Number) {
				String l=NumberUtils.makeLongIfPossible(ea+"");
				if (l!=null) {
					Long v=Long.parseLong(l);
					String number=EnglishNumberToWords.convert(v);;
					if (ordinal) number=EnglishNumberToWords.toOrdinalForm(number);
					return number;
				} else {
					Float v=Float.parseFloat(ea+"");
					String number=EnglishNumberToWords.convert(Math.round(v));
					if (ordinal) number=EnglishNumberToWords.toOrdinalForm(number);
					return number;
				}
			}
		}
		return null;
	}


	@Override
	public boolean test() throws Exception {
		TestRewardDM dm=new TestRewardDM(NLBusConfig.WIN_EXE_CONFIG);
		DialogueKB is=dm.getInformationState();
		is.store(DialogueOperatorEffect.parse("assign(a,11)"), ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		String r=eval(DialogueKBFormula.parse(getName()+"(a)"),is,false,null);
		System.out.println(r);
		if (!r.equals("eleven")) return false;
		r=eval(DialogueKBFormula.parse(getName()+"(2.3)"),is,false,null);
		System.out.println(r);
		if (!r.equals("two")) return false;
		r=eval(DialogueKBFormula.parse(getName()+"(a,true)"),is,false,null);
		System.out.println(r);
		if (!r.equals("eleventh")) return false;
		r=eval(DialogueKBFormula.parse(getName()+"(2.3,true)"),is,false,null);
		System.out.println(r);
		if (!r.equals("second")) return false;
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFnumToString f = new CFnumToString();
		if (!f.test()) throw new Exception("failed test");
	}
}
