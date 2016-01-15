package edu.usc.ict.nl.nlu.wikidata;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import edu.usc.ict.nl.nlu.wikidata.dumps.Queries;
import edu.usc.ict.nl.nlu.wikidata.dumps.WikidataJsonProcessing;
import edu.usc.ict.nl.util.AhoCorasickList;
import edu.usc.ict.nl.util.AhoCorasickList.Match;
import edu.usc.ict.nl.util.ProgressTracker;

public class Test {
	
	
	public static AhoCorasickList loadWiki() throws Exception {
		long start=System.currentTimeMillis();
		AhoCorasickList s=new AhoCorasickList();
		File file=WikidataJsonProcessing.ITEMS;
		BufferedReader in=new BufferedReader(new FileReader(file));
		String line=null;
		ProgressTracker pt=new ProgressTracker(1000, System.out);
		int objectCount=0;
		while((line=in.readLine())!=null) {
			String[] parts=line.split("\t");
			String id=parts[0];
			for(int i=1;i<parts.length;i++) {
				String[] words=parts[i].toLowerCase().split("[\\s]+");
				List<String> wordsList=Arrays.asList(words);
				s.addPattern(wordsList, id);
			}
			objectCount++;
			pt.update(objectCount);
		}
		in.close();
		System.out.println("load time "+(System.currentTimeMillis()-start));
		return s;
	}
	
	public static void main(String[] args) throws Exception {
		AhoCorasickList s=loadWiki();
		List<Match> r = s.findBestMatchInStringOfWords("the movie paris is the best big apple");
		System.out.println(r);
	}
}
