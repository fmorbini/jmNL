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
public class CFget implements CustomFunctionInterface {

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args!=null && (args.size()==2));
	}

	@Override
	public Object eval(DialogueKBFormula f, DialogueKBInterface is,
			boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		DialogueKBFormula arg1 = (DialogueKBFormula) f.getArg(1);
		DialogueKBFormula arg2 = (DialogueKBFormula) f.getArg(2);
		Object listArg=is.evaluate(arg1,context);
		Object indexArg=is.evaluate(arg2,context);
		
		if (listArg!=null) {
			if (listArg instanceof List && indexArg!=null) {
				List list=(List)listArg;
				if (!list.isEmpty()) {
					int index=0;
					if (indexArg instanceof Number) {
						index=((Number) indexArg).intValue();
					} else if (indexArg instanceof String) {
						index=CFRandom.rng.nextInt(list.size());
					}
					if (index>=0 && index<list.size()) {
						return list.get(index);
					}
				}
			} else if (listArg instanceof Map) {
				if (!((Map)listArg).isEmpty()) {
					return ((Map)listArg).get(indexArg);
				}
			}
		}
		return null;
	}
	
	private static final String getNameFromClass() {
		return CFget.class.getSimpleName().toLowerCase().substring(2);
	}
	private static final String name=getNameFromClass();
	@Override
	public String getName() {return name;}
	
	@Override
	public boolean test() throws Exception {
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFget f = new CFget();
		if (!f.test()) throw new Exception("failed test");
	}
}
