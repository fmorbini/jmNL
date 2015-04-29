package edu.usc.ict.nl.parser;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import edu.usc.ict.nl.parser.semantics.ParserSemanticRulesTimeAndNumbers;

public class ChartParser {

	// class for chart and agenda items
	public class Item {
		private String nt;  		// the non-terminal or terminal symbol
		private int i;				// starting index
		private int j;				// ending index
		private Object semantics;
		private boolean terminal;

		Item(String str, Object sem,int start, int end,boolean isTerminal) {
			setString(new String(str));
			setSemantics(sem);
			setStart(start);
			setEnd(end);
			setTerminal(isTerminal);
		}

		Item(Item it) {
			setString(new String(it.getString()));
			setStart(it.getStart());
			setEnd(it.getEnd());
			setTerminal(it.isTerminal());
		}

		private void setTerminal(boolean t) {
			terminal=t;
		}
		boolean isTerminal() {
			return terminal;
		}

		public String toString() {
			return getString()+"["+getStart()+","+getEnd()+"]{"+getSemantics()+"}";
		}

		public boolean subsumes(Item i) {
			return (i.getString().equals(this.getString()) && (this.getStart()<=i.getStart()) && (this.getEnd()>=i.getEnd()));
		}

		public void setStart(int start) {
			this.i = start;
		}

		public int getStart() {
			return i;
		}

		public void setEnd(int end) {
			this.j = end;
		}

		public int getEnd() {
			return j;
		}

		public void setSemantics(Object semantics) {
			this.semantics = semantics;
		}

		public Object getSemantics() {
			return semantics;
		}
		@Override
		public boolean equals(Object obj) {
			if ((obj instanceof Item) && (obj!=null)) {
				Item objItem=(Item)obj;
				return ((i==objItem.i)&&(j==objItem.j)&&getString().equals(objItem.getString()));
			} else return super.equals(obj);
		}

		public void setString(String nt) {
			this.nt = nt;
		}

		public String getString() {
			return nt;
		}
	}

	// active arc class
	class Arc {
		int dot = 0;		// dot for dotted rules
		GrammarRule rule=null;		// the corresponding rule in the grammar
		int i = 0;			// starting index
		int j = 0;			// ending index
		public Vector<Object> semantics;

		Arc(int a, GrammarRule r, int c, int d,Vector<Object> sem) {
			dot = a;
			rule = r;
			i = c;
			j = d;
			semantics=sem;
		}
		public String toString() {
			return "<"+i+","+j+">{"+Arrays.asList(semantics)+"}";
		}
	}

	// the grammar
	Grammar gra;
	private SemanticRulesContainer semanticRules=null;
	
	public ChartParser(File fname, SemanticRulesContainer semanticRules) throws Exception {
		setSemanticRulesContainer(semanticRules);
		gra = new Grammar(fname,this);
	}

	public Vector<Item> parse(List<String> input, String rootEntity) {
		return parse(input, rootEntity, -1);
	}
	// a simple chart parser
	public Vector<Item> parse(List<String> input, String rootEntity,int start) {

		getSemanticRulesContainer().setRootEntity(rootEntity);
		
		// the chart and the agenda
		Vector<Item> chart = new Vector<Item>();
		Vector<Item> agenda = new Vector<Item>();
		HashSet<Item> addItems = new HashSet<Item>();

		// the active arcs
		// indexed by symbol after the dot in the dotted rule
		HashMap<String, HashMap<Integer, Vector<Arc>>> active = new HashMap<String, HashMap<Integer, Vector<Arc>>>();

		// add all input tokens to the agenda
		int i=0;
		Item el;
		for (String e:input) {
			if (start<0 || i>=start) {
				agenda.add(el=new Item(e, e, i, i+1,true));
				addItems.add(el);
			}
			i++;
		}

		// process the agenda
		while(!agenda.isEmpty()) {

			// get next item, remove it from the agenda
			Item citem = agenda.remove(0);

			// is there a grammar rule that starts with this nt?
			Vector<Integer> matchingRules=gra.getGrammarRulesMatchingThisRHS(citem);
			if(!matchingRules.isEmpty()) {

				// look at each such rule
				for( int rnum : matchingRules) {
					GrammarRule r = gra.rules[rnum];

					// if it's a unary rule, we complete it trivially
					// then add the lhs as a new item in the chart and the agenda
					if(r.rhs.length == 1) {
						Object sem=computeSemanticsForRule(r,citem.getSemantics());
						if (sem!=null) {
							el=new Item(r.lhs, sem, citem.getStart(), citem.getEnd(),false);
							chart.add(el);
							if (!addItems.contains(el)) {
								agenda.add(0, el);
								addItems.add(el);
							}
						}
					} else {
						// not a unary rule, so start an active arc
						addArcFor(active,1,r,citem);
					}
				}
			}

			// are there any active arcs that are looking for the current item
			// at the current position?
			if(active.containsKey(citem.getString()) && active.get(citem.getString()).containsKey(citem.getStart())) {

				// complete or extend every such arc
				for(Arc aa : active.get(citem.getString()).get(citem.getStart())) {
					int nextRHSIndex=aa.dot+1;
					// are we completing the arc?
					if(aa.rule.rhs.length == nextRHSIndex) {
						// yes: add new items to the agenda and chart
						Vector<Object> completedSemantics = new Vector<Object>(aa.semantics);
						completedSemantics.add(citem.getSemantics());
						Object sem=computeSemanticsForRule(aa.rule,completedSemantics.toArray());
						el=new Item(aa.rule.lhs, sem,aa.i, citem.getEnd(),false);
						chart.add(el);
						if (!addItems.contains(el)) {
							agenda.add(0, el);
							addItems.add(el);
						}
					}
					else {
						// no: extend the active arc
						// (this means: create a new active arc that extends the current arc)
						extendArcWith(active,aa,citem);
					}
				}
			}
		}

		// done parsing.
		return chart;
	}

