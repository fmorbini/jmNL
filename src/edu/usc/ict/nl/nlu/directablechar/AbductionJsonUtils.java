package edu.usc.ict.nl.nlu.directablechar;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.usc.ict.nl.util.FileUtils;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;

public class AbductionJsonUtils {

	public enum LANG {EN,ES,FA,RU};

	public static final File lccFile=new File("data/Democracy_LM_Examples_NEW.xlsx");
	public static final String utterancesKey="metaphorAnnotationRecords";
	public static final String abductionKey="isiAbductiveExplanation";
	public static final String sentenceIdKey="sentenceId";
	public static final String lmKey="linguisticMetaphor";
	public static final String kbKey="kb";
	public static final String kbContentKey="kb_content";
	public static final String targetConceptSuperDomainKey="targetConceptDomain";
	public static final String targetDomainKey="targetConceptSubDomain";
	public static final String sourceDomainKey="sourceFrame";
	public static final String stepKey="step";
	public static final String languageKey="language";
	public static final String depthKey="depth";
	public static final String doGraphKey="dograph";
	public static final String extractorKey="extractor";
	public static final String parserOutputKey="parser_output";
	
	private URL url=null;
	private Integer defaultMaxDepth=3;

	
	public AbductionJsonUtils(String url) throws MalformedURLException {
		this.url=new URL(url);
	}
	
	public JSONObject buildJsonForRequest(String extractor,boolean doGraph,Integer maxDepth,String kb,LANG lang,String... us) throws Exception {
		return buildJsonForRequest(extractor,doGraph,maxDepth,kb,lang, null, us);
	}
	public JSONObject buildJsonForRequest(String extractor,boolean doGraph,Integer maxDepth,StringBuffer kbContent,LANG lang,String... us) throws Exception {
		return buildJsonForRequest(extractor,doGraph,maxDepth,kbContent,lang, null, us);
	}
	private JSONObject buildJsonForRequest(String extractor,boolean doGraph,Integer maxDepth,String kb,LANG lang,Map<String,Integer> index,String... us) throws Exception {
		JSONObject json=buildcommonRequest(extractor,doGraph,maxDepth,lang, index, us);
		if (kb!=null) json.put(kbKey, kb);
		return json;
	}
	private JSONObject buildJsonForRequest(String extractor,boolean doGraph,Integer maxDepth,StringBuffer kbContent,LANG lang,Map<String,Integer> index,String... us) throws Exception {
		JSONObject json=buildcommonRequest(extractor,doGraph,maxDepth,lang, index, us);
		if (kbContent!=null) json.put(kbContentKey, kbContent.toString());
		return json;
	}
	public JSONObject buildcommonRequest(LANG lang,Map<String,Integer> index,String... us) throws Exception {
		return buildcommonRequest(null,false,defaultMaxDepth, lang, index, us);
	}
	public JSONObject buildcommonRequest(String extractor,boolean doGraph,Integer maxDepth,LANG lang,Map<String,Integer> index,String... us) throws Exception {
		JSONObject json=new JSONObject();
		json.put(stepKey, 3);
		if (maxDepth==null || maxDepth<=0) maxDepth=defaultMaxDepth;
		json.put(depthKey, maxDepth.toString());
		json.put(doGraphKey, doGraph);
		if (!StringUtils.isEmptyString(extractor)) json.put(extractorKey, extractor);
		json.put(languageKey, lang.toString().toUpperCase());
		if (us!=null) {
			for(String u:us) {
				int v=addUtteranceToJson(json,u);
				if (index!=null) index.put(u,v);
			}
		}
		return json;
	}
	private int addUtteranceToJson(JSONObject json, String u) throws Exception {
		Object array=null;
		if (json.has(utterancesKey)) array = json.get(utterancesKey);
		if (array==null) {
			json.put(utterancesKey, array=new JSONArray());
		} else if (array instanceof JSONArray) {
		} else throw new Exception("invalid content for key '"+utterancesKey+"': "+array);
		JSONArray a=(JSONArray) array;
		int max=1;
		for(int i=0;i<a.length();i++) {
			JSONObject o=(JSONObject) a.get(i);
			o.put(parserOutputKey, "");
			int v=(Integer) o.get(sentenceIdKey);
			if (v>=max) max=v+1;
		}
		JSONObject uo=buildJsonForUtterance(u,max);
		a.put(uo);
		return max;
	}
	public int addUtteranceToJsonRemoveAllAlreadyThere(JSONObject json, String u) throws Exception {
		json.put(utterancesKey, new JSONArray());
		return addUtteranceToJson(json, u);
	}
	private JSONObject buildJsonForUtterance(String u, int index) throws JSONException {
		JSONObject o=new JSONObject();
		o.put(sentenceIdKey, index);
		o.put(lmKey,u);
		return o;
	}
	
