package edu.usc.ict.nl.dm.reward.model;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.bus.modules.NLGInterface;
import edu.usc.ict.nl.bus.modules.NLUInterface;
import edu.usc.ict.nl.config.DMConfig;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.dm.reward.matcher.EventMatcher;
import edu.usc.ict.nl.dm.reward.model.macro.EventMacro;
import edu.usc.ict.nl.dm.reward.model.macro.FormulaMacro;
import edu.usc.ict.nl.dm.reward.model.macro.MacroRepository;
import edu.usc.ict.nl.dm.reward.model.textFormat.TextFormatGrammar;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.kb.TrivialDialogueKB;
import edu.usc.ict.nl.nlg.SpeechActWithProperties;
import edu.usc.ict.nl.util.FileUtils;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.XMLUtils;
import edu.usc.ict.nl.util.graph.Edge;
import edu.usc.ict.nl.util.graph.GraphElement;
import edu.usc.ict.nl.utils.LogConfig;

// a list of dialogue operators
public class RewardPolicy {
	
	public static final Logger logger = Logger.getLogger(RewardPolicy.class.getName());
	static {
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );	
	}

	private File policyDirectory=null;
	private String userModelFileName=null;//initKBFileName=null,goalsFileName=null,userModelFileName=null,isUpdatesFileName;
	
	private HashMap<String, Pair<DialogueKBFormula, String>> goals;
	private List<DialogueOperatorEffect> initIS;

	private float stepLoss=1;
	
	private HashMap<String,DialogueOperator> operators,daemonOperators;
	private EventMatcher<List<DialogueOperator>> eventMatcher4UserInitiativeOperators,eventMatcher4UserInitiativeDaemonOperators;
	private Set<DialogueOperator> systemInitiativeOperators,systemInitiativeDaemonOperators;

	private EventMatcher<List<DialogueOperatorEffect>> eventMatcher4isUpdates=null;

	private Map<String,Template> templates=null;
	
	// NL configuration
	private DMConfig config;
	private DMConfig getConfiguration() {return config;}
	private void setConfiguration(DMConfig c) {this.config=c;}

	public static enum OpType {NORMAL,DAEMON,ALL};
	public Collection<DialogueOperator> getOperators(OpType type){
		Collection<DialogueOperator> res=null;
		switch (type) {
		case NORMAL:
			if (operators!=null) res=operators.values();
			break;
		case DAEMON:
			if (daemonOperators!=null) res=daemonOperators.values();
			break;
		case ALL:
			if (operators!=null) res=new HashSet<DialogueOperator>(operators.values());
			if (daemonOperators!=null) {
				if (res!=null) res.addAll(operators.values());
				else res=new HashSet<DialogueOperator>(operators.values());
			}
			break;
		}
		return res;
	}
	public DialogueOperator getOperatorNamed(String name,OpType type) {
		DialogueOperator res=null;
		switch (type) {
		case NORMAL:
			if (operators!=null) res=operators.get(name);
		case DAEMON:
			if (daemonOperators!=null) res=daemonOperators.get(name);
		case ALL:
			if (operators!=null) res=operators.get(name);
			if (daemonOperators!=null && res==null) res=daemonOperators.get(name);
		}
		return res;
	}

	private DialogueOperatorTopic root;
	
	public RewardPolicy(DMConfig config) {
		setConfiguration(config);
	}
	
	public DialogueOperatorTopic getRootTopic() {
		if (root==null) root=new DialogueOperatorTopic("root");
		return root;
	}
	public DialogueOperatorTopic getFirstTopicForString(String topic) {
		DialogueOperatorTopic rt=getRootTopic();

		String remainder=topic;
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

			DialogueOperatorTopic target=(DialogueOperatorTopic) source.getDescendantNamed(nodeName);
			if (target==null) return null;
			source=target;

			if (remainder.length()<=(portion.length()+DialogueOperatorTopic.separator.length())) remainder=null;
			else remainder=remainder.substring(portion.length()+DialogueOperatorTopic.separator.length());
		}

		return (source==rt)?null:source;
	}
	public List<DialogueOperatorTopic> getAllTopicsForString(String topic) {
		DialogueOperatorTopic rt=getRootTopic();

		String remainder=topic;
		LinkedHashSet newTargets=new LinkedHashSet();
		LinkedHashSet<DialogueOperatorTopic> sources=new LinkedHashSet<DialogueOperatorTopic>();
		sources.add(rt);
		while(remainder!=null) {
			String portion;
			int pos=remainder.indexOf(DialogueOperatorTopic.separator);

			if (pos<0) portion=remainder;
			else if (pos==0) {
				remainder=remainder.substring(DialogueOperatorTopic.separator.length());
				continue;
			} else portion=remainder.substring(0,pos);
			String nodeName=portion;

			newTargets.clear();
			for(edu.usc.ict.nl.util.graph.Node source:sources) {
				List targets=source.getAllDescendantsNamed(nodeName);
				if ((targets!=null) && !targets.isEmpty()) newTargets.addAll(targets);
			}
			sources.clear();
			sources.addAll(newTargets);

			if (remainder.length()<=(portion.length()+DialogueOperatorTopic.separator.length())) remainder=null;
			else remainder=remainder.substring(portion.length()+DialogueOperatorTopic.separator.length());
		}

		return (sources.contains(rt) || (sources==null) || sources.isEmpty())?null:new ArrayList<DialogueOperatorTopic>(sources);
	}

	public Collection<DialogueOperatorEffect> getISinitialization() {return initIS;}
	public HashMap<String, Pair<DialogueKBFormula, String>> getGoals() {return goals;}

	public RewardPolicy parseDialoguePolicyFile(String fileName) throws Exception {
		RewardPolicy p=parseDialoguePolicyFile(fileName,this);
		return p;
	}
	public RewardPolicy parseDialoguePolicyFile(String fileName,RewardPolicy dp) throws Exception {
		MacroRepository.clear();
		File f=FileUtils.getFileFromStringInResources(fileName);
		Document doc = XMLUtils.parseXMLFile(f, true, true);
		Node rootNode = doc.getDocumentElement();
		List<DialogueOperator> operatorsToBeAdded=null;
		if (isDialoguePolicy(rootNode)) {
			dp.setPolicyDirectory(f.getParentFile());
			LinkedList<Node> q=new LinkedList<Node>();
			NodeList cs = rootNode.getChildNodes();
			for (int i = 0; i < cs.getLength(); i++) q.add(cs.item(i));
			while(!q.isEmpty()) {
				Node c=q.poll();
				NamedNodeMap childAtt = c.getAttributes();
				if (DialogueOperator.isOperatorsContainerNode(c)) {
					cs = c.getChildNodes();
					for (int i = 0; i < cs.getLength(); i++) q.add(cs.item(i));
				} else if (DialogueOperator.isOperatorNode(c)) {
					DialogueOperator o = DialogueOperator.parseOperator(c);
					if (o!=null) {
						if (operatorsToBeAdded==null) operatorsToBeAdded=new ArrayList<DialogueOperator>();
						operatorsToBeAdded.add(o);
					}
					else throw new Exception("parse of operator failed: "+XMLUtils.prettyPrintDom(c, " ", true, true));
				} else if (isInitsNode(c) || isMacrosNode(c) || isTemplatesNode(c) || isGoalsNode(c) || isISUpdatesNode(c)) {
					cs = c.getChildNodes();
					int addAt=0;
					for (int i = 0; i < cs.getLength(); i++) q.add(addAt++,cs.item(i));
				} else if (isInitNode(c)) {
					DialogueOperatorEffect eff=null;
					try {
						eff=DialogueOperatorEffect.parse(childAtt);
					} catch (Exception e) {e.printStackTrace();}
					if (eff!=null) {
						if (initIS==null) initIS=new ArrayList<DialogueOperatorEffect>();
						initIS.add(eff);
					} else throw new Exception("Problem with IS initialization expr: "+XMLUtils.prettyPrintDom(c, " ", true, true));
				} else if (isEventMacroNode(c)) {
					String left=getLeftNodeValue(childAtt);
					String right=getRightNodeValue(childAtt);
					if (!StringUtils.isEmptyString(left) && !StringUtils.isEmptyString(right)) {
						if (MacroRepository.containsMacro(left)) logger.warn("overwriting macro for: '"+left+"' with event macro '"+right+"'");
						MacroRepository.addMacro(left, new EventMacro(left,right));
					}
				} else if (isFormulatMacroNode(c)) {
					String left=getLeftNodeValue(childAtt);
					String right=getRightNodeValue(childAtt);
					try {
						DialogueKBFormula leftf=DialogueKBFormula.parse(left);
						DialogueKBFormula rightf=DialogueKBFormula.parse(right);
						if (leftf!=null && rightf!=null) {
							if (MacroRepository.containsMacro(leftf.getName())) logger.warn("overwriting macro for: '"+left+"' with formula macro '"+right+"'");
							MacroRepository.addMacro(leftf.getName(), new FormulaMacro(leftf,rightf));
						}
					} catch (Exception e) {
						logger.error("error parsing macro: '"+left+"' => '"+right+"'");
					}
				} else if (isGoalNode(c)) {
					String name=getGoalNameNodeValue(childAtt),desc=getGoalDescriptionNodeValue(childAtt);
					DialogueKBFormula value=DialogueKBFormula.parse(getGoalValueNodeValue(childAtt));
					if (value!=null && !StringUtils.isEmptyString(name)) {
						if (goals==null) goals=new HashMap<String,Pair<DialogueKBFormula,String>>();
						if (goals.containsKey(name)) logger.warn("Duplicated definition for goal: "+name+" OVERWRITING!");
						goals.put(name,new Pair<DialogueKBFormula, String>(value, desc));
					} else throw new Exception("Problem loading goal: "+XMLUtils.prettyPrintDom(c, " ", true, true));
				} else if (isUserModelNode(c)) {
					dp.setUserModelFileName(getUserModelNodeValue(childAtt));
				} else if (isISUpdateNode(c)) {
					String event=getISUpdateEventNodeValue(childAtt);
					DialogueOperatorEffect update=DialogueOperatorEffect.parse(getISUpdateEffectNodeValue(childAtt));
					if (!StringUtils.isEmptyString(event) && update!=null) {
						if (eventMatcher4isUpdates==null) eventMatcher4isUpdates=new EventMatcher<List<DialogueOperatorEffect>>();
						eventMatcher4isUpdates.addEventToList(event, update);
					} else throw new Exception("Problem loading IS update: "+XMLUtils.prettyPrintDom(c, " ", true, true));
				} else if (isStepLossNode(c)) {
					dp.setStepLoss(getStepLossNodeValue(childAtt));
				} else if (isIncludeTxtFormatNode(c)) {
					if (!StringUtils.isEmptyString(getIncludeTxtFormatMacroFile(childAtt))) throw new Exception("Old macro format detected. Change to new format (xml file).");
					try {
						String txtFile=getIncludeTxtFormatNodeValue(childAtt);
						File tff=new File(getPolicyDirectory(),txtFile);
						List<Node> nodes = dp.importTxtFormat(new FileReader(tff),c,txtFile);
						if (nodes!=null && !nodes.isEmpty()) q.addAll(0, nodes);
					} catch (Exception e) {
						throw new Exception("Error while parsing file: '"+fileName+"' in node: '"+XMLUtils.domNode2String(c, true, true)+"':\n\n"+e);
					}
				} else if (c.getNodeType()==Node.COMMENT_NODE) {
					//TODO: should save the comment too so that it can be printed back
				} else if (isTemplateNode(c)) {
					Template t=getTemplate(c,childAtt);
					if (t!=null) {
						if (templates==null) templates=new HashMap<>();
						templates.put(t.getName(), t);
					}
				} else if (isInstantiateNode(c)) {
					String template=getTemplateName(childAtt);
					List<DialogueOperator> ops=instantiateTemplate(c,template);
					if (ops!=null && !ops.isEmpty()) {
						for(DialogueOperator op:ops) {
							if (operatorsToBeAdded==null) operatorsToBeAdded=new ArrayList<DialogueOperator>();
							operatorsToBeAdded.add(op);
						}
					}
				} else {
					String cText=StringUtils.cleanupSpaces(XMLUtils.prettyPrintDom(c, " ", true, true));
					if (!StringUtils.isEmptyString(cText)) logger.warn("unknown item in the input file: "+cText);
				}
			}
			String policyID=fileName.replaceAll("[\\W]", "_");
			if (operatorsToBeAdded!=null) {
				for(DialogueOperator o:operatorsToBeAdded) {
					if (logger.isDebugEnabled()) o.toGDLGraph("policy_"+policyID+"_operator_"+o.getName()+".gdl");
					dp.addOperator(o);
					if (logger.isDebugEnabled()) o.toGDLGraph("policy-postprocessed"+policyID+"_operator_"+o.getName()+".gdl");
				}
			}
			if (logger.isDebugEnabled()) {
				getRootTopic().toGDLGraph("topics"+policyID+".gdl");
				mightEnableTableToGDLGraph("might-"+getPolicyDirectory().getName()+".gdl",true);
			}
			return dp;
		}
		return null;
	}

	private void removeTheseUsedVarsFromThisSet(Set<DialogueKBFormula> used,Set<String> total) {
		if (used!=null && total!=null) {
			for(DialogueKBFormula u:used) {
				String varName=u.getName();
				total.remove(varName);
			}
		}
	}
	public void validate(Long sid, NLBusBase nlModule) throws Exception {
		DM dm = nlModule.getDM(sid);
		DialogueKBInterface is=null;
		if (dm!=null) is = dm.getInformationState();
		Set<String> variables=is.getAllVariables();
		
		//open the standard input
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		
		// check goals
		HashMap<String, Pair<DialogueKBFormula,String>> goals=getGoals();
		for(DialogueOperator op:getOperators(OpType.NORMAL)) {
			for(DialogueOperatorNode state:op.getStates().values()) {
				List<DialogueOperatorEffect> effects = state.getEffects();
				if (effects!=null) {
					for(DialogueOperatorEffect e:effects) {
						if (e.isGoalAchievement()) {
							String name=e.getGoalName();
							if (variables!=null) {
								String goalValueName=DialogueOperatorEffect.buildVarNameForGoal(name);
								variables.remove(goalValueName);
							}
							if (!goals.containsKey(name)) throw new Exception("Error in state: "+state+" of operator: "+op+": using undefined goal: "+name);
						}
					}
				}
			}
		}
		DMConfig c=getConfiguration();
		// check all variables used in formulas
		HashMap<String,String> totalUnknownVars=new HashMap<String,String>();
		if (nlModule!=null) {
			if (is!=null) {

				// check all forward inference rules
				LinkedHashSet<DialogueOperatorEffect> firs = is.getForwardInferenceRules();
				if(firs!=null) {
					for(DialogueOperatorEffect fir:firs) {
						removeTheseUsedVarsFromThisSet(fir.extractAllNamesUsed(), variables);
						Set<String> unknownVars=fir.doesEffectUseAllKnownVariables(is,null);
						if (unknownVars!=null) {
							for(String uv:unknownVars) totalUnknownVars.put(uv,"in imply rule in init KB: '"+fir+"'");
						}
					}
				}
				
				// steless updates
				EventMatcher<List<DialogueOperatorEffect>> eventMatcher = getISUpdatesMatcher();
				if (eventMatcher!=null) {
					HashMap<String, List<DialogueOperatorEffect>> informationStateUpdates = eventMatcher.getAllMatchedEventsWithPayload();
					if (informationStateUpdates!=null) { 
						for(List<DialogueOperatorEffect> effects:informationStateUpdates.values()) {
							if (effects!=null) {
								for(DialogueOperatorEffect eff:effects) {
									removeTheseUsedVarsFromThisSet(eff.extractAllNamesUsed(), variables);
									Set<String> unknownVars=eff.doesEffectUseAllKnownVariables(is,null);
									if (unknownVars!=null) {
										for(String uv:unknownVars) totalUnknownVars.put(uv,"in stateless update '"+eff+"'");
										//throw new Exception("Stateless update '"+eff+"' uses uninitialized variables: "+unknownVars);
									}
								}
							}
						}
					}
				}
			
				for(DialogueOperator op:getOperators(OpType.ALL)) {
					// ignored vars
					HashSet<String> ivs = op.getIgnoredVars();
					if (ivs!=null) {
						for(String iv:ivs) {
							if (!is.hasVariableNamed(iv,ACCESSTYPE.AUTO_OVERWRITEAUTO) && !op.getLocalVars().containsKey(iv)) {
								totalUnknownVars.put(iv,"in ignored var declaration in op '"+op.getName()+"'");
							}
						}
						
					}
					//re-entry conditions
					LinkedHashSet<DialogueOperatorEntranceTransition> rs = op.getEntranceConditions();
					if (rs!=null) {
						for(DialogueOperatorEntranceTransition tr:rs) {
							DialogueKBFormula cnd=tr.getCondition();
							if (cnd!=null) {
								removeTheseUsedVarsFromThisSet(cnd.extractAllNamesUsed(), variables);
								Set<String> unknownVars=cnd.doesFormulaUseAllKnownVariables(is,op.getLocalVars());
								if (unknownVars!=null) {
									for(String uv:unknownVars) totalUnknownVars.put(uv,"in entrance condition '"+tr+"' in op '"+op.getName()+"'");
									//throw new Exception("Entrance condition '"+tr+"' in op '"+op.getName()+"' uses uninitialized variables: "+unknownVars);
								}
							}
						}
					}
					// normal entrance conditions
					LinkedHashSet<DialogueOperatorEntranceTransition> res = op.getReEntranceOptions();
					if (res!=null) {
						for(DialogueOperatorEntranceTransition tr:res) {
							DialogueKBFormula cnd=tr.getCondition();
							if (cnd!=null) {
								removeTheseUsedVarsFromThisSet(cnd.extractAllNamesUsed(), variables);
								Set<String> unknownVars=cnd.doesFormulaUseAllKnownVariables(is,op.getLocalVars());
								if (unknownVars!=null) {
									for(String uv:unknownVars) totalUnknownVars.put(uv,"in re-entrance condition '"+tr+"' in op '"+op.getName()+"'");
									//throw new Exception("RE-Entrance condition '"+tr+"' in op '"+op.getName()+"' uses uninitialized variables: "+unknownVars);
								}
							}
						}
					}

					// states and edges
					for(DialogueOperatorNode state:op.getStates().values()) {
						List<DialogueOperatorEffect> effects = state.getEffects();
						if (effects!=null) {
							for(DialogueOperatorEffect eff:effects) {
								removeTheseUsedVarsFromThisSet(eff.extractAllNamesUsed(), variables);
								Set<String> unknownVars=eff.doesEffectUseAllKnownVariables(is,op.getLocalVars());
								if (unknownVars!=null) {
									for(String uv:unknownVars) totalUnknownVars.put(uv,"in effect '"+eff+"' in op '"+op.getName()+"'");
									//throw new Exception("Effect '"+eff+"' in op '"+op.getName()+"' uses uninitialized variables: "+unknownVars);
								}
							}
						}

						if (state.hasChildren()) {
							for (Edge e:state.getOutgoingEdges()) {
								DialogueOperatorNodeTransition tr=(DialogueOperatorNodeTransition) e;
								DialogueKBFormula cnd=tr.getCondition();
								if (cnd!=null) {
									removeTheseUsedVarsFromThisSet(cnd.extractAllNamesUsed(), variables);
									Set<String> unknownVars=cnd.doesFormulaUseAllKnownVariables(is,op.getLocalVars());
									if (unknownVars!=null) {
										for(String uv:unknownVars) totalUnknownVars.put(uv,"in edge condition '"+tr+"' in op '"+op.getName()+"'");
										//throw new Exception("RE-Entrance condition '"+tr+"' in op '"+op.getName()+"' uses uninitialized variables: "+unknownVars);
									}
								}
							}
						}
					}
				}
			}
			if (!totalUnknownVars.isEmpty()) {
				String msg="";
				for(Entry<String,String> ee:totalUnknownVars.entrySet()) msg+=ee.getKey()+":   "+ee.getValue()+"\n";
				throw new Exception("unknown variables: "+msg);
			}
			if (variables!=null && !variables.isEmpty()) {
				logger.warn("NOT USED variables: "+variables);
			}
		}
		HashSet<String> events=new HashSet<String>();
		// check messages to NLG
		if (nlModule!=null) {
			NLGInterface nlg=nlModule.getNlg(sid);
			if (nlg!=null) {
				Map<String, List<SpeechActWithProperties>> lines = nlg.getAllLines();
				for(DialogueOperator op:getOperators(OpType.NORMAL)) {
					for(DialogueOperatorEntranceTransition ec:op.getAllEntrancePossibilities()) {
						String say=ec.getSay();
						if (!StringUtils.isEmptyString(say)) {
							boolean speechAct=lines!=null?lines.containsKey(say):nlg.doNLG(sid,new DMSpeakEvent(null,say, sid, null,null),null,true)!=null;
							if (!speechAct) {
								String msg=say+" of entrance condition: "+ec+" of operator: "+op;
								events.add(msg);
							}
						}
					}
					for(DialogueOperatorNode state:op.getStates().values()) {
						if (state.hasChildren()) {
							for (Edge e:state.getOutgoingEdges()) {
								DialogueOperatorNodeTransition tr=(DialogueOperatorNodeTransition) e;
								if (tr.isSayTransition()) {
									String say=tr.getEvent();
									boolean speechAct=lines!=null?lines.containsKey(say):nlg.doNLG(sid,new DMSpeakEvent(null,say, sid, null,null),null,true)!=null;
									if (!speechAct) {
										String msg=say+" in state: "+state+" of operator: "+op;
										events.add(msg);
									}
								}
							}
						}
					}
				}
				if (!events.isEmpty()) {
					if (c.getNlgConfigNC().getIsStrictNLG()) {
						logger.error(FunctionalLibrary.printCollection(events, "Unknown system speech acts:\n[", "]", "\n"));
						logger.error("press ENTER to continue.");
						while(in.readLine()==null);
					} else logger.warn(FunctionalLibrary.printCollection(events, "[", "]", "\n"));
				}
			}
		}
		events.clear();
		// check all events in listening transitions
		if (nlModule!=null) {
			NLUInterface nlu=nlModule.getNlu(sid);
			Set<String> possibilities;
			if (nlu!=null && ((possibilities= nlu.getAllSimplifiedPossibleOutputs())!=null)) {
				NLUConfig nluConfig=nlu.getConfiguration();
				Set<String> unUsedPossibilities=new HashSet<String>(possibilities);
				String ev=c.getTimerEvent();
				if (!StringUtils.isEmptyString(ev)) possibilities.add(ev);
				
				ev=c.getUnhandledEventName();
				if (!StringUtils.isEmptyString(ev)) possibilities.add(ev);
				
				ev=nluConfig.getLowConfidenceEvent();
				if (!StringUtils.isEmptyString(ev)) possibilities.add(ev);
				
				ev=c.getForcedIgnoreEventName();
				if (!StringUtils.isEmptyString(ev)) possibilities.add(ev);
				
				ev=c.getLoginEventName();
				if (!StringUtils.isEmptyString(ev)) possibilities.add(ev);
				
				ev=nluConfig.getEmptyTextEventName();
				if (!StringUtils.isEmptyString(ev)) possibilities.add(ev);

				ev=c.getLoopEventName();
				if (!StringUtils.isEmptyString(ev)) possibilities.add(ev);

				for(DialogueOperator op:getOperators(OpType.ALL)) {
					for(DialogueOperatorNode state:op.getStates().values()) {
						if (state.hasChildren()) {
							for (Edge e:state.getOutgoingEdges()) {
								DialogueOperatorNodeTransition tr=(DialogueOperatorNodeTransition) e;
								if (tr.isListenTransition()) {
									String event=tr.getEvent();
									boolean found=isThisEventPossible(event,possibilities);
									removeEntirePath(unUsedPossibilities,event);
									if (!found) events.add(event+" in network "+op);
								}
							}
						}
					}
					for(DialogueOperatorEntranceTransition ec:op.getAllEntrancePossibilities()) {
						if (ec.isListenTransition()) {
							String event=ec.getEvent();
							boolean found=isThisEventPossible(event,possibilities);
							removeEntirePath(unUsedPossibilities,event);
							if (!found) events.add(event+" in network "+op);
						}
					}
				}
				EventMatcher<List<DialogueOperatorEffect>> eventMatcher = getISUpdatesMatcher();
				if (eventMatcher!=null) {
					HashMap<String, List<DialogueOperatorEffect>> informationStateUpdates = eventMatcher.getAllMatchedEventsWithPayload();
					if (informationStateUpdates!=null) {
						for(String event:informationStateUpdates.keySet()) {
							boolean found=isThisEventPossible(event,possibilities);
							removeEntirePath(unUsedPossibilities,event);
							if (!found) events.add(event+" in stateless update.");
						}
					}
				}
				nlModule.killNlu(sid);
				nlModule.killNlg(sid);
				if (!events.isEmpty() && c.getNlgConfigNC().getIsStrictNLG()) {
					logger.error("There are INPUT speech acts used by the system but not defined in the NLU.");
					logger.error(FunctionalLibrary.printCollection(events, "[", "]", "\n"));
				}
				if (!unUsedPossibilities.isEmpty()) {
					logger.warn("There are speech acts that are defined in the NLU training data but are not used:");
					for(String s:unUsedPossibilities) {
						logger.warn("  "+s);
					}
				}
			}
		}
	}
	
	public List<DMSpeakEvent> getAllPossibleSystemLines() {
		Set<String> alreadyListed=null;
		List<DMSpeakEvent> ret=null;
		for(DialogueOperator op:getOperators(OpType.NORMAL)) {
			for(DialogueOperatorEntranceTransition ec:op.getAllEntrancePossibilities()) {
				String say=ec.getSay();
				if (!StringUtils.isEmptyString(say)) {
					if (alreadyListed==null || !alreadyListed.contains(say)) {
						DMSpeakEvent ev=new DMSpeakEvent(null,say, -1l, null,null);
						if (ret==null) ret=new ArrayList<DMSpeakEvent>();
						ret.add(ev);
						if (alreadyListed==null) alreadyListed=new HashSet<String>();
						alreadyListed.add(say);
					}
				}
			}
			for(DialogueOperatorNode state:op.getStates().values()) {
				if (state.hasChildren()) {
					for (Edge e:state.getOutgoingEdges()) {
						DialogueOperatorNodeTransition tr=(DialogueOperatorNodeTransition) e;
						if (tr.isSayTransition()) {
							String say=tr.getEvent();
							if (alreadyListed==null || !alreadyListed.contains(say)) {
								DMSpeakEvent ev=new DMSpeakEvent(null,say, -1l, null,null);
								if (ret==null) ret=new ArrayList<DMSpeakEvent>();
								ret.add(ev);
								if (alreadyListed==null) alreadyListed=new HashSet<String>();
								alreadyListed.add(say);
							}
						}
					}
				}
			}
		}
		return ret;
	}
	
	private void removeEntirePath(Set<String> unUsedPossibilities, String event) {
		unUsedPossibilities.remove(event);
		int start=event.length();
		int pos=0;
		while((pos=event.lastIndexOf('.',start))>=0) {
			if (pos>0) {
				String ev=event.substring(0, pos);
				unUsedPossibilities.remove(ev);
				start=pos-1;
			}
		}
	}
	
	public Set<String> getAllSpokenLines() {
		Set<String> events=null;
		for(DialogueOperator op:getOperators(OpType.NORMAL)) {
			for(DialogueOperatorEntranceTransition ec:op.getAllEntrancePossibilities()) {
				String say=ec.getSay();
				if (!StringUtils.isEmptyString(say)) {
					if (events==null) events=new HashSet<String>();
					events.add(say);
				}
			}
			for(DialogueOperatorNode state:op.getStates().values()) {
				if (state.hasChildren()) {
					for (Edge e:state.getOutgoingEdges()) {
						DialogueOperatorNodeTransition tr=(DialogueOperatorNodeTransition) e;
						if (tr.isSayTransition()) {
							String say=tr.getEvent();
							if (events==null) events=new HashSet<String>();
							events.add(say);
						}
					}
				}
			}
		}
		return events;
	}
	public Set<String> getAllHandledLines() {
		Set<String> events=null;
		for(DialogueOperator op:getOperators(OpType.NORMAL)) {
			for(DialogueOperatorNode state:op.getStates().values()) {
				if (state.hasChildren()) {
					for (Edge e:state.getOutgoingEdges()) {
						DialogueOperatorNodeTransition tr=(DialogueOperatorNodeTransition) e;
						if (tr.isListenTransition()) {
							String event=tr.getEvent();
							if (events==null) events=new HashSet<String>();
							events.add(event);
						}
					}
				}
			}
			for(DialogueOperatorEntranceTransition ec:op.getAllEntrancePossibilities()) {
				if (ec.isListenTransition()) {
					String event=ec.getEvent();
					if (events==null) events=new HashSet<String>();
					events.add(event);
				}
			}
		}
		EventMatcher<List<DialogueOperatorEffect>> eventMatcher = getISUpdatesMatcher();
		if (eventMatcher!=null) {
			HashMap<String, List<DialogueOperatorEffect>> informationStateUpdates = eventMatcher.getAllMatchedEventsWithPayload();
			if (informationStateUpdates!=null) {
				for(String event:informationStateUpdates.keySet()) {
					if (events==null) events=new HashSet<String>();
					events.add(event);
				}
			}
		}
		return events;
	}
	
	private boolean isThisEventPossible(String event,Set<String> possibilities) {
		EventMatcher<Object> em = new EventMatcher<Object>();
		em.addEvent(event, "");
		boolean found=false;
		if (possibilities!=null) {
			for(String p:possibilities) {
				if (!em.match(p).isEmpty()) {
					found=true;
					break;
				}
			}
		}
		return found;
	}
	private List<Node> importTxtFormat(Reader f, Node sourceNode,String dumpfilePrefix) throws Exception {
		List<Node> ret=null;
		TextFormatGrammar parser = new TextFormatGrammar(f);
		String result;
		//System.out.println(f);
		result = parser.Input();
		if (logger.getLevel()==Level.DEBUG) FileUtils.dumpToFile(result, dumpfilePrefix+"-dump-to-xml.xml");
		Document doc = XMLUtils.parseXMLString(result, true, true);
		//System.out.println(XMLUtils.prettyPrintDom(doc, " ", true, true));
		Node rootNode = doc.getDocumentElement();
		NodeList cs = rootNode.getChildNodes();
		for (int i = 0; i < cs.getLength(); i++) {
			if (ret==null) ret=new ArrayList<>();
			ret.add(cs.item(i));
		}
		return ret;
	}

	public void addOperator(DialogueOperator o) throws Exception {
		justAddOperator(o);
		postProcessOperator(o);
	}
	public void removeOperator(String name) throws Exception {
		DialogueOperator o=getOperatorNamed(name, OpType.ALL);
		justRemoveOperator(o);
		postProcessRemovedOperator(o);
	}
	private void justRemoveOperator(DialogueOperator o) throws Exception {
		if (o!=null) {
			
			HashMap<String, DialogueOperator> ops=null;
			EventMatcher<List<DialogueOperator>> userOps=null;
			Set<DialogueOperator> sysOps=null;

			if (o!=null && o.isNormal()) {
				ops=operators;
				userOps=eventMatcher4UserInitiativeOperators;
				sysOps=systemInitiativeOperators;
			} else if (o.isDaemon()) {
				ops=daemonOperators;
				userOps=eventMatcher4UserInitiativeDaemonOperators;
				sysOps=systemInitiativeDaemonOperators;
			}
			
			ops.remove(o);
			if (o.isUserTriggerable()) {
				EventMatcher<List<DialogueOperatorEntranceTransition>> operatorEventMatcher = o.getEventMatcher();
				if (operatorEventMatcher!=null) {
					Set<String> events = operatorEventMatcher.getAllMatchedEvents();
					if (events!=null) {
						for (String event:events) {
							userOps.removeEventFromList(event, o);
						}
					}
				}
			}
			if (o.isSystemTriggerable()) {
				sysOps.remove(o);
			}
		} else throw new Exception("null operator.");
	}
	private void justAddOperator(DialogueOperator o) throws Exception {
		String name=null;
		if (o!=null && !StringUtils.isEmptyString(name=o.getName())) {
			
			HashMap<String, DialogueOperator> ops=null;
			EventMatcher<List<DialogueOperator>> userOps=null;
			Set<DialogueOperator> sysOps=null;
			
			if (o.isDaemon()) {
				if (daemonOperators==null) daemonOperators=new HashMap<String,DialogueOperator>();
				ops=daemonOperators;
				if (o.isUserTriggerable() && eventMatcher4UserInitiativeDaemonOperators==null) eventMatcher4UserInitiativeDaemonOperators=new EventMatcher<List<DialogueOperator>>();
				userOps=eventMatcher4UserInitiativeDaemonOperators;
				if (o.isSystemTriggerable() && systemInitiativeDaemonOperators==null) systemInitiativeDaemonOperators=new HashSet<DialogueOperator>();
				sysOps=systemInitiativeDaemonOperators;
			} else {
				if (operators==null) operators=new HashMap<String,DialogueOperator>();
				ops=operators;
				if (o.isUserTriggerable() && eventMatcher4UserInitiativeOperators==null) eventMatcher4UserInitiativeOperators=new EventMatcher<List<DialogueOperator>>();
				userOps=eventMatcher4UserInitiativeOperators;
				if (o.isSystemTriggerable() && systemInitiativeOperators==null) systemInitiativeOperators=new HashSet<DialogueOperator>();
				sysOps=systemInitiativeOperators;
			}
			if (daemonOperators!=null && daemonOperators.containsKey(name)) throw new Exception("Duplicated operator: "+name);
			if (operators!=null && operators.containsKey(name)) throw new Exception("Duplicated operator: "+name);
			
			ops.put(name,o);
			if (o.isUserTriggerable()) {
				EventMatcher<List<DialogueOperatorEntranceTransition>> operatorEventMatcher = o.getEventMatcher();
				if (operatorEventMatcher!=null) {
					Set<String> events = operatorEventMatcher.getAllMatchedEvents();
					if (events!=null) {
						for (String event:events) userOps.addEventToList(event, o);
					}
				}
			}
			if (o.isSystemTriggerable()) {
				sysOps.add(o);
			}
		} else throw new Exception("null operator.");
	}

	private HashMap<DialogueOperatorEntranceTransition,Set<DialogueOperatorEntranceTransition>> mightEnableTable=null;
	
	private void postProcessOperator(DialogueOperator op) throws Exception {
		if (op!=null && op.isNormal()) {
			op.postProcessOperator(getISUpdatesMatcher(),this);
		}
		if (getConfiguration()!=null && getConfiguration().getApproximatedForwardSearch()) {
			mightEnableTable=updateMightEnableTableWith(mightEnableTable,op);
		}
		/*System.out.println(mightEnableTable.size());
		for(DialogueOperatorEntranceTransition ec1:mightEnableTable.keySet()) {
			Set<DialogueOperatorEntranceTransition> eecs = mightEnableTable.get(ec1);
			System.out.print(ec1.getOperator()+":\n "+((eecs!=null)?eecs.size():0)+" "+FunctionalLibrary.map(eecs, DialogueOperatorEntranceTransition.class.getMethod("getOperator", null)));
			System.out.println();
		}*/
	}
	private void postProcessRemovedOperator(DialogueOperator o) {
		//topics graph
		DialogueOperatorTopic rt = getRootTopic();
		Set<edu.usc.ict.nl.util.graph.Node> nodes = rt.getAllNodes();
		if (nodes!=null) {
			//reset mark on all topics nodes
			for(edu.usc.ict.nl.util.graph.Node n:nodes) {
				DialogueOperatorTopic t=(DialogueOperatorTopic)n;
				t.resetMark();
			}
			//get all topics of all operators (other operators)
			// mark all visited nodes
			for(DialogueOperator op:getOperators(OpType.ALL)) {
				List<DialogueOperatorTopic> ts = op.getTopics();
				if (ts!=null) {
					Deque<DialogueOperatorTopic> q=new LinkedList<DialogueOperatorTopic>();
					q.addAll(ts);
					while(!q.isEmpty()) {
						DialogueOperatorTopic t=q.pop();
						t.setMark();
						try {
							Collection parents = t.getImmediateParents();
							if (parents!=null) q.addAll(parents);
						} catch (Exception e) {
						}
					}
				}
			}
			//keep only visited nodes
			for(edu.usc.ict.nl.util.graph.Node n:nodes) {
				DialogueOperatorTopic t=(DialogueOperatorTopic)n;
				if (!t.isMark()) {
					try {
						Collection<edu.usc.ict.nl.util.graph.Node> parents = t.getImmediateParents();
						if (parents!=null) {
							for(edu.usc.ict.nl.util.graph.Node np:parents) {
								np.removeEdgeTo(t);
							}
						}
						Collection<edu.usc.ict.nl.util.graph.Node> children = t.getImmediateChildren();
						if (children!=null) {
							for(edu.usc.ict.nl.util.graph.Node nc:children) {
								nc.removeEdgeFrom(t);
							}
						}
					} catch (Exception e) {
					}
				}
			}
		}
		
		//updateMightEnableTableWith
		if (mightEnableTable!=null) {
			LinkedHashSet<DialogueOperatorEntranceTransition> ecs = o.getEntranceConditions();
			if (ecs!=null) {
				for(DialogueOperatorEntranceTransition ec:ecs) {
					mightEnableTable.remove(ec);
				}
				for(DialogueOperatorEntranceTransition ec:mightEnableTable.keySet()) {
					Set<DialogueOperatorEntranceTransition> enabled=mightEnableTable.get(ec);
					if (enabled!=null) {
						for(DialogueOperatorEntranceTransition oec:ecs) {
							enabled.remove(oec);
						}
						if (enabled.isEmpty()) {
							mightEnableTable.remove(ec);
						}
					}
				}
			}
		}
	}

	private void mightEnableTableToGDLGraph(String filename,boolean compress) throws IOException {
		mightEnableTableToGDLGraph(mightEnableTable, filename, compress);
	}
	private void mightEnableTableToGDLGraph(HashMap<DialogueOperatorEntranceTransition, Set<DialogueOperatorEntranceTransition>> mightEnableTable,String filename,boolean compress) throws IOException {
		FileWriter out=new FileWriter(filename);
		HashSet<Object> visited=new HashSet<Object>();
		out.write("graph: {display_edge_labels: no\n");
		if (mightEnableTable!=null) {
			for(GraphElement source:mightEnableTable.keySet()) {
				Set<DialogueOperatorEntranceTransition> dests=mightEnableTable.get(source);
				if (dests!=null) {
					for(GraphElement dest:dests) {
						if (compress && (dest instanceof DialogueOperatorEntranceTransition))
							dest=((DialogueOperatorEntranceTransition) dest).getOperator();
						if (compress && (source instanceof DialogueOperatorEntranceTransition))
							source=((DialogueOperatorEntranceTransition) source).getOperator();
						if (dest!=source) {
							String gdl="";
							if (!visited.contains(source)) {
								gdl+="node: { title: \""+source.getID()+"\" label: \""+source.toString()+"\"}\n";
								visited.add(source);
							}
							if (!visited.contains(dest)) {
								gdl+="node: { title: \""+dest.getID()+"\" label: \""+dest.toString()+"\"}\n";
								visited.add(dest);
							}
							String edgeID=source.getID()+"-"+dest.getID();
							if (!visited.contains(edgeID)) {
								gdl+="edge: {source: \""+source.getID()+"\" target: \""+dest.getID()+"\" label: \"\" }\n";
								visited.add(edgeID);
							}
							out.write(gdl);
						}
					}
				}
			}
		}
		out.write("}");
		out.close();
	}
	
	public Set<DialogueOperatorEntranceTransition> getSetOfEntranceOptionsThatCanBeEnabledByHavingExecutedThisEntranceCondition(DialogueOperatorEntranceTransition ec) {
		if (mightEnableTable!=null) {
			return mightEnableTable.get(ec);
		}
		return null;
	}
	private HashSet<DialogueOperatorEntranceTransition> getSetOfEntranceOptionsThatCanBeEnabledByThisSetOfChangedVariables(Set<DialogueKBFormula> changedVars) {
		HashSet<DialogueOperatorEntranceTransition>ret=null;
		if (changedVars!=null && !changedVars.isEmpty()) {
			for(DialogueOperator op:getOperators(OpType.NORMAL)) {
				HashSet<DialogueOperatorEntranceTransition> ecs = op.getAllEntrancePossibilities();
				if (ecs!=null) {
					for(DialogueOperatorEntranceTransition ec:ecs) {
						DialogueKBFormula cnd = ec.getCondition();
						if (cnd!=null) {
							Set<DialogueKBFormula> usedVars=cnd.extractAllNamesUsed();
							if (usedVars!=null && !usedVars.isEmpty()) {
								boolean emptyIntersection=Collections.disjoint(changedVars, usedVars);
								if (!emptyIntersection) {
									if (ret==null) ret=new HashSet<DialogueOperatorEntranceTransition>();
									ret.add(ec);
								}
							} else {
								if (ret==null) ret=new HashSet<DialogueOperatorEntranceTransition>();
								ret.add(ec);
							}
						}
					}
				}
			}
		}
		return ret;
	}
	private HashMap<DialogueOperatorEntranceTransition, Set<DialogueOperatorEntranceTransition>> computeMightEnableTable() {
		HashMap<DialogueOperatorEntranceTransition, Set<DialogueOperatorEntranceTransition>> mightEnableTable=new HashMap<>();
		for(DialogueOperator op:getOperators(OpType.NORMAL)) {
			mightEnableTable=updateMightEnableTableWith(mightEnableTable,op);
		}
		return mightEnableTable;
	}
	private HashMap<DialogueOperatorEntranceTransition, Set<DialogueOperatorEntranceTransition>> updateMightEnableTableWith(HashMap<DialogueOperatorEntranceTransition, Set<DialogueOperatorEntranceTransition>> mightEnableTable,DialogueOperator op) {
		if (op!=null && op.isNormal()) {
			HashMap<DialogueOperatorEntranceTransition, Set<DialogueKBFormula>> modVars = op.getSetOfModifiedVariables();
			for(DialogueOperatorEntranceTransition ec:modVars.keySet()) {
				Set<DialogueKBFormula> modVarsForEC=modVars.get(ec);
				if (mightEnableTable==null) mightEnableTable=new HashMap<DialogueOperatorEntranceTransition, Set<DialogueOperatorEntranceTransition>>();
				mightEnableTable.put(ec,getSetOfEntranceOptionsThatCanBeEnabledByThisSetOfChangedVariables(modVarsForEC));
			}
		}
		return mightEnableTable;
	}

	public EventMatcher<List<DialogueOperatorEffect>> getISUpdatesMatcher() {return eventMatcher4isUpdates;}

	private void setPolicyDirectory(File path) {this.policyDirectory=path;}
	public File getPolicyDirectory() {return policyDirectory;}

	private void setStepLoss(float loss) {this.stepLoss=loss;}
	public float getStepLoss() {return this.stepLoss;}
	
	private void setUserModelFileName(String fileName) {this.userModelFileName=fileName;}
	public String getUserModelFileName() {return userModelFileName;}

	public static boolean isInitsNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.INITISID);
	}
	public static boolean isInitNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.LOADISID);
	}
	public static String getInitNodeValue(NamedNodeMap att) {
		Node node = att.getNamedItem(XMLConstants.EXPRID);
		if (node!=null) return StringUtils.cleanupSpaces(node.getNodeValue());
		else return null;
	}
	
	public static boolean isMacrosNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.MACROSID);
	}
	public static boolean isEventMacroNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.EVENTMACROID);
	}
	public static boolean isFormulatMacroNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.FORMULAMACROID);
	}
	public static String getLeftNodeValue(NamedNodeMap att) {
		Node node = att.getNamedItem(XMLConstants.LEFTID);
		if (node!=null) return StringUtils.cleanupSpaces(node.getNodeValue());
		else return null;
	}
	public static String getRightNodeValue(NamedNodeMap att) {
		Node node = att.getNamedItem(XMLConstants.RIGHTID);
		if (node!=null) return StringUtils.cleanupSpaces(node.getNodeValue());
		else return null;
	}

	private static boolean isGoalsNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.GOALSID);
	}
	private static boolean isGoalNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.GOALID);
	}
	private static String getGoalNameNodeValue(NamedNodeMap att) {
		Node node = att.getNamedItem(XMLConstants.IDID);
		if (node!=null) return StringUtils.cleanupSpaces(node.getNodeValue());
		else return null;
	}
	private static String getGoalDescriptionNodeValue(NamedNodeMap att) {
		Node node = att.getNamedItem(XMLConstants.DESCRIPTIONID);
		if (node!=null) return StringUtils.cleanupSpaces(node.getNodeValue());
		else return null;
	}
	private static String getGoalValueNodeValue(NamedNodeMap att) {
		Node node = att.getNamedItem(XMLConstants.VALUEID);
		if (node!=null) return node.getNodeValue();
		else return null;
	}

	private static boolean isUserModelNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.USERMODELID);
	}
	private static String getUserModelNodeValue(NamedNodeMap att) {
		Node node = att.getNamedItem(XMLConstants.VALUEID);
		if (node!=null) return StringUtils.cleanupSpaces(node.getNodeValue());
		else return null;
	}
	
	private static boolean isStepLossNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.STEPID);
	}
	private static float getStepLossNodeValue(NamedNodeMap att) {
		Node node = att.getNamedItem(XMLConstants.VALUEID);
		if (node!=null) return Float.parseFloat(node.getNodeValue());
		else return 1;
	}

	private static boolean isIncludeTxtFormatNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.INCLUDEID);
	}
	private static String getIncludeTxtFormatNodeValue(NamedNodeMap att) {
		Node node = att.getNamedItem(XMLConstants.HREFID);
		if (node!=null) return StringUtils.cleanupSpaces(node.getNodeValue());
		else return null;
	}
	private static String getIncludeTxtFormatMacroFile(NamedNodeMap att) {
		Node node = att.getNamedItem(XMLConstants.MACROID);
		if (node!=null) return StringUtils.cleanupSpaces(node.getNodeValue());
		else return null;
	}

	private static boolean isISUpdatesNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.LISTENERSID);
	}
	private static boolean isISUpdateNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.LISTENID);
	}
	private static String getISUpdateEventNodeValue(NamedNodeMap att) {
		Node node = att.getNamedItem(XMLConstants.EVENTID);
		if (node!=null) return StringUtils.cleanupSpaces(node.getNodeValue());
		else return null;
	}
	private static String getISUpdateEffectNodeValue(NamedNodeMap att) {
		Node node = att.getNamedItem(XMLConstants.UPDATEID);
		if (node!=null) return StringUtils.cleanupSpaces(node.getNodeValue());
		else return null;
	}
	
	public static boolean isDialoguePolicy(Node n) {
		return (n.getNodeType()==Node.ELEMENT_NODE) && n.getNodeName().toLowerCase().equals(XMLConstants.POLICYID);
	}
	
	public static boolean isTemplatesNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.TEMPLATESID);
	}
	public static boolean isTemplateNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.TEMPLATEID);
	}
	public static boolean isInstantiateNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.INSTANTIATEID);
	}
	public static boolean isArgumentNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.ARGID);
	}
	private Template getTemplate(Node c, NamedNodeMap childAtt) throws Exception {
		String tname=getName(childAtt);
		String content=XMLUtils.getStringContent(c);
		return new Template(tname,content);
	}
	private static String getName(NamedNodeMap att) {
		Node node = att.getNamedItem(XMLConstants.NAMEID);
		if (node!=null) return StringUtils.cleanupSpaces(node.getNodeValue());
		else return null;
	}
	private static String getTemplateName(NamedNodeMap att) {
		Node node = att.getNamedItem(XMLConstants.TEMPLATEID);
		if (node!=null) return StringUtils.cleanupSpaces(node.getNodeValue());
		else return null;
	}
	private DialogueKBFormula getValues(NamedNodeMap att) {
		Node node = att.getNamedItem(XMLConstants.VALUESID);
		if (node!=null) {
			String sv=node.getNodeValue();
			try {
				return DialogueKBFormula.parse(sv);
			} catch (Exception e) {
				logger.error("error while parsing values formula '"+sv+"'",e);
			}
		}
		return null;
	}
	private List<DialogueOperator> instantiateTemplate(Node c, String template) throws Exception {
		List<DialogueOperator> ret=null;
		Map<String,List<String>> vars=null;
		int numArgs=-1;
		if (c!=null && !StringUtils.isEmptyString(template)) {
			NodeList cs = c.getChildNodes();
			for (int i = 0; i < cs.getLength(); i++) {
				Node cc=cs.item(i);
				if (isArgumentNode(cc)) {
					NamedNodeMap att=cc.getAttributes();
					String vName=getName(att);
					DialogueKBFormula valuesF=getValues(att);
					List<String> subs=null;
					if (valuesF!=null) {
						if (valuesF.isConstant()) {
							//NUMBER,STRING,TRUE,FALSE,NULL
							String v=valuesF.toString();
							if (valuesF.isString()) v=DialogueKBFormula.getStringValue(v);
							if (v!=null) {
								if (subs==null) subs=new ArrayList<>();
								subs.add(v);
							}
						} else {
							TrivialDialogueKB is = new TrivialDialogueKB();
							Collection<DialogueOperatorEffect> effs = getISinitialization();
							is.storeAll(effs, ACCESSTYPE.AUTO_OVERWRITEAUTO, true);
							Object r=is.evaluate(valuesF, null);
							if (r!=null) {
								if (r instanceof Collection) {
									for(Object o:(Collection)r) {
										if (o!=null) {
											String v=o.toString();
											if (o instanceof DialogueKBFormula && ((DialogueKBFormula) o).isString()) v=DialogueKBFormula.getStringValue(v);
											if (v!=null) {
												if (subs==null) subs=new ArrayList<>();
												subs.add(v);
											}
										}
									}
								} else {
									String v=valuesF.toString();
									if (v!=null) {
										if (subs==null) subs=new ArrayList<>();
										subs.add(v);
									}
								}
							}
						}
					}
					if (!StringUtils.isEmptyString(vName) && subs!=null) {
						if (vars==null) vars=new HashMap<>();
						vars=new HashMap<>();
						if (vars.containsKey(vName)) logger.error("duplicated assignment for variable: "+vName+" in "+XMLUtils.prettyPrintDom(c, " ", true, true));
						if (numArgs<0) numArgs=subs.size();
						else if (numArgs!=subs.size()) {
							logger.error("unequal number of arguments, expected: "+numArgs+" but for variable "+vName+" got "+subs.size());
							logger.error(" in "+XMLUtils.prettyPrintDom(cc, " ", true, true));
							logger.error(" eval result is: "+subs);
						}
						vars.put(vName, subs);
					}
				}
			}
		}
		if (templates!=null) {
			Template t=templates.get(template);
			if (t!=null && vars!=null) {
				for(int i=0;i<numArgs;i++) {
					String opS=t.applySubstitutions(vars,i);
					List<Node> nodes=importTxtFormat(new StringReader(opS),c,"");
					if (nodes!=null) {
						for(Node n:nodes) {
							if (n!=null && DialogueOperator.isOperatorNode(n)) {
								DialogueOperator op=DialogueOperator.parseOperator(n);
								if (op!=null) {
									if (ret==null) ret=new ArrayList<>();
									ret.add(op);
								}
							}
						}
					}
				}
			}
		}
		return ret;
	}
	
	@Override
	public String toString() {
		String ret="<"+XMLConstants.POLICYID+">";
		if (initIS!=null && !initIS.isEmpty()) {
			for(DialogueOperatorEffect eff:initIS) {
				ret+=eff.toString(false, XMLConstants.LOADISID);
				//ret+="<"+XMLConstants.LOADISID+" "+XMLConstants.EXPRID+"=\""+XMLUtils.escapeStringForXML(eff.toString(true))+"\"/>";
			}
		}
		if (getGoals()!=null) {
			HashMap<String,Pair<DialogueKBFormula,String>>goals=getGoals();
			for(String gn:goals.keySet()) {
				Pair<DialogueKBFormula,String> valueAndDesc=goals.get(gn);
				ret+="<"+XMLConstants.GOALID+" "+XMLConstants.IDID+"=\""+gn+"\" "+
				XMLConstants.DESCRIPTIONID+"=\""+valueAndDesc.getSecond()+"\" "+XMLConstants.VALUEID+"=\""+valueAndDesc.getFirst()+"\"/>";
			}
		}
		if (getISUpdatesMatcher()!=null) {
			HashMap<String, List<DialogueOperatorEffect>> allISupdates = getISUpdatesMatcher().getAllMatchedEventsWithPayload();
			if (allISupdates!=null && !allISupdates.isEmpty()) {
				for (String event:allISupdates.keySet()) {
					List<DialogueOperatorEffect> updates = allISupdates.get(event);
					if (updates!=null && !updates.isEmpty()) {
						for(DialogueOperatorEffect update:updates) {
							ret+="<"+XMLConstants.LISTENID+" "+XMLConstants.EVENTID+"=\""+event+"\" "+XMLConstants.UPDATEID+"=\""+update+"\"/>";
						}
					}
				}
			}
		}
		if (!StringUtils.isEmptyString(userModelFileName)) ret+="<"+XMLConstants.USERMODELID+" "+XMLConstants.VALUEID+"=\""+userModelFileName+"\"/>";
		ret+="<"+XMLConstants.STEPID+" "+XMLConstants.VALUEID+"=\""+getStepLoss()+"\"/>";
		if (operators!=null) for(DialogueOperator o:operators.values()) ret+=o.toString(false);
		ret+="</"+XMLConstants.POLICYID+">";
		return ret;
	}

	public List<DialogueOperator> getUserTriggerableOperatorsForEvent(String evName, OpType type) throws Exception {
		EventMatcher<List<DialogueOperator>> userOps=null;
		switch (type) {
		case NORMAL:
			userOps=eventMatcher4UserInitiativeOperators;
			break;
		case DAEMON:
			userOps=eventMatcher4UserInitiativeDaemonOperators;
			break;
		case ALL:
			throw new Exception("unsupported type.");
		}
		if (userOps==null) return null;
		else {
			Set<List<DialogueOperator>> matches = userOps.match(evName);
			ArrayList<DialogueOperator> result=null;
			if (matches!=null && !matches.isEmpty()) {
				for(List<DialogueOperator> ops:matches) {
					if (ops!=null && !ops.isEmpty()) {
						if (result==null) result=new ArrayList<DialogueOperator>();
						result.addAll(ops);
					}
				}
			}
			return result;
		}
	}
	public Set<DialogueOperator> getSystemInitiatableOperators(OpType type) throws Exception {
		switch (type) {
		case NORMAL:
			return systemInitiativeOperators;
		case DAEMON:
			return systemInitiativeDaemonOperators;
		case ALL:
			throw new Exception("unsupported type.");
		}
		return null;
	}
	
	public void toGDLGraph(String fileName) {
		try {
			PrintWriter out=new PrintWriter(new FileWriter(fileName));
			out.write("graph: {display_edge_labels: yes\n");
			if (getOperators(OpType.ALL)!=null) {
				for(DialogueOperator op:getOperators(OpType.ALL)) {
					op.toGDL(out);
				}
			}
			out.write("}\n");
			out.close();
		} catch (Exception e) {e.printStackTrace();}
	}
	public void toGDLGraphs(String baseFileName) {
		try {
			if (getOperators(OpType.ALL)!=null) {
				for(DialogueOperator op:getOperators(OpType.ALL)) {
					PrintWriter out=new PrintWriter(new FileWriter(op.getName()+"-"+baseFileName));
					out.write("graph: {display_edge_labels: yes\n");
					op.toGDL(out);
					out.write("}\n");
					out.close();
				}
			}
		} catch (Exception e) {e.printStackTrace();}
	}
	
	/*public void getTopicTrainingData(List<String> possibleTopics) {
		HashMap<String,Set<DialogueOperatorTopic>> data=new HashMap<String, Set<DialogueOperatorTopic>>();
		HashMap<String,List<DialogueOperatorTopic>> possibleTopicNodes=new HashMap<String, List<DialogueOperatorTopic>>();
		if (possibleTopics!=null) {
			for(String topic:possibleTopics) {
				List<DialogueOperatorTopic> topicNodes = getAllTopicsForString(topic);
				if (topicNodes!=null) possibleTopicNodes.put(topic, topicNodes);
			}
		}
		Collection<DialogueOperator> ops = getOperators();
		if (ops!=null) {
			for(DialogueOperator op:ops) {
				Set<String> userSas = op.getAllUserTriggers();
				if (userSas!=null) {
					List<DialogueOperatorTopic> topics = op.getTopics();
					for(String usa:userSas) {
						Set<DialogueOperatorTopic> list = data.get(usa);
						if (list==null) data.put(usa, list=new HashSet<DialogueOperatorTopic>());
						for(DialogueOperatorTopic topic:topics) {
							if (topic.containsSomeOfThese(topics))
							list.addAll(topics);
						}
					}
				}
			}
		}
		for(String usa:data.keySet()) {
			System.out.println(usa+": "+data.get(usa));
		}
	}*/
	
	public static void main(String[] args) throws Exception {
		String fileName="C:\\Users\\morbini\\jmNL\\resources\\all-characters\\Afib.-1710744728-C3PO-Generator\\dm\\policy.xml";
		try {
			RewardPolicy rp = new RewardPolicy(DMConfig.WIN_EXE_CONFIG);
			RewardPolicy dp=rp.parseDialoguePolicyFile(fileName);
			HashMap<DialogueOperatorEntranceTransition, Set<DialogueOperatorEntranceTransition>> t = dp.computeMightEnableTable();
			dp.mightEnableTableToGDLGraph(t,"story-met.gdl", true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
