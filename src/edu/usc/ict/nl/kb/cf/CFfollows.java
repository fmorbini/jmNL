package edu.usc.ict.nl.kb.cf;

import java.util.Collection;

import edu.usc.ict.nl.dm.reward.possibilityGraph.OperatorHistoryNode;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.util.StringUtils;

public class CFfollows implements CustomFunctionInterface {

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args!=null && args.size()>=1 && args.size()<=2);
	}

	@Override
	public Boolean eval(DialogueKBFormula f, DialogueKBInterface is,
			boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		// returns true is the argument has a value different from NULL, otherwise returns false.
		DialogueKBFormula arg1 = (DialogueKBFormula) f.getArg(1);
		DialogueKBFormula arg2 = (DialogueKBFormula) f.getArg(2);
		Object opThatMustPreceedArg=is.evaluate(arg1,context);
		Object opThatMustPreceedCompletedArg=is.evaluate(arg2,context);
		String opThatMustPreceed=null;
		boolean opThatMustPreceedCompleted=false;
		if (opThatMustPreceedArg instanceof String) opThatMustPreceed=DialogueKBFormula.getStringValue((String)opThatMustPreceedArg);
		if (opThatMustPreceedCompletedArg!=null && opThatMustPreceedCompletedArg instanceof Boolean) opThatMustPreceedCompleted=(Boolean)opThatMustPreceedCompletedArg;
	
		if (!StringUtils.isEmptyString(opThatMustPreceed)) {
			OperatorHistoryNode lastOp=context.getExecutedOperatorsHistory();
			OperatorHistoryNode found=lastOp.findOperator(opThatMustPreceed,opThatMustPreceedCompleted);
			if (found!=null) return true;
		}
		return false;
	}
	
	private static final String name="follows".toLowerCase();
	@Override
	public String getName() {return name;}
	
	@Override
	public boolean test() throws Exception {
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFfollows f = new CFfollows();
		if (!f.test()) throw new Exception("failed test");
	}
}
