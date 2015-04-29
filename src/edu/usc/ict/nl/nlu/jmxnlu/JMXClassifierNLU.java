package edu.usc.ict.nl.nlu.jmxnlu;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import edu.usc.ict.dialogue.McNLU;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.Model;
import edu.usc.ict.nl.nlu.NLUProcess;
import edu.usc.ict.nl.nlu.mxnlu.MXClassifierNLU;
import edu.usc.ict.nl.nlu.mxnlu.MXClassifierProcess;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nlp.ml.Multitron;
import edu.usc.ict.nlp.ml.Multitron.Bintron;

public class JMXClassifierNLU extends MXClassifierNLU {

	public JMXClassifierNLU() throws Exception {
		super();
	}
	
	public JMXClassifierNLU(NLUConfig c) throws Exception {
		super(c);
	}

	@Override
	public HashSet<String> getAllSimplifiedPossibleOutputs() throws Exception {
		MXClassifierProcess p = (MXClassifierProcess) getNLUProcess();
		Set<String> nlus=p.getAllSimplifiedPossibleOutputs();
		if (nlus!=null) {
			HashSet<String> out=new HashSet<String>();
			for(String cl:nlus) {
				out.add(StringUtils.removeLeadingAndTrailingSpaces(cl));
			}
			return out;
		} else return null;
	}
	
	@Override
	public NLUProcess startMXNLUProcessWithTheseParams(String model,
			int nbest) throws Exception {
		NLUProcess p = new JMXClassifierProcess(null);
		p.run(model, nbest);
		return p;
	}

	public Model readModelFileNoCache(File mf) throws Exception {
		NLUProcess nluP=getNLUProcess();
		McNLU classifier=((JMXClassifierProcess)nluP).getClassifier();
		String oldModel=classifier.getModelName();
		classifier.loadModel(mf.getAbsolutePath());
		Multitron imodel=classifier.getModel();
		Model ret=new Model();
		Vector<Bintron> labels = imodel.getClassesObjects();
		Vector<String> features = imodel.getFeatures();
		for(Bintron l:labels) {
			Vector<Float> weights = l.getPerceptron().getWeights();
			for(int i=0;i<weights.size();i++) {
				ret.addFeatureWeightForSA(features.get(i), l.getName(), ret.new FeatureWeight(features.get(i),weights.get(i)));
			}
		}
		classifier.loadModel(oldModel);
		return ret;
	}
}
