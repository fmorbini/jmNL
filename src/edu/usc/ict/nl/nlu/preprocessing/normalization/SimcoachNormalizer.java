package edu.usc.ict.nl.nlu.preprocessing.normalization;

import java.util.ArrayList;
import java.util.List;

import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.Token.TokenTypes;

public class SimcoachNormalizer extends Normalizer {
	
	private List<Token> normalize(Token t) {
			TokenTypes type=t.getType();
			String word=t.getName();
			if (type==TokenTypes.WORD) {
				if (word.equals("once")) {
					ret.add(new Token("1", TokenTypes.NUM,word));
					t.setName("time"); 
				} else if (word.equals("twice")) {
					ret.add(new Token("2", TokenTypes.NUM,word));
					t.setName("times"); 
				}
			}
			ret.add(t);
		}
		return ret;
	}
}
