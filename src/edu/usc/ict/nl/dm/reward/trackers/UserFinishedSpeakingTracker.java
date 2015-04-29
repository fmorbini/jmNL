package edu.usc.ict.nl.dm.reward.trackers;

import org.apache.log4j.Logger;

import edu.usc.ict.nl.bus.NLBus;
import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.dm.reward.RewardDM;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;


public class UserFinishedSpeakingTracker extends ValueTracker {

	public UserFinishedSpeakingTracker(RewardDM dm) {
		super(dm);
	}

	@Override
	public Boolean getter() {
		DialogueKB is = dm.getInformationState();
		Boolean userSpeaking=null;
		try {
			userSpeaking = (Boolean) is.evaluate(is.getValueOfVariable(NLBusBase.userSpeakingStateVarName,ACCESSTYPE.AUTO_OVERWRITEAUTO,null),null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		boolean ret=userSpeaking==null || !userSpeaking;
		//call the setter to make sure that other methods depending on the set values are in sinck with the result provided by this getter call.
		setter(ret);
		return ret;
	}

	@Override
	protected void setter(Object cv) {
		boolean currentValue=(Boolean)cv;
		Boolean oldValue=(Boolean)value;
		if (oldValue==null || currentValue!=oldValue) {
			value=currentValue;
			if (currentValue) touch();
		}
		if (!currentValue) {// i.e. the user is speaking, update the length variable.
			DialogueKB is = dm.getInformationState();
			try {
				float time = dm.getMessageBus().getTimeUserHasBeenSpeaking();
				if (time>0) is.setValueOfVariable(NLBusBase.lengthOfLastThingUserSaidVarName,time,ACCESSTYPE.AUTO_OVERWRITEAUTO);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void updateIS() {
		super.updateIS();
		try {
			lock();
			DialogueKB localIS=dm.getInformationState();
			if (getter()) {
				float delta=getDelta();
				localIS.setValueOfVariable(NLBusBase.timeSinceLastUserActionVariableName, delta, ACCESSTYPE.AUTO_OVERWRITEAUTO);
			} else {
				localIS.store(DialogueOperatorEffect.createAssignment(NLBusBase.timeSinceLastUserActionVariableName,DialogueKBFormula.create("0", null)),ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
			}
		} catch (Exception e) {
			Logger logger=dm.getLogger();
			logger.error(e);
		}
		finally {unlock();}
	}

}
