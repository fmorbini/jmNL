package edu.usc.ict.nl.nlu.ne;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.BuildTrainingData;
import edu.usc.ict.nl.nlu.Token;

public class Numbers extends BasicNE {
	
	private Pattern[] sas=null;
	
	public static final SpecialVar firstNumVar=new SpecialVar(null,"NUM",
			"Number extracted from a answer.number or answer.number-in-period speech acts.","0",Number.class);
	public static final SpecialVar allNumVar=new SpecialVar(null,"ALLNUMS",
			"Numbers extracted from a answer.number or answer.number-in-period speech acts.","0",List.class);
	public Numbers(String... sas) {
		addSpecialVarToRepository(firstNumVar);
		addSpecialVarToRepository(allNumVar);
		this.sas=new Pattern[sas.length];
		for (int i=0;i<sas.length;i++) {
			this.sas[i]=Pattern.compile(sas[i]);
		}
	}
	
	@Override
	public Map<String, Object> extractPayloadFromText(String text,String speechAct) throws Exception {
		Map<String, Object> payloads = null;
		if (sas!=null) {
			boolean match=false;
			for(int i=0;i<sas.length;i++) {
				Matcher m=sas[i].matcher(speechAct);
				if (match=m.matches()) break;
			}
			if (match) {
				NumberSearcher ns = new NumberSearcher(text);
				List<Double> allNumbers=null;
				while(ns.possiblyContainingNumber()) {
					Double num=ns.getNextNumber();
					if (num!=null) {
						logger.info("Extracted number "+num+" from the answer '"+text+"'.");
						if (payloads==null) payloads=new HashMap<String, Object>();
						if (!payloads.containsKey(firstNumVar.getName())) {
							payloads.put(firstNumVar.getName(), num);
							payloads.put(allNumVar.getName(), allNumbers=new ArrayList<Double>());
							allNumbers.add(num);
						} else {
							allNumbers.add(num);
						}
					}
				}
			}
		}
		return payloads;
	}
	
	public static void main(String[] args) throws Exception {
		NLU nlu=NLU.init("MXNLU");
		nlu.getConfiguration().setForcedNLUContentRoot("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\resources\\characters\\common\\nlu");
		Numbers ne = new Numbers("test");
		ne.setConfiguration(nlu.getConfiguration());
		Map<String, Object> x = ne.extractPayloadFromText("i want 18 and twenty four bananas with 4 more and thirty.", "test");
		System.out.println(x);
	}
}
