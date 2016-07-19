package edu.usc.ict.nl.dm.reward.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.usc.ict.nl.bus.events.Event;
import edu.usc.ict.nl.bus.events.NLUEvent;
import edu.usc.ict.nl.bus.events.changes.VarChange;
import edu.usc.ict.nl.bus.modules.DMEventsListenerInterface;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.dm.reward.RewardDM;
import edu.usc.ict.nl.dm.reward.SwapoutReason;
import edu.usc.ict.nl.dm.reward.SwapoutReason.Reason;
import edu.usc.ict.nl.dm.reward.model.RewardPolicy.OpType;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.XMLUtils;
import edu.usc.ict.nl.util.graph.Edge;
import edu.usc.ict.nl.vhmsg.VHBridge;

public class DialogueOperatorNode extends edu.usc.ict.nl.util.graph.Node{
	private boolean reportStateChange=false;
	public enum TYPE {NORMAL,FINAL,TMP,SWAPOUT};
	private TYPE type;
	private List<DialogueOperatorEffect> effects;
	private VHBridge vhBridge=null;

	private static DialogueOperatorNodeTransition trFactory=new DialogueOperatorNodeTransition();

	public DialogueOperatorNode(String id,DialogueOperatorNode.TYPE t) {
		super(id);
		setType(t);
	}
	public DialogueOperatorNode() {
		setType(TYPE.TMP);
	}
	
	public TYPE getType() {return type;}
	public void setType(TYPE type) {
		switch (type) {
		case FINAL:
			setAsFinal();
			break;
		case NORMAL:
			setAsNormal();
			break;
		case TMP:
			setAsTmp();
			break;
		case SWAPOUT:
			setAsSwapOut();
			break;
		}
	}
	public void setAsFinal() {
		if (!isSwapOut()) type=TYPE.FINAL;
	}
	public void setAsNormal() {
		type=TYPE.NORMAL;
	}
	public void setAsSwapOut() {
		type=TYPE.SWAPOUT;
	}
	public void setAsTmp() {
		type=TYPE.TMP;
	}
	
	public void setReportStateChange(boolean reportStateChange) {
		this.reportStateChange = reportStateChange;
	}
	public boolean getReportStateChange() {return reportStateChange;}
	
