package edu.usc.ict.nl.nlu.preprocessing.normalization;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.Token.TokenTypes;

public class Chattifier extends Normalizer {

	private NLUConfig config;

	public Chattifier(NLUConfig config) {
		this.config=config;
	}
	
	@Override
	public List<Token> normalize(List<Token> tokens) {
		if (tokens!=null && !tokens.isEmpty()) {
			Iterator<Token> it = tokens.iterator();
			while(it.hasNext()) {
				Token cp=it.next();
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
					List<Token> tmpTs = applyBasicTransformationsToStringForClassification("i'm",tokenTypes,chattify,config);
					ret.addAll(tmpTs);
					continue;
				} else if (word.equals("dont")) {
					List<Token> tmpTs = applyBasicTransformationsToStringForClassification("don't",tokenTypes,chattify,config);
					ret.addAll(tmpTs);
					continue;
				} else if (word.equals("ive")) {
					List<Token> tmpTs = applyBasicTransformationsToStringForClassification("i've",tokenTypes,chattify,config);
					ret.addAll(tmpTs);
					continue;
				}

				ret.add(cp);
			}
		}
		return ret;
	}

}
