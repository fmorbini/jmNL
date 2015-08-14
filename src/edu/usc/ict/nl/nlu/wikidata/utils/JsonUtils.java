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
	public static Object get(JSONObject t, String... keys) {
		try {
			Object current=t;
			for(String key:keys) {
				if (current instanceof JSONObject) {
					if (((JSONObject) current).has(key)) {
						current=((JSONObject) current).get(key);
					} else current=null;
					if (current==null) return null;
				} else return null;
			}
			return current;
		} catch (Exception e) {}
		return null;
	}

	public static List<Object> getAll(JSONObject t,String... path) {
		List<Object> ret=new ArrayList<Object>();
		getAllValuesForProperty(t, 0, ret, path);
		return ret;
	}
	
	private static void getAllValuesForProperty(Object json,int i,List<Object> ret,String... path) {
		if (json!=null) {
			if (i>(path.length-1)) ret.add(json);
			else {
				if (json instanceof JSONObject) {
					Object things = get((JSONObject) json,path[i]);
					getAllValuesForProperty(things, i+1, ret, path);
				} else if (json instanceof JSONArray) {
					int l=((JSONArray)json).length();
					for(int j=0;j<l;j++) {
						try {
							Object x=((JSONArray)json).get(j);
							getAllValuesForProperty(x, i, ret, path);
						} catch (JSONException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

}
