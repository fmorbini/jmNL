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

	public static JSONObject getThingForDescription(String description,String lang,TYPE type) {
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

	public static JSONObject getWikidataContentForEntitiesRaw(String lang,String... ids) {
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

	public static JSONObject runWikidataQuery(String query,String lang) {
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
	
	public static void main(String[] args) {
		JSONObject r = runWikidataQuery("en","claim[31:6256]");
		System.out.println(r);
	}

}
