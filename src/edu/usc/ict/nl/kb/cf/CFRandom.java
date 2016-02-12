package edu.usc.ict.nl.kb.cf;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Collection;

import edu.usc.ict.nl.config.DMConfig;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.utils.FloatAndLongUtils;

public class CFRandom implements CustomFunctionInterface {

	private static final String name="random".toLowerCase();
	public static SecureRandom rng=new SecureRandom(ByteBuffer.allocate(Long.SIZE/Byte.SIZE).putLong(System.currentTimeMillis()).array());

	@Override
	public String getName() {return name;}

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args!=null && args.size()==1);
	}

	@Override
	public Integer eval(DialogueKBFormula f, DialogueKBInterface is,boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		else {
			DialogueKBFormula arg = (DialogueKBFormula) f.getFirstChild();
			Object result=is.evaluate(arg,context);
			if (result instanceof Number) {
				Integer n=((Number)result).intValue();
				if (n>0) return rng.nextInt(n);
			}
		}
		return null;
	}

	@Override
	public boolean test() throws Exception {
		TestRewardDM dm=new TestRewardDM(DMConfig.WIN_EXE_CONFIG);
		DialogueKB is=dm.getInformationState();
		is.store(DialogueOperatorEffect.parse("assign(a,11)"), ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		DialogueKBFormula f1=DialogueKBFormula.parse(getName()+"(1)");
		DialogueKBFormula f2=DialogueKBFormula.parse(getName()+"(a)");
		DialogueKBFormula f3=DialogueKBFormula.parse(getName()+"(+(a,10))");
		float avg1=0,avg2=0,avg3=0;
		int tot=1000;
		for(int i=0;i<tot;i++) {
			Number r=eval(f1,dm.getInformationState(),false,null);
			Float v=FloatAndLongUtils.numberToFloat(r);
			avg1+=v;
			if (!(v<1 && v>=0)) return false;
			r=eval(f2,dm.getInformationState(),false,null);
			v=FloatAndLongUtils.numberToFloat(r);
			avg2+=v;
			if (!(v<11 && v>=0)) return false;
			r=eval(f3,dm.getInformationState(),false,null);
			v=FloatAndLongUtils.numberToFloat(r);
			avg3+=v;
			if (!(v<21 && v>=0)) return false;
		}
		if (avg1!=0) return false;
		avg2/=tot;
		if (!(avg2<6 && avg2>4)) return false;
		avg3/=tot;
		if (!(avg3<11 && avg3>9)) return false;
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFRandom f = new CFRandom();
		if (!f.test()) throw new Exception("failed test");
	}
}
