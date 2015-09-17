package edu.usc.ict.nl.dm.reward.model;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.kb.VariableProperties.PROPERTY;
import edu.usc.ict.nl.kb.TrivialDialogueKB;
import edu.usc.ict.nl.kb.VariableProperties;
import edu.usc.ict.nl.kb.parser.FormulaGrammar;
import edu.usc.ict.nl.kb.parser.ParseException;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.XMLUtils;

public class DialogueOperatorEffect implements Comparable<DialogueOperatorEffect> {
	public enum EffectType {ASSIGNMENT,ASSERTION,GOAL,IMPLICATION,SWAPOUT,SEND,INTERRUPT,AssignmentLIST};
	DialogueKBFormula left,right;
	String goalName,eventName;
	Object value;
	DialogueOperatorEffect implyElse;
	List<DialogueOperatorEffect> list;
	EffectType type;
	VariableProperties varPropertiesForAssignment=VariableProperties.defaultProperties;
	
	public static DialogueOperatorEffect createSend(String eventName) throws Exception {
		if (StringUtils.isEmptyString(eventName)) throw new Exception("empty event used to create a send effect.");
		DialogueOperatorEffect f=new DialogueOperatorEffect();
		f.type=EffectType.SEND;
		f.eventName=eventName;
		return f;
	}
	
