package edu.usc.ict.nl.utils;

public class FloatAndLongUtils {
	public static Number sumFloatAndOrLong(java.lang.Number n1, java.lang.Number n2) {
		Long n1l=numberToLong(n1),n2l=numberToLong(n2); 
		if (n1l!=null && n2l!=null) return n1l+n2l;
		else return numberToFloat(n1)+numberToFloat(n2);
	}
	/**
	 * 
	 * @param n1
	 * @param n2
	 * @return n1-n2
	 */
	public static Number subFloatAndOrLong(java.lang.Number n1, java.lang.Number n2) {
		Long n1l=numberToLong(n1),n2l=numberToLong(n2); 
		if (n1l!=null && n2l!=null) return n1l-n2l;
		else return numberToFloat(n1)-numberToFloat(n2);
	}

	/**
	 * 
	 * @param n1
	 * @param n2
	 * @return n1/n2
	 */
	public static Number divFloatAndOrLong(java.lang.Number n1, java.lang.Number n2) {
		Long n1l=numberToLong(n1),n2l=numberToLong(n2); 
		if (n1l!=null && n2l!=null) return n1l/n2l;
		else return numberToFloat(n1)/numberToFloat(n2);
	}

	public static Number mulFloatAndOrLong(java.lang.Number n1, java.lang.Number n2) {
		Long n1l=numberToLong(n1),n2l=numberToLong(n2); 
		if (n1l!=null && n2l!=null) return n1l*n2l;
		else return numberToFloat(n1)*numberToFloat(n2);
	}
	
	public static Float numberToFloat(Number n) {
		if (n!=null) {
			if (n instanceof Integer) return ((Integer)n).floatValue();
			else if (n instanceof Float) return ((Float)n);
			else if (n instanceof Double) return ((Double)n).floatValue();
			else if (n instanceof Short) return ((Short)n).floatValue();
			else if (n instanceof Long) return ((Long)n).floatValue();
			else return (Float)n;
		}
		return null;
	}
	public static Long numberToLong(Number n) {
		if (n!=null && !(n instanceof Float)) {
			if (n instanceof Integer) return ((Integer)n).longValue();
			else if (n instanceof Short) return ((Short)n).longValue();
			else if (n instanceof Long) return ((Long)n).longValue();
		}
		return null;
	}

}
