package edu.usc.ict.nl.nlu.echo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.util.PerformanceResult;
import edu.usc.ict.nl.util.StringUtils;

public class EchoNLU extends NLU {

	public EchoNLU(NLUConfig c) throws Exception {
		super(c);
	}

	@Override
	public List<NLUOutput> getNLUOutput(String text,Set<String> possibleNLUOutputIDs,Integer nBest) throws Exception {
		String emptyEvent=getConfiguration().getEmptyTextEventName();
		List<NLUOutput> ret=new ArrayList<NLUOutput>();
		if (StringUtils.isEmptyString(text) && !StringUtils.isEmptyString(emptyEvent)) ret.add(new NLUOutput(text, emptyEvent, 1, null));
		else ret.add(new NLUOutput(text,text, 1f, null));
		return ret;
	}

	@Override
	public List<NLUOutput> getNLUOutputFake(String[] NLUOutputIDs, String text)
			throws Exception {
		return getNLUOutput(text, null,null);
	}
	@Override
	public Map<String, Object> getPayload(String sa, String text)
			throws Exception {
		return null;
	}
	
	@Override
	public void kill() {
	}

	@Override
	public void loadModel(File nluModel) {
	}

	@Override
	public void train(File input, File model) throws Exception {
		throw new Exception("unhandled");
	}
	@Override
	public void train(List<TrainingDataFormat> input, File model)
			throws Exception {
		throw new Exception("unhandled");
	}

	@Override
	public PerformanceResult test(File test, File model,boolean printErrors) throws Exception {
		throw new Exception("unhandled");
	}
	@Override
	public PerformanceResult test(List<TrainingDataFormat> test, File model,
			boolean printErrors) throws Exception {
		throw new Exception("unhandled");
	}
	@Override
	public boolean nluTest(TrainingDataFormat testSample, List<NLUOutput> nluResults) throws Exception {
		throw new Exception("unhandled");
	}

	@Override
	public void retrain() throws Exception {
		throw new Exception("unhandled");
	}

	@Override
	public boolean isPossibleNLUOutput(NLUOutput o) {
		return false;
	}

	@Override
	public HashSet<String> getAllSimplifiedPossibleOutputs() throws Exception {
		throw new Exception("unhandled");
	}

}
