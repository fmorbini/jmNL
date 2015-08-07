package edu.usc.ict.nl.nlu.clearnlp;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.clearnlp.component.AbstractComponent;
import com.clearnlp.dependency.DEPNode;
import com.clearnlp.dependency.DEPTree;
import com.clearnlp.nlp.NLPGetter;
import com.clearnlp.nlp.NLPMode;
import com.clearnlp.reader.AbstractReader;
import com.clearnlp.segmentation.AbstractSegmenter;
import com.clearnlp.tokenization.AbstractTokenizer;
import com.clearnlp.util.UTInput;
import com.clearnlp.util.UTOutput;

import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.fst.sps.test.NLUTest;
import edu.usc.ict.nl.nlu.fst.train.Aligner;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.graph.Edge;
import edu.usc.ict.nl.util.graph.Node;

public class DepNLU
{
	final String language = AbstractReader.LANG_EN;
	private AbstractComponent[] components;
	private AbstractTokenizer tokenizer;
	
	public DepNLU() {
		this("general-en");
	}
	
	public DepNLU(String modelType) {
		tokenizer  = NLPGetter.getTokenizer(language);
		try {
			AbstractComponent tagger = NLPGetter.getComponent(modelType, language, NLPMode.MODE_POS);
			AbstractComponent parser = NLPGetter.getComponent(modelType, language, NLPMode.MODE_DEP);
			components = new AbstractComponent[]{tagger, parser};
		} catch (IOException e) {
			e.printStackTrace();
		}
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
			DEPTree tree = NLPGetter.toDEPTree(tokenizer.getTokens(line));
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
				String nPos=n.pos,hPos=(head!=null)?head.pos:null;
				if (ret==null) ret=new StringBuffer();
				if (ret.length()>0) ret.append(" ");
				ret.append(n.form+separator+i+separator+n.pos);
				if (head!=null) ret.append(separator+head.form+separator+hPos);
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
