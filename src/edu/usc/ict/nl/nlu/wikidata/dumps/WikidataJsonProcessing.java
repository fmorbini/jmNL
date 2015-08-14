package edu.usc.ict.nl.nlu.wikidata.dumps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import org.json.JSONException;
import org.json.JSONObject;

import edu.usc.ict.nl.nlu.wikidata.WikiClaim;
import edu.usc.ict.nl.nlu.wikidata.WikiLanguage;
import edu.usc.ict.nl.nlu.wikidata.WikiThing;
import edu.usc.ict.nl.nlu.wikidata.WikiThing.TYPE;
import edu.usc.ict.nl.nlu.wikidata.Wikidata;
import edu.usc.ict.nl.nlu.wikidata.dumps.workers.GetWikithings;
import edu.usc.ict.nl.nlu.wikidata.dumps.workers.WriteClaimsToFile;
import edu.usc.ict.nl.nlu.wikidata.dumps.workers.WriteStringsToFile;
import edu.usc.ict.nl.nlu.wikidata.utils.JsonUtils;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.ProgressTracker;

public class WikidataJsonProcessing {
	
	public static List<String> getAllPhrasesInWikidataForEntity(String id,WikiLanguage lang) {
		JSONObject content = Wikidata.getWikidataContentForSpecificEntityOnly(lang, id);
		return getAllPhrasesInWikidataForEntity(content, lang);
	}
	public static List<String> getAllPhrasesInWikidataForEntity(JSONObject content,WikiLanguage lang) {
		List<String> ret=null;
		List<String> aliases = Wikidata.getAliasesForContent(content, lang);
		List<String> labels = Wikidata.getLabelsForContent(content, lang);
		if (aliases!=null &&!aliases.isEmpty()) {
			if (ret==null) ret=new ArrayList<String>();
			ret.addAll(aliases);
		}
		if (labels!=null &&!labels.isEmpty()) {
			if (ret==null) ret=new ArrayList<String>();
			ret.addAll(labels);
		}
		return ret;
	}
	
	public static Set<WikiThing> getObjectsIntoProcessingQueue(File wikidataJsonDump,LinkedBlockingQueue<String> queue) throws IOException, InterruptedException {
		Set<WikiThing> ret=null;
		InputStream fileStream = new FileInputStream(wikidataJsonDump);
		try {
			fileStream = new GZIPInputStream(fileStream);
		} catch (ZipException e) {
			e.printStackTrace();
		}
		Reader decoder = new InputStreamReader(fileStream, "UTF-8");
		BufferedReader buffered = new BufferedReader(decoder);
		String line=null;
		int objects=0;
		ProgressTracker pt=new ProgressTracker(100000, System.out);
		while((line=buffered.readLine())!=null) {
			queue.put(line);
			objects++;
			pt.update(objects);
		}
		buffered.close();
		return ret;
	}

	public static Set<WikiThing> getStringsForThings(File wikidataJsonDump,WikiThing.TYPE desiredType) throws IOException {
		Set<WikiThing> ret=null;
		InputStream fileStream = new FileInputStream(wikidataJsonDump);
		try {
			fileStream = new GZIPInputStream(fileStream);
		} catch (ZipException e) {
			e.printStackTrace();
		}
		Reader decoder = new InputStreamReader(fileStream, "UTF-8");
		BufferedReader buffered = new BufferedReader(decoder);
		String line=null;
		int objects=0,wrong=0;
		ProgressTracker pt=new ProgressTracker(100000, System.out);
		while((line=buffered.readLine())!=null) {
			try {
				JSONObject o=new JSONObject(line);
				String pname=(String) JsonUtils.get(o, "id");
				String typestring=(String) JsonUtils.get(o, "type");
				TYPE type=WikiThing.TYPE.valueOf(typestring.toUpperCase());
				objects++;
				pt.update(objects);
				if (type!=null && type==desiredType && pname!=null) {
					List<String> things=getAllPhrasesInWikidataForEntity(o, WikiLanguage.EN);
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
						if (ret==null) ret=new HashSet<WikiThing>();
						ret.add(thing);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (JSONException e) {
				wrong++;
				//e.printStackTrace();
			}
		}
		buffered.close();
		System.err.println("wrong="+wrong);
		return ret;
	}
	public static void dumpStringsToFile(Set<WikiThing> r,File outfile) throws Exception {
		if (r!=null) {
			BufferedWriter out=new BufferedWriter(new FileWriter(outfile));
			List<WikiThing> sortedProperties=new ArrayList<WikiThing>(r);
			Collections.sort(sortedProperties);
			for(WikiThing p:sortedProperties) {
				List<String> things = p.getLabels();
				if (things!=null && !things.isEmpty()) {
					out.write(p.getName());
					out.write(FunctionalLibrary.printCollection(things, "\t", "\n", "\t"));
					out.flush();
				}
			}
			out.close();
		}
	}
	public static void dumpClaimsToFile(Set<WikiThing> r,File outfile) throws Exception {
		if (r!=null) {
			BufferedWriter out=new BufferedWriter(new FileWriter(outfile));
			for(WikiThing p:r) {
				List<WikiClaim> claims = p.getClaims();
				if (claims!=null && !claims.isEmpty()) {
					for(WikiClaim cl:claims) {
						if (cl!=null) {
							out.write(cl.getProperty()+"\t"+cl.getSubject()+"\t"+cl.getObject()+"\n");
							out.flush();
						}
					}
				}
			}
			out.close();
		}
	}
	
	public static void main(String[] args) throws Exception {
		//Set<WikiThing> ps = getStringsForThings(new File("C:\\Users\\morbini\\Downloads\\20150810.json.gz"),TYPE.PROPERTY);
		//dumpStringsToFile(ps, new File("properties-strings.txt"));
		LinkedBlockingQueue<String> queue=new LinkedBlockingQueue<>(10);
		LinkedBlockingQueue<WikiThing> is = new LinkedBlockingQueue<WikiThing>(10);
		int max=6;
		for(int i=0;i<max;i++) new GetWikithings("object getter "+i,queue, is,TYPE.ITEM).start();
		//new WriteStringsToFile(is, new File("properties-strings.txt")).start();
		//new WriteStringsToFile(is, new File("items-strings.txt")).start();
		new WriteClaimsToFile("writer",is, new File("items-claims.txt")).start();

		getObjectsIntoProcessingQueue(new File("C:\\Users\\morbini\\Downloads\\20150810.json.gz"), queue);
		
		//Set<WikiThing> is = getStringsForThings(new File("C:\\Users\\morbini\\Downloads\\20150810.json.gz"),TYPE.ITEM);
		//dumpStringsToFile(is, new File("items-strings.txt"));
		//dumpClaimsToFile(is,new File("items-claims.txt"));
	}
}
