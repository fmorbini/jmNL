package edu.usc.ict.nl.nlg.lf.utils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.tools.sexpr.Cons;
import org.w3c.tools.sexpr.SimpleSExprStream;
import org.w3c.tools.sexpr.Symbol;

import edu.usc.ict.nl.nlg.lf.Lexicon;
import edu.usc.ict.nl.nlg.lf.WFF;
import edu.usc.ict.nl.nlg.lf.WFF.TYPE;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.graph.Node;


public class NLUUtils {
	public static final Pattern andPattern=Pattern.compile("[aA][nN][dD](')?");
	public static final Pattern implyPattern=Pattern.compile("(([iI][fF])|(=>))(')?");
	public static final Pattern notPattern=Pattern.compile("[nN][oO][tT](')?");
	public static final Pattern orPattern=Pattern.compile("[oO][rR](')?");
	public static boolean isConjunction(Object obj) {
		return isPredicationNamed(obj,andPattern);
	}
	public static boolean isImplication(Object obj) {
		return isPredicationNamed(obj,implyPattern);
	}
	public static boolean isNegation(Object obj) {
		return isPredicationNamed(obj,notPattern);
	}
	public static boolean isDisjunction(Object obj) {
		return isPredicationNamed(obj,orPattern);
	}
	public static String getPredicateName(Object obj) {
		Object c=car(obj);
		if ((c!=null) && (c instanceof Symbol)) return ((Symbol)c).toString();
		else return null;
	}
	public static boolean isPrimedPredicate(Object obj) {
		String name=getPredicateName(obj);
		return (name!=null && name.endsWith("'"));
	}
	public static String getEventualityName(Object obj) {
		if (isPrimedPredicate(obj)) {
			return toString(second(obj));
		} else return null;
	}
	public static boolean isPredicationNamed(Object obj,Pattern name) {
		String n=getPredicateName(obj);
		if (n!=null) {
			Matcher m=name.matcher(n);
			return m.matches();
		}
		return false;
	}
	public static boolean isEquality(Object obj) {
		String n=getPredicateName(obj);
		return (n!=null) && n.equalsIgnoreCase("equal");
	}
	public static Object car(Object obj) {
		if (obj!=null && obj instanceof Cons) {
			return ((Cons) obj).left();
		}
		return null;
	}
	public static Object second(Object obj) {
		return car(cdr(obj));
	}
	public static void car(Object obj,Symbol newCar) {
		if (obj!=null && obj instanceof Cons) {
			((Cons) obj).left(newCar);
		}
	}
	public static Object nth(Object n,int pos) {
		while(n!=null && n instanceof Cons && pos>0) {
			n=cdr(n);
			pos--;
		}
		return car(n);
	}
	public static Object cdr(Object obj) {
		Object ret=null;
		if (obj!=null && obj instanceof Cons) {
			return ((Cons) obj).right();
		}
		return ret;
	}
	public static void cdr(Object obj,Object newRight) {
		if (obj!=null && obj instanceof Cons) {
			((Cons) obj).right(newRight);
		}
	}
	public static Cons list(Object... args) {
		Cons ret=null,next=null;
		for(Object a:args) {
			if (a!=null) {
				if (next!=null) next.right(next=new Cons(a));
				else ret=next=new Cons(a);
			}
		}
		return ret;
	}
	public static Object nconc(Object list,Object otherList) {
		Object cdr=last(list);
		if (cdr!=null) cdr(cdr,otherList);
		return list;
	}
	public static Object last (Object list) {
		if (list==null || !(list instanceof Cons)) return null;
		else {
			while(true) {
				if (cdr(list)==null) break;
				else list=cdr(list);
			}
			return list;
		}
	}
	public static int length(Object obj) {
		int ret=0;
		while(obj!=null && obj instanceof Cons) {
			ret++;
			obj=((Cons) obj).right();
		}
		return ret;
	}
	public static List<Object> asList(Object obj) {
		List<Object> ret=null;
		if (obj!=null && obj instanceof Cons) {
			while(obj!=null && obj instanceof Cons) {
				if (ret==null) ret=new ArrayList<Object>();
				ret.add(car(obj));
				obj=cdr(obj);
			}
		}
		return ret;
	}

