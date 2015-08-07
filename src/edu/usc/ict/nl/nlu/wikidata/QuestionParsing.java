package edu.usc.ict.nl.nlu.wikidata;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.clearnlp.dependency.DEPTree;

import edu.usc.ict.nl.nlu.clearnlp.CONLL;
import edu.usc.ict.nl.nlu.clearnlp.DepNLU;
import edu.usc.ict.nl.nlu.clearnlp.JsonCONLL;
import edu.usc.ict.nl.nlu.clearnlp.parserservice.Client;
import edu.usc.ict.nl.nlu.wikidata.WikiThing.TYPE;
import edu.usc.ict.nl.nlu.wikidata.utils.JsonUtils;

public class QuestionParsing {
	
	/**
	 * retrieve properties based on search string (property argument)
	 * retrieve an item based on search string (object argument)
	 * execute the complete query for every pair and returns the results
	 * @param property
	 * @param object
	 */
	public static Set<WikiThing> getRoughQuery(String property,String object) {
		Set<WikiThing> ret=null;
		try {
			List<WikiThing> properties = Wikidata.getIdsForString(property,WikiLanguage.get("en"),TYPE.PROPERTY);
			List<WikiThing> objects = Wikidata.getIdsForString(object,WikiLanguage.get("en"),TYPE.ITEM);
			if (properties!=null && !properties.isEmpty() && objects!=null && !objects.isEmpty()) {
				for(WikiThing p:properties) {
					for(WikiThing o:objects) {
						//System.out.println(p+" "+o);
						List<WikiThing> result = Queries.getAllSubjectsOf(p, o,WikiLanguage.get("en"));
						if (result==null) result=Queries.getAllObjectsOf(p, o,WikiLanguage.get("en"));
						if (result!=null) {
							if (ret==null) ret=new HashSet<WikiThing>();
							ret.addAll(result);
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public static void test() throws Exception {
		JSONObject response=Client.parse("http://localhost:8080","Who is the prime minister of France?");
		List<Object> parses = JsonUtils.getAll(response, "result","parse");
		for(Object p:parses) {
			CONLL x=JsonCONLL.fromJsonConll((JSONArray) p);
			System.out.println(x);
		}
	}
	
	public static void main(String[] args) throws Exception {
		test();
		//Set<WikiThing> r = getRoughQuery("capital","italy");
		//System.out.println(r);
		//List<WikiThing> properties = Wikidata.getIdsForString("head of government",WikiLanguage.get("en"),TYPE.PROPERTY);
		//System.out.println(properties);
	}
}
