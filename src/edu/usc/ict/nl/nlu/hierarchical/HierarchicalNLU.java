package edu.usc.ict.nl.nlu.hierarchical;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.bus.NLBus;
import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.dm.reward.EventMatcher;
import edu.usc.ict.nl.nlu.ConfusionEntry;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.io.BuildTrainingData;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.PerformanceResult;
import edu.usc.ict.nl.util.Rational;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.Triple;
import edu.usc.ict.nl.util.graph.Node;
import edu.usc.ict.nl.utils.ExcelUtils;

public class HierarchicalNLU extends NLU {

	private static final String ROOTNODENAME = "root";
	private HashMap<String, NLU> hnlu=null;

	public HierarchicalNLU(NLUConfig c) throws Exception {
		super(c);
		setHNLU(buildsHierNLUFromModel(new File(c.getNluModelFile())));
	}
	private void setHNLU(HashMap<String, NLU> hnlu) {this.hnlu=hnlu;}
	private HashMap<String, NLU> getHNLU() {return hnlu;}
	@Override
	public void train(File trainingFile, File modelFile) throws Exception {
		if (!trainingFile.isAbsolute()) trainingFile=new File(getConfiguration().getNLUContentRoot(),trainingFile.getPath());
		if (!modelFile.isAbsolute()) modelFile=new File(getConfiguration().getNLUContentRoot(),modelFile.getPath());

		BuildTrainingData btd=getBTD();
		List<TrainingDataFormat> td=btd.buildTrainingDataFromNLUFormatFile(trainingFile);

		td=btd.cleanTrainingData(td);

		trainNLUOnThisData(td, trainingFile, modelFile);
	}
	@Override
	public void train(List<TrainingDataFormat> input, File model) throws Exception {
		NLUConfig config=getConfiguration();
		trainNLUOnThisData(input, new File(config.getNluTrainingFile()), model);
	}

	@Override
	public PerformanceResult test(File testFile, File modelFile, boolean printErrors) throws Exception {
		if (!testFile.isAbsolute()) testFile=new File(getConfiguration().getNLUContentRoot(),testFile.getPath());
		if (!modelFile.isAbsolute()) modelFile=new File(getConfiguration().getNLUContentRoot(),modelFile.getPath());
		BuildTrainingData btd=getBTD();
		List<TrainingDataFormat> td=btd.buildTrainingDataFromNLUFormatFile(testFile);

		td=btd.cleanTrainingData(td);

		return testNLUOnThisData(td, modelFile, printErrors);
	}
	@Override
	public PerformanceResult test(List<TrainingDataFormat> test, File model,
			boolean printErrors) throws Exception {
		return testNLUOnThisData(test, model, printErrors);
	}

	@Override
	public boolean isPossibleNLUOutput(NLUOutput o) throws Exception {
		NLU rootNLU = getHNLU().get(ROOTNODENAME);
		return rootNLU.isPossibleNLUOutput(o);
	}
	@Override
	public HashSet<String> getAllSimplifiedPossibleOutputs() throws Exception {
		HashSet<String> ret=null;
		HashMap<String, NLU> hnlu = getHNLU();
		if (hnlu!=null) {
			for(NLU nlu:hnlu.values()) {
				Set<String> allRs = nlu.getAllSimplifiedPossibleOutputs();
				if (allRs!=null) {
					if (ret==null) ret=new HashSet<String>();
					ret.addAll(allRs);
				}
			}
		}
		Map<String, String> hardLinks = getHardLinksMap();
		if (hardLinks!=null) for(String label:hardLinks.values()) {
			if (ret==null) ret=new HashSet<String>();
			ret.add(label);
		}
		return ret;
	}