	public static DialogueOperatorEffect createSwapOut() {
		DialogueOperatorEffect f=new DialogueOperatorEffect();
		f.type=EffectType.SWAPOUT;
		return f;
	}
	public static DialogueOperatorEffect createInterrupt() {
		DialogueOperatorEffect f=new DialogueOperatorEffect();
		f.type=EffectType.INTERRUPT;
		return f;
	}
	public static DialogueOperatorEffect createImplication(DialogueKBFormula ante,DialogueOperatorEffect then) throws Exception {
		return createImplication(ante, then, null);
	}
	public static DialogueOperatorEffect createImplication(DialogueKBFormula ante,DialogueOperatorEffect then,DialogueOperatorEffect elsePart) throws Exception {
		if (then==null) throw new Exception("invalid implication: null effect");
		else {
			DialogueOperatorEffect f=new DialogueOperatorEffect();
			f.type=EffectType.IMPLICATION;
			f.left=ante;
			f.value=then;
			f.implyElse=elsePart;
			return f;
		}
	}
	public static DialogueOperatorEffect createList(List<DialogueOperatorEffect> args) throws Exception {
		DialogueOperatorEffect f=new DialogueOperatorEffect();
		f.type=EffectType.AssignmentLIST;
		f.list=new ArrayList<DialogueOperatorEffect>();
		for(DialogueOperatorEffect a:args) {
			if (!a.isAssignment()) throw new Exception(a+" is not an assigment.");
			f.list.add(a);
		}
		return f;
	}
	public List<DialogueOperatorEffect> getAssignmentList() {
		return list;
	}
	public static DialogueOperatorEffect createAssertion(DialogueKBFormula left) throws Exception {
		DialogueOperatorEffect f=new DialogueOperatorEffect();
		left=left.normalize();
		
		if (left.isConstant()) {
			f.type=EffectType.ASSIGNMENT;
			f.left=left;
			f.right=DialogueKBFormula.trueFormula;
			return f;
		} else if (left.isNegatedFormula()) {
			DialogueKBFormula child=(DialogueKBFormula) left.getFirstChild();
			if (child.isConstant()) {
				f.type=EffectType.ASSIGNMENT;
				f.left=child;
				f.right=DialogueKBFormula.falseFormula;
				return f;
			}
		}
		
		f.type=EffectType.ASSERTION;
		if (left.isNegatedFormula()) {
			left=(DialogueKBFormula) left.getFirstChild();
			f.right=DialogueKBFormula.falseFormula;
		} else {
			f.right=DialogueKBFormula.trueFormula;
		}
		f.left=left;
		return f;
	}
	public static DialogueOperatorEffect createAssignment(String varName,Object value) throws Exception {
		return createAssignment(DialogueKBFormula.create(varName, null), value, true);
	}
	public static DialogueOperatorEffect createAssignment(DialogueKBFormula left,Object value) throws Exception {
		return createAssignment(left, value, true);		
	}
	public static DialogueOperatorEffect createAssignment(DialogueKBFormula left,Object value,boolean parseValue) throws Exception {
		if (!left.isConstant()) throw new Exception("Invalid left hand side of assignment: "+left);
		if (value!=null) { 
			if (value instanceof String) {
				String rightString=((String) value).toLowerCase();
				if (rightString.equals("true"))
					return DialogueOperatorEffect.createAssertion(left);
				else if (rightString.equals("false"))
					return DialogueOperatorEffect.createAssertion(left.negate());
			} else if (value instanceof DialogueKBFormula) {
				if (((DialogueKBFormula) value).isConstant()) {
					String rightString=((DialogueKBFormula)value).getName();
					if (rightString.equals("true"))
						return DialogueOperatorEffect.createAssertion(left);
					else if (rightString.equals("false"))
						return DialogueOperatorEffect.createAssertion(left.negate());
				}
			} else if (value instanceof Boolean) {
				if ((Boolean) value) return DialogueOperatorEffect.createAssertion(left);
				else return DialogueOperatorEffect.createAssertion(left.negate());
			}
		}

		DialogueOperatorEffect f=new DialogueOperatorEffect();
		f.type=EffectType.ASSIGNMENT;
		f.left=left;
		if (value instanceof DialogueKBFormula) {
			DialogueKBFormula right=(DialogueKBFormula)value;
			//if (!right.isNumericFormula() && !right.isConstant()) throw new Exception("Right hand side of assignment is not a numeric expression or constant: "+right);
			f.right=right;
		} else {
			if (parseValue && ((value instanceof String) || (value instanceof Number))) {
				try {
					if (value!=null) {
						if (value instanceof String && !DialogueKBFormula.isStringConstant((String) value)) {
							value=DialogueKBFormula.generateStringConstantFromContent((String) value);
						}
						DialogueKBFormula nv=DialogueKBFormula.parse(value.toString());
						f.right=nv;
					}
				} catch (Exception e) {
					f.value=value;
				}
			} else f.value=value;
		}
		return f;
	}
	public void setAssignmentProperties(VariableProperties properties) {
		varPropertiesForAssignment=properties;
	}
	public void setAssignmentProperty(PROPERTY p,boolean v) {
		if (varPropertiesForAssignment==null || varPropertiesForAssignment==VariableProperties.defaultProperties) {
			varPropertiesForAssignment=new VariableProperties();
		}
		varPropertiesForAssignment.setProperty(p, v);
	}
	public VariableProperties getAssignmentProperties() {return varPropertiesForAssignment;}
	public static DialogueOperatorEffect createIncrementForVariable(String var,DialogueKBFormula increment) throws Exception {
		DialogueKBFormula varf=DialogueKBFormula.create(var, null);
		Collection<DialogueKBFormula> args=new ArrayList<DialogueKBFormula>();
		args.add(varf);
		args.add(increment);
		DialogueKBFormula incf=DialogueKBFormula.create("+", args);
		return DialogueOperatorEffect.createAssignment(varf, incf, false);
	}
	public static DialogueOperatorEffect createIncrementForVariable(String var,Number val) throws Exception {
		DialogueKBFormula increment=DialogueKBFormula.create(val+"",null);
		return DialogueOperatorEffect.createIncrementForVariable(var, increment);
	}

	public static DialogueOperatorEffect createGoal(String goal,DialogueKBFormula value) {
		DialogueOperatorEffect f=new DialogueOperatorEffect();
		f.type=EffectType.GOAL;
		f.goalName=goal;
		f.left=value;
		return f;
	}

