package edu.usc.ict.nl.nlu.preprocessing.stopwords;

import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.preprocessing.PreprocesserI;

public interface RemoverI extends PreprocesserI {
	public boolean remove(Token word);
}
