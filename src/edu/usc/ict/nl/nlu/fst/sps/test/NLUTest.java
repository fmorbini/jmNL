package edu.usc.ict.nl.nlu.fst.sps.test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.DynamicFoldsData;
import edu.usc.ict.nl.nlu.FoldsData;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.fst.FSTNLU;
import edu.usc.ict.nl.nlu.fst.FSTNLUOutput;
import edu.usc.ict.nl.nlu.fst.TraverseFST;
import edu.usc.ict.nl.nlu.fst.sps.SAMapper;
import edu.usc.ict.nl.nlu.fst.sps.SPSFSTNLU;
import edu.usc.ict.nl.nlu.fst.train.Aligner;
import edu.usc.ict.nl.nlu.fst.train.AlignmentSummary;
import edu.usc.ict.nl.nlu.io.BuildTrainingData;
import edu.usc.ict.nl.nlu.multi.MultiNLU;
import edu.usc.ict.nl.nlu.preprocessing.TokenizerI;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.PerformanceResult;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.utils.ExcelUtils;

public class NLUTest extends FSTNLU {

	public static final File hpi=new File("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\resources\\characters\\Base-All\\content\\raw-training-data\\hpi-social-trainingdata.xlsx");
	public static final File mh=new File("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\resources\\characters\\Base-All\\content\\raw-training-data\\mh-trainingdata.xlsx");
	//private static final File ros=new File("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\resources\\characters\\Base-All\\content\\raw-training-data\\ros-trainingdata.xlsx");
	public static final File ros=new File("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\resources\\characters\\Base-All\\content\\raw-training-data\\NLU_Step1andStep3_ROS-to-13-values.xlsx");
	public static final File ros1=new File("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\resources\\characters\\Base-All\\content\\ros-1.xlsx");
	public static final File ros2=new File("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\resources\\characters\\Base-All\\content\\ros-2.xlsx");
	public static final File ros3=new File("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\resources\\characters\\Base-All\\content\\ros-3.xlsx");
	public static final File ros5=new File("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\resources\\characters\\Base-All\\content\\ros-5.xlsx");
	public static final File ros6=new File("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\resources\\characters\\Base-All\\content\\ros-6.xlsx");
	public static final File ros7=new File("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\resources\\characters\\Base-All\\content\\ros-7.xlsx");
	public static final File ros9=new File("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\resources\\characters\\Base-All\\content\\ros-9.xlsx");
	
	public static final File testFile=new File("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\annotation\\output.xlsx");
	public static final File testFile2=new File("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\annotation\\output-expor.xlsx");

	public NLUTest(NLUConfig c) throws Exception {
		super(c);
	}

	public static void spsTest(int stage,boolean printErrors) throws Exception {
		NLUConfig config=getNLUConfig("spsNLU");
		NLBusConfig tmp = NLBusConfig.WIN_EXE_CONFIG;
		tmp.setCharacter("Base-All");
		tmp.setNluConfig(config);
		NLU nlu=init(config);
		Map<String, NLU> inlu = ((MultiNLU)nlu).getHNLU();

		switch (stage) {
		case 2:
			NLU nlu1 = inlu.get("stage2");
			String uu=nlu1.getConfiguration().getUserUtterances();
			System.out.println("stage 2: "+uu);
//			List<TrainingDataFormat> tds = BuildTrainingData.extractTrainingDataFromExcel(new File(uu), 0, 0, 2);
			Map<Integer, String> taxonomySAs = ExcelUtils.extractRowAndThisColumn(uu, 0, 0);
			List<TrainingDataFormat> tds=Aligner.getTrainingDataFromGoogle(new File(uu), 0, 2, 3, 11, 1, -1,taxonomySAs);
			PerformanceResult p=new PerformanceResult();
			for(TrainingDataFormat td:tds) {
				List<FSTNLUOutput> rawr = ((SPSFSTNLU)nlu1).getRawNLUOutput(td.getUtterance(), null, 1);
				List<NLUOutput> r = nlu1.getNLUOutput(td.getUtterance(), null, 1);
				TrainingDataFormat spsTD=new TrainingDataFormat(td.getUtterance(), SAMapper.convertSA(td.getId()));
				boolean rr=nlu1.nluTest(spsTD,r);
				p.add(rr);
				if (!rr && printErrors) {
					logger.error("'"+td.getUtterance()+"' classified as: "+r+" instead of "+spsTD.getLabel()+"\n"+
							"   original label: "+td.getLabel()+" returned label: "+rawr
							);
				}
			}
			//PerformanceResult p = nlu1.test(tds, null, true);
			System.out.println("stage2: "+p);
			break;
		case 1:
			nlu1 = inlu.get("stage1");
			uu=nlu1.getConfiguration().getUserUtterances();
			System.out.println("stage 1: "+uu);
			tds = BuildTrainingData.extractTrainingDataFromExcel(new File(uu), 0, 4, 5);
			p = nlu1.test(tds, null, false);
			System.out.println("stage 1: "+p);
			break;
		}
	}
	
