package edu.usc.ict.nl.dm.reward.model;

import java.util.ArrayList;
import java.util.List;

import edu.usc.ict.nl.dm.reward.util.CompressedEffectList;
import edu.usc.ict.nl.util.Rational;
import edu.usc.ict.nl.util.Triple;
import edu.usc.ict.nl.util.graph.GraphElement;

public class DialogueOperatorNodesChain {
		
	private List<Triple<GraphElement,Rational,List<DialogueOperatorEffect>>> chain;
	private CompressedEffectList compressedEffects=new CompressedEffectList();
	private DialogueOperator op;
	private boolean completed=false;
	private Rational chainWeight=Rational.one;
	
	public CompressedEffectList getCompressedEffects() {return compressedEffects;}

	public DialogueOperatorNodesChain(DialogueOperator op) {
		this.op=op;
	}

	public DialogueOperator getOperator() {return op;}
	
	public void addLinkToChain(GraphElement link,Rational weight,List<DialogueOperatorEffect> effects) throws Exception {
		effects=op.filterEffectsWithIgnoredVariables(effects);
		if (chain==null) chain=new ArrayList<Triple<GraphElement,Rational,List<DialogueOperatorEffect>>>();
		chain.add(new Triple<GraphElement, Rational, List<DialogueOperatorEffect>>(link, weight, effects));
		compressedEffects.mergeTheseEffects(effects);
		chainWeight=(chainWeight.compareTo(weight)<=0)?chainWeight:weight;
	}

	@Override
	protected DialogueOperatorNodesChain clone() throws CloneNotSupportedException {
		DialogueOperatorNodesChain newChain=new DialogueOperatorNodesChain(op);
		newChain.chain=(!isEmpty())?new ArrayList<Triple<GraphElement,Rational,List<DialogueOperatorEffect>>>(chain):null;
		newChain.chainWeight=chainWeight;
		newChain.compressedEffects=compressedEffects.clone();
		return newChain;
	}
	
	public List<Triple<GraphElement,Rational,List<DialogueOperatorEffect>>> getChain() {return chain;}

	/*public String getChainID() {
		if (!isEmpty()) {
			String id="";
			for(Triple<GraphElement, Rational, List<DialogueOperatorEffect>> chainEl:getChain()) {
				List<DialogueOperatorEffect> effects = chainEl.getThird();
				if ((effects!=null) && !effects.isEmpty()) for(DialogueOperatorEffect e:effects) id+=e.getID();
			}
			return id;
		} else return null;
	}*/
	public String getChainID() {
		if (compressedEffects!=null) {
			return compressedEffects.getID();
		} else return null;
	}
	/*public static String updateChainIDWithThis(String id,List<DialogueOperatorEffect> effects) {
		if ((effects!=null) && !effects.isEmpty())
			for(DialogueOperatorEffect e:effects) {
				if (id!=null) id+=e.getID();
				else id=e.getID();
			}
		return id;
	}*/
	public String updateChainIDWithThis(List<DialogueOperatorEffect> effects) throws Exception {
		effects=op.filterEffectsWithIgnoredVariables(effects);
		CompressedEffectList copy = compressedEffects.clone();
		copy.mergeTheseEffects(effects);
		return copy.getID();
	}

	public Triple<GraphElement,Rational,List<DialogueOperatorEffect>> getLastElementInChain() {
		List<Triple<GraphElement, Rational, List<DialogueOperatorEffect>>> c = getChain();
		if (c!=null && !c.isEmpty()) {
			return c.get(c.size()-1);
		}else return null;
	}

	public boolean isEmpty() {
		return (chain==null) || chain.isEmpty();
	}
	
	@Override
	public String toString() {
		String cs="<"+op.getName()+"_"+getWeight();
		if (isEmpty()) return cs+">";
		else {
			cs+=" ";
			for(Triple<GraphElement,Rational,List<DialogueOperatorEffect>> link:chain) {
				cs+="["+link.getFirst()+"_"+link.getSecond()+"] ";
			}
			cs+=">";
			return cs;
		}
	}

	public float getWeight() {return chainWeight.getResult();}
	public Rational getRationalWeight() {return chainWeight;}
	public void setWeight(float w) throws Exception {this.chainWeight=new Rational(w);}
	public void setWeight(Rational w) {this.chainWeight=w;}

	public boolean getIsCompleted() {return completed;}
	public void setIsCompleted(boolean c) {this.completed=c;}
	
	public static void main(String[] args) throws Exception {
		CompressedEffectList a = new CompressedEffectList();
		List<DialogueOperatorEffect> es=new ArrayList<DialogueOperatorEffect>();
		es.add(DialogueOperatorEffect.parse("assign(a,+(a,3))"));
		es.add(DialogueOperatorEffect.parse("assign(a,+(a,2))"));
		es.add(DialogueOperatorEffect.parse("assign(a,-(a,1))"));
		es.add(DialogueOperatorEffect.parse("assign(a,-(1,a))"));
		es.add(DialogueOperatorEffect.parse("assign(a,+(4,a))"));
		es.add(DialogueOperatorEffect.parse("assign(a,+(4,b))"));
		es.add(DialogueOperatorEffect.parse("assign(a,+(a,2))"));
		es.add(DialogueOperatorEffect.parse("assign(a,7)"));
		es.add(DialogueOperatorEffect.parse("assign(a,+(a,2))"));
		a.mergeTheseEffects(es);
		System.out.println(a.getCompressedEffects());
	}
}
