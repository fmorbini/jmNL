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
}
