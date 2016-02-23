package edu.usc.ict.nl.kb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKBFormula.CmpOp;
import edu.usc.ict.nl.kb.DialogueKBFormula.NumOp;
import edu.usc.ict.nl.kb.VariableProperties.PROPERTY;
import edu.usc.ict.nl.kb.cf.CustomFunctionInterface;
import edu.usc.ict.nl.kb.internal.Arg;
import edu.usc.ict.nl.kb.internal.PropositionalKB;
import edu.usc.ict.nl.util.graph.Edge;
import edu.usc.ict.nl.utils.FloatAndLongUtils;

public class TrivialDialogueKB extends DialogueKB {
	
	private PropositionalKB kb;
	private DialogueKB inheritedKB;
	private Collection<DialogueKB> inheritedByKBs;
	private LinkedHashSet<DialogueOperatorEffect> rulesKB;
	private HashMap<DialogueKBFormula,Object> cache;//=new HashMap<DialogueKBFormula, Object>();
	
	@Override
	public String getName() {
		String id=getID()+"";
		if (getParent()!=null) return id+"."+getParent().getName();
		else return id;
	}
	
	public TrivialDialogueKB() {
		this((DM)null);
	}
	public TrivialDialogueKB(DM dm) {
		super(dm);
		name=toString();
		kb=new PropositionalKB(this);
		rulesKB=new LinkedHashSet<DialogueOperatorEffect>();
		setParent(null);
	}
	public TrivialDialogueKB(DialogueKB parent) {
		this(parent.getDM());
		setParent(parent);
	}

	@Override
	public DialogueKB storeAll(Collection<DialogueOperatorEffect> effects,ACCESSTYPE type,boolean doForwardInference) throws Exception {
		if (type!=ACCESSTYPE.AUTO_NEW && type!=ACCESSTYPE.AUTO_OVERWRITEAUTO && type!=ACCESSTYPE.AUTO_OVERWRITETHIS) throw new Exception("Invalid access type: "+type);
		TrivialDialogueKB retKB=this;
		boolean didUpdate=false;
		if (effects!=null) {
			for (DialogueOperatorEffect e:effects) {
				if (didUpdate) type=ACCESSTYPE.AUTO_OVERWRITETHIS;
				// force overwrite if a new KB was already created by a previous update
				// (so only one new KB is created for this entire set of updates)
				TrivialDialogueKB updatedKB=(TrivialDialogueKB) retKB.store(e,type,false);
				if (updatedKB!=null) {
					retKB=updatedKB;
					didUpdate=true;
				}
			}
			if (didUpdate && doForwardInference) {
				// run the forward inference rules accessible by this KB
				if (type==ACCESSTYPE.AUTO_OVERWRITETHIS || type==ACCESSTYPE.AUTO_NEW) {
					retKB.runForwardInference(ACCESSTYPE.AUTO_OVERWRITETHIS);
				} else {
					retKB.runForwardInference(ACCESSTYPE.AUTO_OVERWRITEAUTO);
				}
			}
		}
		return (didUpdate)?retKB:null;
	}
	@Override
	public DialogueKB store(DialogueOperatorEffect e,ACCESSTYPE type,boolean doForwardInference) throws Exception {
		if (e==null) return this;

		if (type!=ACCESSTYPE.AUTO_NEW && type!=ACCESSTYPE.AUTO_OVERWRITEAUTO && type!=ACCESSTYPE.AUTO_OVERWRITETHIS) throw new Exception("Invalid access type: "+type);
		boolean overwrite=DialogueKB.isOverwriteMode(type);
		
		if (e.isAssignmentList()) return storeAll(e.getAssignmentList(), type, doForwardInference);
		
		TrivialDialogueKB retKB=this;
		boolean didUpdate=false;
		if (isSupportedFormulaToBeStored(e)) {
			Boolean evalResult=evaluate(e);
			if ((evalResult==null) || !evalResult) {
				didUpdate=true;
				if (!overwrite) {
					retKB=new TrivialDialogueKB(this);
				}
				if (e.isAssertion()) {
					DialogueKBFormula f=e.getAssertedFormula();
					String name=(f.getArgCount()>0)?f.toString():f.getName();
					if (type==ACCESSTYPE.AUTO_OVERWRITETHIS || type==ACCESSTYPE.AUTO_NEW) {
						if (f.getArgCount()>0) {
							retKB.setValueOfPredication(f, e.getAssertionSign(), ACCESSTYPE.AUTO_OVERWRITETHIS);
						} else {
							retKB.setValueOfVariable(name, e.getAssertionSign(), ACCESSTYPE.AUTO_OVERWRITETHIS);
						}
					} else {
						if (f.getArgCount()>0) {
							retKB.setValueOfPredication(f, e.getAssertionSign(), ACCESSTYPE.AUTO_OVERWRITEAUTO);
						} else {
							retKB.setValueOfVariable(name, e.getAssertionSign(), ACCESSTYPE.AUTO_OVERWRITEAUTO);
						}
					}
				} else if (e.isAssignment()) {
					DialogueKBFormula var=e.getAssignedVariable();
					Object varValue=e.getAssignedExpression();
					if (varValue instanceof DialogueKBFormula) {
						varValue=evaluate((DialogueKBFormula) varValue,null);
						if (varValue instanceof String && !DialogueKBFormula.isStringConstant((String) varValue)) {
							varValue=DialogueKBFormula.generateStringConstantFromContent((String) varValue);
						}
					}
					if (type==ACCESSTYPE.AUTO_OVERWRITETHIS || type==ACCESSTYPE.AUTO_NEW) {
						retKB.setValueOfVariable(var.getName(), varValue, ACCESSTYPE.AUTO_OVERWRITETHIS);
					} else {
						retKB.setValueOfVariable(var.getName(), varValue, ACCESSTYPE.AUTO_OVERWRITEAUTO);
					}
				} else if (e.isImplication()) {
					retKB.rulesKB.add(e);
				} else if (e.isSwapOut()) {
					// nothing to do
				} else if (e.isInterrupt()) {
					// nothing to do
				} else if (e.isSend()) {
					// nothing to do
				} else throw new Exception("type of formula unknown: "+e);
			}
		} else throw new Exception("Formula not supported for storage: "+e);
		if (didUpdate && doForwardInference) {
			// run the forward inference rules accessible by this KB
			if (type==ACCESSTYPE.AUTO_OVERWRITETHIS || type==ACCESSTYPE.AUTO_NEW) {
				retKB.runForwardInference(ACCESSTYPE.AUTO_OVERWRITETHIS);
			} else {
				retKB.runForwardInference(ACCESSTYPE.AUTO_OVERWRITEAUTO);
			}
					
		}
		//if (didUpdate && overwrite) invalidateCache();
		return (didUpdate)?retKB:null;
	}
	