	public static boolean isConnective(Object obj) {
		return isConjunction(obj) || isDisjunction(obj) || isImplication(obj);
	}
	public static boolean isPredication(Object obj) {
		return !isConnective(obj);
	}
	public static boolean isConstant(String x) {
		if (x!=null) {
			if (x.startsWith("$")) return false;
			else return StringUtils.isAllUpperCase(x);
		}
		return false;
	}
	public static String toString(Object obj) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintStream ps = new PrintStream(baos);
		SimpleSExprStream.printExpr(obj, ps);
		try {
			return baos.toString("UTF8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	public static Map<String, Symbol> getSymbols(Object nlu) {
		Map<String, Symbol> ret=null;
		Stack<Object> s=new Stack<Object>();
		s.push(nlu);
		while (!s.isEmpty()) {
			Object x=s.pop();
			if (isConnective(x)) {
				Object cdr = NLUUtils.cdr(x);
				if (cdr!=null && cdr instanceof Cons) {
					Enumeration e=((Cons) cdr).elements();
					s.addAll(Collections.list(e));
				}
			} else {
				if (x!=null && x instanceof Cons) {
					Object cdr=cdr(x);
					while(cdr!=null && cdr instanceof Cons) {
						String n=toString(car(cdr));
						if (ret==null) ret=new HashMap<String,Symbol>();
						Symbol.makeSymbol(n, ret);
						cdr=cdr(cdr);
					}
				}
			}
		}
		return ret;
	}
	public static Object parse(String nlu,boolean normalizePrimes,boolean normalizeCOPAConstants) {
		SimpleSExprStream si = new SimpleSExprStream(new ByteArrayInputStream(nlu.getBytes(StandardCharsets.UTF_8)));
		try {
			Object root = si.parse();
			Map<String,Symbol> sys=si.getSymbols();
			Stack<Object> s=new Stack<Object>();
			s.push(root);
			while (!s.isEmpty()) {
				Object x=s.pop();
				if (isConnective(x)) {
					Object cdr = NLUUtils.cdr(x);
					if (cdr!=null && cdr instanceof Cons) {
						Enumeration e=((Cons) cdr).elements();
						s.addAll(Collections.list(e));
					}
				} else if (!isEquality(x)) {
					if (normalizePrimes) {
						String name=getPredicateName(x);
						if (name!=null && !name.endsWith("'")) {
							Object pred=car(x);
							if (pred!=null && pred instanceof Symbol) {
								Symbol ns=Symbol.makeSymbol(name+"'", sys);
								car(x,ns);
							}
							Object rest=cdr(x);
							Symbol newEventVariable=createNewSymbol("e",sys);
							if (newEventVariable!=null) {
								rest=new Cons(newEventVariable, rest);
								cdr(x,rest);
							}
						}
					}
					if (normalizeCOPAConstants) {
						Object cdr=cdr(x);
						while(cdr!=null) {
							Object a=car(cdr);
							String aName=toString(a);
							Symbol newArg=createNewSymbol("c",sys);
							Object newArgPredications=buildExtendedCopaConstantReplacement(aName,newArg,sys);
							if (newArgPredications!=null) {
								car(cdr,newArg);
								nconc(root,newArgPredications);
							}
							cdr=cdr(cdr);
						}
					}
				}
			}
			return root;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public static Object buildExtendedCopaConstantReplacement(String aName, Symbol newArg, Map<String,Symbol> sys) {
		Object newArgPredications=null;
		if (aName.equalsIgnoreCase("BT")) newArgPredications=NLUUtils.buildModifiedNoun("big-adj'","triangle-nn'",newArg,sys);
		else if (aName.equalsIgnoreCase("LT")) newArgPredications=NLUUtils.buildModifiedNoun("little-adj'","triangle-nn'",newArg,sys);
		else if (aName.equalsIgnoreCase("C")) newArgPredications=NLUUtils.buildModifiedNoun(null,"circle-nn'",newArg,sys);
		else if (aName.equalsIgnoreCase("D")) newArgPredications=NLUUtils.buildModifiedNoun(null,"door-nn'",newArg,sys);
		else if (aName.equalsIgnoreCase("B")) newArgPredications=NLUUtils.buildModifiedNoun(null,"box-nn'",newArg,sys);
		else if (aName.equalsIgnoreCase("CORNER")) newArgPredications=NLUUtils.buildModifiedNoun(null,"corner-nn'",newArg,sys);
		return newArgPredications;
	}
	public static Object buildModifiedNoun(String mod, String type,Symbol newArg, Map<String, Symbol> sys) {
		Object modObj=null;
		if (mod!=null) {
			modObj=list(Symbol.makeSymbol(mod, sys),createNewSymbol("e",sys),newArg);
		}
		Object mainObj=list(Symbol.makeSymbol(type, sys),createNewSymbol("e",sys),newArg);
		Object ret=list(modObj,mainObj);
		return ret;
	}

	public static Symbol createNewSymbol(String prefix,Map<String, Symbol> sys) {
		if (sys!=null) {
			int i=1;
			while(sys.get(prefix+i)!=null) {
				i++;
			}
			return Symbol.makeSymbol(prefix+i, sys);
		}
		return null;
	}

	public static List<Object> extractAllPredicatesNamed(Object obj,Pattern name) {
		List<Object> ret=null;
		Stack<Object> s=new Stack<Object>();
		s.push(obj);
		while (!s.isEmpty()) {
			Object x=s.pop();
			if (NLUUtils.isConnective(x)) {
				Object arguments = cdr(x);
				if (arguments!=null) {
					Enumeration e=((Cons) arguments).elements();
					s.addAll(Collections.list(e));
				}
			} else {
				if (isPredicationNamed(x, name)) {
					if (ret==null) ret=new ArrayList<Object>();
					ret.add(x);
				}
			}
		}
		return ret;
	}

	/**
	 * 
	 * @param os a list of parsed wffs
	 * @return the list of parsed literals contained in the input wffs.
	 */
	public static List<Object> getAllPredicates(List<Object> os) {
		List<Object> ret=null;
		if (os!=null) {
			for(Object o:os) {
				List<Object> ps = getAllPredicates(o);
				if (ret==null) ret=new ArrayList<Object>();
				ret.addAll(ps);
			}
		}
		return ret;
	}
	/**
	 * 
	 * @param os a single parsed wff
	 * @return the list of parsed literals contained in the input wff.
	 */
	public static List<Object> getAllPredicates(Object obj) {
		return extractAllPredicates(obj,-1);
	}
	public static List<Object> getAllMonadicPredicates(Object obj) {
		List<Object> ps = extractAllPredicates(obj,-1);
		if (ps!=null) {
			Iterator<Object> it=ps.iterator();
			while(it.hasNext()) {
				Object p=it.next();
				if (!isMonadicPredication(p)) it.remove();
			}
		}
		return ps;
	}
	public static boolean isMonadicPredication(Object p) {
		int l=length(p);
		if (isPrimedPredicate(p)) return l==3;
		else return l==2; 
	}
	/**
	 * given a monadic predicate as input it returns it's main argugment, no eventuality.
	 * If primed returns the second argument, if non-primed returns the only argument.
	 * @param mp
	 * @return
	 */
	public static Object getMonadicPredicateArgument(Object mp) {
		if (isPrimedPredicate(mp)) {
			assert(length(mp)==3);
			return nth(mp,2);
		} else {
			assert(length(mp)==2);
			return nth(mp,1);
		}
	}
	public static Object getArgument(Object p,int pos) {
		int l=length(p);
		if (isPrimedPredicate(p)) {
			l-=1;
			if (pos<l) return nth(p,pos+2);
		} else {
			if (pos<l) return nth(p,pos+1);
		}
		return null;
	}
	public static List<Object> getArguments(Object p) {
		List<Object> ret=null;
		if (p!=null && p instanceof Cons) {
			p=cdr(p);
			while(p!=null && p instanceof Cons) {
				Object a=car(p);
				if (ret==null) ret=new ArrayList<Object>();
				ret.add(a);
				p=cdr(p);
			}
		}
		return ret;
	}

	/**
	 * returns all predications with the specified number of arguments.
	 * @param obj
	 * @param numArguments the exact number of argumnets the predicates should have. -1 means any number
	 * @return
	 */
	public static List<Object> extractAllPredicates(Object obj,int numArguments) {
		List<Object> ret=null;
		Stack<Object> s=new Stack<Object>();
		s.push(obj);
		while (!s.isEmpty()) {
			Object x=s.pop();
			if (NLUUtils.isConnective(x)) {
				Object arguments = cdr(x);
				if (arguments!=null) {
					Enumeration e=((Cons) arguments).elements();
					s.addAll(Collections.list(e));
				}
			} else {
				int l=length(x);
				if (numArguments<0 || l==numArguments+1) { 
					if (ret==null) ret=new ArrayList<Object>();
					ret.add(x);
				}
			}
		}
		return ret;
	}
	/**
	 * returns the list of literals with the given predicate name.
	 * @param obj
	 * @param name
	 * @return
	 */
	public static List<Object> extractAllPredicatesNamed(Object obj,String name) {
		List<Object> ret=null;
		Stack<Object> s=new Stack<Object>();
		s.push(obj);
		while (!s.isEmpty()) {
			Object x=s.pop();
			if (NLUUtils.isConnective(x)) {
				Object arguments = cdr(x);
				if (arguments!=null) {
					Enumeration e=((Cons) arguments).elements();
					s.addAll(Collections.list(e));
				}
			} else {
				String n=getPredicateName(x);
				n=getName(n);
				if (n.equals(name)) { 
					if (ret==null) ret=new ArrayList<Object>();
					ret.add(x);
				}
			}
		}
		return ret;
	}

	private Map<String,Object> extractEventualities(Object nlu) throws Exception {
		List<Object> ps = getAllPredicates(nlu);
		Map<String,Object> ret=null;
		if (ps!=null) {
			for(Object p:ps) {
				String e=NLUUtils.getEventualityName(p);
				if (e!=null) {
					e=e.toLowerCase();                                                                                                                                                                                       
					if (ret!=null && ret.containsKey(e)) throw new Exception("Predications with same eventuality: "+NLUUtils.toString(p)+" and "+NLUUtils.toString(ret.get(e)));
					if (ret==null) ret=new HashMap<String, Object>();
					ret.put(e, p);
				} else throw new Exception("Predication without eventuality: "+NLUUtils.toString(p));
			}
		}
		return ret;
	}

	public static Node parseToNode(Object x) throws Exception {
		Node root=new Node("root");
		parseToNode(x, root);
		return root;
	}
	public static void parseToNode(Object x,Node parent) throws Exception {
		parseToNode(x, parent, new HashMap<String, WFF>());
	}
	/**
	 * 
	 * @param x
	 * @param parent
	 * @param nodes the actual nodes added (value) the key is the name of the node
	 * @param wffs only the nodes for the formula (wff node) associated to a certain event (key)
	 * @throws Exception
	 */
	public static void parseToNode(Object x,Node parent,Map<String,WFF> nodes) throws Exception {
		if (NLUUtils.isConjunction(x)) {
			WFF node=new WFF(WFF.TYPE.CONJ);
			parent.addEdgeTo(node, false, false);
			Object cdr = cdr(x);
			if (cdr!=null && cdr instanceof Cons) {
				Enumeration e=((Cons) cdr).elements();
				while(e.hasMoreElements()) {
					Object xc=e.nextElement();
					parseToNode(xc, node,nodes);
				}
			}
		} else if (x instanceof Cons) {
			WFF node=null;
			String nn=getEventualityName(x);
			WFF.TYPE nnType=isNegation(x)?WFF.TYPE.NEG:WFF.TYPE.SIMPLE;
			if (nn!=null) {
				node=nodes.get(nn);
				if (node==null || node.getName().equals(nn)) {
					if (node==null) node=new WFF(nnType,toString(x));
					node.setName(toString(x));
					node.setType(nnType);
					nodes.put(nn, node);
					List<Object> args = getArguments(x);
					if (args!=null) {
						boolean first=true;
						for(Object arg:args) {
							if (first) {
								// custom processing of eventuality argument. 
								first=false;
								node.addEdgeTo(new WFF(TYPE.CNST, nn), false, false);
							} else parseToNode(arg, node, nodes);
						}
					}
				}
				parent.addEdgeTo(node, false, false);
			} else {
				throw new Exception("non primed predicate: "+nn);
			}
		} else {
			WFF node=null;
			String nn=toString(x);
			
			if (nodes.containsKey(nn)) {
				node=nodes.get(nn);
			} else {
				node=new WFF(WFF.TYPE.CNST,nn);
				nodes.put(nn, node);
			}
			parent.addEdgeTo(node, false, false);
		}
	}

	public static List<Object> parse(File input,boolean normalizePrimes) throws Exception {
		SimpleSExprStream si = new SimpleSExprStream(new FileInputStream(input));
		List<Object> ret=null;
		try {
			Object obj=null;
			while((obj=si.parse())!=null) {
				Map<String,Symbol> sys=si.getSymbols();
				Stack<Object> s=new Stack<Object>();
				s.push(obj);
				while (!s.isEmpty()) {
					Object x=s.pop();
					if (NLUUtils.isConnective(x)) {
						Object cdr = NLUUtils.cdr(x);
						if (cdr!=null && cdr instanceof Cons) {
							Enumeration e=((Cons) cdr).elements();
							s.addAll(Collections.list(e));
						}
					} else {
						if (normalizePrimes) {
							String name=getPredicateName(x);
							if (name!=null && !name.endsWith("'")) {
								Object pred=car(x);
								if (pred!=null && pred instanceof Symbol) {
									Symbol ns=Symbol.makeSymbol(name+"'", sys);
									car(x,ns);
								}
								Object rest=cdr(x);
								Symbol newEventVariable=createNewSymbol("e",sys);
								if (newEventVariable!=null) {
									rest=new Cons(newEventVariable, rest);
									cdr(x,rest);
								}
							}
						}
					}
				}
				if (ret==null) ret=new ArrayList<Object>();
				ret.add(obj);
			}
			si.close();
		} catch (EOFException e) {
			si.close();
		} catch (Exception e) {
			System.err.println("Error reading character at offset: "+si.getReadChars());
			e.printStackTrace();
		} finally {
			si.close();
		}
		return ret;
	}


	private static final Pattern boxerLF=Pattern.compile("([^\\(]+)\\((.*)\\)");
	/**
	 * i-nn(e15,x1) ^ eat-vb(e8,x1,x3,u14) ^ quickly-rb(e13,e8) ^ subset-of(e12,x4,x3) ^ subset-of(e11,x5,x3) ^ sweet-adj(s6,x4) ^ apple-nn(e10,x4) ^ red-adj(s7,x5) ^ apple-nn(e9,x5)
	 * @param lf
	 * @return
	 * @throws InvocationTargetException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
	public static String convertHenryOutputToSexpr(String lf) throws Exception {
		List<String> ret=null;
		String[] ps=lf.split("\\^");
		for (String p:ps) {
			p=StringUtils.removeLeadingAndTrailingSpaces(p);
			Matcher m=boxerLF.matcher(p);
			m.matches();
			String pred=m.group(1);
			String[] args=m.group(2).split(",");
			if (ret==null) ret=new ArrayList<String>();
			ret.add("("+pred+"'"+FunctionalLibrary.printCollection(Arrays.asList(args), " ", "", " ")+")");
		}
		return FunctionalLibrary.printCollection(ret, "(and ", ")", " ");
	}


	private static final Pattern posBoxerPattern=Pattern.compile("[\\s]*(.+)-([^-']+)(')?[\\s]*");
	public static boolean isNoun(String pname, Lexicon lex) {
		return isPOS(pname, lex, "^[nN].+");
	}
	public static boolean isAdjective(String pname, Lexicon lex) {
		return isPOS(pname, lex, "^(([jJ]+.*)|(adj))");
	}
	public static boolean isVerb(String pname, Lexicon lex) {
		return isPOS(pname, lex, "^[vV].+");
	}
	public static boolean isAdverb(String pname, Lexicon lex) {
		return isPOS(pname, lex, "^[rR][bB].*");
	}
	private static boolean isPOS(String pname, Lexicon lex,String desiredPos) {
		Matcher m=posBoxerPattern.matcher(pname);
		if (m.matches()) {
			String pos=m.group(2);
			if (!StringUtils.isEmptyString(pos)) {
				return pos.matches(desiredPos);
			}
		} else if (lex!=null) {
			String pos=lex.getType(pname);
			if (!StringUtils.isEmptyString(pos)) {
				return pos.matches(desiredPos);
			}
		}
		return false;
	}
	public static String getPOS(String pname,Lexicon lex) {
		Matcher m=posBoxerPattern.matcher(pname);
		if (m.matches()) {
			String pos=m.group(2);
			if (!StringUtils.isEmptyString(pos)) {
				return pos.toUpperCase();
			}
		} else if (lex!=null) {
			String pos=lex.getType(pname);
			if (!StringUtils.isEmptyString(pos)) {
				return pos.toUpperCase();
			}
		}
		return null;
	}
	public static String getName(String pname) {
		Matcher m=posBoxerPattern.matcher(pname);
		if (m.matches()) {
			String name=m.group(1);
			return name;
		}
		return pname;
	}


	/**
	 * returns a map in which the keys are the variable names found in the input formula and the value is the new name given to all the variables in that equality group.
	 * @param nluObj
	 * @return
	 * @throws Exception
	 */
	public static Map<String, Symbol> extractEqualityGroups(Object nluObj) throws Exception {
		Map<String,Symbol> ret=null;
		Map<String, Symbol> sys = NLUUtils.getSymbols(nluObj);
		List<Object> ps = NLUUtils.getAllPredicates(nluObj);
		if (ps!=null) {
			for(Object p:ps) {
				if (NLUUtils.isEquality(p)) {
					List<Object> args = NLUUtils.asList(NLUUtils.cdr(p));
					Symbol currentGroup=null;
					for (Object a:args) {
						String vname=NLUUtils.toString(a);
						if (NLUUtils.isConstant(vname)) throw new Exception("constants unsupported");
						if (ret==null) ret=new HashMap<String, Symbol>();
						Symbol vg=ret.get(vname);
						if (currentGroup==null) {
							if (vg==null) vg=NLUUtils.createNewSymbol("eq", sys);
							currentGroup=vg;
							ret.put(vname, currentGroup);
						} else {
							if (vg==null) ret.put(vname, currentGroup);
							else {
								for(String ovname:ret.keySet()) {
									Symbol ovg=ret.get(ovname);
									if (ovg.equals(vg)) {
										ret.put(ovname, currentGroup);
									}
								}
							}
						}
					}
				}

			}
		}
		return ret;
	}
	public static Set<String> getAllVariableNamesEqualTo(Map<String, Symbol> groups, String vName) {
		Set<String> ret=new HashSet<String>();
		if (vName!=null) {
			ret.add(vName);
			if (groups!=null) {
				Symbol g = groups.get(vName);
				if (g!=null) {
					for(String v:groups.keySet()) {
						Symbol og=groups.get(v);
						if (og==g) ret.add(v);
					}
				}
			}
		}
		return ret;
	}

	public static void main(String[] args) throws Exception {
		Object s = parse("AND", false, false);

		String r=convertHenryOutputToSexpr("par(s3) ^ i-nn(e15,x1) ^ eat-vb(e8,x1,x3,u14) ^ quickly-rb(e13,e8) ^ subset-of(e12,x4,x3) ^ subset-of(e11,x5,x3) ^ sweet-adj(s6,x4) ^ apple-nn(e10,x4) ^ red-adj(s7,x5) ^ apple-nn(e9,x5)");
		//r=convertHenryOutputToSexpr("eat-vb(e6,x1,x4,u9) ^ little-adj(s2,x1) ^ triangle-nn(e8,x1) ^ circle-nn(e7,x4)");
		Node root=parseToNode(parse(r, false,true));
		NLUGraphUtils.standardizeDependencies(root,null);
		root.toGDLGraph("example.gdl");
		System.out.println(r);
	}

}