	/*
	 * ERROR 15:29:05.780 [main           ] [NLU                      ] 'can you describe your illness?' classified as: [<18.825188 null>] instead of question.ros.general-health-constitutional.illness.illness-general
ERROR 15:29:05.846 [main           ] [NLU                      ] 'are you feeling sick?' classified as: [<6.645343 null>] instead of question.ros.general-health-constitutional.illness.illness-general
ERROR 15:29:05.975 [main           ] [NLU                      ] 'tell me about your illness?' classified as: [<12.733336 null>] instead of question.ros.general-health-constitutional.illness.illness-general
ERROR 15:29:06.038 [main           ] [NLU                      ] 'are you feeling sick?' classified as: [<6.645343 null>] instead of question.ros.general-health-constitutional.illness.illness-general
ERROR 15:29:06.167 [main           ] [NLU                      ] 'are you feeling ill?' classified as: [<12.587631 null>] instead of question.ros.general-health-constitutional.illness.illness-general
ERROR 15:29:06.305 [main           ] [NLU                      ] 'are you feeling sick?' classified as: [<6.645343 null>] instead of question.ros.general-health-constitutional.illness.illness-general
ERROR 15:29:06.432 [main           ] [NLU                      ] 'What is your state of health?' classified as: [<39.041733 null>] instead of question.ros.general-health-constitutional.illness.illness-general
ERROR 15:29:06.682 [main           ] [NLU                      ] 'how are you feeling?' classified as: [<7.186982 null>] instead of question.ros.general-health-constitutional.illness.illness-general
ERROR 15:29:06.808 [main           ] [NLU                      ] 'are you feeling any chills?' classified as: [<21.148832 null>] instead of question.ros.general-health-constitutional.illness.chills
ERROR 15:29:06.870 [main           ] [NLU                      ] 'do you have chills?' classified as: [<11.796392 null>] instead of question.ros.general-health-constitutional.illness.chills
ERROR 15:29:06.934 [main           ] [NLU                      ] 'are you sweating?' classified as: [<1.0 null>] instead of question.ros.general-health-constitutional.illness.sweating
ERROR 15:29:06.998 [main           ] [NLU                      ] 'are you sweating?' classified as: [<1.0 null>] instead of question.ros.general-health-constitutional.illness.sweating
ERROR 15:29:07.248 [main           ] [NLU                      ] 'do you feel exhausted?' classified as: [<8.339983 null>] instead of question.ros.general-health-constitutional.illness.fatigue
ERROR 15:29:07.378 [main           ] [NLU                      ] 'are you feeling weak?' classified as: [<8.5458765 null>] instead of question.ros.general-health-constitutional.illness.weakness
ERROR 15:29:07.441 [main           ] [NLU                      ] 'do you have weakness?' classified as: [<10.294602 question.ros.musculoskeletal.muscles-joints.weakness>] instead of question.ros.general-health-constitutional.illness.weakness
ERROR 15:29:07.512 [main           ] [NLU                      ] 'are you feeling weak?' classified as: [<8.5458765 null>] instead of question.ros.general-health-constitutional.illness.weakness
ERROR 15:29:07.574 [main           ] [NLU                      ] 'do you have night sweats?' classified as: [<12.677508 null>] instead of question.ros.general-health-constitutional.illness.night-sweats

	 */
	public static void spsStage2RunOn(String text) throws Exception {
		NLUConfig config=getNLUConfig("spsFST");
		NLBusConfig tmp = NLBusConfig.WIN_EXE_CONFIG;
		tmp.setCharacter("Base-All");
		tmp.setNluConfig(config);
		NLU nlu=init(config);

		System.out.println(nlu.getNLUOutput(text, null, 1));
	}
	public static void spsStage2RawRunOn(String text) throws Exception {
		NLUConfig config=getNLUConfig("spsFST");
		NLBusConfig tmp = NLBusConfig.WIN_EXE_CONFIG;
		tmp.setCharacter("Base-All");
		tmp.setNluConfig(config);
		NLU nlu=init(config);

		System.out.println(((SPSFSTNLU)nlu).getRawNLUOutput(text, null, 1));
	}