	@Override
	public void invalidateCache() {
		//cache.clear();
		//Collection<DialogueKBInterface> children=getChildren();
		//if (children!=null) for(DialogueKBInterface c:children) c.invalidateCache();
	}
	
	@Override
	public boolean isSupportedFormulaToBeStored(DialogueOperatorEffect e) {
		if (e.isAssignment()) {
			return e.getAssignedVariable().getArgCount()==0 && !e.getAssignedVariable().hasStarArgs();
		} else if (e.isAssertion()) {
			return !e.getAssertedFormula().hasStarArgs();
		}
		return !e.isGoalAchievement();
	}
	@Override public DialogueKB getParent() {return inheritedKB;}
	@Override public void setParent(DialogueKB parent) {
		inheritedKB=parent;
		if (parent!=null) {
			parent.addChild(this);
			if (parent instanceof DialogueKB) {
				tracedConstants=((DialogueKB) parent).tracedConstants;
			}
		}
	}
	@Override
	public Collection<DialogueKB> getChildren() {return inheritedByKBs;}
	@Override
	public void addChild(DialogueKB c) {
		//if (inheritedByKBs==null) inheritedByKBs=new ArrayList<DialogueKBInterface>();
		//inheritedByKBs.add(c);
	}

	@Override
	public Boolean isTrueInKB(DialogueKBFormula f,EvalContext context) throws Exception {
		DialogueKBInterface parent;
		if (kb.containsKey(f)) {
			Object value=kb.get(f);
			if (context!=null) context.updateLowestKBUsed(this);
			if (value!=null) {
				if (value instanceof Boolean) return (Boolean) value;
				else if (value instanceof DialogueKBFormula) {
					return evaluateLogicalFormula((DialogueKBFormula) value,context);
				} else return null;
			} else return null;
		}
		else if ((parent=getParent())!=null) return parent.isTrueInKB(f,context);
		else return null;
	}

