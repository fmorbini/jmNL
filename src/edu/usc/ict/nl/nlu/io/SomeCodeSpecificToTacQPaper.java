package edu.usc.ict.nl.nlu.io;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.Pair;

public class SomeCodeSpecificToTacQPaper extends BuildTrainingData {
	public SomeCodeSpecificToTacQPaper(NLUConfig config) throws Exception {
		super(config);
	}
	private static final HashMap<Pattern,String> highLevelTacQSpeechActs=new HashMap<Pattern, String>();
	static {
		highLevelTacQSpeechActs.put(Pattern.compile("^whq\\..*$"), "whq");
		highLevelTacQSpeechActs.put(Pattern.compile("^ynq\\..*$"), "ynq");
		highLevelTacQSpeechActs.put(Pattern.compile("^settopic\\..*$"), "settopic");
		highLevelTacQSpeechActs.put(Pattern.compile("^offeragentplayername.*$"), "offeragent");
		highLevelTacQSpeechActs.put(Pattern.compile("^preclosingvalence.*$"), "preclosingvalence");
		highLevelTacQSpeechActs.put(Pattern.compile("^closingvalence.*$"), "closingvalence");
	}
	
	public enum SAStatType {NOSA,FREQ,ALL};
	public void printStatistics(List<TrainingDataFormat> td,boolean compressSpeechActs,SAStatType sa) throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
		List<Pair<String, Integer>> otherStats = getStatistics(td, compressSpeechActs);
		int totUsages=0;
		for(Pair<String, Integer>saAndNum:otherStats) totUsages+=saAndNum.getSecond();
		System.out.println(otherStats.size()+" "+totUsages);
		for(Pair<String, Integer>saAndNum:otherStats) {
			if (sa==SAStatType.FREQ) {
				System.out.println((float)saAndNum.getSecond()/(float)totUsages);
			} else if (sa==SAStatType.ALL) {
				System.out.println(saAndNum.getFirst()+" "+saAndNum.getSecond()+" "+((float)saAndNum.getSecond()/(float)totUsages));
			}
		}
		// compute unique texts
		// unique labels
		// total frequency in training data and number of training data with backup labels
		Set<String> uniqueLabels=new HashSet<String>(FunctionalLibrary.map(td, TrainingDataFormat.class.getMethod("getLabel")));
		Map<String,String> uniqueUtterances=new HashMap<String,String>();
		for(TrainingDataFormat t:td) {
			String u=t.getUtterance();
			String l=t.getLabel();
			if (uniqueUtterances.containsKey(u) && !uniqueUtterances.get(u).equals(l))
				System.out.println("ERROR: multiple labels for utterance: "+u);
			uniqueUtterances.put(u, l);
		}
		Collection backupLabels=FunctionalLibrary.removeIfNot(td, TrainingDataFormat.class.getMethod("hasBackupLabels"));
		Set uniqueBackupLabels=new HashSet(backupLabels);
		System.out.println("Unique utterances: "+uniqueUtterances.size());
		System.out.println("Unique labels: "+uniqueLabels.size());
		System.out.println("Backup labels: "+backupLabels.size()+"/"+td.size()+" unique: "+uniqueBackupLabels.size());
	}
	public List<Pair<String,Integer>> getStatistics(List<TrainingDataFormat> td,boolean compressSpeechActs) {
		HashMap<String,Integer> stats=new HashMap<String, Integer>();
		HashMap<String,HashSet<String>> numcompression=new HashMap<String, HashSet<String>>();
		List<Pair<String,Integer>> ret=new ArrayList<Pair<String,Integer>>();
		for(TrainingDataFormat p:td) {
			String sa=p.getLabel();
			if (compressSpeechActs) {
				for(Entry<Pattern,String> pAndN:highLevelTacQSpeechActs.entrySet()) {
					Matcher m=pAndN.getKey().matcher(sa);
					if (m.matches()) {
						HashSet<String> v=numcompression.get(pAndN.getValue());
						if (v==null) numcompression.put(pAndN.getValue(), v=new HashSet<String>());
						v.add(sa);
						sa=pAndN.getValue();
						break;
					}
				}
			}
			if (stats.containsKey(sa))
				stats.put(sa, stats.get(sa)+1);
			else
				stats.put(sa,1);
		}
		if (compressSpeechActs) {
			for(String sa:numcompression.keySet()) {
				System.out.println(sa+" "+numcompression.get(sa).size());
			}
		}
		for(String sa:stats.keySet()) {
			ret.add(new Pair<String, Integer>(sa, stats.get(sa)));
		}
		Collections.sort(ret, new Comparator<Pair<String,Integer>>() {

			@Override
			public int compare(Pair<String, Integer> arg0,Pair<String, Integer> arg1) {
				return arg1.getSecond()-arg0.getSecond();
			}
		});
		return ret;
	}
}
