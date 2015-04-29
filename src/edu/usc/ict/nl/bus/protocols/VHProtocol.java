package edu.usc.ict.nl.bus.protocols;

import java.io.File;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import edu.usc.ict.nl.audio.util.Audio;
import edu.usc.ict.nl.bus.NLBus;
import edu.usc.ict.nl.bus.VRSpeakSpokeTrackerInterface;
import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.NLUEvent;
import edu.usc.ict.nl.bus.events.SystemUtteranceDoneEvent;
import edu.usc.ict.nl.bus.events.SystemUtteranceLengthEvent;
import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.bus.modules.NLGInterface;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.vhmsg.VHBridge;
import edu.usc.ict.nl.vhmsg.VHBridge.VRGenerate;
import edu.usc.ict.nl.vhmsg.VHBridge.VRNLU;
import edu.usc.ict.nl.vhmsg.VHBridge.VRPlaySound;
import edu.usc.ict.nl.vhmsg.VHBridge.VRSpoke;
import edu.usc.ict.vhmsg.MessageEvent;
import edu.usc.ict.vhmsg.MessageListener;

public class VHProtocol extends Protocol {
	
	private VHBridge vhBridge=null;
	private VHBridge getVHBridge() {return vhBridge;}
	private void setVHBridge(VHBridge s) {this.vhBridge=s;}
	private String vhMyself,vhOther;
	private boolean inTwoVHCharactersMode=false,usingJustVRSpeak=false;
	private long sent=0;

	private XStream nluOutputCenverter=new XStream(new StaxDriver());

	public VHProtocol(NLBus bus) throws Exception {
		super(bus);
		if (config.hasVHConfig() && config.getVhComponentId()!=null) {
			// if vhOtherSpeaker is enabled, the system listens to vrSpoke messages generated from
			// it and maps it to vrspeech messages to itself. Also it converts its own animation
			// complete events into vrSpeech messages for the other character
			vhMyself=config.getVhSpeaker();
			vhOther=config.getVhOtherSpeaker();
			if (!StringUtils.isEmptyString(vhOther)) {
				if (StringUtils.isEmptyString(vhMyself)) throw new Exception("Invalid configuration as it sets property vhOtherSpeak but not vhSpeaker.");
				inTwoVHCharactersMode=true;
				usingJustVRSpeak=!config.getSystemEventsHaveDuration();
			}
			VHBridge vhBridge=new VHBridge(config.getVhServer(), config.getVhTopic());
			setVHBridge(vhBridge);
			if (config.getNluVhListening())
				vhBridge.addMessageListenerFor("vrSpeech", createVrSpeechMessageListener());
			if (config.getDmVhListening())
				vhBridge.addMessageListenerFor("vrNLU", createVrNLUMessageListener());
			if (config.getNlgVhListening())
				vhBridge.addMessageListenerFor("vrGenerate", createVrGenerateMessageListener());
			vhBridge.addMessageListenerFor(createVrSpokeMessageListener(),"vrSpoke","sbm");
			vhBridge.addMessageListenerFor("vrSpeak", createVrSpeakMessageListener());
			MessageListener launcherListener = createVrLauncherMessagesListener();
			vhBridge.addMessageListenerFor("PlaySound", createVrPlaySoundMessageListener());
			vhBridge.addMessageListenerFor("vrKillComponent", launcherListener);
			vhBridge.addMessageListenerFor("vrAllCall", launcherListener);
			logger.info("started vh message listener in "+this.getClass().getCanonicalName());

			vhBridge.sendComponetIsAlive(config.getVhComponentId());
		} else {
			logger.error(this.getClass()+" requested to start but no VH configuration or null VhComponentId.");
		}
	}

	//vrSpoke Brad all 1370985939725-33-1 Our brains are controlled by the NPC Editor.


