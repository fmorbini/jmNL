package edu.usc.ict.nl.dm.reward.model;

import java.util.List;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.Event;
import edu.usc.ict.nl.bus.modules.DMEventsListenerInterface;
import edu.usc.ict.nl.dm.reward.RewardDM;
import edu.usc.ict.nl.dm.reward.matcher.EventMatcher;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorNode.TYPE;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.XMLUtils;
import edu.usc.ict.nl.util.graph.Edge;

public class DialogueOperatorNodeTransition extends Edge {

	protected DialogueKBFormula condition;
	protected String event;
	protected DialogueKBFormula eventToBeEvaluated;
	protected Float delay,wait;
	protected EventMatcher<Object> eventMatcher;
	protected TransitionType type;
	protected boolean consumes;
	protected boolean interruptible=false;
	protected boolean mustBeEvaluated=false;
	protected DialogueOperator operator=null;
	
	private List<List<DialogueOperatorEffect>> possibleEffects;
	public void setPossibleEffects(List<List<DialogueOperatorEffect>> possibleEffects) {this.possibleEffects = possibleEffects;}
	public List<List<DialogueOperatorEffect>> getPossibleEffects() {return possibleEffects;}

	public enum TransitionType {SYSTEM,USER,NOEVENT,ENTRY,WAIT};
	
	public boolean isSayTransition() {return type==TransitionType.SYSTEM;}
	public boolean isWaitTransition() {return type==TransitionType.WAIT;}
	public boolean isListenTransition() {return type==TransitionType.USER;}
	public boolean isNoEventTransition() {return type==TransitionType.NOEVENT;}
	public void setType(String nodeType) throws Exception {
		if(nodeType.equals(XMLConstants.SAYID)) type=TransitionType.SYSTEM;
		else if(nodeType.equals(XMLConstants.LISTENID)) type=TransitionType.USER;
		else if(nodeType.equals(XMLConstants.TRANSITIONID)) type=TransitionType.NOEVENT;
		else if(nodeType.equals(XMLConstants.WAITID)) type=TransitionType.WAIT;
		else throw new Exception("Invalid type: "+nodeType);
	}
	public DialogueOperator getOperator() {return operator;}
	public void setOperator(DialogueOperator operator) {this.operator = operator;}
	public TransitionType getType() {return type;}
	public void setCondition(DialogueKBFormula cnd) {this.condition = cnd;}
	public DialogueKBFormula getCondition() {return condition;}
	public String getEvent() {return event;}
	public String getSay() {return (isSayTransition())?getEvent():null;}
	public void setEvent(String event, DialogueOperator o) throws Exception {
		this.event = event;
		// say transitions (system actions) don't need an event matcher.
		if (!StringUtils.isEmptyString(event) && !isSayTransition()) {
			eventMatcher=new EventMatcher<Object>();
			eventMatcher.addEvent(event, this);
		}
	}
	public void setEventToBeEvaluated(DialogueKBFormula event) {
		this.eventToBeEvaluated=event;
	}
	public DialogueKBFormula getEventToBeEvaluated() {
		return eventToBeEvaluated;
	}
	public void setConsumes(boolean consumes) {this.consumes=consumes;}
	public boolean doesConsume() {return this.consumes;}
	public Float getDelay() {return delay;}
	public void setDelay(Float delay) {this.delay = delay;}
	public void setDefaultWait(Float wait) {this.wait = wait;}
	public Float getDefaultWait() {return this.wait;}
	
	public void setInterruptible(boolean isInterruptible) {
		this.interruptible = isInterruptible;
	}
	public boolean isInterruptible() {return interruptible;}
	public void setMustBeEvaluated(boolean mustBeEvaluated) {
		this.mustBeEvaluated = mustBeEvaluated;
	}
	public boolean getMustBeEvaluated() {return mustBeEvaluated;}
	
	public boolean isTrigger() {return false;}
	
