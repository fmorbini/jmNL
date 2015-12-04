package edu.usc.ict.nl.nlu.preprocessing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.Token.TokenTypes;
import edu.usc.ict.nl.utils.EnglishWrittenNumbers2Digits;

public class Preprocess {
	public static String getStringOfTokensSpan(List<Token> tokens,int start, int end) {
		StringBuffer ret=null;
		if (tokens!=null) {
			int i=0;
			for(Token t:tokens) {
				if (i>=start && i<end) {
					if (ret==null) ret=new StringBuffer();
					ret.append(((ret.length()==0)?"":" ")+t.getName());
				}
				i++;
			}
		}
		return (ret!=null)?ret.toString():null;
	}
	
	public List<Token> applyBasicTransformationsToStringForClassification(String text) throws Exception {
		return applyBasicTransformationsToStringForClassification(text,defaultTokenTypes);
	}
	public List<Token> applyBasicTransformationsToStringForClassification(String text,LinkedHashMap<TokenTypes, Pattern> tokenTypes) throws Exception {
		return applyBasicTransformationsToStringForClassification(text, tokenTypes, true,getConfiguration());
	}
	public static List<Token> applyBasicTransformationsToStringForClassification(String text,LinkedHashMap<TokenTypes, Pattern> tokenTypes,boolean chattify,NLUConfig config) throws Exception {
		text=text.toLowerCase();
		List<Token> tokens=tokenize(text,tokenTypes);		
		if (sc!=null) tokens=doSpellCheck(tokens);
		tokens=uk2us(tokens);
		tokens=filterPunctuation(tokens);
		tokens=normalizeTokens(tokens);
		if (chattify) tokens=chattify(tokens,tokenTypes,chattify,config);
		tokens=contractEnglish(tokens,tokenTypes,chattify,config);
		if (stemmer!=null) tokens=stemm(tokens);
		try {
			tokens=EnglishWrittenNumbers2Digits.parseWrittenNumbers(config,tokens);
		} catch (Exception e) {
			logger.warn("Error converting written numbers to value.",e);
		}
		return tokens;
	}

	public List<List<Token>> prepareUtteranceForClassification(String text) throws Exception {
		List<List<Token>> ret=null;
		List<Token> tokens = applyBasicTransformationsToStringForClassification(text,tokenTypes);
		List<List<Token>> lTokens = generalize(tokens);
		if (lTokens!=null) {
			for(List<Token> lToken:lTokens) {
				if (ret==null) ret=new ArrayList<>();
				ret.add(lToken);
			}
		}
		return ret;
	}

}
