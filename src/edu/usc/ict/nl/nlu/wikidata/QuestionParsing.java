package edu.usc.ict.nl.nlu.wikidata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.usc.ict.nl.nlu.clearnlp.CONLL;
import edu.usc.ict.nl.nlu.clearnlp.JsonCONLL;
import edu.usc.ict.nl.nlu.clearnlp.parserservice.Client;
import edu.usc.ict.nl.nlu.wikidata.WikiThing.TYPE;
import edu.usc.ict.nl.nlu.wikidata.utils.JsonUtils;
import edu.usc.ict.nl.util.FunctionalLibrary;

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
			List<WikiThing> properties = Wikidata.getIdsForString(property,WikiLanguage.EN,TYPE.PROPERTY);
			List<WikiThing> objects = Wikidata.getIdsForString(object,WikiLanguage.EN,TYPE.ITEM);
			if (properties!=null && !properties.isEmpty() && objects!=null && !objects.isEmpty()) {
				for(WikiThing p:properties) {
					for(WikiThing o:objects) {
						//System.out.println(p+" "+o);
						List<WikiThing> result = Queries.getAllSubjectsOf(p, o,WikiLanguage.EN);
						if (result==null) result=Queries.getAllObjectsOf(p, o,WikiLanguage.EN);
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
	
	public static CONLL getDependencies(String sentence) throws Exception {
		JSONObject response=Client.parse("http://localhost:8080",sentence);
		List<Object> parses = JsonUtils.getAll(response, "result","parse");
		for(Object p:parses) {
			CONLL x=JsonCONLL.fromJsonConll((JSONArray) p);
			return x;
		}
		return null;
	}
	
	/**
	 * Who is France\'s prime minister?
	 * "Who is the prime minister of France?" nsubj=who be=root attr=minister attr-amod=prime attr-prep=of attr-prep-pobj=france
	 * nsubj=who be=root =>retrieve-and-return attr
	 * attr+attr-amod Property
	 * attr-prep(of) Entity
	 * 
	 * find entities and properties and build simplified tree.
	 *  -build possible search string
	 *    -
	 * 
	 * @param deps
	 */
	private static void traverse(CONLL deps) {
		if (deps!=null) {
			LinkedList<CONLL> nodes=new LinkedList<CONLL>();
			nodes.add(deps);
			while(!nodes.isEmpty()) {
				CONLL n=nodes.pop();
				String content=verbalize(n);
				System.out.println(content);
				try {
					Collection cs = n.getImmediateChildren();
					if (cs!=null) nodes.addAll(cs);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * get all children and sort them (ascending) by id (position)
	 * @param n
	 * @return
	 */
	private static String verbalize(CONLL n) {
		List cs = n.getAllDescendants();
		if (cs==null) cs=new ArrayList<CONLL>();
		cs.add(n);
		Collections.sort(cs,new Comparator<CONLL>() {
			@Override
			public int compare(CONLL o1, CONLL o2) {
				return o1.getID()-o2.getID();
			}
		});
		try {
			String a = FunctionalLibrary.printCollection(cs, CONLL.class.getMethod("getWord"), "", "", " ");
			return a;
		} catch (Exception e) {
			e.printStackTrace();
		}
		/*
			String a = ((List<CONLL>)cs).stream().sorted((p1, p2) -> p1.getID() - p2.getID()).map(CONLL::getWord).collect(Collectors.joining(" "));
			return a;
		*/
		return null;
	}

	public static void main(String[] args) throws Exception {
		CONLL deps = getDependencies("How old is the son of the main actor of \"I, Robot\"?");
		//System.out.println(verbalize(deps));
		//CONLL deps = getDependencies("The circle is her own parent.");
		deps.toGDLGraph("sentence-4.gdl");
		//traverse(deps);
		Set<WikiThing> r = getRoughQuery("capital","italy");
		//System.out.println(r);
		//List<WikiThing> properties = Wikidata.getIdsForString("head of government",WikiLanguage.get("en"),TYPE.PROPERTY);
		//System.out.println(properties);
	}
}
