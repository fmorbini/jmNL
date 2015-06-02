package edu.usc.ict.nl.nlg.lf;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.w3c.tools.sexpr.Cons;
import org.w3c.tools.sexpr.Symbol;

import edu.usc.ict.nl.nlg.lf.match.ANDMatcher;
import edu.usc.ict.nl.nlg.lf.match.ArgumentOfOtherMatcher;
import edu.usc.ict.nl.nlg.lf.match.LiteralMatcher;
import edu.usc.ict.nl.nlg.lf.match.NotMatcher;
import edu.usc.ict.nl.nlg.lf.match.TimeLiteralMatcher;
import edu.usc.ict.nl.nlg.lf.match.VerbMatcher;
import edu.usc.ict.nl.nlg.lf.pos.Coordination;
import edu.usc.ict.nl.nlg.lf.pos.DT;
import edu.usc.ict.nl.nlg.lf.pos.NP;
import edu.usc.ict.nl.nlg.lf.pos.POS;
import edu.usc.ict.nl.nlg.lf.pos.Sentence;
import edu.usc.ict.nl.nlg.lf.pos.VP;
import edu.usc.ict.nl.nlg.lf.sorting.AfterInInputMeansAfterInOrder;
import edu.usc.ict.nl.nlg.lf.sorting.AfterInTimeMeansAfterInOrder;
import edu.usc.ict.nl.nlg.lf.utils.DotUtils;
import edu.usc.ict.nl.nlg.lf.utils.NLUGraphUtils;
import edu.usc.ict.nl.nlg.lf.utils.NLUUtils;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.util.FileUtils;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.graph.Edge;
import edu.usc.ict.nl.util.graph.GraphElement;
import edu.usc.ict.nl.util.graph.Node;

/**
 * input proof graph
 *  observations + inferences
 * finds chains of explanations
 * finds arguments dependencies. if variable is an event, then the dependent is an ancestor
 *  if variable is not an event, it's unclear if there is a dependency. There is a relation but maybe not a dependency. for NN-ADJs all literals are on teh same level
 * for each argument find set of literals to be used for verbalization. then run the set of event through the ordering machinery and then build the syntax.
 *  if event, get the literal defining that event and all modifying things: if main literal is a noun, then get adjectives. Adjectives usually modify the thing not the event.
 *   if it's a clause, then get the adverbs, they modify the event. 
 * 
 * @author morbini
 *
 */
public class COPANLG2 {

	private MacroPlanner macro;
	private MicroPlanner micro;
	private Realizer realizer;
	private Lexicon lex=null;
	private Logger logger;

	public COPANLG2(String pFile, Logger logger) {
		macro=new MacroPlanner();
		micro=new MicroPlanner();
		realizer=new Realizer();
		lex=new Lexicon(pFile);
		this.logger=logger;
	}

