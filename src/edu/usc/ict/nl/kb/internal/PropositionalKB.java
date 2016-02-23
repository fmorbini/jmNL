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
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
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
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.TrivialDialogueKB;
import edu.usc.ict.nl.nlu.wikidata.WikiClaim;
import edu.usc.ict.nl.nlu.wikidata.dumps.Queries;

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
			doc.add(new StringField(RELATION, wrapperKB.normalizeNames(f.getName()), Store.YES));
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

				List<Arg> starArgs=null;
				String query=RELATION+": "+f.getName();
				int i=0;
				for(DialogueKBFormula a:f.getAllArgs()) {
					String name=a.getName();
					if (!name.equals("?")) {
						query+=" AND "+ARG+i+": "+name;
					}
					else {
						if (starArgs==null) starArgs=new ArrayList<>();
						starArgs.add(new Arg(i,a));
					}
					i++;
				}
				if (starArgs==null) query=WHOLE+": "+QueryParserUtil.escape(f.toString(wrapperKB));
				return find(query,Queries.MAXITEMS,starArgs);
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
	
	public Object find(String query,int n,List<Arg> args) throws Exception {
		DirectoryReader newReader = getReader();
		IndexSearcher searcher = new IndexSearcher(newReader);
		Query q = queryParser.parse(query);
		//System.out.println("query: "+q.getClass()+" "+q);
		int ln=Math.round((float)Math.log10(n));
		ScoreDoc[] hits=null;
		int sn=1;
		for(int i=1;i<=ln;i++) {
			sn=(i==ln)?n:sn*10;
			TopDocs result = searcher.search(q,sn);
			hits = result.scoreDocs;
			if (hits.length<sn || args==null) break;
		}
		List<List<Arg>> ret=null;
		for (int i = 0; i < hits.length; i++) {
			Document doc = searcher.getIndexReader().document(hits[i].doc);
			String v=doc.get(VALUE);
			if (args!=null) {
				List<Arg> ass=null;
				for(Arg a:args) {
					String av=doc.get(ARG+a.getPos());
					if (ass==null) ass=new ArrayList<>();
					ass.add(new Arg(a.getPos(),av));
				}
				if (ass!=null) {
					if (ret==null) ret=new ArrayList<>();
					ret.add(ass);
				}
			} else {
				return Boolean.valueOf(v);
			}
		}
		return ret;
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
	
	public static void main(String[] args) throws Exception {
		PropositionalKB kb = new PropositionalKB(new TrivialDialogueKB());
		kb.storePredication(DialogueKBFormula.parse("question('topic1','question.1')"),true);
		kb.storePredication(DialogueKBFormula.parse("question('topic2','question.2')"),true);
		Object r = kb.get(DialogueKBFormula.parse("question('topic1',?)"));
		System.out.println(r);
	}
}