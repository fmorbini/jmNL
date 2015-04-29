package edu.usc.ict.nl.nlu.ne;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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
		private int start=0,nextstart=0;
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
						if (n!=null && n instanceof Number)
							this.nextstart=inputTokens.get(r.getEnd()-1).getEnd();
							return ((Number)n).doubleValue();
					}
				}
			} catch (Exception e) {e.printStackTrace();}
			return null;
		}
		
	}
	public static Long getTimePeriodInSeconds(List<Token> tokens) {
		ChartParser parser;
		try {
			parser = ChartParser.getParserForGrammar(new File("resources/characters/common/nlu/time-period-grammar.txt"));
			ArrayList<String> input=new ArrayList<String>();
			for (Token t:tokens) {
				input.add(t.getName());
			}
			Collection<Item> result = parser.parseAndFilter(input,"<TP>");
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
	public static Double getTimesEachDay(List<Token> tokens) {
		ChartParser parser;
		try {
			parser = ChartParser.getParserForGrammar(new File("resources/characters/common/nlu/time-period-grammar.txt"));
			ArrayList<String> input=new ArrayList<String>();
			for (Token t:tokens) {
				input.add(t.getName());
			}
			Collection<Item> result = parser.parseAndFilter(input,"<FP>");
			Double prevTimesEachSecond=null;
			for (Item r:result) {
				Double timesEachSecond=(Double)r.getSemantics();
				if ((prevTimesEachSecond!=null) && !equalDoublesWithPrecision(prevTimesEachSecond,timesEachSecond,0.01)) return null;
				else prevTimesEachSecond=timesEachSecond;
			}
			if (prevTimesEachSecond==null) {
				logger.info(" extracting frequency from: "+BuildTrainingData.untokenize(tokens));
				return null;
			} else return ParserSemanticRulesTimeAndNumbers.numSecondsInDay*prevTimesEachSecond;
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}

	@Override
	public Token generalize(Token input) {
		return null;
	}
}
