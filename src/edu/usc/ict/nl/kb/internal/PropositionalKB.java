package edu.usc.ict.nl.kb.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.queryparser.flexible.standard.QueryParserUtil;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;

import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;

public class PropositionalKB {
	/**
	 * 
	 */
	private final DialogueKB wrapperKB;
	private Map<String,Object> variables;

	private RAMDirectory index=null;
	private QueryParser queryParser = new QueryParser(WHOLE, new WhitespaceAnalyzer());
	private IndexWriter writer;
	private DirectoryReader reader; 
	
	private static final String RELATION="R";
	private static final String WHOLE="W";
	private static final String VALUE="V";
	private static final String ARG="A";
	
	public PropositionalKB(DialogueKB wrapperKB) {
		this.wrapperKB = wrapperKB;
		variables=new HashMap<String, Object>();
		try {
			index=new RAMDirectory();
			writer= new IndexWriter(index, new IndexWriterConfig(new StandardAnalyzer()));
			writer.commit();
			reader=DirectoryReader.open(index);
		} catch (IOException e) {
			wrapperKB.getLogger().error(e);
		}
	}
	
	public void put(DialogueKBFormula f,Object v) {
		if (f.getArgCount()==0) storeVariable(f,v);
		else storePredication(f,v);
	}
	private void storePredication(DialogueKBFormula f, Object v) {
		try {
			Document doc = new Document();
			doc.add(new StringField(WHOLE, f.toString(wrapperKB), Store.YES));
			doc.add(new StringField(RELATION, f.toString(wrapperKB), Store.YES));
			doc.add(new StringField(VALUE, v.toString(), Store.YES));
			int i=0;
			for(DialogueKBFormula a:f.getAllArgs()) {
				doc.add(new StringField(ARG+i, a.toString(wrapperKB), Store.YES));
				i++;
			}
			writer.addDocument(doc);
			writer.commit();
		} catch (Exception e) {
			wrapperKB.getLogger().error(e);
		}
	}
	private void storeVariable(DialogueKBFormula f, Object v) {
		String name=f.getName();
		name=wrapperKB.normalizeNames(name);
		Logger logger=wrapperKB.getLogger();
		if (f.isConstant() && wrapperKB.getTracing(name)) {
			Object oldValue=variables.get(name);
			if (oldValue!=v)
				if (logger!=null) logger.info("######Variable '"+name+"'(original: "+f+")"+" in KB "+this.wrapperKB.getName()+" changed value from "+oldValue+" to "+v+".");
		}
		variables.put(name, v);
	}
	
	public boolean containsKey(DialogueKBFormula f) {
		if (f.getArgCount()>0) {
			try {
				DirectoryReader newReader = getReader();
				IndexSearcher searcher = new IndexSearcher(newReader);
				String s=QueryParserUtil.escape(f.toString(wrapperKB));
				Query q = queryParser.parse(WHOLE+": "+s);
				TopDocs result = searcher.search(q,1);
				ScoreDoc[] hits = result.scoreDocs;
				if (hits.length>0) return true;
			} catch (Exception e) {
				wrapperKB.getLogger().error(e);
			}
		} else {
			String name=f.getName();
			name=this.wrapperKB.normalizeNames(name);
			return variables.containsKey(name);
		}
		return false;
	}
	public Object get(DialogueKBFormula f) {
		if (f.getArgCount()>0) {
			try {
				DirectoryReader newReader = getReader();
				IndexSearcher searcher = new IndexSearcher(newReader);
				Query q = queryParser.parse(WHOLE+": "+QueryParserUtil.escape(f.toString(wrapperKB)));
				TopDocs result = searcher.search(q,1);
				ScoreDoc[] hits = result.scoreDocs;
				if (hits.length>0) {
					Document doc = searcher.getIndexReader().document(hits[0].doc);
					String v=doc.get(VALUE);
					return Boolean.valueOf(v);
				}
			} catch (Exception e) {
				wrapperKB.getLogger().error(e);
			}
		} else {
			String name=f.getName();
			name=this.wrapperKB.normalizeNames(name);
			return variables.get(name);
		}
		return null;
	}
	
	public boolean containsKey(String fs) {
		return variables.containsKey(fs);
	}
	
	public Object get(String fs) {
		return variables.get(fs);
	}
	
	public void remove(String fs) {
		variables.remove(fs);
	}
	
	public void clear() {
		variables.clear();
	}
	
	public Set<String> getAllVariablesInThisKB() {
		if (variables!=null && !variables.isEmpty()) {
			Set<String> ret=new HashSet<String>();
			ret.addAll(variables.keySet());
			return ret;
		}
		return null;
	}
	
	private DirectoryReader getReader() {
		DirectoryReader newReader;
		try {
			newReader = DirectoryReader.openIfChanged(reader, writer, true);
			if (newReader==null) newReader=reader;
			return newReader;
		} catch (IOException e) {
			wrapperKB.getLogger().error(e);
		}
		return null;
	}
	
	public List<DialogueOperatorEffect> dumpKB() throws Exception {
		List<DialogueOperatorEffect> ret=null;
		if (variables!=null) {
			for(Entry<String, Object> cnt:variables.entrySet()) {
				Object val=cnt.getValue();
				String var=cnt.getKey();
				DialogueOperatorEffect eff=DialogueOperatorEffect.createAssignment(DialogueKBFormula.createVar(var),val,false);
				eff.setAssignmentProperties(wrapperKB.getProperties(var));
				if (ret==null) ret=new ArrayList<DialogueOperatorEffect>();
				ret.add(eff);
			}
		}
		DirectoryReader newReader = getReader();
		if (newReader.numDocs()>0) {
			for (int i=0; i<newReader.maxDoc(); i++) {
			    if (newReader.hasDeletions()) continue;
			    Document doc = newReader.document(i);
			    String f=doc.get(WHOLE);
			    String v=doc.get(VALUE);
				DialogueOperatorEffect eff=DialogueOperatorEffect.createAssignment(DialogueKBFormula.parse(f),DialogueKBFormula.parse(v),false);
				if (ret==null) ret=new ArrayList<DialogueOperatorEffect>();
				ret.add(eff);
			}
		}
		return ret;
	}
	
	public static void main(String[] args) throws IOException {
		Analyzer analyzer = new WhitespaceAnalyzer();
		TokenStream stream = analyzer.tokenStream("", "p(a,p1,d)");
		stream.reset();
		while (stream.incrementToken()) {
	        String s=stream.getAttribute(CharTermAttribute.class).toString();
	        System.out.println(s);
		}
	}
}