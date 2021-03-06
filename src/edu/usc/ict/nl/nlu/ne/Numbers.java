package edu.usc.ict.nl.nlu.ne;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.nlu.Token.TokenTypes;
import edu.usc.ict.nl.nlu.ne.searchers.NumberSearcher;

public class Numbers extends BasicNE {
	
	public static final SpecialVar firstNumVar=new SpecialVar(null,TokenTypes.NUM.toString(),
			"Number extracted from a answer.number or answer.number-in-period speech acts.","0",Number.class);
	public static final SpecialVar allNumVar=new SpecialVar(null,"ALLNUMS",
			"Numbers extracted from a answer.number or answer.number-in-period speech acts.","0",List.class);
	
	public Numbers(String... sas) {
		this(true,sas);
	}
	public Numbers(boolean generalize,String... sas) {
		super(sas);
		addSpecialVarToRepository(firstNumVar);
		addSpecialVarToRepository(allNumVar);
		this.generalize=generalize;
	}
	
	
	@Override
	public List<NE> extractNamedEntitiesFromText(String text,PreprocessingType type) throws Exception {
		List<NE> payloads = null;
		NumberSearcher ns = new NumberSearcher(getConfiguration(), type,text);
		boolean first=true;
		NE ne=null;
		while(ns.possiblyContainingNumber()) {
			Double num=ns.getNextNumber();
			if (num!=null) {
				logger.info("Extracted number "+num+" from the answer '"+text+"'.");
				if (payloads==null) payloads=new ArrayList<NE>();
				if (first) {
					payloads.add(ne=new NE(firstNumVar.getName(),num,firstNumVar.getName(),ns.getStart(),ns.getEnd(),text.substring(ns.getStart(), ns.getEnd()),this));
					first=false;
				}
				ne.addVariable(allNumVar.getName(), num);
			}
		}
		return payloads;
	}
	
	public static void main(String[] args) throws Exception {
		NLU nlu=NLU.init("MXNLU");
		nlu.getConfiguration().setForcedNLUContentRoot("C:\\simcoach_svn\\trunk\\core\\NLModule\\resources\\characters\\common\\nlu");
		Numbers ne = new Numbers("test");
		ne.setConfiguration(nlu.getConfiguration());
		List<NE> x = ne.extractNamedEntitiesFromText("i want 18 and twenty four bananas with 4 more and thirty.",PreprocessingType.RUN);
		System.out.println(x);
		System.out.println(BasicNE.createPayload(x));
	}
}
