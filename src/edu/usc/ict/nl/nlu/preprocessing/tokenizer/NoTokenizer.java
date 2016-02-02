package edu.usc.ict.nl.nlu.preprocessing.tokenizer;

import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import edu.usc.ict.nl.nlu.Token.TokenTypes;

public class NoTokenizer extends Tokenizer {
	protected static LinkedHashMap<TokenTypes, Pattern> defaultTokenTypes=new LinkedHashMap<TokenTypes, Pattern>(){
		private static final long serialVersionUID = 1L;
		{
			put(TokenTypes.OTHER,Pattern.compile("^.*$"));
		}
	};
}
