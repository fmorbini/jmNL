package edu.usc.ict.nl.kb.cf;

import java.util.Collection;

import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;

/**
 * 
 * @author morbini
 *	this custom function requires 2 arguments: the nlu interpretation, the nlu idenditier and the length of the history to inspect.
 */
public class CFintersect implements CustomFunctionInterface {

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
		Object listArg2=is.evaluate(arg2,context);
		
		if (listArg==null) return null;
		else if (listArg instanceof Collection) {
			if (listArg2==null) return listArg;
			else if (listArg2 instanceof Collection) {
				if (((Collection) listArg2).isEmpty()) return listArg;
				else {
					if (((Collection) listArg).isEmpty()) return listArg;
					else {
						Collection copy=((Collection)listArg).getClass().getConstructor().newInstance();
						copy.addAll((Collection)listArg);
						copy.retainAll((Collection)listArg2);
						return copy;
					}
				}
			}
		}
		return null;
	}
	
	private static final String getNameFromClass() {
		return CFintersect.class.getSimpleName().toLowerCase().substring(2);
	}
	private static final String name=getNameFromClass();
	@Override
	public String getName() {return name;}
	
	@Override
	public boolean test() throws Exception {
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFintersect f = new CFintersect();
		if (!f.test()) throw new Exception("failed test");
	}
}