	@Override
	public boolean nluTest(TrainingDataFormat testSample, List<NLUOutput> nluResults) throws Exception {
		return (nluResults!=null && !nluResults.isEmpty() && testSample.match(nluResults.get(0).getId()));
	}
	public PerformanceResult testNLUOnThisData(List<TrainingDataFormat> testing,File model,boolean printMistakes) throws Exception {
		HashMap<String,NLU> hnlu=getHNLU();
		if (model!=null) setHNLU(buildsHierNLUFromModel(model,false));
		PerformanceResult result=new PerformanceResult();
		for(TrainingDataFormat td:testing) {
			List<NLUOutput> sortedNLUOutput = getNLUOutput(td.getUtterance(),null,null);
			if (nluTest(td, sortedNLUOutput)) {
				result.add(true);
			} else {
				result.add(false);
				if (printMistakes) {
					if (sortedNLUOutput==null || sortedNLUOutput.isEmpty()) logger.error("'"+td.getUtterance()+"' ("+td.getLabel()+") -> NOTHING");
					else logger.error("'"+td.getUtterance()+"' ("+td.getLabel()+") ->"+sortedNLUOutput.get(0));
				}
			}
		}
		if (model!=null) {
			kill();
			setHNLU(hnlu);
		}
		return result;
	}

	@Override
	public void kill() throws Exception {
		HashMap<String,NLU> hnlu=getHNLU();
		if (hnlu!=null) {
			for(NLU nlu:hnlu.values()) {
				if (nlu!=null) nlu.kill();
			}
		}
	}

	@Override
	public List<NLUOutput> getNLUOutputFake(String[] nluOutputIDs,String inputText) throws Exception {
		NLU rootNLU = getHNLU().get(ROOTNODENAME);
		if (rootNLU==null) throw new Exception(" missing root NLU in hierarchical NLU: '"+ROOTNODENAME+"'");
		return rootNLU.getNLUOutputFake(nluOutputIDs, inputText);
	}
	@Override
	public Map<String, Object> getPayload(String sa, String text)
			throws Exception {
		NLU rootNLU = getHNLU().get(ROOTNODENAME);
		if (rootNLU==null) throw new Exception(" missing root NLU in hierarchical NLU: '"+ROOTNODENAME+"'");
		return rootNLU.getPayload(sa, text);
	}

	@Override
	public List<NLUOutput> getNLUOutput(String text,Set<String> possibleNLUOutputIDs,Integer nBest) throws Exception {
		NLUConfig config=getConfiguration();

		String emptyEvent=getConfiguration().getEmptyTextEventName();
		if (StringUtils.isEmptyString(text) && !StringUtils.isEmptyString(emptyEvent)) {
			List<NLUOutput> ret=new ArrayList<NLUOutput>();
			ret.add(new NLUOutput(text, emptyEvent, 1, null));
			return ret;
		}

		NLUOutput hardLabel=getHardLinkMappingOf(text);
		if (hardLabel!=null) {
			List<NLUOutput> ret=new ArrayList<NLUOutput>();
			ret.add(hardLabel);
			return ret;
		}

		List<Pair<NLUOutput,NLU>> iResult=getHierarchyClassificationResults(text, possibleNLUOutputIDs,nBest);

		List<NLUOutput> result=null;
		if (iResult!=null && !iResult.isEmpty()) {
			result=(List) FunctionalLibrary.map(iResult, Pair.class.getMethod("getFirst"));
		} else {
			String lowConfidenceEvent=config.getLowConfidenceEvent();
			if (StringUtils.isEmptyString(lowConfidenceEvent)) {
				logger.warn(" no user speech acts left and LOW confidence event disabled, returning no NLU results.");
			} else {
				if (result==null) result=new ArrayList<NLUOutput>();
				result.add(new NLUOutput(text,lowConfidenceEvent,1f,null));
				logger.warn(" no user speech acts left. adding the low confidence event.");
			}
		}
		return result;
	}

	private NLU getNLUForLabel(String label) {
		if (hnlu==null || hnlu.isEmpty()) return null;
		else {
			Set<String> labels = hnlu.keySet();
			NLU best=hnlu.get(ROOTNODENAME);
			int length=0;
			for(String l:labels) {
				if (label.startsWith(l)) {
					if (l.length()>length) {
						best=hnlu.get(l);
						length=l.length();
					}
				}
			}
			return best;
		}
	}

