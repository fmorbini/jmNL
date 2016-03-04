package edu.usc.ict.nl.bus.events;

import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.config.DMConfig;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.util.StringUtils;


public abstract class Event {
	protected String name=null;
	protected Object payload=null;
	private Long sessionID;

	public Event(String name, Long sid,Object payload) {
		this.name=name;
		this.sessionID=sid;
		this.payload=payload;
	}
	public String getName() {return name;}
	public void setName(String name) {this.name=name;}
	public Object getPayload() {return payload;}
	public void setPayload(Object p) {this.payload=p;}
	public Long getSessionID() {return sessionID;}
	public void setSessionID(Long sid) {this.sessionID=sid;}
	
	@Override
	public String toString() {
		return getName();
	}
	@Override
	public Event clone() {return null;}

	public boolean isTimerEvent(DM dm) {
		String evName=getName();
		if (!StringUtils.isEmptyString(evName)) return (this instanceof DMInternalEvent) && evName.equals(dm.getConfiguration().getTimerEvent());
		return false;
	}
	public boolean isLoginEvent(DM dm) {
		DMConfig config=dm.getConfiguration();
		String name=getName();
		return (this instanceof NLUEvent) && !StringUtils.isEmptyString(name) && name.equalsIgnoreCase(config.getLoginEventName());
	}
	public boolean isEmptyNLUEvent(DM dm) {
		try {
			NLUConfig config=dm.getMessageBus().getNlu(dm.getSessionID()).getConfiguration();
			String name=getName();
			return (this instanceof NLUEvent) && !StringUtils.isEmptyString(name) && name.equalsIgnoreCase(config.getEmptyTextEventName());
		} catch (Exception e) {dm.getLogger().error("error while testing if nlu event is empty",e);}
		return false;
	}
	/**
	 * true for all (real) user events + login event
	 * @param ev
	 * @return
	 */
	public boolean isUserEvent() {
		return (this instanceof NLUEvent);
	}
	/**
	 * true only for real user event (no login event)
	 * @param ev
	 * @return
	 */
	public boolean isExternalUserEvent(DM dm) {
		return (this instanceof NLUEvent) && !isLoginEvent(dm);
	}
}
