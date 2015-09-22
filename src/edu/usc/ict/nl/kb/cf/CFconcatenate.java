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
import edu.usc.ict.nl.util.StringUtils;

public class CFconcatenate implements CustomFunctionInterface {

	private static final String getNameFromClass() {
		return CFconcatenate.class.getSimpleName().toLowerCase().substring(2);
	}
	private static final String name=getNameFromClass();

	@Override
	public String getName() {return name;}

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args!=null && args.size()>=1);
	}

	@Override
	public String eval(DialogueKBFormula f, DialogueKBInterface is,boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		List<DialogueKBFormula> args=f.getAllArgs();
		if (args!=null) {
			StringBuffer out=null;
			for(DialogueKBFormula a:args) {
				Object ea=is.evaluate(a,context);
				String eaString=null;
				if (ea!=null) {
					if (ea instanceof String) eaString=DialogueKBFormula.getStringValue((String) ea);
					else eaString=ea.toString();
				}
				if (!StringUtils.isEmptyString(eaString)) {
					if (out==null) out=new StringBuffer();
					out.append(eaString);
				}
			}
			return out!=null?out.toString():null;
		}
		return null;
	}


	@Override
	public boolean test() throws Exception {
		TestRewardDM dm=new TestRewardDM(NLBusConfig.WIN_EXE_CONFIG);
		DialogueKB is=dm.getInformationState();
		is.store(DialogueOperatorEffect.parse("assign(a,11)"), ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		String r=eval(DialogueKBFormula.parse(getName()+"(a,12)"),is,false,null);
		System.out.println(r);
		r=eval(DialogueKBFormula.parse(getName()+"(a,' how are you ',a)"),is,false,null);
		System.out.println(r);
		r=eval(DialogueKBFormula.parse(getName()+"(-1)"),is,false,null);
		System.out.println(r);
		r=eval(DialogueKBFormula.parse(getName()+"(-1,+(a,1))"),is,false,null);
		System.out.println(r);
		r=eval(DialogueKBFormula.parse(getName()+"(14,+(a,1))"),is,false,null);
		System.out.println(r);
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFconcatenate f = new CFconcatenate();
		if (!f.test()) throw new Exception("failed test");
	}
}
