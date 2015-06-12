package edu.usc.ict.nl.nlg;

import java.util.HashMap;
import java.util.Map;

import edu.usc.ict.nl.bus.NLBusInterface;
import edu.usc.ict.nl.bus.VRSpeakSpokeTrackerInterface;
import edu.usc.ict.nl.bus.events.DMInterruptionRequest;
import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.nlg.echo.EchoNLG;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.vhmsg.VHBridge;

public class VRexpressBasicNLG extends EchoNLG implements VRSpeakSpokeTrackerInterface {

	private int counter=1;
	private String characterName=null; 
	
	private Map<String,NLGEvent> activeMessages;
	protected VHBridge vhBridge;
	
	public VRexpressBasicNLG(NLBusConfig c) {
		super(c);
		try {
			String activeMQserver=c.getVhServer();
			String activeMQtopic=c.getVhTopic();
			if (!StringUtils.isEmptyString(activeMQserver) && !StringUtils.isEmptyString(activeMQtopic)) {
				vhBridge=new VHBridge(c.getVhServer(), c.getVhTopic());
			}
			activeMessages=new HashMap<String, NLGEvent>();
			characterName=getConfiguration().getVhSpeaker();
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
	public NLGEvent doNLG(Long sessionID,
			DMSpeakEvent ev, boolean simulate) throws Exception {
		NLGEvent nlg = super.doNLG(sessionID, ev, simulate);
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
			activeMessages.put(id, nlg);
		}
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
		NLGEvent nlg = activeMessages.get(vrMessageID);
		if (nlg!=null) {
			DMSpeakEvent ev=nlg.getPayload();
			if (ev!=null) return ev.getName();
		}
		return null;
	}
	public void completeVRMessageWithID(String vrMessageID) {
		activeMessages.remove(vrMessageID);
	}
	
	@Override
	public void interrupt(DMInterruptionRequest ev) throws Exception {
		super.interrupt(ev);
		vhBridge.sendMessage("sb","scene.getBmlProcessor().interruptCharacter(\""+characterName+"\", .5)");		
	}
	
	public VHBridge getVhBridge() {
		return vhBridge;
	}
}
