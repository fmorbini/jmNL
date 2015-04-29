package edu.usc.ict.nl.nlu.directablechar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.StringUtils;

public class Literal {
	public static final Pattern pp=Pattern.compile("^[\\s]*(([^(-]+)(-[^\\(]*)?)\\((.*)\\)[\\s]*$");
	private String p,justP;
	private List<String> args;
	Float w=null;
	public Literal() {
	}
	public Literal(String p,String... vars) throws Exception {
		Matcher m=pp.matcher(p+"()");
		if (m.matches()) {
			this.p=m.group(1);
			this.justP=m.group(2);
			this.args=new ArrayList<String>(Arrays.asList(vars));
		} else throw new Exception("error parsing predicate: "+p);
	}
	public static Literal parse(String ps) throws Exception {
		if(!StringUtils.isEmptyString(ps)) {
			Matcher m=pp.matcher(ps);
			if (m.matches()) {
				Literal l=new Literal();
				String pname=m.group(1);
				List<String> args=Arrays.asList(m.group(4).split(","));
				if (StringUtils.isEmptyString(pname)) throw new Exception("failed parsing of: "+ps);
				l.p=pname;
				l.justP=m.group(2);
				l.args=args;
				return l;
			} else throw new Exception("failed parsing of: "+ps);
		} else throw new Exception("failed parsing of: "+ps);
	}
	@Override
	public String toString() {
		String argss="";
		try {
			argss = (args!=null && !args.isEmpty())?FunctionalLibrary.printCollection(args, "", "", ","):"";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return p+"("+argss+((w!=null)?" :"+w:"")+")";
	}
	
	public String getP() {
		return p;
	}
	public String getJustP() {
		return justP;
	}
	public List<String> getArgs() {
		return args;
	}
	public void setWeight(Float w) {
		this.w=w;
	}
	public String toLispString() {
		String argss="";
		try {
			argss = (args!=null && !args.isEmpty())?FunctionalLibrary.printCollection(args, "", "", " "):"";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "("+p+" "+argss+((w!=null)?" :"+w:"")+")";
	}
}
