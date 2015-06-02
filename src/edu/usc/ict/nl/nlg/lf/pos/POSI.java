package edu.usc.ict.nl.nlg.lf.pos;

import java.util.List;

import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;

public interface POSI {
	public NLGElement toSimpleNLG(NLGFactory nlgFactory);
	public boolean equals(POS obj);
	public POS clone();
	public List<POS> getChildren();
	public void put(List<Integer> coord, POS newd);
	public void updateChild(int pos,POS child);
}
