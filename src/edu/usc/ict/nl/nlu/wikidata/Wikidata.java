package edu.usc.ict.nl.nlu.wikidata;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import edu.usc.ict.nl.nlu.wikidata.utils.JsonUtils;
import edu.usc.ict.nl.util.FunctionalLibrary;

public class Wikidata {
	public enum TYPE {PROPERTY,ITEM};

	public static JSONObject getThingForDescription(String description,TYPE type) {
		try {
			URI uri = new URI("https","www.wikidata.org","/w/api.php","action=wbsearchentities&search="+description+"&language=en&format=json&type="+type.toString().toLowerCase(),null);
			String request = uri.toASCIIString();
			URL url = new URL(request);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();           
			connection.setDoOutput(true);
			connection.setDoInput(true);
			connection.setConnectTimeout(1000);
			connection.setReadTimeout(2000);
			connection.setInstanceFollowRedirects(false); 
			connection.setRequestMethod("GET"); 
			connection.setUseCaches(false);

			InputStream is;
			int code=0;
			if ((code=connection.getResponseCode()) >= 400) {
				is = connection.getErrorStream();
				BufferedReader input=new BufferedReader(new InputStreamReader(is));
				StringBuffer msg=new StringBuffer();
				String line=null;
				while((line=input.readLine())!=null) {
					msg.append(line);
				}
				throw new Exception("response: "+code+". msg="+msg.toString());
			} else {
				is = connection.getInputStream();
				BufferedReader input = new BufferedReader(new InputStreamReader(is));
				String str;
				StringBuffer response=new StringBuffer();
				while (null != ((str = input.readLine()))) {
					response.append(str);
				}
				JSONObject json = new JSONObject(response.toString());
				if (json!=null && json.has("success") && json.get("success").equals(1))
					return json;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static JSONObject getWikidataContentForEntitiesRaw(String... ids) {
		if (ids!=null) {
			try {
				StringBuffer idsb=new StringBuffer();
				for(String id:ids) {
					if (idsb.length()==0)
						idsb.append(id);
					else
						idsb.append("|"+id);
				}
				URI uri = new URI("https","www.wikidata.org","/w/api.php","action=wbgetentities&ids="+idsb.toString()+"&languages=en&format=json",null);
				String request = uri.toASCIIString();
				URL url = new URL(request);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();           
				connection.setDoOutput(true);
				connection.setDoInput(true);
				connection.setConnectTimeout(1000);
				connection.setReadTimeout(2000);
				connection.setInstanceFollowRedirects(false); 
				connection.setRequestMethod("GET"); 
				connection.setUseCaches(false);

				InputStream is;
				int code=0;
				if ((code=connection.getResponseCode()) >= 400) {
					is = connection.getErrorStream();
					BufferedReader input=new BufferedReader(new InputStreamReader(is));
					StringBuffer msg=new StringBuffer();
					String line=null;
					while((line=input.readLine())!=null) {
						msg.append(line);
					}
					throw new Exception("response: "+code+". msg="+msg.toString());
				} else {
					is = connection.getInputStream();
					BufferedReader input = new BufferedReader(new InputStreamReader(is));
					String str;
					StringBuffer response=new StringBuffer();
					while (null != ((str = input.readLine()))) {
						response.append(str);
					}
					JSONObject json = new JSONObject(response.toString());
					if (json!=null && json.has("success") && json.get("success").equals(1))
						return json;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	public static JSONObject getWikidataContentForSpecificEntityOnly(String id) {
		JSONObject desc = getWikidataContentForEntitiesRaw(id);
		if (desc!=null) {
			List<JSONObject> entities = getEntities(desc);
			for(JSONObject e:entities) {
				if (JsonUtils.get(e, "id").equals(id)) return e;
			}
		}
		return null;
	}

	public static String getDescriptionForContent(JSONObject t) {
		StringBuffer ret=new StringBuffer();
		if (t!=null && t instanceof JSONObject) {
			List<String> descriptions=JsonUtils.getAllValuesForProperty(t,"descriptions","value");
			try {
				ret.append("descriptions: "+FunctionalLibrary.printCollection(descriptions, "", "", ", ")+"\n");
			} catch (Exception e) {}
		}
		return (ret.length()==0)?null:ret.toString();
	}
	public static String prettyPrintWikidataContent(JSONObject t) {
		StringBuffer ret=new StringBuffer();
		if (t!=null && t instanceof JSONObject) {
			String name=(String) JsonUtils.get((JSONObject) t,"id");
			ret.append("id: "+name+"\n");
			List<String> labels=JsonUtils.getAllValuesForProperty(t,"labels","value");
			try {
				ret.append("labels: "+FunctionalLibrary.printCollection(labels, "", "", ", ")+"\n");
			} catch (Exception e) {}
			List<String> descriptions=JsonUtils.getAllValuesForProperty(t,"descriptions","value");
			try {
				ret.append("descriptions: "+FunctionalLibrary.printCollection(descriptions, "", "", ", ")+"\n");
			} catch (Exception e) {}
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
	public static List<String> getIdsForMatchingEntities(String searchString) {
		List<String> ret=null;
		JSONObject eso = getThingForDescription(searchString,TYPE.ITEM);
		Collection<JSONObject> things = JsonUtils.addThings(JsonUtils.get(eso,"search"));
		if (things!=null) {
			for(JSONObject e:things) {
				Object thing = JsonUtils.get(e, "id");
				if (thing!=null) {
					if (ret==null) ret=new ArrayList<String>();
					ret.add(thing.toString());
				}
			}
		}
		return ret;
	}
	public static List<String> getIdsForMatchingProperties(String searchString) {
		List<String> ret=null;
		JSONObject eso = getThingForDescription(searchString,TYPE.PROPERTY);
		Collection<JSONObject> things = JsonUtils.addThings(JsonUtils.get(eso,"search"));
		if (things!=null) {
			for(JSONObject e:things) {
				Object thing = JsonUtils.get(e, "id");
				if (thing!=null) {
					if (ret==null) ret=new ArrayList<String>();
					ret.add(thing.toString());
				}
			}
		}
		return ret;
	}

	public static String getDescriptionOfWikidataId(String id) {
		StringBuffer ret=null;
		JSONObject r = getWikidataContentForEntitiesRaw(id);
		if (r!=null) {
			List<JSONObject> es = getEntities(r);
			if (es!=null) {
				if (es.size()==1) {
					List<String> descriptions=JsonUtils.getAllValuesForProperty(es.iterator().next(),"descriptions","value");
					if (descriptions!=null) {
						for(String d:descriptions) {
							if (ret==null) ret=new StringBuffer();
							ret.append((ret.length()==0?"":" ")+d);
						}
					}
				} else if (es.size()>1){
					System.err.println(id+" returned multiple entities: "+es);
				} else {
					System.err.println(id+" returned no entities");
				}
			} else {
				System.err.println(id+" returned no entities");
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
	
	public static void main(String[] args) {
		//System.out.println(getDescriptionOfWikidataId("Q195"));
		//System.out.println(getDescriptionOfWikidataId("P143"));

		//List<String> ids = getIdsForMatchingEntities("country");
		List<String> ids = getIdsForMatchingProperties("country");
		//List<String> ids = getIdsForMatchingProperties("author");
		if (ids!=null) {
			for(String id:ids) {
				JSONObject e = getWikidataContentForSpecificEntityOnly(id);
				Map<String, List<WikiClaim>> claims = getClaims(id,e);
				System.out.println(getDescriptionForContent(e));
				if (claims!=null) {
					for(String key:claims.keySet()) {
						System.out.println(key+" "+getDescriptionOfWikidataId(key));
						List<WikiClaim> claimsForP = claims.get(key);
						for (WikiClaim p:claimsForP) {
							System.out.println(p);
						}
					}
				}
			}
		}
	}

}