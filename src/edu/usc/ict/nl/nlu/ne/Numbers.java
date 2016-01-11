package edu.usc.ict.nl.nlu.ne;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.nlu.Token.TokenTypes;
import edu.usc.ict.nl.nlu.ne.searchers.NumberSearcher;

public class Numbers extends BasicNE {
	
	private Pattern[] sas=null;
	
	public static final SpecialVar firstNumVar=new SpecialVar(null,TokenTypes.NUM.toString(),
			"Number extracted from a answer.number or answer.number-in-period speech acts.","0",Number.class);
	public static final SpecialVar allNumVar=new SpecialVar(null,"ALLNUMS",
			"Numbers extracted from a answer.number or answer.number-in-period speech acts.","0",List.class);
	
	public Numbers(String... sas) {
		this(true,sas);
	}
	public Numbers(boolean generalize,String... sas) {
		addSpecialVarToRepository(firstNumVar);
		addSpecialVarToRepository(allNumVar);
		if (sas!=null) {
			this.sas=new Pattern[sas.length];
			for (int i=0;i<sas.length;i++) {
				this.sas[i]=Pattern.compile(sas[i]);
			}
		}
		this.generalize=generalize;
	}
	
	@Override
	public boolean isNEAvailableForSpeechAct(NE ne, String speechAct) {
		boolean match=true;
		if (speechAct!=null && sas!=null) {
			match=false;
			for(int i=0;i<sas.length;i++) {
				Matcher m=sas[i].matcher(speechAct);
				if (match=m.matches()) break;
			}
		}
		return match;
	}
	
	@Override
	public List<NE> extractNamedEntitiesFromText(String text) throws Exception {
		List<NE> payloads = null;
		NumberSearcher ns = new NumberSearcher(getConfiguration(), text);
		boolean first=true;
		while(ns.possiblyContainingNumber()) {
			Double num=ns.getNextNumber();
			if (num!=null) {
				logger.info("Extracted number "+num+" from the answer '"+text+"'.");
				if (payloads==null) payloads=new ArrayList<NE>();
				if (first) {
					payloads.add(new NE(firstNumVar.getName(),num,firstNumVar.getName(),ns.getStart(),ns.getEnd(),text.substring(ns.getStart(), ns.getEnd()),this));
					first=false;
				}
				payloads.add(new NE(allNumVar.getName(),num,allNumVar.getName(),ns.getStart(),ns.getEnd(),text.substring(ns.getStart(), ns.getEnd()),this));
			}
		}
		return payloads;
	}
	
	public static void main(String[] args) throws Exception {
		NLU nlu=NLU.init("MXNLU");
		nlu.getConfiguration().setForcedNLUContentRoot("C:\\simcoach_svn\\trunk\\core\\NLModule\\resources\\characters\\common\\nlu");
		Numbers ne = new Numbers("test");
		ne.setConfiguration(nlu.getConfiguration());
		List<NE> x = ne.extractNamedEntitiesFromText("i want 18 and twenty four bananas with 4 more and thirty.");
		System.out.println(x);
		System.out.println(BasicNE.createPayload(x));
	}
}
