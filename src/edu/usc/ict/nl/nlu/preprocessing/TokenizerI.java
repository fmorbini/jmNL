package edu.usc.ict.nl.nlu.preprocessing;

import java.util.List;

import edu.usc.ict.nl.nlu.Token;

public interface TokenizerI {
	public List<List<Token>> tokenize(String text);
	public List<Token> tokenize1(String text);
	public String untokenize(List<Token> input);
	public String tokAnduntok(String input);
}
