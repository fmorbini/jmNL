package edu.usc.ict.nl.nlg.lf;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.utils.ExcelUtils;

public class Lexicon {
	public enum ARGTYPE {AGENT,THEME,PATIENT,STIMULUS,DESTINATION,EXPERIENCER,LOCATION,PREDICATE};
	private static final Pattern argsPattern=Pattern.compile("^[\\s]*ARG([0-9]+):[\\s]*([^\\s]+)[\\s]*$");
	public class Entry {
		String name;
		String type;
		Map<ARGTYPE,Integer> typeAndPosition;
		String surface;
		String length;

		public Entry(String n, String t, String a, String s) {
			this.name=n;
			this.type=t;
			this.surface=s;
			if (!StringUtils.isEmptyString(a)) {
				String[] args=a.split(",");
				for(String arg:args) {
					Matcher m=argsPattern.matcher(arg);
					if (m.matches()) {
						String at=m.group(2);
						Integer pos=Integer.parseInt(m.group(1));
						if (typeAndPosition==null) typeAndPosition=new HashMap<Lexicon.ARGTYPE, Integer>();
						typeAndPosition.put(ARGTYPE.valueOf(at.toUpperCase()), pos);
					}
				}
			}
		}

		public Integer getSubjectPosition() {
			if (typeAndPosition!=null) {
				Integer pos=typeAndPosition.get(ARGTYPE.AGENT);
				if (pos!=null) return pos;
				pos=typeAndPosition.get(ARGTYPE.PATIENT);
				if (pos!=null) return pos;
				pos=typeAndPosition.get(ARGTYPE.EXPERIENCER);
				return pos;
			}
			return null;
		}
		public ARGTYPE getTypeOfArgumentAtPosition(int pos) {
			if (typeAndPosition!=null) {
				for(ARGTYPE a:typeAndPosition.keySet()) {
					int ap=typeAndPosition.get(a);
					if (pos==ap) return a;
				}
			}
			return null;
		}

		public String getType() {
			return type;
		}
		public String getSurface() {
			return surface;
		}
		public String getName() {
			return name;
		}

		public boolean isPassive() {
			return false;
		}
	}
	private Map<String, Entry> lex=null;

	public Lexicon(String pFile) {
		Map<Integer, String> predicates = ExcelUtils.extractRowAndThisColumn(pFile, 0, 0);
		Map<Integer, String> types = ExcelUtils.extractRowAndThisColumn(pFile, 0, 1);
		Map<Integer, String> arguments = ExcelUtils.extractRowAndThisColumn(pFile, 0, 2);
		Map<Integer, String> surfaces = ExcelUtils.extractRowAndThisColumn(pFile, 0, 3);
		if (predicates!=null && types!=null && arguments!=null && surfaces!=null) {
			for(Integer row:predicates.keySet()) {
				addEntry(predicates.get(row),types.get(row),arguments.get(row),surfaces.get(row));
			}
		}
	}

	private static final Pattern pName=Pattern.compile("^[\\s]*([^\\s]+)_([0-9]+)[\\s]*$");
	private void addEntry(String pname, String type, String args,String surface) {
		if (lex==null) lex=new HashMap<String, Entry>();
		String name=normalize(pname);
		Entry e=new Entry(name,type,args,surface);
		lex.put(name, e);
	}

	private String normalize(String in) {
		Matcher m=pName.matcher(in);
		if (m.matches()) {
			return m.group(1).toLowerCase();
		} //else System.err.println("'"+in+"' doesn't match expected argument lexicon format.");
		return in.toLowerCase();
	}

	public String getType(String pname) {
		pname=normalize(pname);
		if (lex!=null && lex.containsKey(pname)) {
			Entry e=lex.get(pname);
			if (e!=null) {
				String ret = e.getType();
				if (ret==null) System.err.println("POS information not present for lexical entry: "+pname);
				return ret;
			}
		}
		return null;
	}

	public String getSurface(String pname) {
		pname=normalize(pname);
		if (lex!=null && lex.containsKey(pname)) {
			Entry e=lex.get(pname);
			if (e!=null) return e.getSurface();
		}
		return null;
	}

	public Integer getSubjectPosition(String pname) {
		pname=normalize(pname);
		if (lex!=null && lex.containsKey(pname)) {
			Entry e=lex.get(pname);
			if (e!=null) {
				Integer pos=e.getSubjectPosition();
				if (e.getType()==null) System.err.println("POS information not present for lexical entry: "+pname);
				return pos;
			}
		}
		return null;
	}
	public ARGTYPE getTypeOfArgumentAtPosition(String pname,int pos) {
		pname=normalize(pname);
		if (lex!=null && lex.containsKey(pname)) {
			Entry e=lex.get(pname);
			if (e!=null) return e.getTypeOfArgumentAtPosition(pos);
		}
		return null;
	}

	public boolean isPassive(String pname) {
		pname=normalize(pname);
		if (lex!=null && lex.containsKey(pname)) {
			Entry e=lex.get(pname);
			if (e!=null) return e.isPassive();
		}
		return false;
	}

	public boolean contains(String pname) {
		if (lex!=null) {
			pname=normalize(pname);
			return lex.containsKey(pname);
		}
		return false;
	}

}
