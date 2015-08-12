package edu.usc.ict.nl.nlu.wikidata;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipException;

import org.json.JSONException;
import org.json.JSONObject;

import edu.usc.ict.nl.nlu.wikidata.utils.JsonUtils;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.ProgressTracker;

public class WikidataJsonProcessing {
	
	public static List<String> getAllPhrasesInWikidataForEntity(String id,WikiLanguage lang) {
		List<String> ret=null;
		JSONObject content = Wikidata.getWikidataContentForSpecificEntityOnly(lang, id);
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
	
	public static Map<String,List<String>> getStringsForProperties(File wikidataJsonDump) throws IOException {
		Map<String,List<String>> ret=null;
		InputStream fileStream = new FileInputStream(wikidataJsonDump);
		try {
			fileStream = new GZIPInputStream(fileStream);
		} catch (ZipException e) {
			e.printStackTrace();
		}
		Reader decoder = new InputStreamReader(fileStream, "UTF-8");
		BufferedReader buffered = new BufferedReader(decoder);
		String line=null;
		Set<String> properties=new HashSet<String>();
		int objects=0,wrong=0;
		ProgressTracker pt=new ProgressTracker(100000, System.out);
		while((line=buffered.readLine())!=null) {
			try {
				JSONObject o=new JSONObject(line);
				String pname=(String) JsonUtils.get(o, "id");
				String type=(String) JsonUtils.get(o, "type");
				objects++;
				pt.update(objects);
				if (type!=null && type.equals("property") && pname!=null) {
					properties.add(pname);
				}
			} catch (JSONException e) {
				wrong++;
				//e.printStackTrace();
			}
		}
		buffered.close();
		System.err.println("wrong="+wrong);
		for(String p:properties) {
			List<String> things=getAllPhrasesInWikidataForEntity(p, WikiLanguage.EN);
			if (things!=null && !things.isEmpty()) {
				if (ret==null) ret=new HashMap<String, List<String>>();
				ret.put(p, things);
			}
		}
		return ret;
	}
	public static void dumpStringsToFile(Map<String, List<String>> r,File outfile) throws Exception {
		if (r!=null) {
			BufferedWriter out=new BufferedWriter(new FileWriter(outfile));
			List<String> sortedProperties=new ArrayList<String>(r.keySet());
			Collections.sort(sortedProperties);
			for(String p:sortedProperties) {
				List<String> things = r.get(p);
				if (things!=null && !things.isEmpty()) {
					out.write(p);
					out.write(FunctionalLibrary.printCollection(things, "\t", "\n", "\t"));
					out.flush();
				}
			}
			out.close();
		}
	}
	
	public static void main(String[] args) throws Exception {
		Map<String, List<String>> r = getStringsForProperties(new File("C:\\Users\\morbini\\AppData\\Local\\Temp\\20150622.json.gz"));
		dumpStringsToFile(r, new File("properties-strings.txt"));
	}
}
