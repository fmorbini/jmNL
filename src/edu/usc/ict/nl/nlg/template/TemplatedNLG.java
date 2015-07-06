package edu.usc.ict.nl.nlg.template;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.nlg.echo.EchoNLG;
import edu.usc.ict.nl.vhmsg.VHBridge;

public class TemplatedNLG extends EchoNLG {

	private Map<String,Method> methodMap;
	protected VHBridge vhBridge=null;
	private static final Pattern methodNamePattern=Pattern.compile("function([A-Z][\\S]*)");
	
	public TemplatedNLG(NLBusConfig c) {
		super(c);
		Class nlgClass=this.getClass();
		while(nlgClass!=null) {
			Method[] ms = nlgClass.getDeclaredMethods();
			if (ms!=null) {
				for(Method m:ms) {
					String name=m.getName();
					Matcher mName=methodNamePattern.matcher(name);
					if (mName.matches()) {
						if (methodMap==null) methodMap=new HashMap<String, Method>();
						String functionName=mName.group(1).toLowerCase();
						if (!methodMap.containsKey(functionName)) {
							methodMap.put(functionName, m);
							if (logger.isInfoEnabled()) logger.info("adding method "+m+" to templated NLG.");
						}
						else if (logger.isInfoEnabled()) logger.info("ignoring method "+m+" overloaded by child class.");
					}
				}
			}
			nlgClass=nlgClass.getSuperclass();
		}
	}

	public Object functionGet(DialogueKBInterface is,VHBridge vhBridge,String vname,boolean simulate) {
		if (!simulate) return is.get(vname);
		else return null;
	}
	public Object functionGetSA(DialogueKBInterface is,VHBridge vhBridge,String sa,boolean simulate) {
		if (!simulate) {
			try {
				return getTextForSpeechAct(sa, is, false);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	/**
	 * this function uses
	 */
	@Override
	protected String getTextForSpeechAct(String sa, DialogueKBInterface is,boolean simulate) throws Exception {
		String text=super.getTextForSpeechAct(sa, is, simulate);
		List<Function> functions = getFunctions(text);
		text=applyFunctions(text,functions,is,simulate);
		return text;
	}


	private String applyFunctions(String text, List<Function> functions, DialogueKBInterface is, boolean simulate) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (functions!=null) {
			for(Function x:functions) x.reset();
			for(Function f:functions) {
				Method m=f.getMethod();
				if (m!=null) {
					String functionArgs=f.getArguments(text);
					Object obj=m.invoke(this, is,vhBridge,functionArgs,simulate);
					String replacement=getReplacement(obj);
					int start=f.getStart();
					int functionLength=f.getEnd()-start+1;
					int delta=replacement.length()-functionLength;
					text=text.substring(0, f.getStart())+replacement+text.substring(f.getEnd()+1, text.length());
					for(Function x:functions) x.updateIndexes(delta, start);
				}
			}
		}
		return text;
	}


	private String getReplacement(Object obj) {
		String replacement="";
		if (obj!=null) {
			if (obj instanceof String) {
				replacement=(String)obj;
			} else if (obj instanceof DialogueKBFormula) {
				DialogueKBFormula f=(DialogueKBFormula)obj;
				if (f.isString()) {
					try {
						replacement=f.getStringValue(f.getName());
					} catch (Exception e) {
						replacement=f.toString();
					}
				} else if (f.isNumber()) {
					Float n=f.getNumber();
					if (n!=null) return Math.round(n)+"";
				}
			} else if (obj instanceof Collection) {
				int last=((List) obj).size()-1;
				int i=0;
				for(Object x:(List)obj) {
					if (i==last) {
						if (last>0) replacement+=" and "+getReplacement(x);
						else replacement=getReplacement(x);
					} else {
						if (i>0) replacement+=", "+getReplacement(x);
						else replacement=getReplacement(x);
					}
					i++;
				}
			} else {
				replacement=obj.toString();
			}
		}
		return replacement;
	}

	/**
	 * scan the input string character by character
	 * 	search for dollar signs that are not escaped with a backslash
	 *   for each dollar sign, find if it,s a valid function identifier: $functionName(...) or ${...}
	 *    if it is, search for the position of the closing bracket (consider escaped brackets)
	 *    else disregard
	 * extract the list of function and sert them by inclusion. that is, if f1 is included within the brackets of f2 then f1 must come before f2.
	 * this ordered list is the evaluation order.
	 * @param input
	 * @throws Exception 
	 */
	public List<Function> getFunctions(String input) throws Exception {
		List<Integer> dollarPositions=null;
		List<Function> ret=null;
		int i=0;
		boolean escape=false;
		char[] inputa=input.toCharArray();
		for(char c:inputa) {
			if (c=='$') {
				if (!escape) {
					if (dollarPositions==null) dollarPositions=new ArrayList<Integer>();
					dollarPositions.add(i);
				}
				escape=false;
			} else if (c=='\\') {
				escape=!escape;
			} else escape=false;
			i++;
		}
		if (dollarPositions!=null && !dollarPositions.isEmpty()) {
			for(Integer j:dollarPositions) {
				try {
					Function f=new Function(this,j,inputa);
					if (ret==null) ret=new ArrayList<Function>();
					ret.add(f);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		if (ret!=null) {
			Collections.sort(ret);
		}
		return ret;
	}
	
	public Map<String, Method> getMethodMap() {
		return methodMap;
	}
	
	public static void main(String[] args) throws Exception {
		/**
		 * some test cases
		 * this is a sequnece, of meaningless! words that i tell ${name} but if also $makeSentence($makeNP(i am \,\(\) great!!!),$makeVP($get(verb12),$makeNP(the ${name})))
		 */
		TemplatedNLG nlg = new TemplatedNLG(null);
		List<Function> r = nlg.getFunctions("$playCutScene(Player_Scored_Poor)");
		System.out.println(r);
	}
}
