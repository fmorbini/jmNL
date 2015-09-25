package edu.usc.ict.nl.nlg.echo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

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
import edu.usc.ict.nl.utils.ExcelUtils;

public class EchoNLG extends NLG {

	Map<String,List<SpeechActWithProperties>> validSpeechActs;
	Map<String,List<Pair<String,String>>> formsResponses=null;
	Map<String,List<Pair<String,String>>> resources;
	
	public EchoNLG(NLGConfig c) {
		super(c);
		try {
			reloadData();
		} catch (Exception e) {e.printStackTrace();}
	}
	
	@Override
	public Map<String, List<SpeechActWithProperties>> getAllLines() {
		return validSpeechActs;
	}
	
	private void loadSystemResources() throws Exception {
		NLGConfig config=getConfiguration();
		resources=getResources(config.nlBusConfig.getSystemResources(), 0);
	}

	private void loadSystemForms() throws Exception {
		NLGConfig config=getConfiguration();
		if (config.getDisplayFormAnswerInNlg()) formsResponses=getFormsResponses(config.nlBusConfig.getSystemForms());
	}

	private void loadSystemUtterances() throws Exception {
		NLGConfig config=getConfiguration();
		validSpeechActs=extractMappingBetweenTheseTwoColumnsWithProperties(config.nlBusConfig.getSystemUtterances(), 0, 4, 5,6,-1,true);
		if (!StringUtils.isEmptyString(config.nlBusConfig.getNvbs())) {
			File nvbFile=new File(config.nlBusConfig.getNvbs());
			if (nvbFile.exists()) {
				Map<String, List<SpeechActWithProperties>> nvb = extractMappingBetweenTheseTwoColumnsWithProperties(config.nlBusConfig.getNvbs(), 0, 4, 5,6,-1,true);
				validSpeechActs.putAll(nvb);
			}
		}
		if (getConfiguration().getIsAsciiNLG()) normalizeToASCII(validSpeechActs);
		if (getConfiguration().getIsNormalizeBlanksNLG()) normalizeBLANKS(validSpeechActs);
	}
	
