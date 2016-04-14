package edu.usc.ict.nl.nlu.preprocessing.stemmer;

import java.util.List;

import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.preprocessing.Preprocesser;
import edu.usc.ict.nl.util.StringUtils;

public abstract class Stemmer extends Preprocesser implements StemmerI {

	@Override
	public void run(List<List<Token>> input,PreprocessingType type) {
		if (input!=null) {
			for(List<Token> pi:input) {
				for(Token t:pi) {
					if (t!=null && t.isType(Token.TokenTypes.WORD)) {
						String stemmed=stemm(t.getName());
						if (!StringUtils.isEmptyString(stemmed)) t.setName(stemmed);
					}
				}
			}
		}
	}

	@Override
	public void update() {
	}
}
