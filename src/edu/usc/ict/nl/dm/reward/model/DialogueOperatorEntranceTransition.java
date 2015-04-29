package edu.usc.ict.nl.dm.reward.model;

import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import edu.usc.ict.nl.bus.events.Event;
import edu.usc.ict.nl.bus.events.NLUEvent;
import edu.usc.ict.nl.dm.reward.EventMatcher;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.nlu.ChartNLUOutput;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.Triple;
import edu.usc.ict.nl.util.XMLUtils;

public class DialogueOperatorEntranceTransition extends DialogueOperatorNodeTransition {

	private enum EntranceConditionType {CURRENT_USER,CURRENT_TOPIC,PENDING_USER,PENDING_TOPIC,SYSTEM,REENTRANCE_OPTION}

	private EntranceConditionType subtype;
	private String say;

	public static DialogueOperatorEntranceTransition createFakeEntraceConditionForEventAndOperator(String evName,DialogueOperator op) {
		DialogueOperatorEntranceTransition fake=new DialogueOperatorEntranceTransition();
		fake.event=evName;
		fake.setOperator(op);
		return fake;
	}
	
	@Override
	public String getSay() {return say;}
	public void setSay(String s) {this.say=s;}
	private String getSay(NamedNodeMap att) {
		Node sayNode = att.getNamedItem(XMLConstants.SAYID);
		if (sayNode!=null) return StringUtils.cleanupSpaces(sayNode.getNodeValue());
		else return null;
	}

	@Override
	public boolean isListenTransition() {
		return subtype==EntranceConditionType.CURRENT_USER;
	}
	
	@Override
	public boolean isTransitionNode(Node n) {
		return (n.getNodeType()==Node.ELEMENT_NODE) && n.getNodeName().toLowerCase().equals(XMLConstants.ENTRANCECONDITIONID);
	}

	@Override
	public void setType(String nodeType) throws Exception {
		if(nodeType.equals(XMLConstants.ENTRANCECONDITIONID)) type=TransitionType.ENTRY;
		else throw new Exception("Invalid type: "+nodeType);
	}
	
	@Override
	public String toString() {
		return toString(true);
	}
	public String toString(boolean shortForm) {
		DialogueKBFormula condition=getCondition();
		if (shortForm) return ((getOperator()!=null)?getOperator().toString(true):"")+"-{"+eventString()+((condition!=null)?"["+condition+"]":"")+"}->"+((getTarget()!=null)?getTarget().toString(true):"");
		else return "<"+XMLConstants.ENTRANCECONDITIONID+" "+((eventString()!=null)?XMLConstants.EVENTID+"=\""+eventString()+"\"":"")+" "+((!StringUtils.isEmptyString(getSay()))?XMLConstants.SAYID+"=\""+getSay()+"\"":"")+" "+((condition!=null)?XMLConstants.CONDITIONID+"=\""+condition.toString()+"\" ":"")+XMLConstants.TARGETID+"=\""+((getTarget()!=null)?getTarget().getName():"")+"\"/>";
	}
	@Override
	public String toGDL() {
		String gdl="edge: {source: \""+getSource().getID()+"\" target: \""+getTarget().getID()+"\" label: \""+eventString()+" "+((getCondition()==null)?"":getCondition())+"\""+((notDirectional)?"arrowstyle: \"none\"":"")+"}\n";
		return gdl;
	}

	public String eventString() {
		if (type==TransitionType.ENTRY) {
			switch (subtype) {
			case CURRENT_USER:
				return XMLConstants.CURRENT_USER+"("+event+")";
			case CURRENT_TOPIC:
				return XMLConstants.CURRENT_TOPIC+"("+event+")";
			case PENDING_USER:
				return XMLConstants.PENDING_USER+"("+event+")";
			case PENDING_TOPIC:
				return XMLConstants.PENDING_TOPIC+"("+event+")";
			case SYSTEM:
				return XMLConstants.SYSTEM;
			case REENTRANCE_OPTION:
				return XMLConstants.REENTRANCE_OPTION;
			default:
				return null;
			}
		} else return event;
	}
	
	@Override
	public DialogueOperatorNodeTransition parseTransition(Node rootNode,NamedNodeMap attributes, DialogueOperatorNode op, DialogueOperator o,DialogueOperatorNodeTransition tr) throws Exception {
		((DialogueOperatorEntranceTransition) tr).setSay(getSay(attributes));
		super.parseTransition(rootNode, attributes, op, o, tr);
		return tr;
	}

