package edu.usc.ict.nl.nlu.preprocessing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.util.StringUtils;

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

	public List<List<Token>> prepareUtteranceForClassification(String text) throws Exception {
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
	
	public List<String> getStrings(List<List<Token>> input) {
		List<String> ret=null;
		if (input!=null) {
			TokenizerI tokenizer = getConfiguration().getNluTokenizer();
			for(List<Token> ts:input) {
				if (ts!=null) {
					String s=tokenizer.untokenize(ts);
					if (!StringUtils.isEmptyString(s)) {
						if (ret==null) ret=new ArrayList<>();
						ret.add(s);
					}
				}
			}
		}
		return ret;
	}

}
