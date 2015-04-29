package edu.usc.ict.nl.nlu.fst.train;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.usc.ict.nl.util.Pair;

public class AlignmentSummary {

	private Map<String,Map<String,Integer>> words2NLUConcepts=null,nluConcepts2Words=null;
	private Map<String,Map<String,Integer>> nluConcepts2phrases=null;

	public AlignmentSummary(List<Alignment> as) {
		if (as!=null && !as.isEmpty()) {
			for(Alignment a:as) {
				updateWithAlignment(a);
			}
		}
	}

	public Map<String, Map<String, Integer>> getNluConcepts2phrases() {
		return nluConcepts2phrases;
	}
	
	private void updateWithAlignment(Alignment a) {
		updateText2ConceptWithAlignment(a);
		updateConcept2TextWithAlignment(a);
	}
	private void updateText2ConceptWithAlignment(Alignment a) {
		int wl=a.sTs.length;
		Map<String,String> conceptsToPhrases=extractPhrasesForEachConcept(a.sTs,a.tTs,a.aTsM);
		updatePhrases2NLUConcepts(conceptsToPhrases);
		for(int i=0;i<wl;i++) {
			String w=a.sTs[i];
			w=normalize(w);
			if (a.aTsM.containsKey(i)) {
				String nluC=a.tTs[a.aTsM.get(i)];
				if (words2NLUConcepts==null) words2NLUConcepts=new HashMap<String, Map<String,Integer>>();
				Map<String,Integer> nluConcepts4Word=words2NLUConcepts.get(w);
				if (nluConcepts4Word==null) words2NLUConcepts.put(w,nluConcepts4Word=new HashMap<String, Integer>());
				Integer count=nluConcepts4Word.get(nluC);
				if (count==null) nluConcepts4Word.put(nluC,1);
				else nluConcepts4Word.put(nluC,count+1);
			}
		}
	}

	private void updatePhrases2NLUConcepts(Map<String, String> conceptsToPhrases) {
		if (conceptsToPhrases!=null) {
			if (nluConcepts2phrases==null) nluConcepts2phrases=new HashMap<String, Map<String,Integer>>();
			for(String concept:conceptsToPhrases.keySet()) {
				String phrase=conceptsToPhrases.get(concept);
				if (phrase!=null) {
					Map<String,Integer> phrases4Concept=nluConcepts2phrases.get(concept);
					if (phrases4Concept==null) nluConcepts2phrases.put(concept, phrases4Concept=new HashMap<String, Integer>());
					Integer count=phrases4Concept.get(phrase);
					if (count==null) phrases4Concept.put(phrase,1);
					else phrases4Concept.put(phrase,count+1);
				}
			}
		}
	}

	private Map<String, String> extractPhrasesForEachConcept(String[] words, String[] concepts, Map<Integer, Integer> wordPosToConceptID) {
		Map<String, String> ret=null;
		Map<String, Boolean> conceptInterrupted=null;
		int wl=words.length;
		int currentConcept=-1;
		for(int i=0;i<wl;i++) {
			Integer concept=wordPosToConceptID.get(i);
			if (concept!=null) {
				if (concept!=currentConcept) {
					if (currentConcept>=0) {
						//put an interruption in current concept
						String phrase=ret.get(concepts[currentConcept]);
						if (conceptInterrupted==null) conceptInterrupted=new HashMap<String, Boolean>();
						Boolean wasInterrupted=conceptInterrupted.get(concepts[currentConcept]);
						if (wasInterrupted!=null && wasInterrupted) {
							ret.put(concepts[currentConcept], phrase+"...");
							conceptInterrupted.put(concepts[currentConcept],false);
						} else {
							conceptInterrupted.put(concepts[currentConcept],true);
						}
					}
					currentConcept=concept;
				}
				String w=words[i];
				if (ret==null) ret=new HashMap<String, String>();
				String phrase=ret.get(concepts[currentConcept]);
				if (phrase!=null) {
					ret.put(concepts[currentConcept], phrase+" "+w);
				} else {
					ret.put(concepts[currentConcept], w);
				}
			}
		}
		return ret;
	}

	private void updateConcept2TextWithAlignment(Alignment a) {
		int wl=a.sTs.length;
		for(int i=0;i<wl;i++) {
			String w=a.sTs[i];
			w=normalize(w);
			if (a.aTsM.containsKey(i)) {
				String nluC=a.tTs[a.aTsM.get(i)];
				if (nluConcepts2Words==null) nluConcepts2Words=new HashMap<String, Map<String,Integer>>();
				Map<String,Integer> words4Nluconcepts=nluConcepts2Words.get(nluC);
				if (words4Nluconcepts==null) nluConcepts2Words.put(nluC,words4Nluconcepts=new HashMap<String, Integer>());
				Integer count=words4Nluconcepts.get(w);
				if (count==null) words4Nluconcepts.put(w,1);
				else words4Nluconcepts.put(w,count+1);
			}
		}
	}

	public List<Pair<String,Integer>> getNLUConceptsTriggeredByWord(String w) {
		List<Pair<String,Integer>> ret=null;
		if (words2NLUConcepts!=null) {
			w=normalize(w);
			if (words2NLUConcepts.containsKey(w)) {
				final Map<String,Integer> content=words2NLUConcepts.get(w);
				List<String> concepts=new ArrayList<String>(content.keySet());
				Collections.sort(concepts, new Comparator<String>() {
					@Override
					public int compare(String o1, String o2) {
						int c1=0;
						int c2=0;
						if (content.containsKey(o1)) c1=content.get(o1);
						if (content.containsKey(o2)) c2=content.get(o2);
						return c2-c1;
					}
				});
				for(String c:concepts) {
					if (ret==null) ret=new ArrayList<Pair<String,Integer>>();
					ret.add(new Pair<String, Integer>(c, content.get(c)));
				}
			}
		}
		return ret;
	}
	public List<Pair<String,Integer>> getWordsIndicatingNLUConcept(String concept) {
		List<Pair<String,Integer>> ret=null;
		if (nluConcepts2Words!=null) {
			if (nluConcepts2Words.containsKey(concept)) {
				final Map<String,Integer> content=nluConcepts2Words.get(concept);
				List<String> words=new ArrayList<String>(content.keySet());
				Collections.sort(words, new Comparator<String>() {
					@Override
					public int compare(String o1, String o2) {
						int c1=0;
						int c2=0;
						if (content.containsKey(o1)) c1=content.get(o1);
						if (content.containsKey(o2)) c2=content.get(o2);
						return c2-c1;
					}
				});
				for(String w:words) {
					if (ret==null) ret=new ArrayList<Pair<String,Integer>>();
					ret.add(new Pair<String, Integer>(w, content.get(w)));
				}
			}
		}
		return ret;
	}

	private String normalize(String w) {return w.replaceAll("[\\s]+", "").toLowerCase();}
}
