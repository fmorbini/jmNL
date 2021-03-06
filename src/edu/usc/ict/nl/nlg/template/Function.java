package edu.usc.ict.nl.nlg.template;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;

import edu.usc.ict.nl.util.StringUtils;

public class Function implements Comparable<Function> {
	public enum BT {CURLY,ROUND};

	private Method m=null;
	private int start=-1,end=-1,startBracket=-1;
	private BT bracketType=BT.ROUND;
	private String functionName;
	private int beforeDelta=0,withinDelta=0;
	private boolean requiredOutput=false;
	private Map<String, Method> methodMap;
	
	@Override
	public int compareTo(Function o) {
		if (o.getStart()>=getStart() && o.getEnd()<=getEnd()) return +1; //this function is less than o.
		else if (getStart()>=o.getStart() && getEnd()<=o.getEnd()) return -1;
		else return 0; // irrelevant how they are sorted.
	}
	
	/**
	 * @param methodMap
	 * @throws Exception 
	 */
	Function(Map<String, Method> methodMap,int start,char[] inputa) throws Exception {
		this.methodMap = methodMap;
		this.start=start;
		searchStartBracket(inputa);
		searchEndBracket(inputa);
	}

	private void searchStartBracket(char[] inputa) throws Exception {
		assert(start>=0);
		boolean escape=false;
		for(int i=start+1;i<inputa.length;i++) {
			char c=inputa[i];
			if (c=='('||c=='{') {
				if (!escape) {
					startBracket=i;
					bracketType=(c=='{')?BT.CURLY:BT.ROUND;
					char[] functionNamea=Arrays.copyOfRange(inputa, start+1, startBracket);
					boolean foundf=setFunctionName(new String(functionNamea));
					if (!foundf) throw new Exception("failed to find method for function: "+functionName);
					else return;
				}
				escape=false;
			} else if (c=='\\') {
				escape=!escape;
			} else escape=false;
		}
		throw new Exception("failed to find a starting bracket for dollar at position "+start+" in string '"+new String(inputa)+"'.");
	}

	private void searchEndBracket(char[] inputa) throws Exception {
		assert(start>=0 && startBracket>0);
		boolean escape=false;
		int levels=1;
		for(int i=startBracket+1;i<inputa.length;i++) {
			char c=inputa[i];
			if (c=='('||c=='{') {
				if (!escape) levels++;
				escape=false;
			} else if (c=='\\') {
				escape=!escape;
			} else if (c==')'||c=='}') {
				if (!escape) {
					levels--;
					if (levels==0) {
						if (c==')' && bracketType==BT.CURLY) throw new Exception("Errors in bracket types. Expecting a curly but found a round. Position: "+i+" from start bracket "+startBracket);
						else if (c=='}' && bracketType==BT.ROUND) throw new Exception("Errors in bracket types. Expecting a round but found a curly. Position: "+i+" from start bracket "+startBracket);
						else {
							end=i;
							return;
						}
					}
				}
				escape=false;
			} else escape=false;
		}
		throw new Exception("failed to find a closing bracket for dollar at position "+start+" in string '"+new String(inputa)+"'.");
	}

	private boolean setFunctionName(String name) {
		if (StringUtils.isEmptyString(name)) {
			functionName="get";
		} else {
			if (name.endsWith("!")) {
				setRequiredOutput(true);
				name=name.substring(0, name.length()-1);
			}
			functionName=name;
		}
		return findMethod();
	}

	public void setRequiredOutput(boolean requiredOutput) {
		this.requiredOutput = requiredOutput;
	}
	public boolean getRequiredOutput() {return requiredOutput;}
	
	private boolean findMethod() {
		if (this.methodMap!=null) {
			m=methodMap.get(functionName.toLowerCase());
			return m!=null;
		}
		return false;
	}
	
	@Override
	public String toString() {
		int s=getStart(), e=getEnd();
		if (m!=null) return m.getName()+"<"+s+","+e+">";
		else return "<"+s+","+e+">";
	}

	/**
	 * 
	 * @param delta: the change in string length over the original string used to compute the indexes start/startBracket and end. 
	 * @param ds: the current (before change to the string) position at which the change will be applied
	 */
	public void updateIndexes(int delta,int ds) {
		if (ds>getStartBracket() && ds<getEnd()) {
			withinDelta+=delta;
		} else if (ds<getStart()) {
			beforeDelta+=delta;
		}
	}
	
	public Method getMethod() {
		return m;
	}

	public String getArguments(String text) {
		return text.substring(getStartBracket()+1, getEnd());
	}

	public int getStartBracket() {
		return startBracket+beforeDelta;
	}
	
	public int getStart() {
		return start+beforeDelta;
	}

	public int getEnd() {
		return end+beforeDelta+withinDelta;
	}
	
	/**
	 * resets the delta to get the exact start/end indexes based on the application of other functions.
	 */
	public void reset() {
		beforeDelta=withinDelta=0;
	}
}