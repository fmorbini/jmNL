package edu.usc.ict.nl.nlu.textmatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.opennlp.MaxEntOpenNLPClassifierProcess;
import edu.usc.ict.nl.nlu.preprocessing.TokenizerI;
import edu.usc.ict.nl.util.AhoCorasickList;
import edu.usc.ict.nl.util.AhoCorasickList.Match;
import edu.usc.ict.nl.util.AhoCorasickList.MatchList;
import edu.usc.ict.nl.util.FileUtils;

public class SimpleStringMatcherProcess extends MaxEntOpenNLPClassifierProcess {

	private List<TopicMatcher> topics=null;
	private NLUConfig config;

	public SimpleStringMatcherProcess(NLUConfig config) {
		super(null);
		this.config=config;
	}

	public NLUConfig getConfiguration() {
		return config;
	}
	
	private static final Pattern hierModelLine=Pattern.compile("^([^\\s]+)[\\s]+(.+)$");

	
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
					List<String> ts = getTokensStrings(line,PreprocessingType.TRAINING);
					m.addPattern(ts, topicID);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public List<String> getTokensStrings(String line,PreprocessingType type) throws Exception {
		TokenizerI tokenizer = getConfiguration().getNluTokenizer(type);
		List<Token> tokens = tokenizer.tokenize1(line);
		List<String> ts=tokens.stream().map(s->s.getName()).collect(Collectors.toList());
		return ts;
	}
	
	@Override
	public void run(String model, int nb) throws Exception {
		nBest=nb;
		if (model != null)
			topics=loadMatcher(new File(model));
	}

	public List<TopicMatcher> loadMatcher(File modelFile) {
		List<TopicMatcher> topics=null;
		try {
			String line;
			BufferedReader in=new BufferedReader(new FileReader(modelFile));
			while((line=in.readLine())!=null) {
				Matcher m=hierModelLine.matcher(line);
				if (m.matches() && (m.groupCount()==2)) {
					String topicIdentifier=m.group(1);
					File thisNodeModelFile=new File(getConfiguration().getNLUContentRoot(),new File(m.group(2)).getName());
					if (topics==null) topics=new ArrayList<TopicMatcher>();
					topics.add(new TopicMatcher(topicIdentifier,thisNodeModelFile));
				}
			}
			in.close();
		} catch (Exception e) {
			NLU.getLogger().warn(e);
		}
		return topics;
	}

	@Override
	public void train(String model, String trainingFile) throws Exception {
		FileUtils.dumpToFile(FileUtils.readFromFile(trainingFile).toString(),model);
		loadMatcher(new File(model));
	}
	
	@Override
	public void kill() {}
	
	@Override
	public String[] classify(String u,int nBest) throws Exception {
		if (topics!=null) {
			List<String> sList = getTokensStrings(u,PreprocessingType.RUN);
			for(TopicMatcher tm:topics) {
				MatchList[] chart = tm.m.findMatches(sList);
				List<Match> result = tm.m.getOptimalMatchSequence(chart, 0, new HashMap<Integer, List<Match>>());
				if (result!=null && !result.isEmpty()) {
					Match pick=result.get(0);
					return new String[]{"1.0 "+pick.payload};
				}
			}
		}
		return null;
	}
	
	@Override
	public Set<String> getAllSimplifiedPossibleOutputs() {
		if (topics!=null) {
			List<String> list = topics.stream().map(s->s.topicID).collect(Collectors.toList());
			if (list!=null && !list.isEmpty()) {
				return new HashSet<>(list);
			}
		}
		return null;
	}
}