	public static void spsRetrain() throws Exception {
		NLUConfig config=getNLUConfig("spsNLU");
		NLBusConfig tmp = NLBusConfig.WIN_EXE_CONFIG;
		tmp.setCharacter("Base-All");
		tmp.setNluConfig(config);
		NLU nlu=init(config);
		nlu.retrain();
	}
	public static void spsStage1Retrain() throws Exception {
		NLUConfig config=getNLUConfig("spsStage1");
		NLBusConfig tmp = NLBusConfig.WIN_EXE_CONFIG;
		tmp.setCharacter("Base-All");
		tmp.setNluConfig(config);
		NLU nlu=init(config);
		nlu.retrain();
	}
	
	public static void buildROSOnlyTrainingData(File out,boolean changeRos) throws Exception {
		List<TrainingDataFormat> itds = Aligner.extractTrainingDataFromSingleStep1and3GoogleXLSXForSPS(ros);
		for(TrainingDataFormat td:itds) {
			td.setLabel(SAMapper.convertSA(td.getId()));
		}
		BuildTrainingData.dumpTrainingDataToExcel(itds, out, "test",new String[]{},4,5,false);
	}
	public static void buildTrainingData(File out,boolean changeRos) throws Exception {
		List<TrainingDataFormat> hpitds=BuildTrainingData.extractTrainingDataFromExcel(hpi, 0, 4, 5);
		
		List<TrainingDataFormat> rostds=BuildTrainingData.extractTrainingDataFromExcel(ros, 0, 4, 5);
		List<TrainingDataFormat> mhtds=BuildTrainingData.extractTrainingDataFromExcel(mh, 0, 4, 5);
		List<TrainingDataFormat> all=new ArrayList<TrainingDataFormat>();
		all.addAll(hpitds);
		all.addAll(mhtds);
		if (changeRos) {
			for(TrainingDataFormat td:rostds) {
				td.setLabel("question.secondstage");
				all.add(td);
			}
		} else all.addAll(rostds);
		BuildTrainingData.dumpTrainingDataToExcel(all, out, "test",new String[]{},4,5,false);
	}
	
	private static void doSpecialWorkshopTestStage2() throws Exception {
		List<TrainingDataFormat> test = BuildTrainingData.extractTrainingDataFromExcel(testFile, 0, 0, 1, true);
		Iterator<TrainingDataFormat>it=test.iterator();
		while(it.hasNext()) {
			TrainingDataFormat td=it.next();
			if (StringUtils.isEmptyString(td.getLabel())) it.remove();
			else td.setLabel(SAMapper.convertSA(td.getLabel()));
		}
		System.out.println(test.size());
		NLU nlu=createSPSStage2NLU();
		PerformanceResult p = nlu.test(test, null, true);
		System.out.println(p);
	}

	public static NLU createSPSStage2NLU() throws Exception {
		NLUConfig config=getNLUConfig("spsFST");
		NLBusConfig tmp = NLBusConfig.WIN_EXE_CONFIG;
		tmp.setCharacter("Base-All");
		tmp.setNluConfig(config);
		NLU nlu=init(config);
		return nlu;
	}
	
