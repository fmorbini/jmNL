package edu.usc.ict.nl.nlg.lf.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.usc.ict.nl.nlg.lf.Lexicon;
import edu.usc.ict.nl.nlg.lf.WFF;
import edu.usc.ict.nl.util.graph.Node;

public class NLUGraphUtils {

	/**
	 * converts structures that encode some dependencies but that are not based on the eventuality argument into trees.s
	 * -converts ADJ(s)-NN NPs into trees in which all the ADJ(s) are parents of the head NN. 
	 * @param lf
	 * @param lex 
	 * @throws Exception 
	 */
	public static void standardizeDependencies(Node lf, Lexicon lex) throws Exception {
		NLUGraphUtils.convertADJsNNIntoTrees(lf,lex);
	}

	static void keepOnlyNN_ADJsGroups(Map<Node, Set<WFF>> typesByArg, Lexicon lex) {
		if (typesByArg!=null) {
			Iterator<Node> it=typesByArg.keySet().iterator();
			while(it.hasNext()) {
				Node arg=it.next();
				Set<WFF> types = typesByArg.get(arg);
				if (types!=null) {
					Iterator<WFF> innerit=types.iterator();
					while(innerit.hasNext()) {
						WFF t=innerit.next();
						Object nlu=t.getParsedNLUObject(true);
						String pname=NLUUtils.getPredicateName(nlu);
						if (!NLUUtils.isNoun(pname,lex) &&
								!NLUUtils.isAdjective(pname,lex)) {
							innerit.remove();
							System.err.println("  Removing element from nn-adjs refactoring of group: "+t+" remaining group: "+types);
						}
					}
					if (types.isEmpty()) it.remove();
				}
			}
		}
	}
	
	static Map<Node, Node> guessHeadForEachGroup(Map<Node, Set<WFF>> typesbyArg, Lexicon lex) {
		Map<Node, Node> ret=null;
		if (typesbyArg!=null) {
			for(Node arg:typesbyArg.keySet()) {
				Set<WFF> types = typesbyArg.get(arg);
				if (types!=null) {
					Node head=null;
					if (types.size()>1) {
						head=findSingleHeadNoun(types,lex);
					} else {
						head=types.iterator().next();
					}
					if (head!=null) {
						if (ret==null) ret=new HashMap<Node, Node>();
						ret.put(arg, head);
					}
				}
			}
		}
		return ret;
	}
	private static Node findSingleHeadNoun(Set<WFF> types, Lexicon lex) {
		if (types!=null) {
			int count=0;
			Node ret=null;
			for(WFF t:types) {
				Object nlu=t.getParsedNLUObject(true);
				String pname=NLUUtils.getPredicateName(nlu);
				if (NLUUtils.isNoun(pname,lex)) {
					count++;
					ret=t;
				}
			}
			if (count==1) return ret;
		}
		return null;
	}
	/**
	 * (sweet-adj' s6 x4) (apple-nn' e10 x4)
	 *  finds all binary predications (first argument is the eventuality name).
	 *   finds the groups that use the same second argument.
	 *    guesses which one is the head predicate for each group and puts the rest as parents of it. 
	 * @param lf
	 * @param pType 
	 * @throws Exception 
	 */
	static void convertADJsNNIntoTrees(Node lf, Lexicon lex) throws Exception {
		Set<WFF> types=findAllMonadicPredications(lf);
		Map<Node,Set<WFF>> typesByArg=groupAllMonadicPredicationsByArgument(types);
		keepOnlyNN_ADJsGroups(typesByArg,lex);
		Map<Node,Node> headByArg=guessHeadForEachGroup(typesByArg,lex);
		if (headByArg!=null) {
			for(Node arg:headByArg.keySet()) {
				Node head=headByArg.get(arg);
				Set<Node> others=new HashSet<Node>(typesByArg.get(arg));
				others.remove(head);
				if (others!=null) {
					for(Node o:others) {
						o.removeEdgeTo(arg);
						o.addEdgeTo(head, true, true);
					}
				}
			}
		}
	}
	
