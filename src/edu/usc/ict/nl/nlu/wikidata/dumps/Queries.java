package edu.usc.ict.nl.nlu.wikidata.dumps;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;

import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.nlu.wikidata.WikiThing;
import edu.usc.ict.nl.nlu.wikidata.WikiThing.TYPE;
import edu.usc.ict.nl.util.FileUtils;

public class Queries {
	public static final int MAXITEMS=12000000;
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
					fillWikiThing(np);
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
					fillWikiThing(np);
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
					fillWikiThing(np);
					nodes.addLast(np);
				}
				t.addEdgeTo(np, false, false,"ISA");
			}
		}
	}

	public WikiThing fillWikiThing(WikiThing thing) {
		try {
			LuceneWikidataSearch rt = (thing.getType()==TYPE.PROPERTY)?rp:ri;
			rt.buildThing(thing);
			return thing;
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
			String label=LuceneWikidataSearch.removeLuceneMarkers(d.get(LuceneQueryConstants.SEARCH));
			List<IndexableField> fields = d.getFields();
			for(IndexableField f:fields) {
				System.out.println(LuceneWikidataSearch.removeLuceneMarkers(f.stringValue()));
			}
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
	public boolean isInstanceOrSubclassOf(String item,String parent,int maxDepth) throws Exception {
		parent=parent.toUpperCase();
		item=item.toUpperCase();
		long start=System.currentTimeMillis();
		int cd=0;
		LinkedList<String> currentDepth=new LinkedList<>();
		LinkedList<String> nextDepth=new LinkedList<>();
		Set<String> alreadyVisited=new HashSet<>();
		currentDepth.push(item);
		boolean found=false;
		while(!currentDepth.isEmpty() && cd<maxDepth) {
			//System.out.println(nodes.size());
			String t=currentDepth.pop();
			if (t.equals(parent)) {
				found=true;
				break;
			}
			List<String> parents=rc.getOfWhatItIsAnInstance(t, MAXITEMS);
			addTheseChildren(t,parents,nextDepth,alreadyVisited);
			parents=rc.getOfWhatItIsASubclass(t, MAXITEMS);
			addTheseChildren(t,parents,nextDepth,alreadyVisited);
			if (currentDepth.isEmpty()) {
				currentDepth.addAll(nextDepth);
				nextDepth.clear();
				if (maxDepth>0) cd++;
			}
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

	public Set<String> getAllSubclassesAndInstancesOf(String parent,int n,int maxDepth) throws Exception {
		return getAllSubclassesAndInstancesOf(parent, n, maxDepth, true, true);
	}
	/**
	 * 
	 * @param parent the parent of which we want to find all subclasses/instances
	 * @param n the maximum number of items to be returned by each query to the lucene index
	 * @param maxDepth the maximum depth the hierarchy should be searched
	 * @param includeSubclass follow subclass edges
	 * @param includeInstance follow instance (isa) edges
	 * @return
	 * @throws Exception
	 */
	public Set<String> getAllSubclassesAndInstancesOf(String parent,int n,int maxDepth,boolean includeSubclass, boolean includeInstance) throws Exception {
		long start=System.currentTimeMillis();
		int cd=0;
		LinkedList<String> currentDepth=new LinkedList<>();
		LinkedList<String> nextDepth=new LinkedList<>();
		Set<String> alreadyVisited=new HashSet<>();
		currentDepth.push(parent);
		while(!currentDepth.isEmpty() && cd<maxDepth) {
			String t=currentDepth.pop();
			List<String> children=null;
			if (includeInstance) {
				children=rc.getThingsThatAreInstancesOf(t, n);
				addTheseChildren(t,children,nextDepth,alreadyVisited);
			}
			if (includeSubclass) {
				children=rc.getThingsThatAreSubclassesOf(t, n);
				addTheseChildren(t,children,nextDepth,alreadyVisited);
			}
			if (currentDepth.isEmpty()) {
				currentDepth.addAll(nextDepth);
				nextDepth.clear();
				if (maxDepth>0) cd++;
			}
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
			return isInstanceOrSubclassOf(thing, "Q7184903",-1);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public static void createInstaceFileForNEextraction(String parentID) throws Exception {
		Queries qs = new Queries();
		Set<String> r=qs.getAllSubclassesAndInstancesOf("q6256", MAXITEMS, 1,false,true);
		BufferedWriter x=new BufferedWriter(new FileWriter("q6256"));
		for(String t:r) {
			WikiThing thing=new WikiThing(t);
			qs.fillWikiThing(thing);
			x.write(DialogueKBFormula.generateStringConstantFromContent(thing.getName()));
			for(String l:thing.getLabels()) {
				x.write("\t"+l);
			}
			x.write("\n");
		}
		x.close();
	}
	
	public List<String> getObjectsOfThisClaim(WikiThing subject,WikiThing property) {
		List<String> objects = rc.getObjectsOfThisClaim(subject.getName(), property.getName(), MAXITEMS);
		return objects;
	}
	public List<String> getSubjectsOfThisClaim(WikiThing object,WikiThing property) {
		List<String> subjects = rc.getSubjectsOfThisClaim(object.getName(), property.getName(), MAXITEMS);
		return subjects;
	}
	
	public static void main(String[] args) throws Exception {
		//createInstaceFileForNEextraction("q6256");
		Queries qs = new Queries();
		List<WikiThing> things = qs.getIdsForString("capital", TYPE.PROPERTY, MAXITEMS);
		System.out.println(things);
		//FileUtils.dumpToFile(r, "q6256", false, true);
		/*
		qs.getInstanceAndOrSubclassGraphFor("Q51624", MAXITEMS).toGDLGraph();
		System.out.println(qs.getLabelForId(new WikiThing("q8171")));
		System.out.println(qs.getLabelForId(new WikiThing("Q21121474")));
		WikiThing thing=new WikiThing("Q21121474");
		System.out.println(qs.isInstanceOrSubclassOf(thing.getName(), "q8171",-1));
		Set<String> r=qs.getAllSubclassesAndInstancesOf("q8171", MAXITEMS,4);
		System.out.println(r.size());
		FileUtils.dumpToFile(r, "q8171", false, true);
		*/
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
