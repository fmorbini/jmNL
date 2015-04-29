package edu.usc.ict.nl.nlu.fst.train;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Alignment {

	private static final Pattern ap=Pattern.compile("([0-9]+)-([0-9]+)");
	Map<Integer,Integer> aTsM=null;
	String[] sTs,tTs;
	public Alignment(String s, String t, String a) throws Exception {
		this.sTs=s.split("[\\s]+");
		this.tTs=t.split("[\\s]+");
		String[] aTs=a.split("[\\s]+");
		this.aTsM=new HashMap<Integer, Integer>();
		for(String at:aTs) {
			Matcher m=ap.matcher(at);
			if (m.matches()) {
				int sp=Integer.parseInt(m.group(1));
				int tp=Integer.parseInt(m.group(2));
				if(sp>sTs.length) throw new Exception("aligner pattern "+at+" with out of bound source: "+sp+" max allowed: "+sTs.length);
				if(tp>tTs.length) throw new Exception("aligner pattern "+at+" with out of bound target: "+tp+" max allowed: "+tTs.length);
				this.aTsM.put(sp, tp);
			} else throw new Exception("wrong aligner pattern: "+at);
		}
	}
	@Override
	public String toString() {
		StringBuffer ret=new StringBuffer();
		for(int sp=0;sp<sTs.length;sp++) {
			String target="<epsilon>";
			if (aTsM.containsKey(sp)) {
				target=tTs[aTsM.get(sp)];
			}
			if (ret.length()>0) ret.append(" ");
			ret.append(sTs[sp]+"-"+target);
		}
		return ret.toString();
	}
	
}
