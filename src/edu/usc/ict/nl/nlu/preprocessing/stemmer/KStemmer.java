package edu.usc.ict.nl.nlu.preprocessing.stemmer;

public class KStemmer extends Stemmer {
	org.apache.lucene.analysis.ICTKStemmer stemmer;
	public KStemmer() {
		stemmer=new org.apache.lucene.analysis.ICTKStemmer();
	}
	@Override
	public String stemm(String word) {
		if (stemmer!=null) return stemmer.stem(word);
		else return null;
	}
}