	public boolean isSwapOut() {return type==EffectType.SWAPOUT;}
	public boolean isInterrupt() {return type==EffectType.INTERRUPT;}
	public boolean isSend() {return type==EffectType.SEND;}
	public boolean isAssertion() {return type==EffectType.ASSERTION;}
	public boolean isAssignment() {return type==EffectType.ASSIGNMENT;}
	public boolean isGoalAchievement() {return type==EffectType.GOAL;}
	public boolean isImplication() {return type==EffectType.IMPLICATION;}
	public boolean isAssignmentList() {return type==EffectType.AssignmentLIST;}
	public String getSendEvent() {return (isSend())?eventName:null;}
	public DialogueKBFormula getAntecedent() {return (isImplication())?left:null;}
	public DialogueOperatorEffect getConsequent() {if (isImplication()) return (DialogueOperatorEffect) value; else return null;}
	public DialogueOperatorEffect getElseConsequent() {if (isImplication()) return (DialogueOperatorEffect) implyElse; else return null;}
	public DialogueKBFormula getGoalValue() {if (isGoalAchievement()) return left; else return null;}
	public DialogueKBFormula getAssertedFormula() {if (isAssertion()) return left; else return null;}
	public Boolean getAssertionSign() {if (isAssertion()) return right.isTrivialTruth(); else return null;}
	public void setAssertedFormula(DialogueKBFormula a) {this.left=a;}
	public String getGoalName() {return goalName;}
	public DialogueKBFormula getAssignedVariable() {
		if (isAssignment() && left!=null && left.isVariable()) return left;
		else return null;
	}
	public Object getAssignedExpression() {
		if (isAssignment()) {return (right!=null)?right:value;}
		else return null;
	}
	public void setAssignedExpression(Object ne) {
		if (ne instanceof DialogueKBFormula) {
			value=null;
			right=(DialogueKBFormula) ne;
		} else {
			right=null;
			value=ne;
		}
	}
	
	@Override
	public String toString() {
		return toString(true);
	}
	public String toString(boolean shortForm) {
		return toString(shortForm, XMLConstants.EFFECTID);
	}
	public String toString(boolean shortForm,String effectXmlName) {
		String ret="";
		if (shortForm) {
			if (isAssertion()) ret+=XMLConstants.AssignmentID+"("+left+","+right+")";
			else if (isAssignment()) ret+=XMLConstants.AssignmentID+"("+left+","+getAssignedExpression()+")";
			else if (isGoalAchievement()) ret+=XMLConstants.GOALID+": "+goalName+((left!=null)?" "+XMLConstants.VALUEID+": "+left:"");
			else if (isImplication()) ret+=XMLConstants.implyID+"("+left+","+value+","+implyElse+")";
			else if (isSwapOut()) ret+=XMLConstants.SWAPOUTID;
			else if (isInterrupt()) ret+=XMLConstants.INTERRUPTID;
			else if (isSend()) ret+=XMLConstants.SENDID+"("+eventName+")";
			else if (isAssignmentList()) {
				boolean first=true;
				for(DialogueOperatorEffect e:getAssignmentList()) {
					ret+=((!first)?",":XMLConstants.AssignmentLISTID+"(")+e.toString(shortForm);
					first=false;
				}
				ret+=")";
			}
			else ret+="unknown effect"; 
		} else {
			ret="<"+effectXmlName+" ";
			if (isAssertion()) ret+=XMLConstants.EXPRID+"=\""+XMLConstants.AssignmentID+"("+left+","+right+")\"";
			else if (isAssignmentList()) {
				String list="";
				boolean first=true;
				for(DialogueOperatorEffect e:getAssignmentList()) {
					list+=((!first)?",":"")+e.toString(shortForm);
					first=false;
				}
				ret+=XMLConstants.EXPRID+"=\""+XMLConstants.AssignmentLISTID+"("+list+")\"";
			}
			else if (isAssignment()) {
				String ps=XMLConstants.VISIBLEID+"=\""+!varPropertiesForAssignment.getProperty(PROPERTY.HIDDEN)+"\" "+
						XMLConstants.READONLYID+"=\""+varPropertiesForAssignment.getProperty(PROPERTY.READONLY)+"\" "+
						XMLConstants.PERSISTENTID+"=\""+varPropertiesForAssignment.getProperty(PROPERTY.PERSISTENT)+"\" ";
				ret+=ps+XMLConstants.EXPRID+"=\""+XMLConstants.AssignmentID+"("+left+","+serialize(getAssignedExpression())+")\"";
			}
			else if (isGoalAchievement()) ret+=XMLConstants.GOALID+"=\""+goalName+"\""+((left!=null)?" "+XMLConstants.VALUEID+"=\""+left+"\"":"");
			else if (isImplication()) ret+=XMLConstants.EXPRID+"=\""+XMLConstants.implyID+"("+left+","+value+","+implyElse+")\"";
			else if (isSwapOut()) ret="<"+XMLConstants.SWAPOUTID;
			else if (isSend()) ret="<"+XMLConstants.SENDID+" "+XMLConstants.IDID+"=\""+eventName+"\"";
			else return super.toString();
			ret+="/>";
		}
		return ret;
	}

