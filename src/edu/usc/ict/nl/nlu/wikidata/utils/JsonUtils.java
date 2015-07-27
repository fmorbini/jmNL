package edu.usc.ict.nl.nlu.wikidata.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JsonUtils {
	/**
	 * flattens one level: if input is an object, it returns that object, if it's an array it returns the elements. 
	 * @param o
	 * @return
	 */
	public static Collection<JSONObject> addThings(Object o) {
		Collection<JSONObject> ret=new ArrayList<JSONObject>();
		return addThings(ret, o);
	}
	public static Collection<JSONObject> addThings(Collection<JSONObject> s, Object o) {
		if (o instanceof JSONObject) {
			s.add((JSONObject)o);
		} else if (o instanceof JSONArray) {
			int l=((JSONArray) o).length();
			for(int i=0;i<l;i++) {
				try {
					s.add((JSONObject)((JSONArray) o).get(i));
				} catch (JSONException e) {}
			}
		}
		return s;
	}

	/**
	 * get all json objects that contain the given key. Searches recursively in arrays using the method addThings.
	 * @param jsonObject
	 * @param key
	 * @return
	 */
	private static List<JSONObject> getAllObjectsWithKey(Object jsonObject, String key) {
		List<JSONObject> ret=null;
		if (jsonObject!=null) {
			Deque<JSONObject> s=new LinkedList<JSONObject>();
			addThings(s,jsonObject);
			while(!s.isEmpty()) {
				Object t=s.pop();
				if (t!=null) {
					if (t instanceof JSONObject) {
						if (((JSONObject) t).has(key)) {
							if (ret==null) ret=new ArrayList<JSONObject>();
							ret.add((JSONObject) t);
						}
						Iterator it = ((JSONObject) t).keys();
						while(it.hasNext()) {
							Object x = it.next();
							Object o=get((JSONObject) t,(String) x);
							addThings(s, o);
						}
					}
				}
			}
		}
		return ret;
	}

	/**
	 * returns the value of a certain key. If that object doesn't contain the key, ir returns null (removes the exception).
	 * @param t
	 * @param key
	 * @return
	 */
	public static Object get(JSONObject t, String key) {
		try {
			Object value=t.get(key);
			return value;
		} catch (Exception e) {}
		return null;
	}

	public static List<String> getAllValuesForProperty(Object json,String property,String propertyValue) {
		List<String> ret=null;
		if (json!=null && json instanceof JSONObject) {
			Object labels = get((JSONObject) json,property);
			List<JSONObject> things=getAllObjectsWithKey(labels,propertyValue);
			if (things!=null) {
				for(JSONObject o:things) {
					if (o!=null && o.has(propertyValue)) {
						if (ret==null) ret=new ArrayList<String>();
						ret.add(get(o,propertyValue).toString());
					}
				}
			}
		}
		return ret;
	}

}
