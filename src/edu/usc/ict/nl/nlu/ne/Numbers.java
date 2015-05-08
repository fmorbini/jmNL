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
	public List<NE> extractNamedEntitiesFromText(String text,String speechAct) throws Exception {
		List<NE> payloads = null;
		if (sas!=null) {
			boolean match=false;
			for(int i=0;i<sas.length;i++) {
				Matcher m=sas[i].matcher(speechAct);
				if (match=m.matches()) break;
			}
			if (match) {
				NumberSearcher ns = new NumberSearcher(text);
				boolean first=true;
				while(ns.possiblyContainingNumber()) {
					Double num=ns.getNextNumber();
					if (num!=null) {
						logger.info("Extracted number "+num+" from the answer '"+text+"'.");
						if (payloads==null) payloads=new ArrayList<NE>();
						if (first) {
							payloads.add(new NE(firstNumVar.getName(),num,firstNumVar.getName(),ns.getStart(),ns.getEnd(),text.substring(ns.getStart(), ns.getEnd())));
							first=false;
						}
						payloads.add(new NE(allNumVar.getName(),num,allNumVar.getName(),ns.getStart(),ns.getEnd(),text.substring(ns.getStart(), ns.getEnd())));
					}
				}
			}
		}
		return payloads;
	}
	
	public static void main(String[] args) throws Exception {
		NLU nlu=NLU.init("MXNLU");
		nlu.getConfiguration().setForcedNLUContentRoot("C:\\simcoach_svn\\trunk\\core\\NLModule\\resources\\characters\\common\\nlu");
		Numbers ne = new Numbers("test");
		ne.setConfiguration(nlu.getConfiguration());
		List<NE> x = ne.extractNamedEntitiesFromText("i want 18 and twenty four bananas with 4 more and thirty.", "test");
		System.out.println(x);
		System.out.println(BasicNE.createPayload(x));
	}
}
