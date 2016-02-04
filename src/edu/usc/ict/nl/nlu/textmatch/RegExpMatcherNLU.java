package edu.usc.ict.nl.nlu.textmatch;

import java.util.Set;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.NLUProcess;
import edu.usc.ict.nl.nlu.mxnlu.MXClassifierNLU;

public class RegExpMatcherNLU extends MXClassifierNLU {

	public RegExpMatcherNLU(NLUConfig c) throws Exception {
		super(c);
	}

	@Override
	public NLUProcess startMXNLUProcessWithTheseParams(String model, int nbest) throws Exception {
		RegExpMatcherNLUProcess process = new RegExpMatcherNLUProcess(null);
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
		RegExpMatcherNLUProcess p=(RegExpMatcherNLUProcess) getNLUProcess();
		return p.getAllSimplifiedPossibleOutputs();
	}

	@Override
	public void kill() {
	}
}
