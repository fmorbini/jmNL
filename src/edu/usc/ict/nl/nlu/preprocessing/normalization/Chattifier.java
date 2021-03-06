package edu.usc.ict.nl.nlu.preprocessing.normalization;

import java.util.List;

import edu.usc.ict.nl.config.PreprocessingConfig;
import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.preprocessing.TokenizerI;

public class Chattifier extends Normalizer {

	@Override
	public List<Token> normalize(List<Token> tokens,PreprocessingType type) {
		PreprocessingConfig config = getConfiguration(type);
		TokenizerI tokenizer=config.getNluTokenizer();
		if (tokens!=null && !tokens.isEmpty()) {
			int i=0;
			while(i<tokens.size()) {
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
					List<Token> tmp = tokenizer.tokenize1("i'm");
					if (tmp!=null && !tmp.isEmpty()) {
						size=tmp.size();
						tokens.remove(i);
						for(int j=0;j<size;j++) {
							tokens.add(i+j, tmp.get(j));
						}
					}
				} else if (word.equals("dont")) {
					List<Token> tmp = tokenizer.tokenize1("don't");
					if (tmp!=null && !tmp.isEmpty()) {
						size=tmp.size();
						tokens.remove(i);
						for(int j=0;j<size;j++) {
							tokens.add(i+j, tmp.get(j));
						}
					}
				} else if (word.equals("ive")) {
					List<Token> tmp = tokenizer.tokenize1("i've");
					if (tmp!=null && !tmp.isEmpty()) {
						size=tmp.size();
						tokens.remove(i);
						for(int j=0;j<size;j++) {
							tokens.add(i+j, tmp.get(j));
						}
					}
				}
				i+=size;
			}
		}
		return tokens;
	}

}
