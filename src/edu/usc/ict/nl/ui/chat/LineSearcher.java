package edu.usc.ict.nl.ui.chat;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import edu.usc.ict.nl.nlg.SpeechActWithProperties;
import edu.usc.ict.nl.util.StringUtils;

public class LineSearcher {
	
	private Class analyzerClass=null;
	protected IndexSearcher searcher;
	private Directory index=null;
	protected QueryParser queryParser=null;
	private Map<Integer,SpeechActWithProperties> lines;

	private static final String ID="sa";
	private static final String ROW="row";
	private static final String TEXT="text";
	public static final String START="luceneTextFieldStart";
	public static final String END="luceneTextFieldStop";

	public LineSearcher(Map<String, List<SpeechActWithProperties>> linesRaw) throws Exception {
		analyzerClass=Class.forName("org.apache.lucene.analysis.standard.StandardAnalyzer");
		rebuildIndex(linesRaw);
		initSearch();
	}

	private void initSearch() throws Exception {
		searcher = new IndexSearcher(DirectoryReader.open(index));
		Constructor constructor = analyzerClass.getConstructor();
		queryParser = new QueryParser(TEXT, (Analyzer) constructor.newInstance());
	}

	public void rebuildIndex(Map<String, List<SpeechActWithProperties>> linesRaw) throws Exception {
		Constructor constructor = analyzerClass.getConstructor();
		IndexWriterConfig config = new IndexWriterConfig((Analyzer) constructor.newInstance());
		if (index==null) index=new RAMDirectory();
		IndexWriter writer = new IndexWriter(index, config);
		writer.deleteAll();
		writer.commit();

		if (linesRaw!=null) {
			for(List<SpeechActWithProperties> ls:linesRaw.values()) {
				if (ls!=null) {
					for(SpeechActWithProperties l:ls) {
						if (lines==null) lines=new HashMap<>();
						lines.put(l.getRow(), l);
						Document doc=createDoc(l);
						writer.addDocument(doc);
					}
				}
			}
		}
		writer.commit();
		writer.close();
	}
	

	protected Document createDoc(SpeechActWithProperties l) {
		Document doc = new Document();
		doc.add(new IntField(ROW, l.getRow(), Store.YES));
		doc.add(new StringField(ID, l.getSA().toLowerCase(), Store.NO));
		doc.add(new TextField(TEXT, START+" "+l.getText().toLowerCase()+" "+END, Store.NO));
		return doc;
	}

	public List<SpeechActWithProperties> find(String query,int n) throws Exception {
		query=StringUtils.removeLeadingAndTrailingSpaces(query);
		if (query.startsWith("^")) query=START+" "+query.substring(1);
		if (query.endsWith("$")) query=query.substring(0, query.length()-1)+" "+END;
		Query q = queryParser.parse(query);
		//System.out.println("query: "+q.getClass()+" "+q);
		TopDocs result = searcher.search(q,n);
		ScoreDoc[] hits = result.scoreDocs;
		List<SpeechActWithProperties> ret=null;
		for (int i = 0; i < hits.length; i++) {
			Document doc = searcher.getIndexReader().document(hits[i].doc);
			Integer row=Integer.parseInt(doc.get(ROW));
			SpeechActWithProperties s=lines.get(row);
			if (s!=null) {
				if (ret==null) ret=new ArrayList<SpeechActWithProperties>();
				ret.add(s);
			}
		}
		return ret;
	}
}
