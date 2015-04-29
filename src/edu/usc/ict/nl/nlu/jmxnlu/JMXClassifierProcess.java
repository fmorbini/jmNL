package edu.usc.ict.nl.nlu.jmxnlu;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import edu.usc.ict.dialogue.McNLU;
import edu.usc.ict.dialogue.ScoredFrame;
import edu.usc.ict.nl.nlu.mxnlu.MXClassifierProcess;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nlp.ml.Multitron;

public class JMXClassifierProcess extends MXClassifierProcess {

	private McNLU classifier;
	
	private void basicSetup() {
		McNLU classifier=getClassifier();
		classifier.setCompositionalMode(false);
		classifier.setUsePList(false);
		classifier.setSasoGarbageEnabled(false);
		classifier.setTacqUnkownEnabled(false);
		classifier.setEvalMode(false);
		classifier.setIsSasoMode(true);
		classifier.setNumHeldout(0);
		classifier.setTrainMode(false);
		classifier.setUsePList(false);
		classifier.setNumIterations(15);
		classifier.setVhMessagesEnabled(false);
	}

	public JMXClassifierProcess(String exe) {
		super(null, null);
		setClassifier(new McNLU());
		basicSetup();
	}

	public McNLU getClassifier() {
		return classifier;
	}
	public void setClassifier(McNLU classifier) {
		this.classifier = classifier;
	}
	
	@Override
	public void run(String model, int nb) throws IOException {
		nBest=nb;
		McNLU classifier=getClassifier();
		Multitron imodel=classifier.getModel();
		if (imodel!=null) imodel.reinitialize();
		classifier.setModelName(model);
		classifier.loadModel(model);
	}

	@Override
	public void train(String model, String trainingFile) throws Exception {
		McNLU classifier=getClassifier();
		classifier.setModelName(model);
		Multitron imodel=classifier.getModel();
		if (imodel!=null) imodel.reinitialize();
		classifier.trainFromFile(trainingFile, false);
		classifier.loadModel(model);
	}
	
	@Override
	public void kill() {}
	
	@Override
	public String[] classify(String u,int nBest) throws IOException, InterruptedException {
		McNLU classifier=getClassifier();
		ScoredFrame[] rawResults = classifier.classifyUtteranceNBest(u, 0, -1);
		if (rawResults!=null) {
			normalizeScores(rawResults);
			
			String[] results=new String[rawResults.length];
			for(int i=0;i<rawResults.length;i++) {
				results[i]=rawResults[i].getScore()+" "+StringUtils.removeLeadingAndTrailingSpaces(rawResults[i].getFrame());
			}
			return results;
		}
		return null;
	}
	
	private void normalizeScores(ScoredFrame[] rs) {
		if (rs!=null) {
			// normalize to 0-1
			Float max=null,min=null;
			for(int i=0;i<rs.length;i++) {
				ScoredFrame r=rs[i];
				float s=r.getScore(); 
				if (max==null || s>max) max=s;
				if (min==null || s<min) min=s;
			}
			float rf=1f/(float)(max-min);
			float sum=0;
			for(int i=0;i<rs.length;i++) {
				ScoredFrame r=rs[i];
				float s=(r.getScore()-min)*rf;
				r.setScore(s);
				sum+=s;
			}
			rf=1/sum;
			// normalize sum to 1.
			for(int i=0;i<rs.length;i++) {
				ScoredFrame r=rs[i];
				float s=r.getScore()*rf;
				r.setScore(s);
			}
		}
	}

	@Override
	public Set<String> getAllSimplifiedPossibleOutputs() {
		McNLU classifier=getClassifier();
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
			JMXClassifierProcess p = new JMXClassifierProcess(null);
			//p.train("test", "resources/characters/Ellie_DCAPS/nlu/classifier-training.txt");
			p.run("resources/characters/Ellie_DCAPS/nlu/classifier-model-hier-root", 2);
			String[] r = p.classify("yes");
			System.out.println(Arrays.asList(r));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
