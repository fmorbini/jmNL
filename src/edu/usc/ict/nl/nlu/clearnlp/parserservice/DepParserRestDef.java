package edu.usc.ict.nl.nlu.clearnlp.parserservice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.emory.clir.clearnlp.dependency.DEPTree;
import edu.usc.ict.nl.nlu.clearnlp.JsonCONLL;
 
@Path("/clearnlp/")
public class DepParserRestDef {
 
	static Map<String,List<DEPTree>> cache=new LinkedHashMap<String,List<DEPTree>>(10000, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String,List<DEPTree>> eldest) {
            return size() > 10000;
        }
    };
	
    @GET
    @Path("/parse/")
    @Produces(MediaType.APPLICATION_JSON)
    public String parse(@QueryParam("sentence") String sentence,@QueryParam("timeout") Integer timeout) {
        JSONObject output=new JSONObject();
        JSONArray parseResults=new JSONArray();
        if (timeout==null || timeout<1) timeout=10;
		try {
	        output.put("result", parseResults);
			List<DEPTree> result=(cache!=null)?cache.get(sentence):null;
			boolean fromcache=result!=null;
			if (fromcache) System.out.println("retrieving parsing resultfor '"+sentence+"' from cache.");
			else System.out.println("parsing sentence '"+sentence+"' with timeout of "+timeout+" second(s) (ignored).");
			if (result==null) result = Server.parser.parse(sentence);
			if (!fromcache && cache!=null && result!=null) cache.put(sentence,result);
			//System.out.println("server result: "+result);
			if (result!=null) {
				for(DEPTree po:result) {
					JSONObject parseTable=JsonCONLL.toJson(po);
					parseResults.put(parseTable);
				}
			}
			return output.toString(1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
    }
    
    @GET
    @Path("/reset/")
    @Produces(MediaType.TEXT_PLAIN)
    public String reset() {
    	int size=cache.size();
    	cache.clear();
        return "OK. before="+size+" after="+cache.size();
    }
}