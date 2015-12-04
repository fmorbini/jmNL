package edu.usc.ict.nl.nlu.preprocessing;

import java.util.List;

import edu.usc.ict.nl.nlu.Token;

public interface PreprocesserI {
	public void run(List<List<Token>> input);
}
