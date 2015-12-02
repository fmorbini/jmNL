package edu.usc.ict.nl.nlu.wikidata.dumps;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.document.Document;

import edu.usc.ict.nl.nlu.wikidata.WikiThing;
import edu.usc.ict.nl.nlu.wikidata.WikiThing.TYPE;

public class Queries {
	private LuceneWikidataSearch ri,rp;
	private LuceneWikidataClaimsSearch rc;
	public Queries() throws Exception {
		ri = new LuceneWikidataSearch(new File(WikidataJsonProcessing.ITEMS));
		rp = new LuceneWikidataSearch(new File(WikidataJsonProcessing.PROPERTIES));
		rc = new LuceneWikidataClaimsSearch(new File(WikidataJsonProcessing.CLAIMS));
	}
	
	public WikiThing getInstancePathFor(String thing,int n) throws Exception {
		WikiThing root=new WikiThing(thing);
		LinkedList<WikiThing> nodes=new LinkedList<>();
		nodes.push(root);
		while(!nodes.isEmpty()) {
			WikiThing t=nodes.pop();
			List<String> parents=rc.getOfWhatItIsAnInstance(t.getName(), n);
			if (parents!=null) {
				for(String p:parents) {
					WikiThing np=new WikiThing(p);
					np.addLabel(getLabelForId(np));
					nodes.addLast(np);
					t.addEdgeTo(np, true, true);
				}
			}
		}
		return root;
	}
	
	public String getLabelForId(WikiThing thing) {
		try {
			LuceneWikidataSearch rt = (thing.getType()==TYPE.PROPERTY)?rp:ri;
			List<Document> rs = rt.find(LuceneQueryConstants.ID+":"+thing.getName().toLowerCase(), 1);
			if (rs!=null && !rs.isEmpty()) {
				Document result=rs.get(0);
				return result.get(LuceneQueryConstants.ALIAS);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public List<WikiThing> getIdsForString(String searchString,WikiThing.TYPE type,int n) throws Exception {
		List<WikiThing> ret=null;
		LuceneWikidataSearch rt = (type==TYPE.PROPERTY)?rp:ri;
		List<Document> rs = rt.find("search:\"^"+searchString+"$\"", n);
		for(Document d:rs) {
			if (ret==null) ret=new ArrayList<>();
			String id = d.get(LuceneQueryConstants.ID);
			String label=d.get(LuceneQueryConstants.ALIAS);
			WikiThing path = getInstancePathFor(id, n);
			path.addLabel(label);
			ret.add(path);
			System.out.println(id+" "+label);
			List<String> parents = rc.getOfWhatItIsAnInstance(id,n);
			if (parents!=null) {
				for(String p:parents) {
					System.out.println("  instance of: "+ri.getLabelForId(p));
				}
			}
			parents = rc.getOfWhatItIsASubclass(id,n);
			if (parents!=null) {
				for(String p:parents) {
					System.out.println("  subclass of: "+ri.getLabelForId(p));
				}
			}
		}
		return ret;
	}

	/**
	 * search lucene for all matching properties and/or items using {@link item1Search} and {@link item2Search}.
	 *  for each pair of matches.
	 *   If they are two item (Q), then it tries to find a property chain that connects them.
	 *   if one is a property and the other an item, it find 
	 * @param item1Search
	 * @param item2search
	 * @return
	 */
	public List<String> getRelationsBetween(String item1Search,String item2search) {
		return null;
	}
	
	public static void main(String[] args) throws Exception {
		Queries qs = new Queries();
		List<WikiThing> r = qs.getIdsForString("pizza", TYPE.ITEM,10);
		if (r!=null) {
			for(WikiThing w:r) {
				w.toGDLGraph(w.getName()+".gdl");
			}
		}
		System.out.println(r);
		//qs.getRelationsBetween("\" I , Robot \"", "actor");
	}

}
