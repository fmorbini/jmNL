package edu.usc.ict.nl.bus.modules;

import java.util.List;
import java.util.Map;

import edu.usc.ict.nl.bus.NLBusInterface;
import edu.usc.ict.nl.bus.events.DMInterruptionRequest;
import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.nlg.SpeechActWithProperties;

public interface NLGInterface {
	public NLGEvent doNLG(Long sessionID, DMSpeakEvent ev,boolean simulate) throws Exception;
	public NLGEvent doNLG(Long sessionID, DMSpeakEvent ev,SpeechActWithProperties line,boolean simulate) throws Exception;
	public DialogueKBInterface getKBForEvent(DMSpeakEvent ev) throws Exception;
	public void setNLModule(NLBusInterface nlModule);
	public Float getDurationOfThisDMEvent(Long sessionID, NLGEvent ev) throws Exception;
	public boolean isResource(Long sessionID, NLGEvent ev) throws Exception;
	public String getAudioFileName4SA(String sa) throws Exception;
	public void interrupt(DMInterruptionRequest ev) throws Exception;
	/**
	 * call this to force the nlg to reload data required by it. For example, the file of system utterances. A NLG that uses already dynamically sourced
	 *  information may ignore this call. The dialog manager policy is not reloaded by this call.
	 * @throws Exception
	 */
	public void reloadData() throws Exception;

	public void kill() throws Exception;
	public Map<String,List<SpeechActWithProperties>> getAllLines() throws Exception;
}
