package edu.usc.ict.nl.kb.cf;

import java.net.URL;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.utils.LogConfig;

public class CFPrint implements CustomFunctionInterface {

	private static final Logger logger = Logger.getLogger(CFPrint.class.getName());
	static {
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );
	}
	
	private static final String name="print".toLowerCase();

	@Override
	public String getName() {return name;}

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args!=null && args.size()==1);
	}

	@Override
	public Object eval(DialogueKBFormula f, DialogueKBInterface is,boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		DialogueKBFormula arg = (DialogueKBFormula) f.getArg(1);
		if (arg!=null) {
			Object ea=is.evaluate(arg,context);
			if (ea!=null)
				logger.info("###("+is+")> "+arg+"="+ea+"("+ea.getClass().getCanonicalName()+").");
			else
				logger.info("###("+is+")> "+arg+"="+ea+".");
			return ea;
		}
		return null;
	}

	@Override
	public boolean test() throws Exception {
		TestRewardDM dm=new TestRewardDM(NLBusConfig.WIN_EXE_CONFIG);
		DialogueKB is=dm.getInformationState();
		is.store(DialogueOperatorEffect.parse("assign(a,print(a))"), ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		return true;
	}

	public static void main(String[] args) throws Exception {
		CFPrint f = new CFPrint();
		if (!f.test()) throw new Exception("failed test");
	}
}
