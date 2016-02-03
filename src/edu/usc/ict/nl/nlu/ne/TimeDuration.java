package edu.usc.ict.nl.nlu.ne;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.nlu.ne.searchers.TimePeriodSearcher;
import edu.usc.ict.nl.parser.semantics.ParserSemanticRulesTimeAndNumbers;

public class TimeDuration extends BasicNE {

	public static final SpecialVar MonthsVar=new SpecialVar(null,"MONTHS",
			"Number of months extracted from a answer.time-period speech act.","0",Number.class);
	public static final SpecialVar daysVar=new SpecialVar(null,"DAYS",
			"Number of days extracted from a answer.time-period speech act.","0",Number.class);

	public TimeDuration() {
		this(true);
	}
	public TimeDuration(boolean generalize) {
		this(generalize,"answer.time-period");
	}
	public TimeDuration(boolean generalize,String... sas) {
		super(sas);
		addSpecialVarToRepository(MonthsVar);
		addSpecialVarToRepository(daysVar);
		this.generalize=generalize;
	}

	@Override
	public List<NE> extractNamedEntitiesFromText(String text,PreprocessingType type) throws Exception {
		List<NE> payloads = null;
		TimePeriodSearcher ts = new TimePeriodSearcher(getConfiguration(),type,text);
		Long num=ts.getTimePeriodInSeconds();
		if (num!=null) {
			int start=ts.getStart(),end=ts.getEnd();
			logger.info("Extracted time period of "+num+" seconds from the answer '"+text+"'.");
			if (payloads==null) payloads=new ArrayList<NE>();
			payloads.add(new NE(MonthsVar.getName(), convertSecondsIn(num,ParserSemanticRulesTimeAndNumbers.numSecondsInMonth),this.getClass().getName(),start,end,text.substring(start,end),this));
			payloads.add(new NE(daysVar.getName(), convertSecondsIn(num,ParserSemanticRulesTimeAndNumbers.numSecondsInDay),this.getClass().getName(),start,end,text.substring(start,end),this));
		}
		return payloads;
	}
}
