package edu.usc.ict.nl.nlu.fst.train;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.fst.FSTNLUOutput;
import edu.usc.ict.nl.nlu.fst.TraverseFST;
import edu.usc.ict.nl.nlu.io.BuildTrainingData;
import edu.usc.ict.nl.nlu.trainingFileReaders.SimcoachUserXLSXFile;
import edu.usc.ict.nl.util.FileUtils;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.PerformanceResult;
import edu.usc.ict.nl.util.StreamGobbler;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.utils.ExcelUtils;

public class Aligner {
	
	protected File in=null;
	protected File out=null;
	protected File outputDir=null;
	protected File modeltxt=null;
	protected File alignerOutput=null;

	public Aligner(File outputDir) {
		this(outputDir,"input/sample.e","input/sample.nlu","output/training.align","alignments.txt");
	}
	public Aligner(File outputDir,String in,String out,String alignerOutput,String prettyPrintedAlignerOutput) {
		this.outputDir=outputDir;
		this.in=new File(outputDir,in);
		this.out=new File(outputDir,out);
		this.modeltxt=new File(outputDir,prettyPrintedAlignerOutput);
		this.alignerOutput=new File(outputDir,alignerOutput);
	}
	
	public List<Alignment> readAlignerOutputFile() throws Exception {
		return readAlignerOutputFile(in, out, alignerOutput);
	}
	/**
	 * all three files must have the same length. In the aligner file,
	 *  the first number in each par i-j must correspond to the source (english) file. 
	 * @param source the english file
	 * @param target the nlu file
	 * @param aligner the output of the aligner
	 * @throws IOException 
	 */
	public List<Alignment> readAlignerOutputFile(File source,File target,File aligner) throws IOException {
		List<Alignment> ret=null;
		BufferedReader sr=new BufferedReader(new FileReader(source));
		BufferedReader tr=new BufferedReader(new FileReader(target));
		BufferedReader ar=new BufferedReader(new FileReader(aligner));
		String s,t,a;
		int line=1;
		while((s=sr.readLine())!=null && (t=tr.readLine())!=null && (a=ar.readLine())!=null) {
			if (!StringUtils.isEmptyString(s) && !StringUtils.isEmptyString(t) && !StringUtils.isEmptyString(a)) {
				Alignment al;
				try {
					al = new Alignment(s,t,a);
					if (ret==null) ret=new ArrayList<Alignment>();
					ret.add(al);
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("Error while processing line "+line+" in these files:");
					System.err.println("source file: "+source);
					System.err.println("target file: "+target);
					System.err.println("aligner file: "+aligner);
				}
			}
			line++;
		}
		sr.close();tr.close();ar.close();
		return ret;
	}
	
	public void prepareNLUFileForAligner(File nluFile,File e,File f) throws Exception {
		Map<String, List<String>> map = ExcelUtils.extractMappingBetweenTheseTwoColumns(nluFile.getAbsolutePath(), 0, 0, 1);
		List<TrainingDataFormat> tds = BuildTrainingData.buildTrainingDataFromHash(map);
		prepareTDforAligner(tds,e,f);
	}
	
	protected void prepareTDforAligner(List<TrainingDataFormat> tds,File e,File f) throws Exception {
		if (tds!=null) {
			BufferedWriter ew=new BufferedWriter(new FileWriter(e));
			BufferedWriter fw=new BufferedWriter(new FileWriter(f));
			for(TrainingDataFormat td:tds) {
				String s=td.getUtterance();
				String l=td.getLabel();
				//s=BuildTrainingData.untokenize(BuildTrainingData.removeStopWords(BuildTrainingData.tokenize(s)));
				s=s.replaceAll("[-:]", "");
				s=BuildTrainingData.untokenize(BuildTrainingData.tokenize(s));
				l=StringUtils.cleanupSpaces(l);
				s=s.toLowerCase();
				l=l.toLowerCase();
				ew.write(s+"\n");
				fw.write(l+"\n");
			}
			ew.close();
			fw.close();
		}
	}

