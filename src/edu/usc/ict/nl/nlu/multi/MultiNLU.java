package edu.usc.ict.nl.nlu.multi;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.usc.ict.nl.bus.NLBus;
import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLConfig;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.BuildTrainingData;
import edu.usc.ict.nl.nlu.ChartNLUOutput;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.multi.merger.Merger;
import edu.usc.ict.nl.util.PerformanceResult;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.graph.Node;

public class MultiNLU extends NLU {

	private Map<String,NLU> name2nluInstance=null;
	private Map<String, NLUConfig> name2nluClass=null;
	private Merger merger=null;

	public MultiNLU(NLUConfig c) throws Exception {
		super(c);
		name2nluClass=c.getInternalNluListForMultiNlu();
		merger=c.getMergerForMultiNlu();
		setHNLU(buildsHierNLUFromModel());
	}
	private void setHNLU(Map<String, NLU> hnlu) {this.name2nluInstance=hnlu;}
	public Map<String, NLU> getHNLU() {return name2nluInstance;}
	
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
		if (getHNLU()!=null) {
			for(NLU nlu:getHNLU().values()) {
				if (nlu.isPossibleNLUOutput(o)) return true;
			}
		}
		return false;
	}
	@Override
	public HashSet<String> getAllSimplifiedPossibleOutputs() throws Exception {
		HashSet<String> ret=null;
		Map<String, NLU> hnlu = getHNLU();
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
		Map<String,NLU> hnlu=getHNLU();
		setHNLU(buildsHierNLUFromModel());
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
		kill();
		setHNLU(hnlu);
		return result;
	}
	
	@Override
	public void kill() throws Exception {
		Map<String,NLU> hnlu=getHNLU();
		if (hnlu!=null) {
			for(NLU nlu:hnlu.values()) {
				if (nlu!=null) nlu.kill();
			}
		}
	}

	@Override
	public List<NLUOutput> getNLUOutputFake(String[] nluOutputIDs,String inputText) throws Exception {
		Map<String,NLU> hnlu=getHNLU();
		if (hnlu!=null) {
			for(NLU nlu:hnlu.values()) {
				if (nlu!=null) {
					List<NLUOutput> r = null;
					try {
						r = nlu.getNLUOutputFake(nluOutputIDs,inputText);
					} catch (Exception e) {}
					if (r!=null) return r;
				}
			}
		}
		return null;
	}
	@Override
	public Map<String, Object> getPayload(String sa, String text)
			throws Exception {
		Map<String,NLU> hnlu=getHNLU();
		if (hnlu!=null) {
			for(NLU nlu:hnlu.values()) {
				if (nlu!=null) {
					Map<String, Object> p = nlu.getPayload(sa,text);
					if (p!=null) return p;
				}
			}
		}
		return null;
	}
	@Override
	public List<NLUOutput> getNLUOutput(String text,Set<String> possibleNLUOutputIDs,Integer nBest) throws Exception {

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
		
		NLUConfig config=getConfiguration();
		ChartNLUOutput result=null;
		if (name2nluInstance!=null) {
			for(String nluName:name2nluInstance.keySet()) {
				NLU nlu=name2nluInstance.get(nluName);
				if (logger.isDebugEnabled()) logger.debug("considering NLU named: "+nluName);
				List<NLUOutput> presult=nlu.getNLUOutput(text, possibleNLUOutputIDs,nBest);
				if (presult!=null) {
					NLUOutput r=presult.get(0);
					String id=r.getId();
					if (!StringUtils.isEmptyString(id)) { // && !id.equals(config.getLowConfidenceEvent())) {
						r.setNluID(nluName);
						if (result==null) result=new ChartNLUOutput(text, null);
						result.addPortion(0, 0, r);
					}
				}
			}
		} else return null;
		
		result=(merger!=null)?merger.mergeResults(result):result;
		
		if (result==null) {
			String lowConfidenceEvent=config.getLowConfidenceEvent();
			if (StringUtils.isEmptyString(lowConfidenceEvent)) {
				logger.warn(" no user speech acts left and LOW confidence event disabled, returning no NLU results.");
			} else {
				if (result==null) result=new ChartNLUOutput(text, null);
				result.addPortion(0,0,new NLUOutput(text,lowConfidenceEvent,1f,null));
				logger.warn(" no user speech acts left. adding the low confidence event.");
			}
		}
		List<NLUOutput> listResult=new ArrayList<NLUOutput>();
		listResult.add(result);
		return listResult;
	}
	
	@Override
	public void loadModel(File nluModel) throws Exception {
	}
	
	private HashMap<String,NLU> buildsHierNLUFromModel() throws Exception {
		HashMap<String,NLU> ret=new HashMap<String, NLU>();
		for(String nodeName:name2nluClass.keySet()) {
			NLUConfig internalConfig=(NLUConfig) name2nluClass.get(nodeName);
			if (internalConfig!=null) {
				internalConfig.nlBusConfig=getConfiguration().nlBusConfig;
				NLU internalNLU = (NLU) NLBus.createSubcomponent(internalConfig,internalConfig.getNluClass());
				ret.put(nodeName, internalNLU);
			}
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
	
	@Override
	public void retrain() throws Exception {
		Map<String, NLU> hnlu = getHNLU();
		logger.info("Starting multi nlu training...");
		if (hnlu!=null) {
			for(String nluName:hnlu.keySet()) {
				NLU nlu=hnlu.get(nluName);
				logger.info("Retraining nlu named: "+nluName);
				nlu.retrain();
			}
		}
		logger.info("Done multi nlu training.");
	}
	
	public static void main(String[] args) throws Exception {
		NLUConfig config=(NLUConfig) NLUConfig.WIN_EXE_CONFIG.clone();
		config.setNluClass("edu.usc.ict.nl.nlu.multi.MultiNLU");
		Map<String,NLUConfig> nlus=new HashMap<String, NLUConfig>();
		NLUConfig topicConfig=(NLUConfig) NLUConfig.WIN_EXE_CONFIG.clone();
		topicConfig.setNluClass("edu.usc.ict.nl.nlu.topic.WordlistTopicDetection");
		topicConfig.setNluModelFile("topic-models");
		topicConfig.setApplyTransformationsToInputText(false);
		nlus.put("topic", topicConfig);
		NLUConfig classifierConfig=(NLUConfig) NLUConfig.WIN_EXE_CONFIG.clone();
		classifierConfig.setNluClass("edu.usc.ict.nl.nlu.hierarchical.HierarchicalNLU");
		classifierConfig.setInternalNluClass4Hier("edu.usc.ict.nl.nlu.mxnlu.MXClassifierNLU");
		classifierConfig.setNluFeaturesBuilderClass("edu.usc.ict.nl.nlu.features.FeaturesBuilderForMXClassifier");
		classifierConfig.setAcceptanceThreshold(null);
		classifierConfig.setHierNluReturnsNonLeaves(false);
		classifierConfig.setHierarchicalNluSeparator(".");
		classifierConfig.setnBest(3);

		nlus.put("classifier", classifierConfig);
		config.setInternalNluListForMultiNlu(nlus);
		config.setAcceptanceThreshold(null);
		//config.setDefaultCharacter("Ellie_DCAPS");
		MultiNLU nlu = new MultiNLU(config);
		
		System.out.println(nlu.getNLUOutput("yes", null,null));
		System.out.println(nlu.getNLUOutput("yes but i'm depressed", null,null));
		System.out.println(nlu.getNLUOutput("i'm low energy", null,null));
	}
}
