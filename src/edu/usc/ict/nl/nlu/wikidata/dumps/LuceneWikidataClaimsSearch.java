package edu.usc.ict.nl.nlu.wikidata.dumps;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import edu.usc.ict.nl.nlu.wikidata.WikiClaim;
import edu.usc.ict.nl.nlu.wikidata.WikiThing;
import edu.usc.ict.nl.util.graph.Node;

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
		int ln=Math.round((float)Math.log10(n));
		ScoreDoc[] hits=null;
		int sn=1;
		for(int i=1;i<=ln;i++) {
			sn=(i==ln)?n:sn*10;
			TopDocs result = searcher.search(q,sn);
			hits = result.scoreDocs;
			if (hits.length<sn) break;
		}
		List<WikiClaim> ret=null;
		for (int i = 0; i < hits.length; i++) {
			Document doc = searcher.getIndexReader().document(hits[i].doc);
			WikiClaim thing=new WikiClaim(doc.get("subject"), doc.get("pred"), doc.get("object"));
			if (ret==null) ret=new ArrayList<WikiClaim>();
			ret.add(thing);
		}
		return ret;
	}

	public Set<String> getAllRelationsWithThisAsSubject(WikiThing arg) throws Exception {
		Set<String> ret=null;
		if (arg!=null) {
			List<WikiClaim> rs = find("subject:"+arg.getName().toLowerCase(), Integer.MAX_VALUE);
			if(rs!=null) {
				for(WikiClaim cl:rs) {
					if (ret==null) ret=new HashSet<String>();
					ret.add(cl.getProperty());
				}
			}
		}
		return ret;
	}
	public Set<String> getAllRelationsWithThisAsObject(WikiThing arg) throws Exception {
		Set<String> ret=null;
		if (arg!=null) {
			List<WikiClaim> rs = find("object:"+arg.getName().toLowerCase(), Integer.MAX_VALUE);
			if(rs!=null) {
				for(WikiClaim cl:rs) {
					if (ret==null) ret=new HashSet<String>();
					ret.add(cl.getProperty());
				}
			}
		}
		return ret;
	}

	public List<String> getObjectsOfThisClaim(String subject,String property,int n) {
		List<String> ret=null;
		try {
			List<WikiClaim> rs = find("pred:"+property.toLowerCase()+" AND subject:"+subject.toLowerCase(), n);
			if (rs!=null && !rs.isEmpty()) {
				for(WikiClaim result:rs) {
					if (ret==null) ret=new ArrayList<String>();
					ret.add(result.getObject());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	public List<String> getSubjectsOfThisClaim(String object,String property,int n) {
		List<String> ret=null;
		try {
			List<WikiClaim> rs = find("pred:"+property.toLowerCase()+" AND object:"+object.toLowerCase(), n);
			if (rs!=null && !rs.isEmpty()) {
				for(WikiClaim result:rs) {
					if (ret==null) ret=new ArrayList<String>();
					ret.add(result.getSubject());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	public boolean areTheseRelated(String property,String item,int n) {
		try {
			List<WikiClaim> rs = find("pred:"+property.toLowerCase()+" AND (subject:"+item.toLowerCase()+" OR object:"+item.toLowerCase()+")", n);
			if (rs!=null && !rs.isEmpty()) return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
		
	public List<String> getOfWhatItIsAnInstance(String thing,int n) {
		List<String> rs = getObjectsOfThisClaim(thing, "p31",n);
		if (rs!=null && !rs.isEmpty()) return rs;
		return null;
	}
	public List<String> getOfWhatItIsASubclass(String thing,int n) {
		List<String> rs = getObjectsOfThisClaim(thing, "p279",n);
		if (rs!=null && !rs.isEmpty()) return rs;
		return null;
	}
	public List<String> getThingsThatAreInstancesOf(String thing,int n) {
		List<String> rs = getSubjectsOfThisClaim(thing, "p31",n);
		if (rs!=null && !rs.isEmpty()) return rs;
		return null;
	}
	public List<String> getThingsThatAreSubclassesOf(String thing,int n) {
		List<String> rs = getSubjectsOfThisClaim(thing, "p279",n);
		if (rs!=null && !rs.isEmpty()) return rs;
		return null;
	}

	
	public static void main(String[] args) throws Exception {
		/*
		LuceneWikidataClaimsSearch rc = new LuceneWikidataClaimsSearch(new File(WikidataJsonProcessing.CLAIMS));
		LuceneWikidataSearch rp = new LuceneWikidataSearch(new File("properties-strings.txt"));
		Set<String> properties = rc.getAllRelationsWithThisAsSubject(new WikiThing("q200572"));
		for(String p:properties) {
			System.out.println(p+" "+rp.getLabelForId(p));
		}*/
		//getIdsForString("actor",TYPE.ITEM);//q33999//q200572

		
		LuceneWikidataSearch ri = new LuceneWikidataSearch(new File("items-strings.txt"));
		LuceneWikidataSearch rp = new LuceneWikidataSearch(new File("properties-strings.txt"));
		LuceneWikidataClaimsSearch rc = new LuceneWikidataClaimsSearch(new File("items-claims.txt"));
		List<Document> rs2 = ri.find("search:\"^america$\"", 10);
		for(Document d:rs2) {
			System.out.println(d.get(LuceneQueryConstants.ID)+" "+removeLuceneMarkers(d.get(LuceneQueryConstants.SEARCH)));
		}
		List<WikiClaim> rs = rc.find("pred:P31 AND subject:q2842807", 10);
		for(WikiClaim cl:rs) {
			System.out.println(cl+": "+cl.toString(ri,rp));
		}
		
	}

}
