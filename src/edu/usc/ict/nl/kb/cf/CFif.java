package edu.usc.ict.nl.kb.cf;

import java.util.Collection;

import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;

/**
 * 
 * @author morbini
 *	this custom function requires 2 arguments: the nlu interpretation, the nlu idenditier and the length of the history to inspect.
 */
public class CFif implements CustomFunctionInterface {

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args!=null && (args.size()==3));
	}

	@Override
	public Object eval(DialogueKBFormula f, DialogueKBInterface is,
			boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		DialogueKBFormula argCnd = (DialogueKBFormula) f.getArg(1);
		DialogueKBFormula argThen = (DialogueKBFormula) f.getArg(2);
		DialogueKBFormula argElse = (DialogueKBFormula) f.getArg(3);
		Boolean cnd=(Boolean)is.evaluate(argCnd,context);
		if (cnd!=null) {
			DialogueKBFormula r=(cnd)?argThen:argElse;
			return is.evaluate(r, context);
		}
	return null;
	}
	
	private static final String name="if".toLowerCase();
	@Override
	public String getName() {return name;}
	
	@Override
	public boolean test() throws Exception {
		TestRewardDM dm=new TestRewardDM(NLBusConfig.WIN_EXE_CONFIG);
		DialogueKB is=dm.getInformationState();
		is.store(DialogueOperatorEffect.parse("assign(a,2)"), ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		Object r=eval(DialogueKBFormula.parse(getName()+"(a!=0,1,2)"),dm.getInformationState(),false,null);
		if (!r.equals(1f)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"(a>2,1,2)"),dm.getInformationState(),false,null);
		if (!r.equals(2f)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"(b>2,1,2)"),dm.getInformationState(),false,null);
		if (r!=null) return false;
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFif f = new CFif();
		if (!f.test()) throw new Exception("failed test");
	}
}
