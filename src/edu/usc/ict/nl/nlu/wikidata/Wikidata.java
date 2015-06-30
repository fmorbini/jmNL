package edu.usc.ict.nl.nlu.wikidata;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.usc.ict.nl.util.FunctionalLibrary;

public class Wikidata {
	public static JSONObject getEntitiesForDescription(String description) {
		try {
			URI uri = new URI("https","www.wikidata.org","/w/api.php","action=wbsearchentities&search="+description+"&language=en&format=json",null);
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

	public static JSONObject getInfoForEntity(String... ids) {
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
						List<String> descriptions=getAllValuesForProperty(t,"descriptions","value");
						try {
							ret.append("descriptions: "+FunctionalLibrary.printCollection(descriptions, "", "", ", ")+"\n");
						} catch (Exception e) {}
					}
				}
			}
		}
		return (ret.length()==0)?null:ret.toString();
	}
	public static String prettyPrintAllInfoForContent(JSONObject json) {
		StringBuffer ret=new StringBuffer();
		if (json!=null) {
			List<Object> entities = getEntities(json);
			if (entities!=null) {
				int l=entities.size();
				for(int i=0;i<l;i++) {
					Object t=entities.get(i);
					if (t!=null && t instanceof JSONObject) {
						Object name=get((JSONObject) t,"id");
						ret.append("id: "+name+"\n");
						List<String> labels=getAllValuesForProperty(t,"labels","value");
						try {
							ret.append("labels: "+FunctionalLibrary.printCollection(labels, "", "", ", ")+"\n");
						} catch (Exception e) {}
						List<String> descriptions=getAllValuesForProperty(t,"descriptions","value");
						try {
							ret.append("descriptions: "+FunctionalLibrary.printCollection(descriptions, "", "", ", ")+"\n");
						} catch (Exception e) {}
					}
				}
			}
		}
		return (ret.length()==0)?null:ret.toString();
	}

	private static Object get(JSONObject t, String key) {
		try {
			Object value=t.get(key);
			return value;
		} catch (Exception e) {}
		return null;
	}

	private static List<Object> getEntities(JSONObject json) {
		List<Object> ret=null;
		if (json!=null && json instanceof JSONObject) {
			List<Object> things = new ArrayList<Object>();
			addThings(things,get(json,"entities"));
			for(Object x:things) {
				if (x!=null && x instanceof JSONObject) {
					Iterator it=((JSONObject)x).keys();
					while(it.hasNext()) {
						Object key = it.next();
						Object value = get((JSONObject) x,(String) key);
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

	private static List<String> getAllValuesForProperty(Object json,String property,String propertyValue) {
		List<String> ret=null;
		if (json!=null && json instanceof JSONObject) {
			Object labels = get((JSONObject) json,property);
			List<JSONObject> things=getAllObjectsWithKey(labels,propertyValue);
			if (things!=null) {
				for(JSONObject o:things) {
					if (o!=null && o.has(propertyValue)) {
						if (ret==null) ret=new ArrayList<String>();
						ret.add(get(o,propertyValue).toString());
					}
				}
			}
		}
		return ret;
	}

	private static List<JSONObject> getAllObjectsWithKey(Object jsonObject, String key) {
		List<JSONObject> ret=null;
		if (jsonObject!=null) {
			Deque<Object> s=new LinkedList<Object>();
			addThings(s,jsonObject);
			while(!s.isEmpty()) {
				Object t=s.pop();
				if (t!=null) {
					if (t instanceof JSONObject) {
						if (((JSONObject) t).has(key)) {
							if (ret==null) ret=new ArrayList<JSONObject>();
							ret.add((JSONObject) t);
						}
						Iterator it = ((JSONObject) t).keys();
						while(it.hasNext()) {
							Object x = it.next();
							Object o=get((JSONObject) t,(String) x);
							addThings(s, o);
						}
					}
				}
			}
		}
		return ret;
	}

	private static void addThings(Collection<Object> s, Object o) {
		if (o instanceof JSONObject) {
			s.add(o);
		} else if (o instanceof JSONArray) {
			int l=((JSONArray) o).length();
			for(int i=0;i<l;i++) {
				try {
					s.add(((JSONArray) o).get(i));
				} catch (JSONException e) {}
			}
		}
	}

	public static void main(String[] args) {
		JSONObject desc = getInfoForEntity("Q2164820");
		//System.out.println(desc);
		System.out.println(prettyPrintAllInfoForContent(desc));

	}
}
