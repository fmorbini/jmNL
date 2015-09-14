package edu.usc.ict.nl.kb.cf;

import java.util.Collection;
import java.util.List;

import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.dm.reward.RewardDM;
import edu.usc.ict.nl.dm.reward.model.DialogueAction;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorTopic;
import edu.usc.ict.nl.dm.reward.model.RewardPolicy;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.util.StringUtils;

public class CFisCurrentTopic implements CustomFunctionInterface {

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args!=null && args.size()==1);
	}

	@Override
	public Boolean eval(DialogueKBFormula f, DialogueKBInterface is,
			boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		DialogueKBFormula arg = (DialogueKBFormula) f.getFirstChild();
		Object result=is.evaluate(arg,context);
		String topic=null;
		if (result instanceof String) topic=DialogueKBFormula.getStringValue((String)result);
		if (!StringUtils.isEmptyString(topic)) {
			RewardDM dm=(RewardDM) is.getValueOfVariable(NLBusBase.dmVariableName,ACCESSTYPE.AUTO_OVERWRITEAUTO,context);
			DialogueAction aa = (dm!=null)?dm.getCurrentActiveAction():null;
			if (aa!=null) {
				RewardPolicy dp=dm.getPolicy();
				List<DialogueOperatorTopic> topicNodes=dp.getAllTopicsForString(topic);
				if ((topicNodes!=null) && !topicNodes.isEmpty()) {
					List<DialogueOperatorTopic> topics = aa.getOperator().getTopics();
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
	
	private static final String name="isCurrentTopic".toLowerCase();
	@Override
	public String getName() {return name;}
	
	@Override
	public boolean test() throws Exception {
		TestRewardDM dm=new TestRewardDM(NLBusConfig.WIN_EXE_CONFIG);
		TestDialogueOperator op = (TestDialogueOperator) dm.getCurrentActiveAction().getOperator();
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
		CFisCurrentTopic f = new CFisCurrentTopic();
		if (!f.test()) throw new Exception("failed test");
	}
}
