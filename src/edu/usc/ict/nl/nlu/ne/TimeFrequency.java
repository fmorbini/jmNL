package edu.usc.ict.nl.nlu.ne;

import java.util.ArrayList;
import java.util.List;

import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.nlu.ne.searchers.TimePeriodSearcher;
import edu.usc.ict.nl.parser.semantics.ParserSemanticRulesTimeAndNumbers;

public class TimeFrequency extends Numbers {

	public TimeFrequency() {
		this(true);
	}
	public TimeFrequency(boolean generalize) {
		super(generalize,"answer.number-in-period");
	}
	
	@Override
	public List<NE> extractNamedEntitiesFromText(String text,PreprocessingType type) throws Exception {
		List<NE> payloads = null;
		TimePeriodSearcher ts = new TimePeriodSearcher(getConfiguration(),type,text);
		Double num=ts.getTimesEachDay();
		if (num!=null) {
			int start=ts.getStart(),end=ts.getEnd();
			logger.info("Extracted "+num+" times per day from the answer '"+text+"'.");
			if (payloads==null) payloads=new ArrayList<NE>();
			payloads.add(new NE(firstNumVar.getName(),num,this.getClass().getName(),start,end,text.substring(start,end),this));
		}
		return payloads;
	}
}
