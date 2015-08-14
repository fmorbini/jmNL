package edu.usc.ict.nl.nlu.wikidata.dumps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexNotFoundException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import edu.usc.ict.nl.util.StringUtils;

public class LuceneWikidataSearch {

	private static Class analyzerClass=null;
	private static IndexSearcher searcher;
	private static Path indexFile=null;
	private FSDirectory index=null;
	private File wikidataFile=null;
	private QueryParser queryParser=null;
	static {
		try {
			analyzerClass=Class.forName("org.apache.lucene.analysis.standard.StandardAnalyzer");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}


	public LuceneWikidataSearch(File wikidataFile) throws Exception {
		File indexName=new File(wikidataFile.getName()+".index");
		indexFile=indexName.toPath();
		this.wikidataFile=wikidataFile;
		while(true) {
			try {
				initSearch();
				break;
			} catch (IndexNotFoundException e) {
				rebuildIndex();
			}
		}
	}

	private void initSearch() throws Exception {
		if (index==null) index=FSDirectory.open(indexFile);
		searcher = new IndexSearcher(DirectoryReader.open(index));
		Constructor constructor = analyzerClass.getConstructor();
		queryParser = new QueryParser("alias", (Analyzer) constructor.newInstance());
	}

	public void rebuildIndex() throws Exception {
		System.err.println("rebuilding index for: "+wikidataFile);
		Constructor constructor = analyzerClass.getConstructor();
		IndexWriterConfig config = new IndexWriterConfig((Analyzer) constructor.newInstance());
		if (index==null) index=FSDirectory.open(indexFile);
		IndexWriter writer = new IndexWriter(index, config);
		writer.deleteAll();
		writer.commit();

		BufferedReader in=new BufferedReader(new FileReader(wikidataFile));
		String line=null;
		while((line=in.readLine())!=null) {
			String[] parts=line.split("\t");
			Document doc=createDoc(parts);
			writer.addDocument(doc);
		}
		in.close();
		writer.commit();
		writer.close();
		index.close();
		index=null;
		initSearch();
	}

	protected Document createDoc(String[] parts) {
		Document doc = new Document();
		doc.add(new StringField("id", parts[0].toLowerCase(), Store.YES));
		for(int i=1;i<parts.length;i++) {
			doc.add(new TextField("alias", parts[i].toLowerCase(), (i==1)?Store.YES:Store.NO));
		}
		return doc;
	}

	public List<String> find(String query,int n) throws Exception {
		Query q = queryParser.parse(query);
		//System.out.println("query: "+q.getClass()+" "+q);
		TopDocs result = searcher.search(q,n);
		ScoreDoc[] hits = result.scoreDocs;
		List<String> ret=null;
		for (int i = 0; i < hits.length; i++) {
			Document doc = searcher.getIndexReader().document(hits[i].doc);
			String id=doc.get("id")+"_"+doc.get("alias");
			if (!StringUtils.isEmptyString(id)) {
				if (ret==null) ret=new ArrayList<String>();
				ret.add(id);
			}
		}
		return ret;
	}

	public static void main(String[] args) throws Exception {
		LuceneWikidataSearch r = new LuceneWikidataSearch(new File("items-strings.txt"));
		List<String> rs = r.find("usa", 10);
		System.out.println(rs);
	}

}