	protected MessageListener createVrSpeechMessageListener() {
		return new MessageListener() {

			public void messageAction(MessageEvent e)
			{
				VHBridge.VRSpeech msg=null;
				try {
					// this will fire an exception if the input message is not a vrExpress message. 
					msg=vhBridge.processVrSpeechEvent(e);
				} catch (Exception ex){
					System.out.println("MessageListener.messageAction received non vrSpeech message.");
				}
				if (msg!=null) {
					if (!inTwoVHCharactersMode || vhMyself==null || vhMyself.equals(msg.getSpeaker())) {
						if (msg.isComplete()) {
							for(Long sessionID : bus.getSessions()) {
								try {
									bus.setSpeakingStateVarForSessionAs(sessionID, false);
									bus.handleTextUtteranceEvent(sessionID, msg.getUtterance());
								} catch (Exception e1) {
									logger.error("Error processing vrSpeech message: ",e1);
								}
							}
						} else if (!usingJustVRSpeak) {
							for(Long sessionID : bus.getSessions()) {
								try {
									bus.setSpeakingStateVarForSessionAs(sessionID, true);
								} catch (Exception e1) {
									logger.error("Error processing vrSpeech message: ",e1);
								}
							}
						}
					}
				}
			}
		};
	}
	protected MessageListener createVrNLUMessageListener() {
		return new MessageListener() {

			public void messageAction(MessageEvent e)
			{
				VRNLU msg=null;
				try {
					// this will fire an exception if the input message is not a vrExpress message. 
					msg=vhBridge.processVrNLUEvent(e);
				} catch (Exception ex){
					//System.out.println("MessageListener.messageAction received non vrExpress message.");
				}
				if (!inTwoVHCharactersMode || vhMyself==null || vhMyself.equals(msg.getSpeaker())) {
					if (msg!=null) {
						if (msg.isComplete()) {
							for(Long sessionID : bus.getSessions()) {
								try {
									bus.setSpeakingStateVarForSessionAs(sessionID, false);
									String interpretation=msg.getInterpretation();
									//NLUOutput nluOutput=NLUOutput.fromObjectDump(interpretation);
									//NLUOutput nluOutput=new NLUOutput(null, ), 1, null);
									NLUOutput nluOutput=(NLUOutput) nluOutputCenverter.fromXML(interpretation);
									bus.handleNLUEvent(sessionID, new NLUEvent(nluOutput,sessionID));
								} catch (Exception e1) {
									logger.error("Error processing vrNLU event: ",e1);
								}
							}
						} else {
							for(Long sessionID : bus.getSessions()) {
								try {
									bus.setSpeakingStateVarForSessionAs(sessionID, true);
								} catch (Exception e1) {
									logger.error("Error processing vrNLU event: ",e1);
								}
							}
						}
					}
				}
			}
		};
	}
	protected MessageListener createVrGenerateMessageListener() {
		return new MessageListener() {

			public void messageAction(MessageEvent e)
			{
				VHBridge.VRGenerate msg=null;
				try {
					// this will fire an exception if the input message is not a vrExpress message. 
					msg=vhBridge.processVrGenerateEvent(e);
				} catch (Exception ex){
					//logger.warn("MessageListener.messageAction received non vrPerception message.");
				}
				if (msg!=null) {
					if (!inTwoVHCharactersMode || vhMyself==null || vhMyself.equals(msg.getAgent())) {
						try {
							handleDMSpeakEvent(msg);
						} catch (Exception e1) {
							logger.error("Error processing PML message:",e1);
						}
					}
				}
			}
		};
	}
	
	protected MessageListener createVrPlaySoundMessageListener() {
		return new MessageListener() {

			public void messageAction(MessageEvent e)
			{
				VHBridge.VRPlaySound msg=null;
				try {
					// this will fire an exception if the input message is not a vrExpress message. 
					msg=vhBridge.processVrPlaySoundEvent(e);
				} catch (Exception ex){
					//logger.warn("Error processing a VHBridge.VRPlaySound event.",ex);
				}
				if (msg!=null) {
					if (!inTwoVHCharactersMode || vhMyself==null || vhMyself.equals(msg.getAgent())) {
						try {
							handleVRPlaySoundEvent(msg);
						} catch (Exception e1) {
							logger.error("Error processing VRPlaySound message:",e1);
						}
					} else if (inTwoVHCharactersMode && !vhMyself.equals(msg.getAgent()) && !usingJustVRSpeak) {
						for(Long sessionID : bus.getSessions()) {
							try {
								bus.setSpeakingStateVarForSessionAs(sessionID, true);
							} catch (Exception e1) {
								logger.error("Error processing VRPlaySound message:",e1);
							}
						}
					}
				}
			}
		};
	}
	protected MessageListener createVrSpokeMessageListener() {
		return new MessageListener() {

			public void messageAction(MessageEvent e)
			{
				VHBridge.VRSpoke msg=null;
				try {
					// this will fire an exception if the input message is not a vrExpress message. 
					msg=vhBridge.processVrSpokeEvent(e);
				} catch (Exception ex){
					//logger.warn("MessageListener.messageAction received non vrPerception message.");
				}
				if (msg!=null) {
					if (!inTwoVHCharactersMode || vhMyself==null || vhMyself.equals(msg.getSpeaker())) {
						try {
							handleVRSpokeEvent(msg);
						} catch (Exception e1) {
							logger.error("Error processing VRSpoke message:",e1);
						}
						if (inTwoVHCharactersMode && !usingJustVRSpeak) {
							vhBridge.sendVRSpeech(msg.getText(), vhOther, sent++);
						}
					} else if (inTwoVHCharactersMode && !vhMyself.equals(msg.getSpeaker()) && !usingJustVRSpeak) {
						for(Long sessionID : bus.getSessions()) {
							try {
								bus.setSpeakingStateVarForSessionAs(sessionID, false);
								bus.handleTextUtteranceEvent(sessionID, msg.getText());
							} catch (Exception e1) {
								logger.error("Error processing vrSpoke event from "+vhOther+" into an utterance event for myself:",e1);
							}
						}
					}
				}
			}
		};
	}
	protected MessageListener createVrSpeakMessageListener() {
		return new MessageListener() {

			public void messageAction(MessageEvent e)
			{
				VHBridge.VRSpeak msg=null;
				try {
					// this will fire an exception if the input message is not a vrExpress message. 
					msg=vhBridge.processVrSpeakEvent(e);
				} catch (Exception ex){
					//logger.warn("MessageListener.messageAction received non vrPerception message.");
				}
				if (msg!=null) {
					if (inTwoVHCharactersMode && usingJustVRSpeak) {
						if (vhMyself==null || vhMyself.equals(msg.getSpeaker())) {
							vhBridge.sendVRSpeech(msg.getText(), vhOther, sent++);
						} else {
							for(Long sessionID : bus.getSessions()) {
								try {
									bus.handleTextUtteranceEvent(sessionID, msg.getText());
								} catch (Exception e1) {
									logger.error("Error processing vrSpoke event from "+vhOther+" into an utterance event for myself:",e1);
								}
							}
						}
					}
				}
			}
		};
	}
	protected MessageListener createVrLauncherMessagesListener() {
		return new MessageListener() {
			public void messageAction(MessageEvent e) {
				String componentName=config.getVhComponentId();
				Map<String, ?> map = e.getMap();
				if (map.containsKey("vrAllCall")) {
					vhBridge.sendComponetIsAlive(componentName);
				} else if (map.containsKey("vrKillComponent")) {
					String msg=(String) map.get("vrKillComponent");
					if (msg.equals(componentName) || msg.equalsIgnoreCase("all")) {
						vhBridge.sendComponentKilled(componentName);
						try {
							bus.shutdown();
						} catch (Exception e1) {
							e1.printStackTrace();
						}
						System.exit(0);
					}
				}
			}
		};
	}
	private void handleDMSpeakEvent(VRGenerate msg) throws Exception {
		for(Long sessionID : bus.getSessions()) {
			if (bus.getCharacterName4Session(sessionID)!=null) {
				DM dm=bus.getPolicyDMForSession(sessionID,false);
				if (dm!=null) {
					DMSpeakEvent event=new DMSpeakEvent(null, msg.getRequest(), sessionID, null, dm.getInformationState());
					bus.handleDMResponseEvent(event);
				}
			}
		}
	}