	private List<Pair<NLUOutput, NLU>> getHierarchyClassificationResults(String text, Set<String> possibleNLUOutputIDs,Integer nBest) throws Exception {
		NLUConfig config=getConfiguration();
		//nBest=(nBest==null)?config.getnBest():nBest;
		boolean returnNonLeaves=config.getHierNluReturnsNonLeaves();
		HashMap<String,NLU> hnlu= getHNLU();
		List<Pair<NLUOutput,NLU>> result=null;
		if (hnlu==null) return null;
		NLU rootNLU = hnlu.get(ROOTNODENAME);
		if (rootNLU==null) throw new Exception(" missing root NLU in hierarchical NLU: '"+ROOTNODENAME+"'");

		Queue<Triple<NLU,Rational,NLUOutput>> q=new LinkedList<Triple<NLU,Rational,NLUOutput>>();
		q.add(new Triple<NLU,Rational,NLUOutput>(rootNLU,Rational.one,null));
		while(!q.isEmpty()) {
			Triple<NLU,Rational,NLUOutput> n=q.poll();
			if (logger.isDebugEnabled()) logger.debug("considering node: "+n.getFirst().getConfiguration().getNluModelFile()+" "+n);
			NLU cNLU=n.getFirst();
			Rational pp=n.getSecond();
			List<NLUOutput> presult=cNLU.getNLUOutput(text, possibleNLUOutputIDs,nBest);
			boolean added=false;
			if (presult!=null) {
				for(NLUOutput r:presult) {
					String id=r.getId();
					if (!StringUtils.isEmptyString(id) && !id.equals(config.getLowConfidenceEvent())) {
						NLU pNLU=hnlu.get(id);
						//Rational cp=pp.times(r.getProb());
						Rational cp=Rational.min(pp,r.getProb());
						added=true;
						if (pNLU!=null && cNLU!=pNLU) q.add(new Triple<NLU,Rational,NLUOutput>(pNLU,cp,r));
						else {
							// leaf node
							if (result==null) result=new ArrayList<Pair<NLUOutput,NLU>>();
							r.setProb(cp);
							result.add(new Pair<NLUOutput, NLU>(r, cNLU));
						}
					}
				}
			}
			if (!added && returnNonLeaves) {
				NLUOutput pR=n.getThird();
				if (pR!=null && cNLU!=rootNLU) {
					pR.setProb(pp.times(pR.getProb()));
					pR.setIsLeaf(false);
					if (result==null) result=new ArrayList<Pair<NLUOutput,NLU>>();
					result.add(new Pair<NLUOutput, NLU>(pR, cNLU));
				}
			}
		}
		if (result!=null && !result.isEmpty()) {
			Collections.sort(result, new Comparator<Pair<NLUOutput, NLU>>() {
				@Override
				public int compare(Pair<NLUOutput, NLU> o1, Pair<NLUOutput, NLU> o2) {
					Rational p1=o1.getFirst().getProb(),p2=o2.getFirst().getProb();
					return p2.compareTo(p1);
				}
			});
		}
		if (result!=null && nBest!=null && nBest < result.size()) {
			for(int j=result.size()-nBest;j>0;j--) {
				result.remove(result.size()-1);
			}
		}
		return result;
	}
	@Override
	public Map<String, Float> getUtteranceScores(String utt,String modelFileName) throws Exception {
		List<Pair<NLUOutput, NLU>> iResult = getHierarchyClassificationResults(utt, null,null);
		if (iResult!=null && !iResult.isEmpty()) {
			NLU nlu=iResult.get(0).getSecond();
			return nlu.getUtteranceScores(utt,modelFileName);
		}
		return null;
	}
	@Override
	public List<Pair<String,Float>> getTokensScoresForLabel(String utt,String label,String modelFileName) throws Exception {
		NLU nlu=getNLUForLabel(label);
		if (nlu!=null) {
			if (modelFileName==null) {
				return nlu.getTokensScoresForLabel(utt, label, nlu.getConfiguration().getNluModelFile());
			} else {
				return nlu.getTokensScoresForLabel(utt, label,modelFileName);
			}
		}
		return null;
	}	
	@Override
	public void loadModel(File nluModel) throws Exception {
		setHNLU(buildsHierNLUFromModel(nluModel));
	}