	private static XStream serializer=new XStream(new StaxDriver());
	private String serialize(Object assignedExpression) {
		String ret=null; 
		if (assignedExpression!=null) {
			if (assignedExpression instanceof DialogueKBFormula || assignedExpression instanceof Number || assignedExpression instanceof String) {
				ret=assignedExpression.toString();
			}
			else {
				ret = serializer.toXML(assignedExpression);
				ret="'"+ret+"'";
			}
			ret=XMLUtils.escapeStringForXML(ret);
		}
		return ret;
	}

	public static DialogueOperatorEffect fromXML(String xml) throws ParserConfigurationException, SAXException, IOException, ParseException {
		DialogueOperatorEffect eff=null;
		Document doc = XMLUtils.parseXMLString(xml, false,false);
		org.w3c.dom.Node rootNode = doc.getDocumentElement();
		if (RewardPolicy.isInitNode(rootNode)) {
			NamedNodeMap atts = rootNode.getAttributes();
			eff=parse(atts);
		}
		return eff;
	}

	public static DialogueOperatorEffect parse(String fs) throws ParseException {
		fs=StringUtils.removeLeadingAndTrailingSpaces(fs);
		if (!StringUtils.isEmptyString(fs)) {
			//fs=fs.toLowerCase();
			FormulaGrammar parser = new FormulaGrammar(new StringReader(fs));
			return parser.effect();
		} else return null;
	}
	public static DialogueOperatorEffect parse(NamedNodeMap childAtt) throws ParseException {
		DialogueOperatorEffect eff=parse(RewardPolicy.getInitNodeValue(childAtt));
		if (eff!=null && eff.isAssignment()) {
			Boolean hidden=SpecialVar.getIsHidden(childAtt);
			Boolean persistent=SpecialVar.getIsPersistent(childAtt);
			Boolean readOnly=SpecialVar.getIsReadOnly(childAtt);
			if (hidden!=null) eff.setAssignmentProperty(PROPERTY.HIDDEN, hidden);
			if (persistent!=null) eff.setAssignmentProperty(PROPERTY.PERSISTENT, persistent);
			if (readOnly!=null) eff.setAssignmentProperty(PROPERTY.READONLY, readOnly);
		}
		return eff;
	}

	public float evaluateGoalValueIn(DialogueKBInterface is) throws Exception {
		if (isGoalAchievement()) {
			DialogueKBFormula f=getGoalValue();
			if (f!=null) {
				//System.out.println(f+" "+is.evaluateNumericTerm(f));
				Object v = is.evaluate(f,null);
				if (v!=null) {
					if (v instanceof Long) return ((Long) v).floatValue();
					else if (v instanceof Float) return (Float)v;
					else throw new Exception("Invalid goal value type.");
				}
				else return 0;
			}
		}
		return 0;
	}

