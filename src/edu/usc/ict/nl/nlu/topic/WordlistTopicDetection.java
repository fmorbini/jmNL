package edu.usc.ict.nl.nlu.topic;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sun.security.acl.WorldGroupImpl;
import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.BuildTrainingData;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.Token.TokenTypes;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.mxnlu.MXClassifierNLU;
import edu.usc.ict.nl.util.AhoCorasickList;
import edu.usc.ict.nl.util.AhoCorasickList.Match;
import edu.usc.ict.nl.util.AhoCorasickList.MatchList;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.PerformanceResult;
import edu.usc.ict.nl.util.StringUtils;

public class WordlistTopicDetection extends NLU {

	private List<TopicMatcher> topics=null;

	public WordlistTopicDetection(NLUConfig c) throws Exception {
		super(c);
		
		String nluModel=c.getNluModelFile();
		loadModel(new File(nluModel));
		
	}
	
	private static final Pattern hierModelLine=Pattern.compile("^([^\\s]+)[\\s]+(.+)$");
	@Override
	public void loadModel(File modelFile) throws Exception {
		try {
			topics=null;
			String line;
			BufferedReader in=new BufferedReader(new FileReader(modelFile));
			while((line=in.readLine())!=null) {
				Matcher m=hierModelLine.matcher(line);
				if (m.matches() && (m.groupCount()==2)) {
					String topicIdentifier=m.group(1);
					File thisNodeModelFile=new File(getConfiguration().getNLUContentRoot(),new File(m.group(2)).getName());
					if (topics==null) topics=new ArrayList<WordlistTopicDetection.TopicMatcher>();
					topics.add(new TopicMatcher(topicIdentifier,thisNodeModelFile));
				}
			}
			in.close();
		} catch (Exception e) {
			logger.warn("Error during hierarchical model building.",e);
		}
	}
	
	private static LinkedHashMap<TokenTypes, Pattern> topicTokenTypes=new LinkedHashMap<TokenTypes, Pattern>(BuildTrainingData.defaultTokenTypes);
	private class TopicMatcher {
		AhoCorasickList m;
		final String topicID; 
		public TopicMatcher(String topicIdentifier, File patternFile) {
			topicID=topicIdentifier;
			m=new AhoCorasickList();
			loadPatternsFromFile(patternFile.getAbsolutePath());
		}
		
		public void loadPatternsFromFile(String patternFile) {
			String line;
			try {
				BufferedReader in=new BufferedReader(new FileReader(patternFile));
				while((line=in.readLine())!=null) {
					List<String> ts = getTokensStrings(line);
					m.addPattern(ts, topicID);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public List<String> getTokensStrings(String line) throws Exception {
		BuildTrainingData b = getBTD();
		String processedText=(getConfiguration().getApplyTransformationsToInputText())?b.prepareUtteranceForClassification(line,topicTokenTypes):line;
		List<Token> tokens = b.tokenize(processedText, topicTokenTypes);
		List<String> ts=(List<String>) FunctionalLibrary.map(tokens, Token.class.getMethod("getName"));
		return ts;
	}

	@Override
	public List<NLUOutput> getNLUOutput(String text,Set<String> possibleNLUOutputIDs,Integer nBest) throws Exception {
		List<NLUOutput> nluResult = null;
		if (topics!=null) {
			List<String> sList = getTokensStrings(text);
			for(TopicMatcher tm:topics) {
				MatchList[] chart = tm.m.findMatches(sList);
				List<Match> result = tm.m.getOptimalMatchSequence(chart, 0, new HashMap<Integer, List<Match>>());
				if (result!=null && !result.isEmpty()) {
					Match pick=result.get(0);
					if (nluResult==null) nluResult=new ArrayList<NLUOutput>();
					nluResult.add(new NLUOutput(text, pick.payload+"", 1, null));
				}
			}
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
		List<TopicMatcher> oldTopics = topics;
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
		topics=oldTopics;
		return result;
	}

	@Override
	public boolean isPossibleNLUOutput(NLUOutput o) throws Exception {
		if (topics!=null) {
			String id=o.getId();
			for(TopicMatcher tm:topics) {
				if (!StringUtils.isEmptyString(tm.topicID)) {
					if (tm.topicID.equals(id)) return true;
				}
			}
		}
		return false;
	}

	@Override
	public HashSet<String> getAllSimplifiedPossibleOutputs() throws Exception {
		HashSet<String> ret = null;
		if (topics!=null) {
			for(TopicMatcher tm:topics) {
				if (!StringUtils.isEmptyString(tm.topicID)) {
					if (ret==null) ret=new HashSet<String>();
					ret.add(tm.topicID);
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
		c.setNluClass(WordlistTopicDetection.class.getCanonicalName());
		c.setInternalNluClass4Hier(MXClassifierNLU.class.getCanonicalName());
		c.setForcedNLUContentRoot("C:\\Users\\morbini\\simcoach2\\svn_dcaps\\trunk\\core\\DM\\resources\\characters\\Ellie_DCAPS_AI\\nlu\\");
		c.setNluModelFile("military - Copy");
		WordlistTopicDetection wt = new WordlistTopicDetection(c);
		List<NLUOutput> r = wt.getNLUOutput("the things i eat are many fruits liek bananas and apples", null,null);
		System.out.println(r);
	}
}
