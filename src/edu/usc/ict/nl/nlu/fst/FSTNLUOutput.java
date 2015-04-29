package edu.usc.ict.nl.nlu.fst;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.Triple;
import edu.usc.ict.nl.util.graph.Edge;
import edu.usc.ict.nl.util.graph.GraphElement;

public class FSTNLUOutput extends NLUOutput {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private LinkedHashMap<String, List<Triple<Integer,String,Float>>> parts=null;

	public FSTNLUOutput(String text, String id, float prob, Object payload)	throws Exception {
		super(text, id, prob, payload);
	}

	public FSTNLUOutput(Pair<Float, List<GraphElement>> p) throws Exception {
		this(null,null,p.getFirst(),null);
		if (p!=null) {
			int i=0;
			for(GraphElement g:p.getSecond()) {
				Edge e=(Edge)g;
				String out=null,in=null;
				if (!e.getProduce().equalsIgnoreCase("<epsilon>"))
					out=e.getProduce();
				if (!e.getConsume().equalsIgnoreCase("<epsilon>"))
					in=e.getConsume();
				if (!StringUtils.isEmptyString(in)) i++;
				if (!StringUtils.isEmptyString(out)) {
					if (parts==null) parts=new LinkedHashMap<String, List<Triple<Integer,String,Float>>>();
					List<Triple<Integer, String,Float>> alreadyThere = parts.get(out);
					if (alreadyThere==null) parts.put(out, alreadyThere=new ArrayList<Triple<Integer,String,Float>>());
					alreadyThere.add(new Triple<Integer, String,Float>(i,in,e.getWeight()));
				}
			}
		}
		setPayload(parts);
	}

	@Override
	public LinkedHashMap<String, List<Pair<Integer,String>>> getPayload() {
		return (LinkedHashMap<String, List<Pair<Integer, String>>>) super.getPayload();
	}
	
	public static final char keyValueSep=':';
	public static String composeNLUKeyValuePair(String key,String value) throws Exception {
		if (key.contains(String.valueOf(keyValueSep))) throw new Exception("key contains special char '"+keyValueSep+"'.");
		return key+keyValueSep+value;
	}
	private static final Pattern nluPart=Pattern.compile("^([^"+keyValueSep+"]+)"+keyValueSep+"(.+)$");
	/**
	 * given the string "k1:v1 k2:v2 k1:v1.1" it returns a list with the maps. The first {k1:v1, v2:v2} and the second {k1:v1.1, k2:v2}.
	 * @param id
	 * @return
	 */
	public static List<Map<String,String>> decomposeNLUOutput(String id) {
		List<Pair<String,String>> keyValuePairs=getPairsFromString(id);
		int numPairs = keyValuePairs.size();
		List<Pair<String,String>> unique=destructivelyExtractUnique(keyValuePairs);
		List<Map<String,String>> ret=null;
		
		//if all keys are used only once, build single map and return in list 
		if (unique != null && numPairs == unique.size()) {
			ret=new ArrayList<Map<String,String>>();
			Map<String,String> singleRet=new HashMap<String, String>();
			for (Pair<String,String>c:unique) {
				singleRet.put(c.getFirst(), c.getSecond());
			}
			ret.add(singleRet);
			return ret;
		}
		List<List<Pair<String, String>>> combinations=buildAllCombinations(keyValuePairs);
		Map<String,String> base=null;
		if (unique!=null && !unique.isEmpty()) {
			for(Pair<String,String> u:unique) {
				if (base==null) base=new HashMap<String, String>();
				base.put(u.getFirst(),u.getSecond());
			}
		}
		if (combinations!=null && !combinations.isEmpty()) {
			for(List<Pair<String,String>> cs:combinations) {
				Map<String,String> singleRet=new HashMap<String, String>();
				if (ret==null) ret=new ArrayList<Map<String,String>>();
				for(Pair<String,String> c:cs) {
					singleRet.put(c.getFirst(), c.getSecond());
					if (base!=null) singleRet.putAll(base);
				}
				ret.add(singleRet);
			}
		} else if (base!=null) {
			if (ret==null) ret=new ArrayList<Map<String,String>>();
			ret.add(base);
		}
		
		return ret;
	}
	
	public static String removeLastComponent(String concept) {
		if (concept!=null) {
			int p=0,lastp=-1;
			while((p=concept.indexOf(keyValueSep, p))>=0) {
				lastp=p;
				p++;
			}
			if (lastp>=0) return concept.substring(0, lastp); 
		}
		return null;
	}
	public static String getLastComponent(String concept) {
		if (concept!=null) {
			int p=0,lastp=-1;
			while((p=concept.indexOf(keyValueSep, p))>=0) {
				lastp=p;
				p++;
			}
			if (lastp>=0) return concept.substring(lastp+1); 
		}
		return null;
	}
	
