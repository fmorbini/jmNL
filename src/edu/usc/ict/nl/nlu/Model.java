package edu.usc.ict.nl.nlu;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Model {
	Map<String,Map<String,FeatureWeight>> feature2weightForOutputLabel=null;
	Set<String> labels=null;
	
	public class FeatureWeight implements Comparable<FeatureWeight> {
		private float multiplicativeWeight,offsetWeight;
		private String featureName;
		public FeatureWeight(String featureName,float multiplicativeWeight) {
			this(featureName,0f,multiplicativeWeight);
		}
		public FeatureWeight(String featureName,float offsetWeight, float multiplicativeWeight) {
			this.offsetWeight=offsetWeight;
			this.multiplicativeWeight=multiplicativeWeight;
			this.featureName=featureName;
		}
		public String getName() {return featureName;}
		public float getWeight() {return getWeight(0f,1f);}
		public float getWeight(float offsetMultiplier,float multiplicativeMultiplier) {
			return offsetMultiplier*offsetWeight+multiplicativeMultiplier*multiplicativeWeight;
		}
		public void setOffsetWeight(float w) {this.offsetWeight=w;}
		public void setMultiplicativeWeight(float w) {this.multiplicativeWeight=w;}
		@Override
		public String toString() {
			return "<"+getName()+" s="+offsetWeight+" m="+multiplicativeWeight+">";
		}
		@Override
		public int compareTo(FeatureWeight o) {
			if (multiplicativeWeight > o.multiplicativeWeight) return -1;
			else if (multiplicativeWeight < o.multiplicativeWeight) return 1;
			else return 0;
		}
	}
	
	public Map<String,Map<String,FeatureWeight>> getFeatures4Labels() {return feature2weightForOutputLabel;}
	
	public void addFeatureWeightForSA(String feature,String label,FeatureWeight w) throws Exception {
		if (feature2weightForOutputLabel==null) feature2weightForOutputLabel=new HashMap<String, Map<String,FeatureWeight>>();
		Map<String,FeatureWeight> sa2weightMap=feature2weightForOutputLabel.get(feature);
		if (sa2weightMap==null) feature2weightForOutputLabel.put(feature, sa2weightMap=new HashMap<String, FeatureWeight>());
		if (sa2weightMap.containsKey(label)) throw new Exception("read duplicated assignment of weight for feature '"+feature+"' for label '"+label+"'.");
		sa2weightMap.put(label, w);
		if (labels==null) labels=new HashSet<String>();
		labels.add(label);
	}

	public Map<String, FeatureWeight> getWeightsForFeature(String f) {
		if (feature2weightForOutputLabel!=null && feature2weightForOutputLabel.containsKey(f)) return feature2weightForOutputLabel.get(f);
		else return null;
	}

	public Set<String> getOutputLabels() { return labels; }
}
