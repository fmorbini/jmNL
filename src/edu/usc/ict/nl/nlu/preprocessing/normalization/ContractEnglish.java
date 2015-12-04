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
import edu.usc.ict.nl.util.EnglishUtils;

public class ContractEnglish extends Normalizer {

	private NLUConfig config;

	public ContractEnglish(NLUConfig config) {
		this.config=config;
	}

	@Override
	public List<Token> normalize(List<Token> tokens) {
		if (tokens!=null && !tokens.isEmpty()) {
			Token pp=tokens.get(0);
			String pWord=pp.getName();
			for(int i=1;i<tokens.size();i++) {
				int size=1;
				Token cp=tokens.get(i);
				String word=cp.getName();
				String c=EnglishUtils.getContractionFor(pWord,word);
				if (c!=null) {
					TokenizerI tokenizer=config.getNluTokenizer();
					List<Token> tmpTs = tokenizer.tokenize1(c);
					if (tmpTs!=null && !tmpTs.isEmpty()) {
						size=tmpTs.size();
						tokens.remove(i-1);
						tokens.remove(i-1);
						for(int j=0;i<size;j++) {
							tokens.add(i-1+j, tmpTs.get(j));
						}
						size-=1;
					}
				}
				pp=cp;
				pWord=word;
			}
		}
		return tokens;
	}
}
