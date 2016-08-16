package edu.usc.ict.nl.nlg.echo;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.bus.events.DMInterruptionRequest;
import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.bus.modules.DMEventsListenerInterface;
import edu.usc.ict.nl.bus.modules.NLG;
import edu.usc.ict.nl.config.NLGConfig;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.InformationStateInterface;
import edu.usc.ict.nl.kb.template.NoTemplateFoundException;
import edu.usc.ict.nl.kb.template.NoTemplateSelectionException;
import edu.usc.ict.nl.kb.template.PrimaryTemplateDefinitionException;
import edu.usc.ict.nl.kb.template.TemplateProcessing;
import edu.usc.ict.nl.kb.template.TemplateText;
import edu.usc.ict.nl.kb.template.util.TemplateVerifier;
import edu.usc.ict.nl.nlg.SpeechActWithProperties;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;

public class EchoNLG extends NLG {

	protected final static Map<String,EchoNLGData> allNLGData=new HashMap<>();

	public EchoNLG(NLGConfig c) {
		this(c,true);
	}

	public EchoNLG(NLGConfig c, boolean loadData) {
		super(c);
		if (loadData) {
			try {
				reloadData(false);
			} catch (Exception e) {e.printStackTrace();}
		}
	}

	@Override
	public Map<String, List<SpeechActWithProperties>> getAllLines() {
		return getValidSpeechActs();
	}
	
	private String getContentDir() {
		return getConfiguration().getNlBusConfigNC().getCharacterContentRoot();
	}

	public EchoNLGData getData() {
		String dir=getContentDir();
		return allNLGData.get(dir);
	}
	private Map<String, List<SpeechActWithProperties>> getValidSpeechActs() {
		EchoNLGData data=getData();
		return data!=null?data.getValidSpeechActs():null;
	}
	private Map<String, List<Pair<String, String>>> getResources() {
		EchoNLGData data=getData();
		return data!=null?data.getResources():null;
	}
	private Map<String, List<Pair<String, String>>> getFormResponses() {
		EchoNLGData data=getData();
		return data!=null?data.getFormResponses():null;
	}

	@Override
	public Float getDurationOfThisDMEvent(Long sessionID, NLGEvent ev) throws Exception {
		if (getConfiguration().getDmConfigNC().getSystemEventsHaveDuration()) {
			String text=ev.getName();
			if (text!=null) {
				String[] words=text.split("[\\s]+");
				// average of 0.4 seconds per word.
				if (words!=null) return ((float)words.length)*0.4f;
			}
		}
		return 0f;
	}
	@Override
	public boolean isResource(Long sessionID, NLGEvent ev) throws Exception {
		Map<String, List<Pair<String, String>>> resources = getResources();
		if (resources!=null) {
			if (resources.containsKey(ev.getName())) {
				return true;
			}
		}
		return false;
	}


	@Override
	public NLGEvent doNLG(Long sessionID,DMSpeakEvent ev,SpeechActWithProperties line,boolean simulate) throws Exception {
		String evName=ev.getName();
		DMEventsListenerInterface nl = getNLModule();
		DM dm = (nl!=null)?nl.getDM(sessionID):null;
		DialogueKBInterface is = (dm!=null)?dm.getInformationState():null;

		if (line==null) line=pickLineForSpeechAct(sessionID, evName, is, simulate);
		NLGEvent output=processPickedLine(line, sessionID,evName, is, simulate);
		if (output!=null) output.setPayload(ev);
		if (output==null || StringUtils.isEmptyString(output.getName())) {
			if (!getConfiguration().getAllowEmptyNLGOutput()) return null;
		}

		if (logger.isTraceEnabled()) {
			logger.trace("Echo NLG, returning: "+output);
		}

		return output;
	}

	@Override
	public boolean canGenerate(Long sessionID,DMSpeakEvent ev) throws Exception {
		String evName=ev.getName();
		DMEventsListenerInterface nl = getNLModule();
		DM dm = (nl!=null)?nl.getDM(sessionID):null;
		DialogueKBInterface is = (dm!=null)?dm.getInformationState():null;
		SpeechActWithProperties out = pickLineForSpeechActIfThere(sessionID, evName, is, false);
		return out!=null;
	}
	
	public static HashMap<String,String> getTemplateParams(InformationStateInterface infoState, String text) {
		Collection<String> keys = TemplateProcessing.getTemplateKeys(text);
		HashMap<String,String> ret=new HashMap<String, String>();
		for(String key : keys) {
			String value = TemplateProcessing.getTemplateValueForKey(infoState, key);
			ret.put(key, value);
		}
		return ret;
	}

	public static String resolveTemplates(String text,InformationStateInterface is) throws Exception {
		try {
			TemplateText template = new TemplateText(text);
			HashMap<String,String> templateParams = getTemplateParams(is, text);
			String result = template.applySelection(templateParams);
			return result;
		} catch (NoTemplateFoundException e) {
		} catch (NoTemplateSelectionException e) {
			throw new Exception("Unknown template in: "+text.substring(0, 60));
		} catch (PrimaryTemplateDefinitionException e) {
			throw new Exception("Multiple or no non ~ templates in: "+text.substring(0, 60));
		}
		return text;
	}

