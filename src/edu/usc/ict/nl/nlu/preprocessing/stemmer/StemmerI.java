package edu.usc.ict.nl.nlu.preprocessing.stemmer;

import edu.usc.ict.nl.nlu.preprocessing.PreprocesserI;

public interface StemmerI extends PreprocesserI {
	public String stemm(String word);
}
