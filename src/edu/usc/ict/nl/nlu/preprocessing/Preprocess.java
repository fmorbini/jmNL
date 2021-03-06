package edu.usc.ict.nl.nlu.preprocessing;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.config.PreprocessingConfig;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.ne.NE;
import edu.usc.ict.nl.util.StringUtils;

public class Preprocess {


	private NLU nlu;
	private PreprocessingConfig config;
	private PreprocessingType type;

	public Preprocess(NLU nlu,PreprocessingType type) {
		this.nlu=nlu;
		this.type=type;
		if (nlu!=null) {
			this.config=nlu.getConfiguration().getPreprocessingConfig(type);
			if (config!=null){
				List<PreprocesserI> prs = config.getNluPreprocessers();
				if (prs!=null) for(PreprocesserI pr:prs) pr.setNlu(nlu);
			}
		}
	}

	public PreprocessingConfig getConfiguration() {
		return config;
	}

	public PreprocessingType getType() {
		return type;
	}

	public TokenizerI getTokenizer() {
		PreprocessingConfig config=getConfiguration();
		TokenizerI tokenizer = config.getNluTokenizer();
		return tokenizer;
	}
	
	/**
	 * from token at position start (included) to token at position end (excluded).
	 * start is the first and end is the second
	 * @param tokens
	 * @param start
	 * @param end
	 * @return
	 */
	public static int[] getStringSpanOfTokenSpan(List<Token> tokens,int start, int end) {
		int[] ret=new int[]{-1,-1};
		assert(start>=0 && end>start);
		if (tokens!=null && tokens.size()>=end) {
			Token t=tokens.get(start);
			ret[0]=t.getStart();
			if ((end-1)>start) t=tokens.get(end-1);
			ret[1]=t.getEnd();
		}
		return ret;
	}
	
	/**
	 * from token at position start (included) to token at position end (excluded).
	 * @param tokens
	 * @param start
	 * @param end
	 * @return
	 */
	public static String getCurrentStringOfTokensSpan(List<Token> tokens,int start, int end) {
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
	/**
	 * from token at position start (included) to token at position end (excluded).
	 * @param tokens
	 * @param start
	 * @param end
	 * @return
	 */
	public static String getOriginalStringOfTokensSpan(List<Token> tokens,int start, int end) {
		StringBuffer ret=null;
		if (tokens!=null) {
			int i=0;
			for(Token t:tokens) {
				if (i>=start && i<end) {
					if (ret==null) ret=new StringBuffer();
					ret.append(((ret.length()==0)?"":" ")+t.getOriginal());
				}
				i++;
			}
		}
		return (ret!=null)?ret.toString():null;
	}

	/**
	 * returns all possible preprocessing options
	 * @param text
	 * @return
	 * @throws Exception
	 */
	public List<List<Token>> process(String text) throws Exception {
		return process(text, false);
	}
	/**
	 * if acceptOnlyUnambigous is set to true, the method will throw an exception if more than 1 option becomes available.
	 * @param text
	 * @param acceptOnlyUnambigous
	 * @return
	 * @throws Exception
	 */
	public List<List<Token>> process(String text,boolean acceptOnlyUnambigous) throws Exception {
		PreprocessingConfig config=getConfiguration();
		TokenizerI tokenizer = config.getNluTokenizer();
		List<PreprocesserI> prs = config.getNluPreprocessers();
		List<List<Token>> tokens = tokenizer.tokenize(text);
		if (prs!=null) {
			for(PreprocesserI pr:prs) {
				if (acceptOnlyUnambigous && tokens!=null && tokens.size()>1) throw new Exception("more than one option created during processing and option for just 1 set.");
				pr.run(tokens,type);
				for(List<Token> ts:tokens) {
					tokenizer.updateStartsAndEnds(ts, null);
				}
			}
		}
		if (acceptOnlyUnambigous && tokens!=null && tokens.size()>1) throw new Exception("more than one option created during processing and option for just 1 set.");
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
		return getString(ts, tokenizer, null);
	}
	public static String getString(List<Token> ts,TokenizerI tokenizer,String sa) {
		if (ts!=null) {
			String s=tokenizer.untokenize(ts,sa);
			if (!StringUtils.isEmptyString(s)) {
				return s;
			}
		}
		return null;
	}

	public static boolean hasAssociatedNamedEntities(List<Token> input) {
		if (input!=null && !input.isEmpty()) {
			for(Token t:input) {
				if (t!=null) {
					NE ne=t.getAssociatedNamedEntity();
					if (ne!=null) {
						return true;
					}
				}
			}
		}
		return false;
	}
	public static List<NE> getAssociatedNamedEntities(List<Token> input) {
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

	public void update() {
		List<PreprocesserI> prs = config.getNluPreprocessers();
		if (prs!=null) {
			for(PreprocesserI pr:prs) {
				pr.update();
			}
		}
	}
}
