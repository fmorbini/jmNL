package edu.usc.ict.nl.bus.protocols;

import java.util.ArrayList;
import java.util.List;

import edu.usc.ict.nl.bus.NLBus;
import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.bus.special_variables.SpecialEntitiesRepository;
import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.vhmsg.VHBridgewithMinat;
import edu.usc.ict.nl.vhmsg.VHBridgewithMinat.Minat;
import edu.usc.ict.nl.vhmsg.VHBridgewithMinat.Minat.Decision;
import edu.usc.ict.vhmsg.MessageEvent;
import edu.usc.ict.vhmsg.MessageListener;

public class VHMinatProtocol extends Protocol {
	public static final String minatTriageVarName="minatTriage";
	public static final String minatLabelsVarName="minatLabels";
	private VHBridgewithMinat vhBridge=null;

	public VHMinatProtocol(NLBus bus) throws Exception {
		super(bus);
		if (config.hasVHConfig() && config.getMinatListening()) {
			vhBridge=new VHBridgewithMinat(config.getVhServer(), config.getVhTopic());
			vhBridge.addMessageListenerFor("minat", createMinatMessageListener());
			svs=new SpecialEntitiesRepository(config);
			new SpecialVar(svs,minatTriageVarName,"BBN Minat triage class.","TR3",String.class);
			new SpecialVar(svs,minatLabelsVarName,"BBN Minat distress labels.","null",List.class);
		} else {
			logger.error(this.getClass()+" requested to start but no VH/minat configuration.");
		}
	}
	
	protected MessageListener createMinatMessageListener() {
		return new MessageListener() {

			public void messageAction(MessageEvent e)
			{
				VHBridgewithMinat.Minat msg=null;
				try {
					// this will fire an exception if the input message is not a vrExpress message. 
					msg=vhBridge.processMinatEvent(e);
				} catch (Exception ex){
					//logger.warn("MessageListener.messageAction received non vrPerception message.");
				}
				if (msg!=null) {
					try {
						handleMinatEvent(msg);
					} catch (Exception e1) {
						logger.error("Error processing PML message:",e1);
					}
				}
			}
		};
	}
	
	/**
	 * Handles Minat event from BBN Minat
	 * @param msg
	 * @throws Exception
	 */
	public void handleMinatEvent(Minat msg) throws Exception {
		if (msg!=null) {
			for (Long sessionId : bus.getSessions()) {
				if (bus.getCharacterName4Session(sessionId)!=null) {
					DM dm=bus.getPolicyDMForSession(sessionId,false);
					if (dm!=null) {
						DialogueKB informationState = dm.getInformationState();
						if (informationState!=null) {
							Decision triage=msg.getTriage();
							if (triage!=null) informationState.setValueOfVariable(minatTriageVarName, triage.getName(),ACCESSTYPE.AUTO_OVERWRITEAUTO);
							List<Decision> labels=msg.getLabels();
							if (labels!=null) {
								Object v = informationState.getValueOfVariable(minatLabelsVarName,ACCESSTYPE.AUTO_OVERWRITEAUTO,null);
								if (v==null || !(v instanceof List)) informationState.setValueOfVariable(minatLabelsVarName, v=new ArrayList<String>(), ACCESSTYPE.AUTO_OVERWRITEAUTO);
								((List) v).clear();
								for(Decision d:labels) if (d!=null) ((List)v).add(d.getName());
							}
						}
					}
				}
			}
		}
	}
	@Override
	public void kill() {
		if (vhBridge!=null) vhBridge.sendComponentKilled(config.getVhComponentId());
	}
}