	public static List<TrainingDataFormat> getTrainingDataFromGoogle(File file,int header,int uttcol,int startLabelCol,int endLabelCol,int startRow,int endRow, Map<Integer, String> taxonomySAs) throws Exception {
		List<TrainingDataFormat> ret=new ArrayList<TrainingDataFormat>();
		Sheet sheet = ExcelUtils.getSpreadSheet(file.getAbsolutePath(), 0);
		if (sheet != null)
		{
			List<String> types=ExcelUtils.readRow(file, header, startLabelCol, endLabelCol);
			int typesSize=types.size();
			for(Iterator<Row> rowIter = sheet.rowIterator(); rowIter.hasNext(); ) {
				Row row = rowIter.next();
				// skip first row
				if (row.getRowNum()>=startRow && (endRow<0 || row.getRowNum()<=endRow)) {
					String u=null,l=null;
					boolean first=true;
					for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
						Cell cell = cellIter.next();
						if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
							int column=cell.getColumnIndex();
							int typeColumn=column-startLabelCol;
							if (column==uttcol) {//utterance
								String tmp=cell.getStringCellValue();
								tmp=StringUtils.cleanupSpaces(tmp);
								tmp=removeSpeechStuff(tmp);
								u=null;l=null;
								if (!StringUtils.isEmptyString(tmp)) u=tmp;
							} else if (column>=startLabelCol && (column<=endLabelCol || endLabelCol<0) && typesSize>typeColumn) {
								String value=cell.getStringCellValue();
								try {
									String[] parts=readNLUTokens(value);
									if (parts!=null) {
										for(String p:parts) {
											if (l==null) l="";
											l+=((first)?"":" ")+FSTNLUOutput.composeNLUKeyValuePair(types.get(typeColumn),p);
											first=false;
										}
									}
								} catch (Exception e) {
									throw new Exception(e.getMessage()+" at row: "+cell.getRowIndex()+" and column: "+column);
								}
							}
						}
					}
					if (u!=null && l!=null) {
						TrainingDataFormat td=new TrainingDataFormat(u, l);
						td.addExtraInfo(TrainingDataFormat.FILE, file.getAbsolutePath());
						Integer rowNumber=row.getRowNum();
						String taxonomySA=taxonomySAs.get(rowNumber);
						td.setId(taxonomySA);
						ret.add(td);
					}
				}
			}
		}
		return ret;
	}
	
	public static String removeSpeechStuff(String s) {
		s=s.replaceAll("<[^>]*>", "");
		s=s.replaceAll("\\[[^>]*\\]", "");
		s=s.replaceAll("\\{[^>]*\\}", "");
		s=s.replaceAll("[\\w]+\\-([\\s]+|$)", "");
		return s;
	}

	private static final Pattern blank=Pattern.compile("[\\s]+");
	private static String[] readNLUTokens(String input) throws Exception {
		Matcher m=blank.matcher(input);
		if (input.equals("0")) return null;
		if (m.find()) throw new Exception("string contains blanks: '"+input+"'");
		input=StringUtils.cleanupSpaces(input);
		input.replaceAll("[-:]", "_");
		String[] parts=input.split("[,;]");
		return parts;
	}
	
	/**
	 * 
	 * @param trainingCMD 
	 * @param google: step 3 of annotation([] utterance, utterance, nlu annotation in multiple columns)
	 * @param extra: phrase, single nlu annotation
	 * @param taxonomySA2Utterance: step 1 of annotation(taxonomy speech act, [] utterance)
	 * @throws Exception
	 */
	public void buildFSTFromGoogleXLSX(String[] trainingCMD, List<TrainingDataFormat> tds,File extra) throws Exception {
		List<TrainingDataFormat> atds =null;
		try {
			atds = BuildTrainingData.extractTrainingDataFromExcel(extra, 0, 0, 1);
		} catch (Exception e) {
			System.err.println("WARNING while using extra file (i.e. dictionary/lexicon) (set to: '"+extra+"') for alignment: "+e.getMessage());
		}
		if (atds==null) atds=new ArrayList<TrainingDataFormat>();
		atds.addAll(tds);
		prepareTDforAligner(atds,in, out);

		Process p = Runtime.getRuntime().exec(new String[]{"java", "-jar", "berkeleyaligner.jar","++example.conf"},null,outputDir);
		StreamGobbler output = new StreamGobbler(p.getInputStream(), "output",false,true);
		StreamGobbler error = new StreamGobbler(p.getErrorStream(), "error",true,false);
		output.start();
		error.start();
		p.waitFor();
		
		//process the output of the aligner
		List<Alignment> as = readAlignerOutputFile(in, out, alignerOutput);
		FileUtils.dumpToFile(FunctionalLibrary.printCollection(as, "", "", "\n"), modeltxt.getAbsolutePath());
		
		p = Runtime.getRuntime().exec(trainingCMD,null,outputDir);
		output = new StreamGobbler(p.getInputStream(), "output",false,true);
		error = new StreamGobbler(p.getErrorStream(), "error",true,false);
		output.start();
		error.start();
		p.waitFor();
	}
			
	public void generateTDForFirstStageNLU(List<TrainingDataFormat> tds, List<TrainingDataFormat> hpids,File stage1training) throws Exception {
		for(TrainingDataFormat td:tds) {
			td.setLabel("question.secondstage");
		}
		tds.addAll(hpids);
		BuildTrainingData.dumpTrainingDataToExcel(tds, stage1training, "training data", 
				new String[]{"UTTERANCE_ID","VERSION","CHARACTER","STATE","SPEECH_ACT","TEXT","IS_MENU_ACTION","NOTES","IS_CANONICAL"},
				SimcoachUserXLSXFile.getSpeechActColumn(),SimcoachUserXLSXFile.getUtteranceColumn(),
				false);
	}
		
	public List<TrainingDataFormat> extractTrainingDataFromGoogleXLSXForSPS(File step1, File step3) throws Exception {
		Map<Integer, String> taxonomySAs = ExcelUtils.extractRowAndThisColumn(step1.getAbsolutePath(), 0, 0);
		//prepare the training data
		List<TrainingDataFormat> tds=getTrainingDataFromGoogle(step3, 0, 1, 2, 11, 1, 1370,taxonomySAs);
		return tds;
	}
	public static List<TrainingDataFormat> extractTrainingDataFromSingleStep1and3GoogleXLSXForSPS(File step1AndStep3) throws Exception {
		Map<Integer, String> taxonomySAs = ExcelUtils.extractRowAndThisColumn(step1AndStep3.getAbsolutePath(), 0, 0);
		//prepare the training data
		List<TrainingDataFormat> tds=getTrainingDataFromGoogle(step1AndStep3, 0, 2, 3, 11, 1, -1,taxonomySAs);
		return tds;
	}
	
	private PerformanceResult evaluateFST(List<TrainingDataFormat> tds,	File in, File out, File model) throws Exception {
		TraverseFST tf = new TraverseFST(in, out, model, null);
		PerformanceResult ret=new PerformanceResult();
		for(TrainingDataFormat td:tds) {
			String label=td.getLabel();
			List<Pair<String,String>> expected=FSTNLUOutput.getPairsFromString(label);
			int expectedCount=expected.size();
			String text=td.getUtterance();
			String retFST=tf.getNLUforUtterance(text,1);
			List<FSTNLUOutput> results=tf.getResults(retFST);
			if (results!=null && !results.isEmpty()) {
				FSTNLUOutput first=results.get(0);
				List<Pair<String,String>> produced = FSTNLUOutput.getPairsFromString(first.getId());
				int producedCount=produced.size();
				int matchedCount=findBestIntersection(expected,produced);
				ret.add(expectedCount, producedCount, matchedCount);
			} else {
				ret.add(expectedCount, 0, 0);
			}
		}
		return ret;
	}

	private int findBestIntersection(List<Pair<String, String>> expected,List<Pair<String, String>> produced) {
		
		return 0;
	}
	
	public static void main(String[] args) throws Exception {
		String s=removeSpeechStuff("<UH-HUH> and y- you cannot breathe at all");
		System.out.println(s);
	}
}