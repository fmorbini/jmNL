package edu.usc.ict.nl.dm.reward;

import edu.usc.ict.nl.dm.reward.model.DialogueOperatorNodeTransition;

public class SwapoutReason {
	public static final SwapoutReason SEARCH=new SwapoutReason(Reason.SEARCH);
	public static final SwapoutReason EFFECT=new SwapoutReason(Reason.EFFECT);
	public enum Reason {EFFECT,INTERRUPTION,SEARCH};
	private Reason type;
	private DialogueOperatorNodeTransition it=null;
	public SwapoutReason(Reason type) {this.type=type;} 
	public SwapoutReason(DialogueOperatorNodeTransition interruptedTransition) {
		this(Reason.INTERRUPTION);
		this.it=interruptedTransition;
	}
	@Override
	public String toString() {
		return type.toString();
	}
	public boolean isSearchReason() {return this==SEARCH || type==Reason.SEARCH;}
	public boolean isEffectReason() {return this==EFFECT || type==Reason.EFFECT;}
	public boolean isInterruptionReason() {return type==Reason.INTERRUPTION && it!=null;}
}
