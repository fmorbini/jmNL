package edu.usc.ict.nl.nlu.svm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import edu.usc.ict.nl.nlu.mxnlu.MXClassifierProcess;
import edu.usc.ict.nl.util.StringUtils;

public class SvmClassifierProcess extends MXClassifierProcess {


	private SVMModelAndDictionaries classifier=null;
	private LibSVMNLU nlu=null;

	public SvmClassifierProcess(LibSVMNLU nlu) {
		super(null, null);
		this.nlu=nlu;

	}

	public SVMModelAndDictionaries getClassifier() {
		return classifier;
	}
	public void setClassifier(SVMModelAndDictionaries classifier) {
		this.classifier = classifier;
	}
	
	@Override
	public void run(String model, int nb) throws Exception {
		nBest=nb;
		if (model != null) classifier=new SVMModelAndDictionaries(nlu,new File(model));
	}
	
	@Override
	public void train(String modelFile, String trainingFile) throws Exception {
		SVMModelAndDictionaries newclassifier=SVMModelAndDictionaries.train(nlu,trainingFile);
		newclassifier.save(modelFile);
	}
	

	
	private svm_node[] getSVMTestCaseFromString(String text) throws Exception {
		String featuresString = text;//nlu.doPreprocessingForClassify(text);
		String[] features=featuresString.split("[\\s]+");
		int m = features.length;
		List<svm_node> x = new ArrayList<svm_node>();
		for(int j=0;j<m;j++)
		{
			String f=features[j];
			Integer index=classifier.getIdOfFeature(f);
			if (index!=null) {
				svm_node node = new svm_node();
				node.index = index;
				node.value = 1;
				x.add(node);
			}
		}
		return x.toArray(new svm_node[x.size()]);
	}
	
	@Override
	public void kill() {
		classifier=null;
		nlu=null;
	}
	
	@Override
	public String[] classify(String u,int nBest) throws IOException, InterruptedException {
		svm_model svmm=classifier.getSVMClassifier();
		svm_node[] x=null;
		try {
			x = getSVMTestCaseFromString(u);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (x!=null) {
			int c =(int) svm.svm_predict(svmm,x);
			String outputClass=classifier.getOutputClassForId(c);
			System.out.println(outputClass);
			String[] results=new String[]{"1 "+StringUtils.removeLeadingAndTrailingSpaces(outputClass)};
			return results;
		}
		return null;
	}

	public Set<String> getAllSimplifiedPossibleOutputs() {
		return classifier.getAllSimplifiedPossibleOutputs();
	}
	

	public static void main(String[] args) {
		try {
			//McNLU.main(new String[]{"model.txt"});
		/*	NLConfig config=new NLConfig(NLConfig.WIN_EXE_CONFIG);
			config.setInternalNluClass4Hier("edu.usc.ict.nl.nlu.mxnlu.MXClassifierNLU");
			config.setDefaultCharacter("Ellie_DCAPS");
			config.setHierarchicalNluSeparator(".");
			config.setNluFeaturesBuilderClass("edu.usc.ict.nl.nlu.features.DoNothingFeatureBuilder");
			HierarchicalNLU nlu = new HierarchicalNLU(config);
			File tf=new File("resources/characters/Ellie_DCAPS/nlu/classifier-training.txt");
			BuildTrainingData btd = nlu.getBTD();
			List<TrainingDataFormat> data = btd.buildTrainingData();
			nlu.dumpTrainingDataToFileNLUFormat(tf, data);
*/
			SvmClassifierProcess p = new SvmClassifierProcess(null);
			p.train("test", "resources/characters/Test/nlu/classifier-training.txt");
			p.run("test", 2);
			String[] r = p.classify("yes");
			System.out.println(Arrays.asList(r));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
