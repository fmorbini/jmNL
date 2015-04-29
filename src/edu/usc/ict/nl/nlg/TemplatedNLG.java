package edu.usc.ict.nl.nlg;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.vhmsg.VHBridge;

public class TemplatedNLG extends VRexpressBasicNLG {

	private Map<String,Method> methodMap;
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


	private static Object functionGet(DialogueKBInterface is,VHBridge vhBridge,String vname,boolean simulate) {
		if (!simulate) return is.get(vname);
		else return null;
	}
	
	private static final Pattern functionCall=Pattern.compile("(\\$([\\w]+)\\(([\\w]+)\\))|(\\$?\\{([\\w]+)\\})");
	/**
	 * this function uses
	 */
	@Override
	protected String getTextForSpeechAct(String sa, DialogueKBInterface is,boolean simulate) throws Exception {
		String text=super.getTextForSpeechAct(sa, is, simulate);
		Matcher mt=functionCall.matcher(text);
		while(mt.find()) {
			String functionName=mt.group(2);
			if (StringUtils.isEmptyString(functionName)) functionName="get";
			String functionArgs=mt.group(3);
			if (StringUtils.isEmptyString(functionArgs)) functionArgs=mt.group(5);
			Method f=null;
			if (methodMap!=null && (f=methodMap.get(functionName.toLowerCase()))!=null) {
				Object obj=f.invoke(this, is,vhBridge,functionArgs,simulate);
				String replacement=getReplacement(obj);
				text=text.substring(0, mt.start())+replacement+text.substring(mt.end(), text.length());
				mt=functionCall.matcher(text);
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
				if (f.isString())
					try {
						replacement=f.getStringValue(f.getName());
					} catch (Exception e) {
						replacement=f.toString();
					}
			} else if (obj instanceof List) {
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
			}
		}
		return replacement;
	}
}
