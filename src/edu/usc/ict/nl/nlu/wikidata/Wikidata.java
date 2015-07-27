package edu.usc.ict.nl.nlu.wikidata;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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

	public static JSONObject getWikidataContentForEntity(String... ids) {
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

	public static String getDescriptionForContent(JSONObject json) {
		StringBuffer ret=new StringBuffer();
		if (json!=null) {
			List<Object> entities = getEntities(json);
			if (entities!=null) {
				int l=entities.size();
				for(int i=0;i<l;i++) {
					Object t=entities.get(i);
					if (t!=null && t instanceof JSONObject) {
						List<String> descriptions=JsonUtils.getAllValuesForProperty(t,"descriptions","value");
						try {
							ret.append("descriptions: "+FunctionalLibrary.printCollection(descriptions, "", "", ", ")+"\n");
						} catch (Exception e) {}
					}
				}
			}
		}
		return (ret.length()==0)?null:ret.toString();
	}
	public static String prettyPrintWikidataContent(JSONObject json) {
		StringBuffer ret=new StringBuffer();
		if (json!=null) {
			List<Object> entities = getEntities(json);
			if (entities!=null) {
				int l=entities.size();
				for(int i=0;i<l;i++) {
					Object t=entities.get(i);
					if (t!=null && t instanceof JSONObject) {
						Object name=JsonUtils.get((JSONObject) t,"id");
						ret.append("id: "+name+"\n");
						List<String> labels=JsonUtils.getAllValuesForProperty(t,"labels","value");
						try {
							ret.append("labels: "+FunctionalLibrary.printCollection(labels, "", "", ", ")+"\n");
						} catch (Exception e) {}
						List<String> descriptions=JsonUtils.getAllValuesForProperty(t,"descriptions","value");
						try {
							ret.append("descriptions: "+FunctionalLibrary.printCollection(descriptions, "", "", ", ")+"\n");
						} catch (Exception e) {}
					}
				}
			}
		}
		return (ret.length()==0)?null:ret.toString();
	}


	private static List<Object> getEntities(JSONObject json) {
		List<Object> ret=null;
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
									if (ret==null) ret=new ArrayList<Object>();
									ret.add(value);
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


	public static void main(String[] args) {
		//List<String> ids = getIdsForMatchingEntities("city");
		List<String> ids = getIdsForMatchingProperties("author");
		if (ids!=null) {
			for(String id:ids) {
				JSONObject desc = getWikidataContentForEntity(id);
				System.out.println(prettyPrintWikidataContent(desc));
			}
		}
	}
}
