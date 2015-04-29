package edu.usc.ict.nl.nlu.ne;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.nlu.BuildTrainingData;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.parser.semantics.ParserSemanticRulesTimeAndNumbers;

public class TimeDuration extends BasicNE {

	public static final SpecialVar MonthsVar=new SpecialVar(null,"MONTHS",
			"Number of months extracted from a answer.time-period speech act.","0",Number.class);
	public static final SpecialVar daysVar=new SpecialVar(null,"DAYS",
			"Number of days extracted from a answer.time-period speech act.","0",Number.class);
	
	public TimeDuration() {
		addSpecialVarToRepository(MonthsVar);
		addSpecialVarToRepository(daysVar);
	}

	@Override
	public Map<String, Object> extractPayloadFromText(String text,String speechAct) throws Exception {
		Map<String, Object> payloads = null;
		if (speechAct.equals("answer.time-period")) {
			List<Token> tokens = BuildTrainingData.tokenize(text);
			Long num=getTimePeriodInSeconds(tokens);
			if (num!=null) {
				logger.info("Extracted time period of "+num+" seconds from the answer '"+text+"'.");
				if (payloads==null) payloads=new HashMap<String, Object>();
				payloads.put(MonthsVar.getName(), convertSecondsIn(num,ParserSemanticRulesTimeAndNumbers.numSecondsInMonth));
				payloads.put(daysVar.getName(), convertSecondsIn(num,ParserSemanticRulesTimeAndNumbers.numSecondsInDay));
			}
		}
		return payloads;
	}
	

}
