package edu.usc.ict.nl.nlu.preprocessing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

import com.sun.corba.se.impl.ior.GenericTaggedComponent;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.Token.TokenTypes;
import edu.usc.ict.nl.utils.EnglishWrittenNumbers2Digits;

public class Preprocess {
	
	
	private NLUConfig config;

	public Preprocess(NLUConfig c) {
		this.config=c;
	}
	
	public NLUConfig getConfiguration() {
		return config;
	}

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
		NLUConfig config=getConfiguration();
		TokenizerI tokenizer = config.getNluTokenizer();
		List<PreprocesserI> prs = config.getNluPreprocessers();
		List<List<Token>> tokens = tokenizer.tokenize(text);
		if (prs!=null) {
			for(PreprocesserI pr:prs) {
				pr.run(tokens);
			}
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
