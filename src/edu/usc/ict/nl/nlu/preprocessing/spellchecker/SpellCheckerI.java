package edu.usc.ict.nl.nlu.preprocessing.spellchecker;

import edu.usc.ict.nl.nlu.preprocessing.PreprocesserI;

public interface SpellCheckerI extends PreprocesserI {
	public String correct(String word);
}
