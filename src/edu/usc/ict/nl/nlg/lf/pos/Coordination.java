package edu.usc.ict.nl.nlg.lf.pos;

import java.util.ArrayList;
import java.util.List;

import simplenlg.framework.CoordinatedPhraseElement;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;

public class Coordination extends POS {

	public List<POS> conj;
	public Object cc=CONJ.AND;
	public String index=null;

	@Override
	public POS clone() {
		Coordination ret=new Coordination();
		if (conj!=null) {
			ret.conj=new ArrayList<POS>();
			for(POS c:conj) ret.conj.add(c.clone());
		}
		ret.cc=cc;
		return ret;
	}
	
	public NLGElement toSimpleNLG(NLGFactory nlgFactory) {
		if (conj!=null && !conj.isEmpty()) {
			CoordinatedPhraseElement ret=nlgFactory.createCoordinatedPhrase();
			ret.setConjunction(cc.toString().toLowerCase());
			for(POS o:conj) {
				if (o!=null) ret.addCoordinate(o.toSimpleNLG(nlgFactory));
			}
			return ret;
		}
		return null;
	}

	public void add(POS thing) {
		if (conj==null) conj=new ArrayList<POS>();
		conj.add(thing);
	}
	
	public void setFunction(CONJ c) {
		cc=c;
	}
	public void setFunction(Object c) {
		cc=c;
	}

	@Override
	public boolean equals(POS o) {
		if (o!=null && o instanceof Coordination) {
			if (conj==((Coordination)o).conj && cc==((Coordination)o).cc) return true;
			else if (conj!=null && ((Coordination)o).conj!=null && conj.size()==((Coordination)o).conj.size()) {
				for(int i=0;i<conj.size();i++) {
					if (!conj.get(i).equals(((Coordination)o).conj.get(i))) return false;
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuffer ret=new StringBuffer();
		ret.append("CC(");
		ret.append("Conj:("+cc.toString().toLowerCase()+")");
		if (conj!=null && !conj.isEmpty()) {
			for(POS o:conj) {
				if (o!=null) {
					ret.append("C:("+o+")");
				}
			}
		}
		ret.append(")");
		return ret.toString();
	}
	
	@Override
	public List<POS> getChildren() {
		return conj;
	}
}