	private Number evaluateNumericTerm(DialogueKBFormula fv, boolean forSimplification,EvalContext context) throws Exception {
		Number num;
		DialogueKBFormula f=(DialogueKBFormula)fv;
		if (f.isNumericFormula()) {
			NumOp p=f.getTypeOfNumericOperator();
			List<Edge> args = f.getOutgoingEdges();
			Iterator<Edge> argsi = args.iterator();
			Number fa=evaluateNumericTerm((DialogueKBFormula) argsi.next().getTarget(),forSimplification,context);
			if (fa!=null) {
				if ((p==NumOp.SUB) && (args.size()==1)) return FloatAndLongUtils.subFloatAndOrLong(0l, fa);
				while(argsi.hasNext()) {
					Number oa=evaluateNumericTerm((DialogueKBFormula) argsi.next().getTarget(),forSimplification,context);
					if (oa==null) return null;
					else {
						try {
							switch(p) {
							case MUL:
								fa=FloatAndLongUtils.mulFloatAndOrLong(fa, oa);break;
							case DIV:
								fa=FloatAndLongUtils.divFloatAndOrLong(fa, oa);break;
							case ADD:
								fa=FloatAndLongUtils.sumFloatAndOrLong(fa, oa);break;
							case SUB:
								fa=FloatAndLongUtils.subFloatAndOrLong(fa, oa);break;
							}
						} catch (Exception e) {return null;}
					}
				}
				return fa;
			} else return null;
		} else if ((num=f.getNumber())!=null) {
			return num;
		} else if (f.isVariable()) {
			Object value=getValueOfVariable(f.getName(),ACCESSTYPE.AUTO_OVERWRITEAUTO,context);
			if (value instanceof DialogueKBFormula) return evaluateNumericTerm((DialogueKBFormula)value,forSimplification,context);
			if (value instanceof Number) return (Float)value;
			else return null;
		} else if (f.isCustomFormula()) {
			Object r=evaluate(f,forSimplification,context);
			if (r!=null && r instanceof Number) return (Number) r;
			else return null;
		} else return null;
	}
	@Override
	public Object evaluate(Object f,EvalContext context) throws Exception {
		if (f instanceof DialogueKBFormula) return evaluate((DialogueKBFormula) f,context);
		else if (f instanceof DialogueOperatorEffect) return evaluate((DialogueOperatorEffect) f,context);
		else return f;
	}
	@Override
	public Object evaluate(DialogueKBFormula f,EvalContext context) throws Exception {
		//if (cache.containsKey(f)) return cache.get(f);
		//else {
			Object r=evaluate(f, false,context);
			//cache.put(f,r);
			return r;
		//}
	}
	public Object evaluate(DialogueKBFormula f,boolean forSimplification,EvalContext context) throws Exception {
		if (f==null) return null;
		else if (f.isNumber()) return f.getNumber();
		else if (f.isString()) {
			//Matcher m=DialogueKBFormula.stringPattern.matcher(f.getName());
			//if (m.matches() && m.groupCount()==1) return m.group(1);
			//else return f.getName();
			return f.getName();
		}
		else if (f.isVariable()) {
			Object v=getValueOfPredication(f,ACCESSTYPE.AUTO_OVERWRITEAUTO,context);
			if (v instanceof DialogueKBFormula) return evaluate((DialogueKBFormula) v,context);
			else return v;
		}
		else if (f.isQuoted()) return (forSimplification)?null:f.getArg(1);
		else if (f.isCustomFormula()) return evaluateCustomFormula(f, forSimplification,context);
		else if (f.isNumericFormula()) return evaluateNumericTerm(f,forSimplification,context);
		else if (f.hasStarArgs()) {
			DialogueKBFormula nf=evalArgs(f, context);
			return getSatisfyingArguments(nf, ACCESSTYPE.AUTO_OVERWRITEAUTO, context);
		}
		else return evaluateLogicalFormula(f,forSimplification,context);
	}
	private DialogueKBFormula evalArgs(DialogueKBFormula f, EvalContext context) throws Exception {
		DialogueKBFormula nf=f;
		if(f.countStarArgs()<f.getArgCount()) {
			List<DialogueKBFormula> nargs=new ArrayList<>();
			boolean newArg=false;
			for(DialogueKBFormula a:f.getAllArgs()) {
				if (a.isStar()) nargs.add(a);
				else {
					Object v=evaluate(a, context);
					DialogueKBFormula na=a;
					if (v!=null && !v.toString().equals(a.toString())) {
						na=DialogueKBFormula.create(v.toString());
						newArg=true;
					}
					nargs.add(na);
				}
			}
			if (newArg) nf=DialogueKBFormula.create(f.getName(), nargs);
		}
		return nf;
	}

