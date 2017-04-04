package edu.usc.ict.nl.nlu.fst.sps.test;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.usc.ict.nl.bus.NLBus;
import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.config.PreprocessingConfig;
import edu.usc.ict.nl.nlu.DynamicFoldsData;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.fst.FSTNLU;
import edu.usc.ict.nl.nlu.fst.sps.SAMapper;
import edu.usc.ict.nl.nlu.io.BuildTrainingData;
import edu.usc.ict.nl.nlu.preprocessing.PreprocesserI;
import edu.usc.ict.nl.nlu.preprocessing.normalization.Chattifier;
import edu.usc.ict.nl.nlu.preprocessing.normalization.ContractEnglish;
import edu.usc.ict.nl.nlu.preprocessing.normalization.EnglishWrittenNumbers2Digits;
import edu.usc.ict.nl.nlu.preprocessing.normalization.Lowercase;
import edu.usc.ict.nl.nlu.preprocessing.normalization.SimcoachNormalizer;
import edu.usc.ict.nl.nlu.preprocessing.normalization.SimcoachPunctuationNormalizer;
import edu.usc.ict.nl.nlu.preprocessing.normalization.UK2USNormalizer;
import edu.usc.ict.nl.nlu.preprocessing.stemmer.KStemmer;
import edu.usc.ict.nl.nlu.preprocessing.tokenizer.Tokenizer;
import edu.usc.ict.nl.util.PerformanceResult;
import edu.usc.ict.nl.util.Triple;


public class NLURunner {
	
	public static String defaultInternalNluClass = "edu.usc.ict.nl.nlu.mxnlu.MXClassifierNLU";
	public static String defaultFeatureBuilderClass = "edu.usc.ict.nl.nlu.features.FeaturesBuilderForMXClassifier";
	
