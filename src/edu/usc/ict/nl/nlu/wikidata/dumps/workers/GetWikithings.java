package edu.usc.ict.nl.nlu.wikidata.dumps.workers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

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

	private BlockingQueue<String> queue=null;
	private BlockingQueue<WikiThing> ret=null;
	private TYPE desiredType;

	public GetWikithings(String name,BlockingQueue<String> queue,BlockingQueue<WikiThing> ret, TYPE type) {
		super(name);
		this.queue=queue;
		this.ret=ret;
		this.desiredType=type;
	}

	@Override
	public void run() {
		while(true) {
			try {
				String line = queue.poll(10,TimeUnit.SECONDS);
				if (line!=null) {
					try {
						JSONObject o=new JSONObject(line);
						String pname=(String) JsonUtils.get(o, "id");
						String typestring=(String) JsonUtils.get(o, "type");
						TYPE type=WikiThing.TYPE.valueOf(typestring.toUpperCase());
						if (type!=null && type==desiredType && pname!=null) {
							List<String> things=WikidataJsonProcessing.getAllPhrasesInWikidataForEntity(o, WikiLanguage.EN);
							String desc = Wikidata.getDescriptionForContent(o, WikiLanguage.EN);
							Map<String, List<WikiClaim>> claims = Wikidata.getClaims(o);
							try {
								WikiThing thing = new WikiThing(pname);
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
				} else {
					System.err.println("worker "+this.getName()+" exit.");
					break;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}
	}
}
