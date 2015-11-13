package edu.usc.ict.nl.kb.cf;

import java.util.Collection;

import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.util.StringUtils;

public class CFmatch implements CustomFunctionInterface {

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args!=null && args.size()==2);
	}

	@Override
	public Boolean eval(DialogueKBFormula f, DialogueKBInterface is,
			boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		// returns true is the argument has a value different from NULL, otherwise returns false.
		DialogueKBFormula eventArg1 = (DialogueKBFormula) f.getArg(1);
		DialogueKBFormula reArg2 = (DialogueKBFormula) f.getArg(2);
		Object event=is.evaluate(eventArg1,context);
		Object re=is.evaluate(reArg2,context);
		String name=(event!=null)?event.toString():null;
		if ((name!=null) && (re instanceof String)) {
			name=DialogueKBFormula.getStringValue((String)name);
			re=DialogueKBFormula.getStringValue((String)re);
			if (name!=null && !StringUtils.isEmptyString((String) re)) {
				return ((String)name).matches((String)re);
			}
		}
		return false;
	}

	private static final String name="match".toLowerCase();
	@Override
	public String getName() {return name;}
	
	@Override
	public boolean test() throws Exception {
		TestRewardDM dm=new TestRewardDM(NLBusConfig.WIN_EXE_CONFIG);
		DialogueKB is=dm.getInformationState();
		is.store(DialogueOperatorEffect.parse("assign(a,'11a2')"), ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		is.set("b",DialogueKBFormula.generateStringConstantFromContent(this.getName()));
		is.set("c","''");
		Boolean r=eval(DialogueKBFormula.parse(getName()+"(a,'^[1-9]*a2$')"),dm.getInformationState(),false,null);
		if (!r.equals(true)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"(a,'^[1-9]*a3$')"),dm.getInformationState(),false,null);
		if (!r.equals(false)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"('1123','^[1-9]*$')"),dm.getInformationState(),false,null);
		if (!r.equals(true)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"(b,'^match')"),dm.getInformationState(),false,null);
		r=eval(DialogueKBFormula.parse(getName()+"(c,'^$')"),dm.getInformationState(),false,null);
		if (!r.equals(true)) return false;
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFmatch f = new CFmatch();
		if (!f.test()) throw new Exception("failed test");
	}
}