	public List<DialogueOperatorEffect> getEffects() {return effects;}
	public void setEffects(List<DialogueOperatorEffect> es) {this.effects=es;}
	public boolean hasEffects() {
		List<DialogueOperatorEffect> effects = getEffects();
		return (effects!=null && !effects.isEmpty());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DialogueOperatorNode) {
			return (name==null)?name==((DialogueOperatorNode) obj).name:name.equals(((DialogueOperatorNode) obj).name);
		} else return super.equals(obj);
	}
	
	public DialogueOperatorNode parse(String s) {
		return parse(s, this);
	}
	public DialogueOperatorNode parse(String s,DialogueOperatorNode state) {
		Document doc;
		try {
			doc = XMLUtils.parseXMLString(s, false, false);
			//System.out.println(XMLUtils.prettyPrintDom(doc, " ", true, true));
			Node rootNode = doc.getDocumentElement();
			if (isStateNode(rootNode)) return DialogueOperatorNode.parseState(rootNode, rootNode.getAttributes(), new DialogueOperator());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static DialogueOperatorNode parseState(Node n,NamedNodeMap attributes, DialogueOperator o) throws Exception {
		String id=getStateName(attributes);
		if (StringUtils.isEmptyString(id)) throw new Exception("State with null ID: "+XMLUtils.domNode2String(n, true,false));
		TYPE t=getStateType(attributes);
		if (t==null) t=TYPE.NORMAL;
		// retrieve state with same name if already there.
		DialogueOperatorNode state = o.getStateNamed(id);
		if (state==null) state = new DialogueOperatorNode(id, t);
		else state.setType(t);
		
		state.setReportStateChange(getStateChangeReporting(attributes));

		NodeList cs = n.getChildNodes();
		for (int i = 0; i < cs.getLength(); i++) {
			Node c = cs.item(i);
			NamedNodeMap childAtt = c.getAttributes();
			if (DialogueOperatorEffect.isEffectNode(c)) state.addEffectToState(DialogueOperatorEffect.getEffectExpr(c,childAtt));
			else if (trFactory.isTransitionNode(c)) trFactory.parseTransition(c,childAtt,state,o,new DialogueOperatorNodeTransition());
			else if (!StringUtils.isEmptyString(StringUtils.cleanupSpaces(XMLUtils.domNode2String(c, true,true)))) System.out.println("Un-used element in operator node "+state.toString(true)+": "+XMLUtils.domNode2String(c, true,true));
		}

		return state;
	}
	
	private void addEffectToState(DialogueOperatorEffect e) {
		if (effects==null) effects=new ArrayList<DialogueOperatorEffect>();
		if (e!=null) {
			effects.add(e);
			if (e.isSwapOut()) setType(TYPE.SWAPOUT);
		}
	}
	public boolean isSwapOut() {return getType()==TYPE.SWAPOUT;}
	
	private static String getStateName(NamedNodeMap att) {
		Node nodeID = att.getNamedItem(XMLConstants.IDID);
		if (nodeID!=null) return StringUtils.cleanupSpaces(nodeID.getNodeValue());
		else return null;
	}
	private static boolean getStateChangeReporting(NamedNodeMap att) {
		Node nodeID = att.getNamedItem(XMLConstants.REPORTID);
		if (nodeID!=null) {
			String id=StringUtils.cleanupSpaces(nodeID.getNodeValue()).toLowerCase();
			return (id.equals("true"));
		}
		else return false;
	}
	private static TYPE getStateType(NamedNodeMap att) {
		Node nodeType = att.getNamedItem(XMLConstants.TYPEID);
		if (nodeType!=null) return TYPE.valueOf(nodeType.getNodeValue().toUpperCase());
		else return null;
	}

	public static boolean isStateNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.STATEID);
	}
	
	public boolean isLeaf() {
		Collection<Edge> oes = getOutgoingEdges();
		return ((oes==null) || oes.isEmpty());
	}
	public boolean isFinal() {return getType()==TYPE.FINAL;}

	@Override
	public String gdlText() {
		String gdl="node: { shape: "+getShape()+" color: "+((hasEffects())?"red":"white")+" title: \""+getID()+"\" label: \""+toString(true)+"\" info1: \"";
		List<DialogueOperatorEffect> es = getEffects();
		if (es!=null) {
			for(DialogueOperatorEffect e:es) {
				gdl+=e.toString(true)+"\n";
			}
		}
		return gdl+"\"}\n";
	}
	
	public boolean isWaitingForUser() {
		List<Edge> out = getOutgoingEdges();
		if (out!=null) {
			for(Edge e:out) {
				DialogueOperatorNodeTransition tr=(DialogueOperatorNodeTransition)e;
				return tr.isListenTransition();
			}
		}
		return false;
	}
	
	@Override
	public String getShape() {
		if (isWaitingForUser()) return "ellipse";
		else return "box";
	}

	@Override
	public String toString() {
		return toString(true);
	}
	public String toString(boolean shortForm) {
		if (shortForm) return getName()+"["+getWeight()+"]";
		//if (shortForm) return getName();
		else {
			String ret="<"+XMLConstants.STATEID+" "+XMLConstants.IDID+"=\""+getName()+"\" "+XMLConstants.TYPEID+"=\""+getType()+"\">";
			if (getValue()!=null) ret+=getValue().toString();
			if (getEffects()!=null) for (DialogueOperatorEffect e:getEffects()) ret+=e.toString(shortForm);
			if (getOutgoingEdges()!=null) for (Edge edge:getOutgoingEdges()) ret+=((DialogueOperatorNodeTransition)edge).toString(shortForm);
			ret+="</"+XMLConstants.STATEID+">";
			return ret;
		}
	}
	
	public Set<List<DialogueOperatorNode>> visitParentsFindEffects() {
		return visitParentsFindEffects(new HashSet<List<DialogueOperatorNode>>(),null,false);
	}
	// traverses the ancestors and collects the possible ancestors with effects.
	public Set<List<DialogueOperatorNode>> visitParentsFindEffects(Set<List<DialogueOperatorNode>> set,List<DialogueOperatorNode> list,boolean branched) {
		List<DialogueOperatorEffect> effects = getEffects();
		if (effects!=null) {
			if ((list==null) || branched) {
				list=(list!=null)?new ArrayList<DialogueOperatorNode>(list):new ArrayList<DialogueOperatorNode>();
				set.add(list);
			}
			list.add(this);
		}
		Set<edu.usc.ict.nl.util.graph.Node> parents=getDifferentParents();
		if (parents!=null) {
			branched=parents.size()>1;
			for(edu.usc.ict.nl.util.graph.Node n:parents) {
				DialogueOperatorNode parent=(DialogueOperatorNode) n;
				parent.visitParentsFindEffects(set, list, branched);
			}
		}
		return set;
	}

	/*public double computeRewardInKB(DialogueKBInterface is) throws Exception {
	double v=0;
	for(DialogueOperatorEffect e:effects) {
		v+=e.evaluateGoalValueIn(is);
	}
	return v;
}*/
	public double execute(final DialogueAction action, EvalContext context,final Event sourceEvent) throws Exception {
		final RewardDM dm=action.getDM();
		List<DialogueOperatorEffect> effects = getEffects();
		DialogueKB is=(DialogueKB) context.getInformationState();
		
		double reward=0;
		
		if (effects!=null && !effects.isEmpty()) {
			//DialogueKBInterface localVars = (is.getParent()!=null)?is:null;
			//DialogueKBInterface permanentInfoState=(is.getParent()!=null)?is.getParent():is;

			final Logger logger=dm.getLogger();
			final DMEventsListenerInterface messageBus = dm.getMessageBus();
			final long sid=dm.getSessionID();
			for(final DialogueOperatorEffect eff:effects) {
				if (eff.isGoalAchievement()) {
					float r = eff.evaluateGoalValueIn(is);
					reward+=r;
				} else if (eff.isSwapOut()) {
					doSwapOut(action,new SwapoutReason(Reason.EFFECT));
				} else if (eff.isInterrupt()) {
					logger.info("sending interrupt event.");
					dm.getTimerThread().schedule(new TimerTask() {
						@Override
						public void run() {
							dm.interruptCurrentlySpeakingAction(sourceEvent);
						}
					}, 1);
				} else if (eff.isSend()) {
					logger.info("sending internal event: "+eff.getSendEvent());
					dm.getTimerThread().schedule(new TimerTask() {
						@Override
						public void run() {
							dm.handleEvent(new NLUEvent(eff.getSendEvent(), sid));
						}
					}, 1);
				} else if (eff.isVHSend()) {
					logger.info("sending vh event: "+eff.getSendEvent());
					
					if (vhBridge==null) {
						NLBusConfig config = dm.getMessageBus().getConfiguration();
						if (config.hasVHConfig()) vhBridge=new VHBridge(config.getVhServer(), config.getVhTopic());
					}
					
					dm.getTimerThread().schedule(new TimerTask() {
						@Override
						public void run() {
							if (vhBridge!=null) {
								String[] parts=eff.getSendEvent().split("[\\s]+",2);
								vhBridge.sendMessage(parts[0], parts[1]);
							}
						}
					}, 1);
				} else {
					logger.info("saving IS update in node: "+toString(true)+": "+eff);
					Collection<VarChange> changes = is.saveAssignmentsAndGetUpdates(ACCESSTYPE.AUTO_OVERWRITEAUTO,true,eff);
					dm.sendVarChangeEventsCausedby(changes,sourceEvent);
				}
			}
		}
		
		return reward;
	}
	
	public static void doSwapOut(DialogueAction action, SwapoutReason reason) throws Exception {
		RewardDM dm=action.getDM();
		boolean isActionDaemon=action.getOperator().isDaemon();
		dm.getLogger().info("SWAPOUT: ("+(isActionDaemon?OpType.DAEMON:OpType.NORMAL)+" action)"+action.getOperator().getName()+", with active node(s): "+action.getActiveStates());
		dm.getDormantActions(isActionDaemon?OpType.DAEMON:OpType.NORMAL).addAction(action,reason);
		if (!isActionDaemon) dm.setActiveAction(null);
	}
	
	public static void main(String[] args) {
		String test="<state type=\"initial\">"+
		"<say speech-act=\"question.reason-to-visit\"/>"+
		"<effect expr=\"Assert(asked-reason-for-visit)\"/>"+
		"<transition condition=\"speech-act==answer.symptom.*\" trigger=\"true\" target=\"symptom\"/>"+
		"<transition condition=\"speech-act==answer.no\" target=\"no\"/>"+
		"</state>";
		System.out.println(new DialogueOperatorNode().parse(test));
	}
}