	/**
	 * returns a map between eventuality between eventuality name and all the NLU objects associated to that eventuality.
	 * @param lf
	 * @return
	 */
	public static Map<String, Object> getEventsFromLF(Node lf) {
		Map<String, Object> ret=null;
		if (lf!=null) {
			Set<Node> nodes=lf.getAllNodes();
			for(Node n:nodes) {
				if (n instanceof WFF) {
					if (((WFF) n).isLiteral()) {
						Object nlu=NLUUtils.parse(n.getName(), true,false);
						String event=NLUUtils.getEventualityName(nlu);
						if (ret==null) ret=new HashMap<String, Object>();
						ret.put(event, nlu);
					}
				}
			}
		}
		return ret;
	}
	public static Set<WFF> getAllWFFs(Node lf) {
		Set<WFF> ret=null;
		if (lf!=null) {
			Set<Node> nodes=lf.getAllNodes();
			for(Node n:nodes) {
				if (n instanceof WFF) {
					if (((WFF) n).isLiteral()) {
						if (ret==null) ret=new HashSet<WFF>();
						ret.add((WFF) n);
					}
				}
			}
		}
		return ret;
	}

	static Map<Node, Set<WFF>> groupAllMonadicPredicationsByArgument(Set<WFF> types) throws Exception {
		Map<String,Node> eventNode=null;
		Map<Node, Set<WFF>> ret=null;
		if (types!=null) {
			for(WFF lfnode:types) {
				Object nlu=lfnode.getParsedNLUObject(true);
				Object lastArg=NLUUtils.getMonadicPredicateArgument(nlu);
				String lastArgName=NLUUtils.toString(lastArg);
				Node typeArg=null;
				if (eventNode!=null) typeArg=eventNode.get(lastArgName);
				if (typeArg==null) {
					List<Node> args=lfnode.getAllDescendantsNamed(lastArgName);
					if (args!=null && args.size()==1) {
						if (eventNode==null) eventNode=new HashMap<String, Node>();
						eventNode.put(lastArgName, typeArg=args.get(0));
					}
				}
				if (typeArg!=null) {
					if (ret==null) ret=new HashMap<Node, Set<WFF>>();
					Set<WFF> literalsForArg=ret.get(typeArg);
					if (literalsForArg==null) ret.put(typeArg, literalsForArg=new HashSet<WFF>());
					literalsForArg.add(lfnode);
				}
			}
		}
		return ret;
	}
	public static Set<WFF> findAllMonadicPredications(Node lf) {
		Set<WFF> ret=null;
		Set<WFF> literals=getAllWFFs(lf);
		if (literals!=null) {
			for(WFF l:literals) {
				if (isMonadicPredicate(l)) {
					if (ret==null) ret=new HashSet<WFF>();
					ret.add(l);
				}
			}
		}
		return ret;
	}
	public static boolean isMonadicPredicate(Node p) {
		if (p!=null && p instanceof WFF) {
			Object nlu=((WFF) p).getParsedNLUObject(false);
			return NLUUtils.isMonadicPredication(nlu) && !((WFF) p).isNegation();
		}
		return false;
	}

	/**
	 * get the parents of modifiedNode and
	 *  -return all ADJs if it's an noun
	 *  -return all adverbs if it's a verb
	 * @param modifiedNode
	 * @param entireLF
	 * @return
	 */
	public static Set<WFF> getModifiersFor(WFF modifiedNode,Lexicon lex) {
		Set<WFF> ret=null;
		Object nlu=modifiedNode.getParsedNLUObject(false);
		String pname=NLUUtils.getPredicateName(nlu);
		if (NLUUtils.isNoun(pname, lex)) {
			try {
				Collection<Node> parents = modifiedNode.getParents();
				if (parents!=null) {
					for(Node p:parents) {
						if (NLUGraphUtils.isMonadicPredicate(p)) {
							WFF pwff=(WFF)p;
							nlu=pwff.getParsedNLUObject(false);
							pname=NLUUtils.getPredicateName(nlu);
							if (NLUUtils.isAdjective(pname, lex)) {
								if (ret==null) ret=new HashSet<WFF>();
								ret.add(pwff);
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (NLUUtils.isVerb(pname, lex)) {
			try {
				Collection<Node> parents = modifiedNode.getParents();
				if (parents!=null) {
					for(Node p:parents) {
						if (NLUGraphUtils.isMonadicPredicate(p)) {
							WFF pwff=(WFF)p;
							nlu=pwff.getParsedNLUObject(false);
							pname=NLUUtils.getPredicateName(nlu);
							if (NLUUtils.isAdverb(pname, lex)) {
								if (ret==null) ret=new HashSet<WFF>();
								ret.add(pwff);
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ret;
	}

}
