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

	private Class analyzerClass=null;
	protected IndexSearcher searcher;
	private Path indexFile=null;
	private FSDirectory index=null;
	private File wikidataFile=null;
	protected QueryParser queryParser=null;

	public LuceneWikidataSearch(File wikidataFile) throws Exception {
		analyzerClass=Class.forName("org.apache.lucene.analysis.standard.StandardAnalyzer");
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
		queryParser = new QueryParser(LuceneQueryConstants.SEARCH, (Analyzer) constructor.newInstance());
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
		doc.add(new StringField(LuceneQueryConstants.ID, parts[0].toLowerCase(), Store.YES));
		for(int i=1;i<parts.length;i++) {
			if (i==1) doc.add(new TextField(LuceneQueryConstants.ALIAS, parts[i].toLowerCase(), Store.YES));
			doc.add(new TextField(LuceneQueryConstants.SEARCH, LuceneQueryConstants.START+" "+parts[i].toLowerCase()+" "+LuceneQueryConstants.END, Store.NO));
		}
		return doc;
	}

	public List find(String query,int n) throws Exception {
		if (query.contains("^")) query=query.replace("^", LuceneQueryConstants.START+" ");
		if (query.contains("$")) query=query.replace("$", " "+LuceneQueryConstants.END);
		Query q = queryParser.parse(query);
		//System.out.println("query: "+q.getClass()+" "+q);
		TopDocs result = searcher.search(q,n);
		ScoreDoc[] hits = result.scoreDocs;
		List<Document> ret=null;
		for (int i = 0; i < hits.length; i++) {
			Document doc = searcher.getIndexReader().document(hits[i].doc);
			if (ret==null) ret=new ArrayList<Document>();
			ret.add(doc);
		}
		return ret;
	}

	public String getLabelForId(String id) {
		try {
			List<Document> rs = find(LuceneQueryConstants.ID+":"+id.toLowerCase(), 1);
			if (rs!=null && !rs.isEmpty()) {
				Document result=rs.get(0);
				return result.get(LuceneQueryConstants.ALIAS);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void main(String[] args) throws Exception {
		LuceneWikidataSearch r = new LuceneWikidataSearch(new File("properties-strings.txt"));
		System.out.println(r.getLabelForId("p35"));
	}

}