	public static void doWorkshopComparison() throws Exception {
		NLU nluFst=createSPSStage2NLU();
		NLU nluHier=SPSstage1OnlyTest.createNLU();
		List<TrainingDataFormat> test = BuildTrainingData.extractTrainingDataFromExcel(testFile, 0, 0, 1, true);
		Iterator<TrainingDataFormat>it=test.iterator();
		while(it.hasNext()) {
			TrainingDataFormat td=it.next();
			if (StringUtils.isEmptyString(td.getLabel())) it.remove();
			else td.setLabel(SAMapper.convertSA(td.getLabel()));
		}
		PerformanceResult pfst=new PerformanceResult(),phier=new PerformanceResult();
		List<NLUOutput> rfst,rhier;
		for(TrainingDataFormat td:test) {
			rfst=nluFst.getNLUOutput(td.getUtterance(), null, null);
			rhier=nluHier.getNLUOutput(td.getUtterance(), null, null);
			boolean fstright=nluFst.nluTest(td, rfst);
			boolean hierright=nluHier.nluTest(td, rhier);
			pfst.add(fstright);
			phier.add(hierright);
			if (hierright && !fstright) {
				System.out.println(td.getLabel());
				System.out.println(" "+rfst);
			}
		}
		System.out.println(pfst);
		System.out.println(phier);
	}
	public static void doWorkshopComparison2() throws Exception {
		NLUConfig config=getNLUConfig("spsFST");
		config.setNluDir("nlufst-workshop");
		NLBusConfig tmp = NLBusConfig.WIN_EXE_CONFIG;
		tmp.setCharacter("Base-All");
		tmp.setNluConfig(config);
		NLU nlu=init(config);

		NLUConfig config2=getNLUConfig("spsFSTbackup");
		config2.setNluDir("nlufstbackup-workshop");
		NLBusConfig tmp2 = NLBusConfig.WIN_EXE_CONFIG;
		tmp2.setCharacter("Base-All");
		tmp2.setNluConfig(config2);
		NLU nlu2=init(config2);
		
		File model=new File(config.getNluModelFile());
		File model2=new File(config2.getNluModelFile());
		
		File u=new File(config.getUserUtterances()); // this file contains both the new nlu annotation and the ontology single label.
		logger.info("preparing training data for aligner");
		List<TrainingDataFormat> itds = Aligner.extractTrainingDataFromSingleStep1and3GoogleXLSXForSPS(u);
		Set<String> sas = BuildTrainingData.getAllSpeechActsInTrainingData(itds);
		System.out.println(itds.size()+" "+sas.size());
		DynamicFoldsData folds = BuildTrainingData.produceDynamicFolds(itds, 2);
		PerformanceResult all=new PerformanceResult(),all2=new PerformanceResult();
		for(int f=0;f<folds.getNumberOfFolds();f++) {
			System.out.println("fold: "+f+"/"+folds.getNumberOfFolds());
			List<TrainingDataFormat> test = folds.buildTestingDataForFold(f);
			List<TrainingDataFormat> training = folds.buildTrainingDataForFold(f);
			List<TrainingDataFormat> test2=copyTrainingData(test);
			List<TrainingDataFormat> training2=copyTrainingData(training);
			for(TrainingDataFormat td:test2) td.setLabel(SAMapper.convertSA(td.getId()));
			for(TrainingDataFormat td:training2) td.setLabel(SAMapper.convertSA(td.getId()));
			nlu.train(training,model);
			nlu.loadModel(model);
			nlu2.train(training2,model2);
			nlu2.loadModel(model2);
			PerformanceResult p=nlu.test(test2, null, true);
			PerformanceResult p2=nlu2.test(test2, null, false);
			System.out.println(p);
			System.out.println(p2);
			all.add(p);
			all2.add(p2);
		}
		System.out.println(all);
		System.out.println(all2);
	}

