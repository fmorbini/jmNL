package edu.usc.ict.nl.nlu.preprocessing.normalization;

import java.util.List;

import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.Token.TokenTypes;

public class SimcoachNormalizer extends Normalizer {
	
	public List<Token> normalize(List<Token> input) {
		for(int i=0;i<input.size();i++) {
			int size=1;
			Token t=input.get(i);
			TokenTypes type=t.getType();
			String word=t.getName();
			if (type==TokenTypes.WORD) {
				if (word.equals("once")) {
					input.add(i, new Token("1", TokenTypes.NUM,word));
					t.setName("time");
					size=2;
				} else if (word.equals("twice")) {
					input.add(i, new Token("2", TokenTypes.NUM,word));
					t.setName("times"); 
					size=2;
				}
			}
			i+=size;
		}
		return input;
	}
}
