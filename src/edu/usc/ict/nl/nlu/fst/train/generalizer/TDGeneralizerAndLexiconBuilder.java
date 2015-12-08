package edu.usc.ict.nl.nlu.fst.train.generalizer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.fst.FSTNLUOutput;
import edu.usc.ict.nl.nlu.fst.sps.SAMapper;
import edu.usc.ict.nl.nlu.fst.sps.test.NLUTest;
import edu.usc.ict.nl.nlu.fst.train.Aligner;
import edu.usc.ict.nl.nlu.fst.train.Alignment;
import edu.usc.ict.nl.nlu.fst.train.AlignmentSummary;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.utils.ExcelUtils;

public class TDGeneralizerAndLexiconBuilder {

	List<Pattern> patterns=null; 
	/**
	 * provides the list of labels that the training data must comply with.
	 * Any training data point must use a label that contain as prefix one of the valid labels provided.
	 * The suffix part goes into the lexicon,
	 * @param labels
	 */
	public TDGeneralizerAndLexiconBuilder(String... labels) {
		patterns=new ArrayList<Pattern>();
		if (labels!=null) {
			for(String l:labels) {
				patterns.add(Pattern.compile(l));
			}
		}
	}

	public TDGeneralizerAndLexiconBuilder() {
		this("^(utterance_type:.*)$",
				"^(predicate:)([^\\:]+)$",
				//"(predicate_modifier:)([^\\:]+)",
				"^(predicate_modifier:(.+:)?)([^:]+)$",
				"^(pred_modifier:(.+:)?)([^:]+)$",
				"^(object:(.+:)?)([^:]+)$",
				"^(object_modifier:(.+:)?)([^:]+)$",
				"^(time:(.+:)?)([^:]+)$",
				"^(temporal_complement:(.+:)?)([^:]+)$",
				"^(location:(.+:)?)([^:]+)$",
				"^(source:(.+:)?)([^:]+)$");
	}

	public GeneralizedAnnotation createGeneralizerFromTrainingData(List<TrainingDataFormat> tds) {
		GeneralizedAnnotation lexicon=null;
		if (tds!=null) {
			for(TrainingDataFormat td:tds) {
				String label=td.getLabel();
				List<Pair<String, String>> decomposedLabel = FSTNLUOutput.getPairsFromString(label);
				for(Pair<String, String> kv:decomposedLabel) {
					label=kv.getFirst()+FSTNLUOutput.keyValueSep+kv.getSecond();
					boolean added=false;
					boolean patternFound=false;
					for (Pattern p:patterns) {
						Matcher m=p.matcher(label);
						if (m.matches()) {
							patternFound=true;
							if (m.groupCount()>1) {
								String base=m.group(1);
								String lex=m.group(m.groupCount());
								if (!StringUtils.isEmptyString(lex)) {
									base=base.replaceFirst(FSTNLUOutput.keyValueSep+"$", "");
									String lexiconKey=base;
									String[] baseParts=base.split(String.valueOf(FSTNLUOutput.keyValueSep));
									if (baseParts.length>1) {
										lexiconKey=baseParts[baseParts.length-1];
										StringBuffer b=new StringBuffer();
										boolean first=true;
										for(int i=0;i<baseParts.length-1;i++) {
											b.append(baseParts[i]+((first?"":FSTNLUOutput.keyValueSep)));
											first=false;
										}
										base=b.toString();
									}
									if (lexicon==null) lexicon=new GeneralizedAnnotation(String.valueOf(FSTNLUOutput.keyValueSep));
									lexicon.addLeafConceptForParentOfPath(lex,lexiconKey,label);
									added=true;
								}
							}
							break;
						}
					}
					if (!patternFound) System.err.println("no pattern found for: "+label);
					if (!added) {
						if (lexicon==null) lexicon=new GeneralizedAnnotation(String.valueOf(FSTNLUOutput.keyValueSep));
						lexicon.addLeafConceptForParentOfPath("","",label);
					}
				}
			}
		}
		return lexicon;
	}

	public List<TrainingDataFormat> readTDFiles(File... files) {
		List<TrainingDataFormat> itds=null;
		for(File u:files) {
			List<TrainingDataFormat> tds=null;
			try {
				tds = Aligner.extractTrainingDataFromSingleStep1and3GoogleXLSXForSPS(u);
				if (tds!=null) {
					if (itds==null) itds=new ArrayList<TrainingDataFormat>();
					itds.addAll(tds);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return itds;
	}

	public static void main(String[] args) throws Exception {
		//NLUConfig config=getNLUConfig("spsFSTbackup");
		NLUConfig config=NLU.getNLUConfig("FSTNLU");
		//NLUConfig config=getNLUConfig("spsFST");
		NLBusConfig tmp = NLBusConfig.WIN_EXE_CONFIG;
		tmp.setCharacter("Base-All");
		tmp.setNluConfig(config);
		NLU nlu=NLU.init(config);
		nlu.retrain(NLUTest.ros1,NLUTest.ros2,NLUTest.ros3,NLUTest.ros5,NLUTest.ros6,NLUTest.ros7,NLUTest.ros9);
		Aligner a=new Aligner(new File(config.getNLUContentRoot()),config);
		List<Alignment> as = a.readAlignerOutputFile();
		AlignmentSummary asummary = new AlignmentSummary(as);
		Map<String, Map<String, Integer>> phrases = asummary.getNluConcepts2phrases();
		TDGeneralizerAndLexiconBuilder gen = new TDGeneralizerAndLexiconBuilder();
		List<TrainingDataFormat> tds = gen.readTDFiles(NLUTest.ros1,NLUTest.ros2,NLUTest.ros3,NLUTest.ros5,NLUTest.ros6,NLUTest.ros7,NLUTest.ros9);
		GeneralizedAnnotation lexicon = gen.createGeneralizerFromTrainingData(tds);
		/*
		Map<String, Set<String>> prefixes = lexicon.getPrefixes();
		ExcelUtils.dumpMapToExcel((Map)prefixes, new File("test-lexicon-prefixes.xlsx"), "test", null);
		Map<String, Set<String>> lexiconContent = lexicon.buildActualLexicon(phrases);
		ExcelUtils.dumpMapToExcel((Map)lexiconContent, new File("test-lexicon-content.xlsx"), "test", null);
		 */
		lexicon.generalizeTrainingData(tds);
		List<List<String>> toDump=SAMapper.export(tds);
		ExcelUtils.dumpDataToExcel(toDump, new File("output-generalized-test.xlsx"), "test", null);
	}
}
