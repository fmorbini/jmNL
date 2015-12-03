package edu.usc.ict.nl.kb.cf;

import java.util.Collection;

import edu.usc.ict.nl.config.DMConfig;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;

public class CFcurrentTime implements CustomFunctionInterface {

	private static final String name="currentTime".toLowerCase();

	@Override
	public String getName() {return name;}

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args==null || (args!=null && args.size()==0));
	}

	@Override
	public Long eval(DialogueKBFormula f, DialogueKBInterface is,boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		return System.currentTimeMillis();
	}


	@Override
	public boolean test() throws Exception {
		TestRewardDM dm=new TestRewardDM(DMConfig.WIN_EXE_CONFIG);
		DialogueKB is=dm.getInformationState();
		Long r=eval(DialogueKBFormula.parse(getName()),is,false,null);
		long tt=System.currentTimeMillis();
		if (tt-r<1000) return true;
		return false;
	}
	
	public static void main(String[] args) throws Exception {
		CFcurrentTime f = new CFcurrentTime();
		if (!f.test()) throw new Exception("failed test");
	}
}
