package edu.usc.ict.nl.nlu.ne;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.bus.special_variables.SpecialEntitiesRepository;
import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.BuildTrainingData;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.parser.ChartParser;
import edu.usc.ict.nl.parser.ChartParser.Item;
import edu.usc.ict.nl.parser.semantics.ParserSemanticRulesTimeAndNumbers;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.utils.LogConfig;

public abstract class BasicNE implements NamedEntityExtractorI {
	private NLUConfig configuration;
	
	private SpecialEntitiesRepository svs=null;
	
	protected static final Logger logger = Logger.getLogger(NLU.class.getName());
	static {
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );
	}
	
	public NLUConfig getConfiguration() {
		return configuration;
	}
	public void setConfiguration(NLUConfig configuration) {
		this.configuration = configuration;
	}
	
	
	@Override
	public List<SpecialVar> getSpecialVariables() throws Exception {
		if (svs!=null)  return svs.getAllSpecialVariables();
		return null;
	}
	protected void addSpecialVarToRepository(SpecialVar v) {
		if (svs==null) svs=new SpecialEntitiesRepository(null);
		svs.addSpecialVariable(v);
	}

	public class NumberSearcher {
		private int start=0,nextstart=0,thisstart=0;
		private Matcher m;
		private final Pattern p=Pattern.compile("([\\+\\-]*[\\d\\.]+[eE\\+\\-\\d]*)");
		private ChartParser parser=null;
		private List<String> input=new ArrayList<String>();
		private List<Token> inputTokens=new ArrayList<Token>();
		public NumberSearcher(String text) {
			m=p.matcher(text);
			NLUConfig nluConfig = getConfiguration();
			File grammar=new File(nluConfig.getNLUContentRoot(),"written-numbers-grammar.txt");
			if (nluConfig.nlBusConfig!=null && !grammar.exists()) grammar=new File(nluConfig.nlBusConfig.getContentRoot(),"common/nlu/written-numbers-grammar.txt");
			if (grammar.exists()) {
				try {
					parser = ChartParser.getParserForGrammar(grammar);
					inputTokens = BuildTrainingData.tokenize(text);
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
			int dStart=nextstart;
			Double numW=findWrittenNumber();
			int wStart=nextstart;
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
	public static Long getTimePeriodInSeconds(String input) {
		ChartParser parser;
		try {
			parser = ChartParser.getParserForGrammar(new File("resources/characters/common/nlu/time-period-grammar.txt"));
			String[] parts=input.split("[\\s]+");
			Collection<Item> result = parser.parseAndFilter(Arrays.asList(parts),"<TP>");
			if (result.size()==1) {
				Iterator<Item> resultIterator = result.iterator();
				Item r=resultIterator.next();
				return Math.round((Double)r.getSemantics());
			}
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	public static Double convertSecondsIn(Long seconds,Double conversion) {		
		if (seconds!=null) {
			return seconds/conversion;
		}
		return null;
	}
	public static boolean equalDoublesWithPrecision(Double d1,Double d2,double precision) {
		precision=Math.abs(precision);
		if ((d1==null) || (d2==null)) return d1==d2;
		else return ((d1<=(d2+Math.abs(d2)*precision)) && (d1>=(d2-(Math.abs(d2)*precision))));
	}
	
	// return number of times per day
	public static Double getTimesEachDay(String text) {
		ChartParser parser;
		try {
			parser = ChartParser.getParserForGrammar(new File("resources/characters/common/nlu/time-period-grammar.txt"));
			String[] input=text.split("[\\s]+");
			Collection<Item> result = parser.parseAndFilter(Arrays.asList(input),"<FP>");
			Double prevTimesEachSecond=null;
			for (Item r:result) {
				Double timesEachSecond=(Double)r.getSemantics();
				if ((prevTimesEachSecond!=null) && !equalDoublesWithPrecision(prevTimesEachSecond,timesEachSecond,0.01)) return null;
				else prevTimesEachSecond=timesEachSecond;
			}
			if (prevTimesEachSecond==null) {
				logger.info(" extracting frequency from: "+text);
				return null;
			} else return ParserSemanticRulesTimeAndNumbers.numSecondsInDay*prevTimesEachSecond;
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}

	public String fromTokensToOriginalString(List<Token> in) {
		try {
			return FunctionalLibrary.printCollection(FunctionalLibrary.map(in, Pair.class.getMethod("getOriginal")), "", "", " ");
		} catch (Exception e) {
			logger.error(e);
		}
		return null;
	}
	
	@Override
	public void generalize(List<Token> inputTokens) {
		List<Integer> tokenStarts=new ArrayList<Integer>();
		int i=0;
		for(Token t:inputTokens) {
			tokenStarts.add(i);
			i+=1+t.getOriginal().length();
		}
		assert(tokenStarts.size()==inputTokens.size());
		String input=fromTokensToOriginalString(inputTokens);
		try {
			List<NE> nes = extractNamedEntitiesFromText(input, null);
			if (nes!=null) {
				for(NE ne:nes) {
					int start=ne.getStart();
					int end=ne.getEnd();
					boolean isWholeWordsSubstring=StringUtils.isWholeWordSubstring(start,end,input);
					if (isWholeWordsSubstring) {
						for(int j=getTokenAtPosition(start,tokenStarts);j<getTokenAtPosition(end,tokenStarts);j++) {
							Token original=inputTokens.get(j);
							Token newToken=new Token("<"+ne.getType()+">", original.getType(), ne.getMatchedString(), start, end);
							inputTokens.set(j, newToken);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("error generalizing text", e);
		}
	}
	private int getTokenAtPosition(int chPos, List<Integer> tokenStarts) {
		int token=0;
		for(int tokenStartPos:tokenStarts) {
			if (chPos<tokenStartPos) return token-1;
			token++;
		}
		return token;
	}
	public static Map<String, Object> createPayload(List<NE> foundNEs) {
		Map<String,Object> ret=null;
		if (foundNEs!=null) {
			for(NE ne:foundNEs) {
				if (ne!=null) {
					String vname=ne.getVarName();
					if (ret==null) ret=new HashMap<String, Object>();
					Object content=ret.get(vname);
					if (content==null) ret.put(vname, ne.getValue());
					else if (content instanceof List) ((List) content).add(ne.getValue());
					else {
						Object oldElement=content;
						content=new ArrayList();
						ret.put(vname, content);
						((List) content).add(oldElement);
						((List)content).add(ne.getValue());
					}
				}
			}
		}
		return ret;
	}
}
