package edu.usc.ict.nl.bus;

import java.io.File;
import java.util.Collection;
import java.util.List;

import edu.usc.ict.nl.bus.modules.DMEventsListenerInterface;
import edu.usc.ict.nl.bus.protocols.Protocol;
import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.nlu.NLUOutput;

public interface NLBusInterface extends DMEventsListenerInterface,ExternalListenerInterface {
	public NLBusConfig getConfiguration();

	public void setDialogSession2User(Long sessionID,String user);

	public List<SpecialVar> getSpecialVariables(Long sessionId);
	public List<SpecialVar> getSpecialVariables(String characterName, boolean createIfNotThere);
	public NLUOutput getNLUOutput(Long sessionId,String userUtterance) throws Exception;
	public void setSpeakingStateVarForSessionAs(Long sessionId,Boolean state) throws Exception;
	public float getTimeUserHasBeenSpeaking();
	//public void handlePerceptionEvent(VRPerception msg) throws Exception;
	//public void handleMinatEvent(VHBridgewithMinat.Minat msg) throws Exception;
	public void handleLoginEvent(Long sessionId, String userID) throws Exception;

	public void addBusListener(ExternalListenerInterface i);

	public String getCharacterName4Session(Long sid);
	
	void refreshPolicyForCharacter(String characterName) throws Exception;

	/**
	 * is save is true, then it saves the current is to a file. If is false it does everything but saving to file (setting some is variables)
	 * @param sid
	 * @param save
	 * @throws Exception
	 */
	public void saveInformationStateForSession(Long sid,boolean save) throws Exception;
	public void loadInformationStateForSession(Long sid,Collection<DialogueOperatorEffect> content) throws Exception;
	public void loadInformationStateForSession(Long sid,File fis) throws Exception;
	
	public List<Protocol> getProtocols();
	
	public boolean canDetectUtteranceCompleted();
}