	@Override
	public void checkTransition(Node rootNode) throws Exception {
		super.checkTransition(rootNode);
		if (subtype==null) throw new Exception("re/entrance option/condition with null subtype: "+XMLUtils.prettyPrintDom(rootNode, " ", true, true));
	}
	
	@Override
	public DialogueOperatorNode getTarget() {
		return (DialogueOperatorNode) super.getTarget();
	}
	
	private static HashMap<Pattern,EntranceConditionType> types;
	static {
		types=new HashMap<Pattern, DialogueOperatorEntranceTransition.EntranceConditionType>();
		types.put(Pattern.compile("^"+XMLConstants.CURRENT_USER+"\\((.+)\\)$"), EntranceConditionType.CURRENT_USER);
		types.put(Pattern.compile("^"+XMLConstants.PENDING_USER+"(\\(.+\\))$"), EntranceConditionType.PENDING_USER);
		types.put(Pattern.compile("^"+XMLConstants.CURRENT_TOPIC+"(\\(.+\\))$"), EntranceConditionType.CURRENT_TOPIC);
		types.put(Pattern.compile("^"+XMLConstants.PENDING_TOPIC+"(\\(.+\\))$"), EntranceConditionType.PENDING_TOPIC);
		types.put(Pattern.compile("^"+XMLConstants.SYSTEM+"$"), EntranceConditionType.SYSTEM);
		types.put(Pattern.compile("^"+XMLConstants.REENTRANCE_OPTION+"$"), EntranceConditionType.REENTRANCE_OPTION);
	}

	@Override
	public void setEvent(String eventString, DialogueOperator o) throws Exception {
		if (!StringUtils.isEmptyString(eventString)) {
			String event="";
			for(Pattern p:types.keySet()) {
				Matcher m=p.matcher(eventString);
				if (m.matches()) {
					if (m.groupCount()>0) event=m.group(1);
					this.subtype=types.get(p);
					break;
				}
			}
			if (!isSystemInitiatable() && !isReEntrable()) {
				if (!StringUtils.isEmptyString(event)) {
					this.event=event;
					eventMatcher=new EventMatcher<Object>();
					eventMatcher.addEvent(event, this);
					EventMatcher<List<DialogueOperatorEntranceTransition>> emOperator = o.getEventMatcher();
					emOperator.addEventToList(event, this);
				} else throw new Exception("Invalid entrance condition event: '"+eventString+"'");
			}
		}
	}
	
	public boolean isCurrentUserInitiatable() { return (subtype==EntranceConditionType.CURRENT_USER) || (subtype==EntranceConditionType.CURRENT_TOPIC); }
	public boolean isPendingUserInitiatable() { return (subtype==EntranceConditionType.PENDING_USER) || (subtype==EntranceConditionType.PENDING_TOPIC); }
	public boolean isSystemInitiatable() { return subtype==EntranceConditionType.SYSTEM; }
	public boolean isReEntrable() { return subtype==EntranceConditionType.REENTRANCE_OPTION; }
	@Override
	public boolean isSayTransition() {return isReEntrable() && !StringUtils.isEmptyString(getSay());}
	
	public boolean isTrigger() {return true;}

	public Event handlesWhich(Event ev) throws Exception {
		if (ev!=null && !StringUtils.isEmptyString(ev.getName())) {
			NLUOutput sa=(NLUOutput) ev.getPayload();
			if (sa==null) {
				if (handles(ev.getName())) return ev;
				else return null;
			} else if (sa instanceof ChartNLUOutput) {
				List<Triple<Integer, Integer, NLUOutput>> portions = ((ChartNLUOutput)sa).getPortions();
				if (portions!=null) {
					for(Triple<Integer, Integer, NLUOutput> portion:portions) {
						NLUOutput nlu = portion.getThird();
						if (handles(nlu.getId())) return new NLUEvent(nlu, ev.getSessionID());
					}
				}
				return null;
			} else {
				if (handles(sa.getId())) return ev;
				else return null;
			}
		} else return null;
	}
	public boolean handles(String evName) throws Exception {
		if (!StringUtils.isEmptyString(evName)) {
				DialogueOperator op=getOperator();
				if (this.event.equals(evName)) {
					return true;
				} else {
					List<DialogueOperatorEntranceTransition> entranceConditions=op.getUserTriggerableTransitionsForEvent(evName);
					if (entranceConditions!=null) {
						return entranceConditions.contains(this);
					}
			}
		}
		return false;
	}

}
