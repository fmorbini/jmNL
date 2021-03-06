package edu.usc.ict.nl.nlg;

import edu.usc.ict.nl.bus.NLBusInterface;
import edu.usc.ict.nl.bus.VRSpeakSpokeTrackerInterface;
import edu.usc.ict.nl.bus.events.DMInterruptionRequest;
import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.config.NLGConfig;
import edu.usc.ict.nl.nlg.template.TemplatedNLG;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.vhmsg.VHBridge;

public class VRexpressBasicNLG extends TemplatedNLG implements VRSpeakSpokeTrackerInterface {

	private int counter=1;
	private String characterName=null; 
	
	protected VHBridge vhBridge;
	public UtteranceDoneTracker tracker;
	
	public VRexpressBasicNLG(NLGConfig c) {
		this(c,true);
	}

	public VRexpressBasicNLG(NLGConfig c, boolean loadData) {
		super(c,loadData);
		tracker=new UtteranceDoneTracker(logger);
		try {
			String activeMQserver=c.getNlBusConfigNC().getVhServer();
			String activeMQtopic=c.getNlBusConfigNC().getVhTopic();
			if (!StringUtils.isEmptyString(activeMQserver) && !StringUtils.isEmptyString(activeMQtopic)) {
				vhBridge=new VHBridge(c.getNlBusConfigNC().getVhServer(), c.getNlBusConfigNC().getVhTopic());
			}
			characterName=getConfiguration().getNlBusConfigNC().getVhSpeaker();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Float getDurationOfThisDMEvent(Long sessionID, NLGEvent ev) throws Exception {
		return null;
	}

	@Override
	public boolean isResource(Long sessionID, NLGEvent ev) throws Exception {
		return false;
	}
	
	@Override
	public NLBusInterface getNLModule() {return super.getNLModule();};
	
	@Override
	public NLGEvent doNLG(Long sessionID, DMSpeakEvent ev, SpeechActWithProperties line, boolean simulate) throws Exception {
		NLGEvent nlg = super.doNLG(sessionID, ev,line, simulate);
		sendVrExpress(sessionID,nlg,simulate);
		return nlg;
	}
	
	protected void sendVrExpress(Long sessionID, NLGEvent nlg, boolean simulate) {
		if (nlg!=null && !simulate) {
			DMSpeakEvent sa=nlg.getPayload();
			String text=nlg.getName();
			String id=getNextID(sessionID);
			String policyCharacterName=getNLModule().getCharacterName4Session(sessionID);
			vhBridge.sendMessage("vrExpress", buildVrExpress(policyCharacterName,characterName, id, sa.getName(), text));
			storeActiveMessage(id, nlg);
		}
	}
	
	protected void storeActiveMessage(String id,NLGEvent nlg) {
		tracker.storeActiveMessage(id,nlg);
	}
	
	public String getNextID(Long sessionID) {
		return sessionID+"-"+(counter++);
	}
	
	//sample vrExpress brad user 1340823169951-150-1 <?xml version="1.0" encoding="UTF-8" standalone="no" ?><act><participant id="brad" role="actor" /><fml><turn start="take" end="give" /><affect type="neutral" target="addressee"></affect><culture type="neutral"></culture><personality type="neutral"></personality></fml><bml><speech id="sp1" ref="greet_hello" type="application/ssml+xml">Hello</speech></bml></act>
	public String buildVrExpress(String animationCharacterName, String vhCharacterName, String id,String sa, String text) {
		String affect="<affect type=\"neutral\" target=\"addressee\"></affect>";
		String culture="<culture type=\"neutral\"></culture>";
		String personality="<personality type=\"neutral\"></personality>";
		String fml="<fml><turn start=\"take\" end=\"give\" />"+affect+culture+personality+"</fml>";
		String bml="<bml><speech id=\"sp1\" ref=\""+sa+"\" type=\"application/ssml+xml\">"+text+"</speech></bml>";
		String content="<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?><act><participant id=\""+animationCharacterName+"\" role=\"actor\" />"+fml+bml+"</act>";
		String msg=animationCharacterName+" "+vhCharacterName+" "+id+" "+content;
		return msg;
	}

	public String getSpeechActIDFromVRMessageID(String vrMessageID) {
		return tracker.getSpeechActIDFromVRMessageID(vrMessageID);
	}
	public void completeVRMessageWithID(String vrMessageID) {
		tracker.completeVRMessageWithID(vrMessageID);
	}
	
	@Override
	public void interrupt(DMInterruptionRequest ev) throws Exception {
		sendInterruptMessage(ev);
		super.interrupt(ev);
	}
	
	protected void sendInterruptMessage(DMInterruptionRequest ev) {
		vhBridge.sendMessage("sb","scene.getBmlProcessor().interruptCharacter(\""+characterName+"\", .5)");		
	}

	public VHBridge getVhBridge() {
		return vhBridge;
	}

	@Override
	public boolean canSpeechactBeEnded(String sa) throws InterruptedException {
		return tracker.canSpeechactBeEnded(sa);
	}
	@Override
	public void receivedVrSpoke(String sa) throws InterruptedException {
		tracker.receivedVrSpoke(sa);
	}
}
