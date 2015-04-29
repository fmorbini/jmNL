package edu.usc.ict.nl.nlu.directablechar.action;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.events.Characters;

import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.StringUtils;

public class Action implements Comparable<Action> {
	protected String name;
	protected List<String> arguments;
	public static Action create(String n,List<String> args) throws SecurityException, NoSuchMethodException, ClassNotFoundException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		Package p = Action.class.getPackage();
		StringBuilder sb = new StringBuilder(n.toLowerCase());
		sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
		try {
			Class cl=Class.forName(p.getName()+"."+sb.toString());
			Constructor constructor = cl.getConstructor(List.class);
			return (Action) constructor.newInstance(args);
		} catch (Exception e) {
			Action action=new Action();
			action.setName(n);
			action.setArguments(args);
			return action;
		}
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setArguments(List<String> arguments) {
		this.arguments = arguments;
	}
	
	@Override
	public String toString() {
		String args="";
		try {
			args = FunctionalLibrary.printCollection(arguments, "", "", ",");
			if (!StringUtils.isEmptyString(args)) args=","+args;
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return "ACTION("+name+args+")";
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isAction(String name) {
		return (getName()!=null && getName().equals(name));
	}
	
	public boolean allResolved() {
		if (arguments!=null) {
			for(String a:arguments) {
				if (!Character.isUpperCase(a.charAt(0))) {
					return false;
				}
			}
		}
		return true;
	}
	public int countResolved() {
		int ret=0;
		if (arguments!=null) {
			for(String a:arguments) {
				if (Character.isUpperCase(a.charAt(0)) && !a.equalsIgnoreCase("NULL")) {
					ret++;
				}
			}
		}
		return ret;
	}
	public float fractionResolved() {
		int n=countResolved();
		if (arguments==null || arguments.isEmpty()) return 1f;
		else return (float) n/(float) arguments.size();
	}
	
	public boolean good() {
		return false;
	}
	
	public Map<String, Object> getPayload() {
		Map<String,Object> ret=null;
		if (arguments!=null && !arguments.isEmpty()) {
			String base="ARG";
			for(int i=0;i<arguments.size();i++) {
				if (ret==null) ret=new HashMap<String, Object>();
				ret.put(base+i, "'"+arguments.get(i)+"'");
			}
		}
		return ret;
	}

	@Override
	public int compareTo(Action o) {
		return o.getName().compareTo(this.getName());
	}
	
}