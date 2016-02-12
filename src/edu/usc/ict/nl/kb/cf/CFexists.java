package edu.usc.ict.nl.kb.cf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import edu.usc.ict.nl.bus.events.NLUEvent;
import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.DMConfig;
import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.TrivialDialogueKB;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.ne.BasicNE;
import edu.usc.ict.nl.nlu.ne.NE;
import edu.usc.ict.nl.nlu.ne.Numbers;

/**
 * 
 * @author morbini
 *	this custom function requires 2 arguments: the nlu interpretation, the nlu idenditier and the length of the history to inspect.
 */
public class CFexists implements CustomFunctionInterface {

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
		Object collectionArg=is.evaluate(arg2,context);
		List ret=null;
		if (collectionArg!=null && collectionArg instanceof Collection) {
			TrivialDialogueKB internalKB=new TrivialDialogueKB((DialogueKB)is);
			for(Object thing:(Collection)collectionArg) {
				internalKB.set(arg1.getName(), thing);
				Object predicateArg=internalKB.evaluate(arg3,context);
				if (predicateArg!=null && predicateArg instanceof Boolean && ((Boolean)predicateArg)) {
					if (ret==null) ret=new ArrayList<>();
					ret.add(thing);
				}
			}
		}

		return ret;
	}
	
	private static final String getNameFromClass() {
		return CFexists.class.getSimpleName().toLowerCase().substring(2);
	}
	private static final String name=getNameFromClass();
	@Override
	public String getName() {return name;}
	
	@Override
	public boolean test() throws Exception {
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFexists f = new CFexists();
		if (!f.test()) throw new Exception("failed test");
	}
}
