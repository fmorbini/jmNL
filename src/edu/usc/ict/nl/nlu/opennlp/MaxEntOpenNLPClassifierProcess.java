package edu.usc.ict.nl.nlu.opennlp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import edu.usc.ict.nl.nlu.mxnlu.MXClassifierProcess;
import edu.usc.ict.nl.util.StringUtils;
import opennlp.maxent.BasicEventStream;
import opennlp.maxent.DoubleStringPair;
import opennlp.maxent.GIS;
import opennlp.maxent.ModelTrainer;
import opennlp.maxent.io.SuffixSensitiveGISModelWriter;
import opennlp.model.AbstractModel;
import opennlp.model.AbstractModelWriter;
import opennlp.model.EventStream;
import opennlp.model.GenericModelReader;
import opennlp.model.MaxentModel;
import opennlp.model.OnePassDataIndexer;

public class MaxEntOpenNLPClassifierProcess extends MXClassifierProcess {

	private MaxentModel classifier;
    int maxit = 100;
    int cutoff = 1;

	public MaxEntOpenNLPClassifierProcess(String exe) {
		super(null, null);
	}

	public MaxentModel getClassifier() {
		return classifier;
	}
	public void setClassifier(MaxentModel classifier) {
		this.classifier = classifier;
	}
	
	@Override
	public void run(String model, int nb) throws Exception {
		nBest=nb;
		if (model != null)
			classifier=loadClassifier(new File(model));
	}

	public MaxentModel loadClassifier(File serializedFile) {
		MaxentModel classifier=null;

		try {
			classifier = (MaxentModel)new GenericModelReader(serializedFile).getModel();
		} catch (FileNotFoundException e) {
			System.err.println("Classifier model file does not exist: "+serializedFile);
			return null;
		} catch (Exception e) {
			System.err.println("Error loading model: "+serializedFile);
			e.printStackTrace();
		}

		return classifier;
	}

	public void saveClassifier(AbstractModel classifier, File serializedFile) throws IOException {
		AbstractModelWriter writer = new SuffixSensitiveGISModelWriter(classifier, serializedFile);
		writer.persist();
	}

	public EventStream readNLUTrainingDataFile(File nluDataFile) throws Exception {
        BasicEventStream es = new BasicEventStream(new NLUTrainingFileReader(nluDataFile), " ");
		return es;
	}
	
	@Override
	public void train(String model, String trainingFile) throws Exception {
		EventStream es = readNLUTrainingDataFile(new File(trainingFile));
		classifier = GIS.trainModel(maxit,
				 new OnePassDataIndexer(es, cutoff),
				 ModelTrainer.USE_SMOOTHING);
        //QNTrainer tr = new QNTrainer();
        //classifier=tr.trainModel(new OnePassDataIndexer(es, cutoff));
        saveClassifier((AbstractModel)classifier, new File(model));
	}
	
	@Override
	public void kill() {}
	
	@Override
	public String[] classify(String u,int nBest) throws Exception {
		MaxentModel classifier=getClassifier();
		double[] ocs = classifier.eval(u.split(" "));
		int numOutcomes = ocs.length;
	    DoubleStringPair[] classifierOutput = new DoubleStringPair[numOutcomes];
	    for (int i=0; i<numOutcomes; i++)
	      classifierOutput[i] = new DoubleStringPair(-ocs[i], classifier.getOutcome(i));

	    java.util.Arrays.sort(classifierOutput);
		
		int s=Math.min(nBest, classifierOutput.length);
		String[] results=new String[s];
		for (int rank = 0; rank < s; rank++) {
			results[rank]=-classifierOutput[rank].doubleValue+" "+StringUtils.removeLeadingAndTrailingSpaces(classifierOutput[rank].stringValue);
		}
		return results;
	}

	public Set<String> getAllSimplifiedPossibleOutputs() {
		Set<String> ret=null;
		if (classifier!=null) {
			int size=classifier.getNumOutcomes();
			for(int i=0;i<size;i++) {
				if (ret==null) ret=new HashSet<String>();
				ret.add(classifier.getOutcome(i));
			}
		}
		return ret;
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
			MaxEntOpenNLPClassifierProcess p = new MaxEntOpenNLPClassifierProcess(null);
			p.train("test", "resources/characters/Test/nlu/classifier-training.txt");
			p.run("test", 2);
			String[] r = p.classify("yes");
			System.out.println(Arrays.asList(r));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
