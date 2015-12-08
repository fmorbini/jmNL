package edu.usc.ict.nl.nlu.topic;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.io.BuildTrainingData;
import edu.usc.ict.nl.nlu.keyword.KeywordREMatcher;
import edu.usc.ict.nl.nlu.keyword.KeywordREMatcher.TopicMatcherRE;
import edu.usc.ict.nl.nlu.mxnlu.MXClassifierNLU;
import edu.usc.ict.nl.nlu.preprocessing.TokenizerI;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.PerformanceResult;
import edu.usc.ict.nl.util.StringUtils;

public class WordlistTopicDetectionRE extends NLU {

	private KeywordREMatcher matcher=null;
	
	public WordlistTopicDetectionRE(NLUConfig c) throws Exception {
		super(c);
		
		String nluModel=c.getNluModelFile();
		loadModel(new File(nluModel));
		
	}
	
	@Override
	public void loadModel(File nluModel) throws Exception {
		this.matcher=new KeywordREMatcher(nluModel);
	};
	
	public List<String> getTokensStrings(String line) throws Exception {
		TokenizerI tokenizer = getConfiguration().getNluTokenizer();
		List<Token> tokens = tokenizer.tokenize1(line);
		List<String> ts=(List<String>) FunctionalLibrary.map(tokens, Token.class.getMethod("getName"));
		return ts;
	}

	@Override
	public List<NLUOutput> getNLUOutput(String text,Set<String> possibleNLUOutputIDs,Integer nBest) throws Exception {
		List<NLUOutput> nluResult = null;
		if (matcher!=null && matcher.matches(text)) {
			TopicMatcherRE tm = matcher.getLastMatchMatcher();
			String match=tm.getMatchedString(text);
			if (nluResult==null) nluResult=new ArrayList<NLUOutput>();
			nluResult.add(new NLUOutput(text, tm.getTopicID(), 1, NLU.createPayload(tm.getTopicID(),DialogueKBFormula.generateStringConstantFromContent(match), null)));
		}
		if (nluResult==null || nluResult.isEmpty()) {
			String lowConfidenceEvent=getConfiguration().getLowConfidenceEvent();
			if (StringUtils.isEmptyString(lowConfidenceEvent)) {
				logger.warn(" no user speech acts left and LOW confidence event disabled, returning no NLU results.");
			} else {
				if (nluResult==null) nluResult=new ArrayList<NLUOutput>();
				nluResult.add(new NLUOutput(text,lowConfidenceEvent,1f,null));
				logger.warn(" no user speech acts left. adding the low confidence event.");
			}
		}
		return nluResult;
	}
	@Override
	public List<NLUOutput> getNLUOutputFake(String[] NLUOutputIDs, String text)	throws Exception {
		return null;
	}
	
	@Override
	public Map<String, Object> getPayload(String sa, String text) throws Exception {
		return null;
	}

	@Override
	public void train(List<TrainingDataFormat> input, File model) throws Exception {
	}

	@Override
	public void train(File input, File model) throws Exception {
	}

	@Override
	public boolean nluTest(TrainingDataFormat testSample, List<NLUOutput> nluResults) throws Exception {
		return (!nluResults.isEmpty() && testSample.match(nluResults.get(0).getId()));
	}

	@Override
	public PerformanceResult test(List<TrainingDataFormat> test, File model, boolean printErrors) throws Exception {
		return testNLUOnThisData(test, model, printErrors);
	}

	@Override
	public PerformanceResult test(File testFile, File modelFile, boolean printErrors) throws Exception {
		if (!testFile.isAbsolute()) testFile=new File(getConfiguration().getNLUContentRoot(),testFile.getPath());
		if (!modelFile.isAbsolute()) modelFile=new File(getConfiguration().getNLUContentRoot(),modelFile.getPath());
		BuildTrainingData btd=getBTD();
		List<TrainingDataFormat> td=btd.buildTrainingDataFromNLUFormatFile(testFile);

        td=btd.cleanTrainingData(td);
        
		return testNLUOnThisData(td, modelFile, printErrors);
	}
	
	public PerformanceResult testNLUOnThisData(List<TrainingDataFormat> testing,File model,boolean printMistakes) throws Exception {
		KeywordREMatcher oldMatcher = matcher;
		loadModel(model);
		PerformanceResult result=new PerformanceResult();
		for(TrainingDataFormat td:testing) {
			List<NLUOutput> sortedNLUOutput = getNLUOutput(td.getUtterance(),null,null);
			if (nluTest(td, sortedNLUOutput)) {
				result.add(true);
			} else {
				result.add(false);
				if (printMistakes) {
					if (sortedNLUOutput==null || sortedNLUOutput.isEmpty()) logger.error("'"+td.getUtterance()+"' ("+td.getLabel()+") -> NOTHING");
					else logger.error("'"+td.getUtterance()+"' ("+td.getLabel()+") ->"+sortedNLUOutput.get(0));
				}
			}
		}
		matcher=oldMatcher;
		return result;
	}

	@Override
	public boolean isPossibleNLUOutput(NLUOutput o) throws Exception {
		if (matcher!=null) {
			String id=o.getId();
			List<TopicMatcherRE> topics=matcher.getTopics();
			for(TopicMatcherRE tm:topics) {
				if (!StringUtils.isEmptyString(tm.getTopicID())) {
					if (tm.getTopicID().equals(id)) return true;
				}
			}
		}
		return false;
	}

	@Override
	public HashSet<String> getAllSimplifiedPossibleOutputs() throws Exception {
		HashSet<String> ret = null;
		if (matcher!=null) {
			for(TopicMatcherRE tm:matcher.getTopics()) {
				if (!StringUtils.isEmptyString(tm.getTopicID())) {
					if (ret==null) ret=new HashSet<String>();
					ret.add(tm.getTopicID());
				}
			}
		}
		return ret;
	}

	@Override
	public void kill() throws Exception {
	}
	
	public static void main(String[] args) throws Exception {
		NLUConfig c=(NLUConfig) NLUConfig.WIN_EXE_CONFIG.clone();
		c.setNluClass(WordlistTopicDetectionRE.class.getCanonicalName());
		//c.setDefaultCharacter("Ellie_DCAPS");
		c.setForcedNLUContentRoot("C:\\Users\\morbini\\simcoach2\\svn_dcaps\\trunk\\core\\DM\\resources\\characters\\Ellie_DCAPS_AI\\nlu\\");
		c.setNluModelFile("military");
		//c.setNluModelFile("classifier-model-multi-topic_detector");
		c.setInternalNluClass4Hier(MXClassifierNLU.class.getCanonicalName());
		WordlistTopicDetectionRE wt = new WordlistTopicDetectionRE(c);
		List<NLUOutput> r = wt.getNLUOutput("in the military they clean the bills.", null,null);
		System.out.println(r);
	}
}
