package edu.usc.ict.nl.nlu;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import edu.usc.ict.data.ToolkitBase;
import edu.usc.ict.nl.bus.NLBus;
import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.Token.TokenTypes;
import edu.usc.ict.nl.nlu.fst.sps.SAMapper;
import edu.usc.ict.nl.nlu.ne.NamedEntityExtractorI;
import edu.usc.ict.nl.nlu.trainingFileReaders.MXNLUTrainingFile;
import edu.usc.ict.nl.nlu.trainingFileReaders.NLUTrainingFileI;
import edu.usc.ict.nl.nlu.trainingFileReaders.SimcoachUserXLSXFile;
import edu.usc.ict.nl.stemmer.Stemmer;
import edu.usc.ict.nl.test.TargetDialogEntry;
import edu.usc.ict.nl.util.EnglishUtils;
import edu.usc.ict.nl.util.FileUtils;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.Triple;
import edu.usc.ict.nl.utils.EnglishWrittenNumbers2Digits;
import edu.usc.ict.nl.utils.ExcelUtils;
import edu.usc.ict.nl.utils.LogConfig;
import edu.usc.ict.nl.utils.SpellCheckProcess;

public class BuildTrainingData {
	
	private static final Set<String> stopWords=new HashSet<String>();
	static {
		stopWords.add("a");
		stopWords.add("able");
		stopWords.add("about");
		stopWords.add("across");
		stopWords.add("after");
		stopWords.add("all");
		stopWords.add("almost");
		stopWords.add("also");
		stopWords.add("am");
		stopWords.add("among");
		stopWords.add("an");
		stopWords.add("and");
		stopWords.add("any");
		stopWords.add("are");
		stopWords.add("as");
		stopWords.add("at");
		stopWords.add("be");
		stopWords.add("because");
		stopWords.add("been");
		stopWords.add("but");
		stopWords.add("by");
		stopWords.add("can");
		stopWords.add("cannot");
		stopWords.add("could");
		stopWords.add("dear");
		stopWords.add("did");
		stopWords.add("do");
		stopWords.add("does");
		stopWords.add("either");
		stopWords.add("else");
		stopWords.add("ever");
		stopWords.add("every");
		stopWords.add("for");
		stopWords.add("from");
		stopWords.add("get");
		stopWords.add("got");
		stopWords.add("had");
		stopWords.add("has");
		stopWords.add("have");
		stopWords.add("he");
		stopWords.add("her");
		stopWords.add("hers");
		stopWords.add("him");
		stopWords.add("his");
		stopWords.add("how");
		stopWords.add("however");
		stopWords.add("i");
		stopWords.add("if");
		stopWords.add("in");
		stopWords.add("into");
		stopWords.add("is");
		stopWords.add("it");
		stopWords.add("its");
		stopWords.add("just");
		stopWords.add("least");
		stopWords.add("let");
		stopWords.add("like");
		stopWords.add("likely");
		stopWords.add("may");
		stopWords.add("me");
		stopWords.add("might");
		stopWords.add("most");
		stopWords.add("must");
		stopWords.add("my");
		stopWords.add("neither");
		stopWords.add("no");
		stopWords.add("nor");
		stopWords.add("not");
		stopWords.add("of");
		stopWords.add("off");
		stopWords.add("often");
		stopWords.add("on");
		stopWords.add("only");
		stopWords.add("or");
		stopWords.add("other");
		stopWords.add("our");
		stopWords.add("own");
		stopWords.add("rather");
		stopWords.add("said");
		stopWords.add("say");
		stopWords.add("says");
		stopWords.add("she");
		stopWords.add("should");
		stopWords.add("since");
		stopWords.add("so");
		stopWords.add("some");
		stopWords.add("than");
		stopWords.add("that");
		stopWords.add("the");
		stopWords.add("their");
		stopWords.add("them");
		stopWords.add("then");
		stopWords.add("there");
		stopWords.add("these");
		stopWords.add("they");
		stopWords.add("this");
		stopWords.add("tis");
		stopWords.add("to");
		stopWords.add("too");
		stopWords.add("twas");
		stopWords.add("us");
		stopWords.add("wants");
		stopWords.add("was");
		stopWords.add("we");
		stopWords.add("were");
		stopWords.add("what");
		stopWords.add("when");
		stopWords.add("where");
		stopWords.add("which");
		stopWords.add("while");
		stopWords.add("who");
		stopWords.add("whom");
		stopWords.add("why");
		stopWords.add("will");
		stopWords.add("with");
		stopWords.add("would");
		stopWords.add("yet");
		stopWords.add("you");
		stopWords.add("your");
	}
	
