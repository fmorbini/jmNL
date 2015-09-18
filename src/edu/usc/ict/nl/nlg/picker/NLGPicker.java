package edu.usc.ict.nl.nlg.picker;

import edu.usc.ict.nl.bus.modules.NLGInterface;

public abstract class NLGPicker implements NLGPickerI {

	NLGInterface nlg=null;
	
	@Override
	public void setNLG(NLGInterface nlg) {
		this.nlg=nlg;
	}

}
