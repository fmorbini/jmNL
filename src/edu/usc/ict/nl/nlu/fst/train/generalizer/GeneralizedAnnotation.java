package edu.usc.ict.nl.nlu.fst.train.generalizer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.fst.FSTNLUOutput;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;

public class GeneralizedAnnotation {

	private String keyvaluesep=null;
	private Map<String,Set<String>> lexiconTerms=null;
	private Map<String,Set<String>> prefixes=null;

	public GeneralizedAnnotation(String keyvaluesep) {
		this.keyvaluesep=keyvaluesep;
	}

	public void addLeafConceptForParentOfPath(String leaf, String parent,String path) {
		if (!StringUtils.isEmptyString(path)) {
			//System.out.println(path+" -> "+parent+"::"+leaf);
			String prefix=path;
			if (!StringUtils.isEmptyString(leaf) && !StringUtils.isEmptyString(parent)) {
				String suffix=parent+keyvaluesep+leaf;
				assert(path.endsWith(suffix));

				if (lexiconTerms==null) lexiconTerms=new HashMap<String, Set<String>>();
				Set<String> terms=lexiconTerms.get(parent);
				if (terms==null) lexiconTerms.put(parent, terms=new HashSet<String>());
				terms.add(leaf);

				int prefixEnd=path.length()-suffix.length()-1; //-1 to remove the keyvalue separator
				if (prefixEnd>0) prefix=path.substring(0, prefixEnd);
				else prefix=null;
			}
			if (!StringUtils.isEmptyString(prefix)) {
				if (prefixes==null) prefixes=new HashMap<String, Set<String>>();
				Set<String> parents=prefixes.get(prefix);
				if (parents==null) prefixes.put(prefix, parents=new HashSet<String>());
				parents.add(parent);
			}
		}
	}
	
	public Map<String, Set<String>> getLexiconTerms() {
		return lexiconTerms;
	}
	
	public Map<String, Set<String>> getPrefixes() {
		return prefixes;
	}

	/**
	 * 
	 * @param phrases this is the list of phrases (with count) associated to a given nlu concept (i.e. key-value pair).
	 * These phrases are extracted from the aligner output file.  
	 * @return
	 */
	public Map<String, Set<String>> buildActualLexicon(Map<String, Map<String, Integer>> phrases) {
		Map<String,Set<String>> ret=null;
		if (phrases!=null && lexiconTerms!=null) {
			for(String concept:phrases.keySet()) {
				Map<String, Integer> entriesForconcept = phrases.get(concept);
				if (entriesForconcept!=null && !entriesForconcept.isEmpty()) {
					List<Pair<String, String>> pairs = FSTNLUOutput.getPairsFromString(concept);
					for(Pair<String, String> kv:pairs) {
						String label=kv.getFirst()+FSTNLUOutput.keyValueSep+kv.getSecond();
						String[] baseParts=label.split(String.valueOf(FSTNLUOutput.keyValueSep));
						if (baseParts.length>1) {
							String oneButLast=baseParts[baseParts.length-2];
							String last=baseParts[baseParts.length-1];
							if (lexiconTerms.containsKey(oneButLast)) {
								Set<String> lasts = lexiconTerms.get(oneButLast);
								if (!lasts.contains(last)) {
									System.err.println(Arrays.toString(baseParts));
									System.err.println(" didn't find '"+last+"' in available lexicon leaves. Available leaves are: "+lasts);
								} else {
									String lexiconLabel=oneButLast+FSTNLUOutput.keyValueSep+last;
									if (ret==null) ret=new HashMap<String, Set<String>>();
									Set<String> entries=ret.get(lexiconLabel);
									if (entries==null) ret.put(lexiconLabel, entries=new HashSet<String>());
									entries.addAll(entriesForconcept.keySet());
								}
							}
						}
					}
				}
			}
		}
		return ret;
	}

	private String generalizeKeyValuePair(String kvpair) {
		Map<String, Set<String>> ps = getPrefixes();
		if (kvpair!=null && ps!=null) {
			for(String p:ps.keySet()) {
				int lp=p.length();
				String substring=null;
				if (kvpair.startsWith(p) && (kvpair.length()==lp || (substring=kvpair.substring(lp)).startsWith(keyvaluesep))) {
					if (substring!=null) {
						Set<String> subs=ps.get(p);
						if (subs!=null && !subs.isEmpty()) {
							substring=substring.substring(1);
							for(String sub:subs) {
								int lsub=sub.length();
								if (substring.startsWith(sub) && (substring.substring(lsub).startsWith(keyvaluesep) || substring.length()==lsub)) {
									return kvpair.substring(0, lsub+keyvaluesep.length()+lp);
								}
							}
						} else return p;
					} else return p;
				}
			}
		}
		return kvpair;
	}
	
	public void generalizeTrainingData(List<TrainingDataFormat> tds) {
		if (tds!=null) {
			for(TrainingDataFormat td:tds) {
				String label=td.getLabel();
				List<Pair<String, String>> pairs = FSTNLUOutput.getPairsFromString(label);
				StringBuffer newLabel=new StringBuffer();
				for(Pair<String, String> kv:pairs) {
					String kvpair=kv.getFirst()+FSTNLUOutput.keyValueSep+kv.getSecond();
					kvpair=generalizeKeyValuePair(kvpair);
					newLabel.append(((newLabel.length()>0)?" ":"")+kvpair);
				}
				td.setLabel(newLabel.toString());
			}
		}
	}

}