	private void handleVRSpokeEvent(VRSpoke msg) throws Exception {
		for(Long sessionID : bus.getSessions()) {
			NLGInterface nlg = bus.getNlg(sessionID,false);
			DM dm=bus.getPolicyDMForSession(sessionID, false);
			if (dm!=null && !dm.isSessionDone() && nlg!=null && (nlg instanceof VRSpeakSpokeTrackerInterface)) {
				String sa=((VRSpeakSpokeTrackerInterface)nlg).getSpeechActIDFromVRMessageID(msg.getID());
				if (sa!=null) { 
					dm.handleEvent(new SystemUtteranceDoneEvent(sa, sessionID));
				}
			}
		}
	}
	
	private void handleVRPlaySoundEvent(VRPlaySound msg) throws Exception {
		File f=new File(msg.getFileName());
		float length=Audio.getWavLength(f);
		for(Long sessionID : bus.getSessions()) {
			DM dm=bus.getPolicyDMForSession(sessionID, false);
			if (dm!=null && !dm.isSessionDone()) {
				dm.handleEvent(new SystemUtteranceLengthEvent(f.getName(), sessionID, length));
			}
		}
	}
	
	protected void sendVrNLU(Long sessionId,NLUOutput nluOutput) {
		NLBusConfig config=bus.getConfiguration();
		VHBridge vhBridge=getVHBridge();
		if (vhBridge!=null && config.getNluVhGenerating()) {
			String speaker=config.getVhSpeaker();
			try {
				//String nluSA=nluOutput.toObjectDump();
				String nluSA=nluOutputCenverter.toXML(nluOutput);
				vhBridge.sendVRNLU(nluSA, speaker, sessionId);
			} catch (Exception e) {
				logger.error("exception while dumping NLUOut '"+nluOutput+"' to string.", e);
			}
		}
	}

	protected void sendVrGenerate(Long sessionId,String generateID) {
		NLBusConfig config=bus.getConfiguration();
		VHBridge vhBridge=getVHBridge();
		if (vhBridge!=null && config.getDmVhGenerating()) {
			String speaker=config.getVhSpeaker();
			vhBridge.sendVrGenerate(generateID, speaker, sessionId);
		}
		
	}
	
	@Override
	public void kill() {
		if (vhBridge!=null) vhBridge.sendComponentKilled(config.getVhComponentId());
	}
	
	@Override
	public void handleDMSpeakEvent(DMSpeakEvent ev) throws Exception {
		if (ev!=null) {
			Long sessionID=ev.getSessionID();
			sendVrGenerate(sessionID, ev.getName());
		}
	}
	@Override
	public void handleNLUEvent(Long sessionId, NLUEvent ev) throws Exception {
		if (ev!=null) {
			sendVrNLU(sessionId, ev.getPayload());
		}
	}
}
