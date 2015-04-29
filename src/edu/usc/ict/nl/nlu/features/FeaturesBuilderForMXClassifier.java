package edu.usc.ict.nl.nlu.features;

import java.util.ArrayList;
import java.util.List;

import edu.usc.ict.nl.util.StringUtils;

public abstract class FeaturesBuilderForMXClassifier {
	//len=4 cb=<s>:my cb=<s>:friend cb=<s>:</s> <s> <s>:my <s>:my:friend cb=my:friend cb=my:</s> my my:friend my:friend:</s> cb=friend:</s> friend friend:</s> friend:</s>: </s> </s>: </s>::
	/**
	 * 
	 * @param utt
	 * @return features used by maxent classifier (mxnlu, C implementation). Features are:
	 * 1) number of tokens in input utterance
	 * 2) bag-of-words style features composed by all sets of 2 tokens from the utterance.
	 * 3) all unigrams, bigrams and trigrams
	 * @throws Exception
	 */
	public static List<String> buildfeaturesFromUtterance(String utt) throws Exception {
		List<String> features=new ArrayList<String>();
		String[] tokens=("<s> "+utt+" </s>").split("[\\s]+");
		int l=tokens.length;
		features.add("len="+l);
		for(int i=0;i<l;i++) {
			String s0=tokens[i];
			for(int j=i+1;j<l;j++) {
				features.add("cb="+s0+":"+tokens[j]);
			}
		    features.add(s0);
		    String s1 = ((i+1)<l)?tokens[i+1]:"";
		    String s2 = ((i+2)<l)?tokens[i+2]:"";
		    features.add( s0 + ":" + s1 );
		    features.add( s0 + ":" + s1 + ":" + s2 );
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
