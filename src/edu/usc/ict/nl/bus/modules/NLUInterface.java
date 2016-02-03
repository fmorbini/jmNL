package edu.usc.ict.nl.bus.modules;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.ConfusionEntry;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.io.BuildTrainingData;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.PerformanceResult;

public interface NLUInterface {
	
	public List<NLUOutput> getNLUOutput(String text,Set<String> possibleNLUOutputIDs,Integer nBest) throws Exception;
	public List<NLUOutput> getNLUOutputFake(String[] NLUOutputIDs,String text) throws Exception;
	public Map<String, Object> getPayload(String sa,String text) throws Exception;
	public void kill() throws Exception;
	public void loadModel(File nluModel) throws Exception;
	
	public void train(List<TrainingDataFormat> input,File model) throws Exception;
	public void train(File input,File model) throws Exception;
	public void retrain() throws Exception;
	public void retrain(File... files) throws Exception;
	public boolean nluTest(TrainingDataFormat testSample,List<NLUOutput> nluResults) throws Exception;
	public PerformanceResult test(List<TrainingDataFormat> test,File model,boolean printErrors) throws Exception;
	public PerformanceResult test(File test,File model,boolean printErrors) throws Exception;
	
	public boolean isPossibleNLUOutput(NLUOutput o) throws Exception;
	public Set<String> getAllSimplifiedPossibleOutputs() throws Exception;
	
	public BuildTrainingData getBTD();
	public void setBTD(BuildTrainingData btd);
	
	public Map<String,ConfusionEntry> computeConfusionMatrix() throws Exception;
	public NLUConfig getConfiguration();
	
	public List<Pair<String,Float>> getTokensScoresForLabel(String utt,String label,String modelFileName) throws Exception;
	public List<String> getFeaturesFromUtterance(String utt);
	public List<String> getFeaturesFromPositionInUtterance(String[] tokens,int pos);
}