	private Boolean evaluateLogicalFormula(DialogueKBFormula f,EvalContext context) throws Exception {
		return evaluateLogicalFormula(f, false,context);
	}
	private Boolean evaluateLogicalFormula(DialogueKBFormula f,boolean forSimplification,EvalContext context) throws Exception {
		if (f.isLogicalFormula()) {
			Boolean max=true;
			if (f.isConjunction()) {
				Iterator<Edge> ands = f.getOutgoingEdges().iterator();
				while (ands.hasNext()) {
					DialogueKBFormula and=((DialogueKBFormula)ands.next().getTarget());
					Boolean result=evaluateLogicalFormula(and,forSimplification,context);
					if (result==null) max=null;
					else if (result==false) return false;
				}
				return max;
			} else if (f.isDisjunction()) {
				Iterator<Edge> ors = f.getOutgoingEdges().iterator();
				max=false;
				while (ors.hasNext()) {
					DialogueKBFormula or=((DialogueKBFormula)ors.next().getTarget());
					Boolean result=evaluateLogicalFormula(or,forSimplification,context);
					if (result==null) max=null;
					else if (result) return true;
				}
				return max;
			} else if (f.isNegatedFormula()) {
				max=evaluateLogicalFormula((DialogueKBFormula)f.getFirstChild(),forSimplification,context);
				if (max==null) return null;
				else return !max;
			} else if (f.isCmpFormula()) {
				if (forSimplification) return null;
				DialogueKBFormula a1=f.getArg(1);
				DialogueKBFormula a2=f.getArg(2);
				Object v1=evaluate(a1,context);
				Object v2=evaluate(a2,context);
				return doComparison(v1, v2, (CmpOp)f.getValue());
			} else if (f.isTrivialFalsity()) return false;
			else if (f.isTrivialTruth()) return true;
			else if (f.isNull()) return null;
			else throw new Exception("Invalid boolean formula: "+f);
		} else if (f.isCustomFormula()) {
			Boolean result=(Boolean) evaluateCustomFormula(f, forSimplification,context);
			return result;
		} else if (f.isPredication() && !f.hasStarArgs()) {
			DialogueKBFormula nf=evalArgs(f, context);
			return isTrueInKB(nf,context);
		} else {
			throw new Exception("invalid logical formula type: "+f);
		}
	}
	private Object evaluateCustomFormula(DialogueKBFormula f,boolean forSimplification,EvalContext context) throws Exception {
		if (f.isCustomFormula()) {
			CustomFunctionInterface cf=f.getCustomFunction();
			if (cf!=null) {
				Object result=cf.eval(f,this,forSimplification,context);
				return result;
			} else return null;
		} else throw new Exception("Called cutom formula evaluation on the non-custom formula: "+f);
	}
	
	public Boolean doComparison(Object v1,Object v2,CmpOp op) {
		if (v1!=null && v2!=null && (v1 instanceof Number || v1 instanceof Number)) {
			return doNumericComparison((Number) v1, (Number) v2, op);
		} else if (v1!=null && v2!=null && (v1 instanceof String || v1 instanceof String)) {
			return doStringComparison((String)v1, (String)v2, op);
		} else {
			return doObjectComparison(v1, v2, op);
		}
	}
	public Boolean doNumericComparison(Number v1,Number v2,CmpOp op) {
		Float result=FloatAndLongUtils.numberToFloat(FloatAndLongUtils.subFloatAndOrLong(v1, v2));
		switch (op) {
		case EQ: return result==0;
		case NE: return result!=0;
		case LE: return result<=0;
		case GE: return result>=0;
		case LT: return result<0;
		case GT: return result>0;
		}
		return null;
	}
	public Boolean doStringComparison(String v1,String v2,CmpOp op) {
		switch (op) {
		case EQ: return v1.equals(v2);
		case NE: return !v1.equals(v2);
		case LE: return v1.compareTo(v2)<=0;
		case GE: return v1.compareTo(v2)>=0;
		case LT: return v1.compareTo(v2)<0;
		case GT: return v1.compareTo(v2)>0;
		}
		return null;
	}
	public Boolean doObjectComparison(Object v1,Object v2,CmpOp op) {
		boolean eq=v1==v2 || (v1!=null && v1.equals(v2));
		switch (op) {
		case EQ: return eq;
		case NE: return !eq;
		}
		return null;
	}

	@Override
	public Boolean evaluate(DialogueOperatorEffect e) throws Exception {
		if (e.isAssertion()) {
			Boolean value=evaluateLogicalFormula(e.getAssertedFormula(),null);
			if (value!=null) {
				return value.equals(e.getAssertionSign());
			} else return null;
		} else if (e.isAssignment()) {
			Object value=e.getAssignedExpression();
			if (value instanceof DialogueKBFormula) {
				value=evaluate((DialogueKBFormula) value,null);
			}
			return hasPredicationThisValue(e.getAssignedVariable(), value,ACCESSTYPE.AUTO_OVERWRITEAUTO);
		} else if (e.isImplication()) {
			return doesItContainThisRule(e,ACCESSTYPE.AUTO_OVERWRITEAUTO);
		} else if (e.isAssignmentList()) {
			for(DialogueOperatorEffect x:e.getAssignmentList()) {
				Boolean res=evaluate(x);
				if (res==null) return null;
				else if (!res) return false;
			}
			return true;
		} else return null;
	}
	@Override
	public Boolean doesItContainThisRule(DialogueOperatorEffect e,ACCESSTYPE type) {
		boolean stop=true;
		switch (type) {
		case AUTO_NEW:
		case AUTO_OVERWRITEAUTO:
		case AUTO_OVERWRITETHIS:
			stop=false;
		case THIS_NEW:
		case THIS_OVERWRITETHIS:
			if (rulesKB!=null) {
				if (rulesKB.contains(e)) return true;
				else {
					if (stop) return false;
					DialogueKBInterface parent=getParent();
					if (parent!=null) return parent.doesItContainThisRule(e,type);
					else return false;
				}
			} else return false;
		}
		return false;
	}

