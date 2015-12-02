package edu.usc.ict.nl.bus.modules;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.bus.NLBusInterface;
import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.Event;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.bus.events.NLUEvent;
import edu.usc.ict.nl.config.DMConfig;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.util.Network;
import edu.usc.ict.nl.util.XMLUtils;
import edu.usc.ict.nl.utils.LogConfig;

public abstract class DM implements DMInterface {

	protected Logger logger = null;

	private NLBusInterface messageBus;
	private DMConfig configuration;
	private Long sessionID;
	private String personalSessionID;
	private ChatLog chatLog;
	private boolean pausedEventProcessing=false;


	@Override
	public Logger getLogger() {return this.logger;}

	public DM(DMConfig c) {
		this.configuration=c;
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );
	}

	@Override
	public boolean getPauseEventProcessing() {return this.pausedEventProcessing;}
	@Override
	public void setPauseEventProcessing(boolean p) {
		logger.info("Setting pause event processing to: "+p);
		this.pausedEventProcessing=p;
	}

	public NLBusInterface getMessageBus() {return messageBus;}
	public void setMessageBus(NLBusInterface messageBus) {this.messageBus = messageBus;}

	public DMConfig getConfiguration() {return configuration;}
	public void setConfiguration(DMConfig c) {this.configuration = c;}

	@Override
	public String getPersonalSessionID() {return personalSessionID;}
	@Override
	public void setPersonalSessionID(String pid) {
		logger.info("Setting personal session id to: "+pid+ " for session id: "+getSessionID());
		this.personalSessionID=pid;
		setSessionID(getSessionID());
	}
	@Override
	public Long getSessionID(){return sessionID;}
	@Override
	public void setSessionID(long sessionID) {
		this.sessionID=sessionID;
		logger=Logger.getLogger(DM.class.getName()+"-"+sessionID+"-"+System.currentTimeMillis()); // get logger for this session
		Logger baseLogger = Logger.getLogger(DM.class.getName());
		logger.setLevel(baseLogger.getLevel());
		Enumeration appenders = logger.getRootLogger().getAllAppenders();
		while(appenders.hasMoreElements()) {
			Appender appender = (Appender)appenders.nextElement();
			if (appender instanceof FileAppender) {
				FileAppender ofa=(FileAppender) appender;
				try {
					Constructor<? extends FileAppender> c = ofa.getClass().getConstructor(Layout.class,String.class,boolean.class);
					FileAppender fa=c.newInstance(ofa.getLayout(),ofa.getFile(),ofa.getAppend());
					logger.addAppender(fa);
					System.err.println(ofa.getFile());
					fa.setFile(ofa.getFile()+"-"+getIDPortionLogFileName());
					System.err.println(fa.getFile());
					logger.removeAppender(ofa);
					fa.activateOptions();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		logger.info("Finished setting session id to: "+getSessionID());
	}

	private String getIDPortionLogFileName() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("[yyyy_MM_dd]-[HH_mm_ss]");

		String baseFileName=Network.getHostname()+"-"+System.getProperty("user.name")+"-"+sdf.format(cal.getTime())+"-sid="+getSessionID()+"-pid="+getPersonalSessionID();
		return baseFileName;
	}

	@Override
	public File getCurrentChatLogFile() {return (chatLog!=null)?chatLog.getFile():null;}
	private class ChatLogFile {
		BufferedWriter pipe;
		File file;
		public ChatLogFile(File f) throws IOException {
			pipe=new BufferedWriter(new FileWriter(f));
			file=f;
		}
		public void write(String string) throws IOException {
			if (pipe!=null) pipe.write(string);
		}
		public void close() throws IOException {
			pipe.close();
			file=null;
			pipe=null;
		}
		public void flush() throws IOException {
			if (pipe!=null) pipe.flush();
		}
		public File getFile() {
			return file;
		}

	}
	private class ChatLog {
		private ChatLogFile out=null;
		private Semaphore chatLogLock=new Semaphore(1);
		private long startTime;
		int line=0;

		public void closeChatLog() throws IOException, InterruptedException {
			if (out!=null) {
				chatLogLock.acquire();
				out.write("</log>\n");
				out.close();
				out=null;
				chatLogLock.release();
			}
		}
		public File getFile() {
			return out.getFile();
		}
		private ChatLog() {
			try {
				closeChatLog();
				startTime=System.currentTimeMillis();
				line=0;
				String logFileName=getConfiguration().nlBusConfig.getChatLog();

				String baseFileName=logFileName+"-"+getIDPortionLogFileName();
				File f=new File(baseFileName+".xml");

				File p=f.getParentFile();
				if (p!=null && !p.exists()) p.mkdir();

				int i=1;
				while(f.exists()) {
					f=new File(baseFileName+"."+(i++)+".xml");		
				}
				out = new ChatLogFile(f);
				out.write("<?xml version=\"1.0\"?>\n");
				out.write("<?xml-stylesheet type=\"text/xsl\" href=\"style.xsl\"?>\n");
				out.write("<log id=\""+getSessionID()+"\">\n");
				out.flush();
			} catch (Exception e) {e.printStackTrace();}
		}
		private void writeNLGEventToLog(NLGEvent ev) throws Exception {
			if (out!=null) {
				DMSpeakEvent dmSpeakEvent=ev.getPayload();
				String sysText=ev.getName();
				++line;
				chatLogLock.acquire();
				float ts=(float)(System.currentTimeMillis()-startTime)/(float)1000;
				DecimalFormat df = new DecimalFormat("#.00");
				String tagType="system";
				out.write("<"+tagType+" number=\""+line+"\" delta=\""+df.format(ts)+"\" id=\""+dmSpeakEvent+"\">\n");

				//byte[] bytes = text.getBytes("UTF-8");
				//text=new String(bytes, "UTF-8");

				out.write("<text>"+XMLUtils.escapeStringForXML(sysText)+"</text>\n");
				out.write("</"+tagType+">\n");
				out.flush();
				chatLogLock.release();
			}
		}
		private void writeGenericEventToLog(Event ev) throws Exception {
			if (out!=null) {
				String sysText=ev.getName();
				++line;
				chatLogLock.acquire();
				float ts=(float)(System.currentTimeMillis()-startTime)/(float)1000;
				DecimalFormat df = new DecimalFormat("#.00");
				String tagType="other";
				out.write("<"+tagType+" number=\""+line+"\" delta=\""+df.format(ts)+"\" id=\""+sysText+"\">\n");

				//byte[] bytes = text.getBytes("UTF-8");
				//text=new String(bytes, "UTF-8");

				out.write("<text>"+XMLUtils.escapeStringForXML(ev.getClass().getCanonicalName())+"</text>\n");
				out.write("</"+tagType+">\n");
				out.flush();
				chatLogLock.release();
			}
		}
		private void writeNLUEventToLog(NLUOutput nlu) throws Exception {
			if ((out!=null) && (nlu!=null)) {
				++line;
				chatLogLock.acquire();
				float ts=(float)(System.currentTimeMillis()-startTime)/(float)1000;
				DecimalFormat df = new DecimalFormat("#.00");
				String tagType="user";
				out.write("<"+tagType+" number=\""+line+"\" delta=\""+df.format(ts)+"\">\n");

				//byte[] bytes = text.getBytes("UTF-8");
				//text=new String(bytes, "UTF-8");

				String nluInputText=nlu.getText();
				out.write("<text>"+((nluInputText!=null)?XMLUtils.escapeStringForXML(nluInputText):nlu.getId())+"</text>\n");
				out.write(nlu.toXml());
				out.write("</"+tagType+">\n");
				out.flush();
				chatLogLock.release();
			}
		}
		private void writeInformationStateChangeEventToLog(Collection<DialogueOperatorEffect> changes,String label) throws Exception {
			chatLogLock.acquire();
			if (out!=null && changes!=null && !changes.isEmpty()) {
				out.write("<state type=\""+label+"\">\n");
				ArrayList<DialogueOperatorEffect> sortedChanges = new ArrayList<DialogueOperatorEffect>(changes);
				Collections.sort(sortedChanges,new Comparator<DialogueOperatorEffect>() {
					@Override
					public int compare(DialogueOperatorEffect o1,DialogueOperatorEffect o2) {
						if (o1!=null) return o1.compareUsingStrings(o2);
						else return -1; 
					}
				});
				for(DialogueOperatorEffect e:sortedChanges) {
					if (!e.isAssignment()) throw new Exception("un-expected KB content.");
					else {
						Object value = e.getAssignedExpression();
						if (value!=null) value=XMLUtils.escapeStringForXML(value.toString());
						out.write("<assign var=\""+e.getAssignedVariable()+"\" value=\""+value+"\"/>\n");
					}
				}
				out.write("</state>\n");
				out.flush();
			}
			chatLogLock.release();
		}

	}
	private Map<String,String> previousInfoStateContent=null;
	private Collection<DialogueOperatorEffect> extractChangesInInformationState() throws Exception {
		Collection<DialogueOperatorEffect> ret=null;
		DialogueKBInterface kb = getInformationState();
		LinkedHashMap<String, Collection<DialogueOperatorEffect>> contents = kb.dumpKBTree();
		Map<String,DialogueOperatorEffect> content=null;
		if (contents!=null) {
			for(Collection<DialogueOperatorEffect>c:contents.values()) {
				if (c!=null) {
					if (content==null) content=new LinkedHashMap<String, DialogueOperatorEffect>();
					for(DialogueOperatorEffect e:c) {
						DialogueKBFormula v=e.getAssignedVariable();
						String vName=v.toString();
						if (v!=null && !content.containsKey(vName)) {
							content.put(vName, e);
						}
					}
				}
			}
		}

		if (previousInfoStateContent!=null) {
			for (String name:content.keySet()) {
				if (previousInfoStateContent.containsKey(name)) {
					String oldValue=previousInfoStateContent.get(name);
					Object newValueO=content.get(name).getAssignedExpression();
					String newValue=(newValueO!=null)?newValueO.toString():"null";
					if (oldValue.equals(newValue)) continue;
				}
				if (ret==null) ret=new ArrayList<DialogueOperatorEffect>();
				ret.add(content.get(name));
			}
			previousInfoStateContent.clear();
		}

		// save the new information state
		for (String name:content.keySet()) {
			Object newValueO=content.get(name).getAssignedExpression();
			String newValue=(newValueO!=null)?newValueO.toString():"null";
			if (previousInfoStateContent==null) previousInfoStateContent=new LinkedHashMap<String, String>();
			previousInfoStateContent.put(name, newValue);
		}

		// if no assignments to ret, then there was no previousInfoStateContent, so take all the current content. 
		if (ret==null) ret=content.values();

		return ret;
	}

	@Override
	public void logEventInChatLog(Event ev) {
		if (!configuration.nlBusConfig.getLoggingEventsInChatLog())
			return;
		try {
			if (chatLog==null) chatLog=new ChatLog();
			if (ev instanceof NLUEvent) {
				Collection<DialogueOperatorEffect> changes=extractChangesInInformationState();
				chatLog.writeInformationStateChangeEventToLog(changes, "BEFORE NLU EVENT");

				NLUOutput nlu = ((NLUEvent) ev).getPayload();
				chatLog.writeNLUEventToLog(nlu);
			} else if (ev instanceof DMSpeakEvent) {
			} else if (ev instanceof NLGEvent) {
				Collection<DialogueOperatorEffect> changes=extractChangesInInformationState();
				chatLog.writeInformationStateChangeEventToLog(changes, "BEFORE NLG EVENT");
				chatLog.writeNLGEventToLog((NLGEvent) ev);
			} else {
				Collection<DialogueOperatorEffect> changes=extractChangesInInformationState();
				chatLog.writeInformationStateChangeEventToLog(changes, "BEFORE Other EVENT");
				chatLog.writeGenericEventToLog(ev);
			}
		} catch (Exception e) {e.printStackTrace();}
	}

	@Override
	public List<Event> handleEvent(Event ev) {
		if (!getPauseEventProcessing()) {
			logEventInChatLog(ev);
		} else {
			logger.info("Received event while processing paused, not logging: "+ev);
		}
		return null;
	}
	@Override
	public void kill() {
		logger.info("Received kill command.");
		try {
			if (chatLog!=null) chatLog.closeChatLog();
		} catch (Exception e) {e.printStackTrace();}
		logger.removeAllAppenders();
	}

	/**
	 * Checks whether or not forms are preferred for the given speech act
	 * @param speechAct Speech act node
	 * @param scxml SCXML runner
	 * @return True if forms are preferred for given speech act, false otherwise
	 * @throws Exception 
	 */
	public static boolean isFormPreferred(DMSpeakEvent ev,DialogueKBInterface context) throws Exception {
		// first attemp to get the local context
		Object masterPreferForms=context.get(NLBusBase.preferFormsVariableName);
		if (masterPreferForms!=null && (masterPreferForms.toString().equalsIgnoreCase("'yes'") || masterPreferForms.toString().equalsIgnoreCase("true"))) return true;
		else {
			Map parametersOfSpeechAct=(Map) ev.getPayload();
			if (parametersOfSpeechAct!=null) {
				Object varPreferForms = parametersOfSpeechAct.get(NLBusBase.preferFormsVariableName);
				return (varPreferForms != null && (varPreferForms.equals("yes") || masterPreferForms.equals(true))); 
			} else return false;
		}
	}

	@Override
	public List<DMSpeakEvent> getAllPossibleSystemLines() throws Exception {
		return null;
	}
	@Override
	public List<DMSpeakEvent> getAllAvailableSystemLines() throws Exception {
		return null;
	}
}
