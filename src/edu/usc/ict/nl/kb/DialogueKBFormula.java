package edu.usc.ict.nl.kb;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.dm.reward.model.macro.FormulaMacro;
import edu.usc.ict.nl.dm.reward.model.macro.Macro;
import edu.usc.ict.nl.dm.reward.model.macro.MacroRepository;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.kb.cf.CFMin;
import edu.usc.ict.nl.kb.cf.CFPrint;
import edu.usc.ict.nl.kb.cf.CFRandom;
import edu.usc.ict.nl.kb.cf.CFRound;
import edu.usc.ict.nl.kb.cf.CFTrace;
import edu.usc.ict.nl.kb.cf.CFclear;
import edu.usc.ict.nl.kb.cf.CFconcatenate;
import edu.usc.ict.nl.kb.cf.CFcurrentTime;
import edu.usc.ict.nl.kb.cf.CFexists;
import edu.usc.ict.nl.kb.cf.CFfollows;
import edu.usc.ict.nl.kb.cf.CFget;
import edu.usc.ict.nl.kb.cf.CFgetLastTimeMark;
import edu.usc.ict.nl.kb.cf.CFhasBeenInterrupted;
import edu.usc.ict.nl.kb.cf.CFif;
import edu.usc.ict.nl.kb.cf.CFintersect;
import edu.usc.ict.nl.kb.cf.CFisCurrentTopic;
import edu.usc.ict.nl.kb.cf.CFisInterruptible;
import edu.usc.ict.nl.kb.cf.CFisKnown;
import edu.usc.ict.nl.kb.cf.CFisLastNonNullTopic;
import edu.usc.ict.nl.kb.cf.CFisQuestion;
import edu.usc.ict.nl.kb.cf.CFlen;
import edu.usc.ict.nl.kb.cf.CFmatch;
import edu.usc.ict.nl.kb.cf.CFnewMap;
import edu.usc.ict.nl.kb.cf.CFnluQuery;
import edu.usc.ict.nl.kb.cf.CFnumToString;
import edu.usc.ict.nl.kb.cf.CFnumberIPUs;
import edu.usc.ict.nl.kb.cf.CFremoveIf;
import edu.usc.ict.nl.kb.cf.CFremoveIfNot;
import edu.usc.ict.nl.kb.cf.CFset;
import edu.usc.ict.nl.kb.cf.CFsubtract;
import edu.usc.ict.nl.kb.cf.CFtoUnit;
import edu.usc.ict.nl.kb.cf.CFunion;
import edu.usc.ict.nl.kb.cf.CustomFunctionInterface;
import edu.usc.ict.nl.kb.parser.FormulaGrammar;
import edu.usc.ict.nl.nlu.Token.TokenTypes;
import edu.usc.ict.nl.util.NumberUtils;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.graph.Edge;
import edu.usc.ict.nl.util.graph.Node;
import edu.usc.ict.nl.utils.FloatAndLongUtils;

public class DialogueKBFormula extends Node {

	private static final HashMap<String,CustomFunctionInterface> customFunctions=new HashMap<String, CustomFunctionInterface>();
	static {
		addCustomFunction(new CFclear());
		addCustomFunction(new CFconcatenate());
		addCustomFunction(new CFcurrentTime());
		addCustomFunction(new CFexists());
		addCustomFunction(new CFfollows());
		addCustomFunction(new CFif());
		addCustomFunction(new CFintersect());
		addCustomFunction(new CFget());
		addCustomFunction(new CFgetLastTimeMark());
		addCustomFunction(new CFhasBeenInterrupted());
		addCustomFunction(new CFisCurrentTopic());
		addCustomFunction(new CFisInterruptible());
		addCustomFunction(new CFisKnown());
		addCustomFunction(new CFisLastNonNullTopic());
		addCustomFunction(new CFisQuestion());
		addCustomFunction(new CFlen());
		addCustomFunction(new CFmatch());
		addCustomFunction(new CFMin());
		addCustomFunction(new CFnluQuery());
		addCustomFunction(new CFnewMap());
		addCustomFunction(new CFnumberIPUs());
		addCustomFunction(new CFnumToString());
		addCustomFunction(new CFPrint());
		addCustomFunction(new CFRandom());
		addCustomFunction(new CFremoveIf());
		addCustomFunction(new CFremoveIfNot());
		addCustomFunction(new CFRound());
		addCustomFunction(new CFset());
		addCustomFunction(new CFsubtract());
		addCustomFunction(new CFtoUnit());
		addCustomFunction(new CFTrace());
		addCustomFunction(new CFunion());
	}
	public static void addCustomFunction(CustomFunctionInterface cf) {
		customFunctions.put(cf.getName(),cf);
	}