	@Override
	public DialogueKB setValueOfVariable(String vName, Object value, ACCESSTYPE type) throws Exception {
		vName=normalizeNames(vName);
		return setValueOfPredication(DialogueKBFormula.createVar(vName), value, type);
	}
	@Override
	public DialogueKB setValueOfPredication(DialogueKBFormula f, Object value,ACCESSTYPE type) throws Exception {
		DialogueKB retKB=null;
		DialogueKB thisVarKB=null;
		Boolean v=null;
		switch (type) {
		case AUTO_NEW:
			v=hasPredicationThisValue(f, value, ACCESSTYPE.AUTO_OVERWRITEAUTO);
			retKB=this;
			if (v==null || !v) {
				retKB=new TrivialDialogueKB(this);
				retKB.setValueOfPredication(f, value, ACCESSTYPE.THIS_OVERWRITETHIS);
			}
			return retKB;
		case AUTO_OVERWRITEAUTO:
		case AUTO_OVERWRITETHIS:
			retKB=this;
			if (type==ACCESSTYPE.AUTO_OVERWRITEAUTO) {
				thisVarKB=findFirstKBInHierarchyThatContainsThisPredication(f);
				if (thisVarKB!=null) retKB=thisVarKB;
			}
			retKB.setValueOfPredication(f, value, ACCESSTYPE.THIS_OVERWRITETHIS);
			return this;
			// do the storing in current KB (THIS mode)
		case THIS_OVERWRITETHIS:
			if (kb!=null) {
				if (f.isVariable()) {
					String vName=f.getName();
					if (getPropertyForVar(vName, PROPERTY.READONLY)) throw new Exception("trying to set a readonly variable: "+vName);
				}
				if (value instanceof DialogueKBFormula) {
					kb.put(f, value);
				} else if ((value instanceof Number) || (value instanceof String)) {
					try {
						DialogueKBFormula valuef=DialogueKBFormula.parse(value.toString());
						kb.put(f, valuef);
					} catch (Exception e) {
						kb.put(f, value);
					}
				} else {
					kb.put(f, value);
				}
			}
			return this;
		case THIS_NEW:
			v=hasPredicationThisValue(f, value, ACCESSTYPE.THIS_OVERWRITETHIS);
			if (v==null || !v) {
				retKB=new TrivialDialogueKB(this);
				return retKB.setValueOfPredication(f, value, ACCESSTYPE.THIS_OVERWRITETHIS);
			}
			return this;
		default:
			throw new Exception("unsupported access type: "+type);
		}
	}
	@Override
	public Object getValueOfVariable(String vName, ACCESSTYPE type, EvalContext context) {
		vName=normalizeNames(vName);
		try {
			return getValueOfPredication(DialogueKBFormula.createVar(vName), type, context);
		} catch (Exception e) {
			logger.error(e);
		}
		return null;
	}
	@Override
	/**
	 * finds the value of the first KB in the inheritance structure (including the initial KB) that contains the given variable
	 */
	public Object getValueOfPredication(DialogueKBFormula f,ACCESSTYPE type,EvalContext context) {
		switch (type) {
		case AUTO_NEW:
		case AUTO_OVERWRITEAUTO:
		case AUTO_OVERWRITETHIS:
			DialogueKB thisVarKB=findFirstKBInHierarchyThatContainsThisPredication(f);
			if (context!=null) context.updateLowestKBUsed(thisVarKB);
			if (thisVarKB!=null) return thisVarKB.getValueOfPredication(f,ACCESSTYPE.THIS_OVERWRITETHIS,null);
			break;
		case THIS_NEW:
		case THIS_OVERWRITETHIS:
			if (kb!=null && kb.containsKey(f)) return kb.get(f);
			break;
		}
		return null;
	}
	
	@Override
	public List getSatisfyingArguments(DialogueKBFormula f, ACCESSTYPE type, EvalContext context) {
		assert(f.hasStarArgs());
		switch (type) {
		case AUTO_NEW:
		case AUTO_OVERWRITEAUTO:
		case AUTO_OVERWRITETHIS:
			DialogueKB ckb=this;
			List ret=null;
			do {
				List list = ckb.getSatisfyingArguments(f, ACCESSTYPE.THIS_OVERWRITETHIS, context);
				if (list!=null) {
					if (ret==null) ret=list;
					else ret.addAll(list);
				}
			} while ((ckb=ckb.getParent())!=null);
			return ret;
		case THIS_NEW:
		case THIS_OVERWRITETHIS:
			if (kb!=null) {
				List result = (List)kb.get(f);
				if (isListOfArgs(result)) {
					result=simplifyListOfArgs(result);
				}
				return result;
			}
			break;
		}
		return null;
	}

