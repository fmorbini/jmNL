package edu.usc.ict.nl.nlu.preprocessing.tokenizer;

import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import edu.usc.ict.nl.nlu.Token.TokenTypes;

public class NoTokenizer extends Tokenizer {
	private static final LinkedHashMap<TokenTypes, Pattern> tokenTypes=new LinkedHashMap<TokenTypes, Pattern>(){
		private static final long serialVersionUID = 1L;
		{
			put(TokenTypes.OTHER,Pattern.compile("^.*$"));
		}
	};
	
	@Override
	public java.util.LinkedHashMap<TokenTypes,Pattern> getTokenTypes() {
		return tokenTypes;
	}
}
