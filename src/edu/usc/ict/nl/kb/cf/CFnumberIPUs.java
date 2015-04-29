package edu.usc.ict.nl.kb.cf;

import java.util.Collection;
import java.util.Deque;

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

public class CFnumberIPUs implements CustomFunctionInterface {

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args==null || (args!=null && args.size()==0));
	}

	@Override
	public Integer eval(DialogueKBFormula f, DialogueKBInterface is,
			boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		Deque<Deque<NLUOutput>> uh=(Deque<Deque<NLUOutput>>) is.getValueOfVariable(NLBusBase.userEventsHistory,ACCESSTYPE.AUTO_OVERWRITEAUTO,context);
		if (uh!=null && !uh.isEmpty()) {
			Deque<NLUOutput> el = uh.peek();
			if (el!=null) return el.size();
			else return 0;
		} else return 0;
	}
	
	private static final String name="ipuNumber".toLowerCase();
	@Override
	public String getName() {return name;}

	@Override
	public boolean test() throws Exception {
		NLBusConfig config=NLBusConfig.WIN_EXE_CONFIG.cloneObject();
		config.setLoginEventName("login");
		TestRewardDM dm=new TestRewardDM(config);
		DialogueKB is = dm.getInformationState();
		
		Integer r=eval(DialogueKBFormula.parse(getName()+"()"),is,false,null);
		if (!r.equals(0)) return false;
		
		dm.updateUserEventsHistory(new NLUEvent(new NLUOutput("login", "login", 1, null), 0));
		
		r=eval(DialogueKBFormula.parse(getName()+"()"),is,false,null);
		if (!r.equals(0)) return false;
		
		dm.updateUserEventsHistory(new SystemUtteranceDoneEvent("", null));
		
		r=eval(DialogueKBFormula.parse(getName()+"()"),is,false,null);
		if (!r.equals(0)) return false;
		
		dm.updateUserEventsHistory(new NLUEvent(new NLUOutput("text first", "first", 1, null), 0));
		
		r=eval(DialogueKBFormula.parse(getName()+"()"),is,false,null);
		if (!r.equals(1)) return false;

		dm.updateUserEventsHistory(new NLUEvent(new NLUOutput("text second", "second", 1, null), 0));
		
		r=eval(DialogueKBFormula.parse(getName()+"()"),is,false,null);
		if (!r.equals(2)) return false;
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFnumberIPUs f = new CFnumberIPUs();
		if (!f.test()) throw new Exception("failed test");
	}
}
