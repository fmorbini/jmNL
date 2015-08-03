package edu.usc.ict.nl.nlu.wikidata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.usc.ict.nl.nlu.wikidata.WikiThing.TYPE;
import edu.usc.ict.nl.nlu.wikidata.utils.JsonUtils;

public class Wikidata {
	public static JSONObject getWikidataContentForSpecificEntityOnly(String id) {
		JSONObject desc = Queries.getWikidataContentForEntitiesRaw(id);
		if (desc!=null) {
			List<JSONObject> entities = getEntities(desc);
			for(JSONObject e:entities) {
				if (JsonUtils.get(e, "id").equals(id)) return e;
			}
		}
		return null;
	}


	public static String prettyPrintWikidataContent(JSONObject t) {
		StringBuffer ret=new StringBuffer();
		if (t!=null && t instanceof JSONObject) {
			String name=(String) JsonUtils.get((JSONObject) t,"id");
			ret.append("id: "+name+"\n");
			String labels=getLabelsForContent(t);
			ret.append("labels: "+labels);
			String descriptions=getDescriptionForContent(t);
			ret.append("descriptions: "+descriptions);
			Map<String, List<WikiClaim>> claims = getClaims(name,t);
			if (claims!=null) {
				for(String key:claims.keySet()) {
					System.out.println(key+" "+getDescriptionOfWikidataId(key));
					Collection<WikiClaim> claimsForP = claims.get(key);
					for (WikiClaim p:claimsForP) {
						System.out.println(p);
					}
				}
			}

		}
		return (ret.length()==0)?null:ret.toString();
	}


	private static List<JSONObject> getEntities(JSONObject json) {
		List<JSONObject> ret=null;
		if (json!=null && json instanceof JSONObject) {
			List<JSONObject> things = new ArrayList<JSONObject>();
			JsonUtils.addThings(things,JsonUtils.get(json,"entities"));
			for(Object x:things) {
				if (x!=null && x instanceof JSONObject) {
					Iterator it=((JSONObject)x).keys();
					while(it.hasNext()) {
						Object key = it.next();
						Object value = JsonUtils.get((JSONObject) x,(String) key);
						if (value!=null && value instanceof JSONObject) {
							try {
								if (((JSONObject)value).has("id") && ((JSONObject)value).get("id").equals(key)) {
									if (ret==null) ret=new ArrayList<JSONObject>();
									ret.add((JSONObject)value);
								} else {
									System.err.println("entity with key "+key+" different from id "+((JSONObject)value).get("id"));
								}
							} catch (Exception e) {}
						}
					}
				}
			}
		}
		return ret;
	}

