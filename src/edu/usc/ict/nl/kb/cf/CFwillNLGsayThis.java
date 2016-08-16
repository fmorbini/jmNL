package edu.usc.ict.nl.kb.cf;

import java.util.Collection;

import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.bus.modules.NLGInterface;
import edu.usc.ict.nl.config.DMConfig;
import edu.usc.ict.nl.dm.reward.RewardDM;
import edu.usc.ict.nl.dm.reward.SpeakingTracker;
import edu.usc.ict.nl.dm.reward.model.DialogueAction;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorNodeTransition;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.util.StringUtils;

public class CFwillNLGsayThis implements CustomFunctionInterface {

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args!=null && (args.size()==1));
	}

	@Override
	public Boolean eval(DialogueKBFormula f, DialogueKBInterface is,
			boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		DialogueKBFormula arg = (DialogueKBFormula) f.getArg(1);
		if (arg!=null) {
			Object ef=is.evaluate(arg,context);
			String eaString=null;
			if (ef!=null) {
				if (ef instanceof String) eaString=DialogueKBFormula.getStringValue((String) ef);
				else eaString=ef.toString();
			}
			if (!StringUtils.isEmptyString(eaString)) {
				RewardDM dm=(RewardDM) is.getValueOfVariable(NLBusBase.dmVariableName,ACCESSTYPE.AUTO_OVERWRITEAUTO,context);
				Long sid=dm.getSessionID();
				NLGInterface nlg = dm.getMessageBus().getNlg(sid);
				try {
					return nlg.canGenerate(sid, new DMSpeakEvent(null, eaString, sid, null, null));
				} catch (Exception e) {}
			}
		}
		return null;
	}
	
	private static final String getNameFromClass() {
		return CFwillNLGsayThis.class.getSimpleName().toLowerCase().substring(2);
	}
	private static final String name=getNameFromClass();
	@Override
	public String getName() {return name;}
	
	@Override
	public boolean test() throws Exception {
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFwillNLGsayThis f = new CFwillNLGsayThis();
		if (!f.test()) throw new Exception("failed test");
	}
}