	public static Map<String,List<SpeechActWithProperties>> extractMappingBetweenTheseTwoColumnsWithProperties(String file,int skip,int keyColumn,int valueColumn,int startPropertyColumn,int endPropertyColumn,boolean cleanupSpaces) throws Exception {
		Map<String,List<SpeechActWithProperties>> ret=new LinkedHashMap<String, List<SpeechActWithProperties>>();
		Sheet sheet = ExcelUtils.getSpreadSheet(file, 0);
		if (sheet != null)
		{
			String lastKey="";
			Map<Integer, String> ps=null;
			for(Iterator<Row> rowIter = sheet.rowIterator(); rowIter.hasNext(); ) {
				Row row = rowIter.next();
				int rownum=row.getRowNum();
				// skip first row
				if (rownum>skip) {
					for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
						Cell cell = cellIter.next();
						if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
							int column=cell.getColumnIndex();
							if (column==keyColumn) {
								String tmp=cell.getStringCellValue();
								if (cleanupSpaces) tmp=StringUtils.cleanupSpaces(tmp);
								if (!StringUtils.isEmptyString(tmp)) lastKey=tmp;
							} else if (column==valueColumn) {
								String value=cell.getStringCellValue();
								if (cleanupSpaces) value=StringUtils.cleanupSpaces(value);
								if (!StringUtils.isEmptyString(value)) {
									List<SpeechActWithProperties> list = ret.get(lastKey);
									if (list==null) ret.put(lastKey,list=new ArrayList<SpeechActWithProperties>());
									Map<Integer,String> lps = ExcelUtils.extractRowAndColumnWiseDataWithColumns(file, 0, rownum, startPropertyColumn, endPropertyColumn, true, true);
									SpeechActWithProperties rv=new SpeechActWithProperties();
									rv.setText(value);
									if (ps!=null && lps!=null) {
										for(Integer pc:ps.keySet()) {
											String pv=lps.get(pc);
											String pname=ps.get(pc);
											if (!StringUtils.isEmptyString(pv) && !StringUtils.isEmptyString(pname)) {
												rv.setProperty(pname,pv);
											}
										}
									}
									rv.setRow(rownum);
									rv.setSA(lastKey);
									list.add(rv);
								}
							}
						}
					}
				} else {
					ps = ExcelUtils.extractRowAndColumnWiseDataWithColumns(file, 0, 0, startPropertyColumn, endPropertyColumn, true, true);
					if (ps!=null && endPropertyColumn<0) for(Integer c:ps.keySet()) if (c>endPropertyColumn) endPropertyColumn=c;
				}
			}
		}
		return ret;
	}

	public void normalizeToASCII(Map<String,List<SpeechActWithProperties>> utterances) {
		if (utterances!=null) {
			for(List<SpeechActWithProperties> utts:utterances.values()) {
				if (utts!=null) {
					for(SpeechActWithProperties i:utts) {
						String ci=i.getText();
						String ni=StringUtils.flattenToAscii(ci);
						if (!ci.equals(ni)) {
							i.setText(ni);
							logger.warn("normalized to ASCII in line("+i.getProperty(NLG.PROPERTY_ROW)+"): '"+i+"' to '"+ni+"'");
						}
					}
				}
			}
		}
	}
	public void normalizeBLANKS(Map<String,List<SpeechActWithProperties>> utterances) {
		if (utterances!=null) {
			for(List<SpeechActWithProperties> utts:utterances.values()) {
				if (utts!=null) {
					for(SpeechActWithProperties i:utts) {
						String ci=i.getText();
						String ni=StringUtils.cleanupSpaces(ci);
						if (!ci.equals(ni)) {
							i.setText(ni);
							logger.warn("normalized BLANKS in line("+i.getProperty(NLG.PROPERTY_ROW)+"): '"+i+"' to '"+ni+"'");
						}
					}
				}
			}
		}
	}
	
	@Override
	public Float getDurationOfThisDMEvent(Long sessionID, NLGEvent ev) throws Exception {
		if (getConfiguration().nlBusConfig.getSystemEventsHaveDuration()) {
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
		if (resources!=null) {
			if (resources.containsKey(ev.getName())) {
				return true;
			}
		}
		return false;
	}
	

	@Override
	public NLGEvent doNLG(Long sessionID,DMSpeakEvent ev,SpeechActWithProperties line,boolean simulate) throws Exception {
		NLGEvent result=null;
		String evName=ev.getName();
    	DMEventsListenerInterface nl = getNLModule();
    	DM dm = (nl!=null)?nl.getPolicyDMForSession(sessionID):null;
    	DialogueKBInterface is = (dm!=null)?dm.getInformationState():null;

    	if (line==null) line=pickLineForSpeechAct(sessionID, evName, is, simulate); 
    	String text=processPickedLine(line, sessionID, evName, is, simulate);
		if (StringUtils.isEmptyString(text)) {
			if (!getConfiguration().getAllowEmptyNLGOutput()) return null;
		}

		result=new NLGEvent(text, sessionID, ev);

		if (logger.isTraceEnabled()) {
			logger.trace("Echo NLG, returning: "+result);
		}

		return result;
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


	protected SpeechActWithProperties pickLineForSpeechAct(Long sessionID, String sa, DialogueKBInterface is, boolean simulate) throws Exception {
		if (validSpeechActs!=null && validSpeechActs.containsKey(sa)) {

			List<SpeechActWithProperties> ts=validSpeechActs.get(sa);
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
	protected String processPickedLine(SpeechActWithProperties line,Long sessionID, String sa, DialogueKBInterface is, boolean simulate) throws Exception {
		if (line!=null) {
			if (validSpeechActs!=null && validSpeechActs.containsKey(sa)) {
				String text=line.getText();
				//String text=(String) FunctionalLibrary.pickRandomElement(ts);

				if (!StringUtils.isEmptyString(text)) {
					text=resolveTemplates(text, is);
				}
				if (formsResponses!=null && formsResponses.containsKey(sa)) {
					text+="\n";
					for(Pair<String,String> rst:formsResponses.get(sa)) {
						String responseText=rst.getSecond();
						responseText=resolveTemplates(responseText, is);
						text+="\n"+rst.getFirst()+": "+responseText;
					}
				}
				return text;
			} else if (resources!=null && resources.containsKey(sa)) {
				String text="";
				if (line!=null) {
					String rt=line.getText();
					if (!StringUtils.isEmptyString(rt)) text+=resolveTemplates(rt, is)+"\n";
					text+=line.getProperty(NLG.PROPERTY_URL);
				}
				return text;
			}
		}
		return null;
	}

	private static enum FormExtractionState {QUESTION,ANSWER};
	// question sa -> pair<response sa, sasponse text>
	public static Map<String, List<Pair<String,String>>> getFormsResponses(String file) throws Exception {
		return getFormsResponses(file, 0);
	}
	public static Map<String, List<Pair<String,String>>> getFormsResponses(String file,int skip) throws Exception {
		HashMap<String,List<Pair<String,String>>> ret=new HashMap<String, List<Pair<String,String>>>();
		Sheet sheet = null;
		sheet=ExcelUtils.getSpreadSheet(file, 0);
		if (sheet != null)
		{
			String speechAct=null,responseKey=null;
			List<Pair<String,String>> response=null;
			FormExtractionState state=null;
			for(Iterator<Row> rowIter = sheet.rowIterator(); rowIter.hasNext(); ) {
				Row row = rowIter.next();
				// skip first row
				if (row.getRowNum()>skip) {
					for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
						Cell cell = cellIter.next();
						if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
							String cellValue=cell.getStringCellValue();
							cellValue=StringUtils.cleanupSpaces(cellValue);
							switch(cell.getColumnIndex()) {
							case 2:
								if (cellValue.equals("form")) {
									state=FormExtractionState.QUESTION;
									if (!StringUtils.isEmptyString(speechAct) && !response.isEmpty()) {
										ret.put(speechAct, response);
									}
									response=new ArrayList<Pair<String,String>>();
								}
								else if (cellValue.equals("response")) state=FormExtractionState.ANSWER;
								else state=null;
								break;
							case 5:
								if (state==FormExtractionState.QUESTION) speechAct=cellValue;
								else if (state==FormExtractionState.ANSWER) responseKey=cellValue;
								break;
							case 8:
								if (state==FormExtractionState.ANSWER && !StringUtils.isEmptyString(responseKey)) {
									response.add(new Pair<String,String>(responseKey,cellValue));
								}
								responseKey=null;
							}
						}
					}
				}
			}
			if (!StringUtils.isEmptyString(speechAct) && !response.isEmpty()) {
				ret.put(speechAct, response);
			}
		}
		return ret;
	}
	
	public Map<String, List<Pair<String,String>>> getResources(String file,int skip) throws Exception {
		HashMap<String, List<Pair<String,String>>> ret=new HashMap<String, List<Pair<String,String>>>();
		Sheet sheet = null;
		sheet=ExcelUtils.getSpreadSheet(file, 0);
		if (sheet != null)
		{
			String speechAct=null;
			Pair<String,String> resource=null;
			for(Iterator<Row> rowIter = sheet.rowIterator(); rowIter.hasNext(); ) {
				Row row = rowIter.next();
				// skip first row
				if (row.getRowNum()>skip) {
					for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
						Cell cell = cellIter.next();
						if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
							String cellValue=cell.getStringCellValue();
							cellValue=StringUtils.cleanupSpaces(cellValue);
							switch(cell.getColumnIndex()) {
							case 4:
								if (!StringUtils.isEmptyString(speechAct) && (resource!=null) &&
										!StringUtils.isEmptyString(resource.getSecond())) {
									List<Pair<String,String>> resources=ret.get(speechAct);
									if (resources==null) ret.put(speechAct, resources=new ArrayList<Pair<String,String>>());
									resources.add(resource);
								}
								if (!StringUtils.isEmptyString(cellValue)) speechAct=cellValue;
								resource=null;
								break;
							case 5:
								resource=new Pair<String, String>(cellValue, null);
								break;
							case 7:
								if (resource==null) resource=new Pair<String, String>(null, null);
								resource.setSecond(cellValue);
							}
						}
					}
				}
			}
			if (!StringUtils.isEmptyString(speechAct) && (resource!=null) &&
					!StringUtils.isEmptyString(resource.getSecond())) {
				List<Pair<String,String>> resources=ret.get(speechAct);
				if (resources==null) ret.put(speechAct, resources=new ArrayList<Pair<String,String>>());
				resources.add(resource);
			}
		}
		return ret;
	}
	
	@Override
	public void interrupt(DMInterruptionRequest ev) throws Exception {
		super.interrupt(ev);
	}
	
	@Override
	public void reloadData() throws Exception {
		logger.info("re-loading data.");
		try {loadSystemUtterances();}catch (Exception e) {logger.error("error while reloading system utterance: "+e.getMessage());}
		try {loadSystemForms();}catch (Exception e) {logger.error("error while reloading system forms: "+e.getMessage());}
		try {loadSystemResources();}catch (Exception e) {logger.error("error while reloading system resources: "+e.getMessage());}
		logger.info("done loading data.");
	}
}