	public DialogueOperatorNodeTransition parse(String s) {
		return parse(s, this);
	}
	public DialogueOperatorNodeTransition parse(String s,DialogueOperatorNodeTransition tr) {
		Document doc;
		try {
			doc = XMLUtils.parseXMLString(s, false, false);
			//System.out.println(XMLUtils.prettyPrintDom(doc, " ", true, true));
			Node rootNode = doc.getDocumentElement();
			if (isTransitionNode(rootNode)) return parseTransition(rootNode, rootNode.getAttributes(), new DialogueOperatorNode(),new DialogueOperator(),tr);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public DialogueOperatorNodeTransition parseTransition(Node rootNode,NamedNodeMap attributes, DialogueOperatorNode op, DialogueOperator o) throws Exception {
		return parseTransition(rootNode, attributes, op, o, this);
	}
	public DialogueOperatorNodeTransition parseTransition(Node rootNode,NamedNodeMap attributes, DialogueOperatorNode op, DialogueOperator o,DialogueOperatorNodeTransition tr) throws Exception {
		tr.setType(rootNode.getNodeName());
		DialogueKBFormula cnd=getCondition(attributes);
		if (cnd!=null && !cnd.isTrivialTruth()) {
			tr.setCondition(cnd);
		}
		tr.setInterruptible(isInterruptible(attributes));
		tr.setEvent(getEvent(attributes),o);
		tr.setMustBeEvaluated(mustBeEvaluated(attributes));
		if (tr.getMustBeEvaluated()) {
			tr.setEventToBeEvaluated(DialogueKBFormula.parse(tr.getEvent()));
		}
		tr.setDefaultWait(getDefaultWait(attributes));
		tr.setOperator(o);
		tr.setConsumes(getConsumes(attributes));
		tr.setDelay(getDelay(attributes));
		String target=getTarget(attributes);
		if (target!=null) {
			DialogueOperatorNode state=o.getStateNamed(target);
			if (state==null) {
				state=new DialogueOperatorNode(target, TYPE.TMP);
				o.addStateToOperator(state);
			}
			tr.setTarget(state);
			tr.setSource(op);
			if (op!=null) op.addEdge(tr,false,true);
		}
		tr.checkTransition(rootNode);
		return tr;
	}

	public void checkTransition(Node rootNode) throws Exception {
		if (isSayTransition() && StringUtils.isEmptyString(getSay())) throw new Exception("Say transition with no speech act: "+XMLUtils.domNode2String(rootNode, true,false));
		if (isListenTransition() && StringUtils.isEmptyString(getEvent())) throw new Exception("Listen transition with no speech act: "+XMLUtils.domNode2String(rootNode, true,false));
		if (isNoEventTransition() && !StringUtils.isEmptyString(getEvent())) throw new Exception("No event transition with event: "+XMLUtils.domNode2String(rootNode, true,false));
		if (getTarget()==null) throw new Exception("Transition without target: "+XMLUtils.domNode2String(rootNode, true,false));
	}
	
	private String getTarget(NamedNodeMap att) {
		Node targetNode = att.getNamedItem(XMLConstants.TARGETID);
		if (targetNode!=null) return StringUtils.cleanupSpaces(targetNode.getNodeValue());
		else return null;
	}
	private DialogueKBFormula getCondition(NamedNodeMap att) throws DOMException, Exception {
		Node cndNode = att.getNamedItem(XMLConstants.CONDITIONID);
		if (cndNode!=null) return DialogueKBFormula.parse(cndNode.getNodeValue());
		else return null;
	}
	private boolean isInterruptible(NamedNodeMap att) throws DOMException, Exception {
		Node cndNode = att.getNamedItem(XMLConstants.INTERRUPTIBLEID);
		if (cndNode!=null) {
			String value=StringUtils.cleanupSpaces(cndNode.getNodeValue());
			if (StringUtils.isEmptyString(value)) return false;
			else if (value.equalsIgnoreCase("true"))
				return true;
			else return false;
		} else return false;
	}
	private boolean mustBeEvaluated(NamedNodeMap att) throws DOMException, Exception {
		Node cndNode = att.getNamedItem(XMLConstants.EVALID);
		if (cndNode!=null) {
			String value=StringUtils.cleanupSpaces(cndNode.getNodeValue());
			if (StringUtils.isEmptyString(value)) return false;
			else if (value.equalsIgnoreCase("true"))
				return true;
			else return false;
		} else return false;
	}
	private String getEvent(NamedNodeMap att) {
		Node cndNode = att.getNamedItem(XMLConstants.EVENTID);
		if (cndNode!=null) {
			String event=StringUtils.cleanupSpaces(cndNode.getNodeValue());
			if (StringUtils.isEmptyString(event)) return null;
			else return event;
		} else return null;
	}
	private Float getDelay(NamedNodeMap att) {
		Node cndNode = att.getNamedItem(XMLConstants.DELAYID);
		if (cndNode!=null) {
			return Float.parseFloat(cndNode.getNodeValue());
		} else return null;
	}
	private Float getDefaultWait(NamedNodeMap att) {
		Node cndNode = att.getNamedItem(XMLConstants.WAITID);
		if (cndNode!=null) {
			return Float.parseFloat(cndNode.getNodeValue());
		} else return null;
	}
	private boolean getConsumes(NamedNodeMap att) {
		Node cndNode = att.getNamedItem(XMLConstants.CONSUMEID);
		if (cndNode!=null) {
			String value=StringUtils.cleanupSpaces(cndNode.getNodeValue());
			if (StringUtils.isEmptyString(value)) return false;
			else if (value.equalsIgnoreCase("true"))
				return true;
			else return false;
		} else return false;
	}

	public boolean isTransitionNode(Node n) {
		return (n.getNodeType()==Node.ELEMENT_NODE) && (
				n.getNodeName().toLowerCase().equals(XMLConstants.SAYID) ||
				n.getNodeName().toLowerCase().equals(XMLConstants.WAITID) ||
				n.getNodeName().toLowerCase().equals(XMLConstants.LISTENID) ||
				n.getNodeName().toLowerCase().equals(XMLConstants.TRANSITIONID)
				);
	}
	
	public String getUserTriggerEvent() throws Exception {return getEvent();}

	public boolean isExecutableInCurrentIS(String evName,EvalContext context) throws Exception {
		if (!isEventGoodForTransition(evName)) return false;
		else {
			context.setFormulaOperator(getOperator());
			return isConditionSatisfiedInCurrentIS(context);
		}
	}

	public boolean isConditionSatisfiedInCurrentIS(EvalContext context) throws Exception {
		if (getCondition()!=null) {
			DialogueKBInterface is = context.getInformationState();
			Object result=is.evaluate(getCondition(),context);
			if ((result!=null) && (result instanceof Boolean)) return (Boolean)result;
			else return false;
		} else return true;
	}
	/* an event is good for a given transition if:
	 *  the transition has no event associated (event matcher is null)
	 *  the event matches the event associated with the transition
	 */
	public boolean isEventGoodForTransition(String evName) {
		if (eventMatcher!=null) {
			if (evName!=null) return !eventMatcher.match(evName).isEmpty();
			else return false;
		} else if (evName==null) return true;
		else return false;
		/*if (!isSayTransition() && ((evName!=null) || (eventMatcher!=null))) {
			if ((evName!=null) && (eventMatcher!=null)) {
				return !eventMatcher.match(evName).isEmpty();
			}
			else if (evName!=null) return true; // no event transition
			else return false;
		}
		else return true;*/
	}
	
	@Override
	public String toGDL() {
		String gdl="edge: {source: \""+getSource().getID()+"\" target: \""+getTarget().getID()+"\" label: \""+toString()+"["+getWeight()+"]"+"\""+((notDirectional)?"arrowstyle: \"none\"":"")+"}\n";
		//String label=toString();
		//String gdl="edge: {source: \""+getSource().getID()+"\" target: \""+getTarget().getID()+"\" "+((!StringUtils.isEmptyString(label))?("label: \""+toString()+"\""):"")+((notDirectional)?"arrowstyle: \"none\"":"")+"}\n";
		return gdl;
	}
	
	@Override
	public String toString() {
		return toString(true);
	}
	public String toString(boolean shortForm) {
		DialogueKBFormula condition=getCondition();
		String transitionType=null;
		if (isListenTransition()) transitionType=XMLConstants.LISTENID;
		else if (isSayTransition()) transitionType=XMLConstants.SAYID;
		else if (isWaitTransition()) transitionType=XMLConstants.WAITID;
		else if (isNoEventTransition()) transitionType=XMLConstants.TRANSITIONID;
		if (shortForm) return "<"+getSource()+"->"+getTarget()+": "+((event!=null)?event:"")+((delay!=null)?" "+delay+" seconds":"")+((condition==null)?"":" "+condition)+">";
		//if (shortForm)
		//	if ((event!=null) || (condition!=null))
		//		return "<"+((event!=null)?event:"")+" "+((condition==null)?"":condition)+">";
		//	else
		//		return "";
		else return "<"+transitionType+" "+((event!=null)?XMLConstants.EVENTID+"=\""+XMLUtils.escapeStringForXML(event)+"\" ":" ")+
				((delay!=null)?XMLConstants.DELAYID+"=\""+delay+"\" ":" ")+
				((condition!=null)?XMLConstants.CONDITIONID+"=\""+condition.toString()+"\" ":" ")+
		XMLConstants.TARGETID+"=\""+getTarget().getName()+"\" "+
		((doesConsume())?XMLConstants.CONSUMEID+"=\"true\" ":" ")+
		"/>";
	}

	public static void main(String[] args) {
		String test="<transition condition=\"speech-act==answer.symptom.*\" trigger=\"true\" target=\"symptom\"/>";
		test="<ec event=\"reentrance-option\" target=\"s7004-question.bio-info.age\"/>";
		DialogueOperatorEntranceTransition t1 = new DialogueOperatorEntranceTransition();
		DialogueOperatorNodeTransition t=new DialogueOperatorNodeTransition();
		DialogueOperatorNodeTransition pt = t1.parse(test);
		System.out.println(pt);
	}
	public boolean willSay(EvalContext context) throws Exception {
		return isSayTransition() && ((this instanceof DialogueOperatorEntranceTransition) || isExecutableInCurrentIS(null, context));
	}
	public void execute(final DialogueAction a, final EvalContext context,Event sourceEvent) throws Exception {
		if (isSayTransition()) {
			RewardDM dm = a.getDM();
			DMEventsListenerInterface messageBus = dm.getMessageBus();
			long sid=dm.getSessionID();
			String say=getSay();
			if (!StringUtils.isEmptyString(say)) {
				DialogueOperator op=a.getOperator();
				if (willSay(context)) {
					if (getMustBeEvaluated()) {
						dm.getLogger().info("say transition must be evaluated: '"+getEventToBeEvaluated()+"'.");
						DialogueKB is = context.getInformationState();
						Object result = is.evaluate(getEventToBeEvaluated(), context);
						say=DialogueKBFormula.getStringValue(result!=null?result.toString():null);
						dm.getLogger().info("result of evaluation: '"+say+"'.");
					}
					TimemarksTracker tt = dm.getTimemarkTracker();
					if (tt!=null) tt.setMark(getOperator().getName(),TimemarksTracker.TYPES.SAY,say);
					dm.getLogger().info("Operator '"+op+"' will say: '"+say+"'");
					//dm.updateSystemSayTracker(say);
					DMSpeakEvent sayEvent=new DMSpeakEvent(sourceEvent,say,sid,null,context.getInformationState());
					sayEvent.setDefaultWait(getDefaultWait());
					messageBus.handleDMResponseEvent(sayEvent);
				} else {
					dm.getLogger().warn("False condition! Operator '"+op+"' will NOT say: '"+say+"'");
				}
			}
		}
	}
}
