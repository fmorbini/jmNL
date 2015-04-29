package edu.usc.ict.nl.nlu.mallet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import cc.mallet.classify.Classifier;
import cc.mallet.classify.ClassifierTrainer;
import cc.mallet.classify.MaxEnt;
import cc.mallet.classify.MaxEntL1Trainer;
import cc.mallet.pipe.CharSequence2TokenSequence;
import cc.mallet.pipe.FeatureSequence2FeatureVector;
import cc.mallet.pipe.Input2CharSequence;
import cc.mallet.pipe.Pipe;
import cc.mallet.pipe.SerialPipes;
import cc.mallet.pipe.Target2Label;
import cc.mallet.pipe.TokenSequence2FeatureSequence;
import cc.mallet.pipe.TokenSequenceNGrams;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import cc.mallet.types.LabelAlphabet;
import cc.mallet.types.Labeling;
import edu.usc.ict.nl.nlu.BuildTrainingData;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.mxnlu.MXClassifierProcess;
import edu.usc.ict.nl.util.StringUtils;

public class MalletMaxEntClassifierProcess extends MXClassifierProcess {

	private MaxEnt classifier;
	
	private Pipe buildProcessingPipe() {
		ArrayList<Pipe> pipeList = new ArrayList<Pipe>();
        pipeList.add(new Input2CharSequence("UTF-8"));
        Pattern tokenPattern = Pattern.compile("[^\\s]+");
        pipeList.add(new CharSequence2TokenSequence(tokenPattern));
        //pipeList.add(new TEst(new int[]{1,2,3}));
        pipeList.add(new TokenSequenceNGrams(new int[]{1,2,3}));
        pipeList.add(new TokenSequence2FeatureSequence());
        pipeList.add(new FeatureSequence2FeatureVector());
        pipeList.add(new Target2Label());
        //pipeList.add(new PrintInputAndTarget());
        return new SerialPipes(pipeList);
	}
	
	public MalletMaxEntClassifierProcess(String exe) {
		super(null, null);
	}

	public MaxEnt getClassifier() {
		return classifier;
	}
	public void setClassifier(MaxEnt classifier) {
		this.classifier = classifier;
	}
	
	@Override
	public void run(String model, int nb) throws Exception {
		nBest=nb;
		if (model != null)
			classifier=loadClassifier(new File(model));
	}

	public MaxEnt loadClassifier(File serializedFile) {
		MaxEnt classifier=null;

		try {
			ObjectInputStream ois = new ObjectInputStream (new FileInputStream (serializedFile));
			classifier = (MaxEnt) ois.readObject();
			ois.close();
		} catch (FileNotFoundException e) {
			System.err.println("Classifier model file does not exist: "+serializedFile);
			return null;
		} catch (Exception e) {
			System.err.println("Error loading model: "+serializedFile);
			e.printStackTrace();
		}

		return classifier;
	}

	public void saveClassifier(Classifier classifier, File serializedFile) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream (serializedFile));
		oos.writeObject (classifier);
		oos.close();
	}

    
	public InstanceList readNLUTrainingDataFile(File nluDataFile) throws Exception {
		List<TrainingDataFormat> tds = BuildTrainingData.buildTrainingDataFromNLUFormatFile(nluDataFile);
		return readNLUTrainingData(tds);
	}
	public InstanceList readNLUTrainingData(List<TrainingDataFormat> tds) throws Exception {
        InstanceList instances = new InstanceList(buildProcessingPipe());
		int id=1;
		if (tds!=null) {
			for(TrainingDataFormat td:tds) {
				String trainingdata=td.getUtterance();
				Instance i=new Instance(trainingdata,td.getLabel(),"instance-"+(id++),null);
				instances.addThruPipe(i);
			}
		}
		return instances;
	}
	
	public MaxEnt train(String trainingFile,ClassifierTrainer<MaxEnt> trainer) throws Exception {
		InstanceList trainingInstances = readNLUTrainingDataFile(new File(trainingFile));
		return train(trainingInstances,trainer);
	}
	public MaxEnt train(InstanceList trainingInstances,ClassifierTrainer<MaxEnt> trainer) throws Exception {
        MaxEnt classifier=trainer.train(trainingInstances);
        return classifier;
	}
	@Override
	public void train(String model, String trainingFile) throws Exception {
        ClassifierTrainer<MaxEnt> trainer = new MaxEntL1Trainer(.05);
        classifier=train(trainingFile, trainer);
        saveClassifier(classifier, new File(model));
	}
	
	@Override
	public void kill() {}
	
	@Override
	public String[] classify(String u,int nBest) throws IOException, InterruptedException {
		Classifier classifier=getClassifier();
		Pipe pipe=classifier.getInstancePipe();
		Instance i=new Instance(u,null,"instance-test",null);
		i=pipe.instanceFrom(i);
		Labeling l=classifier.classify(i).getLabeling();
		
		
		int s=Math.min(nBest, l.numLocations());
		String[] results=new String[s];
		for (int rank = 0; rank < s; rank++) {
			l.getLabelAtRank(rank); 
			results[rank]=l.getValueAtRank(rank)+" "+StringUtils.removeLeadingAndTrailingSpaces(l.getLabelAtRank(rank).toString());
		}
		return results;
	}

	public Set<String> getAllSimplifiedPossibleOutputs() {
		LabelAlphabet labels = (classifier!=null)?classifier.getLabelAlphabet():null;
		Set<String> ret=null;
		if (labels!=null) {
			for(int i=0;i<labels.size();i++) {
				if (ret==null) ret=new HashSet<String>();
				ret.add(labels.lookupLabel(i).toString());
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
			MalletMaxEntClassifierProcess p = new MalletMaxEntClassifierProcess(null);
			p.train("test", "resources/characters/Test/nlu/classifier-training.txt");
			p.run("test", 2);
			String[] r = p.classify("yes");
			System.out.println(Arrays.asList(r));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