	public static DialogueOperatorEffect getEffectExpr(Node c, NamedNodeMap att) throws Exception {
		boolean isSwapOut=c.getNodeName().toLowerCase().equals(XMLConstants.SWAPOUTID);
		boolean isInterrupt=c.getNodeName().toLowerCase().equals(XMLConstants.INTERRUPTID);
		boolean isSend=c.getNodeName().toLowerCase().equals(XMLConstants.SENDID);
		if (isSwapOut) return createSwapOut();
		else if (isInterrupt) return createInterrupt();
		else if (isSend) {
			Node send = att.getNamedItem(XMLConstants.IDID);
			if (send!=null) {
				String sendEventName=send.getNodeValue();
				return createSend(sendEventName);
			}
		}
		else {
			Node nodeExpr = att.getNamedItem(XMLConstants.EXPRID);
			Node nodeGoal = att.getNamedItem(XMLConstants.GOALID);
			if (nodeGoal!=null) {
				String goalName=nodeGoal.getNodeValue();
				if (StringUtils.isEmptyString(goalName)) throw new Exception("invalid goal effect: '"+XMLUtils.domNode2String(c, true,false)+"'");
				String varGoalName=buildVarNameForGoal(goalName);
	
				String valueString=(nodeExpr!=null)?StringUtils.cleanupSpaces(nodeExpr.getNodeValue()):null;
				DialogueKBFormula value;
				if (StringUtils.isEmptyString(valueString)) value=DialogueKBFormula.parse(varGoalName);
				else value=DialogueKBFormula.parse(valueString);
				
				value=processGoalExpression(value,varGoalName);
				
				return DialogueOperatorEffect.createGoal(goalName, value);
			} else if (nodeExpr!=null) {
				DialogueOperatorEffect e=DialogueOperatorEffect.parse(nodeExpr.getNodeValue());
				DialogueKBFormula f=null;
				if (e==null) throw new Exception("Effect '"+XMLUtils.domNode2String(c, true,false)+"' returned an empty expression for effects (only assertions and assignments).");
				else if (e.isAssertion() && (((f=e.getAssertedFormula())==null) || f.isConjunction() || f.isDisjunction()))
					throw new Exception("Invalid assertion: "+f);
				else if (e.isAssignment() && (e.getAssignedVariable()==null))
					throw new Exception("Invalid assignmend: "+f);
				else return e;
			}
		}
		return null;
	}
	public static String buildVarNameForGoal(String goalName) {
		return ("valueFor_"+goalName).toLowerCase();
	}
	public static DialogueKBFormula processGoalExpression(DialogueKBFormula f,String goalVarName) throws Exception {
		HashMap<DialogueKBFormula,DialogueKBFormula> subs=new HashMap<DialogueKBFormula, DialogueKBFormula>();
		subs.put(DialogueKBFormula.create(".", null), DialogueKBFormula.create(goalVarName,null));
		return f.substitute(subs);
	}
	public static boolean isEffectNode(Node n) {
		return (n.getNodeType()==Node.ELEMENT_NODE) && (
				n.getNodeName().toLowerCase().equals(XMLConstants.EFFECTID) ||
				n.getNodeName().toLowerCase().equals(XMLConstants.SENDID) ||
				n.getNodeName().toLowerCase().equals(XMLConstants.SWAPOUTID) ||
				n.getNodeName().toLowerCase().equals(XMLConstants.INTERRUPTID)
		);
	}

