package edu.usc.ict.nl.nlu.opennlp;

import java.io.File;
import java.util.Set;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.Model;
import edu.usc.ict.nl.nlu.NLUProcess;
import edu.usc.ict.nl.nlu.jmxnlu.JMXClassifierNLU;

public class MaxEntOpenNLPClassifierNLU extends JMXClassifierNLU {

	public MaxEntOpenNLPClassifierNLU(NLUConfig config) throws Exception {
		super(config);
	}

	@Override
	public NLUProcess startMXNLUProcessWithTheseParams(String model,int nbest) throws Exception {
		NLUProcess p = new MaxEntOpenNLPClassifierProcess(null);
		p.run(model, nbest);
		return p;
	}
	
	@Override
	public Model readModelFileNoCache(File mf) throws Exception {
		Model ret=null;
		return ret;
	}
	
	@Override
	public Set<String> getKnownWords() {
		MaxEntOpenNLPClassifierProcess p=(MaxEntOpenNLPClassifierProcess) getNLUProcess();
		return p.getKnownWords();
	}

}
