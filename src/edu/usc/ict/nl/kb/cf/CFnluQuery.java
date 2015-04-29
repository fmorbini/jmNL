package edu.usc.ict.nl.kb.cf;

import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;

import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.bus.events.NLUEvent;
import edu.usc.ict.nl.bus.events.SystemUtteranceDoneEvent;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.dm.reward.RewardDM;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.util.StringUtils;

/**
 * 
 * @author morbini
 *	this custom function requires 2 arguments: the nlu interpretation, the nlu idenditier and the length of the history to inspect.
 */
public class CFnluQuery implements CustomFunctionInterface {

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args!=null && (args.size()>=3 || args.size()<=4));
	}

	@Override
	public Object eval(DialogueKBFormula f, DialogueKBInterface is,
			boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		DialogueKBFormula arg1 = (DialogueKBFormula) f.getArg(1);
		DialogueKBFormula arg2 = (DialogueKBFormula) f.getArg(2);
		DialogueKBFormula arg3 = (DialogueKBFormula) f.getArg(3);
		DialogueKBFormula arg4 = (DialogueKBFormula) f.getArg(4);
		Object nluNameArg=is.evaluate(arg1,context);
		Object nluInterpretationArg=is.evaluate(arg2,context);
		Object historyLengthArg=is.evaluate(arg3,context);
		Object countOutputArg=is.evaluate(arg4,context);
		String nluName=null,nluInterpretation=null;
		Integer historyLength=null;
		boolean returnACount=false;
		boolean justLastNLUmessage=false;
		if (nluNameArg instanceof String) nluName=DialogueKBFormula.getStringValue((String) nluNameArg);
		if (nluInterpretationArg instanceof String) nluInterpretation=DialogueKBFormula.getStringValue((String) nluInterpretationArg);
		if (historyLengthArg instanceof String) {
			historyLengthArg=DialogueKBFormula.getStringValue((String)historyLengthArg);
			justLastNLUmessage=((String) historyLengthArg).equalsIgnoreCase("last");
		} else if (historyLengthArg instanceof Number) historyLength=((Number) historyLengthArg).intValue();
		if (countOutputArg!=null && countOutputArg instanceof Boolean) returnACount=(Boolean)countOutputArg;
		
		int count=0;
		
		if (!StringUtils.isEmptyString(nluInterpretation)) {
			Deque<Deque<NLUOutput>> uh=(Deque<Deque<NLUOutput>>) is.getValueOfVariable(NLBusBase.userEventsHistory,ACCESSTYPE.AUTO_OVERWRITEAUTO,context);
			if (uh!=null && !uh.isEmpty()) {
				Iterator<Deque<NLUOutput>> it=uh.iterator();
				for(;(historyLength==null || historyLength>0) && it.hasNext();) {
					Deque<NLUOutput> uhEl=it.next();
					if (uhEl!=null && !uhEl.isEmpty()) {
						for(NLUOutput x:uhEl) {
							if (x.matchesInterpretation(nluInterpretation,nluName)) {
								if (returnACount) count++;
								else return true;
							}
							if (justLastNLUmessage) {
								if (returnACount) return count;
								else return false;
							}
						}
					}
					if (historyLength!=null) historyLength--;
				}
			}
		}
		if (returnACount) return count;
		else return false;
	}
	
	private static final String name="queryNLU".toLowerCase();
	@Override
	public String getName() {return name;}
	
	@Override
	public boolean test() throws Exception {
		TestRewardDM dm=new TestRewardDM(NLBusConfig.WIN_EXE_CONFIG);
		dm.updateUserEventsHistory(new NLUEvent(new NLUOutput("login", "login", 1, null), 0));
		dm.updateUserEventsHistory(new NLUEvent(new NLUOutput("login", "login", 1, null), 0));
		dm.updateUserEventsHistory(new SystemUtteranceDoneEvent("", null));
		dm.updateUserEventsHistory(new NLUEvent(new NLUOutput("text first", "first", 1, null), 0));
		dm.updateUserEventsHistory(new NLUEvent(new NLUOutput("text first", "first", 1, null), 0));
		dm.updateUserEventsHistory(new NLUEvent(new NLUOutput("text second", "second", 1, null), 0));
		DialogueKB is = dm.getInformationState();
		Object r=eval(DialogueKBFormula.parse(getName()+"(null,'first',1)"),is,false,null);
		if (!r.equals(true)) return false;
		r=dm.getInformationState().evaluate(DialogueKBFormula.parse("==("+getName()+"(null,'first',1,true),2)"),null);
		if (!r.equals(true)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"(null,'second',1)"),is,false,null);
		if (!r.equals(true)) return false;
		r=dm.getInformationState().evaluate(DialogueKBFormula.parse("==("+getName()+"(null,'second',1,true),1)"),null);
		if (!r.equals(true)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"(null,'second','last')"),is,false,null);
		if (!r.equals(true)) return false;
		r=dm.getInformationState().evaluate(DialogueKBFormula.parse("==("+getName()+"(null,'second','last',true),1)"),null);
		if (!r.equals(true)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"(null,'first','last')"),is,false,null);
		if (!r.equals(false)) return false;
		r=dm.getInformationState().evaluate(DialogueKBFormula.parse("==("+getName()+"(null,'first','last',true),0)"),null);
		if (!r.equals(true)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"(null,'login',2)"),is,false,null);
		if (!r.equals(true)) return false;
		r=dm.getInformationState().evaluate(DialogueKBFormula.parse("==("+getName()+"(null,'login',2,true),2)"),null);
		if (!r.equals(true)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"(null,'login',1)"),is,false,null);
		if (!r.equals(false)) return false;
		r=dm.getInformationState().evaluate(DialogueKBFormula.parse("==("+getName()+"(null,'login',1,true),0)"),null);
		if (!r.equals(true)) return false;
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFnluQuery f = new CFnluQuery();
		if (!f.test()) throw new Exception("failed test");
	}
}
