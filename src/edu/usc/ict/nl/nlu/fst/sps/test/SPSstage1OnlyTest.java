package edu.usc.ict.nl.nlu.fst.sps.test;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;

import edu.usc.ict.nl.bus.NLBus;
import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.fst.sps.SAMapper;
import edu.usc.ict.nl.nlu.io.BuildTrainingData;
import edu.usc.ict.nl.util.PerformanceResult;
import edu.usc.ict.nl.util.StringUtils;


public class SPSstage1OnlyTest {
	private static final File testFile=new File("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\annotation\\output.xlsx");
	private static final File nluroot=new File("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\resources\\characters\\Base-All\\nlu\\");
	public static NLU createNLU() throws SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, CloneNotSupportedException {
		NLUConfig config=(NLUConfig) NLUConfig.WIN_EXE_CONFIG.clone();
		config.setForcedNLUContentRoot(nluroot.getAbsolutePath());
		config.setUserUtterances("output.xlsx");

		//config.setInternalNluClass4Hier("edu.usc.ict.nl.nlu.jmxnlu.JMXClassifierNLU");
		//config.setNluClass("edu.usc.ict.nl.nlu.jmxnlu.JMXClassifierNLU");
		//config.setNluFeaturesBuilderClass("edu.usc.ict.nl.nlu.features.FeatureBuilderForJMXClassifier");
		//config.setAcceptanceThreshold(null);
		config.setInternalNluClass4Hier("edu.usc.ict.nl.nlu.mxnlu.MXClassifierNLU");
		//config.setInternalNluClass4Hier("edu.usc.ict.nl.nlu.mallet.MalletMaxEntClassifierNLU");
		config.setNluClass("edu.usc.ict.nl.nlu.hierarchical.HierarchicalNLU");
		//config.setNluClass("edu.usc.ict.nl.nlu.mxnlu.MXClassifierNLU");
		//config.setNluClass("edu.usc.ict.nl.nlu.mallet.MalletMaxEntClassifierNLU");
		//config.setNluClass("edu.usc.ict.nl.nlu.jmxnlu.JMXClassifierNLU");
		config.setNluFeaturesBuilderClass("edu.usc.ict.nl.nlu.features.FeaturesBuilderForMXClassifier");
		//config.setNluFeaturesBuilderClass("edu.usc.ict.nl.nlu.features.FeatureBuilderForJMXClassifier");
		config.setAcceptanceThreshold(0.3f);
		//config.setNluClass("edu.usc.ict.nl.nlu.mallet.MalletMaxEntClassifierNLU");

		config.setHierNluReturnsNonLeaves(false);
		config.setHierarchicalNluSeparator(".");
		config.setnBest(3);
		NLU nlu=(NLU) NLBus.createSubcomponent(config, config.getNluClass());
		return nlu;
	}
	private static NLU train() throws Exception {
		NLU nlu = createNLU();
		NLUConfig config = nlu.getConfiguration();
		List<TrainingDataFormat> tds = BuildTrainingData.extractTrainingDataFromExcel(new File(config.getUserUtterances()), 0, 4, 5);
		nlu.train(tds, new File(config.getNluModelFile()));
		nlu.kill();
		nlu.loadModel(new File(config.getNluModelFile()));
		PerformanceResult p1 = nlu.test(tds, new File(config.getNluModelFile()), false);
		System.out.println(p1);
		return nlu;
	}
	public static void main(String[] args) throws Exception {
		List<TrainingDataFormat> test = BuildTrainingData.extractTrainingDataFromExcel(testFile, 0, 0, 1, true);
		Iterator<TrainingDataFormat>it=test.iterator();
		while(it.hasNext()) {
			TrainingDataFormat td=it.next();
			if (StringUtils.isEmptyString(td.getLabel())) it.remove();
			else td.setLabel(SAMapper.convertSA(td.getLabel()));
		}
		System.out.println(test.size());
		//train();
		NLU nlu = createNLU();
		PerformanceResult p = nlu.test(test, null, true);
		System.out.println(p);
		PerformanceResult phier=new PerformanceResult();
		List<NLUOutput> rhier;
		for(TrainingDataFormat td:test) {
			//td.setUtterance("and are you having any pain in your stomach");
			rhier=nlu.getNLUOutput(td.getUtterance(), null, null);
			boolean hierright=nlu.nluTest(td, rhier);
			phier.add(hierright);
		}
		System.out.println(phier);

	}
}
