package edu.usc.ict.nl.dm.reward.model;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.nlg.template.Function;
import edu.usc.ict.nl.nlg.template.FunctionArguments;
import edu.usc.ict.nl.nlg.template.TemplatedNLG;
import edu.usc.ict.nl.util.StringUtils;

public class Template {
	
	private String name;
	private String content;
	private List<Function> functions;
	private static final Map<String, Method> mm=new HashMap<>();
	static {
		try {
			mm.put("get", Template.class.getMethod("get", String.class,Map.class,int.class));
		} catch (NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
	}
	public static String get(String vname,Map<String,List<String>> values,int vPos) {
		if (values!=null) {
			List<String> vs = values.get(vname);
			if (vs!=null && vs.size()>vPos) return vs.get(vPos);
		}
		return null;
	}

	public Template(String tname, String content) throws Exception {
		this.name=tname;
		this.content=content;
		functions=TemplatedNLG.getFunctions(content, mm);
	}
	
	public String getName() {
		return name;
	}
	public String getContent() {
		return content;
	}

	public String applySubstitutions(Map<String, List<String>> vars,int vPos) throws Exception {
		String text=getContent();
		if (functions!=null) {
			for(Function x:functions) x.reset();
			for(Function f:functions) {
				Method m=f.getMethod();
				if (m!=null) {
					String vName=f.getArguments(text);
					Object obj=m.invoke(this,vName,vars,vPos);
					String replacement=TemplatedNLG.getReplacement(obj);
					if (f.getRequiredOutput() && StringUtils.isEmptyString(replacement)) {
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

}
