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

import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.config.NLGConfig;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.nlg.SpeechActWithProperties;
import edu.usc.ict.nl.nlg.echo.EchoNLG;
import edu.usc.ict.nl.util.StringUtils;

public class TemplatedNLG extends EchoNLG {

	private Map<String,Method> methodMap;
	private static final Pattern methodNamePattern=Pattern.compile("function([A-Z][\\S]*)");
	
	public TemplatedNLG(NLGConfig c) {
		this(c,true);
	}

	public TemplatedNLG(NLGConfig c, boolean loadData) {
		super(c,loadData);
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

	public Object functionGet(FunctionArguments args) {
		if (!args.simulate) return args.is.get(args.stringArg);
		else return null;
	}
	public Object functionGetSA(FunctionArguments args) {
		if (!args.simulate) {
			try {
				SpeechActWithProperties line=pickLineForSpeechAct(args.sessionId, args.stringArg, args.is, false);
				NLGEvent output=processPickedLine(line, args.sessionId,args.stringArg, args.is, false);
				return output.getName();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	@Override
	protected NLGEvent processPickedLine(SpeechActWithProperties line, Long sessionID,String sa, DialogueKBInterface is,boolean simulate) throws Exception {
		NLGEvent output=null;
		if (line!=null) {
			output=buildOutputEvent(null, sessionID, null);
			String text=line.getText();
			List<Function> functions = getFunctions(text);
			text=applyFunctions(output,text,functions,is,sa,simulate);
			output.setName(text);
		}
		return output;
	}


	private String applyFunctions(NLGEvent output,String text, List<Function> functions, DialogueKBInterface is, String sa,boolean simulate) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		if (functions!=null) {
			for(Function x:functions) x.reset();
			for(Function f:functions) {
				Method m=f.getMethod();
				if (m!=null) {
					String functionArgs=f.getArguments(text);
					Object obj=m.invoke(this,new FunctionArguments(output,is,functionArgs,sa,simulate));
					String replacement=getReplacement(obj);
					if (f.getRequiredOutput() && StringUtils.isEmptyString(replacement)) {
						logger.warn("function "+f+" returned null and requires non-null output. Cancelling entire text.");
						try {
							logger.warn("IS is: "+is.dumpKB());
						} catch (Exception e) {
							logger.error(e);
						}
						return null;
					} else {
						int start=f.getStart();
						int functionLength=f.getEnd()-start+1;
						int delta=replacement.length()-functionLength;
						text=text.substring(0, f.getStart())+replacement+text.substring(f.getEnd()+1, text.length());
						for(Function x:functions) x.updateIndexes(delta, start);
					}
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
					Number n=f.getNumber();
					if (n!=null) {
						if (n instanceof Long) return n.toString();
						else if (n instanceof Float) return Math.round((Float)n)+"";
					}
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
	 * extract the list of function and sort them by inclusion. that is, if f1 is included within the brackets of f2 then f1 must come before f2.
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
