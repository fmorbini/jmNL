package edu.usc.ict.nl.nlu.preprocessing.normalization;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.Token.TokenTypes;
import edu.usc.ict.nl.nlu.preprocessing.stopwords.Remover;

public class Punctuation extends Normalizer {
	private static final Pattern puntToKeep=Pattern.compile("^([\\?]+)|([\\%]+)|(')$");
	
	@Override
	public List<Token> normalize(List<Token> input) {
		for(Token t:input) {
			if (t!=null && t.getType().equals(TokenTypes.OTHER)) {
				Matcher m=puntToKeep.matcher(t.getName());
				if (m.matches()) {
					t.setName(t.getName().substring(0, 1));
				}
			}
		}
		return input;
	}
}
