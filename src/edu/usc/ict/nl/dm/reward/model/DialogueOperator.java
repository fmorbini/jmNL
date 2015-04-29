package edu.usc.ict.nl.dm.reward.model;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.usc.ict.nl.bus.events.NLUEvent;
import edu.usc.ict.nl.dm.reward.EventMatcher;
import edu.usc.ict.nl.dm.reward.model.DialogueAction.ActiveStates;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorNode.TYPE;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorNodeTransition.TransitionType;
import edu.usc.ict.nl.dm.reward.util.CompressedEffectList;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.kb.TrivialDialogueKB;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.QueueSet;
import edu.usc.ict.nl.util.Rational;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.XMLUtils;
import edu.usc.ict.nl.util.graph.Edge;
import edu.usc.ict.nl.util.graph.GraphElement;

public class DialogueOperator extends edu.usc.ict.nl.util.graph.Node {
	
	private LinkedHashSet<DialogueOperatorEntranceTransition> entranceTransitions,reentranceOptions;

	private Map<String,DialogueOperatorNode> states;
	private DialogueOperatorNode fakeStart;
	private List<DialogueOperatorTopic> topics;
	private HashSet<String> ignoredVars;

	HashMap<DialogueOperatorNode,List<DialogueOperatorNodesChain>> possibleEffectsSets=null;
	private HashMap<DialogueOperatorEntranceTransition, Set<DialogueKBFormula>> setOfModifiedVariables=null;

	private EventMatcher<List<DialogueOperatorEntranceTransition>> eventMatcher;
	private boolean isUserTriggerable=false,isSystemTriggerable=false,isReEntrable=false;

	private Map<String,DialogueKBFormula> localVars;

	public Map<String,DialogueKBFormula> getLocalVars() {return localVars;}

	public DialogueOperator() {
		eventMatcher=new EventMatcher<List<DialogueOperatorEntranceTransition>>();
	}
	public DialogueOperator(String id) {
		this();
		name=id;
		/*try {
			singleRunEffect=new DialogueOperatorEffect(DialogueKBFormula.create(getName(),null));
		} catch (Exception e) {
			e.printStackTrace();
			singleRunEffect=null;
		}*/
	}
	
	private boolean finalOperator=false;
	public void setFinal(boolean f) {this.finalOperator=f;}
	public boolean isFinal() {return this.finalOperator;}

	private boolean daemonOperator=false;
	public void setDaemon(boolean f) {this.daemonOperator=f;}
	public boolean isDaemon() {return this.daemonOperator;}

	private class OperatorForgetInfo {
		Integer seconds;
		DialogueKBFormula condition;
		public OperatorForgetInfo(Integer s,DialogueKBFormula cnd) {
			this.seconds=s;
			this.condition=cnd;
		}
		public DialogueKBFormula getCondition() {
			return condition;
		};
		public int getSeconds() {
			return seconds;
		}
	}
	
	private List<OperatorForgetInfo> forgets=null;
	private DialogueKBFormula turnTakingCnd=null;

