package edu.usc.ict.nl.nlg.echo;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import edu.usc.ict.nl.bus.modules.NLG;
import edu.usc.ict.nl.config.NLGConfig;
import edu.usc.ict.nl.nlg.SpeechActWithProperties;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.utils.ExcelUtils;
import edu.usc.ict.nl.utils.LogConfig;

public class EchoNLGData {
	
	protected static final Logger logger = Logger.getLogger(EchoNLGData.class.getName());
	
	private Map<String,List<SpeechActWithProperties>> validSpeechActs;
	private Map<String,List<Pair<String,String>>> formsResponses=null;
	private Map<String,List<Pair<String,String>>> resources;
	
	private static enum FormExtractionState {QUESTION,ANSWER};

	private NLGConfig config;

	static {
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );
	}

	public EchoNLGData(NLGConfig configuration) {
		this.config=configuration;
		try {loadSystemUtterances();}catch (Exception e) {logger.error("error while reloading system utterance: "+e.getMessage());}
		try {loadSystemForms();}catch (Exception e) {logger.error("error while reloading system forms: "+e.getMessage());}
		try {loadSystemResources();}catch (Exception e) {logger.error("error while reloading system resources: "+e.getMessage());}
	}

	public Map<String,List<SpeechActWithProperties>> getValidSpeechActs() {
		return validSpeechActs;
	}
	public void setValidSpeechActs(Map<String,List<SpeechActWithProperties>> validSpeechActs) {
		this.validSpeechActs = validSpeechActs;
	}
	public Map<String,List<Pair<String,String>>> getResources() {
		return resources;
	}
	public void setResources(Map<String, List<Pair<String, String>>> resources) {
		this.resources = resources;
	}
	public Map<String,List<Pair<String,String>>> getFormResponses() {
		return formsResponses;
	}
	public void setFormsResponses(Map<String, List<Pair<String, String>>> formsResponses) {
		this.formsResponses = formsResponses;
	}
	
	public NLGConfig getConfiguration() {
		return config;
	}
	
	private void loadSystemResources() throws Exception {
		NLGConfig config=getConfiguration();
		setResources(getResourcesFromXLSX(config.getSystemResources(), 0));
	}

	private void loadSystemForms() throws Exception {
		NLGConfig config=getConfiguration();
		if (config.getDisplayFormAnswerInNlg()) setFormsResponses(getFormResponsesFromXLSX(config.getSystemForms()));
	}

	private void loadSystemUtterances() throws Exception {
		NLGConfig config=getConfiguration();
		setValidSpeechActs(extractMappingBetweenTheseTwoColumnsWithProperties(config.getSystemUtterances(), 0, 4, 5,6,-1,true));
		if (!StringUtils.isEmptyString(config.getNvbs())) {
			File nvbFile=new File(config.getNvbs());
			if (nvbFile.exists()) {
				Map<String, List<SpeechActWithProperties>> nvb = extractMappingBetweenTheseTwoColumnsWithProperties(config.getNvbs(), 0, 4, 5,6,-1,true);
				getValidSpeechActs().putAll(nvb);
			}
		}
		if (getConfiguration().getIsAsciiNLG()) normalizeToASCII(getValidSpeechActs());
		if (getConfiguration().getIsNormalizeBlanksNLG()) normalizeBLANKS(getValidSpeechActs());
	}

	private static Map<String,List<SpeechActWithProperties>> extractMappingBetweenTheseTwoColumnsWithProperties(String file,int skip,int keyColumn,int valueColumn,int startPropertyColumn,int endPropertyColumn,boolean cleanupSpaces) throws Exception {
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

	// question sa -> pair<response sa, sasponse text>
	private static Map<String, List<Pair<String,String>>> getFormResponsesFromXLSX(String file) throws Exception {
		return getFormResponsesFromXLSX(file, 0);
	}
	private static Map<String, List<Pair<String,String>>> getFormResponsesFromXLSX(String file,int skip) throws Exception {
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

	public Map<String, List<Pair<String,String>>> getResourcesFromXLSX(String file,int skip) throws Exception {
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

	
}