	private static List<TrainingDataFormat> copyTrainingData(List<TrainingDataFormat> input) {
		List<TrainingDataFormat> output=new ArrayList<TrainingDataFormat>();
		for(TrainingDataFormat td:input) {
			output.add(new TrainingDataFormat(td));
		}
		return output;
	}
	
	public static void testMapper(File model, File... step1andStep3s) throws Exception {
		List<TrainingDataFormat> tds=null;
		for(File step1andStep3:step1andStep3s) {
			Map<Integer, String> taxonomySAs = ExcelUtils.extractRowAndThisColumn(step1andStep3.getAbsolutePath(), 0, 0);
			List<TrainingDataFormat> tmp=Aligner.getTrainingDataFromGoogle(step1andStep3, 0, 2, 3, -1, 1, -1,taxonomySAs);
			if (tmp!=null) {
				if (tds==null) tds=new ArrayList<TrainingDataFormat>();
				tds.addAll(tmp);
			}
		}
		//DynamicFoldsData folds = BuildTrainingData.produceDynamicFolds(tds, 10);
		FoldsData folds = BuildTrainingData.produceFolds(tds, 10);
		SAMapper mapper = new SAMapper(NLUConfig.WIN_EXE_CONFIG);
		
		Map<String,PerformanceResult> c=new HashMap<String, PerformanceResult>();
		for(int i=0;i<folds.getNumberOfFolds();i++) {
			List<TrainingDataFormat> fdts = folds.buildTrainingDataForFold(i);
			List<TrainingDataFormat> ftestd = folds.buildTestingDataForFold(i);
			mapper.trainMapperAndSave(model,null, fdts);
			mapper.loadMapperModel(model);
			Map<String, PerformanceResult> r=mapper.test(ftestd,false);
			mergeHash(c,r);
			System.out.println("fold "+i+": "+r);
		}
		System.out.println(c);
	}

	private static void mergeHash(Map<String, PerformanceResult> into,Map<String, PerformanceResult> result) {
		if (into!=null && result!=null) {
			for(String k:result.keySet()) {
				PerformanceResult x=into.get(k);
				if (x==null) into.put(k, x=new PerformanceResult());
				x.add(result.get(k));
			}
		}
	}

	public static void runNLUvsGeneralizedPerformanceTest() throws Exception {
		NLUConfig config=getNLUConfig("spsFSTbackup");
		//NLUConfig config=getNLUConfig("FSTNLU");
		//NLUConfig config=getNLUConfig("spsFST");
		NLBusConfig tmp = NLBusConfig.WIN_EXE_CONFIG;
		tmp.setCharacter("Base-All");
		tmp.setNluConfig(config);
		NLU nlu=init(config);
		
		nlu.retrain(ros1,ros2,ros3,ros5,ros6,ros7);
		nlu.loadModel(new File(nlu.getConfiguration().getNluModelFile()));
		
		
		List<TrainingDataFormat> test = Aligner.extractTrainingDataFromSingleStep1and3GoogleXLSXForSPS(ros9);
		for(TrainingDataFormat td:test) td.setLabel(SAMapper.convertSA(td.getId()));
		PerformanceResult p = nlu.test(test, null, false);
		System.out.println(p);
		
		config=getNLUConfig("spsGeneralizedFST");
		tmp.setNluConfig(config);
		nlu=init(config);

		nlu.retrain(ros1,ros2,ros3,ros5,ros6,ros7);
		nlu.loadModel(new File(nlu.getConfiguration().getNluModelFile()));

		PerformanceResult p2 = nlu.test(test, null, false);
		System.out.println(p2);
	}
	
