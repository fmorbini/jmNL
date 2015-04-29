package edu.usc.ict.nl.kb.cf;

import java.util.Collection;
import java.util.List;

import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;

public class CFMin implements CustomFunctionInterface {

	private static final String name="min".toLowerCase();

	@Override
	public String getName() {return name;}

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args!=null && args.size()>=1);
	}

	@Override
	public Number eval(DialogueKBFormula f, DialogueKBInterface is,boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		List<DialogueKBFormula> args=f.getAllArgs();
		if (args!=null) {
			Float min=null;
			for(DialogueKBFormula a:args) {
				Object ea=is.evaluate(a,context);
				if (ea!=null && ea instanceof Number) {
					Float cea=((Number)ea).floatValue();
					if (min==null || cea<min) min=cea;
				} else {
					return null;
				}
			}
			return min;
		}
		return null;
	}


	@Override
	public boolean test() throws Exception {
		TestRewardDM dm=new TestRewardDM(NLBusConfig.WIN_EXE_CONFIG);
		DialogueKB is=dm.getInformationState();
		is.store(DialogueOperatorEffect.parse("assign(a,11)"), ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		Number r=eval(DialogueKBFormula.parse(getName()+"(a,12)"),is,false,null);
		if (!r.equals(11f)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"(a,a,a)"),is,false,null);
		if (!r.equals(11f)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"(-1)"),is,false,null);
		if (!r.equals(-1f)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"(-1,+(a,1))"),is,false,null);
		if (!r.equals(-1f)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"(14,+(a,1))"),is,false,null);
		if (!r.equals(12f)) return false;
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFMin f = new CFMin();
		if (!f.test()) throw new Exception("failed test");
	}
}
