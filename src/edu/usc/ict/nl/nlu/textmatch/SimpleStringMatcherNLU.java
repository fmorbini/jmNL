package edu.usc.ict.nl.nlu.textmatch;

import java.util.Set;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.NLUProcess;
import edu.usc.ict.nl.nlu.mxnlu.MXClassifierNLU;

public class SimpleStringMatcherNLU extends MXClassifierNLU {

	public SimpleStringMatcherNLU(NLUConfig c) throws Exception {
		super(c);
	}

	@Override
	public NLUProcess startMXNLUProcessWithTheseParams(String model, int nbest) throws Exception {
		SimpleStringMatcherProcess process = new SimpleStringMatcherProcess(getConfiguration());
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
		SimpleStringMatcherProcess p=(SimpleStringMatcherProcess) getNLUProcess();
		return p.getAllSimplifiedPossibleOutputs();
	}

	@Override
	public void kill() {
	}
}
