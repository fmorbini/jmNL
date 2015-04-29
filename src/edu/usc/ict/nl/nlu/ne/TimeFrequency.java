package edu.usc.ict.nl.nlu.ne;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.usc.ict.nl.nlu.BuildTrainingData;
import edu.usc.ict.nl.nlu.Token;

public class TimeFrequency extends Numbers {

	public TimeFrequency() {
	}

	@Override
	public Map<String, Object> extractPayloadFromText(String text,String speechAct) throws Exception {
		Map<String, Object> payloads = null;
		if (speechAct.equals("answer.number-in-period")) {
			List<Token> tokens = BuildTrainingData.tokenize(text);
			Double num=getTimesEachDay(tokens);
			if (num!=null) {
				logger.info("Extracted "+num+" times per day from the answer '"+text+"'.");
				if (payloads==null) payloads=new HashMap<String, Object>();
				payloads.put(firstNumVar.getName(), num);
			}
		}
		return payloads;
	}

}
