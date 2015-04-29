package edu.usc.ict.nl.vhmsg;

import java.util.List;

import edu.usc.ict.nl.bus.NLBus;
import edu.usc.ict.nl.bus.events.NLUEvent;
import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.bus.modules.NLUInterface;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.vhmsg.VHBridge.VRSpeech;
import edu.usc.ict.vhmsg.MessageEvent;
import edu.usc.ict.vhmsg.MessageListener;

public class VHmsgNL extends NLBus {

	private edu.usc.ict.nl.vhmsg.VHBridge vhBridge;

	public VHmsgNL() throws Exception {
		super();
		NLBusConfig c=getConfiguration();
		String activeMQserver=c.getVhServer();
		String activeMQtopic=c.getVhTopic();
		if (StringUtils.isEmptyString(activeMQserver) || StringUtils.isEmptyString(activeMQtopic)) throw new Exception("error using class "+this.getClass().getCanonicalName()+": it requires a vh server and topic to be set.");
		vhBridge=new VHBridge(activeMQserver, activeMQtopic, "vrSpeech", createMessageListener());
	}
	
	
	private MessageListener createMessageListener() {
		return new MessageListener() {

			public void messageAction(MessageEvent e)
			{
				try {
					// this will fire an exception if the input message is not a vrExpress message. 
					VHBridge.VRSpeech msg=vhBridge.processVrSpeechEvent(e);
					if (msg!=null && msg.isComplete()) {
						handleTextUtteranceEvent(msg);
					}
				} catch (Exception ex){
					//System.out.println("MessageListener.messageAction received non vrExpress message.");
				}
			}
		};
	}

	private void handleTextUtteranceEvent(VRSpeech msg) throws Exception {
		long sessionId=999l;

		logger.info("replying to received message from "+msg.getSpeaker()+" directed to me (default)="+msg.getUtterance());
		DM dm=getPolicyDMForSession(sessionId);
		NLUInterface nlu=getNlu(sessionId);
		String text = msg.getUtterance();
		if (isInExecuteMode()) {
			List<NLUOutput> userSpeechActs = nlu.getNLUOutput(text, null,null);
			NLUOutput selectedUserSpeechAct=dm.selectNLUOutput(text,sessionId, userSpeechActs);
			//Long saEventId = sendUserSpeechActEvent(text, event, selectedUserSpeechAct.getId());
			vhBridge.sendVRNLU(selectedUserSpeechAct.getId(), msg.getSpeaker(), sessionId);
			dm.handleEvent(new NLUEvent(selectedUserSpeechAct, sessionId));
		} else {
			throw new Exception("unhanlded");
		}
	}
	
	public static void main(String[] args) throws Exception {
		init(args);
		start();
	}
}