	private SpeechActWithProperties pickLineForSpeechActIfThere(Long sessionID, String sa, DialogueKBInterface is, boolean simulate) throws Exception {
		Map<String, List<SpeechActWithProperties>> vsas = getValidSpeechActs();
		Map<String, List<Pair<String, String>>> resources = getResources();
		if (vsas!=null && vsas.containsKey(sa)) {

			List<SpeechActWithProperties> ts=vsas.get(sa);
			// in simulate mode, do template resolution on all paraphrases.
			if (simulate && (ts!=null) && !ts.isEmpty()) {
				TemplateVerifier tv=new TemplateVerifier();
				for(SpeechActWithProperties t:ts) {
					try {
						if (!tv.verify(t.getText())) throw new Exception("Error in (S) templates in: '"+t+"'");
					} catch (Exception e) {
						throw new Exception("Error in (S) templates in: '"+t+"'");
					}
				}
			}
			SpeechActWithProperties line=(SpeechActWithProperties)getConfiguration().getPicker().pick(sessionID, sa,ts);
			//StringWithProperties line=(StringWithProperties) NLBusBase.pickEarliestUsedOrStillUnused(sessionID, ts);
			return line;
		} else if (resources!=null && resources.containsKey(sa)) {
			List<Pair<String,String>> rs=resources.get(sa);
			if (simulate && (rs!=null) && !rs.isEmpty()) {
				TemplateVerifier tv=new TemplateVerifier();
				for(Pair<String,String> t:rs) {
					String msg=t.getFirst();
					if (!StringUtils.isEmptyString(msg)) {
						try {
							if (!tv.verify(msg)) throw new Exception("Error in (R) templates in: '"+msg+"'");
						} catch (Exception e) {
							throw new Exception("Error in (R) templates in: '"+t+"'");
						}
					}
				}
			}
			Pair<String,String> r=(Pair<String, String>) NLBusBase.pickEarliestUsedOrStillUnused(null, rs);
			SpeechActWithProperties ret = new SpeechActWithProperties();
			ret.setText(r.getFirst());
			ret.setProperty(NLG.PROPERTY_URL, r.getSecond());
			return ret;
		}
		return null;
	}
	protected SpeechActWithProperties pickLineForSpeechAct(Long sessionID, String sa, DialogueKBInterface is, boolean simulate) throws Exception {
		SpeechActWithProperties ret = pickLineForSpeechActIfThere(sessionID, sa, is, simulate);
		if (ret==null) {
			ret = new SpeechActWithProperties();
			ret.setText(sa);
		}
		return ret;
	}

	protected NLGEvent buildOutputEvent(String text,Long sessionID,DMSpeakEvent sourceEvent) {
		return new NLGEvent(text, sessionID, sourceEvent);
	}

	protected NLGEvent processPickedLine(SpeechActWithProperties line,Long sessionID,String sa, DialogueKBInterface is, boolean simulate) throws Exception {
		NLGEvent result=null;
		if (line!=null) {
			Map<String, List<SpeechActWithProperties>> vsas = getValidSpeechActs();
			Map<String, List<Pair<String, String>>> resources = getResources();
			Map<String, List<Pair<String, String>>> formResponses = getFormResponses();
			if (vsas!=null && vsas.containsKey(sa)) {
				String text=line.getText();
				//String text=(String) FunctionalLibrary.pickRandomElement(ts);

				if (!StringUtils.isEmptyString(text)) {
					text=resolveTemplates(text, is);
				}
				if (formResponses!=null && formResponses.containsKey(sa)) {
					text+="\n";
					for(Pair<String,String> rst:formResponses.get(sa)) {
						String responseText=rst.getSecond();
						responseText=resolveTemplates(responseText, is);
						text+="\n"+rst.getFirst()+": "+responseText;
					}
				}
				result=buildOutputEvent(null, sessionID, null);
				result.setName(text);
			} else if (resources!=null && resources.containsKey(sa)) {
				String text="";
				if (line!=null) {
					String rt=line.getText();
					if (!StringUtils.isEmptyString(rt)) text+=resolveTemplates(rt, is)+"\n";
					text+=line.getProperty(NLG.PROPERTY_URL);
				}
				result=buildOutputEvent(null, sessionID, null);
				result.setName(text);
			} else {
				String text=line.getText();
				result=buildOutputEvent(null, sessionID, null);
				result.setName(text);
			}
		}
		return result;
	}


	@Override
	public void interrupt(DMInterruptionRequest ev) throws Exception {
		super.interrupt(ev);
	}

	@Override
	public void reloadData(boolean forceReload) throws Exception {
		logger.info("re-loading data.");
		String dir = getConfiguration().getNlBusConfigNC().getCharacterContentRoot();
		if (forceReload || !allNLGData.containsKey(dir)) {
			logger.info(" forcing reload of data.");
			EchoNLGData data = new EchoNLGData(getConfiguration());
			allNLGData.put(dir, data);
		} else {
			logger.info(" not doing anything as data was already loaded.");
		}
		logger.info("done loading data.");
	}

}
