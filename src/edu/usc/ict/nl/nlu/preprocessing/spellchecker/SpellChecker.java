package edu.usc.ict.nl.nlu.preprocessing.spellchecker;

import java.util.List;

import edu.usc.ict.nl.config.PreprocessingConfig;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.preprocessing.Preprocesser;
import edu.usc.ict.nl.nlu.preprocessing.TokenizerI;
import edu.usc.ict.nl.util.StringUtils;

public abstract class SpellChecker extends Preprocesser implements SpellCheckerI {
	
	@Override
	public void run(List<List<Token>> input) {
		if (input!=null) {
			PreprocessingConfig config = getConfiguration();
			for(List<Token> pi:input) {
				int position=0;
				while(position<pi.size()) {
					int size=1;
					Token t=pi.get(position);
					if (t!=null && t.isType(Token.TokenTypes.WORD)) {
						String corrected=correct(t.getName());
						if (!StringUtils.isEmptyString(corrected)) {
							TokenizerI tokenizer = config.getNluTokenizer();
							List<Token> firstOption=tokenizer.tokenize1(corrected);
							if (firstOption!=null && !firstOption.isEmpty()) {
								size=firstOption.size();
								pi.set(position,firstOption.get(0));
								for(int i=1;i<size;i++) {
									pi.add(position+i, firstOption.get(i));
								}
							}
						}
					}
					position+=size;
				}
			}
		}
	}
}
