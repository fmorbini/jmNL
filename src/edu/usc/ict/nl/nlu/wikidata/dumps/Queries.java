package edu.usc.ict.nl.nlu.wikidata.dumps;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;

import edu.usc.ict.nl.nlu.wikidata.WikiThing;
import edu.usc.ict.nl.nlu.wikidata.WikiThing.TYPE;
import edu.usc.ict.nl.util.ProgressTracker;

public class Queries {
	private static final int MAXITEMS=12000000;
	private LuceneWikidataSearch ri,rp;
	private LuceneWikidataClaimsSearch rc;
	public Queries() throws Exception {
		ri = new LuceneWikidataSearch(WikidataJsonProcessing.ITEMS);
		rp = new LuceneWikidataSearch(WikidataJsonProcessing.PROPERTIES);
		rc = new LuceneWikidataClaimsSearch(WikidataJsonProcessing.CLAIMS);
	}
	
	public WikiThing getInstancePathFor(String thing,int n) throws Exception {
		WikiThing root=new WikiThing(thing);
		return getInstancePathFor(root, n);
	}
	public WikiThing getInstancePathFor(WikiThing root,int n) throws Exception {
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
	public WikiThing getSubclassPathFor(String thing,int n) throws Exception {
		WikiThing root=new WikiThing(thing);
		return getSubclassPathFor(root, n);
	}
	public WikiThing getSubclassPathFor(WikiThing root,int n) throws Exception {
		LinkedList<WikiThing> nodes=new LinkedList<>();
		nodes.push(root);
		while(!nodes.isEmpty()) {
			WikiThing t=nodes.pop();
			List<String> parents=rc.getOfWhatItIsASubclass(t.getName(), n);
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
	public WikiThing getInstanceAndOrSubclassGraphFor(String thing,int n) throws Exception {
		WikiThing root=new WikiThing(thing);
		return getInstanceAndOrSubclassGraphFor(root, n);
	}
	public WikiThing getInstanceAndOrSubclassGraphFor(WikiThing root,int n) throws Exception {
		long start=System.currentTimeMillis();
		LinkedList<WikiThing> nodes=new LinkedList<>();
		Map<String,WikiThing> alreadyVisited=new HashMap<>();
		nodes.push(root);
		while(!nodes.isEmpty()) {
			//System.out.println(nodes.size());
			WikiThing t=nodes.pop();
			List<String> parents=rc.getOfWhatItIsAnInstance(t.getName(), n);
			addTheseChildren(t,parents,nodes,alreadyVisited);
			parents=rc.getOfWhatItIsASubclass(t.getName(), n);
			addTheseChildren(t,parents,nodes,alreadyVisited);
		}
		System.out.println("hierarchy finished in: "+(System.currentTimeMillis()-start));
		return root;
	}
	private void addTheseChildren(WikiThing t, List<String> parents, LinkedList<WikiThing> nodes,Map<String, WikiThing> alreadyVisited) throws Exception {
		if (parents!=null) {
			for(String p:parents) {
				p=p.toUpperCase();
				WikiThing np=alreadyVisited.get(p);
				if (np==null) {
					np=new WikiThing(p);
					alreadyVisited.put(p,np);
					np.addLabel(getLabelForId(np));
					nodes.addLast(np);
				}
				t.addEdgeTo(np, false, false,"ISA");
			}
		}
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
	public boolean isInstanceOrSubclassOf(String item,String parent) throws Exception {
		long start=System.currentTimeMillis();
		LinkedList<String> nodes=new LinkedList<>();
		Set<String> alreadyVisited=new HashSet<>();
		nodes.push(item.toUpperCase());
		boolean found=false;
		while(!nodes.isEmpty()) {
			//System.out.println(nodes.size());
			String t=nodes.pop();
			if (t.equals(parent)) {
				found=true;
				break;
			}
			List<String> parents=rc.getOfWhatItIsAnInstance(t, MAXITEMS);
			addTheseChildren(t,parents,nodes,alreadyVisited);
			parents=rc.getOfWhatItIsASubclass(t, MAXITEMS);
			addTheseChildren(t,parents,nodes,alreadyVisited);
		}
		System.out.println("searched for parent done in: "+(System.currentTimeMillis()-start));
		return found;
	}
	private void addTheseChildren(String t, List<String> parents, LinkedList<String> nodes,Set<String> alreadyVisited) throws Exception {
		if (parents!=null) {
			for(String p:parents) {
				p=p.toUpperCase();
				if (!alreadyVisited.contains(p)) {
					alreadyVisited.add(p);
					nodes.addLast(p);
				}
			}
		}
	}

	public Set<String> getAllSubclassesAndInstancesOf(String parent,int n) throws Exception {
		long start=System.currentTimeMillis();
		ProgressTracker pt=new ProgressTracker(1000, System.out);
		LinkedList<String> nodes=new LinkedList<>();
		Set<String> alreadyVisited=new HashSet<>();
		nodes.push(parent);
		while(!nodes.isEmpty()) {
			System.out.println(nodes.size());
			String t=nodes.pop();
			List<String> children=rc.getThingsThatAreInstancesOf(t, n);
			addTheseChildren(t,children,nodes,alreadyVisited);
			children=rc.getThingsThatAreSubclassesOf(t, n);
			addTheseChildren(t,children,nodes,alreadyVisited);
			pt.update(alreadyVisited.size());
		}
		System.out.println("hierarchy finished in: "+(System.currentTimeMillis()-start));
		return alreadyVisited;
	}
	
/*		
		WikiThing graph=getInstanceAndOrSubclassGraphFor(item, MAXITEMS);
		Node pn = graph.getDescendantNamed(parent);
		return pn!=null;
	}
	*/
	public boolean isAbstractObject(String thing) {
		try {
			return isInstanceOrSubclassOf(thing, "Q7184903");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static void main(String[] args) throws Exception {
		Queries qs = new Queries();
		WikiThing thing=new WikiThing("Q21121474");
		System.out.println(qs.isInstanceOrSubclassOf(thing.getName(), "Q7184903"));
		Set<String> r=qs.getAllSubclassesAndInstancesOf("Q7184903", MAXITEMS);
		System.out.println(r.size());
		/*
		List<WikiThing> r = qs.getIdsForString("pizza", TYPE.ITEM,10);
		if (r!=null) {
			for(WikiThing w:r) {
				w.toGDLGraph(w.getName()+".gdl");
			}
		}
		System.out.println(r);
		//qs.getRelationsBetween("\" I , Robot \"", "actor");
		 */
	}

}
