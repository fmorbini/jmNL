package edu.usc.ict.nl.nlu.ne.searchers;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.BuildTrainingData;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.ne.BasicNE;
import edu.usc.ict.nl.parser.ChartParser;
import edu.usc.ict.nl.parser.ChartParser.Item;
import edu.usc.ict.nl.parser.semantics.ParserSemanticRulesTimeAndNumbers;
import edu.usc.ict.nl.util.FunctionalLibrary;

public class TimePeriodSearcher {
	private List<Token> tokens;
	private String text;
	private int end;
	private int start;
	private ChartParser parser;
	private NLUConfig config=null;

	public TimePeriodSearcher(NLUConfig config,String input) {
		this.config=config;
		try {
			this.text=input;
			this.tokens=BuildTrainingData.tokenize(input);
			File grammar=null;
			if (config==null) grammar=new File("resources/characters/common/nlu/time-period-grammar.txt");
			else {
				grammar=new File(config.getNLUContentRoot(),"time-period-grammar.txt");
				if (config.getNlBusConfigNC()!=null && !grammar.exists()) grammar=new File(config.getNlBusConfigNC().getContentRoot(),"common/nlu/time-period-grammar.txt");
			}
			if (grammar.exists()) {
				parser = ChartParser.getParserForGrammar(grammar);
			}
		} catch (Exception e) {
			BasicNE.logger.error(e);
		}

	}
	public Long getTimePeriodInSeconds() {
		try {
			List<String> ts=(List<String>) FunctionalLibrary.map(tokens, Token.class.getMethod("getOriginal"));
			Collection<Item> result = parser.parseAndFilter(ts,"<TP>");
			if (result.size()==1) {
				Iterator<Item> resultIterator = result.iterator();
				Item r=resultIterator.next();
				this.end=tokens.get(r.getEnd()-1).getEnd();
				this.start=tokens.get(r.getStart()).getStart();
				return Math.round((Double)r.getSemantics());
			}
		} catch (Exception e) {
			BasicNE.logger.error(e);
		}
		return null;
	}
	
	public static boolean equalDoublesWithPrecision(Double d1,Double d2,double precision) {
		precision=Math.abs(precision);
		if ((d1==null) || (d2==null)) return d1==d2;
		else return ((d1<=(d2+Math.abs(d2)*precision)) && (d1>=(d2-(Math.abs(d2)*precision))));
	}

	public Double getTimesEachDay() {
		try {
			List<String> ts=(List<String>) FunctionalLibrary.map(tokens, Token.class.getMethod("getOriginal"));
			Collection<Item> result = parser.parseAndFilter(ts,"<FP>");
			Double prevTimesEachSecond=null;
			for (Item r:result) {
				Double timesEachSecond=(Double)r.getSemantics();
				this.end=tokens.get(r.getEnd()-1).getEnd();
				this.start=tokens.get(r.getStart()).getStart();
				if ((prevTimesEachSecond!=null) && !equalDoublesWithPrecision(prevTimesEachSecond,timesEachSecond,0.01)) return null;
				else prevTimesEachSecond=timesEachSecond;
			}
			if (prevTimesEachSecond==null) {
				BasicNE.logger.info(" extracting frequency from: "+text);
				return null;
			} else {
				return ParserSemanticRulesTimeAndNumbers.numSecondsInDay*prevTimesEachSecond;
			}
		} catch (Exception e) {
			BasicNE.logger.error(e);
		}
		return null;
	}
	
	public int getStart() {
		return start;
	}
	public int getEnd() {
		return end;
	}
}