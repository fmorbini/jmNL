package edu.usc.ict.nl.nlu.multi.merger;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import edu.usc.ict.nl.nlu.ChartNLUOutput;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.Triple;

public class Cascade implements Merger {
	
	private Map<String,Map<String,String>> links=null;
	private String firstClassifierName=null;
	
	/**
	 * the input is a list of triples. each triple <c,r,nc> indicates
	 * that is classifier named c returns r then we need to use the result
	 * from classifier named nc
	 * 
	 * to be consistent a triple cannot be two or more triples with equal c and r parts but different nc parts.
	 * 
	 * @param args
	 * @throws Exception 
	 */
	public Cascade(String firstClassifier,List<Triple<String,String,String>> args) throws Exception {
		this.firstClassifierName=firstClassifier;
		if (StringUtils.isEmptyString(firstClassifierName)) throw new Exception("Empty or null first classifier specified.");
		if (args!=null) {
			for(Triple<String, String, String> l:args) {
				String c=l.getFirst();
				if (isConsistent(c,l.getSecond(),l.getThird())) {
					if (links==null) links=new LinkedHashMap<String, Map<String,String>>();
					Map<String,String> olinks=links.get(c);
					if (olinks==null) links.put(c, olinks=new LinkedHashMap<String,String>());
					olinks.put(l.getSecond(), l.getThird());
				} else {
					throw new Exception("Inconsistent link in merger: "+args);
				}
			}
		}
	}
	
	private boolean isConsistent(String sc,String scc,String oc) {
		if (links!=null) {
			Map<String, String> olink = links.get(sc);
			if (olink!=null && olink.get(scc)!=null && !olink.get(scc).equals(oc)) return false;
		}
		return true;
	}

	@Override
	public String getNluNameToBeCalledForThisNluAndResult(String nluName,String result) {
		if (links!=null) {
			Map<String,String> olinks=links.get(nluName);
			if (olinks!=null) return olinks.get(result);
		}
		return null;
	}

	@Override
	public ChartNLUOutput mergeResults(ChartNLUOutput result) throws Exception {
		Map<String,NLUOutput> inputResults=null;
		if (result!=null && result.getPortions()!=null) {
			for(Triple<Integer, Integer, NLUOutput> p:result.getPortions()) {
				NLUOutput nlu=p.getThird();
				String nluID=nlu.getNluID();
				if (inputResults==null) inputResults=new HashMap<String, NLUOutput>();
				inputResults.put(nluID, nlu);

			}
			String c=firstClassifierName;
			NLUOutput finalResult=null;
			while(c!=null) {
				NLUOutput resultForC = inputResults.get(c);
				if (resultForC!=null) {
					finalResult=resultForC;
					c=getNluNameToBeCalledForThisNluAndResult(c, resultForC.getId());
				} else {
					//failure in cascade, return nothing.
					finalResult=null;
					break;
				}
			}
			if (finalResult!=null) {
				result=new ChartNLUOutput(result.getText(),null);
				result.addPortion(0, 0, finalResult);
			} else result=null;
		}
		return result;
	}
}