	public String getLeftID() {		
		if (isAssertion()) return getAssertedFormula().getID()+"";
		else if (isAssignment()) {
			return getAssignedVariable().getID()+"";
		}
		else if (isGoalAchievement()) return getGoalName()+"";
		else if (isImplication()) return getAntecedent().getID()+"";
		else return null; 
	}
	public String getRightID() {
		if (isAssertion()) return getAssertionSign()+"";
		else if (isAssignment()) {
			Object expr=getAssignedExpression();
			String valueID="";
			if (expr instanceof DialogueKBFormula) {
				valueID=((DialogueKBFormula) expr).getID()+"";
			} else  if (null==expr){
				valueID="null";
			} else {
				valueID=expr.toString();
			}
			return valueID;
		}
		else if (isGoalAchievement()) return (getGoalValue()!=null)?getGoalValue().getID()+"":"";
		else if (isImplication()) return getConsequent().getID()+":"+getElseConsequent();
		else return null;
	}
	public String getID() {
		if (isAssignmentList()) {
			String id="";
			boolean first=true;
			for(DialogueOperatorEffect x:getAssignmentList()) { 
				id+=((first)?"":",")+x.getID();
			}
			return id;
		} else return type+":"+getLeftID()+":"+getRightID();
	}
	
	@Override
	public boolean equals(Object obj) {		
		if (obj instanceof DialogueOperatorEffect) {
			DialogueOperatorEffect oe=(DialogueOperatorEffect)obj;
			return type==oe.type && getLeftID().equals(oe.getLeftID()) && getRightID().equals(oe.getRightID());
		} else return super.equals(obj);
	}

	public Set<String> doesEffectUseAllKnownVariables(DialogueKBInterface is, Map<String, DialogueKBFormula> localVars) {
		Set<DialogueKBFormula> variables=null;
		variables = extractAllNamesUsed();
		if (isAssignment()) {
			if (variables==null) variables=new HashSet<DialogueKBFormula>();
			variables.add(getAssignedVariable());
		}
		Set<String>ret=null;
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

	public Set<DialogueKBFormula> extractAllNamesUsed() {
		if (isAssignment()) {
			Object thing = getAssignedExpression();
			if (thing instanceof DialogueKBFormula) {
				DialogueKBFormula r=(DialogueKBFormula) thing;
				return r.extractAllNamesUsed();
			} else return null;
		} else if (isAssertion()) {
			DialogueKBFormula f=getAssertedFormula();
			return f.extractAllNamesUsed();
		} else if (isGoalAchievement()) {
			DialogueKBFormula f=getGoalValue();
			return f.extractAllNamesUsed();
		} else if (isImplication()){
			Set<DialogueKBFormula> resa = getAntecedent().extractAllNamesUsed();
			Set<DialogueKBFormula> resc = getConsequent().extractAllNamesUsed();
			Set<DialogueKBFormula> ret=null;
			if (resa!=null) ret=resa;
			if (resc!=null) {
				if (getConsequent().isAssignment()) {
					resc.add(getConsequent().getAssignedVariable());
				}
				if (ret!=null) ret.addAll(resc);
				else ret=resc;
			}
			return ret;
		} else if (isAssignmentList()) {
			Set<DialogueKBFormula> res=null;
			for(DialogueOperatorEffect x:getAssignmentList()) {
				Set<DialogueKBFormula> newNames = x.extractAllNamesUsed();
				if (newNames!=null) {
					if (res==null) res=newNames;
					else res.addAll(newNames);
				}
			}
			return res;
		} else return null;
	}
	
	@Override
	public int compareTo(DialogueOperatorEffect o) {
		return getID().compareTo((o!=null)?o.getID():null);
	}
	public int compareUsingStrings(DialogueOperatorEffect o) {
		return toString().compareTo((o!=null)?o.toString():null);
	}

	public static void main(String[] args) throws Exception {
		TrivialDialogueKB kb=new TrivialDialogueKB();
		DialogueOperatorEffect f3 = parse("assert(or(a,b))");
		kb.store(f3, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
		//f3=createAssertion(DialogueKBFormula.parse("and(a,b"));
		//DialogueOperatorEffect f3 = parse("++q");
		System.out.println(f3.toString(false));
		System.out.println(f3.extractAllNamesUsed());
	}

}