	/**
	 * returns a list of wikidata IDs for a given search string. 
	 * @param searchString
	 * @return
	 */
	public static List<WikiThing> getIdsForString(String searchString,WikiThing.TYPE type) {
		List<WikiThing> ret=null;
		JSONObject eso = Queries.getThingForDescription(searchString,type);
		Collection<JSONObject> things = JsonUtils.addThings(JsonUtils.get(eso,"search"));
		if (things!=null) {
			for(JSONObject e:things) {
				Object thing = JsonUtils.get(e, "id");
				if (thing!=null) {
					if (thing instanceof Integer) {
						if (ret==null) ret=new ArrayList<WikiThing>();
						ret.add(new WikiThing(((Integer)thing).longValue(), type));
					} else if (thing instanceof String) {
						if (ret==null) ret=new ArrayList<WikiThing>();
						try {
							ret.add(new WikiThing(thing.toString()));
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}
					
				}
			}
		}
		return ret;
	}
	public static String getDescriptionOfWikidataId(String id) {
		JSONObject r=getWikidataContentForSpecificEntityOnly(id);
		return getDescriptionForContent(r);
	}
	public static String getDescriptionForContent(JSONObject r) {
		StringBuffer ret=null;
		if (r!=null) {
			List<Object> descriptions=JsonUtils.getAll(r,"descriptions","en","value");
			if (descriptions!=null) {
				for(Object d:descriptions) {
					if (ret==null) ret=new StringBuffer();
					ret.append((ret.length()==0?"":"| ")+d);
				}
			}
		}
		return ret!=null?ret.toString():null;
	}
	
	public static String getLabelsForWikidataId(String id) {
		JSONObject r=getWikidataContentForSpecificEntityOnly(id);
		return getLabelsForContent(r);
	}
	public static String getLabelsForContent(JSONObject r) {
		StringBuffer ret=null;
		if (r!=null) {
			List<Object> descriptions=JsonUtils.getAll(r,"labels","en","value");
			if (descriptions!=null) {
				for(Object d:descriptions) {
					if (ret==null) ret=new StringBuffer();
					ret.append((ret.length()==0?"":"| ")+d);
				}
			}
		}
		return ret!=null?ret.toString():null;
	}

	public static Map<String,List<WikiClaim>> getClaims(String subject,JSONObject e) {
		Map<String,List<WikiClaim>> ret=null;
		JSONObject claims=(JSONObject) JsonUtils.get(e, "claims");
		if (claims!=null) {
			Iterator<String> it = claims.keys();
			while(it.hasNext()) {
				String key=it.next();
				Collection<JSONObject> claimsForP = JsonUtils.addThings(JsonUtils.get(claims, key));
				if (claimsForP!=null) {
					for(JSONObject c:claimsForP) {
						String value=getValueOfMainSnak(c);
						if (ret==null) ret=new HashMap<String, List<WikiClaim>>();
						List<WikiClaim> list=ret.get(key);
						if (list==null) ret.put(key, list=new ArrayList<WikiClaim>());
						list.add(new WikiClaim(subject,key,value));
					}
				}
			}
		}
		return ret;
	}
	
	public static String getValueOfMainSnak(JSONObject p) {
		JSONObject main=(JSONObject) JsonUtils.get(p, "mainsnak");
		if (main!=null) {
			JSONObject dv=(JSONObject) JsonUtils.get(main, "datavalue");
			String type=(String) JsonUtils.get(main, "datatype");
			if (type!=null) {
				if (type.equals("wikibase-item")) {
					String id=JsonUtils.get(dv,"value","numeric-id").toString();
					return "Q"+id;
				} else if (type.equals("wikibase-property")) {
					String id=JsonUtils.get(dv,"value","numeric-id").toString();
					return "P"+id;
				} else if (type.equals("string") || type.equals("commonsMedia") || type.equals("url")) return (String) JsonUtils.get(dv, "value");
				else if (type.equals("time")) {
					System.out.println(JsonUtils.get(dv, "value"));
					return (String) JsonUtils.get(dv, "value","time");
				} else {
					System.err.println("unknown type: "+main);
				}
			} else {
				System.err.println("null type: "+main);
			}
		}
		return null;
	}
	
	public static Set<WikiThing> findAllItemsThatAre(WikiThing type) {
		Set<WikiThing> ret=null;
		JSONObject result = Queries.runWikidataQuery("claim["+"31"+":(tree["+type.getId()+"][][279])]");//claim[31:(tree[3314483][][279])]
		if (result!=null) {
			Object r = JsonUtils.get(result, "items");
			if (r!=null && r instanceof JSONArray) {
				int l=((JSONArray)r).length();
				for(int i=0;i<l;i++) {
					Object item;
					try {
						item = ((JSONArray)r).get(i);
						if (item!=null && item instanceof Integer) {
							if (ret==null) ret=new HashSet<WikiThing>();
							ret.add(new WikiThing(((Integer)item).longValue(),TYPE.ITEM));
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return ret;
	}
	
	public static void main(String[] args) {
		//List<WikiThing> ids = getIdsForString("earth",WikiThing.TYPE.ITEM);
		//JSONObject content = getWikidataContentForSpecificEntityOnly("Q2");
		//String lbs = getLabelsForWikidataId("Q2");
		//System.out.println(lbs);
		List<WikiThing> ids = getIdsForString("fruit",WikiThing.TYPE.ITEM);
		System.out.println(ids);

		Set<WikiThing> items = findAllItemsThatAre(new WikiThing(1364, TYPE.ITEM));
		System.out.println(items);
	}

}