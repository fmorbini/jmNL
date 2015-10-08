package edu.usc.ict.nl.nlg.template;

import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.vhmsg.VHBridge;

public class FunctionArguments {
	public final DialogueKBInterface is;
	public final String stringArg;
	public boolean simulate=false;
	public final String speechAct;
	public final NLGEvent output;
	public Long sessionId;
	
	public FunctionArguments(final NLGEvent output,final DialogueKBInterface is,final String stringArg,final String speechAct,final boolean simulate) {
		this.is=is;
		this.stringArg=stringArg;
		this.simulate=simulate;
		this.output=output;
		this.speechAct=speechAct;
		this.sessionId=output.getSessionID();
	}
}
