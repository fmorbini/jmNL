package edu.usc.ict.nl.nlu.features;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.usc.ict.nl.util.StringUtils;

public abstract class FeaturesBuilderForOpenNLPClassifier {
	/**
	 * Derived loosely from FeaturesBuilderForMXClassifier
	 * @param utt
	 * @return features used by maxent classifier (opennlp implementation). Features are:
	 * 1) all unigrams, bigrams and trigrams
	 * 2) word frequencies in utt
	 * 3) utt length
	 * @throws Exception
	 */
	public static List<String> buildfeaturesFromUtterance(String utt) throws Exception {
		List<String> features=new ArrayList<String>();
		String[] tokens=(utt).split("[\\s]+");
		int l=tokens.length;
		features.add("len="+l);
		Map<String,Integer> wordcounts = new HashMap<String,Integer>();
		for(int i=0;i<l;i++) {
			String s0=tokens[i];
			if (!wordcounts.containsKey(s0)) {
				wordcounts.put(s0, 1);
			} else
				wordcounts.put(s0, wordcounts.get(s0)+1);
			//for(int j=i+1;j<l;j++) {
			//	features.add(s0+":"+tokens[j]);
			//}
		    features.add(s0);
		    String s1 = ((i+1)<l)?tokens[i+1]:"";
		    String s2 = ((i+2)<l)?tokens[i+2]:"";
		    features.add( s0 + ":" + s1 );
		    features.add( s0 + ":" + s1 + ":" + s2 );
		}
		for(String key:wordcounts.keySet()) {
			features.add(key+"="+wordcounts.get(key));
		}
		return features;
	}
	
	public static List<String> buildFeatureForWordAtPosition(String[] tokens,int pos) {
		int l=tokens.length;
		List<String> features=new ArrayList<String>();
		int apos=pos+1;
		assert(apos<l);
		for(int j=Math.max(0, apos-2);j<=apos;j++) {
		    String s1 = ((j+1)<l)?tokens[j+1]:"";
		    String s2 = ((j+2)<l)?tokens[j+2]:"";
			int d=apos-j;
			switch (d) {
			case 0:
				// unigram at position j
				features.add(tokens[j]);
			case 1:
				// bigram at position j
				assert(!StringUtils.isEmptyString(s1));
				features.add(tokens[j]+":"+s1);
				features.add("cb="+tokens[j]+":"+s1);
			case 2:
				// trigram at position j
				features.add(tokens[j]+":"+s1+":"+s2);
			}
		}
		return features;
	}
}