	private List simplifyListOfArgs(List result) {
		List ret=null;
		for(Object x:result) {
			if (x!=null && x instanceof List && ((List)x).size()==1) {
				Arg a=(Arg) ((List)x).get(0);
				if (ret==null) ret=new ArrayList<>();
				ret.add(a.getValue());
			}
		}
		return ret!=null?ret:result;
	}

	private boolean isListOfArgs(List result) {
		if (result!=null && !result.isEmpty()) {
			for(Object x:result) {
				if (x!=null && x instanceof List && !((List)x).isEmpty()) {
					for(Object thing:((List)x)) {
						if (thing==null || !(thing instanceof Arg)) return false;
					}
				} else return false;
			}
			return true;
		} else return false;
	}

	@Override
	public void removeVariable(String vName,ACCESSTYPE type) throws Exception {
		vName=normalizeNames(vName);
		switch (type) {
		case AUTO_NEW:
		case AUTO_OVERWRITEAUTO:
		case AUTO_OVERWRITETHIS:
			DialogueKBInterface thisVarKB=findFirstKBInHierarchyThatContainsThisVariableName(vName);
			if (thisVarKB!=null) thisVarKB.removeVariable(vName,ACCESSTYPE.THIS_OVERWRITETHIS);
			break;
		case THIS_NEW:
		case THIS_OVERWRITETHIS:
			if (kb!=null && kb.containsKey(vName)) kb.remove(vName);
			break;
		default:
			throw new Exception("unsupported access type: "+type);
		}
	}
	@Override
	public DialogueKB findFirstKBInHierarchyThatContainsThisVariableName(String vName) {
		if (kb!=null) {
			vName=normalizeNames(vName);
			if (kb.containsKey(vName)) return this;
			else {
				DialogueKBInterface parent=null;
				if ((parent=getParent())!=null) return parent.findFirstKBInHierarchyThatContainsThisVariableName(vName);
				else return null;
			}
		} else return null;
	}
	@Override
	public DialogueKB findFirstKBInHierarchyThatContainsThisPredication(DialogueKBFormula f) {
		if (kb!=null) {
			if (kb.containsKey(f)) return this;
			else {
				DialogueKBInterface parent=null;
				if ((parent=getParent())!=null) return parent.findFirstKBInHierarchyThatContainsThisPredication(f);
				else return null;
			}
		} else return null;
	}
	@Override
	public boolean hasVariableNamed(String vName, ACCESSTYPE type) {
		try {
			vName=normalizeNames(vName);
			return hasPredication(DialogueKBFormula.createVar(vName), type);
		} catch (Exception e) {
			logger.error(e);
		}
		return false;
	}
	@Override
	public boolean hasPredication(DialogueKBFormula f,ACCESSTYPE type) {
		switch (type) {
		case AUTO_NEW:
		case AUTO_OVERWRITETHIS:
		case AUTO_OVERWRITEAUTO:
			DialogueKB thisVarKB=findFirstKBInHierarchyThatContainsThisPredication(f);
			return thisVarKB!=null;
		case THIS_NEW:
		case THIS_OVERWRITETHIS:
			return (kb!=null && kb.containsKey(f));
		}
		return false;
	}
	private Boolean hasPredicationThisValue(DialogueKBFormula f,Object value,ACCESSTYPE type) {
		//vName=normalizeNames(vName);
		DialogueKB kb=this;
		switch (type) {
		case AUTO_NEW:
		case AUTO_OVERWRITEAUTO:
		case AUTO_OVERWRITETHIS:
			kb=findFirstKBInHierarchyThatContainsThisPredication(f);
			if (kb==null) break;
		case THIS_NEW:
		case THIS_OVERWRITETHIS:
			if (!kb.hasPredication(f,ACCESSTYPE.THIS_OVERWRITETHIS)) return null;
			Object oldValue=kb.getValueOfPredication(f,ACCESSTYPE.THIS_OVERWRITETHIS,null);
			if (oldValue==null) return value==oldValue;
			else return oldValue.equals(value);
		}
		return null;
	}
	