	private Arc extendArcWith(HashMap<String, HashMap<Integer, Vector<Arc>>> active, Arc a,Item item) {
		Vector<Object> newSemantics = new Vector<Object>(a.semantics);
		newSemantics.add(item.getSemantics());
		Arc na = addArcFor(active, a.dot+1, a.rule, item);
		na.i=a.i;
		na.semantics=newSemantics;
		return na;
	}

	private Arc addArcFor(HashMap<String, HashMap<Integer, Vector<Arc>>> active, int rhsIndex,GrammarRule r, Item item) {
		String[] rhs = r.rhs;
		// ugly way to add to hash maps, again...
		if(!active.containsKey(rhs[rhsIndex])) {
			active.put(rhs[rhsIndex], new HashMap<Integer, Vector<Arc>>());
		}
		if(!active.get(rhs[rhsIndex]).containsKey(item.getEnd())) {
			active.get(rhs[rhsIndex]).put(item.getEnd(), new Vector<Arc>());
		}

		Vector<Object> semantics=new Vector<Object>();
		semantics.add(item.getSemantics());
		Arc a;
		// we add the active arc, indexing by the next
		// rhs symbol after the dot, and the arc's current
		// end position		
		active.get(rhs[rhsIndex]).get(item.getEnd()).add(a=new Arc(rhsIndex, r, item.getStart(), item.getEnd(),semantics));
		return a;
	}

	private Object computeSemanticsForRule(GrammarRule r,Object... semantics) {
		if (r.sem!=null)
			try {
				return r.sem.apply(semantics);
			} catch (Exception e) {
				e.printStackTrace();
				if (e instanceof InvocationTargetException)
					System.out.println(e.getCause());
				return null;
			}
			else return null;
	}

	public String untokenize(List<String> input) {
		String utterance="";
		boolean first=true;
		for (String w:input) {
			if (first) first=false;
			else utterance+=" ";
			utterance+=w;
		}
		return utterance;
	}
	
	public Collection<Item> parseAndFilter(List<String> input,String root) {
		return parseAndFilter(input, root, -1);
	}
	public Collection<Item> parseAndFilter(List<String> input,String root,int start) {
		// parse the string and get the chart
		Vector<Item> chart = parse(input,root,start);
		ArrayList<Item> output = new ArrayList<Item>();		
		int[] coverage = new int[input.size()];
		for (int i=0;i<coverage.length;i++) coverage[i]=-1;
		for(int i=0;i<chart.size();i++) {
			Item it=chart.get(i);
			//System.out.println(it.nt+" "+it.getSemantics()+" "+it.getStart()+"-"+it.getEnd());
			if (it.getString().equals(root) && (it.getSemantics()!=null)) {
				for (int j=it.getStart();j<it.getEnd();j++) {
					if ((coverage[j]<0) || (it.subsumes(chart.get(coverage[j]))))
						coverage[j]=i;
				}
			}
		}
		for(int i=0;i<coverage.length;) {
			int j=coverage[i];
			if (j>=0) {
				Item it = chart.get(j);
				output.add(it);
				i=it.getEnd();
			} else
				i++;
		}
		
		return output;
	}

	private static HashMap<String,ChartParser> preloadedParsers=new HashMap<String, ChartParser>();
	public static ChartParser getParserForGrammar(File grammarFile) throws Exception {
		return getParserForGrammar(grammarFile,new ParserSemanticRulesTimeAndNumbers());
	}
	public static ChartParser getParserForGrammar(File grammarFile,SemanticRulesContainer rulesContainer) throws Exception {
		String fileName=grammarFile.getAbsolutePath();
		ChartParser ret=preloadedParsers.get(fileName);
		if (ret==null) preloadedParsers.put(fileName, ret=new ChartParser(grammarFile,rulesContainer));
		return ret;
	}

	public SemanticRulesContainer getSemanticRulesContainer() {
		return semanticRules;
	}
	private void setSemanticRulesContainer(SemanticRulesContainer sr) {
		this.semanticRules=sr;
	}
}
