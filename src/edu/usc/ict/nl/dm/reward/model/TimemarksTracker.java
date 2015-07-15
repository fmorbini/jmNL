package edu.usc.ict.nl.dm.reward.model;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.usc.ict.nl.util.StringUtils;

public class TimemarksTracker {
	public enum TYPES {SAY,ENTER,DONE}

	private Logger logger=null;
	/**
	 * keys: operators,types,parameter
	 */
	private Map<String,Map<TYPES,Map<String,Long>>> marks=null;

	public TimemarksTracker(Logger logger) {
		this.logger=logger;
	}

	public void setMark(String op, TYPES type,String p) {
		switch (type) {
		case SAY:
		case DONE:
		case ENTER:
			if (!StringUtils.isEmptyString(op)) {
				Map<String, Long> tT =getTimeMarksForOperatorAndType(op,type);
				long time=System.currentTimeMillis();
				if (logger.isInfoEnabled()) logger.info("setting mark "+type+"("+p+") for "+op+" to "+time);
				tT.put(p, time);
			} else {
				logger.error("empty operator in set time mark call.");
			}
		default:
			logger.error("error while setting time mark, unhandled type: "+type);
		}
	};
	
	private Map<String, Long> getTimeMarksForOperatorAndType(String op, TYPES type) {
		if (marks==null) marks=new HashMap<String, Map<TYPES,Map<String,Long>>>();
		Map<TYPES, Map<String, Long>> opT = marks.get(op);
		if (opT==null) marks.put(op, opT=new HashMap<TimemarksTracker.TYPES, Map<String,Long>>());
		Map<String, Long> tT = opT.get(type);
		if (tT==null) opT.put(type, tT=new HashMap<String, Long>());
		return tT;
	}

	public Long getLastTimeMark(String op,TYPES type,String p) {
		Map<String, Long> tT =getTimeMarksForOperatorAndType(op,type);
		Long time=tT.get(p);
		if (logger.isInfoEnabled()) logger.info("getting mark "+type+"("+p+") for "+op+" to "+time);
		return time;
	}
}
