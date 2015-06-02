package edu.usc.ict.nl.nlg.lf.pos;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import simplenlg.framework.NLGFactory;
import simplenlg.phrasespec.SPhraseSpec;

public class Sentence extends POS {
	public POS subject=null;
	public POS verbPhrase=null;
	
	@Override
	public POS clone() {
		Sentence ret=new Sentence();
		ret.subject=(subject!=null)?subject.clone():null;
		ret.verbPhrase=(verbPhrase!=null)?verbPhrase.clone():null;
		return ret;
	}
	
	public SPhraseSpec toSimpleNLG(NLGFactory nlgFactory) {
		SPhraseSpec s=nlgFactory.createClause();
		if (subject==null) subject=NP.EMPTYNP;
		s.setSubject(subject.toSimpleNLG(nlgFactory));
		s.setVerbPhrase(verbPhrase.toSimpleNLG(nlgFactory));
		return s;
	}
	public void addSubject(POS s) {
		if (subject==null) subject=s;
		else if (subject instanceof Coordination) {
			((Coordination)subject).add(s);
		} else {
			NP olds=(NP) subject;
			subject=new Coordination();
			((Coordination)subject).add(olds);
			((Coordination)subject).add(s);
		}
	}
	
	@Override
	public boolean equals(POS o) {
		if (o!=null && o instanceof Sentence) {
			return (subject==((Sentence)o).subject || subject.equals(((Sentence)o).subject)) && (verbPhrase==((Sentence)o).verbPhrase || verbPhrase.equals(((Sentence)o).verbPhrase));
		}
		return false;
	}
	
	public void addVerbPhrase(VP vp) {
		if (verbPhrase==null) verbPhrase=vp;
		else {
			if (verbPhrase instanceof Coordination) {
				((Coordination)verbPhrase).add(vp);
			} else {
				POS oldvp=verbPhrase;
				verbPhrase=new Coordination();
				((Coordination)verbPhrase).add(oldvp);
				((Coordination)verbPhrase).add(vp);
			}
		}
	}
	
	public void addVerbModifier(String m) {
		if (verbPhrase!=null) {
			Stack<POS> s=new Stack<POS>();
			s.push(verbPhrase);
			while (!s.isEmpty()) {
				POS x=s.pop();
				if (x instanceof VP) {
					((VP) x).addModifier(m);
				} else {
					s.addAll(((Coordination)x).conj);
				}
			}
		}
	}
	
	@Override
	public String toString() {
		StringBuffer ret=new StringBuffer();
		if (subject==null) subject=NP.EMPTYNP;
		ret.append("CLAUSE(");
		ret.append("Subj=("+subject+")");
		ret.append("Vp=("+verbPhrase+")");
		ret.append(")");
		return ret.toString();
	}
	
	@Override
	public List<POS> getChildren() {
		List<POS> ret=null;
		if (subject!=null) {
			if (ret==null) ret=new ArrayList<POS>();
			ret.add(subject);
		}
		if (verbPhrase!=null) {
			if (ret==null) ret=new ArrayList<POS>();
				ret.add(verbPhrase);
		}
		return ret;
	}
	@Override
	public void updateChild(int pos, POS child) {
		if (pos==0) {
			if (subject!=null) subject=child;
			else verbPhrase=child;
		} else if (pos==1) verbPhrase=child;
		else System.err.println("error setting child of sentence at position: "+pos);
	}

}
