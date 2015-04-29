package edu.usc.ict.nl.kb.cf;

import java.util.Collection;

import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;

public interface CustomFunctionInterface {
	public String getName();
	public boolean checkArguments(Collection<DialogueKBFormula> args);
	public Object eval(DialogueKBFormula f,DialogueKBInterface is,boolean forSimplification,EvalContext context) throws Exception;
	public boolean test() throws Exception;
}
