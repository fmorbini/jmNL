package edu.usc.ict.nl.nlu.wikidata.dumps;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import edu.usc.ict.nl.nlu.wikidata.WikiClaim;
import edu.usc.ict.nl.util.StringUtils;

import org.apache.lucene.document.StringField;

public class LuceneWikidataClaimsSearch extends LuceneWikidataSearch {

	public LuceneWikidataClaimsSearch(File wikidataFile) throws Exception {
		super(wikidataFile);
	}

	@Override
	protected Document createDoc(String[] parts) {
		Document doc = new Document();
		doc.add(new StringField("pred", parts[0].toLowerCase(), Store.YES));
		doc.add(new StringField("subject", parts[1].toLowerCase(), Store.YES));
		doc.add(new StringField("object", parts[2].toLowerCase(), Store.YES));
		return doc;
	}

	@Override
	public List find(String query,int n) throws Exception {
		Query q = queryParser.parse(query);
		//System.out.println("query: "+q.getClass()+" "+q);
		TopDocs result = searcher.search(q,n);
		ScoreDoc[] hits = result.scoreDocs;
		List<WikiClaim> ret=null;
		for (int i = 0; i < hits.length; i++) {
			Document doc = searcher.getIndexReader().document(hits[i].doc);
			WikiClaim thing=new WikiClaim(doc.get("subject"), doc.get("pred"), doc.get("object"));
			if (ret==null) ret=new ArrayList<WikiClaim>();
			ret.add(thing);
		}
		return ret;
	}

	public static void main(String[] args) throws Exception {
		LuceneWikidataSearch ri = new LuceneWikidataSearch(new File("items-strings.txt"));
		LuceneWikidataSearch rp = new LuceneWikidataSearch(new File("properties-strings.txt"));
		LuceneWikidataClaimsSearch rc = new LuceneWikidataClaimsSearch(new File("items-claims.txt"));
		List<WikiClaim> rs = rc.find("pred:P35 AND subject:q30", 10);
		for(WikiClaim cl:rs) {
			System.out.println(cl+": "+cl.toString(ri,rp));
		}
	}

}