	public static final String STARARG="?";
	
	private static final TrivialDialogueKB simplifyKB=new TrivialDialogueKB();

	private enum BooleanOp {AND,OR,NOT};
	public enum NumOp {ADD,SUB,MUL,DIV};
	public enum CmpOp {EQ,NE,GE,LE,GT,LT};
	public enum Type {BOOL,NUMBER,STRING,PRED,NUMPRED,CMP,TRUE,FALSE,QUOTED,NULL,CUSTOM};
	private Type type;

	private static final Map<String,Macro> macros=new HashMap<String,Macro>();
	
	protected static HashMap<String,LTS> basicElements=new HashMap<String, LTS>();

	public static DialogueKBFormula trueFormula,falseFormula,nullFormula;
	static {
		try {
			trueFormula=DialogueKBFormula.create("true");
			falseFormula=DialogueKBFormula.create("false");
			nullFormula=DialogueKBFormula.create("null");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * more efficient call to create a formula for a variable name
	 * @param pred
	 * @return
	 * @throws Exception
	 */
	public static DialogueKBFormula createVar(String pred) throws Exception {
		return create(pred, null, Type.PRED);
	}
	public static DialogueKBFormula create(String pred) throws Exception {
		return create(pred, null, null);
	}
	public static DialogueKBFormula create(String pred,Collection<DialogueKBFormula> args) throws Exception {
		return create(pred, args, null);
	}
	/*
	 * input: a predicate and a list fo arguments
	 * output: an object avoid duplications
	 * does: get the object, op, corresponding to pred
	 *       access the list of formulas using op as key
	 *        if empty create a new one  
	 */
	public static DialogueKBFormula create(String pred,Collection<DialogueKBFormula> args,Type type) throws Exception {
		pred=normalizePredName(pred,type);

		LTS predElement=basicElements.get(pred);
		if (predElement==null) {
			if (args==null || args.isEmpty()) predElement=new LTS(new DialogueKBFormula(pred,null,type));
			else predElement=new LTS();
			basicElements.put(pred, predElement);
		}
		if ((args!=null) && (!args.isEmpty())) {
			DialogueKBFormula f=getFormulaForArguments(pred,predElement,args,args.iterator());
			Macro m=MacroRepository.getMacro(f.getName(),f.getArgCount());
			if (m!=null && m instanceof FormulaMacro) {
				f=((FormulaMacro)m).generateSubstituteFormula(f);
			}
			f=f.simplify();
			return f;
		} else {
			Macro m=MacroRepository.getMacro(pred,args!=null?args.size():0);
			if (m!=null && m instanceof FormulaMacro) {
				return ((FormulaMacro)m).generateSubstituteFormula(predElement.completeF);
			} else {
				return predElement.completeF;
			}
		}
	}

	private static String normalizePredName(String pred) {
		return normalizePredName(pred, null);
	}
	private static String normalizePredName(String pred, Type type) {
		if (type==null) type=getTypeOfPredicate(pred);
		Object predValue=pred;
		switch(type) {
		case BOOL:
			predValue=getTypeOfBooleanOperator(pred);
			break;
		case NUMPRED:
			predValue=getTypeOfNumericOperator(pred);
			break;
		case CMP:
			predValue=getTypeOfCMPOperator(pred);
			break;
		case NUMBER:
			String num=NumberUtils.makeLongIfPossible(pred);
			if (num!=null) predValue=Long.parseLong(num);
			else predValue=Float.parseFloat(pred);
			break;
		case PRED:
			//predValue=predValue;//pred.toLowerCase();
			break;
		case CUSTOM:
			predValue=pred.toLowerCase();
			break;
		}
		return predValue.toString();
	}

	private static DialogueKBFormula getFormulaForArguments(String pred, LTS lts,Collection<DialogueKBFormula> args, Iterator<DialogueKBFormula> it) throws Exception {
		if (it.hasNext()) {
			DialogueKBFormula el=it.next();
			HashMap<DialogueKBFormula, LTS> ltsChildren=lts.children;
			if (ltsChildren==null) {
				lts.children=new HashMap<DialogueKBFormula, LTS>();
			}
			LTS nextLTS=lts.children.get(el);
			if (nextLTS==null) {
				if (it.hasNext())
					nextLTS=new LTS(el,null);
				else
					nextLTS=new LTS(el,new DialogueKBFormula(pred,args));
				lts.children.put(el, nextLTS);
			}
			return getFormulaForArguments(pred, nextLTS, args, it);
		} else {
			if (lts.completeF==null) lts.completeF=new DialogueKBFormula(pred,args);
			return lts.completeF;
		}
	}
	public static final Pattern stringPattern=Pattern.compile("^'(.*)'$");
	public static boolean isStringConstant(String in) {return stringPattern.matcher(in).matches();}
	public static String getStringValue(String in) throws Exception {
		Matcher m=stringPattern.matcher(in);
		if (m.matches()) return m.group(1);
		else throw new Exception("invalid input string: "+in+". Doesn't match string pattern: "+stringPattern.toString());
	}
	public static String generateStringConstantFromContent(String in) {return "'"+in+"'";}
	public static Type getTypeOfPredicate(String pred) {
		try {
			BooleanOp.valueOf(pred.toUpperCase());
			return Type.BOOL;
		} catch (IllegalArgumentException e) {}
		try {
			NumOp.valueOf(pred.toUpperCase());
			return Type.NUMPRED;
		} catch (IllegalArgumentException e) {}
		try {
			CmpOp.valueOf(pred.toUpperCase());
			return Type.CMP;
		} catch (IllegalArgumentException e) {}
		if (NumberUtils.isNumber(pred)) return Type.NUMBER;
		if (pred.matches("^[\\*\\+\\-/]$")) return Type.NUMPRED;
		else if (pred.matches("^(==|<=|>=|!=|<|>)$")) return Type.CMP;
		else if (pred.compareToIgnoreCase("true")==0) return Type.TRUE;
		else if (pred.compareToIgnoreCase("false")==0) return Type.FALSE;
		else if (pred.compareToIgnoreCase("null")==0) return Type.NULL;
		else if (isStringConstant(pred)) return Type.STRING;
		else if (getCustomFunction(pred)!=null) return Type.CUSTOM;
		else if (pred.equals("quote")) return Type.QUOTED;
		else return Type.PRED;
	}
	public BooleanOp getTypeOfBooleanOperator() {
		String name=getName();
		return getTypeOfBooleanOperator(name);
	}
	public static BooleanOp getTypeOfBooleanOperator(String pred) {
		return BooleanOp.valueOf(pred.toUpperCase());
	}
	public NumOp getTypeOfNumericOperator() {
		return getTypeOfNumericOperator(getName());
	}
	public static NumOp getTypeOfNumericOperator(String pred) {
		try {
			return NumOp.valueOf(pred.toUpperCase());
		} catch (IllegalArgumentException e) {}
		if (pred.equals("*")) return NumOp.MUL;
		else if (pred.equals("+")) return NumOp.ADD;
		else if (pred.equals("-")) return NumOp.SUB;
		else if (pred.equals("/")) return NumOp.DIV;
		else return null;
	}
	public CmpOp getTypeOfCMPOperator() {
		return getTypeOfCMPOperator(getName());
	}
	public static CmpOp getTypeOfCMPOperator(String pred) {
		try {
			return CmpOp.valueOf(pred.toUpperCase());
		} catch (IllegalArgumentException e) {}
		if (pred.equals("==")) return CmpOp.EQ;
		else if (pred.equals("<=")) return CmpOp.LE;
		else if (pred.equals(">=")) return CmpOp.GE;
		else if (pred.equals("!=")) return CmpOp.NE;
		else if (pred.equals("<")) return CmpOp.LT;
		else if (pred.equals(">")) return CmpOp.GT;
		else return null;
	}
	private DialogueKBFormula(String pred,Collection<DialogueKBFormula> args) throws Exception {
		this(pred,args,null);
	}
	private DialogueKBFormula(String pred,Collection<DialogueKBFormula> args,Type type) throws Exception {
		super(pred);
		if (type==null) type=getTypeOfPredicate(pred);
		setType(type);		
		Object predValue=pred;
		int numArgs=(args!=null)?args.size():0;
		switch (type) {
		case BOOL:
			predValue=getTypeOfBooleanOperator(pred);
			if (predValue==BooleanOp.NOT) {
				if (numArgs!=1) throw new Exception("wrong number of arguments to NOT.");
			} else if (numArgs<1) throw new Exception("wrong number of arguments for AND/OR.");
			break;
		case NUMPRED:
			predValue=getTypeOfNumericOperator(pred);
			if (predValue==NumOp.SUB && (numArgs<1)) throw new Exception("wrong number of arguments to -.");
			else if	(numArgs<2) throw new Exception("wrong number of arguments to +/*.");
			break;
		case CMP:
			predValue=getTypeOfCMPOperator(pred);
			if (numArgs!=2) throw new Exception("Wrong number of arguments for comparison operator.");
			break;
		case QUOTED:
			if (numArgs!=1) throw new Exception("wrong number of arguments to QUOTE.");
		case CUSTOM:
			CustomFunctionInterface sc=getCustomFunction(pred);
			if (sc!=null) {
				Boolean result=(Boolean)sc.checkArguments(args);
				if (result==null || !result) throw new Exception("wrong arguments for custom function: "+pred);
			}
		}
		setValue(predValue);

		if (numArgs>0) {
			for (DialogueKBFormula arg:args) {
				addEdgeTo(arg,true,true);
			}
		}
	}

	public CustomFunctionInterface getCustomFunction() {
		return getCustomFunction(getName());
	}
	public static CustomFunctionInterface getCustomFunction(String pred) {
		return (customFunctions!=null)?customFunctions.get(pred.toLowerCase()):null;
	}
	public static HashMap<String, CustomFunctionInterface> getCustomfunctions() {
		return customFunctions;
	}

	public DialogueKBFormula simplify() throws Exception {
		DialogueKBFormula f=this;
		if (hasChildren()) {
			Object result=simplifyKB.evaluate(this,true,null);
			if (result!=null) {
				String pred=result.toString();
				f=DialogueKBFormula.create(pred, null);
			}
			boolean cont=true;
			while(cont) {
				cont=false;
				Pair<Number,DialogueKBFormula>pair1;
				if ((pair1=f.isConstantIncrement())!=null) {
					Pair<Number,DialogueKBFormula>pair2;
					if ((pair2=pair1.getSecond().isConstantIncrement())!=null) {
						Number n1=pair1.getFirst();
						Number n2=pair2.getFirst();
						Number number=FloatAndLongUtils.sumFloatAndOrLong(n1,n2);
						List<DialogueKBFormula> args=new ArrayList<DialogueKBFormula>();
						args.add(pair2.getSecond());
						args.add(DialogueKBFormula.create(number.toString(), null));
						f=DialogueKBFormula.create(NumOp.ADD.toString(), args);
						cont=true;
					}
				}
			}
		}
		return f;
	}

	public boolean isLogicalFormula() {return (getType()==Type.BOOL) || isCmpFormula() || isTrivialTruth() || isTrivialFalsity() || isNull();}
	public boolean isTrivialTruth() {return getType()==Type.TRUE;} 
	public boolean isTrivialFalsity() {return getType()==Type.FALSE;} 
	public boolean isNull() {return getType()==Type.NULL;} 
	public boolean isCmpFormula() {return (getType()==Type.CMP);}
	public boolean isNumericFormula() {return getType()==Type.NUMPRED;}
	public boolean isConstant() {return !hasChildren();}// && !StringUtils.isEmptyString(getName());}
	public boolean isNumber() {return getType()==Type.NUMBER;}
	public boolean isString() {return getType()==Type.STRING;}
	public boolean isQuoted() {return getType()==Type.QUOTED;}
	public boolean isStar() {return isConstant() && getName().equals(STARARG);}
	public boolean hasStarArgs() {
		if (isPredication()) {
			List<DialogueKBFormula> args = getAllArgs();
			return (args!=null && !args.isEmpty() && args.stream().anyMatch(s->s.isStar()));
		}
		return false;
	}
	public int countStarArgs() {
		if (isPredication()) {
			List<DialogueKBFormula> args = getAllArgs();
			if (args!=null && !args.isEmpty()) {
				int count=args.stream().mapToInt(s->s.isStar()?1:0).sum();
				return count;
			} else return 0;
		}
		return -1;
	}
	public Number getNumber() {
		if (isNumber()) {
			try {
				return Long.parseLong(getName());
			} catch (Exception e) {
				return Float.parseFloat(getName());
			}
		} else return null;
	}
	public boolean isVariable() {
		return isConstant() && !isNumber() && !isString() && !isTrivialFalsity() && !isTrivialTruth() && !isNull() && !isCustomFormula();
	}
	public boolean isPredication() {
		return !isLogicalFormula() && !isNumber() && !isString() && !isTrivialFalsity() && !isTrivialTruth() && !isNull() && !isCustomFormula();
	}
	public boolean isNegatedFormula() {return isLogicalFormula() && (getValue()==BooleanOp.NOT);}
	public boolean isConjunction() {return isLogicalFormula() && getValue()==BooleanOp.AND;}
	public boolean isCustomFormula() {return getType()==Type.CUSTOM;}
	public boolean isDisjunction() {return isLogicalFormula() && getValue()==BooleanOp.OR;}

	@Override
	public String toString() {
		return toString(null);
	}
	public String toString(DialogueKB kb) {
		switch (getType()) {
		case QUOTED:
		case FALSE:
		case STRING:
		case TRUE:
		case NULL:
		case PRED:
		case CUSTOM:
		case NUMBER:
			try {
				String nname=kb!=null?kb.normalizeNames(getName()):getName();
				List<Edge> edgs = getOutgoingEdges();
				String children=(edgs!=null)?edgs.stream().map(s->((DialogueKBFormula)(s.getTarget())).toString(kb)).collect(Collectors.joining(",", "(", ")")):"";
				return nname+children;
				//return nname+FunctionalLibrary.printCollection(getOutgoingEdges(),Edge.class.getMethod("getTarget"),"(", ")", ",");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		case NUMPRED:
			try {
				List<Edge> edgs = getOutgoingEdges();
				String children=(edgs!=null)?edgs.stream().map(s->((DialogueKBFormula)(s.getTarget())).toString(kb)).collect(Collectors.joining(",", "(", ")")):"";
				return getTypeOfNumericOperator()+children;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		case CMP:
			try {
				List<Edge> edgs = getOutgoingEdges();
				String children=(edgs!=null)?edgs.stream().map(s->((DialogueKBFormula)(s.getTarget())).toString(kb)).collect(Collectors.joining(",", "(", ")")):"";
				return getTypeOfCMPOperator()+children;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		case BOOL:
			try {
				List<Edge> edgs = getOutgoingEdges();
				String children=(edgs!=null)?edgs.stream().map(s->((DialogueKBFormula)(s.getTarget())).toString(kb)).collect(Collectors.joining(",", "(", ")")):"";
				return normalizePredName(getName())+children;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		default:
			return null;
		}
	}

	public void setType(Type t) {
		this.type=t;
	}
	public Type getType() {
		return type;
	}

	public static DialogueKBFormula parse(String fs) throws Exception {
		fs=StringUtils.removeLeadingAndTrailingSpaces(fs);
		if (!StringUtils.isEmptyString(fs)) {
			FormulaGrammar parser = new FormulaGrammar(new StringReader(fs));
			return parser.formula();
		} else return null;
	}

	public DialogueKBFormula getArg(int i) {
		List<Edge> children = getOutgoingEdges();
		if ((children!=null) && (children.size()>=i)) {
			Edge n = children.get(i-1);
			return (n!=null)?(DialogueKBFormula)n.getTarget():null;
		} else return null;
	}
	public int getArgCount() {
		List<Edge> children = getOutgoingEdges();
		if (children!=null) return children.size();
		else return 0;
	}
	public List<DialogueKBFormula> getAllArgs() {
		List<Edge> children = getOutgoingEdges();
		List<DialogueKBFormula> ret=null;
		if (children!=null) {
			for(Edge n:children) {
				if (n!=null) {
					if (ret==null) ret=new ArrayList<DialogueKBFormula>();
					ret.add((DialogueKBFormula)n.getTarget());
				}
			}
		}
		return ret;
	}

	public static final LinkedHashMap<TokenTypes,Pattern> formulaTokenTypes=new LinkedHashMap<TokenTypes, Pattern>(){
		private static final long serialVersionUID = 1L;
		{
			put(TokenTypes.OTHER,Pattern.compile("\\(|\\)|\\,|[\\s]+|[\\<\\>\\!\\=]=|[\\<\\>]|\\=|\\~|\\!|\\+\\+"));
			put(TokenTypes.WORD, Pattern.compile("[^\\,\\(\\)\\s\\=\\<\\>\\!\\~]+"));
			put(TokenTypes.NUM, Pattern.compile("([0-9]*\\.[\\d]+)|([\\d]+)"));
		}
	};

	public DialogueKBFormula negatedNormalForm() throws Exception {
		if (isNegatedFormula()) {
			DialogueKBFormula fc = (DialogueKBFormula) getFirstChild();
			if (fc.isNegatedFormula()) return (DialogueKBFormula) fc.getFirstChild();
			else if (fc.isConjunction()) {
				return disjunctionOf(fc.getArg(1).negate().negatedNormalForm(),fc.getArg(2).negate().negatedNormalForm());
			} else if (fc.isDisjunction()) {
				return conjunctionOf(fc.getArg(1).negate().negatedNormalForm(),fc.getArg(2).negate().negatedNormalForm());
			} else return this;
		} else return this;
	}

	private static DialogueKBFormula conjunctionOf(DialogueKBFormula... fs) throws Exception {
		return create(BooleanOp.AND.toString(), Arrays.asList(fs));
	}

	private static DialogueKBFormula disjunctionOf(DialogueKBFormula... fs) throws Exception {
		return create(BooleanOp.OR.toString(), Arrays.asList(fs));
	}

	public DialogueKBFormula negate() throws Exception {
		ArrayList<DialogueKBFormula> args = new ArrayList<DialogueKBFormula>();
		args.add(this);
		return create(BooleanOp.NOT.toString(),args);
	}
	public DialogueKBFormula normalize() throws Exception {
		DialogueKBFormula f=this.negatedNormalForm();
		// normalize variables and skolemization not necessary, for the moment propositional only, no variables.
		return f;
	}

	public Set<DialogueKBFormula> extractAllNamesUsed() {
		HashSet<DialogueKBFormula> ret=null;
		Stack<DialogueKBFormula> s=new Stack<DialogueKBFormula>();
		s.push(this);
		while (!s.isEmpty()) {
			DialogueKBFormula f=s.pop();
			if (f.isConstant()) {
				if (f.isVariable()) {
					if (ret==null) ret=new HashSet<DialogueKBFormula>();
					ret.add(f);
				}
			} else {
				List<Edge> edges = f.getOutgoingEdges();
				if (edges!=null && !edges.isEmpty()) for(Edge e:edges) s.push((DialogueKBFormula) e.getTarget());
			}
		}
		return ret;
	}

	public Pair<Number,DialogueKBFormula> isConstantIncrement() {
		if (isNumericFormula()) {
			NumOp op=getTypeOfNumericOperator();
			Number number;
			if (op==NumOp.ADD) {
				if ((number=getArg(2).getNumber())!=null) {
					return new Pair<Number, DialogueKBFormula>(number, getArg(1));
				} else if ((number=getArg(1).getNumber())!=null) {
					return new Pair<Number, DialogueKBFormula>(number, getArg(2));
				}
			} else if (op==NumOp.SUB) {
				if ((number=getArg(2).getNumber())!=null) {
					if (number instanceof Long) {
						return new Pair<Number, DialogueKBFormula>(-(Long)number, getArg(1));						
					} else if (number instanceof Float) {
						return new Pair<Number, DialogueKBFormula>(-(Float)number, getArg(1));
					}
					
				}
			}
		}
		return null;
	}

	public Number isIncrementForVariable(DialogueKBFormula pl) {
		if (isNumericFormula()) {
			NumOp op=getTypeOfNumericOperator();
			if (op==NumOp.ADD) {
				if (getArg(1)==pl) {
					return getArg(2).getNumber();
				} else if (getArg(2)==pl) {
					return getArg(1).getNumber();
				}
			} else if (op==NumOp.SUB) {
				if (getArg(1)==pl) {
					Number dec=getArg(2).getNumber();
					if (dec!=null) {
						if (dec instanceof Long) return -(Long)dec;
						else if (dec instanceof Float) return -(Float)dec;
					}
				}
			}
		}
		return null;
	}

	public DialogueKBFormula substitute(HashMap<DialogueKBFormula, DialogueKBFormula> replacements) throws Exception {
		if (replacements==null) return this;
		else {
			DialogueKBFormula out=replacements.get(this);
			if (out!=null) return out;
			else {
				if (hasChildren()) {
					ArrayList<DialogueKBFormula> arguments=new ArrayList<DialogueKBFormula>();
					int n=getOutgoingEdges().size();
					for(int i=1;i<=n;i++) {
						DialogueKBFormula arg=getArg(i);
						DialogueKBFormula argOut=replacements.get(arg);
						if (argOut==null) argOut=arg.substitute(replacements);
						arguments.add(argOut);
					}
					return DialogueKBFormula.create(getName(), arguments);
				} else return this;
			}
		}
	}

	public Set<String> doesFormulaUseAllKnownVariables(DialogueKBInterface is, Map<String, DialogueKBFormula> localVars) {
		Set<String> ret=null;
		Set<DialogueKBFormula> variables = extractAllNamesUsed();
		if (variables!=null) {
			for(DialogueKBFormula var:variables) {
				String vName=var.getName();
				if (!is.hasVariableNamed(vName,ACCESSTYPE.AUTO_OVERWRITEAUTO) && ((localVars==null) || !localVars.containsKey(vName))) {
					if (ret==null) ret=new HashSet<String>();
					ret.add(vName);
				}
			}
		}
		return ret;
	}

	@Override
	public boolean equals(Object obj) {
		if (isNumber()) return getNumber().equals(obj);
		else if (isString()) return getName().equals(obj);
		return super.equals(obj);
	}

	public static void main(String[] args) throws Exception {
		//MacroRepository.loadFromXML(new File("C:\\Users\\morbini\\jmNL\\resources\\story\\Story\\dm\\macros.xml"));
		TrivialDialogueKB mykb22 = new TrivialDialogueKB();
		//mykb22.store(DialogueOperatorEffect.createAssignment("systemEvent", "'question.1'"), ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		mykb22.store(DialogueOperatorEffect.createAssignment("currentQuestion", "'question.1'"), ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		mykb22.store(DialogueOperatorEffect.createAssertion(DialogueKBFormula.parse("answered('self',currentQuestion)")), ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		DialogueKBFormula x=parse("answered('self',currentQuestion)");
		System.out.println(x);
		Object r84 = mykb22.evaluate(x, null);
		System.out.println(r84);
		MacroRepository.block();
		x=parse("and(test(ddd),test(a),test(b))");
		System.out.println(x);
		System.exit(0);
		DialogueKBFormula rrr = parse("currentTime()");
		System.out.println(rrr);
		DialogueKBFormula f40 = parse("isQuestion(a)");
		System.out.println(f40);
		System.out.println(f40.getAllArgs());
		//DialogueKBFormula f9=parse("or(queryNLU('cGDA','Conventional-opening',1),queryNLU('cGDA','Acknowledge-backchannel-agree-accept',1),and(queryNLU('cGDA','Statement',1),!queryNLU('cV','negative',1)),queryNLU('cV','positive',1)");
		//DialogueKBFormula f9=parse("and(timeSinceLastAction>=qwait,ipuNumber()>0)");
		DialogueKBFormula f9=parse("print('3')");
		System.out.println(f9);
		DialogueKBFormula f19=parse("print('3')");
		DialogueKBFormula f20=new DialogueKBFormula(f19.getName(), f19.getAllArgs());
		System.out.println(f19);
		DialogueKBFormula f8=parse("+(a,random(3))");
		System.out.println(f8);
		TrivialDialogueKB mykb = new TrivialDialogueKB();
		mykb.evaluate(f9,null);
		mykb.set("a", 0);
		for (int i=0;i<1000;i++) {
			Object r=mykb.evaluate(f8,null);
			System.out.println(r);
		}
		DialogueKBFormula f7=parse("quote(+(a,1,2))");
		DialogueKBFormula f6=parse("       ");
		System.out.println(f7);
		System.exit(0);
		Collection<DialogueKBFormula> argss2=new ArrayList<DialogueKBFormula>();
		argss2.add(create("10",null));
		argss2.add(create("5",null));
		DialogueKBFormula.create("<", argss2);
		System.out.println(getTypeOfPredicate("<="));
		DialogueKBFormula f2 = parse("/(10,greeting-counter)");
		f2 = parse("10>greeting-counter");
		System.out.println(f2);
		f2 = parse("isQuestion(a)");
		System.out.println(f2);
		System.out.println(f2.getAllArgs());
		System.out.println(f2.isNumericFormula());
		DialogueKBFormula f4=create("a", null);
		Collection<DialogueKBFormula> argss=new ArrayList<DialogueKBFormula>();
		argss.add(create("10",null));
		DialogueKBFormula f5=create("a",argss);
		DialogueKBFormula f3 = parse("+(10,1)");
		f3 = parse("10+1");
		System.out.println(f3);
		System.out.println(f3.isNumericFormula());
		f2=parse("not(or(a,b))");
		System.out.println(f2);
		f2=parse("isLastNonNullTopic('bbq')");
		System.out.println(f2);
		//f2=parse(f2.toString());
		//System.out.println(f2);
	}
}
