package edu.usc.ict.nl.kb.cf;

import java.util.Collection;
import java.util.HashMap;

import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;

/**
 * 
 * @author morbini
 *	this custom function requires 2 arguments: the nlu interpretation, the nlu idenditier and the length of the history to inspect.
 */
public class CFnewMap implements CustomFunctionInterface {

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args==null || (args!=null && args.size()==0));
	}

	@Override
	public Object eval(DialogueKBFormula f, DialogueKBInterface is,
			boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		return new HashMap<>();
	}
	
	private static final String getNameFromClass() {
		return CFnewMap.class.getSimpleName().toLowerCase().substring(2);
	}
	private static final String name=getNameFromClass();
	@Override
	public String getName() {return name;}
	
	@Override
	public boolean test() throws Exception {
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFnewMap f = new CFnewMap();
		if (!f.test()) throw new Exception("failed test");
	}
}