	/**
	 * the input should contain pairs for which the first part appear at least in two pairs.
	 * @param keyValuePairs
	 * @return
	 */
	private static List<List<Pair<String, String>>> buildAllCombinations(List<Pair<String, String>> keyValuePairs) {
		List<List<Pair<String, String>>> ret=null;
		if (keyValuePairs!=null) {
			Map<String,Set<String>> keys=null;
			for(Pair<String,String> kv:keyValuePairs) {
				String k=kv.getFirst();
				if (keys==null) keys=new HashMap<String, Set<String>>();
				Set<String> vs=keys.get(k);
				if (vs==null) keys.put(k, vs=new HashSet<String>());
				vs.add(kv.getSecond());
			}
			if (ret==null) ret=new ArrayList<List<Pair<String,String>>>();
			List<String> keysAsList = new ArrayList<String>();
			Map<String,Integer> numOccurrences = new HashMap<String,Integer>();
			Map<String,Integer> maxNumOccurrences = new HashMap<String,Integer>();
			int numTotalCombinations=1;
			for (String key:keys.keySet()) {
				keysAsList.add(key);
				numTotalCombinations*=keys.get(key).size();
			}
			for (String key:keys.keySet()) {
				for (String value:keys.get(key)) {
					numOccurrences.put(key+":"+value,0);
					maxNumOccurrences.put(key+":"+value, numTotalCombinations/keys.get(key).size());
				}
			}
			recursiveProduceCombinations(ret,keysAsList, keys,new HashMap<String,String>(),numOccurrences,maxNumOccurrences);

			
		}
		return ret;
	}
	private static void recursiveProduceCombinations(final List<List<Pair<String,String>>> combinations, final List<String> keysAsList, final Map<String,Set<String>> keys, final Map<String,String> combination, final Map<String,Integer> numOccurrences, Map<String,Integer> maxNumOccurrences) {
		for (String key:keysAsList) {
			if (combination.containsKey(key))
				continue;
			for (String keyOption: keys.get(key)) {
				combination.put(key, keyOption);
				recursiveProduceCombinations(combinations,keysAsList,keys,combination,numOccurrences,maxNumOccurrences);
				
				if (combination.size() == keys.size()) {
					List<Pair<String,String>> comboList = new ArrayList<Pair<String,String>>();
					for (String k:combination.keySet()) {
						if (numOccurrences.get(k+":"+combination.get(k))+1>maxNumOccurrences.get(k+":"+combination.get(k)))
							continue;
						comboList.add(new Pair<String,String>(k,combination.get(k)));
					}
					if (comboList.size() == keys.size()) {
						for (String k:combination.keySet()) {
							int num = numOccurrences.get(k+":"+combination.get(k));
							numOccurrences.put(k+":"+combination.get(k),num+1);
						}
						combinations.add(comboList);
					}
					combination.remove(key);
				}
				
			}
			combination.remove(key);
		}
	}
	/**
	 * removes from the input list of key-value pairs, all keys that appear only once and returns them in a separate list.
	 * @param keyValuePairs
	 * @return
	 */
	private static List<Pair<String, String>> destructivelyExtractUnique(List<Pair<String, String>> keyValuePairs) {
		List<Pair<String, String>> unique=null;
		if (keyValuePairs!=null) {
			Set<String> seenKeys=new HashSet<String>();
			Set<String> multiple=null;
			// find the duplicates
			for(Pair<String,String> kv:keyValuePairs) {
				String key=kv.getFirst();
				if (seenKeys.contains(key)) {
					if (multiple==null) multiple=new HashSet<String>();
					multiple.add(key);
				}
				else seenKeys.add(key);
			}
			//remove the unique keys form the input and return them in a separate list.
			if (multiple!=null && !multiple.isEmpty()) {
				Iterator<Pair<String,String>> it=keyValuePairs.iterator();
				while(it.hasNext()) {
					Pair<String,String> kv=it.next();
					String key=kv.getFirst();
					if (!multiple.contains(key)) {
						it.remove();
						if (unique==null) unique=new ArrayList<Pair<String,String>>();
						unique.add(kv);
					}
				}
			} else 
				return keyValuePairs;
			
		}
		return unique;
	}

	public static List<Pair<String, String>> getPairsFromString(String id) {
		List<Pair<String,String>> keyValuePairs=null;
		if (id!=null) {
			String[] parts=id.split("[\\s]+");
			if (parts!=null) {
				for(String p:parts) {
					Matcher m=nluPart.matcher(p);
					if (m.matches() && m.groupCount()==2) {
						if (keyValuePairs==null) keyValuePairs=new ArrayList<Pair<String,String>>();
						keyValuePairs.add(new Pair<String,String>(m.group(1), m.group(2)));
					}
				}
			}
		}
		return keyValuePairs;
	}

	@Override
	public String getId() {
		if (parts!=null) {
			try {
				return FunctionalLibrary.printCollection(parts.keySet(), "", "", " ");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public LinkedHashMap<String, List<Triple<Integer, String, Float>>> getParts() {
		return parts;
	}
	public void setParts(LinkedHashMap<String, List<Triple<Integer, String, Float>>> parts) {
		this.parts = parts;
	}
	
	@Override
	public String toString() {
		if (parts!=null) {
			StringBuffer ret=new StringBuffer();
			ret.append("<"+((getNluID()!=null)?getNluID()+" ":""));
			boolean first=true;
			for(String nlu:parts.keySet()) {
				List<Triple<Integer, String,Float>> input = parts.get(nlu);
				try {
					String range=FunctionalLibrary.printCollection(input, Triple.class.getMethod("getFirst"), "<", ">", ",");
					String text=FunctionalLibrary.printCollection(input, Triple.class.getMethod("getSecond"), "", "", " ");
					float w=0;
					for(Triple<Integer, String,Float> i:input) w+=i.getThird();
					ret.append((first?"":" ")+nlu+"("+range+":"+text+"["+w+"])");
					first=false;
				} catch (Exception e) {e.printStackTrace();}
			}
			ret.append(">");
			ret.append(getProb().getResult());
			return ret.toString();
		}
		return null;
	}
}
