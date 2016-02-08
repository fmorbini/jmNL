package edu.usc.ict.nl.kb.cf;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import edu.usc.ict.nl.bus.events.NLUEvent;
import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.DMConfig;
import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.ne.BasicNE;
import edu.usc.ict.nl.nlu.ne.NE;
import edu.usc.ict.nl.nlu.ne.Numbers;

/**
 * 
 * @author morbini
 *	this custom function requires 2 arguments: the nlu interpretation, the nlu idenditier and the length of the history to inspect.
 */
public class CFset implements CustomFunctionInterface {

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args!=null && (args.size()==3));
	}

	@Override
	public Object eval(DialogueKBFormula f, DialogueKBInterface is,
			boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		DialogueKBFormula arg1 = (DialogueKBFormula) f.getArg(1);
		DialogueKBFormula arg2 = (DialogueKBFormula) f.getArg(2);
		DialogueKBFormula arg3 = (DialogueKBFormula) f.getArg(3);
		Object listArg=is.evaluate(arg1,context);
		Object indexArg=is.evaluate(arg2,context);
		Object setArg=is.evaluate(arg3,context);
		
		if (listArg!=null) {
			if (listArg instanceof List && indexArg!=null && indexArg instanceof Number) {
				int index=((Number) indexArg).intValue();
				List list=(List)listArg;
				if (index>=0 && index<list.size()) {
					return list.set(index,setArg);
				}
			} else if (listArg!=null && listArg instanceof Map) {
				return ((Map)listArg).put(indexArg,setArg);
			}
		}
		return null;
	}
	
	private static final String getNameFromClass() {
		return CFset.class.getSimpleName().toLowerCase().substring(2);
	}
	private static final String name=getNameFromClass();
	@Override
	public String getName() {return name;}
	
	@Override
	public boolean test() throws Exception {
		TestRewardDM dm=new TestRewardDM(DMConfig.WIN_EXE_CONFIG);
		NLU nlu=NLU.init("openNLU");
		nlu.getConfiguration().setForcedNLUContentRoot("resources\\characters\\common\\nlu");
		Numbers ne = new Numbers("test");
		ne.setConfiguration(nlu.getConfiguration());
		String string="i want 18 and twenty four bananas with 4 more and thirty.";
		List<NE> nes = ne.extractNamedEntitiesFromText(string,PreprocessingType.RUN);
		Map<String, Object> x = BasicNE.createPayload(nes);
		dm.updateISwithNLUvariablesFromEvent(dm.getRootInformationState(),new NLUEvent(new NLUOutput(string, "test", 1, x), 0));
		Object r=dm.getInformationState().evaluate(DialogueKBFormula.parse("==("+getName()+"(allnums,1),24)"),null);
		return (Boolean) r;
	}
	
	public static void main(String[] args) throws Exception {
		CFset f = new CFset();
		if (!f.test()) throw new Exception("failed test");
	}
}
