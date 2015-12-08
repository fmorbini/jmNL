package edu.usc.ict.nl.nlu.preprocessing;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.config.PreprocessingConfig;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.ne.NE;
import edu.usc.ict.nl.util.StringUtils;

public class Preprocess {
	
	
	private PreprocessingConfig config;

	public Preprocess(PreprocessingConfig c) {
		this.config=c;
	}
	
	public PreprocessingConfig getConfiguration() {
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

	public List<List<Token>> process(String text) throws Exception {
		PreprocessingConfig config=getConfiguration();
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
					String s=getString(ts, tokenizer);
					if (!StringUtils.isEmptyString(s)) {
						if (ret==null) ret=new ArrayList<>();
						ret.add(s);
					}
				}
			}
		}
		return ret;
	}
	public String getString(List<Token> ts) {
		return getString(ts, getConfiguration().getNluTokenizer());
	}
	public static String getString(List<Token> ts,TokenizerI tokenizer) {
		if (ts!=null) {
			String s=tokenizer.untokenize(ts);
			if (!StringUtils.isEmptyString(s)) {
				return s;
			}
		}
		return null;
	}
	
	public List<NE> getAssociatedNamedEntities(List<Token> input) {
		Set<NE> ret=null;
		if (input!=null && !input.isEmpty()) {
			for(Token t:input) {
				if (t!=null) {
					NE ne=t.getAssociatedNamedEntity();
					if (ne!=null) {
						if (ret==null) ret=new LinkedHashSet<>();
						ret.add(ne);
					}
				}
			}
		}
		return ret!=null?new ArrayList<>(ret):null;
	}

}