	/**
	 * given a proof
	 *  get the observations
	 *  compute the subset of observations that are clauses.
	 *   a predicate that is a verb with subject and object(s)
	 *   -(x)remove all clauses that are arguments of other predications
	 *   -classify all other predications as either modifications or arguments or predicates or subordinate/relative clauses.
	 *   => use NLG1 criteria to sort the sentences in the subset after point x.
	 *    compose the sentence structure by using the classification above (S/ADJ/ADV/REL) and ordering.
	 *  get the chains of explanations
	 * @param proof
	 * @return
	 */
	public String process(File proof) {
		try {
			NLG2Data result=getSyntaxPrecursor(proof);
			List<POS> order=generateSyntax(result);
			return processSyntax(order);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public String processSyntax(List<POS> order) {
		return realizer.process(order);
	}
	
	public NLG2Data getSyntaxPrecursor(List<Node> obs) throws Exception {
		logger.info("Input observations: "+DotUtils.extractLF(obs,false));
		normalizeProofGraph(obs);
		
		String nlu=DotUtils.extractLF(obs); // entire LF with inferences and time predicates.
		Object nluObj=NLUUtils.parse(nlu,true,false);
		Map<String,Symbol> groupsOfEqualities=NLUUtils.extractEqualityGroups(nluObj);
		Node lf=NLUUtils.parseToNode(nluObj);
		NLUGraphUtils.standardizeDependencies(lf,lex);
		lf.toGDLGraph("lf.gdl");
		
		List<Node> obsNoTimePreds=new ArrayList<Node>(obs);
		removeLiteralsThatAre(obsNoTimePreds, new TimeLiteralMatcher());
		String nluObsOnly=DotUtils.extractLF(obsNoTimePreds,false); // LF that contains only observations
		Node nluObsOnlyLF=NLUUtils.parseToNode(NLUUtils.parse(nluObsOnly,false,false));
		NLUGraphUtils.standardizeDependencies(nluObsOnlyLF,lex);
		List<Node> rootObs=new ArrayList<Node>(obs);
		removeLiteralsThatAre(rootObs,new ArgumentOfOtherMatcher(nluObsOnlyLF));
		removeLiteralsThatAre(rootObs,new ANDMatcher(new NotMatcher(new VerbMatcher(lex)),
				new NotMatcher(new TimeLiteralMatcher())));// remove all literals that are not sentences and not time literals and arguments of others (eventuality only).
		String rootnlu=DotUtils.extractLF(rootObs,false);
		logger.info("Independent literals: "+rootnlu);
		Object independentLiterals=NLUUtils.parse(rootnlu, true,false);
		
		//select the list of literals to be sorted, the rest of the literals will be pulled in using the arguments
		// literals that are adj or adverbs (rb) should be taken out, only S should be left in.

		Node tg=macro.process(independentLiterals,"time.gdl");
		List<Predication> independentLiteralsOrder=micro.process(tg,null,new AfterInTimeMeansAfterInOrder(10),new AfterInInputMeansAfterInOrder(obs,0.1f));
		logger.info("Independent literals order: "+independentLiteralsOrder);

		List<Node> explanations = getExplanationChains(obsNoTimePreds,lf);
		if (explanations!=null) for(Node n:explanations) n.toGDLGraph(n.getName().replaceAll("[\\s]+", "_")+".gdl");
		Map<Predication,Node> associatedExplanations=attachExplanationToPredications(independentLiteralsOrder,explanations);
		return new NLG2Data(independentLiteralsOrder,lf,groupsOfEqualities,associatedExplanations);
	}
	public NLG2Data getSyntaxPrecursor(String...obs) throws Exception {
		if (obs!=null) {
			List<Node> nodes=new ArrayList<Node>();
			for(String o:obs) {
				nodes.add(new Node(o));
			}
			return getSyntaxPrecursor(nodes);
		}
		return null;
	}
	public NLG2Data getSyntaxPrecursor(InputStream in) throws Exception {
		List<Node> obs=DotUtils.read(in);
		return getSyntaxPrecursor(obs);
	}
	public NLG2Data getSyntaxPrecursor(File proof) throws Exception {
		return getSyntaxPrecursor(new FileInputStream(proof));
	}
	
	/**
	 * 
	 * @param obs this is a proof graph as read by {@link DotUtils}
	 * 
	 *  finds all literals and for the literals adds and modifies the references to the constants with logic form.
	 *  add the new literals introduced by the normalization as additional observations.
	 * @throws Exception 
	 *  
	 */
	private void normalizeProofGraph(List<Node> obs) throws Exception {
		replaceCOPAConstants(obs);
		replaceCOPAPredicates(obs);
	}
	private void replaceCOPAPredicates(List<Node> obs) throws Exception {
		String boxArgName=null;
		String lf = DotUtils.extractLF(obs, true);
		Object lfObj=NLUUtils.parse(lf, false,false);
		Map<String, Symbol> sys = NLUUtils.getSymbols(lfObj);
		List<Object> box = NLUUtils.extractAllPredicatesNamed(lfObj,"box");
		if (box!=null) {
			if (box.size()>1) throw new Exception("multiple box definitions.");
			for(Object b:box) {
				Object arg = NLUUtils.getMonadicPredicateArgument(b);
				boxArgName=NLUUtils.toString(arg);
			}
		}
		if (boxArgName==null) {
			Symbol arg=NLUUtils.createNewSymbol("c", sys);
			Symbol ev=NLUUtils.createNewSymbol("e", sys);
			boxArgName=NLUUtils.toString(arg);
			Node nob=new Node("(box-nn' "+ev+" "+boxArgName+")");
			obs.add(nob);
		}
		
		Set<Node> visited=null;
		if (obs!=null) {
			Stack<GraphElement> s=new Stack<GraphElement>();
			s.addAll(obs);
			while(!s.empty()) {
				GraphElement n=s.pop();
				if (n instanceof Edge) {
					s.add(((Edge) n).getSource());
					s.add(((Edge) n).getTarget());
				} else if (visited==null || !visited.contains(n)) {
					if (visited==null) visited=new HashSet<Node>();
					visited.add((Node) n);
					Collection<Edge> es=((Node) n).getEdges();
					if (es!=null) s.addAll(((Node) n).getEdges());
					if (DotUtils.isLiteralNode((Node) n)) {
						String l=((Node) n).getName();
						Object nluObj=NLUUtils.parse(l, true, false);
						String name=NLUUtils.getPredicateName(nluObj);
						if (isCopaPredicate(name)) {
							List<String> newArgPredications=adjustCOPAPredicates(nluObj,boxArgName);
							((Node) n).setName(NLUUtils.toString(nluObj));
							if (newArgPredications!=null) {
								for(String p:newArgPredications) {
									Node nob=new Node(p);
									obs.add(nob);
								}
							}
						}
					}
				}
			}
		}		
	}
	private List<String> adjustCOPAPredicates(Object nluObj, String boxArgName) {
		Object lastarg=NLUUtils.last(nluObj);
		if (lastarg!=null && lastarg instanceof Cons) {
			((Cons)lastarg).right(new Cons(Symbol.makeSymbol(boxArgName, null)));
		}
		return null;
	}

	private void replaceCOPAConstants(List<Node> obs) {
		Map<String,Symbol> constantToNewArg=null;
		Map<String,Object> constantToNewObs=null;
		String nlu=DotUtils.extractLF(obs,true); // entire LF with inferences and time predicates.
		Object nluObj=NLUUtils.parse(nlu, true,false);
		Map<String, Symbol> allSymbols=NLUUtils.getSymbols(nluObj);
		
		Set<Node> visited=null;
		if (obs!=null) {
			Stack<GraphElement> s=new Stack<GraphElement>();
			s.addAll(obs);
			while(!s.empty()) {
				GraphElement n=s.pop();
				if (n instanceof Edge) {
					s.add(((Edge) n).getSource());
					s.add(((Edge) n).getTarget());
				} else if (visited==null || !visited.contains(n)) {
					if (visited==null) visited=new HashSet<Node>();
					visited.add((Node) n);
					Collection<Edge> es=((Node) n).getEdges();
					if (es!=null) s.addAll(((Node) n).getEdges());
					if (DotUtils.isLiteralNode((Node) n)) {
						String l=((Node) n).getName();
						nluObj=NLUUtils.parse(l, false, false);
						if (nluObj!=null && nluObj instanceof Cons) {
							Object cdr=NLUUtils.cdr(nluObj);
							while(cdr!=null && cdr instanceof Cons) {
								String name=NLUUtils.toString(NLUUtils.car(cdr));
								if (isCopaConstant(name)) {
									name=name.toUpperCase();
									if (constantToNewArg==null) constantToNewArg=new HashMap<String, Symbol>();
									Symbol replacementVar=constantToNewArg.get(name);
									if (replacementVar==null) {
										replacementVar=NLUUtils.createNewSymbol("c",allSymbols);
										constantToNewArg.put(name, replacementVar);
									}
									if (constantToNewObs==null) constantToNewObs=new HashMap<String, Object>();
									Object newArgPredications=constantToNewObs.get(name);
									if (newArgPredications==null) {
										newArgPredications=NLUUtils.buildExtendedCopaConstantReplacement(name,replacementVar,allSymbols);
										constantToNewObs.put(name, newArgPredications);
										for(Object p:NLUUtils.asList(newArgPredications)) {
											Node nob=new Node(NLUUtils.toString(p));
											obs.add(nob);
										}
									}
									assert(replacementVar!=null && newArgPredications!=null);
									NLUUtils.car(cdr,replacementVar);
									((Node) n).setName(NLUUtils.toString(nluObj));
								}
								cdr=NLUUtils.cdr(cdr);
							}
						}
					}
				}
			}
		}		
	}
	private void replaceEqualities(List<Node> obs) {
		try {
			Map<String,Symbol> groupsOfEqualities=extractEqualityGroups(obs);
			if (groupsOfEqualities!=null) {
				Set<Node> visited=null;
				if (obs!=null) {
					Stack<GraphElement> s=new Stack<GraphElement>();
					s.addAll(obs);
					while(!s.empty()) {
						GraphElement n=s.pop();
						if (n instanceof Edge) {
							s.add(((Edge) n).getSource());
							s.add(((Edge) n).getTarget());
						} else if (visited==null || !visited.contains(n)) {
							if (visited==null) visited=new HashSet<Node>();
							visited.add((Node) n);
							Collection<Edge> es=((Node) n).getEdges();
							if (es!=null) s.addAll(((Node) n).getEdges());
							if (DotUtils.isLiteralNode((Node) n)) {
								String l=((Node) n).getName();
								Object nluObj=NLUUtils.parse(l, false, false);
								nluObj=NLUUtils.parse(l, false, false);
								if (nluObj!=null && nluObj instanceof Cons) {
									Object cdr=NLUUtils.cdr(nluObj);
									while(cdr!=null && cdr instanceof Cons) {
										String name=NLUUtils.toString(((Cons)cdr).left());
										Symbol rep=groupsOfEqualities.get(name);
										if (rep!=null) {
											((Cons)cdr).left(rep);
										}
										cdr=NLUUtils.cdr(cdr);
									}
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * returns a map in which the keys are the variable names found in the input formula and the value is the new name given to all the variables in that equality group.
	 * @param obs
	 * @return
	 * @throws Exception
	 */
	private Map<String, Symbol> extractEqualityGroups(List<Node> obs) throws Exception {
		String nlu=DotUtils.extractLF(obs,true); // entire LF with inferences and time predicates.
		Object nluObj=NLUUtils.parse(nlu, false,false);
		return NLUUtils.extractEqualityGroups(nluObj);
	}

	private boolean isCopaConstant(String aName) {
		return aName.equalsIgnoreCase("BT")||aName.equalsIgnoreCase("LT")||aName.equalsIgnoreCase("C")||aName.equalsIgnoreCase("D")||aName.equalsIgnoreCase("B")||aName.equalsIgnoreCase("CORNER");
	}
	private boolean isCopaPredicate(String aName) {
		return aName.equals("inside'")||aName.equals("outside'")||aName.equals("exit'")||aName.equals("enter'");
	}

	/**
	 * all Predications in independentLiteralsOrder should be clauses.
	 *  for each predication p in independentLiteralsOrder
	 *   build the standard S
	 * @param independentLiteralsOrder
	 * @param entireLF 
	 * @param groupsOfEqualities 
	 * @param associatedExplanations 
	 * @return
	 */
	public List<POS> generateSyntax(NLG2Data nlg2) {
		List<Predication> independentLiteralsOrder=nlg2.clauses;
		Node entireLF=nlg2.logicForm;
		Map<String, Symbol> groupsOfEqualities=nlg2.equalities;
		Map<Predication, Node> associatedExplanations=nlg2.explanations;
		Map<String,Object> eventToLiteral=nlg2.eventToLiteral;
		List<POS> ret=null;
		if (independentLiteralsOrder!=null) {
			Sentence s=null;
			for(Predication p:independentLiteralsOrder) {
				String reference=p.getEventualityName();
				s=(Sentence) createPOS(reference, entireLF, groupsOfEqualities, eventToLiteral);
						
				if (ret==null) ret=new ArrayList<POS>();
				POS ex=null;
				Node explanation=null;
				if (associatedExplanations!=null && (explanation=associatedExplanations.get(p))!=null) {
					ex=generateExplanation(explanation, entireLF, groupsOfEqualities,eventToLiteral);
				}
				if (ex!=null) {
			        Coordination ns = new Coordination();
			        ns.add(s);
			        ns.add(ex);
			        ns.setFunction("because");
			        ret.add(ns);
				} else {
					ret.add(s);
				}
			}
		}
		return ret;
	}
	public POS createPOS(String reference,Node entireLF, Map<String, Symbol> groupsOfEqualities,Map<String,Object> eventToLiteral) {
		POS ret=null;
		if (eventToLiteral!=null && eventToLiteral.containsKey(reference)) {
			Object referenceNLU=eventToLiteral.get(reference);
			String pname=NLUUtils.getPredicateName(referenceNLU);
			if (NLUUtils.isNegation(referenceNLU)) {
				ret=createPOS(NLUUtils.toString(NLUUtils.getArgument(referenceNLU, 0)), entireLF, groupsOfEqualities, eventToLiteral);
				if (ret==null || !(ret instanceof Sentence) || ((Sentence)ret).verbPhrase==null) logger.error("couldn't apply negation to result: "+NLUUtils.toString(referenceNLU));
				else ((VP)((Sentence)ret).verbPhrase).setNegated(true);
			} else if (NLUUtils.isVerb(pname, lex)) {
				Predication p = new Predication(referenceNLU,lex);
				String subjectReference=p.getSubject();
				POS np=createNP(subjectReference,entireLF,groupsOfEqualities, eventToLiteral);
				VP vp=createVP(p,entireLF,groupsOfEqualities, eventToLiteral);
				ret=new Sentence();
				((Sentence)ret).addSubject(np);
				((Sentence)ret).addVerbPhrase(vp);
			} else if (NLUUtils.isAdverb(pname, lex) || NLUUtils.isAdjective(pname, lex)) {
				ret=createPOS(NLUUtils.toString(NLUUtils.getArgument(referenceNLU, 0)), entireLF, groupsOfEqualities,eventToLiteral);
				if (ret!=null) {
					if (ret instanceof VP) {
						((VP)ret).addModifier(normalizeText(pname, lex));
					} else if (ret instanceof NP) {
						((NP)ret).addModifier(normalizeText(pname, lex));
					} else if (ret instanceof Sentence) {
						((Sentence)ret).addVerbModifier(normalizeText(pname, lex));
					} else {
						logger.error("ignoring modifier '"+pname+"' as returned argument is neither a verb nor a noun.");
					}
				}
			}
		} else {
			ret=createNP(reference, entireLF, groupsOfEqualities,eventToLiteral);
		}
		return ret;
	}
	
	/**
	 * 
	 * @param explanation
	 * @param groupsOfEqualities 
	 * @param eventToLiteral 
	 * @return
	 */
	private POS generateExplanation2(Node explanation,Node entireLF,Map<String, Symbol> groupsOfEqualities, Map<String, Object> eventToLiteral) {
		POS ret=null;
		List<Predication> reasons;
		try {
			reasons = getParentChainOfVerbs(explanation, lex);
			Sentence s=null;
			for(Predication p:reasons) {
				String reference=p.getEventualityName();
				s=(Sentence) createPOS(reference, entireLF, groupsOfEqualities, eventToLiteral);
				
				if (ret==null) ret=s;
				else if (ret instanceof Sentence) {
					Sentence olds=(Sentence)ret;
					ret=new Coordination();
					((Coordination)ret).add(olds);
					((Coordination)ret).add(s);
					((Coordination)ret).setFunction("because");
				} else {
					((Coordination)ret).add(s);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	/**
	 * get the first node of explanation
	 * 	get its parents
	 *   is it one parent? generate sentence for it (sentence x, pass it one to a further call to this function with explanation the parent)
	 *   is it more than 1 parent
	 *    generate a coordination (and) between the output of this function called on the n parents)
	 * @param explanation
	 * @param entireLF
	 * @param groupsOfEqualities
	 * @param lex
	 * @param eventToLiteral
	 * @return
	 */
	public POS generateExplanation(Node explanation,Node entireLF,Map<String, Symbol> groupsOfEqualities, Map<String, Object> eventToLiteral) {
		POS ret=null;
		while(explanation!=null && explanation.hasParents()) {
			try {
				Collection<Node> parents = explanation.getParents();
				if (parents!=null && !parents.isEmpty()) {
					Node v;
					POS s=null;
					if (parents.size()>1) {
						s=new Coordination();
						for (Node p:parents) {
							POS thisParent=generatePOSForExplanationNode(p, entireLF, groupsOfEqualities, eventToLiteral);
							POS ancestorsOfThisParent=generateExplanation(p, entireLF, groupsOfEqualities, eventToLiteral);
							if (thisParent!=null || ancestorsOfThisParent!=null) {
								POS entireParent=null;
								if (thisParent!=null) {
									entireParent=thisParent;
								}
								if (ancestorsOfThisParent!=null) {
									if (entireParent==null) entireParent=ancestorsOfThisParent;
									else {
										Sentence olds=(Sentence)entireParent;
										entireParent=new Coordination();
										((Coordination)entireParent).add(olds);
										((Coordination)entireParent).add(ancestorsOfThisParent);
										((Coordination)entireParent).setFunction("because");
									}
								}
								((Coordination)s).add(entireParent);
							}
						}
						explanation=null;
					} else {
						v=(Node) parents.iterator().next();
						s=generatePOSForExplanationNode(v, entireLF, groupsOfEqualities, eventToLiteral);
						explanation=v;
					}
					if (s!=null) {
						if (ret==null) ret=s;
						else if (ret instanceof Sentence) {
							Sentence olds=(Sentence)ret;
							ret=new Coordination();
							((Coordination)ret).add(olds);
							((Coordination)ret).add(s);
							((Coordination)ret).setFunction("because");
						} else {
							((Coordination)ret).add(s);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return ret;
	}
	private POS generatePOSForExplanationNode(Node v,Node entireLF,Map<String, Symbol> groupsOfEqualities, Map<String, Object> eventToLiteral) {
		POS ret=null;
		Object nlu=NLUUtils.parse(v.getName(), false, false);
		String pname=NLUUtils.getPredicateName(nlu);
		if (NLUUtils.isVerb(pname, lex)) {
			Predication p=new Predication(nlu,lex);
			if (p!=null) {
				String reference=p.getEventualityName();
				ret = (Sentence) createPOS(reference, entireLF, groupsOfEqualities, eventToLiteral);
			}
		}
		return ret;
	}
	private List<Predication> getParentChainOfVerbs(Node explanation, Lexicon lex) throws Exception {
		List<Predication> ret=null;
		Node v=explanation;
		while((v=v.getSingleParent())!=null) {
			Object nlu=NLUUtils.parse(v.getName(), false, false);
			String pname=NLUUtils.getPredicateName(nlu);
			if (NLUUtils.isVerb(pname, lex)) {
				Predication p=new Predication(nlu,lex);
				if (p!=null) {
					if (ret==null) ret=new ArrayList<Predication>();
					ret.add(p);
				}
			} else if (lex.contains(pname) && lex.getType(pname)==null) logger.error("POS info not available for lexicon item: "+pname);
		}
		return ret;
	}

	private VP createVP(Predication p, Node entireLF,Map<String, Symbol> groupsOfEqualities, Map<String, Object> eventToLiteral) {
		String pname=p.getPredicate();
		VP vp=new VP();
		vp.verb=normalizeText(pname, lex);
		vp.setPassive(lex.isPassive(pname));
		//String ev=p.getEventualityname();
		//List<Object> mods=NLUUtils.getModifiersFor(ev,"VB",entireLF);
		int numArguments=p.getLength();
		for(int argPos=1;argPos<numArguments;argPos++) {
			//ARGTYPE type=lex.getTypeOfArgumentAtPosition(pname, argPos);
			String arg = p.getArgument(argPos);
			if (arg!=null) {
				POS a=createPOS(arg, entireLF, groupsOfEqualities, eventToLiteral);
				vp.addArgument(a);
			}
		}
		return vp;
	}

	/*
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
	}*/

	/**
	 *  get type of thing
	 *   if reference is an eventuality, get the literal that defines it
	 *    get the literal's pos
	 *     generate accordingly
	 *   if reference is a variable find a monadic predicate that uses it as argument.
	 *    get the literal's pos
	 *     -case 1: NN-ADJs => it's a noun.
	 * @param reference
	 * @param entireLF
	 * @param groupsOfEqualities
	 * @return
	 */
	private POS createThing(String reference, Node entireLF, Map<String, Symbol> groupsOfEqualities) {
		
		return null;
	}

	/**
	 * if the input is a constant, just build an NP of that constant with a definite article.
	 * if the input is a variable, decide which literals to use to describe it and realise them
	 *  -case 1: there is 1 NN literal and several ADJ literals that define that variable. Build a modified NP.
	 * @param reference
	 * @param entireLF 
	 * @param groupsOfEqualities 
	 * @param eventToLiteral 
	 * @return
	 */
	private POS createNP(String reference, Node entireLF, Map<String, Symbol> groupsOfEqualities, Map<String, Object> eventToLiteral) {
		POS ret=null;
		if (NLUUtils.isConstant(reference)) {
			ret=new NP(reference);
			((NP)ret).determiner=DT.THE;
			return ret;
		} else {
			Map<String,Set<WFF>> literalsForMembersOfEqualityGroup=null;
			Set<String> eqReferences = NLUUtils.getAllVariableNamesEqualTo(groupsOfEqualities, reference);
			// it's a variable
			/**
			 * case 1: find if there are types defining this variable
			 *  if there is one, find if there are adjectives modifying this noun.
			 *   build NP
			 *  if there are more than 1 (considering equalities), apply the above procedure to each, build a conjunction
			 */
			boolean case1=false;
			Set<WFF> types = NLUGraphUtils.findAllMonadicPredications(entireLF);
			if (types!=null) {
				for(String eqRef:eqReferences) {
					Node referenceNode=entireLF.getNodeNamed(eqRef);
					try {
						Collection<Node> parents = referenceNode.getParents();
						//find if there is a NN parent that could be used to describe the reference.
						if (parents!=null) {
							for(Node p:parents) {
								if (NLUGraphUtils.isMonadicPredicate(p)) {
									WFF pwff=(WFF)p;
									Object nlu=pwff.getParsedNLUObject(false);
									String pname=NLUUtils.getPredicateName(nlu);
									if (NLUUtils.isNoun(pname, lex)) {
										if (literalsForMembersOfEqualityGroup==null) literalsForMembersOfEqualityGroup=new HashMap<String, Set<WFF>>();
										Set<WFF> literals=literalsForMembersOfEqualityGroup.get(eqRef);
										if (literals==null) literalsForMembersOfEqualityGroup.put(eqRef,literals=new HashSet<WFF>());
										literals.add(pwff);
										Set<WFF> mods=NLUGraphUtils.getModifiersFor(pwff,lex);
										if (mods!=null) {
											literals.addAll(mods);
										}
										break;
									}
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				if (literalsForMembersOfEqualityGroup!=null) {
					for(String vName:literalsForMembersOfEqualityGroup.keySet()) {
						Set<WFF> literals=literalsForMembersOfEqualityGroup.get(vName);
						case1=true;
						NP nn=generateThisNN_ADJsSet(literals,lex);
						if (ret==null) ret=nn;
						else if (ret instanceof NP) {
							NP oldnp=(NP)ret;
							ret=new Coordination();
							((Coordination)ret).add(oldnp);
							((Coordination)ret).add(nn);
						} else {
							((Coordination)ret).add(nn);
						}
					}
				}
			}
			/**
			 * case 2: it's a reference to an eventuality. find the literal defining that eventuality and verbalize it.
			 */
			if (!case1) {
				if (eventToLiteral!=null && eventToLiteral.containsKey(reference)) {
					ret=createPOS(reference, entireLF, groupsOfEqualities, eventToLiteral);
				}
			}
		}
		return ret;
	}

	private NP generateThisNN_ADJsSet(Set<WFF> literals, Lexicon lex) {
		NP ret=null;
		if (literals!=null) {
			for(WFF ln:literals) {
				Object l=ln.getParsedNLUObject(false);
				String pname=NLUUtils.getPredicateName(l);
				if (ret==null) ret=new NP(null);
				if (NLUUtils.isNoun(pname, lex)) {
					ret.noun=normalizeText(pname,lex);
				} else {
					ret.addModifier(normalizeText(pname,lex));
				}
			}
		}
		return ret;
	}

	private String normalizeText(String pname, Lexicon lex) {
		String ret=null;
		if (lex!=null) ret=lex.getSurface(pname);
		if (ret==null) ret=NLUUtils.getName(pname);
		if (ret.endsWith("'")) ret=ret.substring(0, ret.length()-1);
		return ret;
	}

	private boolean isThisNN_ADJsSet(Set<Object> literals, Lexicon lex) {
		if (literals!=null) {
			boolean foundNoun=false;
			for(Object l:literals) {
				String pname=NLUUtils.getPredicateName(l);
				if (NLUUtils.isNoun(pname, lex) && !foundNoun) foundNoun=true;
				else if (!NLUUtils.isAdjective(pname, lex)) return false;
			}
			return true;
		}
		return false;
	}

	/**
	 * 
	 * @param independentLiteralsOrder list of predications.
	 * @param explanations list of nodes corresponding to the observations only. each node is a leaf in a tree that represents the explanation of that observation.
	 * @return a map with key each predication and value the observation node associated with it (with its explanation)
	 */
	private Map<Predication, Node> attachExplanationToPredications(List<Predication> observations, List<Node> explanations) {
		Map<Predication, Node> ret=null;
		if (explanations!=null) {
			Map<String,Node> exp=new HashMap<String, Node>();
			for(Node e:explanations) exp.put(e.getName(),e);
			for(Predication o:observations) {
				Object oNLU=o.getSource();
				String pName=NLUUtils.toString(oNLU);
				Node e=exp.get(pName);
				if (e!=null) {
					if (ret==null) ret=new HashMap<Predication, Node>();
					ret.put(o, e);
				}
			}
		}
		return ret;
	}

	private void removeLiteralsThatAre(List<Node> obs,LiteralMatcher keep) throws Exception {
		if (obs!=null) {
			Iterator<Node> it=obs.iterator();
			while(it.hasNext()) {
				Node ob=it.next();
				Object nlu=NLUUtils.parse(ob.getName(), false,false);
				if (keep.matches(nlu)) it.remove();
			}
		}
	}

	
	/**
	 * for each observation in obs.
	 *  get all parents that are inferences (no numbers)
	 *  for all points with more than 1 parent see if the parents are related in the lf graph (by argument relations).
	 *   if they are related, detach the parent edge and attach to the related parent.
	 * @param obs
	 * @param lf
	 * @return
	 */
	public List<Node> getExplanationChains(List<Node> obs,Node lf) {
		List<Node> ret=null;
		
		for(Node ob:obs) {
			String fileName=ob.getName().replaceAll("[\\s]+", "_");
			ob.toGDLGraph(fileName+"-before.gdl");
			Node obE=getRawExplanationSubgraphFor(ob);
			obE.toGDLGraph(fileName+"-after.gdl");
			applyArgumentRelations(obE,lf); 
			obE.toGDLGraph(fileName+"-after2.gdl");
			int numNodes=obE.countNodes();
			if (numNodes>1) {
				boolean singleAncestorChain=true;
				Node x=obE;
				try {
					while((x=x.getSingleParent())!=null) {
						if (!x.hasParents()) break;
					}
				} catch (Exception e) {
					singleAncestorChain=false;
				}
				//if (singleAncestorChain) {
					if (ret==null) ret=new ArrayList<Node>();
					ret.add(obE);
				/*} else {
					System.err.println("removing chain explaining "+obE+" as it has multiple parents.");
				}*/
			}
		}
		return ret;
	}

	/**
	 * traverse the ancestors of obE.
	 *  compute all pairs in ancestors
	 *   for each pair mark if one is the argument of the other (direct or indirect).
	 *    for each of the nodes that are arguments, <mp,p> (mp uses p as argument):
	 *     for each child c of p
	 *      if c has multiple parents, (one is p) and at least another has as ancestor mp. Remove the link c-p
	 *     if p has no more children, attach it as child of mp.
	 * @param ob it's the explanation graph
	 * @param lf it's the logic form graph (includes all inferences)
	 */
	private void applyArgumentRelations2(Node ob, Node lf) {
		Set<Node> nodes = ob.getAllNodes();
		nodes.remove(ob);
		Map<Node,Node> explanationToLF=computeNodeMap(ob,lf);
		for(Node p:nodes) {
			Node lfp=explanationToLF.get(p);
			for(Node mp:nodes) {
				if (mp!=p && mp!=ob) {
					Node lfMp=explanationToLF.get(mp);
					Integer distance=lfMp.getDistanceTo(lfp);
					if (distance!=null) {
						try {
							Collection<Node> children = p.getImmediateChildren();
							for(Node c:children) {
								if (c.hasThisAncestor(mp)) {
									c.removeEdgeFrom(p);
								}
							}
							if (!p.hasChildren()) {
								mp.addEdgeTo(p, true, true);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
						
					}
				}
			}
		}
	}
	/**
	 * for every pair of nodes excluding the observation node, if the parent uses the eventuality defined by the children in its arguments, then 
	 * @param ob explanation graph (from abduction)
	 * @param lf logic form graph (argument relations)
	 */
	private void applyArgumentRelations(Node ob, Node lf) {
		Set<Node> nodes = ob.getAllNodes();
		nodes.remove(ob);
		Map<Node,Node> explanationToLF=computeNodeMap(ob,lf);
		for(Node p:nodes) {
			Node lfp=explanationToLF.get(p);
			for(Node mp:nodes) {
				if (mp!=p && mp!=ob) {
					Node lfMp=explanationToLF.get(mp);
					Integer distance=lfMp.getDistanceTo(lfp);
					if (distance!=null) {
						Object nlu=((WFF)lfp).getParsedNLUObject(false);
						String ev=NLUUtils.getEventualityName(nlu);
						nlu=((WFF)lfMp).getParsedNLUObject(false);
						try {
							Set<String> mpArgs=new HashSet<String>(FunctionalLibrary.map(NLUUtils.getArguments(nlu), NLUUtils.class.getMethod("toString", Object.class)));
							if (mpArgs.contains(ev)) {
								Collection<Node> children = p.getImmediateChildren();
								for(Node c:children) {
									//if (c.hasThisAncestor(mp)) {
										c.removeEdgeFrom(p);
									//}
								}
								//if (!p.hasChildren()) {
									mp.addEdgeTo(p, true, true);
								//}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	/**
	 * returns a map from nodes in the from graph to the corresponding nodes in the to graph. Corresponding means with the same name.
	 * @param from
	 * @param to
	 * @return
	 */
	private Map<Node, Node> computeNodeMap(Node from, Node to) {
		Map<String,Node> fromMap=new HashMap<String, Node>(),toMap=new HashMap<String, Node>();
		Set<Node> nodes=from.getAllNodes();
		for(Node n:nodes) fromMap.put(n.getName(), n);
		nodes=to.getAllNodes();
		for(Node n:nodes) toMap.put(n.getName(), n);
		Map<Node,Node> ret=new HashMap<Node, Node>();
		for(String fromName:fromMap.keySet()) {
			Node toNode=toMap.get(fromName);
			ret.put(fromMap.get(fromName), toNode);
		}
		return ret;
	}

	/**
	 * given the observation node, get all its parents that are not numbers.
	 * @param ob
	 * @return
	 */
	private Node getRawExplanationSubgraphFor(Node ob) {
		Map<Node,Node> oldToNew=new HashMap<Node, Node>();
		Stack<Node> s=new Stack<Node>();
		s.push(ob);
		while(!s.isEmpty()) {
			Node x=s.pop();
			Node nx=getNewNode(x, oldToNew);
			try {
				Collection<Node> parents = x.getImmediateParents();
				if (parents!=null) {
					Stack<Node> pStack=new Stack<Node>();
					pStack.addAll(parents);
					while(!pStack.isEmpty()) {
						Node p=pStack.pop();
						String name=p.getName();
						if (StringUtils.isEmptyString(name)) {
							Collection<Node> pps = p.getImmediateParents();
							if (pps!=null) {
								pStack.addAll(pps);
							}
						} else if (DotUtils.isLiteralNode(p)) {
							s.push(p);
							getNewNode(p,oldToNew).addEdgeTo(nx, true, true);
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return oldToNew.get(ob);
	}

	private Node getNewNode(Node x, Map<Node, Node> oldToNew) {
		Node nx=oldToNew.get(x);
		if (nx==null) oldToNew.put(x,nx=new Node(x.getName()));
		return nx;
	}

	public static void main(String[] args) throws Exception {
	}
}
