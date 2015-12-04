package edu.usc.ict.nl.nlu.preprocessing.stemmer;

import edu.usc.ict.nl.config.NLConfig;


public class KStemmer implements StemmerI {
	org.apache.lucene.analysis.ICTKStemmer stemmer;
	public KStemmer(NLConfig config) {
		stemmer=new org.apache.lucene.analysis.ICTKStemmer();
	}
	@Override
	public String stemm(String word) {
		if (stemmer!=null) return stemmer.stem(word);
		else return null;
	}
}
