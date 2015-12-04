package edu.usc.ict.nl.nlu.preprocessing.normalization;

import java.util.List;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.Token.TokenTypes;
import edu.usc.ict.nl.util.StringUtils;

public class Lowercase extends Normalizer {

	private NLUConfig config;

	public Lowercase(NLUConfig config) {
		this.config=config;
	}
	
	@Override
	public List<Token> normalize(List<Token> tokens) {
		if (tokens!=null && !tokens.isEmpty()) {
			for(Token t:tokens) {
				if (t!=null && t.isType(TokenTypes.WORD)) {
					String word=t.getName();
					if (!StringUtils.isEmptyString(word)) t.setName(word.toLowerCase());
				}
			}
		}
		return tokens;
	}

}
