package edu.usc.ict.nl.bus.protocols;

import edu.usc.ict.nl.bus.NLBus;
import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.vhmsg.VHBridge;
import edu.usc.ict.vhmsg.MessageEvent;
import edu.usc.ict.vhmsg.MessageListener;

public class VHmetaProtocol extends Protocol {
	private VHBridge vhBridge=null;

	public VHmetaProtocol(NLBus bus) throws Exception {
		super(bus);
		if (config.hasVHConfig()) {
			vhBridge=new VHBridge(config.getVhServer(), config.getVhTopic());
			vhBridge.addMessageListenerFor("vrMeta", createVrMetaMessageListener());
		} else {
			logger.error(this.getClass()+" requested to start but no VH configuration.");
		}
	}
	
	protected MessageListener createVrMetaMessageListener() {
		return new MessageListener() {

			public void messageAction(MessageEvent e)
			{
				VHBridge.VRMeta msg=null;
				try {
					// this will fire an exception if the input message is not a vrExpress message. 
					msg=vhBridge.processVrMetaEvent(e);
				} catch (Exception ex){
					//logger.warn("MessageListener.messageAction received non vrPerception message.");
				}
				if (msg!=null) {
					Long foundSession=null;
					//find session for addressee.
					for(Long sessionID : bus.getSessions()) {
						String ch=bus.getCharacterName4Session(sessionID);
						if (ch.equals(msg.getAddressee())) {
							foundSession=sessionID;
							logger.info("found session for character "+msg.getAddressee()+" as session: "+foundSession);
							break;
						}
					}
					try {
						DM dm=(foundSession!=null)?bus.getDM(foundSession, false):null;
						switch (msg.getType()) {
						case PAUSE:
							logger.info("doing pause on session: "+foundSession);
							if (dm!=null) dm.setPauseEventProcessing(true);
							break;
						case START:
							logger.info("doing start on session: "+foundSession);
							foundSession=bus.startSession(msg.getAddressee(), foundSession);
							logger.info("started new session for character "+msg.getAddressee()+" as session: "+foundSession);
							dm=bus.getDM(foundSession);
							bus.handleLoginEvent(foundSession, null);
							break;
						case STOP:
							logger.info("doing stop of session: "+foundSession);
							bus.terminateSession(foundSession);
							break;
						}
					} catch (Exception e1) {
						logger.error("Error processing vrMeta message:",e1);
					}
				}
			}
		};
	}
	
	@Override
	public void kill() {
		if (vhBridge!=null) vhBridge.sendComponentKilled(config.getVhComponentId());
	}
}
