package edu.usc.ict.nl.bus;

import java.io.File;
import java.util.List;

import edu.usc.ict.nl.bus.modules.DMEventsListenerInterface;
import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.nlu.NLUOutput;

public interface NLBusInterface extends DMEventsListenerInterface,ExternalListenerInterface {
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
	 * saves in the provided file the current information state of the given session.
	 * @param sid
	 * @param is
	 * @throws Exception
	 */
	public void saveInformationStateForSession(Long sid,File is) throws Exception;
	/**
	 * imports the values saved in the provided file into the information state for the given session.
	 * @param sid
	 * @param is
	 * @throws Exception
	 */
	public void loadInformationStateForSession(Long sid,File is) throws Exception;
}
