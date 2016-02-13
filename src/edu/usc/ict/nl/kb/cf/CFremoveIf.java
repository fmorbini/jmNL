package edu.usc.ict.nl.kb.cf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.TrivialDialogueKB;

/**
 * 
 * @author morbini
 *	this custom function requires 2 arguments: the nlu interpretation, the nlu idenditier and the length of the history to inspect.
 */
public class CFremoveIf implements CustomFunctionInterface {

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args!=null && (args.size()==3));
	}

	@Override
	public Collection eval(DialogueKBFormula f, DialogueKBInterface is,
			boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		DialogueKBFormula arg1 = (DialogueKBFormula) f.getArg(1);
		DialogueKBFormula arg2 = (DialogueKBFormula) f.getArg(2);
		DialogueKBFormula arg3 = (DialogueKBFormula) f.getArg(3);
		Object collectionArg=is.evaluate(arg2,context);
		List ret=null;
		if (collectionArg!=null && collectionArg instanceof Collection) {
			TrivialDialogueKB internalKB=new TrivialDialogueKB((DialogueKB)is);
			ret=new ArrayList<>();
			for(Object thing:(Collection)collectionArg) {
				internalKB.set(arg1.getName(), thing);
				Object predicateArg=internalKB.evaluate(arg3,context);
				if (predicateArg==null || !(predicateArg instanceof Boolean) || !((Boolean)predicateArg)) {
					ret.add(thing);
				}
			}
		}

		return ret;
	}
	
	private static final String getNameFromClass() {
		return CFremoveIf.class.getSimpleName().toLowerCase().substring(2);
	}
	private static final String name=getNameFromClass();
	@Override
	public String getName() {return name;}
	
	@Override
	public boolean test() throws Exception {
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFremoveIf f = new CFremoveIf();
		if (!f.test()) throw new Exception("failed test");
	}
}
