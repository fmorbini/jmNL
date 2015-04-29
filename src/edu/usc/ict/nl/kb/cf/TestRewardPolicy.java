package edu.usc.ict.nl.kb.cf;

import java.util.HashMap;
import java.util.Map;

import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.dm.reward.model.DialogueOperator;
import edu.usc.ict.nl.dm.reward.model.RewardPolicy;

public class TestRewardPolicy extends RewardPolicy {

	private Map<String,DialogueOperator> ops=null;
	
	public TestRewardPolicy(NLBusConfig config) {
		super(config);
		ops=new HashMap<String, DialogueOperator>();
	}
	
	@Override
	public DialogueOperator getOperatorNamed(String name, OpType type) {
		DialogueOperator op=ops.get(name);
		if (op==null) ops.put(name,op=new TestDialogueOperator(name));
		return op;
	}
}
