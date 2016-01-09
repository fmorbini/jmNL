package edu.usc.ict.nl.nlu.ne.searchers;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.preprocessing.TokenizerI;
import edu.usc.ict.nl.parser.ChartParser;
import edu.usc.ict.nl.parser.ChartParser.Item;

public class NumberSearcher {
	/**
	 * 
	 */
	private int start=0,nextstart=0,thisstart=0;
	private Matcher m;
	private final Pattern p=Pattern.compile("([\\+\\-]*[\\d\\.]+[eE\\+\\-\\d]*)");
	private ChartParser parser=null;
	private List<String> input=new ArrayList<String>();
	private List<Token> inputTokens=new ArrayList<Token>();
	public NumberSearcher(NLUConfig nluConfig, String text) {
		TokenizerI tokenizer=nluConfig.getNluTokenizer();
		m=p.matcher(text);
		File grammar=new File(new File(nluConfig.getNlBusConfigNC().getContentRoot()).getParent(),"preprocessing/written-numbers-grammar.txt");
		if (grammar.exists()) {
			try {
				parser = ChartParser.getParserForGrammar(grammar);
				inputTokens = tokenizer.tokenize1(text);
				for (Token t:inputTokens) {
					input.add(t.getName());
				}
			} catch (Exception e) {	
				e.printStackTrace();
			}
		}
	}
	private int findTokenIndexForCharIndex(int charIndex) {
		int out=-1;
		if (inputTokens!=null) {
			for(Token t:inputTokens) {
				out++;
				if (t.getStart()>=charIndex) return out;
			}
		}
		return out;
	}
	public Double getNextNumber() {
		Double numD=findNumber();
		if (numD==null) {
			start=-1;
		} else {
			start=nextstart;
		}
		return numD;
	}
	public Double getNextNumberAlsoWritten() {
		Double numD=findNumber();
		int dStart=nextstart;
		Double numW=findWrittenNumber();
		int wStart=nextstart;
		if (numW!=null && wStart<=start) numW=null;
		Double num=null;
		if (numD==null) {
			if (numW!=null) {
				this.start=wStart;
				num=numW;
			} else {
				start=-1;
			}
		} else if (numW!=null) {
			if (dStart<wStart) {
				num=numD;
				this.start=dStart;
			} else {
				num=numW;
				this.start=wStart;
			}
		} else {
			num=numD;
			this.start=dStart;
		}
		
		return num;
	}
	public boolean possiblyContainingNumber() {return start>=0;}
	public Double findNumber() {
		try {
			if (m.find(start)) {
				this.nextstart=m.end();
				this.thisstart=m.start();
				Double number=Double.parseDouble(m.group(1));
				return number;
			}
		} catch (NumberFormatException e) {}
		return null;
	}
	public Double findWrittenNumber() {
		try {
			if (parser!=null) {
				Collection<Item> result = parser.parseAndFilter(input,"<N>",findTokenIndexForCharIndex(start));
				if (result.isEmpty()) return null;
				else {
					Iterator<Item> resultIterator = result.iterator();
					Item r=resultIterator.next();
					Object n=r.getSemantics();
					if (n!=null && n instanceof Number) {
						this.nextstart=inputTokens.get(r.getEnd()-1).getEnd();
						this.thisstart=inputTokens.get(r.getStart()).getStart();
						return ((Number)n).doubleValue();
					}
				}
			}
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	public int getStart() {
		return thisstart;
	}
	public int getEnd() {
		return nextstart;
	}
}