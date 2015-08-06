package edu.usc.ict.nl.nlu.wikidata;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import org.json.JSONObject;

import edu.usc.ict.nl.nlu.wikidata.WikiThing.TYPE;
import edu.usc.ict.nl.nlu.wikidata.utils.JsonUtils;

public class Queries {

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
	public static JSONObject runWikidataSparQLQuery(String query,WikiLanguage lang) {
		if (query!=null) {
			try {
				URI uri = new URI("http","wdqs-beta.wmflabs.org","/bigdata/namespace/wdq/sparql","query="+query.toString()+"&languages="+lang+"&format=json",null);
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
	
	public static void main(String[] args) throws Exception {
		String q="PREFIX wd: <http://www.wikidata.org/entity/>"+
				"PREFIX wdt: <http://www.wikidata.org/prop/direct/>"+
				"PREFIX wikibase: <http://wikiba.se/ontology#>"+
				"PREFIX p: <http://www.wikidata.org/prop/>"+
				"PREFIX v: <http://www.wikidata.org/prop/statement/>"+
				"PREFIX q: <http://www.wikidata.org/prop/qualifier/>"+
				"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+
				"SELECT ?p ?w ?l ?wl WHERE {"+
				"wd:Q30 p:P6/v:P6 ?p ."+
				"?p wdt:P26 ?w ."+
				"OPTIONAL  {"+  
				"?p rdfs:label ?l filter (lang(?l) = \"en\") ."+ 
				"}"+
				"OPTIONAL {"+
				"?w rdfs:label ?wl filter (lang(?wl) = \"en\")."+ 
				"}"+
				"}";
		JSONObject r = runWikidataSparQLQuery(q,WikiLanguage.get("en"));
		System.out.println(r);
	}

}
