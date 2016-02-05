package edu.usc.ict.nl.nlu.preprocessing;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.Token.TokenTypes;

public interface TokenizerI {
	public List<List<Token>> tokenize(String text);
	public List<Token> tokenize1(String text);
	public String untokenize(List<Token> input,String sa);
	public int getStart(List<Token> tokens,String sa,int pos);
	public int getEnd(List<Token> tokens,String sa,int pos);
	public String tokAnduntok(String input);
	public LinkedHashMap<TokenTypes, Pattern> getTokenTypes();
}
