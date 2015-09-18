package edu.usc.ict.nl.nlg.picker;

import java.util.Collection;

import edu.usc.ict.nl.bus.modules.NLGInterface;

public interface NLGPickerI {
	public Object pick(Long sessionId,String sa, Collection things) throws Exception;
	public void setNLG(NLGInterface nlg);
}
