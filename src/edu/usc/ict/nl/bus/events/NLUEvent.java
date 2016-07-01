package edu.usc.ict.nl.bus.events;

import java.util.HashMap;
import java.util.Map;

import edu.usc.ict.nl.nlu.NLUOutput;

public class NLUEvent extends UserEvent {

	protected NLUEvent(String speechActID,Long sid,NLUOutput nlu) {
		super(speechActID,sid,nlu);
	}
	public NLUEvent(String speechActID, long sid) {
		this(speechActID,sid,null);
	}
	public NLUEvent(NLUOutput nlu, long sid) {
		this(nlu.getId(),sid,nlu);
	}
	
	public NLUEvent clone(NLUEvent sourceEvent) {
		return new NLUEvent(sourceEvent.getName(),sourceEvent.getSessionID(),sourceEvent.getPayload());
	}
	
	@Override
	public NLUOutput getPayload() {return (NLUOutput) payload;}
	@Override
	public void setName(String name) {
		super.setName(name);
		NLUOutput sa=getPayload();
		if (sa!=null) sa.setId(name);
	}

	public void addVariableToPayload(String name, Object value) {
		if (payload!=null && (payload instanceof NLUOutput)) {
			Map<String,Object> varMap = (Map<String, Object>) ((NLUOutput)payload).getPayload();
			if (varMap==null) ((NLUOutput)payload).setPayload(varMap=new HashMap<String, Object>());
			varMap.put(name,value);
		}
	}
}
