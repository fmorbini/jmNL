package edu.usc.ict.nl.nlg.lf.pos;

import java.util.ArrayList;
import java.util.List;

import simplenlg.features.Feature;
import simplenlg.features.Form;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.phrasespec.VPPhraseSpec;
import edu.usc.ict.nl.util.StringUtils;

public class VP extends POS {
	public String verb;
	public List<POS> arguments;
	private boolean passive=false;
	private boolean negated=false;
	private boolean infinitive=false;
	public List<String> mods=null; 

	@Override
	public VP clone() {
		VP ret=new VP();
		ret.verb=verb;
		if (arguments!=null) {
			ret.arguments=new ArrayList<POS>();
			for(POS a:arguments) ret.arguments.add((a!=null)?a.clone():null);
		}
		ret.passive=passive;
		ret.negated=negated;
		ret.infinitive=infinitive;
		if (mods!=null) {
			ret.mods=new ArrayList<String>(mods);
		}
		return ret;
	}
	
	public NLGElement toSimpleNLG(NLGFactory nlgFactory) {
		VPPhraseSpec vp = nlgFactory.createVerbPhrase(verb);
		vp.setFeature(Feature.PASSIVE, isPassive());
		vp.setFeature(Feature.NEGATED, isNegated());
		if (isInfinitive()) vp.setFeature(Feature.FORM, Form.INFINITIVE);
		if (arguments!=null) {
			int i=0;
			for(POS a:arguments) {
				if (a==null) a=NP.EMPTYNP;
				NLGElement complement = a.toSimpleNLG(nlgFactory);
				//if (i==0) complement.setFeature(InternalFeature.DISCOURSE_FUNCTION,DiscourseFunction.OBJECT);
				//else if (i==1) complement.setFeature(InternalFeature.DISCOURSE_FUNCTION,DiscourseFunction.INDIRECT_OBJECT);
				vp.setComplement(complement);
				i++;
			}
		}
		if (mods!=null) for(String m:mods) vp.addModifier(m);
		return vp;
	}

	public void addArgument(POS a) {
		if (arguments==null) arguments=new ArrayList<POS>();
		arguments.add(a);
	}

	public void addModifier(String m) {
		if (!StringUtils.isEmptyString(m)) {
			if (mods==null) mods=new ArrayList<String>();
			mods.add(m);
		}
	}
	
	public POS getArgument(int i) {
		if (arguments!=null && arguments.size()>i) return arguments.get(i);
		return null;
	}
	public POS getObject() {return getArgument(0);}
	public POS getIndirectObject() {return getArgument(1);}

	@Override
	public boolean equals(POS o) {
		if (o!=null && o instanceof VP) {
			if (verb.equals(((VP)o).verb)) {
				if (arguments==((VP)o).arguments) return true;
				else if (arguments!=null && ((VP)o).arguments!=null && arguments.size()==((VP)o).arguments.size()) {
					for(int i=0;i<arguments.size();i++) {
						if (!arguments.get(i).equals(((VP)o).arguments.get(i))) return false;
					}
					return true;
				}
			}
		}
		return false;
	}

	public void setPassive(boolean passive) {
		this.passive=passive;
	}
	public boolean isPassive() {
		return passive;
	}

	public void setNegated(boolean negated) {
		this.negated=negated;
	}
	public boolean isNegated() {
		return negated;
	}
	public void setInfinitive(boolean infinitive) {
		this.infinitive = infinitive;
	}
	public boolean isInfinitive() {
		return infinitive;
	}
	
	@Override
	public String toString() {
		StringBuffer ret=new StringBuffer();
		ret.append("VP(");
		ret.append(verb);
		if (mods!=null) for(String m:mods) {
			ret.append("MOD:("+m+")");
		}
		if (isPassive()) ret.append("_PASSIVE_");
		if (isNegated()) ret.append("_NEGATED_");
		if (isInfinitive()) ret.append("_INFINITIVE_");
		if (arguments!=null) {
			int i=0;
			for(POS a:arguments) {
				if (a==null) a=NP.EMPTYNP;
				ret.append(i+":("+a+")");
				i++;
			}
		}
		ret.append(")");
		return ret.toString();
	}
	
	@Override
	public List<POS> getChildren() {
		return arguments;
	}
}
