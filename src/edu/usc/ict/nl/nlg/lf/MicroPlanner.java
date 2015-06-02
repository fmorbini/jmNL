package edu.usc.ict.nl.nlg.lf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;

import edu.usc.ict.nl.nlg.lf.pos.NP;
import edu.usc.ict.nl.nlg.lf.pos.Sentence;
import edu.usc.ict.nl.nlg.lf.pos.VP;
import edu.usc.ict.nl.nlg.lf.sorting.EvalLinearization;
import edu.usc.ict.nl.nlg.lf.time.EventNode;
import edu.usc.ict.nl.util.graph.Edge;
import edu.usc.ict.nl.util.graph.Node;

/**
 * given a temporal ordering of events and the attached logic form.
 * 	produce a linear ordering of sentences (i.e. predications) and insert the glue to take care of temporal ordering
 * 
 * generate all permutations of the nodes
 *  evaluate sorting criteria for each permutation and update a stack of top n
 * @author morbini
 *
 */
public class MicroPlanner {

	public class ElementG<T> implements Comparable<ElementG<T>> {
		T thing;
		float reward;
		public ElementG(T thing, float reward) {
			this.thing=thing;
			this.reward=reward;
		}
		@Override
		public int compareTo(ElementG<T> o) {
			return Float.compare(reward, o.reward);
		}
	}
	public class Element extends ElementG<List<Node>> {
		public Element(List<Node> index, float similarity) {
			super(index, similarity);
		}
		@Override
		public int compareTo(ElementG<List<Node>> o) {
			return Float.compare(reward, o.reward);
		}
		@Override
		public String toString() {
			StringBuffer s=new StringBuffer();
			for(Node n:thing) {
				s.append(n.getName());
			}
			return s.toString()+": "+reward;
		}
	}
	
	public static String normalizeText(String name) {
		if (name.equals("BT")) return "big triangle";
		else if (name.equals("LT")) return "little triangle";
		else if (name.equals("C")) return "circle";
		else if (name.equals("D")) return "door";
		else if (name.equals("B")) return "box";
		else if (name.equals("CORNER")) return "corner";
		else if (name.endsWith("'")) return name.substring(0, name.length()-1);
		else return name;
	}

	public List<Predication> process(Node tg,String graphOutputFileName,EvalLinearization... criteria) throws Exception {
		unpackEvents(tg);
		if (graphOutputFileName!=null) tg.toGDLGraph(graphOutputFileName);
		Set<Node> nodes = tg.getAllNodes();
		nodes.remove(tg);
		PriorityQueue<Element> result = permutations(nodes,3,criteria);
		Element top=null;
		while(!result.isEmpty()) top=result.poll();
		List<Predication> ps=null;
		for(Node n:top.thing) {
			if (n!=null) {
				List<Predication> predications = ((EventNode)n).getPredications();
				if (predications!=null) {
					if (ps==null) ps=new ArrayList<Predication>();
					ps.add(predications.get(0));
				}
			}
		}
		return ps;
	}

	public static NP createNP(String s) {
		return new NP(normalizeText(s));
	}

	public static VP buildVP(Predication p) {
		VP vp=new VP();
		vp.verb=normalizeText(p.getPredicate());
		if (p.getObject()!=null) {
			NP a=createNP(p.getObject());
			vp.addArgument(a);
		}
		if (p.getOtherObjects()!=null) {
			for(String oo:p.getOtherObjects()) {
				NP a=createNP(oo);
				vp.addArgument(a);
			}
		}
		return vp;
	}

	/** 
	 * for each node in the timegraph, extract its associated predications and add parallel nodes for each predication.
	 * @param tg
	 * @return
	 * @throws Exception 
	 */
	private void unpackEvents(Node tg) throws Exception {
		Stack<Node> s=new Stack<Node>();
		Set<Node> visited=new HashSet<Node>();
		s.push(tg);
		while(!s.isEmpty()) {
			Node n=s.pop();
			if (!visited.contains(n)) {
				visited.add(n);
				if (n!=null && n instanceof EventNode) {
					List<Predication> ps = ((EventNode)n).getPredications();
					if (ps!=null && ps.size()>1) {
						// change only if the size is greater than 1
						List<Edge> ins = n.getIncomingEdges();
						List<Edge> outs = n.getOutgoingEdges();
						boolean first=true;
						int counter=1;
						for(Predication p:ps) {
							String nname=n.getName()+"_"+counter;
							ArrayList<Predication> nps = new ArrayList<Predication>();
							nps.add(p);
							if (first) {
								// reuse the existing node
								((EventNode)n).setPredications(nps);
								n.setName(nname);
								first=false;
							} else {
								// add a new parallel node
								EventNode nn=new EventNode(nname);
								for(Edge in:ins) {
									Node source=in.getSource();
									source.addEdgeTo(nn, true, true);
								}
								for(Edge out:outs) {
									Node target=out.getTarget();
									nn.addEdgeTo(target, true, true);
								}
								((EventNode)nn).setPredications(nps);
							}
							counter++;
						}
					}
				}
				Collection cs = n.getImmediateChildren();
				if (cs!=null) s.addAll(cs);
			}
		}
	}

	private PriorityQueue<Element> permutations(Set<Node> nodes,int nbest,EvalLinearization... criteria) throws Exception {
		PriorityQueue<Element> ret = new PriorityQueue<Element>();
		permutations(nodes,nbest,new LinkedList<Node>(),ret,criteria);
		return ret;
	}
	private void permutations(Set<Node> nodes,int nbest,LinkedList<Node> pm,PriorityQueue<Element> ret,EvalLinearization... criteria) throws Exception {
		if (!nodes.isEmpty()) {
			for(Node n:new ArrayList<Node>(nodes)) {
				nodes.remove(n);
				pm.push(n);
				permutations(nodes,nbest,pm,ret,criteria);
				pm.pop();
				nodes.add(n);
			}
		} else {
			// here there is a complete permutation
			// evaluate its value
			float reward=evaluate(pm,criteria);
			ret.add(new Element(new ArrayList<Node>(pm), reward));
			if (ret.size()>nbest) ret.poll();
		}
	}

	private float evaluate(LinkedList<Node> pm, EvalLinearization[] criteria) throws Exception {
		float reward=0;
		if (criteria!=null) {
			for(EvalLinearization e:criteria) {
				float r=e.reward(pm);
				reward+=r;
			}
		}
		return reward;
	}

	public List<Sentence> generateSyntax(List<Predication> ps) {
		List<Sentence> ret=null;
		if (ps!=null) {
			Sentence s=null;
			for(Predication p:ps) {
				NP np=createNP(p.getSubject());
				VP vp=buildVP(p);
				//if same subject, add a coordination to the vp.
				// if same vp, add coordination to subject.
				boolean sameSubject=(s!=null && s.subject!=null && s.subject.equals(np));
				boolean sameVP=(s!=null && s.verbPhrase!=null && s.verbPhrase.equals(vp));
				Sentence oldS=s;
				if (!sameSubject && !sameVP) s=new Sentence();
				if (!sameSubject) s.addSubject(np);
				if (!sameVP) s.addVerbPhrase(vp);
				if (oldS!=s) {
					if (ret==null) ret=new ArrayList<Sentence>();
					ret.add(s);
				}
			}
		}
		return ret;
	}

}