	@Override
	public String getContentID() throws Exception {
		TreeMap<String, String> content=dumpAsSingleKB();
		if (content==null) return null;
		else return content.toString();
	}
	public TreeMap<String, String> dumpAsSingleKB() throws Exception {
		TreeMap<String, String> ret=null;
		DialogueKBInterface currentKB=this;
		while (currentKB!=null) {
			Collection<DialogueOperatorEffect> content=currentKB.dumpKB();
			if (content!=null && !content.isEmpty()) {
				if (ret==null) ret=new TreeMap<String, String>();
				for(DialogueOperatorEffect e:content) {
					String var=e.getLeftID();
					String value=e.getRightID();
					if (!ret.containsKey(var)) ret.put(var,value);
				}
			}
			currentKB=currentKB.getParent();
		}
		return ret;
	}
	@Override
	public LinkedHashMap<String, Collection<DialogueOperatorEffect>> dumpKBTree() throws Exception {
		LinkedHashMap<String, Collection<DialogueOperatorEffect>> ret=null;
		DialogueKBInterface currentKB=this;
		while (currentKB!=null) {
			Collection<DialogueOperatorEffect> content=currentKB.dumpKB();
			if (content!=null && !content.isEmpty()) {
				String name=currentKB.getName();
				if (ret==null) ret=new LinkedHashMap<String, Collection<DialogueOperatorEffect>>();
				ret.put(name,content);
			}
			currentKB=currentKB.getParent();
		}
		return ret;
	}
	@Override
	public Set<String> getAllVariables() throws Exception {
		Set<String> ret=null;
		DialogueKBInterface currentKB=this;
		while (currentKB!=null) {
			Set<String> content=currentKB.getAllVariablesInThisKB();
			if (content!=null && !content.isEmpty()) {
				if (ret==null) ret=new HashSet<String>();
				ret.addAll(content);
			}
			currentKB=currentKB.getParent();
		}
		return ret;
	}
	@Override
	public Set<String> getAllVariablesInThisKB() {
		if (kb!=null) { return kb.getAllVariablesInThisKB();}
		return null;
	}
	@Override
	public Collection<DialogueOperatorEffect> dumpKB() throws Exception {
		if (kb!=null) return kb.dumpKB();
		return null;
	}
	@Override
	public void printKB(String indent) {
		System.out.println(indent+"---------------------");
		printKBInternal(indent);
	}
	public void printKBInternal(String indent) {
		System.out.println("|"+indent+"Printing content of KB: "+this);
		try {
			Collection<DialogueOperatorEffect> l = dumpKB();
			if (l!=null) {
				for (DialogueOperatorEffect e:l) {
					System.out.println("|"+indent+e.toString());
				}
			}
			if (getParent()!=null) getParent().printKB(indent+" ");
		} catch (Exception e) {e.printStackTrace();}
		
	}

	@Override
	public void clearKBTree() {
		clearKB();
		if (getParent()!=null) getParent().clearKBTree();
	}
	private void clearKB() {
		if (kb!=null) kb.clear();
		if (rulesKB!=null) rulesKB.clear();
	}

	@Override
	public Object get(String vName) {
		Object value = getValueOfVariable(vName,ACCESSTYPE.AUTO_OVERWRITEAUTO,null);
		return value;
	}

