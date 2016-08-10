package edu.usc.ict.nl.nlu.nlp4j.parserservice;

import javax.ws.rs.client.ClientBuilder;

import org.json.JSONException;
import org.json.JSONObject;

public class Client {
	
	public static JSONObject parse(String target,String sentence) throws JSONException {
		String responseEntity = ClientBuilder.newClient()
				.target(target).path("clearnlp/parse").queryParam("sentence", sentence).queryParam("timeout", "20")
				.request().get(String.class);
		return new JSONObject(responseEntity);
	}
	
	public static void main(String[] args) throws JSONException {
		System.out.println(parse("http://localhost:8080","i eat an apple because i am angry."));
	}
}
