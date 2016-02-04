package edu.usc.ict.nl.nlu.keyword;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.io.BuildTrainingData;
import edu.usc.ict.nl.utils.LogConfig;

/**
 * class that defines a map between regular expressions and speech acts or labels.
 * These mapping can be provided by a standard NLU style training data set where the utterances are the regular expressions
 * of by custom files in the hierarchical style: a root file lists a map between labels and files containing the list of regular expressions for that label.
 * @author morbini
 *
 */
public class KeywordREMatcher {

	private static final Pattern hierModelLine=Pattern.compile("^([^\\s]+)[\\s]+(.+)$");
	private List<ActualMultiREMatcher> topics=null;
	private ActualMultiREMatcher lastMatcher=null;
	protected static final Logger logger = Logger.getLogger(NLU.class.getName());
	static {
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );
	}

	/**
	 * here the matcher is built from a standard training data set in which the utterance corresponds to the regular expression and the label field remains the label. 
	 * @param data
	 */
	public KeywordREMatcher(List<TrainingDataFormat> data) {
		if (data!=null) {
			Map<String, List<TrainingDataFormat>> sas = BuildTrainingData.getAllSpeechActsWithTrainingData(data);
			for(String sa:sas.keySet()) {
				if (topics==null) topics=new ArrayList<ActualMultiREMatcher>();
				List<TrainingDataFormat> saPatterns = sas.get(sa);
				if (saPatterns!=null && !saPatterns.isEmpty()) {
					List<String> patterns=saPatterns.stream().map(s->s.getUtterance()).collect(Collectors.toList());
					topics.add(new ActualMultiREMatcher(sa,patterns));
				}
			}
		}
	}

	/**
	 * the file is the root file that contains a mapping between a label (first) and a file name (second).
	 * the file associated toa label contains a regular expression in each line, all to be joined in a OR.
	 * @param modelFile
	 * @throws Exception
	 */
	public KeywordREMatcher(File modelFile) throws Exception {
		try {
			topics=null;
			String line;
			BufferedReader in=new BufferedReader(new FileReader(modelFile));
			while((line=in.readLine())!=null) {
				Matcher m=hierModelLine.matcher(line);
				if (m.matches() && (m.groupCount()==2)) {
					String topicIdentifier=m.group(1);
					File thisNodeModelFile=new File(modelFile.getParent(),new File(m.group(2)).getName());
					if (topics==null) topics=new ArrayList<ActualMultiREMatcher>();
					topics.add(new ActualMultiREMatcher(topicIdentifier,thisNodeModelFile));
				}
			}
			in.close();
		} catch (Exception e) {
			logger.warn("Error during hierarchical model building.",e);
		}
	}
	
	public boolean matches(String text) {
		if (topics!=null) {
			for(ActualMultiREMatcher tm:topics) {
				this.lastMatcher=tm;
				if (tm.matches(text)) {
					return true;
				}
			}
		}
		reset();
		return false;
	}
	private int topicIndex=0;
	public boolean findIn(String text) {
		if (topics!=null) {
			for(topicIndex=0;topicIndex<topics.size();topicIndex++) {
				ActualMultiREMatcher tm=topics.get(topicIndex);
				this.lastMatcher=tm;
				if (tm.find(text)) return true;
			}
		}
		reset();
		return false;
	}
	public boolean findNext() {
		String text=null;
		if (topics!=null) {
			boolean newTopic=false;
			for(;topicIndex<topics.size();topicIndex++) {
				ActualMultiREMatcher tm=topics.get(topicIndex);
				if (newTopic) tm.set(text);
				else text=tm.text;
				this.lastMatcher=tm;
				if (tm.find()) return true;
				newTopic=true;
			}
		}
		reset();
		return false;
	}
	
	public void reset() {
		topicIndex=0;this.lastMatcher=null;
	}
	
	public ActualMultiREMatcher getLastMatchMatcher() {
		return lastMatcher;
	}
	
	public List<ActualMultiREMatcher> getTopics() {
		return topics;
	}
}
