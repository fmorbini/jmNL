package edu.usc.ict.nl.nlu.preprocessing.normalization;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.Token.TokenTypes;
import edu.usc.ict.nl.util.EnglishUtils;

public class ContractEnglish extends Normalizer {

	@Override
	public String normalize(String word) {
		// TODO Auto-generated method stub
		return null;
	}

	private static List<Token> contractEnglish(List<Token> tokens, LinkedHashMap<TokenTypes, Pattern> tokenTypes, boolean chattify, NLUConfig config) throws Exception {
		List<Token> ret=new ArrayList<Token>();
		if (tokens!=null && !tokens.isEmpty()) {
			Iterator<Token> it = tokens.iterator();
			Token pp=it.next();
			Token cp=pp;
			while(it.hasNext()) {
				cp=it.next();
				
				String pWord=pp.getName();
				
				String word=cp.getName();
				
				String c=EnglishUtils.getContractionFor(pWord,word);
				if (c!=null) {
					List<Token> tmpTs = applyBasicTransformationsToStringForClassification(c,tokenTypes,chattify,config);
					ret.addAll(tmpTs);
					if (it.hasNext()) cp=it.next();
					else cp=null;
				} else {
					ret.add(pp);
				}
				
				pp=cp;
			}
			if (cp!=null) ret.add(cp);
		}
		return ret;
	}
}
