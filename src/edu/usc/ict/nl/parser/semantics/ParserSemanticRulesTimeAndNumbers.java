package edu.usc.ict.nl.parser.semantics;

import edu.usc.ict.nl.parser.SemanticRulesContainer;

public class ParserSemanticRulesTimeAndNumbers extends SemanticRulesContainer {
	
	public static final Double numSecondsInMonth=30.0*24.0*60.0*60.0;
	public static final Double numSecondsInDay=24.0*60.0*60.0;

	// functions available in the semantic rules
	public Object whileParsing(Object[] args) {
		if ((args.length<2) || (args.length>3)) return null;
		if ((rootEntity==args[0]) || ((rootEntity!=null) && rootEntity.equals(args[0]))) return args[1];
		else return (args.length<3)?null:args[2];
	}
	public Object secondsInYear(Object[] args) {
		return 12*(Double)secondsInMonth(args);
	}
	public Object secondsInMonth(Object[] args) {
		return numSecondsInMonth; 
	}
	public Object secondsInWeek(Object[] args) {
		return 7*(Double)secondsInDay(args);
	}
	public Object secondsInDay(Object[] args) {
		return numSecondsInDay; 
	}
	public Object secondsInDecade(Object[] args) {
		return 10*(Double)secondsInYear(args);
	}
	public Object toDouble(Object[] args) {
		return Double.parseDouble((String) args[0]);
	}
	public Object nothing(Object[] args) {
		return args[0];
	}
	public Object avg(Object[] args) throws Exception {
		Double res=0.0;
		for(Object arg:args) {
			if (arg instanceof Integer) res+=(Integer)arg;
			else if (arg instanceof Long) res+=(Long)arg;
			else if (arg instanceof Double) res+=(Double)arg;
			else if (arg instanceof Float) res+=(Float)arg;
			else throw new Exception("Error summing: "+arg);
		}
		return res/(double)args.length;
	}
	public Object sum(Object[] args) throws Exception {
		Double res=0.0;
		for(Object arg:args) {
			if (arg instanceof Integer) res+=(Integer)arg;
			else if (arg instanceof Long) res+=(Long)arg;
			else if (arg instanceof Double) res+=(Double)arg;
			else if (arg instanceof Float) res+=(Float)arg;
			else throw new Exception("Error summing: "+arg);
		}
		return res;
	}
	public Object asDouble(Object[] args) throws Exception {
		Double res=null;
		if (args.length==1) {
			Object arg=args[0];
			if (arg instanceof Integer) res=new Double((Integer)arg);
			else if (arg instanceof Long) res=new Double((Long)arg);
			else if (arg instanceof Double) res=(Double)arg;
			else if (arg instanceof Float) res=new Double((Float)arg);
			else throw new Exception("Error converting: "+arg+" to double.");
		}
		return res;
	}
	public Object mul(Object[] args) throws Exception {
		Double res=1.0;
		for(Object arg:args) {
			if (arg instanceof Integer) res*=(Integer)arg;
			else if (arg instanceof Long) res*=(Long)arg;
			else if (arg instanceof Double) res*=(Double)arg;
			else if (arg instanceof Float) res*=(Float)arg;
			else throw new Exception("Error multiplying: "+arg);
		}
		return res;
	}
	public Object div(Object[] args) throws Exception {
		Double res=null;
		if (args.length==2) {
			Double num=(Double) asDouble(new Object[]{args[0]});
			Double den=(Double) asDouble(new Object[]{args[1]});
			if (den!=0) {
				res=num/den;
			} else throw new Exception("Error division by 0.");
		}
		return res;
	}
	public Object round(Object[] args) throws Exception {
		Object arg=args[0];
		if (arg instanceof Integer) return arg;
		else if (arg instanceof Long) return arg;
		else if (arg instanceof Double) return Math.round((Double) args[0]);
		else if (arg instanceof Float) return Math.round((Float) args[0]);
		else throw new Exception("Error rounding input: "+arg);
	}

}
