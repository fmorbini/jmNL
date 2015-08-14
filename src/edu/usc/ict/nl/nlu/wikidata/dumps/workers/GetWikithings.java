package edu.usc.ict.nl.nlu.wikidata.dumps.workers;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.JSONException;
import org.json.JSONObject;

import edu.usc.ict.nl.nlu.wikidata.WikiClaim;
import edu.usc.ict.nl.nlu.wikidata.WikiLanguage;
import edu.usc.ict.nl.nlu.wikidata.WikiThing;
import edu.usc.ict.nl.nlu.wikidata.WikiThing.TYPE;
import edu.usc.ict.nl.nlu.wikidata.Wikidata;
import edu.usc.ict.nl.nlu.wikidata.dumps.WikidataJsonProcessing;
import edu.usc.ict.nl.nlu.wikidata.utils.JsonUtils;

public class GetWikithings extends Thread {

	LinkedBlockingQueue<String> queue=null;
	private BlockingQueue<WikiThing> ret=null;
	private TYPE desiredType;

	public GetWikithings(LinkedBlockingQueue<String> queue,BlockingQueue<WikiThing> ret, TYPE type) {
		this.queue=queue;
		this.ret=ret;
		this.desiredType=type;
	}

	@Override
	public void run() {
		while(true) {
			try {
				String line = queue.take();
				try {
					JSONObject o=new JSONObject(line);
					String pname=(String) JsonUtils.get(o, "id");
					String typestring=(String) JsonUtils.get(o, "type");
					TYPE type=WikiThing.TYPE.valueOf(typestring.toUpperCase());
					if (type!=null && type==desiredType && pname!=null) {
						List<String> things=WikidataJsonProcessing.getAllPhrasesInWikidataForEntity(o, WikiLanguage.EN);
						String desc = Wikidata.getDescriptionForContent(o, WikiLanguage.EN);
						Map<String, List<WikiClaim>> claims = Wikidata.getClaims(o);
						WikiThing thing;
						try {
							thing = new WikiThing(pname);
							thing.setLabels(things);
							thing.setDesc(desc);
							if (claims!=null) {
								for(List<WikiClaim> cls:claims.values()) {
									if (cls!=null) {
										for(WikiClaim cl:cls) {
											thing.addClaim(cl);
										}
									}
								}
							}
							ret.put(thing);
						} catch (Exception e) {e.printStackTrace();}
					}
				} catch (JSONException e) {
					//e.printStackTrace();
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}
}
