package edu.usc.ict.nl.nlu.wikidata;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.usc.ict.nl.nlu.wikidata.WikiThing.TYPE;
import edu.usc.ict.nl.nlu.wikidata.utils.JsonUtils;

public class Queries {

	private static final String SparQLPrefixes="PREFIX wd: <http://www.wikidata.org/entity/>"+
			"PREFIX wdt: <http://www.wikidata.org/prop/direct/>"+
			"PREFIX wikibase: <http://wikiba.se/ontology#>"+
			"PREFIX p: <http://www.wikidata.org/prop/>"+
			"PREFIX v: <http://www.wikidata.org/prop/statement/>"+
			"PREFIX q: <http://www.wikidata.org/prop/qualifier/>";

	public static JSONObject getThingForDescription(String description,WikiLanguage lang,TYPE type) {
		try {
			URI uri = new URI("https","www.wikidata.org","/w/api.php","action=wbsearchentities&search="+description+"&language="+lang+"&format=json&type="+type.toString().toLowerCase(),null);
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

	public static JSONObject getWikidataContentForEntitiesRaw(WikiLanguage lang,String... ids) {
		if (ids!=null) {
			try {
				StringBuffer idsb=new StringBuffer();
				for(String id:ids) {
					if (idsb.length()==0)
						idsb.append(id);
					else
						idsb.append("|"+id);
				}
				URI uri = new URI("https","www.wikidata.org","/w/api.php","action=wbgetentities&ids="+idsb.toString()+"&languages="+lang+"&format=json",null);
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

	public static JSONObject runWikidataQuery(String query,WikiLanguage lang) {
		if (query!=null) {
			try {
				URI uri = new URI("https","wdq.wmflabs.org","/api","q="+query.toString()+"&languages="+lang+"&format=json",null);
				String request = uri.toASCIIString();
				URL url = new URL(request);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();           
				connection.setDoOutput(true);
				connection.setDoInput(true);
				connection.setConnectTimeout(1000);
				connection.setReadTimeout(20000);
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
					if (json!=null && JsonUtils.get(json, "status","error").equals("OK")) {
						return json;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	
	//curl http://wdqs-beta.wmflabs.org/bigdata/namespace/wdq/sparql?query=select%20distinct%20%3Ftype%20where%20%7B%0A%3Fthing%20a%20%3Ftype%0A%7D%0Alimit%2020 -H "Accept: text/csv"
	/*
	 * PREFIX wd: <http://www.wikidata.org/entity/>
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
PREFIX wikibase: <http://wikiba.se/ontology#>
PREFIX p: <http://www.wikidata.org/prop/>
PREFIX v: <http://www.wikidata.org/prop/statement/>
PREFIX q: <http://www.wikidata.org/prop/qualifier/>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
SELECT ?p ?w ?l ?wl WHERE {
   wd:Q30 p:P6/v:P6 ?p .
   ?p wdt:P26 ?w .
   OPTIONAL  {  
     ?p rdfs:label ?l filter (lang(?l) = "en") . 
   }
   OPTIONAL {
     ?w rdfs:label ?wl filter (lang(?wl) = "en"). 
   }
 }
	 */
	public static JSONObject runWikidataSparQLQuery(String query) {
		if (query!=null) {
			try {
				if (!query.startsWith("PREFIX")) query=SparQLPrefixes+"\n"+query;
				URI uri = new URI("http","wdqs-beta.wmflabs.org","/bigdata/namespace/wdq/sparql","query="+query.toString(),null);
				String request = uri.toASCIIString();
				URL url = new URL(request);
				HttpURLConnection connection = (HttpURLConnection) url.openConnection();           
				connection.setDoOutput(true);
				connection.setDoInput(true);
				connection.setConnectTimeout(1000);
				connection.setReadTimeout(20000);
				connection.setInstanceFollowRedirects(false); 
				connection.setRequestProperty("Accept", "application/json");
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
					return json;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	public static List<WikiThing> getAllSubjectsOf(WikiThing property,WikiThing object,WikiLanguage lang) {
		if (property!=null && object!=null) {
			String q="SELECT ?p ?l WHERE {?p wdt:"+property.getName()+" wd:"+object.getName()+" . OPTIONAL  {?p rdfs:label ?l filter (lang(?l) = \""+lang.getLcode()+"\") .}}";
			JSONObject json=runWikidataSparQLQuery(q);
			return getEntities(json,"p","l");
		}
		return null;
	}
	public static List<WikiThing> getAllObjectsOf(WikiThing property,WikiThing subject,WikiLanguage lang) {
		if (property!=null && subject!=null) {
			String q="SELECT ?p ?l WHERE {wd:"+subject.getName()+" wdt:"+property.getName()+" ?p . OPTIONAL  {?p rdfs:label ?l filter (lang(?l) = \""+lang.getLcode()+"\") .}}";
			JSONObject json=runWikidataSparQLQuery(q);
			return getEntities(json,"p","l");
		}
		return null;
	}
	
	private static List<WikiThing> getEntities(JSONObject json, String entityVarName,String labelVarName) {
		List<WikiThing> ret=null;
		if (json!=null) {
			Object results = JsonUtils.get(json, "results","bindings");
			if (results!=null && results instanceof JSONArray) {
				JSONArray rs=(JSONArray)results;
				int l=rs.length();
				for(int i=0;i<l;i++) {
					Object r;
					try {
						r = rs.get(i);
						if (r!=null && r instanceof JSONObject) {
							//System.out.println(r);
							Object rv=JsonUtils.get((JSONObject)r, entityVarName,"value");
							Object rl=JsonUtils.get((JSONObject)r, labelVarName,"value");
							URI u=new URI((String) rv);
							String entityName=new File(u.getPath()).getName();
							WikiThing thing=new WikiThing(entityName);
							thing.setLabel((String) rl);
							if (ret==null) ret=new ArrayList<WikiThing>();
							ret.add(thing);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return ret;
	}

	public static void main(String[] args) throws Exception {
		String q="SELECT ?p ?w ?l ?wl WHERE {"+
				"wd:Q30 p:P6/v:P6 ?p ."+
				"?p wdt:P26 ?w ."+
				"OPTIONAL  {"+  
				"?p rdfs:label ?l filter (lang(?l) = \"en\") ."+ 
				"}"+
				"OPTIONAL {"+
				"?w rdfs:label ?wl filter (lang(?wl) = \"en\")."+ 
				"}"+
				"}";
		String q2="SELECT ?p WHERE {wd:Q30 wdt:P35 ?p .}";
		List<WikiThing> r = getAllObjectsOf(new WikiThing(35, TYPE.PROPERTY), new WikiThing(30, TYPE.ITEM),WikiLanguage.get("en"));
		System.out.println(r);
	}

}