	public static void main(String[] args) throws Exception {
		runNLUvsGeneralizedPerformanceTest();
		System.exit(1);
		testMapper(new File("mapperModelForTest"), ros1,ros2,ros3,ros5,ros6,ros7,ros9);
		System.exit(1);
		//NLUConfig config=getNLUConfig("spsFSTbackup");
		NLUConfig config=getNLUConfig("FSTNLU");
		//NLUConfig config=getNLUConfig("spsFST");
		NLBusConfig tmp = NLBusConfig.WIN_EXE_CONFIG;
		tmp.setCharacter("Base-All");
		tmp.setNluConfig(config);
		NLU nlu=init(config);
		
		//nlu.retrain(ros1,ros2,ros3,ros5,ros6,ros7);
		//nlu.loadModel(new File(nlu.getConfiguration().getNluModelFile()));
		
		/*
		List<TrainingDataFormat> test = Aligner.extractTrainingDataFromSingleStep1and3GoogleXLSXForSPS(ros9);
		for(TrainingDataFormat td:test) td.setLabel(SAMapper.convertSA(td.getId()));
		PerformanceResult p = nlu.test(test, null, true);
		System.out.println(p);
		*/

		
		String text="do you have any problems with breathing";
		text=Aligner.removeSpeechStuff(text);
		TokenizerI tokenizer=nlu.getConfiguration().getNluTokenizer();
		text=tokenizer.untokenize(tokenizer.tokenize1(text));

		//List<NLUOutput> r = nlu.getNLUOutput(text, null, null);
		List<FSTNLUOutput> nlus = ((SPSFSTNLU)nlu).getRawNLUOutput(text, null, 100);
		TraverseFST tf=((SPSFSTNLU)nlu).getFSTTraverser();
		System.out.println(tf.generateInputTextForFST(text, tf.getInputSymbols()));
		System.out.println(FunctionalLibrary.printCollection(nlus, " ", "", "\n "));
		AlignmentSummary x = ((SPSFSTNLU)nlu).readAlignerInfo();
		System.out.println(FunctionalLibrary.printCollection(x.getNLUConceptsTriggeredByWord("problem")," ","","\n "));
		//System.out.println(FunctionalLibrary.printCollection(x.getNLUConceptsTriggeredByWord("breathing")," ","","\n "));
		SAMapper saMapper = ((SPSFSTNLU)nlu).getMapper();
		Set<Pair<String,Integer>> possibleInputs=saMapper.getFSTNLUOutputsForSimcoachSA("question.ros.question.ros.chest.respiratory.breathing-general");
		System.out.println(FunctionalLibrary.printCollection(possibleInputs, "  ", "", "\n  "));

		//System.out.println(r);
		
		System.exit(1);

		
		//doWorkshopComparison2();
		doWorkshopComparison();
		//buildROSOnlyTrainingData(new File(hpi.getParentFile(),"output.xlsx"), false);
		//doSpecialWorkshopTestStage2();
		System.exit(1);
		
		//buildTrainingData(new File(hpi.getParentFile(),"output.xlsx"),false);
		//buildTrainingData(new File(hpi.getParentFile(),"stage1_new_withmh.xlsx"),true);
		//spsStage2TestRaw("Do you have excessive tears?");
		//spsRetrain();
		//spsStage1Retrain();
		//spsTest(1,true);
		//TraverseFST tfst = new TraverseFST();
		//String fst = tfst.generateFSTforUtterance("are you feeling any chills?");
		//FileUtils.dumpToFile(fst, "utterance.fsa");
		System.exit(1);
		
		/*
		NLUConfig config=getNLUConfig("FSTNLU");
		NLBusConfig tmp = NLBusConfig.WIN_EXE_CONFIG;
		tmp.setDefaultCharacter("Base-All");
		tmp.setNluConfig(config);
		NLU nlu=init(config);
		NLUConfig c = nlu.getConfiguration();
		File u=new File(c.getUserUtterances()); // this file contains both the new nlu annotation and the ontology single label.
		Aligner a=new Aligner();
		logger.info("preparing training data for aligner");
		List<TrainingDataFormat> tds = a.extractTrainingDataFromSingleStep1and3GoogleXLSXForSPS(u);
		produceAcceptorForPathContainingOneOf("what", tds);
		produceAcceptorForPathContainingOneOf("type", tds);
		System.exit(1);
		*/
		
		// String text="are you feeling sick?";
		//runFSTNLUTest("Base-All", text);
		//spsStage2Test(text);
	}
}
