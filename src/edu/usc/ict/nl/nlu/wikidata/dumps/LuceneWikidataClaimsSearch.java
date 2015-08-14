package edu.usc.ict.nl.nlu.wikidata.dumps;

import java.io.File;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;

public class LuceneWikidataClaimsSearch extends LuceneWikidataSearch {

	public LuceneWikidataClaimsSearch(File wikidataFile) throws Exception {
		super(wikidataFile);
	}

	@Override
	protected Document createDoc(String[] parts) {
		Document doc = new Document();
		doc.add(new StringField("pred", parts[0].toLowerCase(), Store.NO));
		doc.add(new StringField("subject", parts[1].toLowerCase(), Store.NO));
		doc.add(new StringField("object", parts[2].toLowerCase(), Store.NO));
		return doc;
	}

	public static void main(String[] args) throws Exception {
		LuceneWikidataClaimsSearch r = new LuceneWikidataClaimsSearch(new File("items-claims.txt"));
		List<String> rs = r.find("pred:P35", 10);
		System.out.println(rs);
	}

}