	@Override
	public void set(String vName, Object value) {
		try {
			setValueOfVariable(vName, value,ACCESSTYPE.AUTO_OVERWRITEAUTO);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	/* traverse the inheritance relation. For every KB with associated some rules:
	 *  If a rule has already been successfully applied, skip it.
	 *  else:
	 *   if the rule can be applied, store the consequent in THIS kb (not the kb associated with the rule)
	 *   if it changes the KB, restart traversal from THIS kb.
	 */
	public void runForwardInference(ACCESSTYPE type) throws Exception {
		EvalContext context=new EvalContext();
		if (type!=ACCESSTYPE.AUTO_OVERWRITEAUTO && type!=ACCESSTYPE.AUTO_OVERWRITETHIS) throw new Exception("Invalid access type: "+type);
		DialogueKBInterface currentKB=this;
		DM dm=getDM();
		Logger logger=(dm!=null)?dm.getLogger():null;
		HashSet<DialogueOperatorEffect> rulesSuccessfullyApplied=new HashSet<DialogueOperatorEffect>();
		boolean restart=false;
		while (currentKB!=null) {
			restart=false;
			LinkedHashSet<DialogueOperatorEffect> rules=currentKB.getForwardInferenceRules();
			if (rules!=null && !rules.isEmpty()) {

				for(DialogueOperatorEffect r:rules) {
					if (!rulesSuccessfullyApplied.contains(r)) {
						DialogueKBFormula c=r.getAntecedent();
						DialogueOperatorEffect update=r.getConsequent();
						if((logger!=null) && logger.isTraceEnabled()) logger.debug("###("+this+")> forward inference("+type+"), using: "+c);
						if (type==ACCESSTYPE.AUTO_OVERWRITEAUTO) {
							context.setLowestUsed(new DialogueKB[1]);
						} else {
							context.setLowestUsed(null);
						}
						Boolean result=evaluateLogicalFormula(c,context);
						if (result!=null) {
							if (result) update=r.getConsequent();
							else update=r.getElseConsequent();
							if (update!=null) {
								rulesSuccessfullyApplied.add(r);
								if((logger!=null) && logger.isTraceEnabled()) logger.debug("###("+this+")> doing update with: "+update);
								DialogueKBInterface updatedKB=null;
								DialogueKBInterface[] lowest = context.getLowestUsed();
								if (lowest!=null && lowest[0]!=null) {
									updatedKB = lowest[0].store(update,ACCESSTYPE.AUTO_OVERWRITETHIS,false);
								} else {
									updatedKB = store(update,type,false);
								}
								if (updatedKB!=null) {
									if(logger!=null) {
										if (logger.isTraceEnabled()) logger.debug("#"+getID()+": the update changed the KB.");
										else if (logger.isDebugEnabled()) {
											logger.debug("###("+this+")> forward inference("+type+"), using: "+c);
											logger.debug("###("+this+")> done update with("+(result?"THEN":"ELSE")+"): "+update);
											logger.debug("###("+this+")> and the update changed the KB.");
										}
									}
									// the rule changed THIS KB, restart traversal
									currentKB=this;
									restart=true;
									if((logger!=null) && logger.isTraceEnabled()) logger.debug("======restart forward inference====>");
									break;
								}
							}
						}
					}
				}

			}
			if (!restart) currentKB=currentKB.getParent();
		}
	}

	@Override
	public LinkedHashSet<DialogueOperatorEffect> getForwardInferenceRules() {return rulesKB;}

	public static void main(String[] args) throws Exception {
		TrivialDialogueKB mykb = new TrivialDialogueKB();
		DialogueOperatorEffect e23=DialogueOperatorEffect.createAssignment(DialogueKBFormula.parse("tellAvailable(topic)"),DialogueKBFormula.parse("quote(exists(x,question(topic,?),!known(answered('self',x))))"));
		mykb.store(e23, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		
		mykb.store(DialogueOperatorEffect.createAssertion(DialogueKBFormula.parse("P(a,P1,c)")), ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		mykb.store(DialogueOperatorEffect.createAssertion(DialogueKBFormula.parse("P(a,P1,d)")), ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		mykb.store(DialogueOperatorEffect.createAssertion(DialogueKBFormula.parse("P(b,P1,c)")), ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		System.out.println(mykb.dumpKB());
		DialogueKBFormula e8=DialogueKBFormula.parse("p(a,p1,d)");
		System.out.println(mykb.evaluate(e8,null));
		System.exit(0);
		
		TrivialDialogueKB mykb2 = new TrivialDialogueKB(mykb);
		DialogueOperatorEffect f = DialogueOperatorEffect.parse("assign(b,2)");
		mykb.store(f, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		DialogueKBFormula e3=DialogueKBFormula.create("b", null);
		System.out.println(mykb.evaluate(e3,null));
		DialogueOperatorEffect f2=DialogueOperatorEffect.createAssignment("a", DialogueKBFormula.parse("quote(known(b))"));
		mykb.store(f2,ACCESSTYPE.AUTO_OVERWRITEAUTO,false);
		DialogueKBFormula e4=DialogueKBFormula.create("a", null);
		System.out.println(mykb.evaluate(e4,null));
		
		DialogueOperatorEffect e=DialogueOperatorEffect.createAssertion(DialogueKBFormula.create("test", null));
		System.out.println(mykb.evaluate(e));
		System.out.println(mykb.evaluate(e.getAssertedFormula(),null));
		mykb.store(e,ACCESSTYPE.AUTO_OVERWRITEAUTO,true);
		System.out.println(mykb.evaluate(e));
		System.out.println(mykb.evaluate(e.getAssertedFormula(),null));
		DialogueOperatorEffect e1=DialogueOperatorEffect.createAssertion(e.getAssertedFormula().negate());
		DialogueOperatorEffect e2 = DialogueOperatorEffect.createAssertion(DialogueKBFormula.create("test2", null));
		mykb2.store(e1,ACCESSTYPE.AUTO_OVERWRITEAUTO,true);
		mykb.store(e2,ACCESSTYPE.AUTO_OVERWRITEAUTO,true);
		System.out.println(mykb2.evaluate(e));
		System.out.println(mykb2.evaluate(e.getAssertedFormula(),null));
		System.out.println(mykb2.evaluate(e1));
		System.out.println(mykb2.evaluate(e1.getAssertedFormula(),null));
		System.out.println(mykb.evaluate(e2));
		System.out.println(mykb2.evaluate(e2));
		System.out.println(mykb.evaluate(DialogueKBFormula.parse("known(test)"),null));
	}

}
