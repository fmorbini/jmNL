package edu.usc.ict.nl.nlu.wikidata;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.usc.ict.nl.nlu.wikidata.WikiThing.TYPE;
import edu.usc.ict.nl.nlu.wikidata.utils.JsonUtils;
import edu.usc.ict.nl.util.ProgressTracker;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.graph.Node;

public class Wikidata {
	
	public static JSONObject getWikidataContentForSpecificEntityOnly(WikiLanguage lang,String id) {
		JSONObject desc = Queries.getWikidataContentForEntitiesRaw(lang,id);
		if (desc!=null) {
			List<JSONObject> entities = getEntities(desc);
			for(JSONObject e:entities) {
				if (JsonUtils.get(e, "id").equals(id)) return e;
			}
		}
		return null;
	}


	public static String prettyPrintWikidataContent(JSONObject t,WikiLanguage lang) {
		StringBuffer ret=new StringBuffer();
		if (t!=null && t instanceof JSONObject) {
			String name=(String) JsonUtils.get((JSONObject) t,"id");
			ret.append("id: "+name+"\n");
			List<String> labels=getLabelsForContent(t,lang);
			ret.append("labels: "+labels);
			String descriptions=getDescriptionForContent(t,lang);
			ret.append("descriptions: "+descriptions);
			Map<String, List<WikiClaim>> claims = getClaims(name,t);
			if (claims!=null) {
				for(String key:claims.keySet()) {
					System.out.println(key+" "+getDescriptionOfWikidataId(lang,key));
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
	public static List<WikiThing> getIdsForString(String searchString,WikiLanguage lang,WikiThing.TYPE type) {
		List<WikiThing> ret=null;
		JSONObject eso = Queries.getThingForDescription(searchString,lang,type);
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
	public static String getDescriptionOfWikidataId(WikiLanguage lang,String id) {
		JSONObject r=getWikidataContentForSpecificEntityOnly(lang,id);
		return getDescriptionForContent(r,lang);
	}
	public static String getDescriptionForContent(JSONObject r,WikiLanguage lang) {
		StringBuffer ret=null;
		if (r!=null) {
			List<Object> descriptions=JsonUtils.getAll(r,"descriptions",lang.getLcode(),"value");
			if (descriptions!=null) {
				for(Object d:descriptions) {
					if (ret==null) ret=new StringBuffer();
					ret.append((ret.length()==0?"":"| ")+d);
				}
			}
		}
		return ret!=null?ret.toString():null;
	}

	public static List<String> getLabelsForWikidataId(WikiLanguage lang,String id) {
		JSONObject r=getWikidataContentForSpecificEntityOnly(lang,id);
		return getLabelsForContent(r,lang);
	}
	public static List<String> getLabelsForContent(JSONObject r,WikiLanguage lang) {
		List<String> ret=null;
		if (r!=null) {
			List<Object> descriptions=JsonUtils.getAll(r,"labels",lang.getLcode(),"value");
			if (descriptions!=null) {
				for(Object d:descriptions) {
					if (ret==null) ret=new ArrayList<String>();
					ret.add((String) d);
				}
			}
		}
		return ret;
	}
	public static List<String> getAliasesForWikidataId(WikiLanguage lang,String id) {
		JSONObject r=getWikidataContentForSpecificEntityOnly(lang,id);
		return getAliasesForContent(r,lang);
	}
	public static List<String> getAliasesForContent(JSONObject r,WikiLanguage lang) {
		List<String> ret=null;
		if (r!=null) {
			List<Object> aliases=JsonUtils.getAll(r,"aliases",lang.getLcode(),"value");
			if (aliases!=null) {
				for(Object d:aliases) {
					if (ret==null) ret=new ArrayList<String>();
					ret.add((String) d);
				}
			}
		}
		return ret;
	}

	public static Map<String,List<WikiClaim>> getClaims(JSONObject e) {
		String name=(String) JsonUtils.get((JSONObject) e,"id");
		return getClaims(name, e);
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
						if (value!=null) {
							if (ret==null) ret=new HashMap<String, List<WikiClaim>>();
							List<WikiClaim> list=ret.get(key);
							if (list==null) ret.put(key, list=new ArrayList<WikiClaim>());
							list.add(new WikiClaim(subject,key,value));
						}
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
			String valuetype=(String) JsonUtils.get(main, "datatype");
			if (valuetype==null) valuetype=(String) JsonUtils.get(dv, "type");
			String snaktype=(String) JsonUtils.get(main, "snaktype");
			if (snaktype.equals("value")) {
				if (valuetype!=null) {
					if (valuetype.equals("wikibase-item")) {
						String id=JsonUtils.get(dv,"value","numeric-id").toString();
						return "Q"+id;
					} else if (valuetype.equals("wikibase-property")) {
						String id=JsonUtils.get(dv,"value","numeric-id").toString();
						return "P"+id;
					} else if (valuetype.equals("string") || valuetype.equals("commonsMedia") || valuetype.equals("url")) return (String) JsonUtils.get(dv, "value");
					else if (valuetype.equals("time")) {
						//System.out.println(JsonUtils.get(dv, "value"));
						return JsonUtils.get(dv, "value").toString();
					} else {
						return JsonUtils.get(dv, "value").toString();
					}
				} else {
					System.err.println("null type: "+main);
				}
			}
		}
		return null;
	}

	public static List<WikiThing> findAllItemsThatAre(WikiThing type,WikiLanguage lang) {
		return runQuery("claim["+"31"+":(tree["+type.getId()+"][][279])]", lang);
	}
	public static List<WikiThing> runQuery(String query,WikiLanguage lang) {
		List<WikiThing> ret=null;
		JSONObject result = Queries.runWikidataQuery(query,lang);
		if (result!=null) {
			Object r = JsonUtils.get(result, "items");
			if (r!=null && r instanceof JSONArray) {
				int l=((JSONArray)r).length();
				for(int i=0;i<l;i++) {
					Object item;
					try {
						item = ((JSONArray)r).get(i);
						if (item!=null && item instanceof Integer) {
							if (ret==null) ret=new ArrayList<WikiThing>();
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

	public static void dumpInstancesForType(String type,WikiLanguage searchLang,WikiLanguage outputLang,String filePrefix) {
		List<WikiThing> ids = getIdsForString(type,searchLang,WikiThing.TYPE.ITEM);
		if (ids!=null && !ids.isEmpty()) {
			for(WikiThing id:ids)
				try {
					System.out.println(id.toString(WikiLanguage.get("en"), true));
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			long id=-1;
			if (ids.size()>1) {
				try{
					BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
					String input;
					while(true){
						System.out.println("Select an ID to use (just the number, not the P or Q): ");
						if ((input=br.readLine())!=null) {
							try{
								id=Long.parseLong(input);
								boolean found=false;
								for(WikiThing i:ids) if (i.getId()==id) {
									found=true;
									break;
								}
								if (found) break;
								else {
									System.err.println("number not found.");
								}
							}catch(Exception e){
								System.err.println("input a number.");
							}
						} else break;
					}
				}catch(Exception io){
					io.printStackTrace();
				}
			} else {
				id=ids.get(0).getId();
			}
			dumpAllItemsToFile(new WikiThing(id, TYPE.ITEM),outputLang,new File(filePrefix+"_"+id));
		} else {
			System.err.println("no items found with search string: "+type);
		}
	}

	private static void dumpAllItemsToFile(WikiThing thing, WikiLanguage lang,File file) {
		List<WikiThing> items = findAllItemsThatAre(thing,lang);
		if (items!=null) {
			Set<String> things=null;
			int size=items.size();
			ProgressTracker pt = new ProgressTracker(1, size, System.out);
			int j=1;
			for(WikiThing i:items) {
				List<String> labels=getLabelsForWikidataId(lang,i.toString(lang,false));
				if (labels!=null) {
					for(String l:labels) {
						if (!StringUtils.isEmptyString(l)) {
							if (things==null) things=new HashSet<String>();
							things.add(l);
						}
					}
				}
				pt.update(j++);
			}
			try {
				if (things!=null && !things.isEmpty()) {
					BufferedWriter out=new BufferedWriter(new FileWriter(file));
					List<String> sortedThings=new ArrayList<String>(things);
					Collections.sort(sortedThings);
					for(String l:things) {
						out.write(l+"\n");
						out.flush();
					}
					out.close();
				}
			} catch (Exception e) {e.printStackTrace();}
		}
	}

	public static Set<WikiThing> getAllPropertiesThatHaveClaimsContainsThisItem() {
		
		return null;
	}
	
	public static Node buildPropertyTree(String id,String pname) throws Exception {
		Node root=new Node(id+"_"+pname);
		WikiThing current=new WikiThing(id);
		root.addEdgeTo(current, true, true);
		Deque<WikiThing> toProcess=new LinkedList<WikiThing>();
		toProcess.push(current);
		while(!toProcess.isEmpty()) {
			current=toProcess.pop();
			JSONObject content = getWikidataContentForSpecificEntityOnly(WikiLanguage.EN, current.getName());
			Map<String, List<WikiClaim>> claims = getClaims(content);
			if (claims!=null && claims.containsKey(pname)) {
				List<WikiClaim> pcls = claims.get(pname);
				if (pcls!=null) {
					root.removeEdgeTo(current);
					for(WikiClaim pcl:pcls) {
						String object = pcl.getObject();
						WikiThing n=new WikiThing(object);
						toProcess.add(n);
						root.addEdgeTo(n, true, true);
						n.addEdgeTo(current, true, true);
					}
				}
			}
		}
		return root;
	}
	
	public static void main(String[] args) throws Exception {
		JSONObject r = getWikidataContentForSpecificEntityOnly(WikiLanguage.EN, "Q200572");
		Map<String, List<WikiClaim>> cls = getClaims(r);
		System.out.println(cls);
		//Node root=buildPropertyTree("Q18216", "P279");
		//Node root=buildPropertyTree("Q11173", "P171");
		//root.toGDLGraph(root.getName()+".gdl");
		//System.out.println(prettyPrintWikidataContent(getWikidataContentForSpecificEntityOnly(WikiLanguage.get("en"), "Q18216"), WikiLanguage.get("en")));
		//JSONObject content = getWikidataContentForSpecificEntityOnly("Q2");
		//String lbs = getLabelsForWikidataId("Q2");
		//System.out.println(lbs);
		List<WikiThing> ids = getIdsForString("main actor",WikiLanguage.EN,WikiThing.TYPE.PROPERTY);
		System.out.println(ids);

		//dumpInstancesForType("aspirin",WikiLanguage.get("en"),WikiLanguage.get("en"),"nes");
	}

}