package edu.usc.ict.nl.nlu;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.usc.ict.nl.bus.modules.NLUInterface;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.io.BuildTrainingData;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.PerformanceResult;

public class NoNLU implements NLUInterface {

	private NLUConfig configuration=null;
	
	public NoNLU(NLUConfig c) {
		this.configuration=c;
	}
	
	@Override
	public List<NLUOutput> getNLUOutput(String text, Set<String> possibleNLUOutputIDs, Integer nBest) throws Exception {
		return null;
	}

	@Override
	public List<NLUOutput> getNLUOutputFake(String[] NLUOutputIDs, String text) throws Exception {
		return null;
	}

	@Override
	public Map<String, Object> getPayload(String sa, String text) throws Exception {
		return null;
	}

	@Override
	public void kill() throws Exception {
	}

	@Override
	public void loadModel(File nluModel) throws Exception {
	}

	@Override
	public void train(List<TrainingDataFormat> input, File model) throws Exception {
	}

	@Override
	public void train(File input, File model) throws Exception {
	}

	@Override
	public void retrain() throws Exception {
	}

	@Override
	public void retrain(File... files) throws Exception {
	}

	@Override
	public boolean nluTest(TrainingDataFormat testSample, List<NLUOutput> nluResults) throws Exception {
		return false;
	}

	@Override
	public PerformanceResult test(List<TrainingDataFormat> test, File model, boolean printErrors) throws Exception {
		return null;
	}

	@Override
	public PerformanceResult test(File test, File model, boolean printErrors) throws Exception {
		return null;
	}

	@Override
	public boolean isPossibleNLUOutput(NLUOutput o) throws Exception {
		return false;
	}

	@Override
	public Set<String> getAllSimplifiedPossibleOutputs() throws Exception {
		return null;
	}

	@Override
	public BuildTrainingData getBTD() {
		return null;
	}

	@Override
	public void setBTD(BuildTrainingData btd) {
	}

	@Override
	public Map<String, ConfusionEntry> computeConfusionMatrix() throws Exception {
		return null;
	}

	@Override
	public NLUConfig getConfiguration() {
		return configuration;
	}

	@Override
	public List<Pair<String, Float>> getTokensScoresForLabel(String utt, String label, String modelFileName)
			throws Exception {
		return null;
	}

	@Override
	public List<String> getFeaturesFromUtterance(String utt) {
		return null;
	}

	@Override
	public List<String> getFeaturesFromPositionInUtterance(String[] tokens, int pos) {
		return null;
	}

	@Override
	public Set<String> getKnownWords() {
		return null;
	}

}