	public static NLU createNLU(File nluroot, File nluexeroot, File preprocessingDir, String internalNluClass, String featureBuilderClass, Float acceptanceThreshold) throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, CloneNotSupportedException {
		NLUConfig config=(NLUConfig) NLUConfig.WIN_EXE_CONFIG.clone();
		Logger.getRootLogger().setLevel(Level.OFF);
		//System.out.println("Building mx NLU");
		config.setForcedNLUContentRoot(nluroot.getAbsolutePath());
		config.setUserUtterances("user-utterances.xlsx");
		config.setNluExeRoot(nluexeroot.getAbsolutePath());
		
		// preprocessing
		List<PreprocesserI> pre = new ArrayList<>();
		pre.add(new Lowercase()); // lower cases input
		pre.add(new UK2USNormalizer()); // UK -> US
		pre.add(new SimcoachNormalizer()); // handles SimCoach specific words
		pre.add(new SimcoachPunctuationNormalizer()); // remove unnecessary punctuation
		pre.add(new Chattifier()); // chat to words (u -> you)
		pre.add(new ContractEnglish()); // you are -> you're
		pre.add(new EnglishWrittenNumbers2Digits()); // spelled out numbers -> digits
		pre.add(new KStemmer()); // converts input to standard present (was -> is)
		
		PreprocessingConfig preConfig = new PreprocessingConfig();
		preConfig.setNluTokenizer(new Tokenizer());
		preConfig.setNluPreprocessers(pre);
		try {
			preConfig.setForcedPreprocessingContentRoot(preprocessingDir.getAbsolutePath());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		config.setForcedNLUContentRoot(nluroot.getAbsolutePath());
		config.setPreprocessingTrainingConfig(preConfig);
		config.setPreprocessingRunningConfig(preConfig);
		
		//config.setInternalNluClass4Hier("edu.usc.ict.nl.nlu.jmxnlu.JMXClassifierNLU");
		//config.setNluClass("edu.usc.ict.nl.nlu.jmxnlu.JMXClassifierNLU");
		//config.setNluFeaturesBuilderClass("edu.usc.ict.nl.nlu.features.FeatureBuilderForJMXClassifier");
		//config.setAcceptanceThreshold(null);
		config.setInternalNluClass4Hier(internalNluClass);
		//config.setInternalNluClass4Hier("edu.usc.ict.nl.nlu.mallet.MalletMaxEntClassifierNLU");
		//config.setNluClass("edu.usc.ict.nl.nlu.hierarchical.HierarchicalNLU");
		//config.setNluClass("edu.usc.ict.nl.nlu.multi.MultiNLU");
		config.setNluClass(internalNluClass);
		//config.setNluClass("edu.usc.ict.nl.nlu.mallet.MalletMaxEntClassifierNLU");
		//config.setNluClass("edu.usc.ict.nl.nlu.jmxnlu.JMXClassifierNLU");
		config.setNluFeaturesBuilderClass(featureBuilderClass);
		//config.setNluFeaturesBuilderClass("edu.usc.ict.nl.nlu.features.FeatureBuilderForJMXClassifier");
		config.setAcceptanceThreshold(acceptanceThreshold);
		config.setRegularization(0.01f);
		if (internalNluClass.contains("MXClassifier"))
			config.setMaximumNumberOfLabels(255);
		else
			config.setMaximumNumberOfLabels(null);
		//config.setNluClass("edu.usc.ict.nl.nlu.mallet.MalletMaxEntClassifierNLU");

		config.setHierNluReturnsNonLeaves(false);
		config.setHierarchicalNluSeparator(".");
		config.setnBest(3);
		NLU nlu=(NLU) NLBus.createSubcomponent(config, config.getNluClass());
		return nlu;
	}
	public static NLU createHierNLU(File nluroot, File nluexeroot, String internalNluClass, String featureBuilderClass, Float acceptanceThreshold) throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, CloneNotSupportedException {
		NLUConfig config=(NLUConfig) NLUConfig.WIN_EXE_CONFIG.clone();
		Logger.getRootLogger().setLevel(Level.OFF);
		//System.out.println("Building mx hierarchical NLU");
		config.setForcedNLUContentRoot(nluroot.getAbsolutePath());
		config.setUserUtterances("user-utterances.xlsx");
		config.setNluExeRoot(nluexeroot.getAbsolutePath());
		//config.setInternalNluClass4Hier("edu.usc.ict.nl.nlu.jmxnlu.JMXClassifierNLU");
		//config.setNluClass("edu.usc.ict.nl.nlu.jmxnlu.JMXClassifierNLU");
		//config.setNluFeaturesBuilderClass("edu.usc.ict.nl.nlu.features.FeatureBuilderForJMXClassifier");
		//config.setAcceptanceThreshold(null);
		config.setInternalNluClass4Hier(internalNluClass);
		//config.setInternalNluClass4Hier("edu.usc.ict.nl.nlu.mallet.MalletMaxEntClassifierNLU");
		config.setNluClass("edu.usc.ict.nl.nlu.hierarchical.HierarchicalNLU");
		//config.setNluClass("edu.usc.ict.nl.nlu.multi.MultiNLU");
		//config.setNluClass("edu.usc.ict.nl.nlu.mxnlu.MXClassifierNLU");
		//config.setNluClass("edu.usc.ict.nl.nlu.mallet.MalletMaxEntClassifierNLU");
		//config.setNluClass("edu.usc.ict.nl.nlu.jmxnlu.JMXClassifierNLU");
		config.setNluFeaturesBuilderClass(featureBuilderClass);
		//config.setNluFeaturesBuilderClass("edu.usc.ict.nl.nlu.features.FeatureBuilderForJMXClassifier");
		config.setAcceptanceThreshold(acceptanceThreshold);
		config.setRegularization(0.01f);
		//config.setNluClass("edu.usc.ict.nl.nlu.mallet.MalletMaxEntClassifierNLU");
	
		config.setHierNluReturnsNonLeaves(false);
		config.setHierarchicalNluSeparator(".");
		config.setnBest(3);
		NLU nlu=(NLU) NLBus.createSubcomponent(config, config.getNluClass());
	return nlu;
	}
	public static NLU createSPSFST(File nluroot)  throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, CloneNotSupportedException {
		NLUConfig spsfst = createSPSFSTConfig(nluroot);

		NLU nlu=(NLU) NLBus.createSubcomponent(spsfst, spsfst.getNluClass());
		return nlu;
	}
	public static NLUConfig createSPSFSTConfig(File nluroot)  throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, CloneNotSupportedException {

		NLUConfig spsfst=(NLUConfig) NLUConfig.WIN_EXE_CONFIG.clone();
		spsfst.setNluClass("edu.usc.ict.nl.nlu.fst.sps.SPSFSTNLU");
		spsfst.setLowConfidenceEvent("internal.low-confidence");
		spsfst.setForcedNLUContentRoot(nluroot.getAbsolutePath());
		spsfst.setSpsMapperUsesNluOutput(false);
		spsfst.setSpsMapperModelFile("mapper.model");
		spsfst.setUserUtterances("ROS-FST-014.a.trainingdata.xlsx");
		spsfst.setNluModelFile("alignments.fst");
		spsfst.setFstInputSymbols("input.syms");
		spsfst.setFstOutputSymbols("output.syms");
		spsfst.setnBest(1);
		
		String[] fstargs = {"/bin/bash","-c","fstcompile --isymbols=%IN% --osymbols=%IN%|fstcompose - %MODEL%|fstshortestpath --nshortest=%NBEST% |fstprint --isymbols=%IN% --osymbols=%OUT%"};
		spsfst.setRunningFstCommand(fstargs);
				
		String[] fsttraining =  {
				"/bin/bash",
				"-c",
				"DYLD_LIBRARY_PATH=/opt/lib;DYLD_FALLBACK_LIBRARY_PATH=/opt/lib;PATH=$PATH:/bin:/usr/local/bin:/opt/bin; cat alignments.txt|ngramsymbols >alignments.syms; farcompilestrings -symbols=alignments.syms -keep_symbols=1 alignments.txt>alignments.far; ngramcount -order=5 alignments.far > alignments.cnts; ngrammake alignments.cnts >alignments.mod; fstprint alignments.mod >alignments.fst-txt; cat alignments.fst-txt |tr -s \"\t\" \" \"|/usr/local/bin/gsed -e's/^\\(.*[\t ]\\+\\)\\([^- ]\\+\\)-\\(.*\\)[\t ]\\+\\2-\\3\\([\t ]\\+.*\\)$/\\1\\2 \\3\\4/g'>alignments.fst-txt-processed;  cat alignments.fst-txt-processed |cut -d ' ' -f3|ngramsymbols >%IN%; cat alignments.fst-txt-processed |cut -d ' ' -f4|ngramsymbols >%OUT%; fstcompile --isymbols=%IN% --osymbols=%OUT% alignments.fst-txt-processed >%MODEL%;"
				};
		spsfst.setTrainingFstCommand(fsttraining);
		spsfst.setSpsMapperUsesNluOutput(false);
		return spsfst;
	}
	
	public static NLU createSPSWithoutFST(File nluroot, File backupnluroot, File nluexeroot, String internalNluClass, String featureBuilderClass) throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, CloneNotSupportedException {
		NLUConfig stage1_mh_and_hpi=(NLUConfig) NLUConfig.WIN_EXE_CONFIG.clone();
		stage1_mh_and_hpi.setNluClass(internalNluClass);
		stage1_mh_and_hpi.setNluExeRoot(nluexeroot.getAbsolutePath());
		stage1_mh_and_hpi.setForcedNLUContentRoot(nluroot.getAbsolutePath());
		stage1_mh_and_hpi.setUserUtterances("user-utterances.xlsx");
		stage1_mh_and_hpi.setInternalNluClass4Hier(internalNluClass);
		stage1_mh_and_hpi.setNluFeaturesBuilderClass(featureBuilderClass);
		stage1_mh_and_hpi.setAcceptanceThreshold(0.0f);
		//stage1_mh_and_hpi.setRegularization(0.1f);
		if (internalNluClass.contains("MXClassifier"))
			stage1_mh_and_hpi.setMaximumNumberOfLabels(255);
		else
			stage1_mh_and_hpi.setMaximumNumberOfLabels(null);
		
		NLUConfig stage2_ros=(NLUConfig) NLUConfig.WIN_EXE_CONFIG.clone();
		
		stage2_ros.setNluClass("edu.usc.ict.nl.nlu.hierarchical.HierarchicalNLU");
		//backup.setNluClass("edu.usc.ict.nl.nlu.mxnlu.MXClassifierNLU");
		stage2_ros.setForcedNLUContentRoot(backupnluroot.getAbsolutePath());
		stage2_ros.setInternalNluClass4Hier(internalNluClass);
		stage2_ros.setNluDir(backupnluroot.getAbsolutePath());
		stage2_ros.setNluFeaturesBuilderClass(featureBuilderClass);
		stage2_ros.setNluExeRoot(nluexeroot.getAbsolutePath());
		

		NLUConfig mainNLU=(NLUConfig) NLUConfig.WIN_EXE_CONFIG.clone();
		mainNLU.setForcedNLUContentRoot(nluroot.getAbsolutePath());
		mainNLU.setNluFeaturesBuilderClass(featureBuilderClass);
		mainNLU.setNluClass("edu.usc.ict.nl.nlu.multi.MultiNLU");
		
		Map<String,NLUConfig> nlus=new HashMap<String, NLUConfig>();
		nlus.put("stage1_mh_and_hpi", stage1_mh_and_hpi);
		nlus.put("stage2_ros", stage2_ros);
		mainNLU.setInternalNluListForMultiNlu(nlus);
		

		List<Triple<String,String,String>> args = new ArrayList<Triple<String,String,String>>();
		args.add(new Triple<String,String,String>("stage1_mh_and_hpi","question.secondstage","stage2_ros"));

		edu.usc.ict.nl.nlu.multi.merger.Cascade merger;
		try {
			merger = new edu.usc.ict.nl.nlu.multi.merger.Cascade("stage1_mh_and_hpi", args);
			mainNLU.setMergerForMultiNlu(merger);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		mainNLU.setnBest(3);
		NLU nlu=(NLU) NLBus.createSubcomponent(mainNLU, mainNLU.getNluClass());
		return nlu;
		
	}
	public static NLU createSPSNLU(File nluroot, File backupnluroot, File nluexeroot) throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, CloneNotSupportedException {

		//Logger.getRootLogger().setLevel(Level.OFF);
		//Logger.getLogger(SAMapper.class.getName()).setLevel(Level.OFF);
		//System.out.println("Building SPS 3 phase NLU (mx, fst, mxhier)");
		NLUConfig stage1=(NLUConfig) NLUConfig.WIN_EXE_CONFIG.clone();
		stage1.setNluClass("edu.usc.ict.nl.nlu.mxnlu.MXClassifierNLU");
		stage1.setNluExeRoot(nluexeroot.getAbsolutePath());
		stage1.setForcedNLUContentRoot(nluroot.getAbsolutePath());
		stage1.setUserUtterances("user-utterances.xlsx");
		stage1.setInternalNluClass4Hier("edu.usc.ict.nl.nlu.mxnlu.MXClassifierNLU");
		stage1.setNluFeaturesBuilderClass("edu.usc.ict.nl.nlu.features.FeaturesBuilderForMXClassifier");
		stage1.setAcceptanceThreshold(0.0f);
		
		NLUConfig stage1andfst=(NLUConfig) NLUConfig.WIN_EXE_CONFIG.clone();
		stage1andfst.setForcedNLUContentRoot(nluroot.getAbsolutePath());
		stage1andfst.setNluClass("edu.usc.ict.nl.nlu.multi.MultiNLU");
		
		NLUConfig spsfst = createSPSFSTConfig(nluroot);
		
		Map<String,NLUConfig> nlusStage1AndFst=new HashMap<String, NLUConfig>();
		nlusStage1AndFst.put("stage1", stage1);
		nlusStage1AndFst.put("stage2", spsfst);
		stage1andfst.setInternalNluListForMultiNlu(nlusStage1AndFst);

		List<Triple<String,String,String>> args = new ArrayList<Triple<String,String,String>>();
		args.add(new Triple<String,String,String>("stage1","question.secondstage","stage2"));
 		
		edu.usc.ict.nl.nlu.multi.merger.Cascade merger;
		try {
			merger = new edu.usc.ict.nl.nlu.multi.merger.Cascade("stage1", args);
			stage1andfst.setMergerForMultiNlu(merger);
		} catch (Exception e) {
			e.printStackTrace();
		}
		NLUConfig backup=(NLUConfig) NLUConfig.WIN_EXE_CONFIG.clone();
	
		backup.setNluClass("edu.usc.ict.nl.nlu.hierarchical.HierarchicalNLU");
		//backup.setNluClass("edu.usc.ict.nl.nlu.mxnlu.MXClassifierNLU");
		backup.setForcedNLUContentRoot(backupnluroot.getAbsolutePath());
		backup.setInternalNluClass4Hier("edu.usc.ict.nl.nlu.mxnlu.MXClassifierNLU");
		backup.setNluDir(backupnluroot.getAbsolutePath());
		backup.setNluFeaturesBuilderClass("edu.usc.ict.nl.nlu.features.FeaturesBuilderForMXClassifier");
		backup.setNluExeRoot(nluexeroot.getAbsolutePath());
		
		NLUConfig mainNLU=(NLUConfig) NLUConfig.WIN_EXE_CONFIG.clone();
		mainNLU.setForcedNLUContentRoot(nluroot.getAbsolutePath());
		mainNLU.setNluFeaturesBuilderClass("edu.usc.ict.nl.nlu.features.FeaturesBuilderForMXClassifier");
		mainNLU.setUserUtterances("NLU_Step1andStep3_ROS-to-13-values.xlsx");
		mainNLU.setNluClass("edu.usc.ict.nl.nlu.multi.MultiNLU");
		
		Map<String,NLUConfig> nlus=new HashMap<String, NLUConfig>();
		nlus.put("fst", stage1andfst);
		nlus.put("backup", backup);
		mainNLU.setInternalNluListForMultiNlu(nlus);
		
		args = new ArrayList<Triple<String,String,String>>();
		args.add(new Triple<String,String,String>("fst","internal.low-confidence","backup"));
		edu.usc.ict.nl.nlu.multi.merger.Cascade merger2;
		try {
			merger2 = new edu.usc.ict.nl.nlu.multi.merger.Cascade("fst", args);
			mainNLU.setMergerForMultiNlu(merger2);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		mainNLU.setnBest(3);
		NLU nlu=(NLU) NLBus.createSubcomponent(mainNLU, mainNLU.getNluClass());
		Logger.getRootLogger().setLevel(Level.OFF);
		Logger.getLogger(NLU.class.getName()).setLevel(Level.OFF);
		Logger.getLogger(nlu.getClass().getName()).setLevel(Level.OFF);
		Logger.getLogger(SAMapper.class.getName()).setLevel(Level.OFF);
		Logger.getLogger(BuildTrainingData.class.getName()).setLevel(Level.OFF);
		return nlu;
	}
	
	private static NLU train(NLU nlu, List<File> trainingFiles, List<File> trainingFilesToRelabel, String targetRelabel, Boolean analyzeOnly, Map<String,List<String>> labelsForListValues, Boolean cleanupUtterance) throws Exception {
		NLUConfig config = nlu.getConfiguration();
		if (!nlu.getClass().getName().contains("FST")) {
			List<TrainingDataFormat> tds = new ArrayList<TrainingDataFormat>();
			if (!config.getNluClass().toLowerCase().contains("fst")) {
				System.out.println("Preparing training data...");
				for (File file: trainingFiles) { 
					System.out.println(file.getName());
					tds.addAll(BuildTrainingData.extractTrainingDataFromExcel(file, 0, 4, 5,true));
					
				}
				
				if (trainingFilesToRelabel.size() > 0)
					System.out.println("Preparing relabeled training data [" + targetRelabel+ "]...");
				for (File file:trainingFilesToRelabel) {
					System.out.println(file.getName());
					List<TrainingDataFormat> tdsForRelabel = BuildTrainingData.extractTrainingDataFromExcel(file, 0, 4, 5,true);
					for (TrainingDataFormat td:tdsForRelabel) {
						td.setLabel(targetRelabel);
					}
					tds.addAll(tdsForRelabel);
				}
				
				if (cleanupUtterance) {
					for (TrainingDataFormat td: tds) {
						td.setUtterance(cleanupUtterance(td.getUtterance()));
					}
				}
			}
			
			if (analyzeOnly || labelsForListValues.size() > 0) {
				System.out.println("Analyzing training data...");
				Map<String,Integer> depth3 = new HashMap<String,Integer>();
				Map<String,Integer> depth2 = new HashMap<String,Integer>();
				Map<String,Integer> depth1 = new HashMap<String,Integer>();
				for (TrainingDataFormat td:tds) {
					//ignore question.ros for depth
					final String d3 = td.getLabel();
					String d2 = d3.substring(0,d3.lastIndexOf('.'));
					String d1 = null;
					if (d2.lastIndexOf('.')>0)
						d1 = d2.substring(0,d2.lastIndexOf('.'));
					if (!depth3.containsKey(d3)) 
						depth3.put(d3, 0);
					depth3.put(d3,depth3.get(d3)+1);
					if (!depth2.containsKey(d2)) 
						depth2.put(d2,0);
					depth2.put(d2, depth2.get(d2)+1);
					if (d1 != null && !depth1.containsKey(d1)) 
						depth1.put(d1, 0);
					if (d1 != null)
						depth1.put(d1, depth1.get(d1)+1);
					if (labelsForListValues.containsKey(td.getLabel()))
						labelsForListValues.get(td.getLabel()).add(td.getUtterance());
				}
				
				if (labelsForListValues.size()>0) {
					System.out.println("Values for labels:");
					System.out.println("-------------------");
					for (String label:labelsForListValues.keySet()) {
						System.out.println();
						System.out.println(label);
						System.out.println("-------------------");
						for (String value:labelsForListValues.get(label)) {
							System.out.println(value);
						}
					}
				}
				
				if (analyzeOnly) {
					System.out.println("Leaves " );
					Map<String,Integer> histogram = depth3;
					List<String> sorted = new LinkedList<String>();
					for (String key:histogram.keySet()) {
						
						sorted.add(key);
					}
					Collections.sort(sorted);
					int count=0;
					for (String key:sorted) {
						System.out.println(key + ": (" + histogram.get(key) + ")");
						count+=histogram.get(key);
					}
					histogram = depth2;
					sorted = new LinkedList<String>();
					for (String key:histogram.keySet()) {
						
						sorted.add(key);
					}
					Collections.sort(sorted);
					System.out.println("Leaves - 1: " );
					for (String key:sorted) {
						System.out.println(key + ": (" + histogram.get(key) + ")");
					}
					histogram = depth2;
					for (String key:histogram.keySet())
						System.out.println(key + ": (" + histogram.get(key) + ")");
					System.out.println("Leaves - 2: " );
					histogram = depth1;
					for (String key:histogram.keySet())
						System.out.println(key + ": (" + histogram.get(key) + ")");
					System.out.println("Total # items: " + tds.size());
					assert(count==tds.size());
				}
			}
			else {
				System.out.println("Training...");
				nlu.train(tds, new File(config.getNluModelFile()));
				nlu.kill();
			}
		} else {
			if (analyzeOnly) {
				System.out.println("Analysis of FST data not yet available.");
				return null;
			} else {
				FSTNLU fstNLU = (FSTNLU)nlu;
				fstNLU.retrain(trainingFiles.toArray(new File[0]));
			}
		}
		System.out.println("Done.");
		return nlu;
	}

	private static void testData(List<TrainingDataFormat> test, NLU nlu, PerformanceResult perfResult, List<Triple<String,String,String>> mismatches) throws Exception {
		Iterator<TrainingDataFormat>it=test.iterator();
		while(it.hasNext()) {
			TrainingDataFormat td=it.next();
			List<NLUOutput> output = nlu.getNLUOutput(td.getUtterance(), null, nbest);
			if (td.getLabel() == null || td.getLabel().isEmpty() || td.getUtterance() == null || td.getUtterance().isEmpty())
				continue;
			
			if (!td.match(output.get(0).getId())) {
				perfResult.add(false);
				mismatches.add(new Triple<String,String,String>(td.getLabel(),td.getUtterance(),output.get(0).getId()));
			}
			else {
				perfResult.add(true);
			}
		}
	}

	
	static Pattern removeTags = Pattern.compile("</?[^/>]*>");
	static Pattern removeParentheticals = Pattern.compile("[({][^()].*[)}]");
	static Pattern removeTrailingPunctuation = Pattern.compile("[^a-zA-Z]+$");
	
	private static String cleanupUtterance(String utt) {
		String replacement = removeTags.matcher(utt).replaceAll("");
		replacement = removeParentheticals.matcher(replacement).replaceAll("");
		replacement = removeTrailingPunctuation.matcher(replacement).replaceAll("");
		return replacement;
	}
	
	static int nbest = 1;
	public static void usage() {
		System.out.println("Usage: runnlu [--outputResults filename.xlsx] [--dir nlurootdir] [--dirBackup nlubackuprootdir] [--preprocessingDir preprocessingdirectory] [--type mx | mxhier | spsfst | spsnlu | spsnlu_nofst] [--mxDir mxnlu-dir] {--train trainingfile-1 trainingfile-2... trainingfile-n} {--listValuesForLabels [label1 ... labeln] {--relabel trainingfile-1 ... trainingfile-n } {--test testfile-1 testfile-2... testfile-n}");
	}
	public static void main(String[] args) throws Exception {
		
		
		boolean train = false;
		List<File> trainFiles = new ArrayList<File>();
		List<File> relabelTrainFiles = new ArrayList<File>();
		List<File> testFiles = new ArrayList<File>();
		File nluRoot = null;
		File nluRootBackup = null;
		File mxDir = null;
		File preprocessingDir = null;
		String nluClass = NLURunner.defaultInternalNluClass;
		String featureBuilderClass = NLURunner.defaultFeatureBuilderClass;
		String nluType = null;
		String relabel = null;
		Boolean analyzeOnly = false;
		Boolean cleanupUtterance = false;
		Float acceptanceThreshold = 0.0f;
		Map<String,List<String>> labelsForListValues = new HashMap<String,List<String>>();
		Integer numFolds = null;
		
		for (int i=0;i<args.length;i++) {
			
			if (args[i].equalsIgnoreCase("--type")) {
				nluType = args[i+1];
			}
			if (args[i].equalsIgnoreCase("--listValuesForLabel") || args[i].equalsIgnoreCase("--labels")) {
				for (int j=i+1;j<args.length;j++) {
					if (args[j].contains("--"))
						break;
					labelsForListValues.put(args[j],new ArrayList<String>());
				}
			}
			if (args[i].equalsIgnoreCase("--cleanupUtterance")) {
				cleanupUtterance = true;
				System.out.println("Configured for utterance cleanup.");
			}
			if (args[i].equalsIgnoreCase("--analyzeOnly"))
				analyzeOnly = true;
			if (args[i].equalsIgnoreCase("--nfolds"))
				numFolds = Integer.parseInt(args[i+1]);
			if (args[i].equalsIgnoreCase("--train")) {
				train = true;
				for (int j=i+1;j<args.length;j++) {
					if (args[j].contains("--"))
						break;
					File file = new File(args[j]);
					if (!file.exists() ) {
						System.err.println("File does not exist: " + args[j]);
						usage();
						System.exit(2);
					}
					trainFiles.add(file);
				}
			}	
			if (args[i].equalsIgnoreCase("--relabel")) {
				for (int j=i+1;j<args.length;j++) {
					if (args[j].contains("--"))
						break;
					File file = new File(args[j]);
					if (!file.exists() ) {
						System.err.println("File does not exist: " + args[j]);
						usage();
						System.exit(2);
					}
					relabelTrainFiles.add(file);
				}
			}
			if (args[i].equalsIgnoreCase("--test")) {
				for (int j=i+1;j<args.length;j++) {
					if (args[j].contains("--"))
						break;
					File file = new File(args[j]);
					if (!file.exists()) {
						System.err.println("File does not exist: " + args[j]);
						usage();
						System.exit(2);
					}
					testFiles.add(file);
				}
			}
			if (args[i].equalsIgnoreCase("--dir")) {
				nluRoot = new File(args[i+1]);
				if (!nluRoot.exists()) {
					System.err.println("File does not exist: " + args[i+1]);
					usage();
					System.exit(2);
				}
			}
			if (args[i].equalsIgnoreCase("--dirBackup")) {
				nluRootBackup = new File(args[i+1]);
				if (!nluRootBackup.exists()) {
					System.err.println("File does not exist: " + args[i+1]);
					usage();
					System.exit(2);
				}
			}
			if (args[i].equalsIgnoreCase("--mxDir")) {
				mxDir = new File(args[i+1]);
				if (!mxDir.exists()) {
					System.err.println("File does not exist: " + args[i+1]);
					usage();
					System.exit(2);
				}
			}
			if (args[i].equalsIgnoreCase("--relabelTarget")) {
				relabel = args[i+1];
			}
			if (args[i].equalsIgnoreCase("--acceptanceThreshold")) {
				acceptanceThreshold = Float.valueOf(args[i+1]);
			}
			if (args[i].equalsIgnoreCase("--nluClass")) {
				nluClass = args[i+1];
			}
			if (args[i].equalsIgnoreCase("--featureBuilderClass")) {
				featureBuilderClass = args[i+1];
			}
			if (args[i].equalsIgnoreCase("--preprocessingDir")) {
				preprocessingDir = new File(args[i+1]);
				if (!preprocessingDir.exists()) {
					System.err.println("Preprocessing dir does not exist: " + args[i+1]);
					usage();
					System.exit(2);;
				}
			}
		}
		if (nluType == null || nluRoot == null) {
			usage();
			System.exit(2);
		}
		
		NLU nlu = null;
		if (mxDir == null) {
			System.out.println("Expecting 'mxDir' to be defined");
			usage();
			System.exit(2);
		}
		if (nluType.equalsIgnoreCase("mx")) {
			nlu = createNLU(nluRoot, mxDir, preprocessingDir, nluClass,featureBuilderClass,acceptanceThreshold);
		} else if (nluType.equalsIgnoreCase("mxhier")) {
			nlu = createHierNLU(nluRoot,mxDir,nluClass,featureBuilderClass,acceptanceThreshold);
		} else if (nluType.equalsIgnoreCase("spsfst")) {
			nlu = createSPSFST(nluRoot);
		} else if (nluType.equalsIgnoreCase("spsnlu")) {	
		//stage1 mx+stage2 fst+stage3 hier backup
			if (nluRootBackup == null) {
				System.out.println("Expecting 'dirBackup' to be defined");
				usage();
				System.exit(2);
			}
			nlu = createSPSNLU(nluRoot,nluRootBackup,mxDir);
		} else if (nluType.equalsIgnoreCase("spsnlu_nofst")) {
			//stage1 mx + stage2 hier ros
			nlu = createSPSWithoutFST(nluRoot,nluRootBackup,mxDir,nluClass,featureBuilderClass);
		}	else {
			usage();
			System.exit(2);
		}
		if (train) {
			nlu = train(nlu, trainFiles, relabelTrainFiles, relabel, analyzeOnly, labelsForListValues, cleanupUtterance);
		} 
		if (nlu == null) {
			System.out.println("Error in training, exiting.");
			System.exit(2);
		}
	
		PerformanceResult perfResult = new PerformanceResult();
		List<PerformanceResult> nFoldsResults = new ArrayList<PerformanceResult>();
		List<Triple<String,String,String>> mismatches = new ArrayList<Triple<String,String,String>>();
		List<TrainingDataFormat> allTestItems = new ArrayList<TrainingDataFormat>();
		
		if (testFiles.size() > 0) {
			System.out.println("Preparing test data");
			for (File testFile:testFiles) {
				System.out.println(testFile.getName());
				allTestItems.addAll(BuildTrainingData.extractTrainingDataFromExcel(testFile, 0, 4, 5, true,false));
			}
		}
		if (testFiles.size() > 0 && relabelTrainFiles.size() > 0) { 
			System.out.println("Preparing relabeled test data [" + relabel+ "]");
			for (File testFile:relabelTrainFiles) {
				System.out.println(testFile.getName());
				List<TrainingDataFormat> tdsForRelabel = BuildTrainingData.extractTrainingDataFromExcel(testFile, 0, 4, 5,true);
				for (TrainingDataFormat td:tdsForRelabel)
					td.setLabel(relabel);
				allTestItems.addAll(tdsForRelabel);
			}	
		}
		if (cleanupUtterance) 
			for (TrainingDataFormat td: allTestItems) {
				td.setUtterance(cleanupUtterance(td.getUtterance()));
			}
		if (allTestItems.size() > 0) {
			System.out.println("Total test items: " + allTestItems.size());
			if (numFolds == null) {
	
				testData(allTestItems, nlu, perfResult, mismatches);
				//if (outputResults) {
					//String[] headers = {"match","label","utt","classification"};
					//ExcelUtils.dumpListToExcel(test, new File(testFile.getParent(),testFile.getName()+"-results.xlsx"), "data", headers, 1,2);
				//}
							
			} else {
				
				DynamicFoldsData dfs = BuildTrainingData.produceDynamicFolds(allTestItems, numFolds);
				NLUConfig config = nlu.getConfiguration();
				BuildTrainingData btd = new BuildTrainingData(config);
				List<TrainingDataFormat> trainingData = (allTestItems!=null)?allTestItems:btd.buildTrainingData(),testingData=null;
				for(int i=0;i<dfs.getNumberOfFolds();i++) {
					PerformanceResult t=new PerformanceResult();
					trainingData = dfs.buildTrainingDataForFold(i,trainingData);
					File model=new File(config.getNLUContentRoot(),"fold-model-"+i+".model");
					nlu.train(trainingData, model);
					nlu.kill();
					nlu.loadModel(model);
					testingData = dfs.buildTestingDataForFold(i, testingData);
					testData(testingData,nlu,t,mismatches);
					nFoldsResults.add(t);
					nlu.kill();
				}
				
			}
			
			//output results
			if (numFolds == null) {
				System.out.println("MISMATCH INFORMATION");
				System.out.println("LABEL,UTT,CLASSIFICATION");
				for (Triple<String,String,String> mismatch:mismatches) {
					System.out.println(mismatch.getFirst()+","+mismatch.getSecond()+","+mismatch.getThird());
				}
				
				System.out.println("Count: " + perfResult.getProduced());
				System.out.println("Correct: " + perfResult.getCorrect());
				System.out.println("Errors: " + (perfResult.getProduced() - perfResult.getCorrect()));
				System.out.println("Precision: "+ perfResult.getPrecision());
				//System.out.println("Accuracy: "+ perfResult.getAccuracy());
				//System.out.println("Performance: "+  (  ((double)correct) / ((double)comparisons) )*100.0);
			}
			else {
				System.out.println("Performance analysis for " + nFoldsResults.size() + " folds:");
				float totalCount = 0.0f;
				float totalCorrect = 0.0f;
				for (PerformanceResult result:nFoldsResults) {
					totalCount+= result.getProduced();
					totalCorrect+= result.getCorrect();
					
				}
				System.out.println("Total Count: " +totalCount);
				System.out.println("Total Correct: " + totalCorrect);
				System.out.println("Performance: "+ totalCorrect / totalCount);
			}
		}
			
		
		
	}
	
	
}