	private static final Pattern hierModelLine=Pattern.compile("^([^\\s]+)[\\s]+([^\\s]+)$");
	private HashMap<String,NLU> buildsHierNLUFromModel(File modelFile) throws Exception {
		return buildsHierNLUFromModel(modelFile, true);
	}
	private HashMap<String,NLU> buildsHierNLUFromModel(File modelFile,boolean trainIfNoModel) throws Exception {
		String line;
		NLUConfig config=getConfiguration();
		String internalNLUclass = config.getInternalNluClass4Hier();
		HashMap<String,NLU> ret=new HashMap<String, NLU>();
		if (!modelFile.isAbsolute()) modelFile=new File(getConfiguration().getNLUContentRoot(),modelFile.getPath());

		if (!modelFile.exists()) {
			logger.warn("no model found.");
			if (trainIfNoModel) {
				logger.warn("retraining...");
				retrain();
			} else {
				logger.warn("doing nothing.");
				return null;
			}
		}

		try {
			BufferedReader in=new BufferedReader(new FileReader(modelFile));
			while((line=in.readLine())!=null) {
				Matcher m=hierModelLine.matcher(line);
				if (m.matches() && (m.groupCount()==2)) {
					String nodeName=m.group(1);
					File thisNodeModelFile=new File(m.group(2));
					NLUConfig internalConfig=(NLUConfig) config.clone();
					internalConfig.setNluHardLinks(null);
					internalConfig.setNluModelFile(thisNodeModelFile.getName());
					NLU internalNLU = (NLU) NLBus.createSubcomponent(internalConfig,internalNLUclass);
					ret.put(nodeName, internalNLU);
				}
			}
			in.close();
		} catch (Exception e) {
			logger.warn("Error during hierarchical model building.",e);
		}
		return ret;
	}

