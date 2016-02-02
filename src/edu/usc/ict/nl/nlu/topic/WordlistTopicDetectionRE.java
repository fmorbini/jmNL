package edu.usc.ict.nl.nlu.topic;

import java.util.Set;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.NLUProcess;
import edu.usc.ict.nl.nlu.mxnlu.MXClassifierNLU;

public class WordlistTopicDetectionRE extends MXClassifierNLU {

	public WordlistTopicDetectionRE(NLUConfig c) throws Exception {
		super(c);
	}

	@Override
	public NLUProcess startMXNLUProcessWithTheseParams(String model, int nbest) throws Exception {
		WordlistTopicDetectionREProcess process = new WordlistTopicDetectionREProcess(null);
		process.run(model, nbest);
		return process;
	}
	
	@Override
	public boolean isPossibleNLUOutput(NLUOutput o) throws Exception {
		Set<String> outputs = getAllSimplifiedPossibleOutputs();
		if (outputs!=null && outputs.contains(o.getId())) return true;
		return false;
	}

	@Override
	public Set<String> getAllSimplifiedPossibleOutputs() throws Exception {
		WordlistTopicDetectionREProcess p=(WordlistTopicDetectionREProcess) getNLUProcess();
		return p.getAllSimplifiedPossibleOutputs();
	}

	@Override
	public void kill() {
	}
}
