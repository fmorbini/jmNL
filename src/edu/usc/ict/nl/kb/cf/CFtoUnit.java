package edu.usc.ict.nl.kb.cf;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;

public class CFtoUnit implements CustomFunctionInterface {

	public static enum UNIT {milliseconds,seconds,hours,minutes,days,months,years};
	public static final Map<UNIT,Double> unitReferenceFactors=new HashMap<UNIT, Double>();
	public static final Map<UNIT,Integer> unitgroups=new HashMap<UNIT, Integer>();
	static {
		unitReferenceFactors.put(UNIT.milliseconds, 0.001d);
		unitReferenceFactors.put(UNIT.seconds, 1d);
		unitReferenceFactors.put(UNIT.hours, 3600d);
		unitReferenceFactors.put(UNIT.minutes, 60d);
		unitReferenceFactors.put(UNIT.days, 86400d);
		unitReferenceFactors.put(UNIT.months, 2592000d);
		unitReferenceFactors.put(UNIT.years, 31536000d);
	}
	static {
		unitgroups.put(UNIT.milliseconds,1);
		unitgroups.put(UNIT.seconds,1);
		unitgroups.put(UNIT.hours,1);
		unitgroups.put(UNIT.minutes,1);
		unitgroups.put(UNIT.days,1);
		unitgroups.put(UNIT.months,1);
		unitgroups.put(UNIT.years,1);
	}
	private static final String name="toUnit".toLowerCase();

	@Override
	public String getName() {return name;}

	@Override
	public boolean checkArguments(Collection<DialogueKBFormula> args) {
		return (args!=null && args.size()==3);
	}

	@Override
	public Number eval(DialogueKBFormula f, DialogueKBInterface is,boolean forSimplification,EvalContext context) throws Exception {
		if (forSimplification) return null;
		DialogueKBFormula arg1 = (DialogueKBFormula) f.getArg(1);
		DialogueKBFormula arg2 = (DialogueKBFormula) f.getArg(2);
		DialogueKBFormula arg3 = (DialogueKBFormula) f.getArg(3);
		Object var=is.evaluate(arg1,context);
		Object afromUnit=is.evaluate(arg2,context);
		Object atoUnit=is.evaluate(arg3,context);
		if (var!=null && var instanceof Number) {
			if (afromUnit!=null && afromUnit instanceof String) {
				UNIT fromUnit=UNIT.valueOf(DialogueKBFormula.getStringValue((String) afromUnit));
				if (afromUnit!=null && afromUnit instanceof String) {
					UNIT toUnit=UNIT.valueOf(DialogueKBFormula.getStringValue((String) atoUnit));
					Double factor=conversionFactor(fromUnit, toUnit);
					if (factor!=null) {
						if (var instanceof Integer) {
							return ((Integer)var)*factor;
						} else if (var instanceof Float) {
							return ((Float)var)*factor;
						} else if (var instanceof Double) {
							return ((Double)var)*factor;
						}
					}
				}
			}
		}
		return null;
	}

	private Double conversionFactor(UNIT from,UNIT to) {
		if (unitgroups.get(from)==unitgroups.get(to)) {
			return unitReferenceFactors.get(from)/unitReferenceFactors.get(to);
		}
		return null;
	}

	@Override
	public boolean test() throws Exception {
		TestRewardDM dm=new TestRewardDM(NLBusConfig.WIN_EXE_CONFIG);
		DialogueKB is=dm.getInformationState();
		is.store(DialogueOperatorEffect.parse("assign(a,currentTime)"), ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		Number r=eval(DialogueKBFormula.parse(getName()+"(a,'milliseconds','years')"),is,false,null);
		int year = Calendar.getInstance().get(Calendar.YEAR);
		if (r==null || (Math.floor((Double)r))!=(year-1970)) return false;
		r=eval(DialogueKBFormula.parse(getName()+"(a,'milliseconds','hours')"),is,false,null);
		double h=Math.floor(System.currentTimeMillis()/3600000f);
		if (r==null || (Math.floor((Double)r))!=h) return false;
		return true;
	}
	
	public static void main(String[] args) throws Exception {
		CFtoUnit f = new CFtoUnit();
		if (!f.test()) throw new Exception("failed test");
	}
}
