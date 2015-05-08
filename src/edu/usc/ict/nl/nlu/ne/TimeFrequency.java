package edu.usc.ict.nl.nlu.ne;

import java.util.ArrayList;
import java.util.List;

public class TimeFrequency extends Numbers {

	public TimeFrequency() {
	}

	@Override
	public List<NE> extractNamedEntitiesFromText(String text,String speechAct) throws Exception {
		List<NE> payloads = null;
		if (speechAct.equals("answer.number-in-period")) {
			Double num=getTimesEachDay(text);
			if (num!=null) {
				logger.info("Extracted "+num+" times per day from the answer '"+text+"'.");
				if (payloads==null) payloads=new ArrayList<NE>();
				payloads.add(new NE(firstNumVar.getName(), num));
			}
		}
		return payloads;
	}

}
