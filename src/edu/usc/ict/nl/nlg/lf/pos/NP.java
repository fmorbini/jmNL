package edu.usc.ict.nl.nlg.lf.pos;

import java.util.ArrayList;
import java.util.List;

import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.phrasespec.NPPhraseSpec;
import edu.usc.ict.nl.util.StringUtils;

public class NP extends POS {
	public String noun;
	public DT determiner;
	public List<String> mods=null; 
	
	public NP(String noun) {
		this.noun=noun;
	}

	@Override
	public POS clone() {
		NP ret=new NP(noun);
		ret.determiner=determiner;
		if (mods!=null) {
			ret.mods=new ArrayList<String>(mods);
		}
		return ret;
	}
	
	public NLGElement toSimpleNLG(NLGFactory nlgFactory) {
		NPPhraseSpec thisPhrase=nlgFactory.createNounPhrase();
		thisPhrase.setHead(noun);
		if (determiner!=null) {
			if (determiner!=DT.NULL) {
				thisPhrase.setDeterminer(determiner.toString().toLowerCase());
			}
		} else thisPhrase.setDeterminer(DT.THE.toString().toLowerCase());
		if (mods!=null) for(String m:mods) thisPhrase.addModifier(m);
		return thisPhrase;
	}
	
	public void addModifier(String m) {
		if (!StringUtils.isEmptyString(m)) {
			if (mods==null) mods=new ArrayList<String>();
			mods.add(m);
		}
	}
	
	@Override
	public boolean equals(POS o) {
		if (o!=null && o instanceof NP) {
			return ((noun==((NP)o).noun || noun.equals(((NP)o).noun)) &&
					(determiner==((NP)o).determiner || determiner.equals(((NP)o).determiner)));
		}
		return false;
	}

	public static final NP EMPTYNP=createEmptyNP();
	private static NP createEmptyNP() {
		NP ret=new NP("something");
		((NP)ret).determiner=DT.NULL;
		return ret;
	}
	
	@Override
	public String toString() {
		StringBuffer ret=new StringBuffer();
		ret.append("NP(");
		if (determiner!=null) {
			if (determiner!=DT.NULL) {
				ret.append(determiner.toString().toLowerCase());
			}
		} else ret.append(DT.THE.toString().toLowerCase());
		ret.append(" "+noun);
		if (mods!=null) for(String m:mods) {
			ret.append("MOD:("+m+")");
		}
		ret.append(")");
		return ret.toString();
	}
	
	@Override
	public List<POS> getChildren() {
		return null;
	}
}