	private static final Logger logger = Logger.getLogger(BuildTrainingData.class.getName());
	static {
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );
	}

	private static SpellCheckProcess sc=null;
	private static Stemmer stemmer=null;

	private NLUConfig config;
	private NLUTrainingFileI reader=null;
	private NLUConfig getConfiguration() {return config;}
	private void setConfiguration(NLUConfig c) {this.config=c;}

	/**
	 * @return list of pairs: <utterance, speech act label>
	 * @throws InvalidFormatException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public List<TrainingDataFormat> buildTrainingData() {
		NLUConfig config=getConfiguration();
		File userFile=new File(config.getUserUtterances());
		List<TrainingDataFormat> td = null;
		try {
			td=buildConfiguredTrainingDataFromExcel(userFile.getAbsolutePath());
		} catch (Exception e) {
        	logger.error("Error processing file: "+userFile.getAbsolutePath()+" "+e.getMessage());
			td=new ArrayList<TrainingDataFormat>();
		}
		
        File userSessionFile=new File(config.getNLUContentRoot()+File.separator+"user-utterances-from-user-data-collection.xlsx");
        try {
        	addToTrainingData(td,buildConfiguredTrainingDataFromExcel(userSessionFile.getAbsolutePath()));
        } catch (Exception e) {
        	logger.warn("Error processing file: "+userSessionFile.getAbsolutePath()+" "+e.getMessage());
        }
        
        if (!StringUtils.isEmptyString(config.getSystemForms())) {
        	File formsFile=new File(config.getSystemForms());
        	if (config.getUseSystemFormsToTrainNLU()) {
        		try {
        			addToTrainingData(td,buildTrainingDataFromFormsExcel(formsFile.getAbsolutePath(), 0));
        		} catch (Exception e) {
        			logger.warn("Error processing file: "+formsFile.getAbsolutePath()+" "+e.getMessage());
        		}
        	}
        }
        
        td=cleanTrainingData(td);
        //td=new ArrayList<Pair<String,String>>();
        //btd.addToTrainingData(td,btd.buildTrainingDataFromFormsExcel("../../simcoach-runtime/SimcoachApp/src/forms.xlsx", 0),false);
        return td;
	}
	
	public static List<TrainingDataFormat> buildTrainingDataFromHash(Map<String,List<String>> sas) throws Exception {
		List<TrainingDataFormat> ret=null;
		if (sas!=null) {
			for(String sa:sas.keySet()) {
				List<String> us=sas.get(sa);
				if (us!=null && !us.isEmpty()) {
					if (ret==null) ret=new ArrayList<TrainingDataFormat>();
					for(String u:us) {
						ret.add(new TrainingDataFormat(u, sa));
					}
				}
			}
		}
		return ret;
	}

	public Set<String> getAllWordsInTrainingData(List<Pair<String, String>> td) throws Exception {
		if (td==null || td.isEmpty()) return null;
		else {
			HashSet<String> ret=new HashSet<String>();
			for (Pair<String,String> d:td) {
				String line=d.getFirst();
				line=line.toLowerCase();
				List<Token> tokens = tokenize(line);
				List<String> words=getAllWords(tokens);
				if (words!=null) {
					ret.addAll(words);
				}
			}
			return ret;
		}
	}
	public List<String> getAllWords(List<Token> tokens) {
		if (tokens==null || tokens.isEmpty()) return null;
		else {
			List<String> ret=new ArrayList<String>();
			for (Token t:tokens) {
				String word=t.getName();
				TokenTypes type=t.getType();
				if (type==TokenTypes.WORD) ret.add(word);
			}
			return ret;
		}
	}

	public BuildTrainingData(NLUConfig config) throws Exception {
		setConfiguration(config);
		reader=config.getTrainingDataReader();
		if ((sc==null) && config.getDoSpellChecking()) sc=new SpellCheckProcess(config);
		String stemmerClass=config.getStemmerClass();
		if (stemmerClass!=null) stemmer=(Stemmer) NLBus.createSubcomponent(config, stemmerClass);
	}

	
	public void dumpIORDataInImportableTXT(HashMap<String, Pair<String, Collection<String>>> iorData,String lineFile,String speechActFile,String linksFile) throws IOException {
		BufferedWriter lineOut=new BufferedWriter(new FileWriter(lineFile));
		BufferedWriter saOut=new BufferedWriter(new FileWriter(speechActFile));
		BufferedWriter linksOut=new BufferedWriter(new FileWriter(linksFile));
		HashSet<String> sas=new HashSet<String>();
		for (String line:iorData.keySet()) {
			Pair<String, Collection<String>> saAndSystemUttsPrecedingIt=iorData.get(line);
			String sa=saAndSystemUttsPrecedingIt.getFirst();
			Collection<String> systemUtts=saAndSystemUttsPrecedingIt.getSecond();
			
			String userLine="";
			for (String systemUtt:systemUtts) {
				userLine+="SIMCOACH: "+systemUtt+"\n";
			}
			userLine+="=> "+line+"\n\n";

			if (!StringUtils.isEmptyString(sa)) {
				sas.add(sa);
				linksOut.write(userLine+"\n\n"+sa+"\n\n");
			}

			lineOut.write(userLine);
		}
		for(String sa:sas) {
			saOut.write(sa+"\n\n");
		}
		lineOut.close();
		saOut.close();
		linksOut.close();
	}
	public HashMap<String, Pair<String, Collection<String>>> produceDataForIOR(List<TrainingDataFormat> data, List<Pair<String, String>> suPairs) throws Exception {
		HashMap<String, Pair<String, Collection<String>>> ad=new HashMap<String, Pair<String,Collection<String>>>();
		HashMap<String,Collection<String>> userSystemMap=new HashMap<String, Collection<String>>();
		// first build the map of user utterance paired to the list of preceding system utterances.
		for(Pair<String, String> su:suPairs) {
			String systemUtt=su.getFirst();
			String userUtt=prepareUtteranceForClassification(su.getSecond());
			Collection<String> systemUtts=userSystemMap.get(userUtt);
			if (systemUtts==null) userSystemMap.put(userUtt, systemUtts=new HashSet<String>());
			systemUtts.add(systemUtt);
		}
		// then build the map of user utterances paired to the speech act and system utterances preceding that user utterance.
		for(TrainingDataFormat d:data) {
			String userUtterance=prepareUtteranceForClassification(d.getUtterance());
			String userUtteranceSpeechAct=d.getLabel();
			Pair<String, Collection<String>> saAndsystemUttsPair = ad.get(userUtterance);
			String a=(saAndsystemUttsPair!=null)?saAndsystemUttsPair.getFirst():null;
			if (!StringUtils.isEmptyString(a)) {
				if (!a.equals(userUtteranceSpeechAct)) {
					System.out.println("Line '"+userUtterance+"' was '"+a+"' and now it is '"+userUtteranceSpeechAct+"'.");
				}
			} else {
				if (saAndsystemUttsPair==null) saAndsystemUttsPair=new Pair<String, Collection<String>>(userUtteranceSpeechAct, new HashSet<String>());
				if (userSystemMap.containsKey(userUtterance))
					saAndsystemUttsPair.getSecond().addAll(userSystemMap.get(userUtterance));
				ad.put(userUtterance, saAndsystemUttsPair);
			}
		}
		return ad;
	}
	public List<TrainingDataFormat> getAllSimcoachData() throws InvalidFormatException, FileNotFoundException, IOException {
		List<TrainingDataFormat> td = buildTrainingData();
		for (File f:FileUtils.getAllFiles(new File("resources/data/"), ".*\\.xlsx$")) {
			String filename=f.getAbsolutePath();
			System.out.println("considering file: "+filename+" for addition.");
			try {
				List<TrainingDataFormat> ltd = buildTrainingDataFromExcelSessionsExport(filename,0);
				addToTrainingData(td, ltd, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return td;
	}
	public List<Pair<String, String>> getAllSystemUserPairs() {
		List<Pair<String, String>> td = new ArrayList<Pair<String,String>>();
		for (File f:FileUtils.getAllFiles(new File("resources/data/"), ".*\\.xlsx$")) {
			String filename=f.getAbsolutePath();
			System.out.println("considering file: "+filename+" for addition.");
			try {
				ArrayList<Pair<String, String>> ltd = getSystemUserPairsFromExcelSessionsExport(filename,0);
				td.addAll(ltd);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return td;
	}

	public static List<TrainingDataFormat> buildTrainingDataFromTargetDialog(String dialogFile) throws Exception {
		List<TargetDialogEntry> td = TargetDialogEntry.readTargetDialog(dialogFile,null);
		List<TrainingDataFormat> trData=new ArrayList<TrainingDataFormat>();
		for (TargetDialogEntry tde:td) {
			if (tde.getType().equals(TargetDialogEntry.Type.USER)) {
				for (NLUOutput sa:tde.getSpeechActs()) {
					if (sa instanceof ChartNLUOutput) {
						List<Triple<Integer, Integer, NLUOutput>> portions = ((ChartNLUOutput) sa).getPortions();
						if (portions!=null) {
							for(Triple<Integer, Integer, NLUOutput> p:portions) {
								NLUOutput nlu=p.getThird();
								trData.add(new TrainingDataFormat(nlu.getText(), nlu.getId()));
							}
						}
					} else {
						trData.add(new TrainingDataFormat(tde.getText(), sa.getId()));
					}
				}
			}
		}
		return trData;
	}
	
	// user-utterances excel file
	public static List<TrainingDataFormat> buildStandardTrainingDataFromExcel(String file,int skip) throws Exception {
		SimcoachUserXLSXFile reader = new SimcoachUserXLSXFile(skip);
		return reader.getTrainingInstances(new File(file));
	}
	public List<TrainingDataFormat> buildConfiguredTrainingDataFromExcel(String file) throws Exception {
		return reader.getTrainingInstances(new File(file));
	}

	// extract user-utterances like info from the forms.xlsx file
	public List<TrainingDataFormat> buildTrainingDataFromFormsExcel(String file,int skip) throws Exception {
		List<TrainingDataFormat>ret=new ArrayList<TrainingDataFormat>();
		Sheet sheet = ExcelUtils.getSpreadSheet(file, 0);
		if (sheet != null)
		{
			String speechAct=null;
			boolean getThisRow=false;
			for(Iterator<Row> rowIter = sheet.rowIterator(); rowIter.hasNext(); ) {
				Row row = rowIter.next();
				// skip first row
				if (row.getRowNum()>skip) {
					for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
						Cell cell = cellIter.next();
						if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
							String cellValue=cell.getStringCellValue();
							switch(cell.getColumnIndex()) {
							case 2:
								if (cellValue.equals("response")) getThisRow=true;
								else {
									getThisRow=false;
									speechAct=null;
								}
								break;
							case 5:
								if (getThisRow) speechAct=cellValue;
								break;
							case 8:
								String utterance=cell.getStringCellValue();
								if (getThisRow && (!StringUtils.isEmptyString(speechAct)) && (!StringUtils.isEmptyString(utterance)) && (!speechAct.startsWith("answer.form.choice")) && (!speechAct.startsWith("answer.relative-quantity")) && (!speechAct.startsWith("answer.intensity"))) {
									//String utterance=prepareUtteranceForClassification(cellValue);
									ret.add(new TrainingDataFormat(utterance,speechAct));
								}
							}
						}
					}
				}
			}
		}
		return ret;
	}
	/* return a list of pair in which the first is the system utterance immediately preceeding the user utterance that is the second of the pair.
	 * 
	 */
	public ArrayList<Pair<String,String>> getSystemUserPairsFromExcelSessionsExport(String file,int skip) throws InvalidFormatException, FileNotFoundException, IOException {
		ArrayList<Pair<String,String>>ret=new ArrayList<Pair<String,String>>();
		Sheet sheet = ExcelUtils.getSpreadSheet(file, 0);
		if (sheet != null)
		{
			String systemUtterance=null,userUtterance=null;
			boolean storeSystemUtterance=false,storeUserUtterance=false;
			for(Iterator<Row> rowIter = sheet.rowIterator(); rowIter.hasNext(); ) {
				Row row = rowIter.next();
				// skip first row
				if (row.getRowNum()>skip) {
					for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
						Cell cell = cellIter.next();
						if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
							int column=cell.getColumnIndex();
							switch(column) {
							case 0:
								storeSystemUtterance=false;
								storeUserUtterance=false;
								String speaker=StringUtils.cleanupSpaces(cell.getStringCellValue());
								if (speaker.equals(NLBusConfig.SIMCOACH_INTERNAL_ID)) storeSystemUtterance=true;
								else storeUserUtterance=true;
								break;
							case 1:
								if (storeUserUtterance) {
									userUtterance = StringUtils.cleanupSpaces(cell.getStringCellValue());
								} else if (storeSystemUtterance) {
									systemUtterance = StringUtils.cleanupSpaces(cell.getStringCellValue());
									userUtterance=null;
								}
								break;
							}
						}
					}
				}
				if (storeUserUtterance && !StringUtils.isEmptyString(systemUtterance) && !StringUtils.isEmptyString(userUtterance)) {
					ret.add(new Pair<String, String>(systemUtterance, userUtterance));
					storeSystemUtterance=false;
					storeUserUtterance=false;
					userUtterance=null;
				}
			}
		}
		return ret;
	}
	public List<TrainingDataFormat> buildTrainingDataFromExcelSessionsExport(String file,int skip) throws Exception {
		List<TrainingDataFormat>ret=new ArrayList<TrainingDataFormat>();
		Sheet sheet = ExcelUtils.getSpreadSheet(file, 0);
		if (sheet != null)
		{
			boolean skipRow;
			for(Iterator<Row> rowIter = sheet.rowIterator(); rowIter.hasNext(); ) {
				skipRow=false;
				Row row = rowIter.next();
				String utterance=null,speechAct=null;
				// skip first row
				if (row.getRowNum()>skip) {
					for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
						Cell cell = cellIter.next();
						if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
							int column=cell.getColumnIndex();
							switch(column) {
							case 0:
								String speaker=StringUtils.cleanupSpaces(cell.getStringCellValue());
								if (StringUtils.isEmptyString(speaker) || speaker.equals(NLBusConfig.SIMCOACH_INTERNAL_ID)) skipRow=true;
								break;
							case 1:
								utterance = StringUtils.cleanupSpaces(cell.getStringCellValue());
								break;
							case 2:
								speechAct=StringUtils.cleanupSpaces(cell.getStringCellValue());
							}
						}
						if (skipRow) break;
					}
				}
				if (!skipRow && !StringUtils.isEmptyString(utterance)) {
					ret.add(new TrainingDataFormat(utterance, speechAct));
				}
			}
		}
		return ret;
	}

	private Set<String> hardLinks=null;
	// the mapping returned is from input text to label
	public Map<String,String> buildHardLinksMap() throws Exception {
		Set<String> links = getHardLinks();
		Map<String,String> ret=null;
		if (links!=null && !links.isEmpty()) {
			NLUConfig config=getConfiguration();
	        List<TrainingDataFormat> td = buildConfiguredTrainingDataFromExcel(config.getUserUtterances());
	        for(TrainingDataFormat d:td) {
	        	String label=d.getLabel();
	        	if (links.contains(label)) {
	        		if (ret==null) ret=new HashMap<String, String>();
	        		ret.put(d.getUtterance(),label);
	        	}
	        }
		}
		return ret;
	}
	public Set<String> getHardLinks() throws IOException {
		if (hardLinks!=null) return hardLinks;
		else {
			NLUConfig config=getConfiguration();
			String hardLinksFileName=config.getNluHardLinks();
			File hardLinksFile=null;
			Set<String> ret=null; 
			if (!StringUtils.isEmptyString(hardLinksFileName) && ((hardLinksFile=new File(hardLinksFileName)).exists())) {
				BufferedReader in=new BufferedReader(new FileReader(hardLinksFile));
				String line;
				while ((line=in.readLine())!=null) {
					line=StringUtils.cleanupSpaces(line);
					if (!StringUtils.isEmptyString(line)) {
						if (ret==null) ret=new HashSet<String>();
						ret.add(line);
					}
				}
				in.close();
			}
			hardLinks=ret;
			return ret;
		}
	}
	
	public List<TrainingDataFormat> cleanTrainingData(List<TrainingDataFormat> td) {
		Set<String> links=null;
		try {
			links = getHardLinks();
		} catch (IOException e) {
        	logger.warn("Error processing hardlinks file.",e);
		}
		if (links==null) return td;
		else {
			List<TrainingDataFormat> ret=new ArrayList<TrainingDataFormat>();
			for (TrainingDataFormat row:td) {
				String sa=row.getLabel();
				if (!links.contains(sa)) ret.add(row);
			}
			return ret;
		}
	}

	private static final HashMap<Pattern,String> highLevelTacQSpeechActs=new HashMap<Pattern, String>();
	static {
		highLevelTacQSpeechActs.put(Pattern.compile("^whq\\..*$"), "whq");
		highLevelTacQSpeechActs.put(Pattern.compile("^ynq\\..*$"), "ynq");
		highLevelTacQSpeechActs.put(Pattern.compile("^settopic\\..*$"), "settopic");
		highLevelTacQSpeechActs.put(Pattern.compile("^offeragentplayername.*$"), "offeragent");
		highLevelTacQSpeechActs.put(Pattern.compile("^preclosingvalence.*$"), "preclosingvalence");
		highLevelTacQSpeechActs.put(Pattern.compile("^closingvalence.*$"), "closingvalence");
	}
	
	public enum SAStatType {NOSA,FREQ,ALL};
	public void printStatistics(List<TrainingDataFormat> td,boolean compressSpeechActs,SAStatType sa) throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
		List<Pair<String, Integer>> otherStats = getStatistics(td, compressSpeechActs);
		int totUsages=0;
		for(Pair<String, Integer>saAndNum:otherStats) totUsages+=saAndNum.getSecond();
		System.out.println(otherStats.size()+" "+totUsages);
		for(Pair<String, Integer>saAndNum:otherStats) {
			if (sa==SAStatType.FREQ) {
				System.out.println((float)saAndNum.getSecond()/(float)totUsages);
			} else if (sa==SAStatType.ALL) {
				System.out.println(saAndNum.getFirst()+" "+saAndNum.getSecond()+" "+((float)saAndNum.getSecond()/(float)totUsages));
			}
		}
		// compute unique texts
		// unique labels
		// total frequency in training data and number of training data with backup labels
		Set<String> uniqueLabels=new HashSet<String>(FunctionalLibrary.map(td, TrainingDataFormat.class.getMethod("getLabel")));
		Map<String,String> uniqueUtterances=new HashMap<String,String>();
		for(TrainingDataFormat t:td) {
			String u=t.getUtterance();
			String l=t.getLabel();
			if (uniqueUtterances.containsKey(u) && !uniqueUtterances.get(u).equals(l))
				System.out.println("ERROR: multiple labels for utterance: "+u);
			uniqueUtterances.put(u, l);
		}
		Collection backupLabels=FunctionalLibrary.removeIfNot(td, TrainingDataFormat.class.getMethod("hasBackupLabels"));
		Set uniqueBackupLabels=new HashSet(backupLabels);
		System.out.println("Unique utterances: "+uniqueUtterances.size());
		System.out.println("Unique labels: "+uniqueLabels.size());
		System.out.println("Backup labels: "+backupLabels.size()+"/"+td.size()+" unique: "+uniqueBackupLabels.size());
	}
	public List<Pair<String,Integer>> getStatistics(List<TrainingDataFormat> td,boolean compressSpeechActs) {
		HashMap<String,Integer> stats=new HashMap<String, Integer>();
		HashMap<String,HashSet<String>> numcompression=new HashMap<String, HashSet<String>>();
		List<Pair<String,Integer>> ret=new ArrayList<Pair<String,Integer>>();
		for(TrainingDataFormat p:td) {
			String sa=p.getLabel();
			if (compressSpeechActs) {
				for(Entry<Pattern,String> pAndN:highLevelTacQSpeechActs.entrySet()) {
					Matcher m=pAndN.getKey().matcher(sa);
					if (m.matches()) {
						HashSet<String> v=numcompression.get(pAndN.getValue());
						if (v==null) numcompression.put(pAndN.getValue(), v=new HashSet<String>());
						v.add(sa);
						sa=pAndN.getValue();
						break;
					}
				}
			}
			if (stats.containsKey(sa))
				stats.put(sa, stats.get(sa)+1);
			else
				stats.put(sa,1);
		}
		if (compressSpeechActs) {
			for(String sa:numcompression.keySet()) {
				System.out.println(sa+" "+numcompression.get(sa).size());
			}
		}
		for(String sa:stats.keySet()) {
			ret.add(new Pair<String, Integer>(sa, stats.get(sa)));
		}
		Collections.sort(ret, new Comparator<Pair<String,Integer>>() {

			@Override
			public int compare(Pair<String, Integer> arg0,Pair<String, Integer> arg1) {
				return arg1.getSecond()-arg0.getSecond();
			}
		});
		return ret;
	}
	public static boolean areTrainingDataEqual(List<TrainingDataFormat> td1,List<TrainingDataFormat> td2) {
		boolean td1Null=td1==null,td2Null=td2==null;
		if (td1Null && td2Null) return true;
		else if (td1Null) return td2.isEmpty();
		else if (td2Null) return td1.isEmpty();
		else {
			int length=td1.size();
			if (td2.size()!=length) return false;
			else {
				Comparator<TrainingDataFormat> cmp=new Comparator<TrainingDataFormat>() {
					public int compare(TrainingDataFormat o1, TrainingDataFormat o2) {
						String sa1=o1.getLabel(),sa2=o2.getLabel();
						String utt1=o1.getUtterance(),utt2=o2.getUtterance();
						int r=sa1.compareTo(sa2);
						if (r==0) return utt1.compareTo(utt2);
						else return r;
					}};
					Collections.sort(td1,cmp);
					Collections.sort(td2,cmp);
					for (int i=0;i<length;i++) {
						if (!td1.get(i).equals(td2.get(i))) return false;
					}
					return true;
			}
		}
	}
	public void compareTrainingData(List<TrainingDataFormat> td1,List<TrainingDataFormat> td2) throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
		int overlap=0,size1=0,size2=0;
		Collection<String> sas1=FunctionalLibrary.map(td1, Pair.class.getMethod("getLabel"));
		HashSet<String> td1SA=new HashSet<String>(sas1);
		Collection<String> sas2=FunctionalLibrary.map(td2, Pair.class.getMethod("getLabel"));
		HashSet<String> td2SA=new HashSet<String>(sas2);
		HashSet<String> td1SaCopy = (HashSet<String>) td1SA.clone();
		size1=td1SA.size();
		size2=td2SA.size();
		td1SaCopy.removeAll(td2SA);
		overlap=size1-td1SaCopy.size();
		System.out.println("size 1: "+size1+" size2: "+size2+" overlap: "+overlap);
		System.out.println("only in 1:");
		for(String onlyIn1:td1SaCopy) {
			System.out.println(onlyIn1);
		}
		HashSet<String>onlyIn1=td1SaCopy;
		System.out.println("only in 2:");
		td2SA.removeAll(td1SA);
		for(String onlyIn2:td2SA) {
			System.out.println(onlyIn2);
		}
		HashSet<String>onlyIn2=td2SA;
		
		int countOnly1=0;
		for(String sa1:sas1) if (onlyIn1.contains(sa1)) countOnly1++;
		int countOnly2=0;
		for(String sa2:sas2) if (onlyIn2.contains(sa2)) countOnly2++;
		
		System.out.println(" frequency of only in 1: "+((float)countOnly1/(float)sas1.size())+" only in 2: "+((float)countOnly2/(float)sas2.size()));
	}

	public static final LinkedHashMap<TokenTypes, Pattern> defaultTokenTypes=new LinkedHashMap<TokenTypes, Pattern>(){
		private static final long serialVersionUID = 1L;
		{
			put(TokenTypes.NUM, Pattern.compile("([0-9]*\\.[\\d]+)|([\\d]+)|(<"+TokenTypes.NUM.toString()+">)|(<"+TokenTypes.NUM.toString().toLowerCase()+">)"));
			put(TokenTypes.WORD, Pattern.compile("[\\d]*[a-zA-Z]+[\\d]*[a-zA-Z]*"));
			put(TokenTypes.OTHER,Pattern.compile("[^\\w\\s]+"));
		}
	};
	public static List<Token> tokenize(String u) throws Exception {
		return tokenize(u, defaultTokenTypes);
	}

	public static List<Token> removeStopWords(List<Token> input) {
		List<Token> ret=null;
		if (input!=null) {
			for(Token t:input) {
				String w=t.getName().toLowerCase();
				if (!stopWords.contains(w)) {
					if (ret==null) ret=new ArrayList<Token>();
					ret.add(t);
				}
			}
		}
		return ret;
	}
	
	public static List<Token> tokenize(String u,LinkedHashMap<TokenTypes,Pattern> types) throws Exception {
		ArrayList<Token> ret= new ArrayList<Token>();
		u=StringUtils.cleanupSpaces(u);
		LinkedHashMap<TokenTypes,Matcher> matchers=new LinkedHashMap<TokenTypes,Matcher>();
		for(Entry<TokenTypes, Pattern> tp:types.entrySet())	matchers.put(tp.getKey(),tp.getValue().matcher(u));
		int currentPos=0,length=u.length();		
		while(currentPos<length) {
			int start=length,end=0;
			TokenTypes type=null;
			for(Entry<TokenTypes, Matcher> mt:matchers.entrySet()) {
				Matcher m=mt.getValue();
				if (m.find(currentPos)) {
					if (m.start()<start) {
						start=m.start();
						end=m.end();
						type=mt.getKey();
					}
				}
			}
			if ((start<length) && (type!=null)) {
				Matcher m=matchers.get(type);
				String token=u.substring(m.start(),m.end());
				if ((token=token.replaceAll("[\\s]","")).length()>0) {
					ret.add(new Token(token, type,token,start,end));
				}
				currentPos=m.end();
			} else {
				System.out.println("Error while tokenizing");
				break;
			}
		}
		return ret;
	}
	public static String untokenize(List<Token> tokens) {
		String utterance="";
		boolean first=true;
		for (Token m:tokens) {
			if (first) first=false;
			else utterance+=" ";
			utterance+=m.getName();
		}
		return utterance;
	}
	public List<NamedEntityExtractorI> getNamedEntityExtractors() {
		return getConfiguration().getNluNamedEntityExtractors();
	}
	public List<Token> generalize(List<Token> tokens) {
		List<NamedEntityExtractorI> nes = getNamedEntityExtractors();
		List<Token> ret=new ArrayList<Token>(tokens);
		TokenTypes type;
		boolean generalized=false;
		if (nes!=null) {
			for(NamedEntityExtractorI ne:nes) {
				ne.generalize(ret);
			}
		}
		for (Token t:ret) {
			if (!generalized) {
				type=t.getType();
				if ((type==TokenTypes.NUM) && getConfiguration().getGeneralizeNumbers()) {
					ret.add(new Token("<"+TokenTypes.NUM.toString()+">", TokenTypes.NUM,t.getOriginal()));
				} else {
					ret.add(t);
				}
			}
		}
		return ret;
	}

	public static List<Token> stemm(List<Token> tokens) {
		List<Token> ret=new ArrayList<Token>();
		if (tokens!=null && !tokens.isEmpty()) {
			Iterator<Token> it = tokens.iterator();
			while(it.hasNext()) {
				Token cp=it.next();
				String word=cp.getName();
				//String fixedStemm=EnglishUtils.getFixedWordStemm(word);
				//if (fixedStemm!=null) cp.setFirst(fixedStemm);
				//else {
					word=stemmer.stemm(word);
					cp.setName(word);
				//}
				ret.add(cp);
			}
		}
		return ret;
	}
	
	public static String getStringOfTokensSpan(List<Token> tokens,int start, int end) {
		StringBuffer ret=null;
		if (tokens!=null) {
			int i=0;
			for(Token t:tokens) {
				if (i>=start && i<end) {
					if (ret==null) ret=new StringBuffer();
					ret.append(((ret.length()==0)?"":" ")+t.getName());
				}
				i++;
			}
		}
		return (ret!=null)?ret.toString():null;
	}
	
	public List<Token> applyBasicTransformationsToStringForClassification(String text) throws Exception {
		return applyBasicTransformationsToStringForClassification(text,defaultTokenTypes);
	}
	public List<Token> applyBasicTransformationsToStringForClassification(String text,LinkedHashMap<TokenTypes, Pattern> tokenTypes) throws Exception {
		return applyBasicTransformationsToStringForClassification(text, tokenTypes, true,getConfiguration());
	}
	public static List<Token> applyBasicTransformationsToStringForClassification(String text,LinkedHashMap<TokenTypes, Pattern> tokenTypes,boolean chattify,NLUConfig config) throws Exception {
		text=text.toLowerCase();
		List<Token> tokens=tokenize(text,tokenTypes);		
		if (sc!=null) tokens=doSpellCheck(tokens);
		tokens=uk2us(tokens);
		tokens=filterPunctuation(tokens);
		tokens=normalizeTokens(tokens);
		if (chattify) tokens=chattify(tokens,tokenTypes,chattify,config);
		tokens=contractEnglish(tokens,tokenTypes,chattify,config);
		if (stemmer!=null) tokens=stemm(tokens);
		try {
			tokens=EnglishWrittenNumbers2Digits.parseWrittenNumbers(config,tokens);
		} catch (Exception e) {
			logger.warn("Error converting written numbers to value.",e);
		}
		return tokens;
	}
	private static List<Token> doSpellCheck(List<Token> tokens) {
		List<Token> ret=new ArrayList<Token>();
		if (tokens!=null && !tokens.isEmpty()) {
			Iterator<Token> it = tokens.iterator();
			while(it.hasNext()) {
				Token cp=it.next();
				String word=cp.getName();
				TokenTypes type=cp.getType();
				if (type==TokenTypes.WORD) {
					try {
						word=sc.sendWordGetFirstChoice(word);
						List<Token> tmpTokens = tokenize(word);
						ret.addAll(tmpTokens);
					} catch (Exception e) {
						logger.warn("Error during spell check.",e);
						ret.add(cp);
					}
				} else ret.add(cp);
			}
		}
		return ret;
	}
	private static List<Token> uk2us(List<Token> tokens) {
		List<Token> ret=new ArrayList<Token>();
		if (tokens!=null && !tokens.isEmpty()) {
			Iterator<Token> it = tokens.iterator();
			while(it.hasNext()) {
				Token cp=it.next();
				String word=cp.getName();
				if (EnglishUtils.getUSspellingFor(word)!=null) cp.setName(EnglishUtils.getUSspellingFor(word));
				ret.add(cp);
			}
		}
		return ret;
	}
	private static List<Token> chattify(List<Token> tokens, LinkedHashMap<TokenTypes, Pattern> tokenTypes, boolean chattify, NLUConfig config) throws Exception {
		List<Token> ret=new ArrayList<Token>();
		if (tokens!=null && !tokens.isEmpty()) {
			Iterator<Token> it = tokens.iterator();
			while(it.hasNext()) {
				Token cp=it.next();
				String word=cp.getName();
				if (word.equals("you")) {
					cp.setName("u");
				} else if (word.equals("are")) {
					cp.setName("r");
				} else if (word.equals("great")) {
					cp.setName("gr8");
				} else if (word.equals("your")) {
					cp.setName("ur");
				} else if (word.equals("im")) {
					List<Token> tmpTs = applyBasicTransformationsToStringForClassification("i'm",tokenTypes,chattify,config);
					ret.addAll(tmpTs);
					continue;
				} else if (word.equals("dont")) {
					List<Token> tmpTs = applyBasicTransformationsToStringForClassification("don't",tokenTypes,chattify,config);
					ret.addAll(tmpTs);
					continue;
				} else if (word.equals("ive")) {
					List<Token> tmpTs = applyBasicTransformationsToStringForClassification("i've",tokenTypes,chattify,config);
					ret.addAll(tmpTs);
					continue;
				}
				
				ret.add(cp);
			}
		}
		return ret;
	}
	private static List<Token> contractEnglish(List<Token> tokens, LinkedHashMap<TokenTypes, Pattern> tokenTypes, boolean chattify, NLUConfig config) throws Exception {
		List<Token> ret=new ArrayList<Token>();
		if (tokens!=null && !tokens.isEmpty()) {
			Iterator<Token> it = tokens.iterator();
			Token pp=it.next();
			Token cp=pp;
			while(it.hasNext()) {
				cp=it.next();
				
				String pWord=pp.getName();
				
				String word=cp.getName();
				
				String c=EnglishUtils.getContractionFor(pWord,word);
				if (c!=null) {
					List<Token> tmpTs = applyBasicTransformationsToStringForClassification(c,tokenTypes,chattify,config);
					ret.addAll(tmpTs);
					if (it.hasNext()) cp=it.next();
					else cp=null;
				} else {
					ret.add(pp);
				}
				
				pp=cp;
			}
			if (cp!=null) ret.add(cp);
		}
		return ret;
	}
	private static List<Token> normalizeTokens(List<Token> tokens) {
		List<Token> ret=new ArrayList<Token>();
		for(Token t:tokens) {
			TokenTypes type=t.getType();
			String word=t.getName();
			if (type==TokenTypes.WORD) {
				if (word.equals("once")) {
					ret.add(new Token("1", TokenTypes.NUM,word));
					t.setName("time"); 
				} else if (word.equals("twice")) {
					ret.add(new Token("2", TokenTypes.NUM,word));
					t.setName("times"); 
				}
			}
			ret.add(t);
		}
		return ret;
	}

	public String prepareUtteranceForClassification(String text) throws Exception {
		return prepareUtteranceForClassification(text,defaultTokenTypes);
	}
	public String prepareUtteranceForClassification(String text,LinkedHashMap<TokenTypes, Pattern> tokenTypes) throws Exception {
		//System.out.println(text);
		List<Token> tokens = applyBasicTransformationsToStringForClassification(text,tokenTypes);
		tokens=generalize(tokens);
		//System.out.println(untokenize(tokens));
		return untokenize(tokens);
	}
	public List<TrainingDataFormat> prepareTrainingDataForClassification(List<TrainingDataFormat> td) throws Exception {
		List<TrainingDataFormat> ret=new ArrayList<TrainingDataFormat>();
		for(TrainingDataFormat d:td) {
			//System.out.println(d.getUtterance()+" :: "+d.getLabel());
			String nu=prepareUtteranceForClassification(d.getUtterance());
			if (StringUtils.isEmptyString(nu)) {
				logger.error("Empty utterance after filters to prepare it from training: ");
				logger.error("start='"+d.getUtterance()+"'");
				logger.error("end='"+nu+"'");
			} else {
				ret.add(new TrainingDataFormat(nu, d.getLabel()));
			}
		}
		return ret;
	}

	private static final Pattern puntToKeep=Pattern.compile("^([\\?]+)|([\\%]+)|(')$");
	private static List<Token> filterPunctuation(List<Token> tokens) {
		List<Token> ret=new ArrayList<Token>();
		for (Token t:tokens) {
			if (t.getType().equals(TokenTypes.OTHER)) {
				Matcher m=puntToKeep.matcher(t.getName());
				if (m.matches()) {
					t.setName(t.getName().substring(0, 1));
					ret.add(t);
				}
			} else ret.add(t);
		}
		return ret;
	}

	public ArrayList<String> getSessionsFromString(String sessionsString) throws Exception {
		ArrayList<String> ret=new ArrayList<String>();
		HashSet<String> seen=new HashSet<String>();
		sessionsString=sessionsString.replaceAll("[\\s]", "");
		String[]sessions=sessionsString.split(",");
		for(String s:sessions) {
			String[] se=s.split(":", 2);
			if (se.length<=1) {
				if (!seen.contains(s)) {
					ret.add(s);
					seen.add(s);
				}
			} else {
				int start=Integer.parseInt(se[0]);
				int end=Integer.parseInt(se[1]);
				if (start>end) throw new Exception("Start and end in '"+s+"' are not properly ordered.");
				else {
					for(int i=start;i<=end;i++) {
						String sn=""+i;
						if (!seen.contains(sn)) {
							ret.add(sn);
							seen.add(sn);
						}
					}
				}
			}
		}
		return ret;
	}
	public static ArrayList<Pair<String,String>> concatenateTrainingData(HashMap<String, ArrayList<Pair<String, String>>> content,ArrayList<Pair<String, String>> ret) {
		if (ret==null) ret=new ArrayList<Pair<String,String>>();
		for(ArrayList<Pair<String, String>> session:content.values()) {
			ret.addAll(session);
		}
		return ret;
	}

	public static void addToTrainingData(List<TrainingDataFormat> out,List<TrainingDataFormat> toAdd) {
		addToTrainingData(out,toAdd,true);
	}
	public static void addToTrainingData(List<TrainingDataFormat> out,List<TrainingDataFormat> toAdd,boolean addOnlyIfUsingOneLabelAlreadyInOut) {
		HashSet<String> sas=new HashSet<String>();
		if (addOnlyIfUsingOneLabelAlreadyInOut) for(TrainingDataFormat d:out) sas.add(d.getLabel());
		for(TrainingDataFormat d:toAdd) if (!addOnlyIfUsingOneLabelAlreadyInOut || sas.contains(d.getLabel())) out.add(d);
	}
	

	public class DecreasingIntegerComparator implements Comparator<Integer> {
		@Override
		public int compare(Integer o1, Integer o2) {
			return o1-o2;
		}
	}
	
	public void subtractFromTrainingData(List<TrainingDataFormat> out,List<TrainingDataFormat> toSubtract) {
		ArrayList<Integer> indexesToBeRemoved=new ArrayList<Integer>();
		HashMap<TrainingDataFormat,ArrayList<Integer>> index=new HashMap<TrainingDataFormat, ArrayList<Integer>>();
		int i=0;
		for(TrainingDataFormat el:out) {
			ArrayList<Integer> positions = index.get(el);
			if (positions==null) index.put(el, positions=new ArrayList<Integer>());
			positions.add(i++);
		}
		
		for(TrainingDataFormat el:toSubtract) {
			ArrayList<Integer> positions = index.get(el);
			if ((positions!=null) && !positions.isEmpty()) {
				indexesToBeRemoved.add(positions.get(0));
			} else {
				System.out.println(el+" not found in training data.");
			}
		}
		
		System.out.println("Asked to remove "+toSubtract.size()+" element(s) and removing "+indexesToBeRemoved.size()+".");
		
		Collections.sort(indexesToBeRemoved, new DecreasingIntegerComparator());
		for (Integer pos:indexesToBeRemoved) {
			out.remove(pos);
		}
	}

	public static void dumpTextToTextLabelSequence(List<TrainingDataFormat> data, File outFile) throws IOException {
		if (data!=null) {
			BufferedWriter out=new BufferedWriter(new FileWriter(outFile));
			if (outFile.exists()) outFile.delete();
			for(TrainingDataFormat td:data) {
				String utt=td.getUtterance().toLowerCase();
				String label=td.getLabel();
				out.write(utt+"\n"+label+"\n");
			}
			out.close();
		}
	}
	public static void dumpTrainingDataToMXNLUFormat(List<TrainingDataFormat> data, File outFile) throws IOException {
		if (data!=null) {
			BufferedWriter out=new BufferedWriter(new FileWriter(outFile));
			if (outFile.exists()) outFile.delete();
			for(TrainingDataFormat td:data) {
				String s=td.toNluformat(null);
				out.write(s);
			}
			out.close();
		}
	}
	public static void dumpTextFromTrainingDataForLM(List<TrainingDataFormat> data, File outFile) throws IOException {
		if (data!=null) {
			BufferedWriter out=new BufferedWriter(new FileWriter(outFile));
			if (outFile.exists()) outFile.delete();
			for(TrainingDataFormat td:data) {
				String utt=td.getUtterance().toUpperCase();
				out.write(utt+"\n");
			}
			out.close();
		}
	}
	public static void buildArpaLM(File text,File output) throws Exception {
		// example to create a language model. Capitalization of the input text matters because the asr returns all capital.
		kylm.main.CountNgrams.main(new String[]{text.getAbsolutePath(),output.getAbsolutePath(),"-arpa"});
	}
	
	public static List<TrainingDataFormat> buildTrainingDataFromNLUFormatFile(File fileToRead) throws Exception {
		return buildTrainingDataFromNLUFormatFile(fileToRead, true);
	}
	public static List<TrainingDataFormat> buildTrainingDataFromNLUFormatFile(File fileToRead,boolean hasFeatures) throws Exception {
		MXNLUTrainingFile reader=new MXNLUTrainingFile();
		reader.setHasFeatures(hasFeatures);
		return reader.getTrainingInstances(fileToRead);
	}
	
	public ArrayList<Pair<String,Collection<String>>> readIORExportedLinks(String fileName) throws Exception {
		BufferedReader inp=new BufferedReader(new FileReader(fileName));
		ArrayList<Pair<String,Collection<String>>> ret=new ArrayList<Pair<String,Collection<String>>>();
		String line=null;
		String text="";
		Collection<String> sas=null;
		Pair<String, Collection<String>> lastItem=null;
		boolean foundText=false;
		while((line=inp.readLine())!=null) {
			if (!StringUtils.isEmptyString(line)) {
				if (!foundText) text+=line;
				else sas.add(line);
			} else {
				foundText=!foundText;
				if (!foundText) {
					text=text.replaceFirst("^.*=>[\\s]*", "");
					text=prepareUtteranceForClassification(text);
					if ((lastItem!=null) && text.equals(lastItem.getFirst())) {
						lastItem.getSecond().addAll(sas);
					} else {
						ret.add(lastItem=new Pair<String, Collection<String>>(text, sas));
					}
					text="";
				} else {
					sas=new HashSet<String>();
				}
			}
		}
		return ret;
	}

	public static Set<String> getAllSpeechActsInTrainingData(List<TrainingDataFormat> td) {
		if (td==null || td.isEmpty()) return null;
		else {
			LinkedHashSet<String> ret=new LinkedHashSet<String>();
			for(TrainingDataFormat p:td) ret.add(p.getLabel());
			return ret;
		}
	}
	public static Map<String, List<TrainingDataFormat>> getAllSpeechActsWithTrainingData(List<TrainingDataFormat> tds) {
		if (tds==null || tds.isEmpty()) return null;
		else {
			LinkedHashMap<String,List<TrainingDataFormat>> ret=new LinkedHashMap<String,List<TrainingDataFormat>>();
			for(TrainingDataFormat td:tds) {
				String s=td.getLabel();
				List<TrainingDataFormat> utts4sa=ret.get(s);
				if (utts4sa==null) ret.put(s, utts4sa=new ArrayList<TrainingDataFormat>());
				utts4sa.add(td);
			}
			return ret;
		}
	}
	public static Map<String, List<String>> getAllSpeechActsWithUtterances(List<TrainingDataFormat> tds) {
		if (tds==null || tds.isEmpty()) return null;
		else {
			LinkedHashMap<String,List<String>> ret=new LinkedHashMap<String,List<String>>();
			for(TrainingDataFormat td:tds) {
				String label=td.getLabel();
				String utterance=td.getUtterance();
				addTDto(label,utterance,ret);
				if (td.getBackupLabels()!=null) {
					for(String bl:td.getBackupLabels()) {
						addTDto(bl, utterance, ret);
					}
				}
			}
			return ret;
		}
	}
	public static Map<String,List<String>> getAllUtterancesWithLabels(List<TrainingDataFormat> tds) {
		if (tds==null || tds.isEmpty()) return null;
		else {
			LinkedHashMap<String, List<String>> ret=new LinkedHashMap<String,List<String>>();
			for(TrainingDataFormat td:tds) {
				String label=td.getLabel();
				String utterance=td.getUtterance();
				addTDto(utterance,label,ret);
				if (td.getBackupLabels()!=null) {
					for(String bl:td.getBackupLabels()) {
						addTDto(utterance,bl, ret);
					}
				}
			}
			return ret;
		}		
	}
	
	private static void addTDto(String label, String utterance,
			LinkedHashMap<String, List<String>> ret) {
		List<String> utts4sa=ret.get(label);
		if (utts4sa==null) ret.put(label, utts4sa=new ArrayList<String>());
		utts4sa.add(utterance);
	}
	public static void dumpXMLDMLogToXLSX(File dmLog,NLU nlu,File xlsxOutput) throws Exception {
		ArrayList<TargetDialogEntry> tds = TargetDialogEntry.readTargetDialogFromXMLDMLog(dmLog.getAbsolutePath(),nlu);
		if (tds!=null && !tds.isEmpty()) {
			final String workSheetName = "transcript";
			final String[] headers = new String[]{"Delta time","Speaker","Utterance","Speech act"};
			final Workbook spreadsheet = ToolkitBase.buildBaseWorkbook(headers, workSheetName);
			final CellStyle dataStyle = buildBasicDataStyle(spreadsheet,false);
			final CellStyle boldStyle = buildBasicDataStyle(spreadsheet,true);
			final Sheet workingSheet = spreadsheet.getSheet(workSheetName);
			
			for (TargetDialogEntry td:tds) {
				addRowToXLSXTranscript(td, workingSheet, dataStyle,boldStyle);
			}
			ToolkitBase.autoLayout(headers.length,workingSheet);
			
			FileOutputStream out = new FileOutputStream(xlsxOutput);
			spreadsheet.write(out);
			out.close();
		}
	}
	private static Row addRowToXLSXTranscript(final TargetDialogEntry td, final Sheet workingSheet, final CellStyle style, final CellStyle boldStyle) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException, InstantiationException {
		final Row row = workingSheet.createRow(workingSheet.getLastRowNum()+1);
		int i=0;
		final Cell deltaCell = row.createCell(i++);
		deltaCell.setCellValue(td.getDeltaT());
		deltaCell.setCellStyle(style);
		final Cell speakerCell = row.createCell(i++);
		String speaker=(td.isSystemAction())?"simcoach":"user";
		speakerCell.setCellValue(speaker);
		speakerCell.setCellStyle(style);
		final Cell utteranceCell = row.createCell(i++);
		utteranceCell.setCellValue(td.getText());
		utteranceCell.setCellStyle(style);
		final Cell speechActCell = row.createCell(i++);
		ArrayList<NLUOutput> sas = td.getSpeechActs();
		String id="";
		if (sas!=null && !sas.isEmpty()) {
			Collection tmp=FunctionalLibrary.map(sas, NLUOutput.class.getMethod("getId"));
			id=FunctionalLibrary.printCollection(tmp, "", "", "\n");
		}
		speechActCell.setCellValue(id);
		speechActCell.setCellStyle((td.isUserAction())?boldStyle:style);
		return row;
	}
	public static CellStyle buildBasicDataStyle(Workbook spreadsheet,boolean bold) {
		final CellStyle dataStyle = spreadsheet.createCellStyle();
		final Font font = spreadsheet.createFont();
		font.setFontHeightInPoints((short)12);
		font.setFontName("Arial");
		if (bold) font.setBoldweight((short)Font.BOLDWEIGHT_BOLD);
		dataStyle.setWrapText(false);
		dataStyle.setFont(font);
		return dataStyle;
	}
	/*public void runNFoldsValidation(List<Pair<String,String>> tds,int folds,NLU nlu) throws Exception {
		if(folds<2) throw new Exception("Number of folds must be at least 2.");
		
		ArrayList<List<Pair<String,String>>> td4Fold=new ArrayList<List<Pair<String,String>>>();
		for(int i=0;i<folds;i++) td4Fold.add(new ArrayList<Pair<String,String>>());
		int i=0;
		for(Pair<String,String> td:tds) {
			td4Fold.get(i).add(td);
			i=(++i)%folds;
		}
		List<Pair<String,String>> trainingData=new ArrayList<Pair<String,String>>(),testingData;
		PerformanceResult totalPerformace=new PerformanceResult();
		long totalTime=0,time,diff;
		for(i=0;i<folds;i++) {
			System.out.println("fold: "+i+"/"+folds);
			trainingData.clear();
			testingData=td4Fold.get(i);
			for(int j=0;j<folds;j++) if (i!=j) trainingData.addAll(td4Fold.get(j));
			dumpTrainingDataToFileNLUFormat(trainingFile, trainingData);
			nlu.train(trainingFile, modelFile);
		}
	}*/

	public static FoldsData produceFolds(List<TrainingDataFormat> tds,int folds) throws Exception {
		if(folds<1) throw new Exception("Number of folds must be at least 1.");
		FoldsData ret=new FoldsData(tds,folds);
		return ret;
	}
	public static DynamicFoldsData produceDynamicFolds(List<TrainingDataFormat> trainingData, int folds) throws Exception {
		if(folds<1) throw new Exception("Number of folds must be at least 1.");
		DynamicFoldsData ret=new DynamicFoldsData(trainingData,folds);
		return ret;
	}
	
	public static List<TrainingDataFormat> extractThese(List<TrainingDataFormat> tds,int[] selected) {
		if (tds!=null) {
			if (selected!=null) {
				List<TrainingDataFormat> ret=null;
				for(int i:selected) {
					if (ret==null) ret=new ArrayList<TrainingDataFormat>();
					ret.add(tds.get(i));
				}
				return ret;
			}
		}
		return null;
	}
	
	/**
	 *  
	 * @param in input list that will be modified
	 * @param p number from 0 to 1, defines the percentage of the input list that will be removed and returned
	 * @return
	 */
	public static List destructivelyExtractPortionOfList(List in,float p) {
		List ret=null;
		assert(p<=1 && p>=0);
		if (p==0) return null;
		if (p==1) {
			ret=new ArrayList(in);
			in.clear();
			return ret;
		}
			
		if (in!=null) {
			int i=0,selected=0;
			Iterator it=in.iterator();
			while(it.hasNext()) {
				Object x=it.next();
				i++;
				float cp=(float)selected/(float)i;
				if (cp<p) {
					if (ret==null) ret=new ArrayList();
					ret.add(x);
					it.remove();
					selected++;
				}
			}
		}
		return ret;
	}
	public static int[] extractPortionOfList(List in,float p) {
		assert(p<1 && p>0);
		List<Integer> ret=null;
		int i=0,selected=0;
		if (in!=null) {
			Iterator it=in.iterator();
			while(it.hasNext()) {
				Object x=it.next();
				i++;
				float cp=(float)selected/(float)i;
				if (cp<p) {
					if (ret==null) ret=new ArrayList<Integer>();
					ret.add(i-1);
					selected++;
				}
			}
		}
		if (ret==null) return new int[0];
		else {
			int[] rret=new int[ret.size()];
			i=0;
			for(Integer x:ret) {
				rret[i++]=x;
			}
			return rret;
		}
	}
	public static List destructivelyExtractOnlyWithBackupLabels(List<TrainingDataFormat> in) {
		List ret=null;
		if (in!=null) {
			int i=0,selected=0;
			Iterator<TrainingDataFormat> it=in.iterator();
			while(it.hasNext()) {
				TrainingDataFormat x=it.next();
				i++;
				if (x.hasBackupLabels()) {
					if (ret==null) ret=new ArrayList();
					ret.add(x);
					it.remove();
					selected++;
				}
			}
		}
		return ret;
	}

	public void keepOnlyTheseLabels(List<TrainingDataFormat> data,String[] strings) {
		if (data!=null) {
			Set<String> okLabels=new HashSet<String>(Arrays.asList(strings));
			Iterator<TrainingDataFormat> it=data.iterator();
			while(it.hasNext()) {
				TrainingDataFormat v = it.next();
				if (!okLabels.contains(v.getLabel())) it.remove();
			}
		}
	}


	/*
	 * finds xml files by recursively exploring the given root directory (avoids considering xml files in the root directory is exluderoorDir is true).
	 * For each found file it converts it to xlsx format.
	 */
	public static void convertXMLDMLogsInDirToXLSXFormat(String directory,NLU nlu,boolean exludeRootDir) throws Exception {
		//List<File> logs = FileUtils.getAllFiles(new File("../../../support/dialogue_testbed/logs/"), "^.*\\.xml$",true);
		List<File> logs = FileUtils.getAllFiles(new File(directory), "^.*\\.xml$",exludeRootDir);
		if (logs!=null) {
			for(File f:logs) {
				System.out.println("converting: "+f);
				File xlsx=new File(f.getParent(),f.getName()+".xlsx");
				dumpXMLDMLogToXLSX(f, nlu, xlsx);
			}
		}
	}
	

	public static void dumpTrainingDataToExcel(List<TrainingDataFormat> tds,File xlsx, String sheetName) throws Exception {
		dumpTrainingDataToExcel(tds, xlsx, sheetName, new String[]{"label","utterance"});
	}
	public static void dumpTrainingDataToExcel(List<TrainingDataFormat> tds,File xlsx, String sheetName, String[] headings) throws Exception {
		dumpTrainingDataToExcel(tds, xlsx, sheetName, headings, 0, 1,false);
	}
	public static void dumpTrainingDataToExcel(List<TrainingDataFormat> tds,File xlsx, String sheetName, String[] headings,int keyColumn, int valueColumn,boolean preserveOrdering) throws Exception {
		if (preserveOrdering) {
			ExcelUtils.dumpListToExcel(tds, xlsx, sheetName, headings, keyColumn, valueColumn);
		} else {
			Map<String, List<TrainingDataFormat>> x = getAllSpeechActsWithTrainingData(tds);
			Map<String,List<String>> y=new LinkedHashMap<String, List<String>>();
			for(String key:x.keySet()) {
				List<TrainingDataFormat> utts=x.get(key);
				y.put(key, (List)FunctionalLibrary.map(utts, TrainingDataFormat.class.getMethod("getUtterance")));
			}
			ExcelUtils.dumpMapToExcel((Map)y, xlsx, sheetName, headings,keyColumn,valueColumn);
		}
	}
	public static List<TrainingDataFormat> extractTrainingDataFromExcel(File xlsx, int skip, int key, int value) throws Exception {
		return extractTrainingDataFromExcel(xlsx, skip, key, value, false);
	}
	public static List<TrainingDataFormat> extractTrainingDataFromExcel(File xlsx, int skip, int key, int value,boolean noAutofillLabel) throws Exception {
		return extractTrainingDataFromExcel(xlsx, skip, key, value, noAutofillLabel, true);
	}
	public static List<TrainingDataFormat> extractTrainingDataFromExcel(File xlsx, int skip, int key, int value,boolean noAutofillLabel,boolean discardEmptyLabels) throws Exception {
		Map<String, List<String>> data = ExcelUtils.extractMappingBetweenTheseTwoColumns(xlsx.getAbsolutePath(), skip, key, value,noAutofillLabel);
		List<TrainingDataFormat> ret=null;
		if (data!=null) {
			for(String k:data.keySet()) {
				List<String> us=data.get(k);
				if (us!=null) {
					for(String u:us) {
						if (ret==null) ret=new ArrayList<TrainingDataFormat>();
						if (StringUtils.isEmptyString(u) || StringUtils.isEmptyString(k)) {
							logger.error("Empty utterance or label: "+xlsx);
							logger.error("utterance: '"+u+"'");
							logger.error("label: '"+k+"'");
						}
						try {
							ret.add(new TrainingDataFormat(u, k,!discardEmptyLabels));
						} catch (Exception e) {
							logger.error("ignored instance because: "+e.getMessage());
							logger.error("Error processing file: "+xlsx);
						}
					}
				}
			}
		}
		return ret;
	}
	public static List<TrainingDataFormat> extractTrainingDataFromExcelMaintainingOrder(File xlsx, int skip, int key, int value,boolean noAutofillLabel) throws Exception {
		List<Pair<String, String>> data = ExcelUtils.extractListOfPairsBetweenTheseTwoColumns(xlsx.getAbsolutePath(), skip, key, value,noAutofillLabel,false);
		List<TrainingDataFormat> ret=null;
		if (data!=null) {
			for(Pair<String,String> kv:data) {
				String k=kv.getFirst();
				String u=kv.getSecond();
				if (ret==null) ret=new ArrayList<TrainingDataFormat>();
				if (StringUtils.isEmptyString(u) || StringUtils.isEmptyString(k)) {
					logger.error("Empty utterance or label: "+xlsx);
					logger.error("utterance: '"+u+"'");
					logger.error("label: '"+k+"'");
				}
				try {
					ret.add(new TrainingDataFormat(u, k,noAutofillLabel));
				} catch (Exception e) {
					logger.error(e);
					logger.error("Error processing file: "+xlsx);
				}
			}
		}
		return ret;
	}
	
	public static Set<String> getSessionIDsInTrainingData(List<TrainingDataFormat> tds) {
		Set<String> ret=null;
		if (tds!=null) {
			for(TrainingDataFormat td:tds) {
				if (!StringUtils.isEmptyString(td.getId())) {
					if (ret==null) ret=new HashSet<String>();
					ret.add(td.getId());
				}
			}
		}
		return ret;
	}
	public static List<TrainingDataFormat> extractTrainingDataWithTheseSessions(List<TrainingDataFormat> tds,Set<String> sids) {
		List<TrainingDataFormat> ret=null;
		if (tds!=null && sids!=null) {
			for (TrainingDataFormat td:tds) {
				if (sids.contains(td.getId())) {
					if (ret==null) ret=new ArrayList<TrainingDataFormat>();
					ret.add(td);
				}
			}
		}
		return ret;
	}
	

	
	public static void main(String[] args) throws Exception {
		
		File ros1=new File("resources/characters/Base-All/content/trainingData/standard/ros-trainingdata.babylon.xlsx");
		File ros2=new File("resources/characters/Base-All/content/NLU_Step1andStep3_ROS-to-13-values.xlsx");
		List<TrainingDataFormat> rostds1=extractTrainingDataFromExcel(ros1, 0, 4, 5);
		List<TrainingDataFormat> rostds2=extractTrainingDataFromExcel(ros2, 0, 0, 2);
		SAMapper.convertSAInTrainingData(rostds2);
		List<TrainingDataFormat> all2=new ArrayList<TrainingDataFormat>();
		BuildTrainingData.addToTrainingData(all2, rostds1, false);
		BuildTrainingData.addToTrainingData(all2, rostds2, false);
		FoldsData folds = new FoldsData(all2, 10);
		for(int i=0;i<folds.getNumberOfFolds();i++) {
			List<TrainingDataFormat> p=folds.buildTestingDataForFold(i);
			dumpTrainingDataToExcel(p, new File("annotation/ros-"+i+".xlsx"), "test");
		}
		System.exit(1);
		
		File x=new File("annotation/ros-trainingdata.babylon.xlsx");
		List<TrainingDataFormat> tds6 = BuildTrainingData.extractTrainingDataFromExcel(x, 0, 4, 5);
		SAMapper.convertSAInTrainingData(tds6);
		BuildTrainingData.dumpTrainingDataToExcel(tds6, new File("annotation/ros-trainingdata.babylon.xlsx"), "test");
		System.exit(1);
		/*
		List<String> nn=ExcelUtils.extractRowAndColumnWiseData(new File("annotation/complete_all_spk1_lines-after-annotation.xlsx").getAbsolutePath(), 2,0, 2, -1, true, true);
		List<TrainingDataFormat> tds5=new ArrayList<TrainingDataFormat>();
		for(String n:nn) {
			tds5.add(new TrainingDataFormat(n, null,true));
		}
		BuildTrainingData.dumpTrainingDataToExcel(tds5, new File("annotation/edits-single-column.xlsx"), "test");
		System.exit(1);
		*/
		
		File one=new File("annotation/output-expor.xlsx");
		File two=new File("annotation/output-expor-part2.xlsx");
		File three=new File("annotation/output-expor-part3.xlsx");
		File four=new File("annotation/output-expor-part-edits.xlsx");
		List<TrainingDataFormat> tds1=BuildTrainingData.extractTrainingDataFromExcel(one, 0, 0, 1, true);
		List<TrainingDataFormat> tds2=BuildTrainingData.extractTrainingDataFromExcel(two, 0, 0, 1, true);
		List<TrainingDataFormat> tds3=BuildTrainingData.extractTrainingDataFromExcel(three, 0, 0, 1, true);
		List<TrainingDataFormat> tds4=BuildTrainingData.extractTrainingDataFromExcel(four, 0, 0, 1, true);
		List<TrainingDataFormat> all=new ArrayList<TrainingDataFormat>();
		BuildTrainingData.addToTrainingData(all, tds1, false);
		BuildTrainingData.addToTrainingData(all, tds2, false);
		BuildTrainingData.addToTrainingData(all, tds3, false);
		BuildTrainingData.addToTrainingData(all, tds4, false);
		BuildTrainingData.dumpTrainingDataToExcel(all, new File("annotation/output-expor-part-merged.xlsx"), "test");
		System.exit(1);
		
		try {
			
			
			//System.out.println(stemm("i'm cars read reads redded"));
			BuildTrainingData btd = new BuildTrainingData(NLUConfig.WIN_EXE_CONFIG);
			List<TrainingDataFormat> td2 = btd.buildTrainingData();
			System.out.println(btd.prepareUtteranceForClassification("are you really smart the cars <num> lifted tomatoes running matters years months sometimes on the streets nothing quite"));
			System.exit(1);
			ArrayList<Pair<String, Collection<String>>> r = btd.readIORExportedLinks("exported-links.txt");
			int tot=0,mul=0;
			for(Pair<String, Collection<String>>d:r) {
				if (d.getSecond().size()>1) mul++;
				tot++;
			}
			System.out.println(tot+" "+mul);
			System.out.println(btd.prepareUtteranceForClassification("<NUM> years"));
			System.exit(1);

			List<TrainingDataFormat> td=btd.getAllSimcoachData();
			List<Pair<String, String>> sud=btd.getAllSystemUserPairs();
			HashMap<String, Pair<String,Collection<String>>> iorData = btd.produceDataForIOR(td,sud);
			btd.dumpIORDataInImportableTXT(iorData, "allSimcoachLines", "allSimcoachSpeechActs","allSimcoachLinks");
			System.exit(1);
			
			List<TrainingDataFormat> a = btd.buildTrainingDataFromFormsExcel("../../simcoach-runtime/SimcoachApp/src/forms.xlsx", 0);
			//btd.mergeUserUtterances("localhost:8080","sclab1");
			//System.out.println(btd.tokenize("??"));
			//ArrayList<Pair<String, TokenTypes>> tokens = btd.tokenize("forty-four");			
			System.out.println(btd.applyBasicTransformationsToStringForClassification("fourty six"));
			//System.out.println(EnglishWrittenNumbers2Digits.parseWrittenNumbers(tokens));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 * find the portion of the training data that uses labels listed in the set given as second argument. 
	 * @param tds
	 * @param sas
	 * @return
	 */
	public static Float findPortionCoveredByLabelsIn(List<TrainingDataFormat> tds,Set<String> sas) {
		if (tds!=null && sas!=null && !tds.isEmpty()) {
			int den=tds.size();
			int num=0;
			for(TrainingDataFormat td:tds) {
				String l=td.getLabel();
				if (sas.contains(l)) {
					num++;
					continue;
				}
				if (td.hasBackupLabels()) {
					for(String bl:td.getBackupLabels()) {
						if (sas.contains(bl)) {
							num++;
							break;
						}
					}
				}
				
			}
			return (float)num/(float)den;
		}
		return null;
	}
	/**
	 * returns the percentage of labels in sas that are found in the set of training data (second argument)
	 * @param sas
	 * @param tds
	 * @return
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws InstantiationException
	 */
	public static Float findPortionCoveredByLabelsIn(Set<String> sas,List<TrainingDataFormat> tds) throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
		if (tds!=null && sas!=null && !tds.isEmpty()) {
			int den=sas.size();
			int num=0;
			Set<String> foundLabels=getAllSpeechActsInTrainingData(tds);
			for(String l:foundLabels) {
				if (sas.contains(l)) {
					num++;
					continue;
				}
			}
			return (float)num/(float)den;
		}
		return null;
	}

	private static long id=0;
	public static BufferedWriter writeBikelEvalBFormat(File dumpFile,BufferedWriter out, boolean result) throws IOException {
		boolean first=out==null;
		if (first) out=(dumpFile!=null)?new BufferedWriter(new FileWriter(dumpFile)):null;
		if (out!=null) {
			if (first) {
				//  Sent.                        Matched  Bracket   Cross        Correct Tag
				// ID  Len.  Stat. Recal  Prec.  Bracket gold test Bracket Words  Tags Accracy
				// ============================================================================
				out.write(" Sent.                        Matched  Bracket   Cross        Correct Tag\n");
				out.write("ID  Len.  Stat. Recal  Prec.  Bracket gold test Bracket Words  Tags Accracy\n");
				out.write("============================================================================\n");
			}
			String p=(result)?"100.00":"0.00";
			String mb=(result)?"1":"0";
			out.write((id++)+"\t0\t0\t"+p+"\t"+p+"\t"+mb+"\t1\t1\t0\t0\t0\t0.0\n");
			//   1    8    0  100.00 100.00     5      5    5      0      6     5    83.33
		}
		return out;
	}
}
