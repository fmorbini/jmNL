package edu.usc.ict.nl.nlu.preprocessing.normalization;

import java.util.List;

import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.config.PreprocessingConfig;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.preprocessing.TokenizerI;
import edu.usc.ict.nl.util.EnglishUtils;

public class ContractEnglish extends Normalizer {

	@Override
	public List<Token> normalize(List<Token> tokens,PreprocessingType type) {
		if (tokens!=null && !tokens.isEmpty()) {
			TokenizerI tokenizer=getConfiguration(type).getNluTokenizer();
			Token pp=tokens.get(0);
			String pWord=pp.getName();
			int s=tokens.size();
			for(int i=1;i<s;) {
				int size=1;
				Token cp=tokens.get(i);
				Token nt=(i+1<s)?tokens.get(i+1):null;
				String word=cp.getName();
				boolean waitForNext=nt!=null && EnglishUtils.getContractionFor(word,nt.getName())!=null;
				if (!waitForNext) {
					String c=EnglishUtils.getContractionFor(pWord,word);
					if (c!=null) {
						List<Token> tmpTs = tokenizer.tokenize1(c);
						if (tmpTs!=null && !tmpTs.isEmpty()) {
							size=tmpTs.size();
							tokens.remove(i-1);
							tokens.remove(i-1);
							for(int j=0;j<size;j++) {
								tokens.add(i-1+j, tmpTs.get(j));
							}
							size-=1;
						}
					}
				}
				pp=cp;
				pWord=word;
				i+=size;
			}
		}
		return tokens;
	}
}
