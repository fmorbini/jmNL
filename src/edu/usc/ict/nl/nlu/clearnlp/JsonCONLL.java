package edu.usc.ict.nl.nlu.clearnlp;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.emory.clir.clearnlp.dependency.DEPNode;
import edu.emory.clir.clearnlp.dependency.DEPTree;
import edu.emory.clir.clearnlp.util.arc.DEPArc;
import edu.usc.ict.nl.nlu.wikidata.utils.JsonUtils;

public class JsonCONLL {

	public static CONLL fromJsonConll(JSONArray parse) throws Exception {
		return new CONLL(new BufferedReader(new StringReader(toString(parse))));
	}
	
	public static String toString(JSONArray parse) throws JSONException {
		StringBuffer ret=null;
		int l=parse.length();
		for(int i=0;i<l;i++) {
			Object row = parse.get(i);
			Object id=JsonUtils.get((JSONObject)row, "id");
			Object lemma=JsonUtils.get((JSONObject)row, "lemma");
			Object form=JsonUtils.get((JSONObject)row, "form");
			Object pos=JsonUtils.get((JSONObject)row, "pos");
			Object parent=JsonUtils.get((JSONObject)row, "parent");
			Object edge=JsonUtils.get((JSONObject)row, "edge");
			if (ret==null) ret=new StringBuffer();
			ret.append(id+"\t"+form+"\t"+lemma+"\t"+pos+"\t"+pos+"\t"+parent+"\t"+edge+"\n");
		}
		return ret!=null?ret.toString():null;
	}
	
	public static JSONObject toJson(DEPTree tree) throws Exception {
		JSONObject output=new JSONObject();
		Iterator<DEPNode> it=tree.iterator();
		JSONArray table=new JSONArray();
		while(it.hasNext()) {
			JSONObject row=new JSONObject();
			DEPNode n=it.next();
			DEPNode p=n.getHead();
			String label=n.getLabel();
			if (p!=null) {
				row.put("id", n.getID());
				row.put("form", n.getWordForm());
				row.put("lemma", n.getLemma());
				row.put("pos", n.getPOSTag());
				row.put("parent", p.getID());
				row.put("edge", label);
				table.put(row);
			}
		}
		output.put("parse", table);
		return output;
	}
}
