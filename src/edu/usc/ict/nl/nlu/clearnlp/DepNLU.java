package edu.usc.ict.nl.nlu.clearnlp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.emory.clir.clearnlp.component.AbstractComponent;
import edu.emory.clir.clearnlp.component.mode.dep.DEPConfiguration;
import edu.emory.clir.clearnlp.component.utils.GlobalLexica;
import edu.emory.clir.clearnlp.component.utils.NLPUtils;
import edu.emory.clir.clearnlp.dependency.DEPNode;
import edu.emory.clir.clearnlp.dependency.DEPTree;
import edu.emory.clir.clearnlp.tokenization.AbstractTokenizer;
import edu.emory.clir.clearnlp.util.lang.TLanguage;
import edu.usc.ict.nl.util.graph.Edge;
import edu.usc.ict.nl.util.graph.Node;

public class DepNLU
{
	final TLanguage language = TLanguage.ENGLISH;
	private AbstractComponent[] components;
	private AbstractTokenizer tokenizer;
	
	public DepNLU() {
		List<String> paths = new ArrayList<>();
		paths.add("brown-rcv1.clean.tokenized-CoNLL03.txt-c1000-freq1.txt.xz");
		GlobalLexica.initDistributionalSemanticsWords(paths);
		tokenizer = NLPUtils.getTokenizer(language);
		AbstractComponent tagger = NLPUtils.getPOSTagger   (language, "general-en-pos.xz");
		AbstractComponent parser = NLPUtils.getDEPParser   (language, "general-en-dep.xz", new DEPConfiguration("root"));
		components = new AbstractComponent[]{tagger, parser};
	}
	public List<DEPTree> parse(String text) throws Exception {
		BufferedReader input=new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes()),"UTF-8"));
		List<DEPTree> result=parse(tokenizer, components, input);
		return result;
	}
	
	public List<DEPTree> parse(AbstractTokenizer tokenizer, AbstractComponent[] components, BufferedReader input) throws Exception {
		List<DEPTree> ret=null;
		String line;
		while((line=input.readLine())!=null) {
			DEPTree tree = new DEPTree(tokenizer.tokenize(line));
			for (AbstractComponent component : components) {
				component.process(tree);
			}
			if (ret==null) ret=new ArrayList<DEPTree>();
			ret.add(tree);
		}
		return ret;
	}
	
	public String enrichedInputString(DEPTree r,String separator) {
		StringBuffer ret=null;
		if (r!=null) {
			int s=r.size();
			for(int i=1;i<s;i++) {
				DEPNode n = r.get(i);
				DEPNode head=n.getHead();
				String nPos=n.getPOSTag(),hPos=(head!=null)?head.getPOSTag():null;
				if (ret==null) ret=new StringBuffer();
				if (ret.length()>0) ret.append(" ");
				ret.append(n.getWordForm()+separator+i+separator+n.getPOSTag());
				if (head!=null) ret.append(separator+head.getWordForm()+separator+hPos);
			}
		}
		return (ret!=null)?ret.toString():null;
	}
	
	private List<Node> getSubject(DEPTree depTree) {
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
		List<DEPTree> result = parser.parse("A small triangle and a big triangle argue inside the room.");
		List subject=parser.getSubject(result.get(0));
		System.out.println(subject);
		//List verb=parser.getVerb();
		
		int id=1;
		if (result!=null) {
			for(DEPTree r:result) {
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
