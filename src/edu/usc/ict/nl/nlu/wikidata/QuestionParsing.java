package edu.usc.ict.nl.nlu.wikidata;

import java.util.List;
import java.util.Set;

import com.clearnlp.dependency.DEPTree;

import edu.usc.ict.nl.nlu.clearnlp.CONLL;
import edu.usc.ict.nl.nlu.clearnlp.DepNLU;
import edu.usc.ict.nl.nlu.wikidata.WikiThing.TYPE;

public class QuestionParsing {
	
	/**
	 * retrieve properties based on search string (property argument)
	 * retrieve an item based on search string (object argument)
	 * execute the complete query for every pair and returns the results
	 * @param property
	 * @param object
	 */
	public static void getRoughQuery(String property,String object) {
		try {
			List<WikiThing> properties = Wikidata.getIdsForString(property,WikiLanguage.get("en"),TYPE.PROPERTY);
			List<WikiThing> objects = Wikidata.getIdsForString(object,WikiLanguage.get("en"),TYPE.ITEM);
			if (properties!=null && !properties.isEmpty() && objects!=null && !objects.isEmpty()) {
				for(WikiThing p:properties) {
					for(WikiThing o:objects) {
						Set<WikiThing> result = Wikidata.runQuery("claim["+p.getId()+":"+o.getId()+"]", WikiLanguage.get("en"));
						System.out.println(result);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void test() throws Exception {
		DepNLU parser=new DepNLU();
		List<DEPTree> result = parser.parse("tell me the capital of france?", System.out);
		
		int id=1;
		if (result!=null) {
			for(DEPTree r:result) {
				System.out.println(parser.enrichedInputString(r,"_"));
				new CONLL(r).toGDLGraph("sentence-"+id+".gdl");
				id++;
			}
		}

	}
	
	public static void main(String[] args) throws Exception {
		getRoughQuery("position", "president of the united states");
		//List<WikiThing> properties = Wikidata.getIdsForString("head of government",WikiLanguage.get("en"),TYPE.PROPERTY);
		//System.out.println(properties);
	}
}
