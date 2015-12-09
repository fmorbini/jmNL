package edu.usc.ict.nl.nlu.mallet;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntL1Trainer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSelection;
import cc.mallet.types.InstanceList;
import cc.mallet.types.Label;
import cc.mallet.types.LabelAlphabet;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.DynamicFoldsData;
import edu.usc.ict.nl.nlu.Model;
import edu.usc.ict.nl.nlu.NLUProcess;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.io.BuildTrainingData;
import edu.usc.ict.nl.nlu.jmxnlu.JMXClassifierNLU;
import edu.usc.ict.nl.util.PerformanceResult;

public class MalletMaxEntClassifierNLU extends JMXClassifierNLU {

	public MalletMaxEntClassifierNLU(NLUConfig config) throws Exception {
		super(config);
	}

	@Override
	public NLUProcess startMXNLUProcessWithTheseParams(String model,
			int nbest) throws Exception {
		NLUProcess p = new MalletMaxEntClassifierProcess(null);
		p.run(model, nbest);
		return p;
	}
	
	@Override
	public Model readModelFileNoCache(File mf) throws Exception {
		Model ret=null;

		MalletMaxEntClassifierProcess tmp = new MalletMaxEntClassifierProcess(null);
		MaxEnt classifier=tmp.loadClassifier(mf);


		int defaultFeatureIndex=classifier.getDefaultFeatureIndex();
		int numFeatures = defaultFeatureIndex + 1;

		LabelAlphabet labelAlphabet = classifier.getLabelAlphabet();
		int numLabels = labelAlphabet.size();
		double[] parameters = classifier.getParameters();
		FeatureSelection[] perClassFeatureSelection = classifier.getPerClassFeatureSelection();
		double[] scores=new double[numLabels]; 
		assert (scores.length == numLabels);
		for (int li = 0; li < numLabels; li++) {
			Label label=labelAlphabet.lookupLabel(li);
			FeatureSelection featureSelectionForThisLabel = (perClassFeatureSelection == null ? classifier.getFeatureSelection():perClassFeatureSelection[li]);
			Alphabet featureAlphabet = featureSelectionForThisLabel.getAlphabet();
			Iterator<String> featuresIt=featureAlphabet.iterator();
			while(featuresIt.hasNext()) {
				String feature=featuresIt.next();
				int ri=featureAlphabet.lookupIndex(feature);
				if (ret==null) ret=new Model();
				ret.addFeatureWeightForSA(feature, label.toString(), ret.new FeatureWeight(feature,(float)parameters[li*numFeatures + defaultFeatureIndex], (float)parameters[li*numFeatures+ri]));
			}
		}
		return ret;
	}
	
	
	@Override
	public void trainNLUOnThisData(List<TrainingDataFormat> td,File trainingFile, File modelFile) throws Exception {
		MalletMaxEntClassifierProcess process = (MalletMaxEntClassifierProcess) getNLUProcess();

		dumpTrainingDataToFileNLUFormat(trainingFile,td);
		
		List<TrainingDataFormat> tds = BuildTrainingData.buildTrainingDataFromNLUFormatFile(trainingFile);
		MaxEnt classifier=null;

		Float reguralizationParameter=getConfiguration().getRegularization();
		if (reguralizationParameter==null) {

			DynamicFoldsData dfs = BuildTrainingData.produceDynamicFolds(tds, 5);
			List<TrainingDataFormat> list=null;
			float bestAccuracy=0f;
			double bestParameter=0,parameter=0.01;
			for(int iteration=1;iteration<=20;iteration++) {
				PerformanceResult t=new PerformanceResult();
				for(int i=0;i<dfs.getNumberOfFolds();i++) {
					list = dfs.buildTrainingDataForFold(i,list);
					InstanceList trainingInstances = process.readNLUTrainingData(list);
					ClassifierTrainer<MaxEnt> trainer = new MaxEntL1Trainer(parameter);
					classifier=process.train(trainingInstances, trainer);
					process.saveClassifier(classifier, modelFile);
					list = dfs.buildTestingDataForFold(i,list);
					PerformanceResult p=testNLUOnThisData(list, modelFile, false);
					t.add(p);
				}
				getLogger().warn("parameter search: "+parameter+" obtained accuracy of: "+t.getPrecision());
				if (t.getPrecision()>bestAccuracy) {
					bestAccuracy=t.getPrecision();
					bestParameter=parameter;
				}
				parameter*=2;
			}
			getLogger().warn("best parameter: "+bestParameter);
			reguralizationParameter=(float) bestParameter;
		}

		InstanceList trainingInstances = process.readNLUTrainingData(tds);
        ClassifierTrainer<MaxEnt> trainer = new MaxEntL1Trainer(reguralizationParameter);
		classifier=process.train(trainingInstances, trainer);
        process.saveClassifier(classifier, modelFile);
	}
	
}