	private void addForget(Integer time,DialogueKBFormula cnd) {
		if (time!=null) {
			if (forgets==null) forgets=new ArrayList<OperatorForgetInfo>();
			forgets.add(new OperatorForgetInfo(time, cnd));
		}
	}
	public boolean shouldItBeForgotten(long deltaTime, DialogueKBInterface is) {
		if (forgets!=null && !forgets.isEmpty()) {
			for (OperatorForgetInfo f:forgets) {
				Integer forgetTime=f.getSeconds();
				DialogueKBFormula forgetCondition=f.getCondition();
				if (forgetTime!=null && forgetTime>=0 && forgetTime<=(deltaTime/1000)) {
					if (forgetCondition!=null) {
						try {
							Boolean result = (Boolean)is.evaluate(forgetCondition,null);
							return (result!=null && result);
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else return true;
				}
			}
		}
		return false;
	}

	private void addTurnTakingCnd(DialogueKBFormula cnd) {
		if (cnd!=null) {
			turnTakingCnd=cnd;
		}
	}
	public DialogueKBFormula getTurnTakingCnd() {return turnTakingCnd;}
	public boolean canItBeExecutedNow(DialogueKBInterface is) {
		DialogueKBFormula cnd=getTurnTakingCnd();
		if (cnd!=null) {
			try {
				Boolean result = (Boolean)is.evaluate(cnd,null);
				return (result!=null && result);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return true;
	}
	
	public List<DialogueOperatorNodesChain> getEffectsSetsForStartingState(DialogueOperatorNode start) {
		if (possibleEffectsSets!=null) return possibleEffectsSets.get(start);
		else return null;
	}
	public HashMap<DialogueOperatorEntranceTransition,Set<DialogueKBFormula>> getSetOfModifiedVariables() {
		return setOfModifiedVariables;
	}

	public DialogueOperator parse(String o) {
		return parse(o, this);
	}
	public DialogueOperator parse(String o,DialogueOperator op) {
		Document doc;
		try {
			doc = XMLUtils.parseXMLString(o, false, false);
			//System.out.println(XMLUtils.prettyPrintDom(doc, " ", true, true));
			Node rootNode = doc.getDocumentElement();
			if (isOperatorNode(rootNode)) return parseOperator(rootNode,op);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public DialogueOperator parseOperator(Node n) throws Exception {
		return parseOperator(n, this);
	}
	public DialogueOperator parseOperator(Node n,DialogueOperator o) throws Exception {
		try {
			String id=null;
			assert(isOperatorNode(n));
			NamedNodeMap att = n.getAttributes();
			if ((id=getOperatorName(att))!=null) {
				o.setName(id);
				o.setFinal(isOperatorFinal(att));
				o.setDaemon(isOperatorDaemon(att));
				NodeList cs = n.getChildNodes();
				for (int i = 0; i < cs.getLength(); i++) {
					Node c = cs.item(i);
					NamedNodeMap childAtt = c.getAttributes();
					if (isEntranceConditionNode(c)) o.addEntranceConditionToOperator(getEntranceCondition(c,childAtt));
					else if (isVarNode(c)) o.addLocalVarToOperator(c,childAtt);
					else if (DialogueOperatorNode.isStateNode(c)) o.addStateToOperator(DialogueOperatorNode.parseState(c,childAtt,o));
					else if (DialogueOperatorTopic.isTopicNode(c)) o.addTopicToOperator(DialogueOperatorTopic.getTopicName(childAtt));
					else if (isForgetNode(c)) o.addForget(getForgetTime(childAtt),getForgetCnd(childAtt));
					else if (isTurnTakingCndNode(c)) o.addTurnTakingCnd(getTurnTakingCnd(childAtt));
					else if (isIgnoreVarNode(c)) o.addIgnoreVariable(getIgnoredVarName(childAtt,o));
				}
				//System.out.println(XMLUtils.prettyPrintXMLString(o.toString(false)," ",true));
				o.checkOperator();
				return o;
			} else return null;
		} catch (Exception e) {
			throw new Exception(e.getMessage()+" in operator: "+getName());
		}
	}

	private void addIgnoreVariable(String vName) {
		vName=StringUtils.cleanupSpaces(vName);
		if (StringUtils.isEmptyString(vName)) logger.info("ignore var declaration in operator "+getName()+" with empty var.");
		else {
			vName=vName.toLowerCase();
			if (ignoredVars==null) ignoredVars=new HashSet<String>();
			ignoredVars.add(vName);
		}
	}
	protected void addTopicToOperator(String topicName) {
		topicName=StringUtils.cleanupSpaces(topicName);
		if (StringUtils.isEmptyString(topicName)) logger.error("topic declaration in operator "+getName()+" with empty topic.");
		else {
			if (topics==null) topics=new ArrayList<DialogueOperatorTopic>();
			topics.add(new DialogueOperatorTopic(topicName));
		}
	}
	public List<DialogueOperatorTopic> getTopics() {return topics;}
	private void setTopics(List<DialogueOperatorTopic> newTopics) {topics=newTopics;}
	
	public HashSet<String> getIgnoredVars() {return ignoredVars;}

	private void addLocalVarToOperator(Node v,NamedNodeMap att) throws Exception {
		Node nodeID = att.getNamedItem(XMLConstants.IDID);
		Node nodeValue=att.getNamedItem(XMLConstants.VALUEID);
		if (nodeID==null || nodeValue==null) throw new Exception("Invalid variable node '"+XMLUtils.domNode2String(v, true,false));
		else {
			if (localVars==null) localVars=new HashMap<String, DialogueKBFormula>();
			localVars.put(nodeID.getNodeValue(), DialogueKBFormula.parse(nodeValue.getNodeValue()));
		}
	}

	private void addEntranceConditionToOperator(DialogueOperatorEntranceTransition ec) {
		if (ec!=null) {
			Set<DialogueOperatorEntranceTransition> list;
			if (ec.isReEntrable()) {
				if (reentranceOptions==null) reentranceOptions=new LinkedHashSet<DialogueOperatorEntranceTransition>();
				list=reentranceOptions;
				
				isReEntrable=true;
			} else {
				if (entranceTransitions==null) entranceTransitions=new LinkedHashSet<DialogueOperatorEntranceTransition>();
				list=entranceTransitions;
				
				if (ec.isCurrentUserInitiatable() || ec.isPendingUserInitiatable()) isUserTriggerable=true;
				else isSystemTriggerable=true;
			}
			list.add(ec);
			ec.setOperator(this);
		}
	}
	public LinkedHashSet<DialogueOperatorEntranceTransition> getEntranceConditions() {return entranceTransitions;}
	public LinkedHashSet<DialogueOperatorEntranceTransition> getReEntranceOptions() {return reentranceOptions;}
	public HashSet<DialogueOperatorEntranceTransition> getAllEntrancePossibilities() {
		LinkedHashSet<DialogueOperatorEntranceTransition> eecs = getEntranceConditions();
		LinkedHashSet<DialogueOperatorEntranceTransition> recs = getReEntranceOptions();
		HashSet<DialogueOperatorEntranceTransition> ecs=null;
		if (eecs!=null && !eecs.isEmpty()) ecs=new HashSet<DialogueOperatorEntranceTransition>(eecs);
		if (recs!=null && !recs.isEmpty()) {
			if (ecs==null) ecs=new HashSet<DialogueOperatorEntranceTransition>(recs);
			else ecs.addAll(recs); 
		}
		return ecs;
	}
	
	public List<DialogueOperatorNodesChain> traverseOperatorAndCollectEffects(GraphElement start) throws Exception {
		logger.debug("##############################Considering Operator: "+getName()+" starting point: "+start);
		assert((start instanceof DialogueOperatorNode) || (start instanceof DialogueOperatorNodeTransition));
		start.setWeightsStartingHere();
		QueueSet<DialogueOperatorNode> stack=new QueueSet<DialogueOperatorNode>();
		HashMap<String,DialogueOperatorNodesChain> result=new HashMap<String, DialogueOperatorNodesChain>();
		HashMap<DialogueOperatorNodeTransition,Set<Entry<String,Rational>>> results4Edge=new HashMap<DialogueOperatorNodeTransition, Set<Entry<String,Rational>>>();
		// initialize stuff:
		DialogueOperatorNodesChain initChain=new DialogueOperatorNodesChain(this);
		String initChainID=initChain.getChainID();
		result.put(initChainID, initChain);
		DialogueOperatorNode firstStackNode;
		if (start instanceof edu.usc.ict.nl.util.graph.Node) firstStackNode=(DialogueOperatorNode) start;
		else firstStackNode = (DialogueOperatorNode) ((Edge) start).getTarget();
		List<Edge> ins = firstStackNode.getIncomingEdges();
		if (ins!=null) for(Edge in:ins) results4Edge.put((DialogueOperatorNodeTransition) in, new HashSet<Entry<String,Rational>>());
		// the same for re-entrance options
		LinkedHashSet<DialogueOperatorEntranceTransition> rops = getReEntranceOptions();
		if (rops!=null) for(Edge in:rops) results4Edge.put((DialogueOperatorNodeTransition) in, new HashSet<Entry<String,Rational>>());
		
		stack.add(firstStackNode);
		
		while(!stack.isEmpty()) {
			DialogueOperatorNode node=stack.poll();
			if (logger.isDebugEnabled()) logger.debug("===============> Considering node: "+node);
			ins = node.getIncomingEdges();
			// two cases: all incoming edges are in results4Edge or not. If not do nothing.
			if (allIncomingEdgesAreInMap(ins, results4Edge)) {
				if (ins != null && logger.isDebugEnabled()) {
					logger.debug("All incoming edges set: do merging.");
					for(Edge in:ins) {
						logger.debug("edge: "+((DialogueOperatorNodeTransition)in).toString(true));
						Set<Entry<String, Rational>> inputEdges=results4Edge.get(in);
						for(Entry<String, Rational> inE:inputEdges) {
							logger.debug("    "+inE.getValue()+": "+result.get(inE.getKey()));
						}
					}
				}
				// if more than one incoming edge, do merging.
				HashMap<String, Rational> outgoingIDs=mergeIncomingChains(ins,results4Edge);
				// if empty, initialize with the empty chain and the weight of the node.
				if (outgoingIDs.isEmpty()) outgoingIDs.put(initChainID, node.getRationalWeight());
				if (ins != null && logger.isDebugEnabled()) {
					logger.debug("Merging result:");
					for(Edge in:ins) {
						logger.debug("edge: "+((DialogueOperatorNodeTransition)in).toString(true));
						Set<Entry<String, Rational>> inputEdges=results4Edge.get(in);
						for(Entry<String, Rational> inE:inputEdges) {
							logger.debug("    "+inE.getValue()+": "+result.get(inE.getKey()));
						}
					}
				}

				// cleanup
				if (ins != null)
					for(Edge in:ins) results4Edge.remove(in);

				Set<Entry<String, Rational>> newOutgoingIDs = getOutgoingChainsForNode(outgoingIDs,node,node.getEffects(),result);

				// storing results:
				List<Edge> outs=node.getOutgoingEdges();
				if (outs!=null) {
					// divide the weight of the outgoing list by size and associate it to all outgoing edges.
					int size=outs.size();
					if (size>1) for(Entry<String,Rational> e:newOutgoingIDs) e.setValue(e.getValue().divideBy(size)); 
					for(Edge e:outs) {
						DialogueOperatorNodeTransition out=(DialogueOperatorNodeTransition) e;
						assert(!results4Edge.containsKey(out));
						
						List<List<DialogueOperatorEffect>> possibleEffects = out.getPossibleEffects();
						if ((possibleEffects!=null) && !possibleEffects.isEmpty()) {
							if (logger.isDebugEnabled()) logger.debug(";;;;;;; current edge has possible effects.");
							// handle possible effects associated to this edges.
							HashMap<String, Rational> inputIDs=convertChainForms(newOutgoingIDs);
							int numEdges=possibleEffects.size();
							if (numEdges>1) for(String id:inputIDs.keySet()) inputIDs.put(id, inputIDs.get(id).divideBy(numEdges)); 
							HashMap<String, Rational> finalResult=new HashMap<String, Rational>();
							for(List<DialogueOperatorEffect> effects:possibleEffects) {
								Set<Entry<String, Rational>> tmpResult = getOutgoingChainsForNode(inputIDs,out,effects,result);
								mergeThisIntoThis(tmpResult,finalResult);
								if (logger.isDebugEnabled()) {
									logger.debug(">>> TMP merging:");
									for(String id:finalResult.keySet()) {
										logger.debug(" "+finalResult.get(id)+" "+result.get(id).getCompressedEffects().getCompressedEffects());
									}
								}
							}
							if (logger.isDebugEnabled()) logger.debug(";;;;;;; finished handling possible effects for edge.");
							results4Edge.put((DialogueOperatorNodeTransition) out, finalResult.entrySet());
						} else {
							results4Edge.put((DialogueOperatorNodeTransition) out, newOutgoingIDs);
						}
						
						stack.add((DialogueOperatorNode) out.getTarget());
					}
				}
			}
		}
		
		if (logger.isDebugEnabled()) {
			logger.debug(">>>FINAL results:");
			for(DialogueOperatorNodesChain e:result.values()) {
				logger.debug(e.getCompressedEffects().getCompressedEffects());
			}
			Rational s=Rational.zero;
			for (DialogueOperatorNodesChain c:result.values()) s=s.plus(c.getRationalWeight());
			if (s.compareTo(Rational.one)!=0) throw new Exception("sum should be 1: "+s);
		}
		
		return new ArrayList<DialogueOperatorNodesChain>(result.values());
	}
	private HashMap<String, Rational> convertChainForms(Set<Entry<String, Rational>> input) {
		HashMap<String, Rational> result=new HashMap<String, Rational>();
		if (input!=null) {
			for(Entry<String,Rational> e:input) {
				result.put(e.getKey(), e.getValue());
			}
		}
		return result;
	}

	private HashMap<String, Rational> mergeIncomingChains(List<Edge> edgesToMerge,
			HashMap<DialogueOperatorNodeTransition, Set<Entry<String, Rational>>> chains4edges) throws Exception {
		HashMap<String, Rational> result = new HashMap<String, Rational>();
		if (edgesToMerge!=null) {
			for(Edge in:edgesToMerge) {
				Set<Entry<String, Rational>> ids4Edge=chains4edges.get(in);
				mergeThisIntoThis(ids4Edge,result);
			}
		}
		return result;
	}

	private void mergeThisIntoThis(Set<Entry<String, Rational>> toBeMerged,HashMap<String, Rational> result) throws Exception {
		if (toBeMerged!=null) {
			for (Entry<String, Rational> id:toBeMerged) {
				String idName=id.getKey();
				Rational idWeight=id.getValue();
				Rational presentWeight=result.get(idName);
				if (presentWeight==null) result.put(idName, idWeight);
				else result.put(idName, presentWeight.plus(idWeight));
			}
		}
	}

	private boolean allIncomingEdgesAreInMap(List<Edge> ins,HashMap<DialogueOperatorNodeTransition, Set<Entry<String, Rational>>> map) {
		if (ins!=null) {
			for(Edge in:ins) {
				Rational r=in.getRationalWeight();
				if (r!=Rational.zero) {
					assert(r.getResult()>0);
					if (!map.containsKey(in)) return false;
				}
			}
			return true;
		} else return true;
	}
	
	private Set<Entry<String, Rational>> getOutgoingChainsForNode(HashMap<String, Rational> inputIDs, GraphElement ge, List<DialogueOperatorEffect> effects, HashMap<String,
			DialogueOperatorNodesChain> result) throws Exception {
		HashMap<String,Rational> outgoingIDsMap=new HashMap<String, Rational>();
		Set<Entry<String,Rational>> outgoingIDs=new HashSet<Entry<String,Rational>>();

		// if has effects, updates result table and results for outgoing edges.
		if (effects!=null && !effects.isEmpty()) {
			if (logger.isDebugEnabled()) {
				logger.debug("++++++ with effects:"+effects);
				logger.debug("Input chains:");
				for(String id:inputIDs.keySet()) {
					logger.debug(" "+inputIDs.get(id)+" "+result.get(id).getCompressedEffects().getCompressedEffects());
				}
				logger.debug("BEFORE results:");
				for(DialogueOperatorNodesChain e:result.values()) {
					logger.debug(e.getRationalWeight()+": "+e.getCompressedEffects().getCompressedEffects());
				}
			}
			/* for every chain, c, in the merged list calculated above:
			 *  produce the new chain ID by concatenating to c node.
			 *   if the new ID exists in result, increment the weight of the associated chain by the weight stored in outgoingIDs for ID.
			 *   subtract to the weight associated to the old ID (in result) by the weight stored in outgoingIDs for ID.
			 *    if something reaches 0, remove it from result
			 *   put ID in the final outgoing list with weight the same as the input.
			 */
			for(String outID:inputIDs.keySet()) {
				DialogueOperatorNodesChain oldChain=result.get(outID);
				Rational thisChainWeight=inputIDs.get(outID);
				assert(oldChain!=null);

				String newOutID=oldChain.updateChainIDWithThis(effects);
				DialogueOperatorNodesChain chain=result.get(newOutID);

				if (chain!=null) {
					chain.setWeight(chain.getRationalWeight().plus(thisChainWeight));
				} else {
					chain=oldChain.clone();
					chain.addLinkToChain(ge,Rational.one,effects);
					chain.setWeight(thisChainWeight);
					result.put(newOutID, chain);
					assert(chain.getChainID().equals(newOutID));
				}

				oldChain.setWeight(oldChain.getRationalWeight().minus(thisChainWeight));
				if (oldChain.getWeight()==0) result.remove(outID);

				Rational existingWeight=outgoingIDsMap.get(newOutID);
				if (existingWeight!=null) {
					outgoingIDsMap.put(newOutID, existingWeight.plus(thisChainWeight));
				} else {
					outgoingIDsMap.put(newOutID, thisChainWeight);
				}
			}
			outgoingIDs=outgoingIDsMap.entrySet();
		} else {
			outgoingIDs.addAll(inputIDs.entrySet());
		}

		if (logger.isDebugEnabled()) {
			logger.debug("outgoing chains:");
			for(Entry<String,Rational> e:outgoingIDs) {
				logger.debug(" "+e.getValue()+" "+result.get(e.getKey()).getCompressedEffects().getCompressedEffects());
			}
			logger.debug("AFTER results:");
			for(DialogueOperatorNodesChain e:result.values()) {
				logger.debug(e.getRationalWeight()+": "+e.getCompressedEffects().getCompressedEffects());
			}
		}
		return outgoingIDs;
	}
	
	/*
	public List<DialogueOperatorNodesChain> traverseOperatorFromTransitionAndCollectEffects(DialogueOperatorNodeTransition tr) throws Exception {
		tr.setWeightsStartingHere();
		List<DialogueOperatorNodesChain> result = new ArrayList<DialogueOperatorNodesChain>();
		DialogueOperatorNodesChain currentChain=new DialogueOperatorNodesChain(this);
		traverseOperatorFromTransitionAndCollectEffects(tr,result,currentChain);
		result=filterChainsWithEffects(result);
		return result;
	}
	private List<DialogueOperatorNodesChain> traverseOperatorFromTransitionAndCollectEffects(
			DialogueOperatorNodeTransition tr,
			List<DialogueOperatorNodesChain> result,
			DialogueOperatorNodesChain currentChain) {
		// add the node if it has effects
		List<List<DialogueOperatorEffect>> possibleEffects = tr.getPossibleEffects();
		if ((possibleEffects!=null) && !possibleEffects.isEmpty()) {
			DialogueOperatorNodesChain originalChain=currentChain;
			int numEdges=possibleEffects.size();
			currentChain.setWeight(currentChain.getWeight()/numEdges);
			for(List<DialogueOperatorEffect> effects:possibleEffects) {
				if (!effects.isEmpty()) {
					currentChain=originalChain.clone();
					currentChain.addLinkToChain(tr,Rational.one,effects);
				}
				traverseOperatorFromNodeAndCollectEffects((DialogueOperatorNode) tr.getTarget(),result,currentChain,false);
			}
		} else {
			traverseOperatorFromNodeAndCollectEffects((DialogueOperatorNode) tr.getTarget(),result,currentChain,false);
		}
		return result;
	}
	public List<DialogueOperatorNodesChain> traverseOperatorFromNodeAndCollectEffects(DialogueOperatorNode node,boolean skipFirstNode) throws Exception {
		node.setWeightsStartingHere();
		List<DialogueOperatorNodesChain> result = new ArrayList<DialogueOperatorNodesChain>();
		DialogueOperatorNodesChain currentChain=new DialogueOperatorNodesChain(this);
		traverseOperatorFromNodeAndCollectEffects(node,result,currentChain,skipFirstNode);
		result=filterChainsWithEffects(result);
		return result;
	}
	private List<DialogueOperatorNodesChain> traverseOperatorFromNodeAndCollectEffects(
			DialogueOperatorNode node,
			List<DialogueOperatorNodesChain> result,
			DialogueOperatorNodesChain currentChain,
			boolean skipNode) {
		// add the node if it has effects
		List<DialogueOperatorEffect> effects = node.getEffects();
		boolean hasEffects=(effects!=null) && !effects.isEmpty() && !skipNode;
		List<Edge> edges = node.getOutgoingEdges();
		if (edges!=null && !edges.isEmpty()) {
			int numEdges=edges.size();
			boolean duplicate=numEdges>1 || hasEffects;
			DialogueOperatorNodesChain originalChain = currentChain;
			for(Edge e:edges) {
				if (duplicate) currentChain=originalChain.clone();
				if (hasEffects) currentChain.addLinkToChain(node,Rational.one,effects);
				currentChain.setWeight(currentChain.getWeight()/numEdges);
				
				traverseOperatorFromTransitionAndCollectEffects((DialogueOperatorNodeTransition) e, result, currentChain);
			}
		} else {
			if (hasEffects) currentChain.addLinkToChain(node,Rational.one,effects);
			currentChain.setWeight(Math.min(node.getWeight(), currentChain.getWeight()));
			if (!currentChain.isEmpty()) result.add(currentChain);
		}
		return result;
	}
	
	private String computeEffectsID(LinkedHashSet<DialogueOperatorEffect> effects) {
		String id="";
		if ((effects!=null) && !effects.isEmpty()) for(DialogueOperatorEffect e:effects) id+=e.getID();
		return id;
	}*/
	public List<DialogueOperatorNodesChain> filterChainsWithEffects(List<DialogueOperatorNodesChain> result) throws Exception {
		float precision=0.0001f;
		normalizeProbabilitiesInPossiblePaths(result);
		float combinedTotalWeight=0;
		HashMap<String,DialogueOperatorNodesChain> ceffectsIDs=new HashMap<String, DialogueOperatorNodesChain>();
		List<DialogueOperatorNodesChain> differentLists=new ArrayList<DialogueOperatorNodesChain>();
		for(DialogueOperatorNodesChain chain:result) {
			combinedTotalWeight+=chain.getWeight();
			String id=chain.getChainID();
			DialogueOperatorNodesChain chainForID = ceffectsIDs.get(id);
			if (chainForID==null) {
				ceffectsIDs.put(id,chain);
				differentLists.add(chain);
			} else {
				chainForID.setWeight(chainForID.getWeight()+chain.getWeight());
			}
		}
		if (Math.abs(combinedTotalWeight-1)>precision) {
			if (combinedTotalWeight>1) throw new Exception("error, prob greater than 1.");
			else {
				DialogueOperatorNodesChain elseBranch = new DialogueOperatorNodesChain(this);
				elseBranch.setWeight(1f-combinedTotalWeight);
				differentLists.add(elseBranch);
			}
		}
		return differentLists;
	}

	private void normalizeProbabilitiesInPossiblePaths(List<DialogueOperatorNodesChain> paths) throws Exception {
		float totalLessThan1=0;
		DialogueOperatorNodesChain unNormalizedChain=null;
		if (paths!=null && !paths.isEmpty()) {
			for(DialogueOperatorNodesChain p:paths) {
				if (p.getWeight()==1) {
					if (unNormalizedChain!=null) throw new Exception("more than 1 unnormalizaed chain.");
					else unNormalizedChain=p;
				} else {
					totalLessThan1+=p.getWeight();
				}
			}
			if (totalLessThan1>=1 && unNormalizedChain!=null) throw new Exception("less than 1 greater than 1.");
			else if (unNormalizedChain!=null) unNormalizedChain.setWeight(1f-totalLessThan1);
		}
	}

	public boolean hasNoPathsWithEffects(Set<List<Object>> paths) {
		if (paths==null || paths.isEmpty()) return true;
		else {
			for(List<Object> chain:paths) if(!chain.isEmpty()) return false;
		}
		return true;
	}
	
	public void postProcessOperator(EventMatcher<List<DialogueOperatorEffect>> eventMatcher, RewardPolicy dp) throws Exception {
		logger.debug("post processing operator: "+getName());
		updateListenTransitionsWithEventBasedUpdates(eventMatcher);
		// for every entrance condition
		//  traverse the nodes breadth first
		//   collects the nodes with effects and the listen transitions, stop a chain when a final node is reached
		if (getEntranceConditions()!=null) {
			LinkedHashSet<DialogueOperatorEntranceTransition> entranceConditions = getEntranceConditions();
			LinkedHashSet<DialogueOperatorEntranceTransition> entranceOptions = getReEntranceOptions();
			List<DialogueOperatorEntranceTransition> ecs=new ArrayList<DialogueOperatorEntranceTransition>();
			if (entranceConditions!=null) ecs.addAll(entranceConditions);
			if (entranceOptions!=null) ecs.addAll(entranceOptions);
			
			for(DialogueOperatorEntranceTransition ec:ecs) {
				DialogueOperatorNode startPoint = ec.getTarget();

				if (possibleEffectsSets==null) possibleEffectsSets=new HashMap<DialogueOperatorNode, List<DialogueOperatorNodesChain>>();
				if (setOfModifiedVariables==null) setOfModifiedVariables=new HashMap<DialogueOperatorEntranceTransition, Set<DialogueKBFormula>>();
				
				if (!possibleEffectsSets.containsKey(startPoint)) {
					List<DialogueOperatorNodesChain> chains_effects=traverseOperatorAndCollectEffects(ec);
					possibleEffectsSets.put(startPoint, chains_effects);
					setOfModifiedVariables.put(ec,extractSetOfVariablesModifiedBy(chains_effects));

					if (chains_effects.size()>20) {
						HashSet<String> usedNames=new HashSet<String>();
						for(DialogueOperatorNodesChain chain:chains_effects) {
							List<DialogueOperatorEffect> effs = chain.getCompressedEffects().getEffects();
							if (effs!=null) {
								for(DialogueOperatorEffect e:effs) {
									Set<DialogueKBFormula> used = e.extractAllNamesUsed();
									if (used!=null && !used.isEmpty()) {
										Collection<String>names=FunctionalLibrary.map(used, DialogueKBFormula.class.getMethod("getName"));
										usedNames.addAll(names);
									}
								}
							}
						}
						logger.warn("Number of possibilities ("+chains_effects.size()+"): "+ec);
						logger.warn(" var used: "+FunctionalLibrary.printCollection(usedNames, "", "", "\n           "));
						/*for(DialogueOperatorNodesChain c:chains_effects) {
							System.out.println("111111111111111111111111111");
							for(DialogueOperatorEffect e:c.getCompressedEffects().getEffects()) {
								System.out.println("  "+e+" "+e.getID());
							}
						}
						System.in.read();*/
					}
					else logger.info("Number of possibilities ("+chains_effects.size()+"): "+ec);
				}
				
				/*
				Set<List<Object>> chains = traverseOperatorFromNode(ec);
				if (!hasNoPathsWithEffects(chains)) {
					if (possibleEffectsSets==null) possibleEffectsSets=new HashMap<DialogueOperatorNode, HashSet<Pair<List<Object>, LinkedHashSet<DialogueOperatorEffect>>>>();
					HashSet<Pair<List<Object>, LinkedHashSet<DialogueOperatorEffect>>> effectsSetsForStartPoint = possibleEffectsSets.get(startPoint);
					if (effectsSetsForStartPoint==null) possibleEffectsSets.put(startPoint, effectsSetsForStartPoint=new HashSet<Pair<List<Object>,LinkedHashSet<DialogueOperatorEffect>>>());
					chains=filterListOfChains(chains);
					for(List<Object>chain:chains) {
						LinkedHashSet<DialogueOperatorEffect> effects = effectsForThisChain(chain);
						effectsSetsForStartPoint.add(new Pair<List<Object>, LinkedHashSet<DialogueOperatorEffect>>(chain, effects));
					}
				}
				*/
			}
		}
		/* associate to each final state the set of cumulative effects:
		*  cumulative effects is the set of effects that can be executed while reaching
		*  the given final state including the final state itself.
		*/
		/*
		if (getLeaves()!=null) {
			for(DialogueOperatorNode fs:getLeaves()) {
				Set<List<DialogueOperatorNode>> set = fs.visitParentsFindEffects();
				fs.setParentsWithEffects(set);
				numPossibleSetsOfEffects+=(set!=null)?set.size():0;
			}
		}*/
		processOperatorTopics(dp);
	}

	private void clearUnreachablePortions() {
		Stack<DialogueOperatorNode> nodes=new Stack<DialogueOperatorNode>();
		nodes.addAll(getStates().values());
		List<Edge> toBeRemoved=new ArrayList<Edge>();
		while(!nodes.isEmpty()) {
			DialogueOperatorNode node=nodes.pop();
			List<Edge> ins = node.getIncomingEdges();
			if (ins==null || ins.isEmpty()) {
				// delete all outgoing edges and add to the stack
				List<Edge> outs = node.getOutgoingEdges();
				if (outs!=null) {
					for (Edge e:outs) {
						DialogueOperatorNode target=(DialogueOperatorNode) e.getTarget();
						nodes.push(target);
					}
					node.clearOutgoingEdges();
				}
				removeStateFromOperator(node);
			} else {
				toBeRemoved.clear();
				for (Edge e:ins) {
					DialogueOperatorNodeTransition tr=(DialogueOperatorNodeTransition) e;
					DialogueKBFormula cnd=tr.getCondition();
					if (cnd!=null && cnd.isTrivialFalsity() && !tr.doesConsume()) toBeRemoved.add(e);
				}
				// remove all outgoing edges from a swap out node.
				if (node.isSwapOut()) {
					List<Edge> outs = node.getOutgoingEdges();
					if (outs!=null) {
						for (Edge e:outs) {
							toBeRemoved.add(e);
						}
					}
				}
				if (!toBeRemoved.isEmpty()) {
					for(Edge e:toBeRemoved) {
						edu.usc.ict.nl.util.graph.Node source=e.getSource();
						DialogueOperatorNode target=(DialogueOperatorNode) e.getTarget();
						source.removeThisOutgoingEdge(e);
						nodes.push(target);
					}
				}
			}
		}
	}
	
	protected void processOperatorTopics(RewardPolicy dp) throws Exception {
		// get root topic
		//  for each topic of the operator
		//   get the name and split.
		//    build the ancestry line from root to leaf
		//    update list of topics with nodes corresponding to the leaves only
		// set the topics list with the updated list
		DialogueOperatorTopic rt=dp.getRootTopic();
		if (StringUtils.isEmptyString(DialogueOperatorTopic.separator)) throw new Exception("empty topic separator.");

		if (getTopics()!=null) {
			List<DialogueOperatorTopic> newTopics=new ArrayList<DialogueOperatorTopic>();
			for(DialogueOperatorTopic t:getTopics()) {
				String remainder=t.getName();
				DialogueOperatorTopic source=rt;
				while(remainder!=null) {
					String portion;
					int pos=remainder.indexOf(DialogueOperatorTopic.separator);

					if (pos<0) portion=remainder;
					else if (pos==0) {
						remainder=remainder.substring(DialogueOperatorTopic.separator.length());
						continue;
					} else portion=remainder.substring(0,pos);
					String nodeName=portion;

					DialogueOperatorTopic target=(DialogueOperatorTopic) source.getChildNamed(nodeName);
					if (target==null) target=new DialogueOperatorTopic(nodeName);
					if (!source.hasEdgeTo(target)) source.addEdgeTo(target, true, true);
					source=target;

					if (remainder.length()<=(portion.length()+DialogueOperatorTopic.separator.length())) remainder=null;
					else remainder=remainder.substring(portion.length()+DialogueOperatorTopic.separator.length());
				}
				newTopics.add(source);
			}
			setTopics(newTopics);
		}
	}

	private Set<DialogueKBFormula> extractSetOfVariablesModifiedBy(List<DialogueOperatorNodesChain> chains_effects) {
		Set<DialogueKBFormula> ret=null;
		if (chains_effects!=null) {
			for(DialogueOperatorNodesChain chain:chains_effects) {
				List<DialogueOperatorEffect> effects = chain.getCompressedEffects().getEffects();
				if (effects!=null) {
					for(DialogueOperatorEffect e:effects) {
						DialogueKBFormula var = e.getAssignedVariable();
						if (var!=null) {
							if (ret==null) ret=new HashSet<DialogueKBFormula>();
							ret.add(var);
						}
					}
				}
			}
		}
		return ret;
	}

	private void updateListenTransitionsWithEventBasedUpdates(EventMatcher<List<DialogueOperatorEffect>> eventMatcher) throws Exception {
		// extract events from the matcher.
		// for each transition in the operator, get the event
		//  go through the events in the matcher and compare the two events (transition and amtcher)
		//   4 cases: equal, one strictly includes the other, non-empty intersection, disjoint
		//   equal: add {effect}
		//   transition strictly included in matcher: add {effect}
		//   matcher strictly included in transition: add {effect and empty set}
		//   non-empty intersection: add {effect and empty set}
		//   disjoint: add nothing
		//  compute all possible sets of effects after taking each transition (compulsory effects, optional effects)
		// attach effects to listen transitions.

		if (eventMatcher!=null) {
		
			HashMap<String, List<DialogueOperatorEffect>> events = eventMatcher.getAllMatchedEventsWithPayload();

			// do not consider timer event in computing the possible effects associated with a transition
			if (events!=null && !events.isEmpty()) {
				events=new HashMap<String, List<DialogueOperatorEffect>>(events);
				ArrayList<String> toBeRemoved=null;
				for(String evName:events.keySet()) {
					if (evName.equals("internal.timer")) {
						if (toBeRemoved==null) toBeRemoved=new ArrayList<String>();
						toBeRemoved.add(evName);
					}
				}
				if (toBeRemoved!=null) {
					for(String evName:toBeRemoved) events.remove(evName);
				}

				// for entrance conditions
				Set<DialogueOperatorEntranceTransition> ecs = getEntranceConditions();
				for (DialogueOperatorEntranceTransition ec:ecs) {
					List<List<DialogueOperatorEffect>> possibleEffects=new ArrayList<List<DialogueOperatorEffect>>();
					for(String matcherEvent:events.keySet()) {
						List<DialogueOperatorEffect> effects = events.get(matcherEvent);
						findHowToUpdatePossibleSetOfEffectsWith(possibleEffects,matcherEvent,effects,ec);
					}
					ec.setPossibleEffects(possibleEffects);
				}
				// for other transitions
				Collection<DialogueOperatorNode> states=getStates().values();
				for(DialogueOperatorNode state:states) {
					List<Edge> edges = state.getOutgoingEdges();
					if (edges!=null) {
						for(Edge e:edges) {
							DialogueOperatorNodeTransition tr=(DialogueOperatorNodeTransition)e;
							if (tr.isListenTransition()) {
								List<List<DialogueOperatorEffect>> possibleEffects=computePossibleEffectsForEvent(tr.getEvent(),events);

								/*List<List<DialogueOperatorEffect>> possibleEffects=new ArrayList<List<DialogueOperatorEffect>>();
						for(String matcherEvent:events.keySet()) {
							List<DialogueOperatorEffect> effects = events.get(matcherEvent);
							findHowToUpdatePossibleSetOfEffectsWith(possibleEffects,matcherEvent,effects,tr);
						}*/
								tr.setPossibleEffects(possibleEffects);
							}
						}
					}
				}
			}
		}
	}

	public List<DialogueOperatorEffect> filterEffectsWithIgnoredVariables(List<DialogueOperatorEffect> effects) {
		HashSet<String> ivs = getIgnoredVars();
		if (ivs!=null && effects!=null) {
			List<DialogueOperatorEffect> ret=new ArrayList<DialogueOperatorEffect>();
			for(DialogueOperatorEffect e:effects) {
				if (e.isAssignment()) {
					Object thing = e.getAssignedExpression();
					if (thing instanceof DialogueKBFormula) {
						String var=e.getAssignedVariable().getName();
						if (ivs.contains(var)) continue;
					}
				}
				ret.add(e);
			}
			return ret;
		}
		return effects;
	}
	
	private class EffectsSet {
		private CompressedEffectList effects;
		private List<String> events;
		
		public EffectsSet(EffectsSet es) {
			if (es.effects!=null) effects=new CompressedEffectList(es.effects.getEffects());
			if (es.events!=null) events=new ArrayList<String>(es.events);
		}
		public EffectsSet() {}
		public void addEffectsAndEvent(String event,List<DialogueOperatorEffect> effects) throws Exception {
			if (effects!=null) effects=filterEffectsWithIgnoredVariables(effects);
			if (effects!=null && !effects.isEmpty()) {
				if (this.effects==null) this.effects=new CompressedEffectList(effects);
				else this.effects.mergeTheseEffects(effects);
				
				if (this.events==null) this.events=new ArrayList<String>();
				this.events.add(event);
			}
		}
		public void addEvent(String event) {
			if (this.events==null) this.events=new ArrayList<String>();
			this.events.add(event);
		}
		@Override
		public String toString() {
			if (events!=null) {
				return events.toString();
			} else return super.toString();
		}
	}
	private enum EventREcomparison {Y,N,M};
	private EventREcomparison givenThisWillTheOtherHappen(String re1,String re2) {
		int result=EventMatcher.compare(re1, re2);
		switch (result) {
		case 0:
		case -1:
			return EventREcomparison.Y;
		case 2:
		case 1:
			return EventREcomparison.M;
		case -2:
			return EventREcomparison.N;
		}
		return null;
	}
	private EventREcomparison givenTheIntersectionOfThoseEventsWillThisHappen(String event,Collection<String> events) {
		for(String e:events) {
			EventREcomparison tmp = givenThisWillTheOtherHappen(e,event);
			if (tmp==EventREcomparison.N) return EventREcomparison.N;
			else if (tmp==EventREcomparison.Y) return EventREcomparison.Y;
		}
		return EventREcomparison.M;
	}
	private List<List<DialogueOperatorEffect>> computePossibleEffectsForEvent(String inputEvent, HashMap<String, List<DialogueOperatorEffect>> events) throws Exception {
		List<EffectsSet> toBeAdded=null;
		List<EffectsSet> ret=null;
		Set<String>stringEvents=events.keySet();
		for(String event:stringEvents) {
			EventREcomparison tmp = givenThisWillTheOtherHappen(inputEvent, event);
			if ((tmp==EventREcomparison.Y) || (tmp==EventREcomparison.M)) {
				if (ret==null) {
					ret=new ArrayList<EffectsSet>();
					if (tmp==EventREcomparison.Y) {
						EffectsSet es=new EffectsSet();
						es.addEffectsAndEvent(event, events.get(event));
						es.addEvent(inputEvent);
						ret.add(es);
					} else {
						EffectsSet es=new EffectsSet();
						es.addEffectsAndEvent(event, events.get(event));
						es.addEvent(inputEvent);
						ret.add(es);
						es=new EffectsSet();
						es.addEvent(inputEvent);
						ret.add(es);
					}
				} else {
					if (toBeAdded!=null) toBeAdded.clear();
					for(EffectsSet es:ret) {
						List<String> esEvents=es.events;
						tmp=givenTheIntersectionOfThoseEventsWillThisHappen(event, esEvents);
						if (tmp==EventREcomparison.N) continue;
						else if (tmp==EventREcomparison.Y) es.addEffectsAndEvent(event, events.get(event));
						else {
							EffectsSet newEs=new EffectsSet(es);
							newEs.addEffectsAndEvent(event, events.get(event));
							if (toBeAdded==null) toBeAdded=new ArrayList<DialogueOperator.EffectsSet>();
							toBeAdded.add(newEs);
						}
					}
					if (toBeAdded!=null && !toBeAdded.isEmpty()) {
						for(EffectsSet es:toBeAdded) ret.add(es);
					}
				}
			}
		}
		return filterSetsOfEffects(ret);
	}
	private List<List<DialogueOperatorEffect>> filterSetsOfEffects(List<EffectsSet> possibleEffects) {
		List<List<DialogueOperatorEffect>> ret=null;
		if (possibleEffects!=null) {
			HashSet<String> alreadySeen=new HashSet<String>();
			for(EffectsSet es:possibleEffects) {
				String id=(es.effects!=null)?es.effects.getID():null;
				if (!alreadySeen.contains(id) && id!=null) {
					alreadySeen.add(id);
					if (ret==null) ret=new ArrayList<List<DialogueOperatorEffect>>();
					ret.add(es.effects.getEffects());					
				}
			}
		}
		return ret;
	}


	private void findHowToUpdatePossibleSetOfEffectsWith(
			List<List<DialogueOperatorEffect>> possibleEffects,
			String matcherEvent, List<DialogueOperatorEffect> effects,
			DialogueOperatorNodeTransition tr) {
		if (effects!=null) effects=filterEffectsWithIgnoredVariables(effects);
		if (effects!=null && !effects.isEmpty()) {
			String transitionEvent=tr.getEvent();
			if(!StringUtils.isEmptyString(transitionEvent)) {
				int relation=EventMatcher.compare(transitionEvent, matcherEvent);
				switch (relation) {
				case 0:
				case -1:
					updatePossibleEffectsSetsWith(possibleEffects,effects,false);
					break;
				case 1:
				case 2:
					updatePossibleEffectsSetsWith(possibleEffects,effects,true);
					break;
				default:
					break;
				}
			}
		}
	}

	private void updatePossibleEffectsSetsWith(List<List<DialogueOperatorEffect>> possibleEffects,List<DialogueOperatorEffect> effs, boolean optional) {
		// if no possible effects, then add an empty set
		if (possibleEffects.isEmpty()) possibleEffects.add(new ArrayList<DialogueOperatorEffect>());
		if (optional) {
			List<List<DialogueOperatorEffect>> newPossibleEffects=null;
			for(List<DialogueOperatorEffect> peffects:possibleEffects) {
				List<DialogueOperatorEffect> newEffects=new ArrayList<DialogueOperatorEffect>(peffects);
				newEffects.addAll(effs);
				if (newPossibleEffects==null) newPossibleEffects=new ArrayList<List<DialogueOperatorEffect>>();
				newPossibleEffects.add(newEffects);
			}
			if (newPossibleEffects!=null) possibleEffects.addAll(newPossibleEffects);
		} else {
			for(List<DialogueOperatorEffect> effects:possibleEffects) {
				effects.addAll(effs);
			}
		}
	}

	public List<DialogueOperatorEffect> effectsForThisChain(List<Object> chain) throws Exception {
		List<DialogueOperatorEffect> result=null;
		if(chain!=null) {
			for(Object e:chain) {
				if (e instanceof DialogueOperatorNode) {
					DialogueOperatorNode n=(DialogueOperatorNode) e;
					List<DialogueOperatorEffect> effects = n.getEffects();
					if (effects!=null) {
						if (result==null) result=new ArrayList<DialogueOperatorEffect>();
						result.addAll(effects);
					}
				} else if (e instanceof DialogueOperatorNodeTransition) {
					DialogueOperatorNodeTransition n=(DialogueOperatorNodeTransition) e;
					List<List<DialogueOperatorEffect>> effects = n.getPossibleEffects();
					if (effects!=null) {
						if (effects.size()>1) throw new Exception("optional effects in transitions are not supported.");
						if (result==null) result=new ArrayList<DialogueOperatorEffect>();
						result.addAll(effects.get(0));
					}
				}
			}
		}
		return result;
	}

	private DialogueOperator checkOperator() throws Exception {
		clearUnreachablePortions();
		// no TMP states
		// set all leaf states as final.
		// at least one effect.
		boolean hasEffects=false;
		if (entranceTransitions==null) throw new Exception("Operator "+getName()+" has no entrance conditions.");
		if (reentranceOptions==null) logger.info("operator: "+getName()+" cannot be restarted.");
		else for(DialogueOperatorEntranceTransition ec:entranceTransitions) {
			if (ec.getOperator()==null) throw new Exception("Entrance condition "+ec+" is not associated to this operator: "+getName());
		}
		for(DialogueOperatorNode state:states.values()) {
			if (state.isLeaf() && !state.isFinal()) {
				state.setType(TYPE.FINAL);
				if (state.isFinal()) logger.info("Set leaf state '"+state.getName()+"' as FINAL.");
			}
			if (state.getType()==TYPE.TMP) throw new Exception("TMP state '"+state+"' in operator: "+getName());
			if (state.hasEffects()) {
				hasEffects=true;
			}
			if (state.getOutgoingEdges()!=null) {
				TransitionType type=null;
				List<Edge> oes = state.getOutgoingEdges();
				for(Edge e:oes) {
					DialogueOperatorNodeTransition tr=(DialogueOperatorNodeTransition)e;
					if (type==null) type=tr.getType();
					else if (type!=tr.getType()) throw new Exception("Error in state "+state.getName()+" in operator "+getName()+": mix between transitions of different types.");
					if (isDaemon() && (type==TransitionType.SYSTEM || type==TransitionType.USER)) {
						throw new Exception("Daemon operators cannot have system actions or wait for user actions. Problem in operator: "+getName());
					}
				}
				/*if (type==TransitionType.NOEVENT) {
					boolean found=false;
					for(Edge e:oes) {
						DialogueOperatorNodeTransition tr=(DialogueOperatorNodeTransition)e;
						if (tr.getCondition()==null) {
							found=true;
							break;
						}
					}
					if (!found) throw new Exception("Error in state "+state.getName()+" in operator "+getName()+": No ELSE edge.");
				}*/
			}
			if (state.hasEffects()) {
				boolean swapOutEncountered=false;
				for(DialogueOperatorEffect e:state.getEffects()) {
					if (e.isSwapOut()) swapOutEncountered=true;
					else if (swapOutEncountered) {
						if (isDaemon() && e.isGoalAchievement()) throw new Exception("Daemon operator cannot have a goal: "+getName());
						throw new Exception("Effect '"+e.toString(false)+"' happens after a swap out effect in state: "+state.getName()+" in operator "+getName()+".");
					}
				}
			}
		}
		if (!hasEffects) throw new Exception("No states have effects. At least one state needs to have effects. In operator: "+getName());
		// no cycles
		// no initial state
		// all states are reachable from the initial state and user triggerable states. 
		HashSet<DialogueOperatorNode> reachedStates=new HashSet<DialogueOperatorNode>();
		if (entranceTransitions!=null)
			for(DialogueOperatorEntranceTransition tr:entranceTransitions) {
				traverseStatesFrom(tr.getTarget(),reachedStates);
			}
		if (reentranceOptions!=null)
			for(DialogueOperatorEntranceTransition tr:reentranceOptions) {
				traverseStatesFrom(tr.getTarget(),reachedStates);
			}
		if (states!=null) {
			for(DialogueOperatorNode state:states.values())
				if (!reachedStates.contains(state)) throw new Exception("State '"+state.getName()+"' is not reachable from start or user triggerable states.");
		}
		
		if (getTopics()==null || getTopics().isEmpty()) {
			if (!isDaemon()) throw new Exception("Operator "+getName()+" has no associated topics.");
		} else {
			if (isDaemon()) throw new Exception("Daemon operators cannot have a topic. Error in operator: "+getName());
		}
		return this;
	}
	private void traverseStatesFrom(DialogueOperatorNode root, HashSet<DialogueOperatorNode> reachedStates) {
		if (root!=null) {
			Stack<DialogueOperatorNode> stack=new Stack<DialogueOperatorNode>();		
			stack.push(root);
			while(!stack.isEmpty()) {
				DialogueOperatorNode i=stack.pop();
				reachedStates.add(i);
				if (i.getOutgoingEdges()!=null) {
					for(Edge e:i.getOutgoingEdges()) {
						DialogueOperatorNode t=(DialogueOperatorNode) e.getTarget();
						if (!reachedStates.contains(t)) stack.push(t);
					}
				}
			}
		}
	}
	public void addStateToOperator(DialogueOperatorNode state) throws Exception {
		if (state!=null) {
			String stateID=state.getName();
			DialogueOperatorNode alreadyThere=getStateNamed(stateID);
			if ((alreadyThere!=null) && (alreadyThere!=state)) throw new Exception("Operator contains multiple states with same name: "+state);
			else {
				if (alreadyThere==null) {
					if (states==null) states=new HashMap<String, DialogueOperatorNode>();
					states.put(state.getName(),state);
				}
			}
		}
	}
	public void removeStateFromOperator(DialogueOperatorNode state) {
		if (state!=null) {
			if (states!=null) states.remove(state.getName());
		}
	}
	public DialogueOperatorNode getStateNamed(String stateName) {
		if (states!=null) return states.get(stateName);
		else return null;
	}
	public Map<String,DialogueOperatorNode> getStates() {return states;}

	public static boolean isOperatorNode(Node rootNode) {
		return (rootNode.getNodeType()==Node.ELEMENT_NODE) && rootNode.getNodeName().toLowerCase().equals(XMLConstants.OPERATORID);
	}
	public static boolean isOperatorsContainerNode(Node n) {
		return (n.getNodeType()==Node.ELEMENT_NODE) && n.getNodeName().toLowerCase().equals(XMLConstants.OPERATORCONTAINERID);
	}
	public static boolean isPreconditionNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.PRECONDITIONID);
	}
	public static boolean isEntranceConditionNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.ENTRANCECONDITIONID);
	}
	private static boolean isVarNode(Node n) {
		return (n.getNodeType()==Node.ELEMENT_NODE) && n.getNodeName().toLowerCase().equals(XMLConstants.VARID);
	}
	private static boolean isIgnoreVarNode(Node n) {
		return (n.getNodeType()==Node.ELEMENT_NODE) && n.getNodeName().toLowerCase().equals(XMLConstants.IGNOREID);
	}
	private static boolean isForgetNode(Node n) {
		return (n.getNodeType()==Node.ELEMENT_NODE) && n.getNodeName().toLowerCase().equals(XMLConstants.FORGETID);
	}
	private static boolean isTurnTakingCndNode(Node n) {
		return (n.getNodeType()==Node.ELEMENT_NODE) && n.getNodeName().toLowerCase().equals(XMLConstants.TTID);
	}
	
	private String getOperatorName(NamedNodeMap att) {
		Node nodeID = att.getNamedItem(XMLConstants.IDID);
		if (nodeID!=null) return StringUtils.cleanupSpaces(nodeID.getNodeValue());
		else return null;
	}
	private boolean isOperatorFinal(NamedNodeMap att) {
		Node nodeID = att.getNamedItem(XMLConstants.FINALID);
		if (nodeID!=null) {
			String id=StringUtils.cleanupSpaces(nodeID.getNodeValue()).toLowerCase();
			return (id.equals("true"));
		}
		else return false;
	}
	private boolean isOperatorDaemon(NamedNodeMap att) {
		Node nodeID = att.getNamedItem(XMLConstants.DAEMONID);
		if (nodeID!=null) {
			String id=StringUtils.cleanupSpaces(nodeID.getNodeValue()).toLowerCase();
			return (id.equals("true"));
		}
		else return false;
	}
	private Integer getOperatorForgetTime(NamedNodeMap att) {
		Node nodeID = att.getNamedItem(XMLConstants.FORGETID);
		if (nodeID!=null) {
			Integer time=Integer.parseInt(nodeID.getNodeValue());
			if (time!=null && time>=0) return time;
		}
		return null;
	}
	private DialogueKBFormula getOperatorForgetCondition(NamedNodeMap att) throws Exception {
		Node nodeID = att.getNamedItem(XMLConstants.FORGETCNDID);
		if (nodeID!=null) {
			return DialogueKBFormula.parse(nodeID.getNodeValue());
		}
		return null;
	}
	private DialogueOperatorEntranceTransition getEntranceCondition(Node n, NamedNodeMap att) {
		DialogueOperatorEntranceTransition ec = new DialogueOperatorEntranceTransition();
		try {
			if (fakeStart==null) {
				fakeStart=new DialogueOperatorNode();
				fakeStart.setType(TYPE.NORMAL);
				fakeStart.setName(getName());
			}
			return (DialogueOperatorEntranceTransition) ec.parseTransition(n, att, fakeStart, this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	private String getIgnoredVarName(NamedNodeMap att, DialogueOperator o) {
		Node nodeID = att.getNamedItem(XMLConstants.IDID);
		if (nodeID!=null) return StringUtils.cleanupSpaces(nodeID.getNodeValue());
		else return null;
	}
	private Integer getForgetTime(NamedNodeMap att) {
		Node nodeID = att.getNamedItem(XMLConstants.FORGETTIMEID);
		if (nodeID!=null) return Integer.parseInt(nodeID.getNodeValue());
		else return null;
	}
	private DialogueKBFormula getForgetCnd(NamedNodeMap att) throws DOMException, Exception {
		Node nodeID = att.getNamedItem(XMLConstants.FORGETCNDID);
		if (nodeID!=null) return DialogueKBFormula.parse(nodeID.getNodeValue());
		else return null;
	}

	private DialogueKBFormula getTurnTakingCnd(NamedNodeMap att) throws DOMException, Exception {
		Node nodeID = att.getNamedItem(XMLConstants.TTCNDID);
		if (nodeID!=null) return DialogueKBFormula.parse(nodeID.getNodeValue());
		else return null;
	}

	public boolean isUserTriggerable() {return isUserTriggerable;}
	public boolean isSystemTriggerable() {return isSystemTriggerable;}
	public boolean isReEntrable() {return isReEntrable;}
	
	public Set<String> getAllUserTriggers() {
		LinkedHashSet<String> ret=null;
		if (isUserTriggerable()) {
			LinkedHashSet<DialogueOperatorEntranceTransition> ecs = getEntranceConditions();
			for(DialogueOperatorEntranceTransition ec:ecs) {
				String event=ec.getEvent();
				if (ret==null) ret=new LinkedHashSet<String>();
				ret.add(event);
			}
		}
		return ret;
	}

	@Override
	public String toString() {
		return toString(true);
	}
	public String toString(boolean shortForm) {
		if (shortForm) return getName();
		else {
			String ret="<"+XMLConstants.OPERATORID+" "+XMLConstants.IDID+"=\""+getName()+"\" "+
				XMLConstants.FINALID+"=\""+isFinal()+"\" "+
				XMLConstants.DAEMONID+"=\""+isDaemon()+"\">";
			if (getTopics()!=null) for(DialogueOperatorTopic t:getTopics()) ret+=t.toString(shortForm);
			if (forgets!=null && !forgets.isEmpty()) {
				for(OperatorForgetInfo f:forgets) {
					ret+="<"+XMLConstants.FORGETID+" "+XMLConstants.FORGETTIMEID+"=\""+f.getSeconds()+"\" "+
					((f.getCondition()!=null)?XMLConstants.FORGETCNDID+"=\""+f.getCondition().toString()+"\" ":" ")+
					"/>\n";
				}
			}
			if (turnTakingCnd!=null) ret+="<"+XMLConstants.TTID+" "+XMLConstants.TTCNDID+"=\""+turnTakingCnd.toString()+"\"/>\n";
			if (getIgnoredVars()!=null) for(String iv:getIgnoredVars()) ret+="<"+XMLConstants.IGNOREID+" "+XMLConstants.IDID+"=\""+iv+"\"/>";
			if (entranceTransitions!=null) for (DialogueOperatorEntranceTransition t:entranceTransitions) ret+=t.toString(shortForm);
			if (reentranceOptions!=null) for (DialogueOperatorEntranceTransition t:reentranceOptions) ret+=t.toString(shortForm);
			if (states!=null) for (DialogueOperatorNode state:states.values()) ret+=state.toString(shortForm);
			ret+="</"+XMLConstants.OPERATORID+">";
			return ret;
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof DialogueOperator) {
			if (name==null) return name==((DialogueOperator)obj).name;
			else return name.equals(((DialogueOperator)obj).name);
		} else return super.equals(obj);
	}

	public List<DialogueOperatorEntranceTransition> getUserTriggerableTransitionsForEvent(String evName) {
		if (eventMatcher==null) return null;
		else {
			Set<List<DialogueOperatorEntranceTransition>> matches = eventMatcher.match(evName);
			List<DialogueOperatorEntranceTransition> result=null;
			if (matches!=null && !matches.isEmpty()) {
				for(List<DialogueOperatorEntranceTransition> ecs:matches) {
					if (ecs!=null && !ecs.isEmpty()) {
						if (result==null) result=new ArrayList<DialogueOperatorEntranceTransition>();
						result.addAll(ecs);
					}
				}
			}
			return result;
		}
	}
	public EventMatcher<List<DialogueOperatorEntranceTransition>> getEventMatcher() {return eventMatcher;}
	public void setEventMatcher(EventMatcher<List<DialogueOperatorEntranceTransition>> eventMatcher) {this.eventMatcher = eventMatcher;}

	@Override
	public void toGDL(PrintWriter out) throws IOException {
		out.write("graph: { title: \""+getID()+"\" label: \""+this.toString()+(isFinal()?"(final)":"")+"\"\n");
		if(entranceTransitions!=null) {
			fakeStart.toGDL(out);
		}
		out.write("}\n");
	}
	
	@Override
	public edu.usc.ict.nl.util.graph.Node getRoot() {return fakeStart;}

	public List<List<String>> simulateAndCollectAllSystemActions(NLUEvent nluEvent,
			TrivialDialogueKB tmp,
			ActiveStates activeStates) throws Exception {
		EvalContext context=new EvalContext(tmp,this);
		String evName=(nluEvent!=null)?nluEvent.getName():null;
		List<List<String>> ret=new ArrayList<List<String>>();
		for(DialogueOperatorNode state:activeStates) {
			List<Edge> transitions=state.getOutgoingEdges();
			if (transitions!=null) {
				for(Edge e:transitions) {
					DialogueOperatorNodeTransition tr=(DialogueOperatorNodeTransition) e;
					boolean eventGoodForTransition=tr.isEventGoodForTransition(evName);
					if (eventGoodForTransition) {
						boolean conditionGoodForTransition=tr.isConditionSatisfiedInCurrentIS(context);
						if (conditionGoodForTransition) { 
							if (evName!=null) evName=null;
							simulateTakeTransition(tr,context,nluEvent,activeStates,ret,null);
							break;
						} else if (tr.doesConsume()) break;
					}
				}
			}
		}
		return ret;
	}

	private void simulateTakeTransition(DialogueOperatorNodeTransition tr,
			EvalContext context, NLUEvent nluEvent,ActiveStates activeStates,
			List<List<String>> ret,List<String> current) throws Exception {
		DialogueKB tmp = context.getInformationState();
		if (!tr.isListenTransition()) {
			DialogueOperatorNode startState = (DialogueOperatorNode) tr.getSource();
			DialogueOperatorNode endState = (DialogueOperatorNode) tr.getTarget();
			activeStates.transition(startState, endState);
			if (tr.isSayTransition()) {
				if (current==null) {
					current=new ArrayList<String>();
					ret.add(current);
				}
				current.add(tr.getSay());
			}
			// execute final state
			if (endState.getEffects()!=null) {
				for(final DialogueOperatorEffect eff:endState.getEffects()) {
					if (eff.isAssertion() || eff.isAssignment() || eff.isAssignmentList() || eff.isImplication()) {
						tmp.store(eff, ACCESSTYPE.AUTO_OVERWRITETHIS, false);
					}
				}
				tmp.runForwardInference(ACCESSTYPE.AUTO_OVERWRITETHIS);
			}

			if (!endState.isSwapOut()) {
				List<Edge> transitions = endState.getOutgoingEdges();
				if (transitions!=null && !transitions.isEmpty()) {
					DialogueOperatorNodeTransition first = (DialogueOperatorNodeTransition)transitions.get(0);
					if (first.isSayTransition()) {
						List<DialogueOperatorNodeTransition> possibilities=pickPossibleSayTransitions(transitions,context);
						int last=possibilities.size();
						for(DialogueOperatorNodeTransition trc:possibilities) {
							last--;
							if (last>0) {
								current=new ArrayList<String>(current);
								ret.add(current);
								activeStates=new DialogueAction().getActiveStates();
							}
							simulateTakeTransition(trc,new EvalContext(new TrivialDialogueKB(tmp)),nluEvent,activeStates,ret,current);
						}
					} else if (first.isNoEventTransition()) {
						// check and take the first one in order that is executable
						for(Edge e:transitions) {
							DialogueOperatorNodeTransition trc=(DialogueOperatorNodeTransition) e;
							if (trc.isExecutableInCurrentIS(null, context)) {
								simulateTakeTransition(trc,context,nluEvent,activeStates,ret,current);
								break;
							}
						}
					}
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		String test=" <network id=\"welcome\">"+
		"<ec event=\"current-user-speech-act(login)\" target=\"welcome-user\"/>"+
		"<state id=\"welcome-user\">"+
		"<say event=\"conventional-opening.generic\" target=\"response-welcome\"/>"+
		"<effect goal=\"polite\"/>"+
		"</state>"+
		"<state id=\"response-welcome\">"+
		"<var name=\"tmp\" value=\"timeSinceLastUserAction\"/>"+
		"<listen event=\"conventional-opening.generic\" condition=\"\" target=\"intro\"/>"+
		"<listen event=\"timer\" condition=\"-(timeSinceLastUserAction,tmp)>=3\" target=\"intro\"/>"+
		"</state>"+
		"<state id=\"intro\">"+
		"<say event=\"conventional-opening.instructions\" target=\"response-intro\"/>"+
		"</state>"+
		"<state id=\"response-intro\">"+
		"<var name=\"tmp\" value=\"timeSinceLastUserAction\"/>"+
		"<listen event=\"answer.yes\" target=\"end\"/>"+
		"<listen event=\"timer\" condition=\"-(timeSinceLastUserAction,tmp)>=3\" target=\"end\"/>"+
		"</state>"+
		"<state type=\"final\" id=\"end\"/>"+
		"</network>";
		DialogueOperator o = new DialogueOperator();
		DialogueOperator op = o.parse(test);
		System.out.println(XMLUtils.prettyPrintXMLString(op.toString(), " ", true));
		op.toGDL(new PrintWriter(System.out));
	}

	/**
	 *  scan all outgoing edges, keep only those with satisfied condition.
	 *  if at least one with satisfied condition, pick randomly among those (i.e. satisfied)
	 *  else pick randomly among all (because all are not satisfied).
	 *  execute picked.
	 * @param transitions
	 * @return
	 * @throws Exception 
	 */
	public List<DialogueOperatorNodeTransition> pickPossibleSayTransitions(List<Edge> transitions,EvalContext context) throws Exception {
		List<DialogueOperatorNodeTransition> goodSay=null;
		for(Edge e:transitions) {
			DialogueOperatorNodeTransition ctr=(DialogueOperatorNodeTransition) e;
			if (ctr.willSay(context)) {
				if (goodSay==null) goodSay=new ArrayList<DialogueOperatorNodeTransition>();
				goodSay.add(ctr);
			}
		}
		if (goodSay!=null && !goodSay.isEmpty()) return goodSay;
		else return (List)transitions;
	}
}
