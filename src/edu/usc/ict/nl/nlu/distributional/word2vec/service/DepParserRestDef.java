package edu.usc.ict.nl.nlu.distributional.word2vec.service;

import java.util.PriorityQueue;
import java.util.Queue;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.json.JSONObject;

import edu.usc.ict.nl.nlu.distributional.word2vec.W2V2.Element;
 
@Path("/w2v2/")
public class DepParserRestDef {
 
    @GET
    @Path("/getVector/")
    @Produces(MediaType.APPLICATION_JSON)
    public String getVector(@QueryParam("pos") Integer i,@QueryParam("timeout") Integer timeout) {
        JSONObject output=new JSONObject();
        if (timeout==null || timeout<1) timeout=10;
		try {
			float[] vector = Server.w2v.getVectorForWord(i);
			if (vector!=null) {
				output.put("vector", vector);
			}
			return output.toString(1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
    }
    @GET
    @Path("/getWordPos/")
    @Produces(MediaType.APPLICATION_JSON)
    public String getWordPos(@QueryParam("word") String word,@QueryParam("timeout") Integer timeout) {
        JSONObject output=new JSONObject();
        if (timeout==null || timeout<1) timeout=10;
		try {
			int pos = Server.w2v.isWordInVocabulary(word);
			output.put("pos", pos);
			return output.toString(1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
    }
    @GET
    @Path("/getMostSimilar/")
    @Produces(MediaType.APPLICATION_JSON)
    public String getMostSimilar(@QueryParam("word") String word,@QueryParam("nbest") Integer nbest,@QueryParam("timeout") Integer timeout) {
        JSONObject output=new JSONObject();
        if (timeout==null || timeout<1) timeout=10;
		try {
			Queue<Element> pos = Server.w2v.getTopSimilarTo(nbest, word);
			output.put("pos", pos);
			return output.toString(1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
    }
}