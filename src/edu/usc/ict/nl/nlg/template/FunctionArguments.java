package edu.usc.ict.nl.nlg.template;

import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.vhmsg.VHBridge;

public class FunctionArguments {
	public final DialogueKBInterface is;
	public final VHBridge vhBridge;
	public final String stringArg;
	public boolean simulate=false;
	public final String speechAct;
	
	public FunctionArguments(final DialogueKBInterface is,final VHBridge vhBridge,final String stringArg,final String speechAct,final boolean simulate) {
		this.is=is;
		this.vhBridge=vhBridge;
		this.stringArg=stringArg;
		this.simulate=simulate;
		this.speechAct=speechAct;
	}
}
