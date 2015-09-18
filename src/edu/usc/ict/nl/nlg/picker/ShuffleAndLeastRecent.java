package edu.usc.ict.nl.nlg.picker;

import java.util.Collection;

import edu.usc.ict.nl.bus.NLBusBase;

public class ShuffleAndLeastRecent extends NLGPicker {

	@Override
	public Object pick(Long sessionId, String sa,Collection things) throws Exception {
		return NLBusBase.pickEarliestUsedOrStillUnused(sessionId, things);
	}

}
