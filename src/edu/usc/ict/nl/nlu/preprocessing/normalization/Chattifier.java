package edu.usc.ict.nl.nlu.preprocessing.normalization;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.Token.TokenTypes;
import edu.usc.ict.nl.nlu.preprocessing.TokenizerI;

public class Chattifier extends Normalizer {

	private NLUConfig config;

	public Chattifier(NLUConfig config) {
		this.config=config;
	}
	
	@Override
	public List<Token> normalize(List<Token> tokens) {
		if (tokens!=null && !tokens.isEmpty()) {
			for(int i=0;i<tokens.size();) {
				int size=1;
				Token cp=tokens.get(i);
				String word=cp.getName();
				if (word.equals("you")) {
					cp.setName("u");
				} else if (word.equals("are")) {
					cp.setName("r");
				} else if (word.equals("great")) {
					cp.setName("gr8");
				} else if (word.equals("your")) {
					cp.setName("ur");
				} else if (word.equals("im")) {
					TokenizerI tokenizer = config.getNluTokenizer();
					List<Token> tmp = tokenizer.tokenize1("i'm");
					if (tmp!=null && !tmp.isEmpty()) {
						size=tmp.size();
						for(int j=0;j<size;j++) tokens.add(i+j, tmp.get(j));
					}
				} else if (word.equals("dont")) {
					TokenizerI tokenizer = config.getNluTokenizer();
					List<Token> tmp = tokenizer.tokenize1("don't");
					if (tmp!=null && !tmp.isEmpty()) {
						size=tmp.size();
						for(int j=0;j<size;j++) tokens.add(i+j, tmp.get(j));
					}
				} else if (word.equals("ive")) {
					TokenizerI tokenizer = config.getNluTokenizer();
					List<Token> tmp = tokenizer.tokenize1("i've");
					if (tmp!=null && !tmp.isEmpty()) {
						size=tmp.size();
						for(int j=0;j<size;j++) tokens.add(i+j, tmp.get(j));
					}
				}
				i+=size;
			}
		}
		return tokens;
	}

}
