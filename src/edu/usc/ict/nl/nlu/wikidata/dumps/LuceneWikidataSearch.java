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
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;

import edu.usc.ict.nl.nlu.wikidata.WikiThing;
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

	public static String addLuceneMarkers(String input) {
		return LuceneQueryConstants.START+" "+input+" "+LuceneQueryConstants.END;
	}
	public static String removeLuceneMarkers(String input) {
		if (input.startsWith(LuceneQueryConstants.START)) {
			input=input.substring(LuceneQueryConstants.START.length()+1);
			int l=input.length();
			return input.substring(0, l-(LuceneQueryConstants.END.length()+1));
		} else {
			return input;
		}
	}
	
	protected Document createDoc(String[] parts) {
		Document doc = new Document();
		doc.add(new StringField(LuceneQueryConstants.ID, parts[0].toLowerCase(), Store.YES));
		for(int i=1;i<parts.length;i++) {
			doc.add(new TextField(LuceneQueryConstants.SEARCH, addLuceneMarkers(parts[i].toLowerCase()), Store.YES));
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

	public WikiThing buildThing(WikiThing thing) throws Exception {
		List<String> strings=getSearchStringsId(thing.getName());
		if (strings!=null) {
			for(String s:strings) {
				thing.addLabel(s);
			}
		}
		return thing;
	}
	public WikiThing buildThing(String id) throws Exception {
		return buildThing(new WikiThing(id));
	}
	
	public String getLabelForId(String id) {
		try {
			List<Document> rs = find(LuceneQueryConstants.ID+":"+id.toLowerCase(), 1);
			if (rs!=null && !rs.isEmpty()) {
				Document result=rs.get(0);
				return removeLuceneMarkers(result.get(LuceneQueryConstants.SEARCH));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public List<String> getSearchStringsId(String id) {
		List<String> ret=null;
		try {
			List<Document> rs = find(LuceneQueryConstants.ID+":"+id.toLowerCase(), 1);
			if (rs!=null && !rs.isEmpty()) {
				Document result=rs.get(0);
				IndexableField[] searchFields = result.getFields(LuceneQueryConstants.SEARCH);
				if (searchFields!=null) {
					for(IndexableField f:searchFields) {
						String t=f.stringValue();
						if (!StringUtils.isEmptyString(t)) {
							if (ret==null) ret=new ArrayList<>();
							ret.add(removeLuceneMarkers(t));
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	
	public static void main(String[] args) throws Exception {
		LuceneWikidataSearch r = new LuceneWikidataSearch(new File("properties-strings.txt"));
		System.out.println(r.getSearchStringsId("p35"));
	}

}
