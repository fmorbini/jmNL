package edu.usc.ict.nl.nlu.nlp4j;

import java.io.BufferedReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.emory.mathcs.nlp.common.util.IOUtils;
import edu.emory.mathcs.nlp.component.template.node.AbstractNLPNode;
import edu.emory.mathcs.nlp.component.template.node.NLPNode;
import edu.emory.mathcs.nlp.decode.NLPDecoder;
import edu.usc.ict.nl.util.graph.Edge;
import edu.usc.ict.nl.util.graph.Node;

public class DepNLU
{
	private NLPDecoder nlp4j;
	
	public DepNLU() {
		final String configFile = "config-decode-en.xml";
		nlp4j = new NLPDecoder(IOUtils.createFileInputStream(configFile));
	}
	public List<NLPNode[]> parse(String text) throws Exception {
		List<NLPNode[]> ret=null;
		NLPNode[] bestResult = nlp4j.decode(text);
		if (bestResult!=null && bestResult.length>0) {
			ret=new ArrayList<>();
			ret.add(bestResult);
		}
		return ret;
	}
	
	public List<NLPNode[]> parse(BufferedReader input) throws Exception {
		List<NLPNode[]> ret=null;
		String line;
		while((line=input.readLine())!=null) {
			NLPNode[] res=nlp4j.decode(line);
			if (res!=null && res.length>0) {
				if (ret==null) ret=new ArrayList<>();
				ret.add(res);
			}
		}
		return ret;
	}
	
	public String enrichedInputString(NLPNode[] r,String separator) {
		StringBuffer ret=null;
		if (r!=null) {
			int s=r.length;
			for(int i=1;i<s;i++) {
				NLPNode n = r[i];
				NLPNode head=n.getDependencyHead();
				String nPos=n.getPartOfSpeechTag(),hPos=(head!=null)?head.getPartOfSpeechTag():null;
				if (ret==null) ret=new StringBuffer();
				if (ret.length()>0) ret.append(" ");
				ret.append(n.getWordForm()+separator+i+separator+n.getPartOfSpeechTag());
				if (head!=null) ret.append(separator+head.getWordForm()+separator+hPos);
			}
		}
		return (ret!=null)?ret.toString():null;
	}
	
	private List<NLPNode> getSubject2(NLPNode[] r) {
		if (r!=null) {
			int s=r.length;
			for(int i=1;i<s;i++) {
				NLPNode n = r[i];
				String l=n.getDependencyLabel();
				if (l.equalsIgnoreCase("nsubj")) {
					List<NLPNode> list = n.getDependentList();
					list.add(n);
					list.sort(new Comparator<NLPNode>() {

						@Override
						public int compare(NLPNode o1, NLPNode o2) {
							return o1.getDependentIndex(o1)-o2.getDependentIndex(o2);
						}
					});
					System.out.println(list);
				}
			}
		}
		return null;
	}
	private List<Node> getSubject(NLPNode[] depTree) {
		List<Node> ret=null;
		try {
			CONLL conll = new CONLL(depTree);
			List<DepEdge> root=conll.getAllEdgesNamed("root");
			if (root!=null) {
				for(DepEdge r:root) {
					List<Edge> oes = r.getTarget().getOutgoingEdges();
					if (oes!=null) {
						for(Edge oe:oes) {
							if (oe.getConsume().equals("nsubj")) {
								Node target = oe.getTarget();
								System.out.println(target);
								return getLeavesInOrder(target);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}

	public List<Node> getLeavesInOrder(Node start) {
		List<Node> ret=null;
		Deque<Node> q=new ArrayDeque<Node>();
		Set<Node> visited=new HashSet<Node>();
		q.add(start);
		while(!q.isEmpty()) {
			Node n=q.pop();
			if (!visited.contains(n)) {
				visited.add(n);
				if (n.hasChildren()) {
					List<Edge> oes = n.getOutgoingEdges();
					if (oes!=null) {
						for(Edge oe:oes) {
							Node target=oe.getTarget();
							if (target!=null) q.add(target);
						}
					}
				} else {
					if (ret==null) ret=new ArrayList<Node>();
					ret.add(n);
				}
			}
		}
		return ret;
	}

	public static void main(String[] args) throws Exception
	{
		DepNLU parser=new DepNLU();
		//List<NLPNode[]> result = parser.parse("A small triangle and a big triangle argue inside the room.");
		List<NLPNode[]> result = parser.parse("My wife, Donna, has served on a jury.");
		//List subject=parser.getSubject(result.get(0));
		//System.out.println(subject);
		parser.getSubject2(result.get(0));
		//List verb=parser.getVerb();
		
		int id=1;
		if (result!=null) {
			for(NLPNode[] r:result) {
				System.out.println(parser.enrichedInputString(r,"_"));
				new CONLL(r).toGDLGraph("sentence-"+id+".gdl");
				id++;
			}
		}
		
		/*
		List<TrainingDataFormat> test = Aligner.extractTrainingDataFromSingleStep1and3GoogleXLSXForSPS(NLUTest.ros9);
		
		DepNLU parser=new DepNLU();
		int id=1;
		for(TrainingDataFormat td:test) {
			List<CONLL> result = parser.parse(td.getUtterance(), System.out);
			if (result!=null) {
				for(CONLL r:result) {
					r.toGDLGraph("sentence-"+id+".gdl");
					id++;
				}
			}
		}
*/
	}


}
