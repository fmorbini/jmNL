package edu.usc.ict.nl.nlu.features;

import java.util.List;
import java.util.ListIterator;

public abstract class FeatureBuilderForJMXClassifier extends FeaturesBuilderForMXClassifier {
	public static List<String> buildfeaturesFromUtterance(String utt) throws Exception {
		return null;
	}
	public static List<String> buildFeatureForWordAtPosition(String[] tokens,int pos) {
		if (tokens!=null) {
			for(int i=0;i<tokens.length;i++) tokens[i]=tokens[i].toUpperCase();
			List<String> features = FeaturesBuilderForMXClassifier.buildFeatureForWordAtPosition(tokens, pos);
			if (features!=null) {
				ListIterator<String> fit=features.listIterator();
				while(fit.hasNext()) fit.set("PRT0"+fit.next());
				return features;
			}
		}
		return null;
	}
}
