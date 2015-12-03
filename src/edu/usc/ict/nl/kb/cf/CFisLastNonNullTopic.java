package edu.usc.ict.nl.kb.cf;

import java.util.Collection;
import java.util.List;

import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.config.DMConfig;
import edu.usc.ict.nl.dm.reward.RewardDM;
import edu.usc.ict.nl.dm.reward.model.DialogueOperator;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorTopic;
import edu.usc.ict.nl.dm.reward.model.RewardPolicy;
import edu.usc.ict.nl.dm.reward.model.RewardPolicy.OpType;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.util.StringUtils;

public class CFisLastNonNullTopic implements CustomFunctionInterface {

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args!=null && args.size()==1);
	}

	@Override
	public Boolean eval(DialogueKBFormula f, DialogueKBInterface is,
			boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		// returns true is the argument has a value different from NULL, otherwise returns false.
		DialogueKBFormula arg = (DialogueKBFormula) f.getFirstChild();
		Object result=is.evaluate(arg,context);
		String topic=null;
		if (result instanceof String) topic=DialogueKBFormula.getStringValue((String) result);
		if (!StringUtils.isEmptyString(topic)) {
			RewardDM dm=(RewardDM) is.getValueOfVariable(NLBusBase.dmVariableName,ACCESSTYPE.AUTO_OVERWRITEAUTO,context);
			RewardPolicy dp=(dm!=null)?dm.getPolicy():null;
			String opName=(String)is.evaluate(RewardDM.lastNonNullOperatorVariableFormula,context);
			if (opName!=null) opName=DialogueKBFormula.getStringValue(opName);
			DialogueOperator op=(dp!=null)?dp.getOperatorNamed(opName,OpType.NORMAL):null;
			
			if (op!=null) {
				List<DialogueOperatorTopic> topicNodes=dp.getAllTopicsForString(topic);
				if ((topicNodes!=null) && !topicNodes.isEmpty()) {
					List<DialogueOperatorTopic> topics = op.getTopics();
					for(DialogueOperatorTopic t:topics) {
						for(DialogueOperatorTopic topicNode:topicNodes) {
							if (t.contains(topicNode)) return true;
						}
					}
				}
			} else if (topic.equals("null")) {
				return true;
			}
		}
		return false;
	}
	
	private static final String name="isLastNonNullTopic".toLowerCase();
	@Override
	public String getName() {return name;}
	
	@Override
	public boolean test() throws Exception {
		TestRewardDM dm=new TestRewardDM(DMConfig.WIN_EXE_CONFIG);
		DialogueKB is=dm.getInformationState();
		is.setValueOfVariable(NLBusBase.lastNonNullOperatorVariableName, "'optest'", ACCESSTYPE.AUTO_OVERWRITEAUTO);
		TestDialogueOperator op = (TestDialogueOperator) dm.getPolicy().getOperatorNamed("optest", null);
		op.addTopicToOperator("a.b");
		op.addTopicToOperator("c.d");
		op.processOperatorTopics(dm.getPolicy());
		Boolean r=eval(DialogueKBFormula.parse(getName()+"('a')"),dm.getInformationState(),false,null);
		if (!r.equals(true)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"('a.b')"),dm.getInformationState(),false,null);
		if (!r.equals(true)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"('a.b.c')"),dm.getInformationState(),false,null);
		if (!r.equals(false)) return false;
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFisLastNonNullTopic f = new CFisLastNonNullTopic();
		if (!f.test()) throw new Exception("failed test");
	}
}
