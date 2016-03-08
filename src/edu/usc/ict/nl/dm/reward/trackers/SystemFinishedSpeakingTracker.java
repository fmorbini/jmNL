package edu.usc.ict.nl.dm.reward.trackers;

import org.apache.log4j.Logger;

import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.dm.reward.RewardDM;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;


public class SystemFinishedSpeakingTracker extends ValueTracker {

	public SystemFinishedSpeakingTracker(RewardDM dm) {
		super(dm);
	}

	@Override
	public Boolean getter() {
		boolean ret=!dm.getSpeakingTracker().isSpeaking();
		setter(ret);
		return ret;
	}

	@Override
	protected void setter(Object cv) {
		boolean currentValue=(Boolean) cv;
		Boolean oldValue=(Boolean)value;
		if (oldValue==null || currentValue!=oldValue) {
			value=currentValue;
			if (currentValue) touch();
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
				localIS.setValueOfVariable(NLBusBase.timeSinceLastSystemActionVariableName, delta, ACCESSTYPE.AUTO_OVERWRITEAUTO);
			} else {
				Float spokenFraction = dm.getSpeakingTracker().getSpokenFraction();
				localIS.setValueOfVariable(NLBusBase.systemSpeakingCompletionVarName, spokenFraction!=null?spokenFraction:0,ACCESSTYPE.AUTO_OVERWRITEAUTO);
			}
		} catch (Exception e) {
			dm.getLogger().error("Error while updating IS in "+this.getClass().getSimpleName(),e);
		}
		finally {unlock();}
	}

}
