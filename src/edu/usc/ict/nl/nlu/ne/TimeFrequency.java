package edu.usc.ict.nl.nlu.ne;

import java.util.ArrayList;
import java.util.List;

import edu.usc.ict.nl.nlu.ne.searchers.TimePeriodSearcher;

public class TimeFrequency extends Numbers {

	public TimeFrequency() {
		this(true);
	}
	public TimeFrequency(boolean generalize) {
		super(generalize,"answer.number-in-period");
	}
	
	@Override
	public List<NE> extractNamedEntitiesFromText(String text) throws Exception {
		List<NE> payloads = null;
		TimePeriodSearcher ts = new TimePeriodSearcher(getConfiguration(),text);
		Double num=ts.getTimesEachDay();
		if (num!=null) {
			logger.info("Extracted "+num+" times per day from the answer '"+text+"'.");
			if (payloads==null) payloads=new ArrayList<NE>();
			payloads.add(new NE(firstNumVar.getName(),num,this));
		}
		return payloads;
	}
}
