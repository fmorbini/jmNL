package edu.usc.ict.nl.bus;

public interface VRSpeakSpokeTrackerInterface {
	/**
	 *  
	 * @param vrMessageID
	 * @return this method returns the speech act ID that initiated the vr messages with the given ID
	 */
	public String getSpeechActIDFromVRMessageID(String vrMessageID);
	/**
	 * this methods records that the vr messages sequence with the given ID is concluded
	 * @param vrMessageID
	 */
	public void completeVRMessageWithID(String vrMessageID);
	/**
	 * returns true if the speech act can be closed. Used to decide if a vrspoke can cause a utterance done event to the dm. or if there are
	 * other things that the system has to wait before sending the utterance done event.
	 * @param sa
	 * @return
	 * @throws InterruptedException 
	 */
	public boolean canSpeechactBeEnded(String sa) throws InterruptedException;
	/**
	 * this method is called when a vrspoke is received and will make canSpeechactBeEnded return true if no other waiting criterion are specified by the specific nlg class used.
	 * @param sa
	 * @throws InterruptedException 
	 */
	public void receivedVrSpoke(String sa) throws InterruptedException;
}
