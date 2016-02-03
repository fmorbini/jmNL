package edu.usc.ict.nl.nlu.preprocessing.normalization;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.Token.TokenTypes;

public class SimcoachPunctuationNormalizer extends Normalizer {
	private static final Pattern puntToKeep=Pattern.compile("^([\\?]+)|([\\%]+)|(')$");
	
	@Override
	public List<Token> normalize(List<Token> input,PreprocessingType type) {
		Iterator<Token> it=input.iterator();
		while(it.hasNext()) {
			Token t=it.next();
			if (t!=null && t.getType().equals(TokenTypes.OTHER)) {
				Matcher m=puntToKeep.matcher(t.getName());
				if (m.matches()) {
					t.setName(t.getName().substring(0, 1));
				} else {
					it.remove();
				}
			}
		}
		return input;
	}
}