	public JSONObject sendRequest(JSONObject o) throws Exception {
		byte[] data=o.toString().getBytes("UTF-8");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();           
		connection.setDoOutput(true);
		connection.setDoInput(true);
		connection.setInstanceFollowRedirects(false); 
		connection.setRequestMethod("POST"); 
		connection.setRequestProperty("Content-Type", "application/json"); 
		connection.setRequestProperty("Accept", "application/json");
		connection.setRequestProperty("charset", "utf-8");
		connection.setRequestProperty("Content-Length", "" + data.length);
		connection.setUseCaches (false);

		DataOutputStream wr = new DataOutputStream(connection.getOutputStream ());
		wr.write(data);
		wr.flush();
		wr.close();
		
		BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream ()));
	    String str;
	    StringBuffer response=new StringBuffer();
	    while (null != ((str = input.readLine()))) {
	    	response.append(str);
	    }
	    JSONObject json = new JSONObject(response.toString());
	    return json;
	}
	public String getAbductionForPositionInJsonReply(JSONObject reply,int position) throws JSONException {
		JSONArray utterances=getUtterances(reply);
		if (utterances!=null && utterances.length()>position) {
			JSONObject uo=utterances.getJSONObject(position);
			return uo.get(abductionKey).toString();
		}
		return null;
	}
	public String getAbductionForIdInJsonReply(JSONObject reply,int searchThisId) throws JSONException {
		JSONArray utterances=getUtterances(reply);
		if (utterances!=null) {
			int max=utterances.length();
			for(int i=0;i<max;i++) {
				JSONObject uo=utterances.getJSONObject(i);
				int id=(Integer) uo.get(sentenceIdKey);
				if (id==searchThisId) return uo.getString(abductionKey);
			}
		}
		return null;
	}
	public static JSONArray getUtterances(JSONObject reply) throws JSONException {
		return (JSONArray) reply.get(utterancesKey);
	}
	public static List<Literal> extractParsingPart(JSONObject uo) throws Exception {
		List<Literal> ret=null;
		if (uo.has(abductionKey)) {
			String abduction=uo.getString(abductionKey);
			String[] parts=StringUtils.cleanupSpaces(abduction).split("[\\s]+");
			if (parts!=null) {
				for(String p:parts) {
					//if (!Character.isUpperCase(p.charAt(0))) {
						if (ret==null) ret=new ArrayList<Literal>();
						try {
						Literal l=Literal.parse(p);
						ret.add(l);
						} catch (Exception e) {
							
						}
					//}
				}
			}
		}
		return ret;
	}
	private static boolean outputGood(JSONObject output, boolean printError) {
		if (output!=null) {
			if (output.has(lmKey)) {
				if (output.has(abductionKey)) {
					if (output.has(targetDomainKey)) {
						if (output.has(sourceDomainKey)) {
							return true;
						} else {
							if (printError) System.err.println("Missing '"+sourceDomainKey+"'");
						}
					} else {
						if (printError) System.err.println("Missing '"+targetDomainKey+"'");
					}
				} else {
					if (printError) System.err.println("Missing '"+abductionKey+"'");
				}
			} else {
				if (printError) System.err.println("Missing '"+lmKey+"'");
			}
		} else {
			if (printError) System.err.println("null json output.");
		}
		return false; 
	}

	/**
	 * returns a pair of two lists, the first list contains the predicates that match the second phrase argument.
	 * The second list ar ethe variables that are unique associated with these predicates.
	 * @param rawPredicates
	 * @param phrase
	 * @return
	 * @throws Exception
	 */
	private Pair<Set<String>,List<Literal>> findPredicatesForWords(List<Literal> rawPredicates, String[] phrase) throws Exception {
		Map<String,List<Literal>> predicates=processRawPredicates(rawPredicates);
		List<Literal> ret=null;
		if (phrase!=null && predicates!=null) {
			for(String p:phrase) {
				p=p.toLowerCase();
				List<Literal> literals=specialGet(p,predicates);
				if (literals!=null && literals.size()==1) {
					if (ret==null) ret=new ArrayList<Literal>();
					ret.add(literals.get(0));
				}
			}
		}
		Set<String> vars=findIdentifyingVars(ret,predicates);
		return new Pair<Set<String>, List<Literal>>(vars, ret);
	}
	private List<Literal> specialGet(String pname,Map<String, List<Literal>> predicates) {
		if (predicates!=null) {
			if (predicates.containsKey(pname)) return predicates.get(pname);
			else {
				int l=pname.length();
				for (int i=1;i<Math.min(2, l-1);i++) {
					String pnameSub=pname.substring(0, l-i);
					if (predicates.containsKey(pnameSub)) return predicates.get(pnameSub);
				}
			}
		}
		return null;
	}
	private static Set<String> findIdentifyingVars(List<Literal> ret,Map<String, List<Literal>> predicates) {
		if (ret!=null) {
			if (ret.size()==1) {
				return new HashSet<String>(ret.get(0).getArgs());
			} else {
				for(Literal p:ret) {
					return new HashSet<String>(ret.get(0).getArgs());
				}
			}
		}
		//Map<String,Set<String>> varIndex=createVarIndex(predicates);
		return null;
	}
	/**
	 * 
	 * @param varIndex <variable, list of literals that use that variable>
	 * @return <
	 */
	/*
	private static Map<String, List<String>> inverseVarIndex(Map<String, Set<String>> varIndex) {
		Map<String, List<String>> ret=null;
		if (varIndex!=null) {
			for(String v:varIndex.keySet()) {
				List<String> literals=new ArrayList<String>(varIndex.get(v));
				Collections.sort(literals);
				if (ret==null) ret=new HashMap<String, List<String>>();
				List<String> varsForLiterals
			}
		}
		return ret;
	}
	private static Map<String, Set<String>> createVarIndex(Map<String, List<String>> predicates) {
		Map<String,Set<String>> ret=null;
		if (predicates!=null) {
			for(List<String> literals:predicates.values()) {
				if (literals!=null) {
					for(String l:literals) {
						Matcher m=pp.matcher(l);
						if (m.matches()) {
							String[] args=m.group(3).split(",");
							for(String a:args) {
								a=StringUtils.cleanupSpaces(a);
								if (ret==null) ret=new HashMap<String, Set<String>>();
								Set<String> varsUsedWithThisArg = ret.get(a);
								if (varsUsedWithThisArg==null) ret.put(a,varsUsedWithThisArg=new HashSet<String>());
								varsUsedWithThisArg.addAll(Arrays.asList(args));
							}
						}
					}
				}
			}
		}
		return ret;
	}*/
	private Map<String, List<Literal>> processRawPredicates(List<Literal> rawPredicates) throws Exception {
		Map<String, List<Literal>> ret=null;
		if (rawPredicates!=null) {
			for(Literal p:rawPredicates) {
				if (ret==null) ret=new HashMap<String, List<Literal>>();
				String justP=p.getJustP();
				List<Literal> argForP=ret.get(justP);
				if (argForP==null) ret.put(justP,argForP=new ArrayList<Literal>());
				argForP.add(p);
			}
		}
		return ret;
	}
	private String generateNewVar(Set<String> vars) {
		String base="u";
		int b=0;
		String vname=base+b;
		if (vars!=null && !vars.isEmpty()) {
			while(vars.contains(vname)) {
				b++;
				vname=base+b;
			}
		}
		return vname;
	}
	private Set<String> exractAllVars(List<Literal> lf) {
		Set<String> ret=null;
		if (lf!=null) {
			for(Literal l:lf) {
				List<String> lvs=l.getArgs();
				if (lvs!=null && !lvs.isEmpty()) {
					if (ret==null) ret=new HashSet<String>();
					for(String v:lvs) {
						ret.add(v);
					}
				}
			}
		}
		return ret;
	}
	
}
