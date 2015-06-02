package edu.usc.ict.nl.nlg.lf.pos;

import java.util.ArrayList;
import java.util.List;

import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.phrasespec.PPPhraseSpec;

public class PP extends POS {

	private POS complement;
	private String preposition; 
	@Override
	public NLGElement toSimpleNLG(NLGFactory nlgFactory) {
        PPPhraseSpec pp = nlgFactory.createPrepositionPhrase();
        pp.addComplement(complement.toSimpleNLG(nlgFactory));
        pp.setPreposition(preposition);
		return pp;
	}

	@Override
	public boolean equals(POS o) {
		if (o!=null && o instanceof PP) {
			return ((preposition==((PP)o).preposition || preposition.equals(((PP)o).preposition)) &&
					(complement==((PP)o).complement || complement.equals(((PP)o).complement)));
		}
		return false;
	}

	@Override
	public POS clone() {
		PP ret=new PP();
		ret.complement=complement.clone();
		ret.preposition=preposition;
		return ret;
	}
	
	public void setComplement(POS complement) {
		this.complement = complement;
	}
	public void setPreposition(String preposition) {
		this.preposition = preposition;
	}

	@Override
	public List<POS> getChildren() {
		List<POS> ret=null;
		if (complement!=null) {
			ret=new ArrayList<POS>();
			ret.add(complement);
		}
		return ret;
	}
	@Override
	public void updateChild(int pos, POS child) {
		if (complement!=null && pos==0) {
			complement=child;
		} else System.err.println("error updating child of pp at position: "+pos);
	}
}
