package edu.usc.ict.nl.nlu.preprocessing.stemmer;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import edu.usc.ict.nl.config.NLConfig;

public class PorterStemmer implements StemmerI {
	SnowballStemmer stemmer;
	public PorterStemmer(NLConfig config) {
        stemmer = (SnowballStemmer) new englishStemmer();
	}
	@Override
	public String stemm(String word) {
		if (stemmer!=null) {
			stemmer.setCurrent(word);
			stemmer.stem();
			return stemmer.getCurrent();
		}
		else return null;
	}
}
