package edu.usc.ict.nl.nlu.topic;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.keyword.KeywordREMatcher;
import edu.usc.ict.nl.nlu.keyword.TopicMatcherRE;
import edu.usc.ict.nl.nlu.opennlp.MaxEntOpenNLPClassifierProcess;
import edu.usc.ict.nl.nlu.opennlp.NLUTrainingFileReader;
import edu.usc.ict.nl.util.FileUtils;
import edu.usc.ict.nl.util.StringUtils;

public class WordlistTopicDetectionREProcess extends MaxEntOpenNLPClassifierProcess {

	public WordlistTopicDetectionREProcess(String exe) {
		super(exe);
	}

	private KeywordREMatcher matcher=null;

	
	@Override
	public void run(String model, int nb) throws Exception {
		nBest=nb;
		if (model != null)
			matcher=loadMatcher(new File(model));
	}

	public KeywordREMatcher loadMatcher(File serializedFile) {
		NLUTrainingFileReader f = new NLUTrainingFileReader(serializedFile);
		List<TrainingDataFormat> tds=null;
		while(f.hasNext()) {
			TrainingDataFormat td=f.nextTrainingData();
			if (tds==null) tds=new ArrayList<>();
			tds.add(td);
		}
		return new KeywordREMatcher(tds);
	}

	@Override
	public void train(String model, String trainingFile) throws Exception {
		FileUtils.dumpToFile(FileUtils.readFromFile(trainingFile).toString(),model);
		loadMatcher(new File(model));
	}
	
	@Override
	public void kill() {}
	
	@Override
	public String[] classify(String u,int nBest) throws IOException, InterruptedException {
		if (matcher!=null && matcher.matches(u)) {
			TopicMatcherRE tm = matcher.getLastMatchMatcher();
			String match=tm.getMatchedString(u);
			return new String[]{"1.0 "+tm.getTopicID()};
		}
		return null;
	}
	
	@Override
	public Set<String> getAllSimplifiedPossibleOutputs() {
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
	

}
