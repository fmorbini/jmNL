package edu.usc.ict.nl.nlu.ne;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.nlu.ne.searchers.TimePeriodSearcher;
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
	public List<NE> extractNamedEntitiesFromText(String text,String speechAct) throws Exception {
		List<NE> payloads = null;
		if (speechAct!=null && speechAct.equals("answer.time-period")) {
			TimePeriodSearcher ts = new TimePeriodSearcher(getConfiguration(),text);
			Long num=ts.getTimePeriodInSeconds();
			if (num!=null) {
				int start=ts.getStart(),end=ts.getEnd();
				logger.info("Extracted time period of "+num+" seconds from the answer '"+text+"'.");
				if (payloads==null) payloads=new ArrayList<NE>();
				payloads.add(new NE(MonthsVar.getName(), convertSecondsIn(num,ParserSemanticRulesTimeAndNumbers.numSecondsInMonth),this.getClass().getName(),start,end,text.substring(start,end)));
				payloads.add(new NE(daysVar.getName(), convertSecondsIn(num,ParserSemanticRulesTimeAndNumbers.numSecondsInDay),this.getClass().getName(),start,end,text.substring(start,end)));
			}
		}
		return payloads;
	}
}
