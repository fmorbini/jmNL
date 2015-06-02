package edu.usc.ict.nl.nlg.lf;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.w3c.tools.sexpr.Cons;

import edu.usc.ict.nl.nlg.lf.utils.NLUUtils;

public class Predication {
	private String predicate=null,subject=null,object=null;
	private List<String> objs=null;
	private Object source=null;
	private String evName=null;
	
	public Predication(Object p) {
		this(p,null);
	}
	public Predication(Object p, Lexicon lex) {
		this.source=p;
		evName=NLUUtils.getEventualityName(p);
		String pname=NLUUtils.getPredicateName(p);
		String subject=null,object=null;
		List<String> arguments=null;
		Object cdr = NLUUtils.cdr(p);
		if (cdr!=null && cdr instanceof Cons) {
			Enumeration e=((Cons) cdr).elements();
			boolean isPrimed=NLUUtils.isPrimedPredicate(p);
			int i=0,argPos=0;
			Integer subjectPos=0;
			if (lex!=null) {
				subjectPos=lex.getSubjectPosition(pname);
			}
			while(e.hasMoreElements()) {
				Object el = e.nextElement();
				if (!isPrimed || i>0) {
					if (subjectPos!=null && argPos==subjectPos) {
						subject=NLUUtils.toString(el);
					} else {
						if (object==null) object=NLUUtils.toString(el);
						else {
							if (arguments==null) arguments=new ArrayList<String>();
							arguments.add(NLUUtils.toString(el));
						}
					}
					argPos++;
				}
				i++;
			}
		}
		setProperties(pname,subject,object,arguments);
	}
	
	private void setProperties(String p,String s,String o,List<String> args) {
		this.predicate=p;
		this.subject=s;
		this.object=o;
		if (args!=null && !args.isEmpty()) {
			for(String a:args) {
				if (this.objs==null) this.objs=new ArrayList<String>();
				this.objs.add(a);
			}
		}
	}
	
	@Override
	public String toString() {
		StringBuffer ret=new StringBuffer();
		ret.append(subject);
		ret.append(" "+predicate);
		if (object!=null) ret.append(" A0: "+object);
		if (objs!=null) {
			int i=1;
			for(String o:objs) {
				ret.append(" A"+i+": "+o);
				i++;
			}
		}
		return ret.toString();
	}
	
	public String getSubject() {
		return subject;
	}
	
	public String getObject() {
		return object;
	}
	public String getPredicate() {
		return predicate;
	}
	
	public List<String> getOtherObjects() {
		return objs;
	}
	
	public Object getSource() {
		return source;
	}
	
	public String getEventualityName() {
		return evName;
	}

	public String getArgument(int pos) {
		if (pos==0) return getSubject();
		if (pos==1) return getObject();
		if (pos>1 && getOtherObjects()!=null && getOtherObjects().size()>(pos-2)) {
			return getOtherObjects().get(pos-2);
		}
		return null;
	}
	public int getLength() {
		int ret=0;
		if (getSubject()!=null) ret=1;
		if (getObject()!=null) ret=2;
		if (getOtherObjects()!=null) ret+=getOtherObjects().size()+2;
		return ret;
	}
}
