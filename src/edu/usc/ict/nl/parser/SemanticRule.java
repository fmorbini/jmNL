package edu.usc.ict.nl.parser;

import java.lang.reflect.Method;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;

public class SemanticRule {

	private String rule;
	private Pair<Method,Object> method;
	private Object[] arguments;
	private Object value=null;
	public Object getValue() {
		return value;
	}
	public String getRule() {
		return rule;
	}
	private final Pattern stringPattern=Pattern.compile("^\"(.*)\"$");

	public SemanticRule(String rule,SemanticRulesContainer semanticRulesContainer) throws Exception {
		this.rule=rule;
		Pattern p=Pattern.compile("^[\\s]*([\\w]+)(\\((.*)\\))*[\\s]*$");
		Matcher m=p.matcher(rule);
		if (m.matches()) {
			int numMatches=m.groupCount();
			if (numMatches>=1) {
				String part=m.group(1);
				Integer pos=null;
				try {
					pos=Integer.parseInt(part);
					if (!StringUtils.isEmptyString(m.group(2))) throw new Exception("error: argument given for number method: "+rule);
					else {
						try {
							method=new Pair<Method, Object>(semanticRulesContainer.getClass().getDeclaredMethod("nothing",new Class[]{Object[].class}),semanticRulesContainer);
							arguments=new Object[]{pos};
						} catch (Exception e) {e.printStackTrace(); throw new Exception();}
					}
				} catch (NumberFormatException e) {
					method=new Pair<Method, Object>(semanticRulesContainer.getClass().getMethod(part,new Class[]{Object[].class}),semanticRulesContainer);
					String[] args=extractArguments(m.group(3));
					arguments=new Object[args.length];
					int i=0;
					for (String arg:args) {
						Matcher ms=stringPattern.matcher(arg);
						if (ms.matches()) arguments[i]=new String(ms.group(1));
						else {
							try {
								pos=Integer.parseInt(arg);
								arguments[i]=pos;
							} catch (NumberFormatException e1) {
								arguments[i]=new SemanticRule(arg, semanticRulesContainer);
							}
						}
						i++;
					}
				}
			} else throw new Exception("error 1 in rule: "+rule);
		}
	}
	
	public static int diffOfOpenAndClosedBrackets(String s) {
		int open=StringUtils.countOccurrencesOf(s,'(');
		int close=StringUtils.countOccurrencesOf(s,')');
		return open-close;
	}

	public static String[] extractArguments(String stringArg) {
		if (stringArg==null) return new String[0];
		else {
			String[] args=stringArg.split(",");
			Vector<String> finalArgs=new Vector<String>();
			int diff=0;
			String tmp=null;
			for(String arg:args) {
				diff+=diffOfOpenAndClosedBrackets(arg);
				if (diff!=0) {
					if (tmp==null)
						tmp=arg;
					else
						tmp+=","+arg;
				} else {
					if (tmp!=null) tmp+=","+arg;
					else tmp=arg;
					finalArgs.add(tmp);
					tmp=null;
				}
			}
			return finalArgs.toArray(new String[finalArgs.size()]);
		}
	}
	public Object apply(Object... args) throws Exception {
		Object[] applyArguments=new Object[arguments.length];
		int i;
		for(i=0;i<arguments.length;i++) {
			Object arg=arguments[i];			
			if (arg instanceof SemanticRule) {
				applyArguments[i]=((SemanticRule) arg).apply(args);
			} else if (arg instanceof Integer) {
				applyArguments[i]=args[(Integer) arg-1];
			} else if (arg instanceof String) {
				applyArguments[i]=arg;
			} else {
				throw new Exception("error in argument "+arg+".");
			}
		}
		value=method.getFirst().invoke(method.getSecond(), new Object[]{applyArguments});
		return value;
	}
}
