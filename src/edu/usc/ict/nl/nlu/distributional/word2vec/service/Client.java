package edu.usc.ict.nl.nlu.distributional.word2vec.service;

import javax.ws.rs.client.ClientBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.usc.ict.nl.nlu.distributional.word2vec.W2V2;

public class Client {
	
	public static JSONObject getVector(String target,Integer pos) throws JSONException {
		String responseEntity = ClientBuilder.newClient()
				.target(target).path("w2v2/getVector").queryParam("pos", pos).queryParam("timeout", "20")
				.request().get(String.class);
		return new JSONObject(responseEntity);
	}
	public static JSONObject getWordPos(String target,String word) throws JSONException {
		String responseEntity = ClientBuilder.newClient()
				.target(target).path("w2v2/getWordPos").queryParam("word", word).queryParam("timeout", "20")
				.request().get(String.class);
		return new JSONObject(responseEntity);
	}
	public static JSONObject getMostSimilar(String target,String word) throws JSONException {
		String responseEntity = ClientBuilder.newClient()
				.target(target).path("w2v2/getMostSimilar").queryParam("word", word).queryParam("nbest", "5").queryParam("timeout", "20")
				.request().get(String.class);
		return new JSONObject(responseEntity);
	}
	
	public static float[] getVectorForWord(String target,Integer pos) throws JSONException {
		String responseEntity = ClientBuilder.newClient()
				.target(target).path("w2v2/getVector").queryParam("pos", pos).queryParam("timeout", "20")
				.request().get(String.class);
		JSONObject result=new JSONObject(responseEntity);
		JSONArray thing = result.getJSONArray("vector");
		int l=thing.length();
		float[] ret=new float[l];
		for(int i=0;i<l;i++) {
			ret[i]=(float)thing.getDouble(i);
		}
		return ret;
	}

	private static final String target="http://localhost:8080";
	public static void main(String[] args) throws JSONException {
		System.out.println(getWordPos(target,"red"));//1618
		System.out.println(getWordPos(target,"pear"));//52377
		float[] v1=getVectorForWord(target, 1618);
		float[] v2=getVectorForWord(target, 52377);
		float d=W2V2.similarity(v1, v2);
		System.out.println(d);
		//System.out.println(getMostSimilar(target,"pear"));
		//System.out.println(getVectorForWord(target,"fuji_apple"));
		//System.out.println(getVectorForWord(target,"pear"));
	}
}
