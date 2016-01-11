package edu.usc.ict.nl.nlu.preprocessing.stemmer;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

public class PorterStemmer extends Stemmer {
	SnowballStemmer stemmer;
	public PorterStemmer() {
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
