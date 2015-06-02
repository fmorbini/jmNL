package edu.usc.ict.nl.nlg.lf.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.graph.Edge;
import edu.usc.ict.nl.util.graph.GraphElement;
import edu.usc.ict.nl.util.graph.Node;

public class DotUtils {
	private static final Pattern node=Pattern.compile("([\\w]+)[\\s]*(\\[(((\\.)|([^]]))*)\\])?;");
	private static final Pattern edge=Pattern.compile("([\\w]+)[\\s]*->[\\s]*([\\w]+)[\\s]*(\\[(((\\.)|([^\\]]))*)\\])?(;)?");
	public static List<Node> read(InputStream dotFile) throws Exception {
		Map<String,Node> nodes=null;
		BufferedReader in=new BufferedReader(new InputStreamReader(dotFile));
		String line;
		while((line=in.readLine())!=null) {
			Matcher m=node.matcher(line);
			int start=0;
			while(m.find(start)) {
				start=m.end();
				String nn=m.group(1);
				String label=m.group(3);
				String literal=extractLiteral(label);
				if (nodes==null) nodes=new HashMap<String, Node>();
				Node n=nodes.get(nn);
				if (n!=null) n.setName(literal);
				else nodes.put(nn, new Node(literal));
			}
			start=0;
			m=edge.matcher(line);
			while(m.find(start)) {
				start=m.end();
				String ns=m.group(1);
				String ne=m.group(2);
				if (nodes==null) nodes=new HashMap<String, Node>();
				Node sn=nodes.get(ns);
				Node en=nodes.get(ne);
				if (sn==null) nodes.put(ns, sn=new Node());
				if (en==null) nodes.put(ne, en=new Node());
				sn.addEdgeTo(en, false, false);
			}
		}
		in.close();
		normalizeObservations(nodes);
		List<Node> obs=extractObservations(nodes);
		return obs;
	}
	public static List<Node> read(File dotFile) throws Exception {
		return read(new FileInputStream(dotFile));
	}
	/**
	 * if a node associated with a key that is o[0-9]+ has children, then unconnect them and for each child create a null node (unification nodes) that has both that child and the observation node as children.  
	 * @param nodes
	 */
	private static void normalizeObservations(Map<String, Node> nodes) {
		if (nodes!=null) {
			for(String k:nodes.keySet()) {
				if (k.matches("o[0-9]+")) {
					Node n=nodes.get(k);
					if (n.hasChildren()) {
						try {
							Collection<Node> cs = n.getImmediateChildren();
							if (cs!=null) {
								for(Node c:cs) {
									n.removeEdgeTo(c);
									Node np=new Node();
									np.setName(null);
									np.addEdgeTo(c, false, false);
									np.addEdgeTo(n, false, false);
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	private static List<Node> extractObservations(Map<String, Node> nodes) {
		List<Node> ret=null;
		for(Node n:nodes.values()) {
			if (!n.hasChildren()) {
				if (ret==null) ret=new ArrayList<Node>();
				ret.add(n);
			}
		}
		return ret;
	}
	private static final Pattern label=Pattern.compile("label[\\s]*=[\\s]*\"(((\\.)|([^\"]))*)\"");
	private static String extractLiteral(String s) {
		if (!StringUtils.isEmptyString(s)) {
			Matcher m=label.matcher(s);
			if (m.find()) {
				return m.group(1);
			}
		}
		return null;
	}
	
	public static final Pattern p=Pattern.compile("[0-9]*(\\.)?[0-9]+");
	public static boolean isEtcProbNode(Node n) {
		String name=((Node) n).getName();
		if (!StringUtils.isEmptyString(name)) {
			Matcher m=p.matcher(name);
			return m.matches();
		}
		return false;
	}
	public static boolean isLiteralNode(Node n) {
		String name=((Node) n).getName();
		return !StringUtils.isEmptyString(name) && !isEtcProbNode(n);
	}
	public static String extractLF(List<Node> obs) {
		return extractLF(obs, true);
	}
	public static String extractLF(List<Node> obs,boolean all) {
		StringBuffer ret=null;
		Set<Node> visited=null;
		if (obs!=null) {
			Stack<GraphElement> s=new Stack<GraphElement>();
			s.addAll(obs);
			while(!s.empty()) {
				GraphElement n=s.pop();
				if (n instanceof Edge) {
					if (all) {
						s.add(((Edge) n).getSource());
						s.add(((Edge) n).getTarget());
					}
				} else if (visited==null || !visited.contains(n)) {
					if (visited==null) visited=new HashSet<Node>();
					visited.add((Node) n);
					Collection<Edge> es=((Node) n).getEdges();
					if (es!=null) s.addAll(((Node) n).getEdges());
					if (isLiteralNode((Node) n)) {
						if (ret==null) ret=new StringBuffer();
						if (ret.length()==0) ret.append("(and");
						ret.append(" "+((Node) n).getName());
					}
				}
			}
			if (ret!=null && ret.length()>0) ret.append(")");
		}
		return (ret!=null)?ret.toString():null;
	}

	public static void main(String[] args) throws Exception {
		List<Node> obs = read(new File("paetc/prbs/prb-3-proof.dot"));
		String lf=extractLF(obs);
		System.out.println(lf);
	}

}
