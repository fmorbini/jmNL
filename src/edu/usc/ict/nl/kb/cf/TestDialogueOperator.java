package edu.usc.ict.nl.kb.cf;

import edu.usc.ict.nl.dm.reward.model.DialogueOperator;
import edu.usc.ict.nl.dm.reward.model.RewardPolicy;

public class TestDialogueOperator extends DialogueOperator {

	public TestDialogueOperator(String name) {
		super(name);
	}

	public TestDialogueOperator() {
		super();
	}

	@Override
	public void addTopicToOperator(String topicName) {
		super.addTopicToOperator(topicName);
	}
	
	@Override
	public void processOperatorTopics(RewardPolicy dp) throws Exception {
		super.processOperatorTopics(dp);
	}

}