	private static Method getNameMethod=null;
	static {
		try {
			getNameMethod=Node.class.getMethod("getName");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	private void trainNLUOnThisData(List<TrainingDataFormat> td,File trainingFile, File modelFile) throws Exception {
		NLUConfig config=getConfiguration();
		String internalNLUclass = config.getInternalNluClass4Hier();
		Hnode root = buildDataHierachy(td);
		root=compressHierarchyAt(root,50,2);
		if (logger.isDebugEnabled()) root.toGDLGraph("nlu-hier.gdl");
		HashSet<Node> trainingNodes=root.getReachableNonLeaves();
		// at least the root must be there
		if (trainingNodes==null || trainingNodes.isEmpty()) {
			logger.warn("empty set of intermediate NLU nodes in NLU hierarchy. No training.");
		} else {
			NLUConfig internalConfig=(NLUConfig) config.clone();
			internalConfig.setNluModelFile(null);
			NLU internalNLU = (NLU) NLBus.createSubcomponent(internalConfig,internalNLUclass);
			String newModelContent="";
			for(Node n:trainingNodes) {
				String nodeName=n.getName();
				File modelFileForNode=new File(modelFile.getAbsoluteFile()+"-hier-"+nodeName);
				File trainingFileForNode=new File(trainingFile.getAbsoluteFile()+"-hier-"+nodeName);
				// these are the possible labels in the model learnt for this node.
				Collection<String> possibleLabels = FunctionalLibrary.map(n.getImmediateChildren(), getNameMethod);
				// change training data using only the above labels.
 				List<TrainingDataFormat> ctd = compressTrainingDataBasedOnPossibleLabels(td,n.getName(),possibleLabels);
				logger.info(nodeName+" "+ctd.size()+" utterances with "+BuildTrainingData.getAllSpeechActsInTrainingData(ctd).size()+" labels "+modelFileForNode+" "+trainingFileForNode);
				//System.out.println(nodeName+" "+ctd.size()+" with "+BuildTrainingData.getAllSpeechActsInTrainingData(ctd).size()+" "+modelFileForNode+" "+trainingFileForNode);
				if (!ctd.isEmpty()) {
					dumpTrainingDataToFileNLUFormat(trainingFileForNode, ctd);
					newModelContent+=nodeName+"\t"+modelFileForNode.getName()+"\n";
					internalNLU.train(trainingFileForNode, modelFileForNode);
					if (modelFileForNode.length()==0) throw new Exception("training failed for "+modelFileForNode+" resulting file is empty.");
				} else logger.error(nodeName+" has produced a null compressed training data.");
			}
			BufferedWriter out=new BufferedWriter(new FileWriter(modelFile));
			out.write(newModelContent);
			out.close();
		}
	}

	private List<TrainingDataFormat> compressTrainingDataBasedOnPossibleLabels(List<TrainingDataFormat> td, String parentLabel, Collection<String> possibleLabels) throws Exception {
		List<TrainingDataFormat> retTD=null;
		if (possibleLabels!=null && !possibleLabels.isEmpty()) {
			String separator=getConfiguration().getHierarchicalNluSeparator();
			if (StringUtils.isEmptyString(separator)) throw new Exception("empty separator.");
			EventMatcher<String> mapper=new EventMatcher<String>();
			for(String pl:possibleLabels) {
				if (!StringUtils.isEmptyString(pl)) {
					mapper.addEvent(pl, pl);
					mapper.addEvent(pl+separator+"*", pl);
				}
			}
			mapper.addEvent(parentLabel, parentLabel);
			if (td!=null && !td.isEmpty()) {
				for(TrainingDataFormat ul:td) {
					String label=ul.getLabel();
					String utt=ul.getUtterance();
					Set<String> matches = mapper.match(label);
					if (matches!=null && matches.size()>1) {
						List<String> toRemove=null;
						//remove all strict prefixes (i.e. leave the most specific label)
						for(String m:matches) {
							String prefix=m+separator;
							for(String mm:matches) {
								if(mm.startsWith(prefix)) {
									if (toRemove==null) toRemove=new ArrayList<String>();
									toRemove.add(m);
								}
							}
						}
						if (toRemove!=null) {
							for(String t:toRemove) matches.remove(t);
						}
					}
					// one or nothing in matches
					if (matches!=null && (matches.size()>1)) throw new Exception("ambiguity in compressing training data for label: '"+label+"', matches found: "+matches);
					if (matches!=null && !matches.isEmpty()) {
						if (retTD==null) retTD=new ArrayList<TrainingDataFormat>();
						label=matches.iterator().next();
						retTD.add(new TrainingDataFormat(utt, label));
					}
				}
			}
			return retTD;
		} else return td;
	}
	private Set<String> getAllNodesWithTrainingData(Hnode root) throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
		Set<String> ret=null;
		Queue<Node> q=new LinkedList<Node>();
		q.add(root);
		while(!q.isEmpty()) {
			Hnode n=(Hnode)q.poll();
			if (n.getHasTrainingData()) {
				if (ret==null) ret=new HashSet<String>();
				ret.add(n.getName());
			}
			Collection cs = n.getImmediateChildren();
			if (cs!=null) q.addAll(cs);
		}
		return ret;
	}
	private Hnode compressHierarchyAt(Hnode root,int maxth,int minth) throws Exception {
		Set<String> initSet = getAllNodesWithTrainingData(root);
		Queue<Node> q=new LinkedList<Node>();
		q.add(root);
		while(!q.isEmpty()) {
			Node n=q.poll();
			//System.out.println(n);
			Collection<Node> children=n.getImmediateChildren();
			if (children!=null) {
				// computes the number of first descendants with training datafor a given node
				List<Pair<Node,Integer>> childAndNumLeaves=new ArrayList<Pair<Node,Integer>>();
				HashMap<Node,HashSet<Node>> child2Leaves=new HashMap<Node, HashSet<Node>>();
				int totalLeaves=0;
				for(Node c:children) {
					HashSet<Node> leaves = ((Hnode)c).getFirstDescendantsWithTrainingData();
					child2Leaves.put(c, leaves);
					int thisLeaves;
					childAndNumLeaves.add(new Pair<Node, Integer>(c, thisLeaves=(leaves!=null && !leaves.isEmpty())?leaves.size():0));
					totalLeaves+=thisLeaves;
				}
				//if there are no children or the total number of children is less than the splitting threashold, then
				// attach all descendants with training data directly to this node.
				if (children.isEmpty() || totalLeaves<=maxth) {
					if (n.getOutgoingEdges()!=null) {
						Set<Node> immediateChildrenWithNoTrainingData=new HashSet<Node>(children);
						// attach all leaves to n directly
						for(HashSet<Node> leaves:child2Leaves.values()) {
							if (leaves!=null) {
								immediateChildrenWithNoTrainingData.removeAll(leaves);
								if (totalLeaves>minth) {
									for(Node l:leaves) {
										if (l.hasChildren()) q.add(l);
										if (l.getSingleParent()!=n) {
											if (logger.isDebugEnabled()) logger.debug("attaching "+l+" to current node: "+n);
											l.clearIncomingEdges();
											n.addEdgeTo(l, true, true);
										}
									}
								} else {
									Node p=n.getSingleParent();
									for(Node l:leaves) {
										if (logger.isDebugEnabled()) logger.debug("attaching "+l+" to parent of current node("+n+"): "+p);
										if (l.hasChildren()) q.add(l);
										n.removeEdgeTo(l);
										l.clearIncomingEdges();
										p.addEdgeTo(l, true, true);
									}
								}
							}
						}
						if (!immediateChildrenWithNoTrainingData.isEmpty()) {
							for(Node l:immediateChildrenWithNoTrainingData)
								l.removeEdgeFrom(n);
						}
					}
				} else if (children.size()==1 && children.iterator().next().getImmediateChildren()!=null) {
					// remove this link
					n.clearOutgoingEdges();
					for(Object x:children.iterator().next().getImmediateChildren()) {
						Node cc=(Node)x;
						cc.clearIncomingEdges();
						n.addEdgeTo(cc, true, true);
						q.add(n);
					}
				} else if (children.size()>minth && children.size()<maxth) {
					if (logger.isDebugEnabled()) logger.debug("leaving level rooted at "+n+" as it is.");
					if (logger.isDebugEnabled()) logger.debug("   children: "+children);
					for(int i=0;i<childAndNumLeaves.size();i++) {
						q.add(childAndNumLeaves.get(i).getFirst());
					}
				} else {
					Collections.sort(childAndNumLeaves, new Comparator<Pair<Node, Integer>>() {
						@Override
						public int compare(Pair<Node, Integer> arg0,Pair<Node, Integer> arg1) {
							return arg0.getSecond().compareTo(arg1.getSecond());
						}
					});
					Collection<Integer> numbers = FunctionalLibrary.map(childAndNumLeaves, Pair.class.getMethod("getSecond"));
					List<Double> deltas=FunctionalLibrary.derivative(numbers);
					deltas=FunctionalLibrary.derivative(deltas);
					int sumLabelsToBeIncludedInSingleClassifier=0;
					int iter=1;
					int posMax=-1;
					do{
						if (posMax>0) {
							if (logger.isDebugEnabled()) logger.debug("iter="+iter+", setting previous max to 0: "+deltas.get(posMax));
							deltas.set(posMax, 0d);
						} else if (posMax==0) {
							throw new Exception("max found at position 0 but sum of labels larger than threashold, impossible to compress hier.");
						}
						posMax=FunctionalLibrary.findPosMax(deltas);
						if (logger.isDebugEnabled()) logger.debug("iter="+iter+", pos of max delta="+posMax+": input numbers: "+FunctionalLibrary.printCollection(numbers, "", "", " "));
						sumLabelsToBeIncludedInSingleClassifier=0;
						iter++;
						for(int i=0;i<posMax;i++) sumLabelsToBeIncludedInSingleClassifier+=childAndNumLeaves.get(i).getSecond();
					} while (sumLabelsToBeIncludedInSingleClassifier>=maxth);
					for(int i=0;i<posMax;i++) {
						Node child=childAndNumLeaves.get(i).getFirst();
						if (logger.isDebugEnabled()) logger.debug("removing edge "+n+"->"+child);
						n.removeEdgeTo(child);
						HashSet<Node> leaves = child2Leaves.get(child);
						if (leaves!=null) {
							for(Node l:leaves) {
								l.clearIncomingEdges();
								n.addEdgeTo(l, true, true);
								q.add(l);
							}
						}
					}
					for(int i=posMax;i<childAndNumLeaves.size();i++) {
						q.add(childAndNumLeaves.get(i).getFirst());
					}
				}
			}
		}
		Set<String> endSet=getAllNodesWithTrainingData(root);
		if (!endSet.containsAll(initSet) || !initSet.containsAll(endSet)) {
			throw new Exception("invalid hierarchy transformation:\n+"+initSet+"\n"+endSet);
		}
		return root;
	}

	private Hnode buildDataHierachy(List<TrainingDataFormat> td) throws Exception {
		HashMap<String, Hnode> nodesCreated;
		// get training data, extract all possible labels
		// builds tree based on names of labels
		//  delete non-leaf children of a node if the number if number of children is below certain threashold
		//  keep non-leaf child if it has enough children
		Hnode root = new Hnode(ROOTNODENAME);
		Set<String> sas=getBTD().getAllSpeechActsInTrainingData(td);
		if (sas!=null && !sas.isEmpty()) {
			String separator=getConfiguration().getHierarchicalNluSeparator();
			if (StringUtils.isEmptyString(separator)) throw new Exception("empty separator.");
			nodesCreated=new HashMap<String,Hnode>();
			for(String sa:sas) {
				String remainder=sa;
				String nodeName="";
				Node source=root;
				while(remainder!=null) {
					String portion;
					int pos=remainder.indexOf(separator);

					if (pos<0) portion=remainder;
					else if (pos==0) {
						remainder=remainder.substring(separator.length());
						continue;
					} else portion=remainder.substring(0,pos);
					nodeName+=portion;

					Hnode target=nodesCreated.get(nodeName);
					if (target==null) nodesCreated.put(nodeName, target=new Hnode(nodeName));
					if (!source.hasEdgeTo(target)) source.addEdgeTo(target, true, true);
					source=target;

					if (remainder.length()<=(portion.length()+separator.length())) remainder=null;
					else remainder=remainder.substring(portion.length()+separator.length());
					nodeName+=separator;

					if (remainder==null) target.setHasTrainingData(true);
				}
			}
		}
		return root;
	}

	public static void main(String[] args) throws Exception {
		NLUConfig config=(NLUConfig) NLUConfig.WIN_EXE_CONFIG.clone();
		config.setForcedNLUContentRoot("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\nlu");

		//config.setInternalNluClass4Hier("edu.usc.ict.nl.nlu.jmxnlu.JMXClassifierNLU");
		//config.setNluClass("edu.usc.ict.nl.nlu.jmxnlu.JMXClassifierNLU");
		//config.setNluFeaturesBuilderClass("edu.usc.ict.nl.nlu.features.FeatureBuilderForJMXClassifier");
		//config.setAcceptanceThreshold(null);
		//config.setInternalNluClass4Hier("edu.usc.ict.nl.nlu.mxnlu.MXClassifierNLU");
		//config.setInternalNluClass4Hier("edu.usc.ict.nl.nlu.mallet.MalletMaxEntClassifierNLU");
		//config.setNluClass("edu.usc.ict.nl.nlu.hierarchical.HierarchicalNLU");
		//config.setNluClass("edu.usc.ict.nl.nlu.mxnlu.MXClassifierNLU");
		config.setNluClass("edu.usc.ict.nl.nlu.mallet.MalletMaxEntClassifierNLU");
		//config.setNluClass("edu.usc.ict.nl.nlu.jmxnlu.JMXClassifierNLU");
		//config.setNluFeaturesBuilderClass("edu.usc.ict.nl.nlu.features.FeaturesBuilderForMXClassifier");
		config.setNluFeaturesBuilderClass("edu.usc.ict.nl.nlu.features.FeatureBuilderForJMXClassifier");
		config.setAcceptanceThreshold(null);
		//config.setNluClass("edu.usc.ict.nl.nlu.mallet.MalletMaxEntClassifierNLU");

		config.setHierNluReturnsNonLeaves(false);
		config.setHierarchicalNluSeparator(".");
		config.setnBest(3);
		NLU nlu=(NLU) NLBus.createSubcomponent(config, config.getNluClass());
		//System.exit(1);
		//List<TrainingDataFormat> test=BuildTrainingData.buildTrainingDataFromNLUFormatFile(new File("\\\\netapp2\\simcoach\\support\\harvests\\Bill_Ford_PB\\annotations\\none-annotation\\merged.txt"), false);
		//List<TrainingDataFormat> td = BuildTrainingData.buildTrainingDataFromExcel("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\nlu\\user-utterances.xlsx", 0);
		List<TrainingDataFormat> td = BuildTrainingData.readExcelData("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\nlu\\user-utterances-kenji-only.xlsx", 0);
		List<TrainingDataFormat> atd = BuildTrainingData.readExcelData("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\nlu\\user-utterances-only-added.xlsx", 0);
		List<TrainingDataFormat> ctd = BuildTrainingData.readExcelData("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\nlu\\user-utterances-DDA.xlsx", 0);
		
		
		//List<TrainingDataFormat> td=nlu.getBTD().buildTrainingData();
		nlu.train(ctd, new File(config.getNluModelFile()));

		//nlu.retrain();
		//nlu.kill();
		//nlu.loadModel(new File(config.getNluModelFile()));
		//PerformanceResult p1 = nlu.test(test, new File(config.getNluModelFile()), false);
		//System.out.println(p1);
		nlu.computeConfusionMatrix(ctd);
		//PerformanceResult p1 = nlu.test(test, new File(config.getNluModelFile()), false);
		//System.out.println(p1);
		System.exit(1);
		int i=1;
		for(TrainingDataFormat t:td) {
			System.out.println(i+" "+t.getUtterance()+" "+t.getLabel());
			System.out.println(nlu.getNLUOutput(t.getUtterance(), null, 3));
			i++;
		}
		//System.out.println(nlu.getTokensScoresForLabel("tomorrow at nine am", "answer.yes"));
		//System.out.println(nlu.getTokensScoresForLabel("tomorrow at nine am", "statement.time-of-flight"));
		//System.out.println(nlu.getUtteranceScores("tomorrow at nine am"));
		System.out.println(nlu.getNLUOutput("tomorrow at nine am", null,null));
		System.exit(1);
		Map<String, ConfusionEntry> p = nlu.computeConfusionMatrix();
		NLU.printConfusionMatrix(System.out,p);
		System.exit(1);

		/*
		System.out.println(nlu.getNLUOutput("yes", null));
		System.out.println(nlu.getNLUOutput("a cake please", null));
		System.out.println(nlu.getNLUOutput("a sponge cake please", null));

		System.out.println(nlu.getAllSimplifiedPossibleOutputs());

		System.exit(0);
		 */
		BuildTrainingData btd = nlu.getBTD();
		List<TrainingDataFormat> data = btd.buildTrainingData();
		Collection<String> data2 = ExcelUtils.extractValuesInThisColumn(config.getNLUContentRoot()+File.separator+"user-utterances-from-user-data-collection.xlsx", 0, 5);
		for(String s:data2) data.add(new TrainingDataFormat(s, "nothing"));
		//btd.keepOnlyTheseLabels(data,new String[]{"answer.yes","answer.no"});
		File lmText=new File("lm-data.txt"),arpa=new File("lm-dcaps-ai-1.arpa");
		btd.dumpTextFromTrainingDataForLM(data,lmText);
		btd.buildArpaLM(lmText, arpa);
		/*
		Hnode root = nlu.buildDataHierachy(data);
		root=nlu.compressHierarchyAt(root, 1);
		root.toGDLGraph("nlu-hier.gdl");

		//nlu.retrain();
		//nlu.loadModel(new File(config.getNluModelFile()));
		System.out.println(nlu.getNLUOutput("45", null));
		 */
	}
}
