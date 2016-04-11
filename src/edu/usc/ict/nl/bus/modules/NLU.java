package edu.usc.ict.nl.bus.modules;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.nlu.ConfusionEntry;
import edu.usc.ict.nl.nlu.FoldsData;
import edu.usc.ict.nl.nlu.Model;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.io.BuildTrainingData;
import edu.usc.ict.nl.nlu.ne.BasicNE;
import edu.usc.ict.nl.nlu.ne.NE;
import edu.usc.ict.nl.nlu.ne.NamedEntityExtractorI;
import edu.usc.ict.nl.nlu.preprocessing.Preprocess;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.PerformanceResult;
import edu.usc.ict.nl.util.ProgressTracker;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.utils.LogConfig;

public abstract class NLU implements NLUInterface {

	private NLUConfig configuration;
	private BuildTrainingData btd;
	private Preprocess trainingPreprocess,runningPreprocess;
	private Map<String, String> hardLinkMap;
	private Method featuresBuilder,featuresAtPosBuilder;
	private static NLU _instance;
	protected Float acceptanceThreshold;

	protected static final Logger logger = Logger.getLogger(NLU.class.getName());
	static {
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );
	}

	public NLU(NLUConfig c) throws Exception {
		_instance = this;
		this.configuration=c;
		setBTD(new BuildTrainingData(c));
		setPreprocess(new Preprocess(this,PreprocessingType.TRAINING));
		setPreprocess(new Preprocess(this,PreprocessingType.RUN));
		hardLinkMap=getBTD().buildHardLinksMap();
		featuresBuilder=Class.forName(c.getNluFeaturesBuilderClass()).getMethod("buildfeaturesFromUtterance", String.class);
		featuresAtPosBuilder=Class.forName(c.getNluFeaturesBuilderClass()).getMethod("buildFeatureForWordAtPosition", String[].class,int.class);
		configureNamedEntityExtractors();
		Boolean inAdviserMode=configuration.isInAdvicerMode();
		setAcceptanceThreshold((inAdviserMode!=null && inAdviserMode)?null:configuration.getAcceptanceThreshold());
	}
	
	public void setAcceptanceThreshold(Float t) {this.acceptanceThreshold=t;}

	public Preprocess getPreprocess(PreprocessingType type) {
		switch (type) {
		case RUN: return runningPreprocess;
		case TRAINING: return trainingPreprocess;
		}
		return null;
	}
	public void setPreprocess(Preprocess preprocess) {
		PreprocessingType type=preprocess.getType();
		switch (type) {
		case RUN:
			this.runningPreprocess=preprocess;
			break;
		case TRAINING:
			this.trainingPreprocess=preprocess;
			break;
		}
	}
	public void setRunningPreprocess(Preprocess runningPreprocess) {
		this.runningPreprocess = runningPreprocess;
	}
	
	private void configureNamedEntityExtractors() {
		configureNamedEntityExtractors(PreprocessingType.RUN);	
		configureNamedEntityExtractors(PreprocessingType.TRAINING);	
	}
	private void configureNamedEntityExtractors(PreprocessingType type) {
		NLUConfig config=getConfiguration();
		List<NamedEntityExtractorI> nes=config.getNluNamedEntityExtractors(type);
		if (nes!=null) {
			for(NamedEntityExtractorI ne:nes)  {
				ne.setConfiguration(config);
			}
		}
	}
	
	@Override
	public void setBTD(BuildTrainingData btd) {this.btd = btd;}
	@Override
	public BuildTrainingData getBTD() {return btd;}
	
	public NLUConfig getConfiguration() {return configuration;}

	public NLUOutput getHardLinkMappingOf(String text) throws Exception {
		String emptyLineEvent=getConfiguration().getEmptyTextEventName();
		if (StringUtils.isEmptyString(text)) {
			getLogger().info("Empty line received.");
			if (!StringUtils.isEmptyString(emptyLineEvent)) {
				getLogger().info("Sending special '"+emptyLineEvent+"' event.");
				return new NLUOutput(text, emptyLineEvent, 1f, null);
			}
		}
		Map<String, String> hlm = getHardLinksMap();
		if (hlm!=null && hlm.containsKey(text)) {
			String label=hlm.get(text);
			return new NLUOutput(text, label, 1f, null);
		} else return null;
	}

	public Map<String, String> getHardLinksMap() {return this.hardLinkMap;}

	@Override
	public List<NLUOutput> getNLUOutput(String text,
			Set<String> possibleNLUOutputIDs,Integer nBest) throws Exception {
		throw new Exception("unhandled");
	}

	@Override
	public List<NLUOutput> getNLUOutputFake(String[] nluOutputIDs,String inputText) throws Exception {
		List<NLUOutput> ret=new ArrayList<NLUOutput>();
		List<List<Token>> options=null;
		Preprocess pr=getPreprocess(PreprocessingType.RUN);
		if (!StringUtils.isEmptyString(inputText)) {
			options = pr.process(inputText);
			sortOptionsByText(options,pr);
		}

		List<NLUOutput> userSpeechActsWithProb = processNLUOutputs(nluOutputIDs,null,null,null);		

		if (userSpeechActsWithProb!=null) {
			for(int i=0;i<userSpeechActsWithProb.size();i++) {
				NLUOutput o=userSpeechActsWithProb.get(0);
				boolean added=false;
				if (options!=null && !options.isEmpty()) {
					for(List<Token> option:options) {
						if (Preprocess.hasAssociatedNamedEntities(option)) {
							String t=pr.getString(option);
							o.setText(t);
							List<NE> fnes=BasicNE.filterNESwithSpeechAct(option,o.getId(),pr.getTokenizer());
							Map<String, Object> payload = BasicNE.createPayload(fnes);
							o.setPayload(payload);
							if (ret==null) ret=new ArrayList<>();
							ret.add(o);
							userSpeechActsWithProb.set(i, new NLUOutput(o.getText(), o.getId(), o.getProb().floatValue(), null));
							added=true;
						}
					}
				}
				if (!added) {
					if (ret==null) ret=new ArrayList<>();
					ret.add(o);
				}
			}
		}
		return ret;
	}
	/**
	 * options may differ just because of different named entities recognized altough superficially the text is the same.
	 * to avoid computing features (for classifiers) over and over for the same text, this sorting routine can be used together
	 * with some more intelligent code.
	 * @param options
	 */
	protected void sortOptionsByText(List<List<Token>> options,final Preprocess pr) {
		Collections.sort(options, new Comparator<List<Token>>(){
			@Override
			public int compare(List<Token> o1, List<Token> o2) {
				String t1=pr.getString(o1);
				String t2=pr.getString(o2);
				return t1.compareTo(t2);
			}
		});
	}
	/*
	@Override
	public List<NLUOutput> getNLUOutputFake(String[] NLUOutputIDs, String text)
			throws Exception {
		throw new Exception("unhandled");
	}
*/
	@Override
	public void kill() throws Exception {
		throw new Exception("unhandled");
	}

	@Override
	public void loadModel(File nluModel) throws Exception {
		throw new Exception("unhandled");
	}
	
	@Override
	public void retrain() throws Exception {
		NLUConfig c=getConfiguration();
		BuildTrainingData btd=getBTD();
		List<TrainingDataFormat> td=btd.buildTrainingData();
		File trainingFile=new File(c.getNluTrainingFile());
		if (td!=null && !td.isEmpty()) {
			File nluModel=new File(c.getNluModelFile());
			dumpTrainingDataToFileNLUFormat(trainingFile, td);
			train(trainingFile, nluModel);
		}
	}
	@Override
	public void retrain(File... files) throws Exception {
		NLUConfig c=getConfiguration();
		BuildTrainingData btd=getBTD();
		if (files!=null && files.length>0) {
			List<TrainingDataFormat> all=new ArrayList<TrainingDataFormat>();
			for(File file:files) {
				List<TrainingDataFormat> td = btd.readData(file.getAbsolutePath());
				if (td!=null) all.addAll(td);
			}
			File trainingFile=new File(c.getNluTrainingFile());
			if (all!=null && !all.isEmpty()) {
				File nluModel=new File(c.getNluModelFile());
				dumpTrainingDataToFileNLUFormat(trainingFile, all);
				train(trainingFile, nluModel);
			}
		}
	}
	
	public void dumpTrainingDataToFileNLUFormat(File trainingFile,List<TrainingDataFormat> td) throws Exception {
		btd=getBTD();
		if (trainingFile.exists()) trainingFile.delete();
		List<TrainingDataFormat> preparedTrainingData=prepareTrainingDataForClassification(td);
        BufferedWriter outputStream = new BufferedWriter(new FileWriter(trainingFile));
		for(TrainingDataFormat row:preparedTrainingData) {
			outputStream.write(row.toNluformat(this));
		}
		outputStream.close();
	}
	
	private static final Pattern question=Pattern.compile("(.+\\.)?question(\\..+)?");
	public static boolean isQuestion(String id) {
		Matcher m=question.matcher(id);
		return m.matches();
	}

	public Map<String,ConfusionEntry> computeConfusionMatrix() throws Exception {
		return computeConfusionMatrix(null);
	}
	public Map<String,ConfusionEntry> computeConfusionMatrix(List<TrainingDataFormat> data) throws Exception {
		return computeConfusionMatrix(data,null);
	}
	public Map<String,ConfusionEntry> computeConfusionMatrix(List<TrainingDataFormat> data,List<TrainingDataFormat> additionalTraining) throws Exception {
		// for each speech act in the training data
		//  record total number of utterances for it, for each mistake, number of time it happened and what speech act was selected instead.
		Map<String,ConfusionEntry> ret=new HashMap<String, ConfusionEntry>();
		BuildTrainingData btd=getBTD();
		List<TrainingDataFormat> trainingData = (data!=null)?data:btd.buildTrainingData(),testingData=null;
		
		Integer numFolds=10;
		FoldsData td4Fold = btd.produceDynamicFolds(trainingData, numFolds);
		numFolds=td4Fold.getNumberOfFolds();
		
		NLUConfig config = getConfiguration();
		String baseNLUmodel=config.getNluModelFile();
		PerformanceResult total=new PerformanceResult();
		for(int currentFold=0;currentFold<numFolds;currentFold++) {
			trainingData = td4Fold.buildTrainingDataForFold(currentFold,trainingData);
			testingData = td4Fold.buildTestingDataForFold(currentFold,testingData);
			if (additionalTraining!=null) trainingData.addAll(additionalTraining);
			
			File model=File.createTempFile("confusion-model-"+currentFold, ".mod", new File(config.getNLUContentRoot()));
			train(trainingData, model);
			kill();
			loadModel(model);
			PerformanceResult testPerformance=test(testingData, model, false);
			System.out.println("fold: "+currentFold+" performance: "+testPerformance);
			model.delete();
			total.add(testPerformance);
			
			Map<String, List<TrainingDataFormat>> sas = btd.getAllSpeechActsWithTrainingData(testingData);
			for(String label:sas.keySet()) {
				List<TrainingDataFormat> tds=sas.get(label);
				if (tds!=null) {
					for(TrainingDataFormat td:tds) {
						assert(label.equals(td.getLabel()));
						String u=td.getUtterance();
						try {
							List<NLUOutput> rs = getNLUOutput(u, null,null);
							ConfusionEntry ce=ret.get(label);
							if (ce==null) ret.put(label, ce=new ConfusionEntry(label));
							if (rs!=null) {
								String id=rs.get(0).getId();
								ce.addResult(id,u);
							} else {
								ce.addResult(null,u);
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			}
		}

		System.out.println("total: "+total);
		retrain();
		return ret;
	}
	public static void printConfusionMatrix(PrintStream out,Map<String,ConfusionEntry> cmx) {
		if (cmx!=null) {
			List<ConfusionEntry> sortedEntries=new ArrayList<ConfusionEntry>(cmx.values());
			Collections.sort(sortedEntries);
			for(ConfusionEntry ce:sortedEntries) {
				float ep=ce.getErrorPercentage();
				if (ep>0) {
					String label=ce.getLabel();
					List<String> confusedWithSorted=ce.getSortedConfusedWithSAs();
					if (confusedWithSorted!=null) {
						out.println(label+" "+ce.getTotalCount()+" "+ce.getTotalErrors()+" ("+ep+"):");
						for(String sa:confusedWithSorted) {
							List<String> us=ce.getConfusedWith().get(sa);
							out.println("  "+((us!=null)?us.size():0)+"\t"+sa);
						}
					}
				}
			}
		}
	}
	public static void printConfusionMatrix(PrintStream out,Map<String, ConfusionEntry> pp,Map<String, Integer> positions) {
		Integer[][] counts=computeErrorCounts(pp,positions);
		Map<Integer,String> inverse=new HashMap<Integer,String>();
		for(String p:positions.keySet()) {
			inverse.put(positions.get(p), p);
		}
		for(int i=-1;i<counts.length;i++) {
			for(int j=-1;j<counts.length;j++) {
				if (i<0) {
					if (j<0) out.print("\t");
					else out.print(inverse.get(j)+"\t");
				} else {
					if (j<0) out.print(inverse.get(i)+"\t");
					else {
						Integer errors=counts[i][j];
						out.print(((errors!=null)?errors:0)+"\t");
					}
				}
			}
			out.print("\n");
		}
	}

	
	private static Integer[][] computeErrorCounts(Map<String, ConfusionEntry> cmx, Map<String, Integer> positions) {
		if (positions!=null) {
			Collection<Integer> s=positions.values();
			Integer max=null;
			for(Integer i:s) {
				if (max==null || max<i) max=i;
			}
			Integer[][] ret=new Integer[max][max];
			for(ConfusionEntry ce:cmx.values()) {
				float ep=ce.getErrorPercentage();
				if (ep>0) {
					String label=ce.getLabel();
					Integer labelPos=positions.get(label);
					List<String> confusedWithSorted=ce.getSortedConfusedWithSAs();
					if (confusedWithSorted!=null) {
						for(String sa:confusedWithSorted) {
							List<String> us=ce.getConfusedWith().get(sa);
							Integer saPos=positions.get(sa);
							ret[labelPos][saPos]=(us!=null)?us.size():0;
						}
					}
				}
			}
			return ret;
		}
		return null;
	}
/*
	public List<Pair<String, Map<String, Object>>> associatePayloadToSpeechActs(List<String> speechActs, String userText) throws Exception {
		List<Pair<String, Map<String, Object>>> payloads = new ArrayList<Pair<String, Map<String, Object>>>();
		if (speechActs != null) {
			for (String sa : speechActs) {
				Map<String, Object> payload = getPayload(sa,userText);
				payloads.add(new Pair<String, Map<String, Object>>(sa, payload));
			}
		}
		return payloads;
	}*/
	@Override
	public Map<String, Object> getPayload(String speechAct, String text) throws Exception {
		Map<String, Object> totalPayload=null;
		Preprocess pr = getPreprocess(PreprocessingType.RUN);
		List<List<Token>> options = pr.process(text);
		if (options!=null) {
			for(List<Token> option:options) {
				List<NE> foundNEs=BasicNE.filterNESwithSpeechAct(option, speechAct,pr.getTokenizer());
				if (foundNEs!=null) {
					Map<String,Object> payload=BasicNE.createPayload(foundNEs);
					if (totalPayload==null) totalPayload=payload;
					else totalPayload.putAll(payload);
				}
			}
		}
		return totalPayload;
	}
	protected static String springConfig=null;
	static protected AbstractApplicationContext context;
	public static NLUConfig getNLUConfig(String beanName) {
		logger.info("Initializing NLU configuration with bean named: '"+beanName+"'");
		if (springConfig==null)
			context = new ClassPathXmlApplicationContext(new String[] {"NLUConfigs.xml"});
		else 
			context = new FileSystemXmlApplicationContext(new String[] {springConfig});
		NLUConfig config = (NLUConfig) context.getBean(beanName);
		return config;
	}
	public static NLU init(String beanName) throws Exception {
		NLUConfig config=getNLUConfig(beanName);
		return init(config);
	}
	public static NLU init(NLUConfig config) throws Exception {
		_instance=(NLU) NLBusBase.createSubcomponent(config, config.getNluClass());
		return getInstance();
	}
	public static NLU getInstance() {return _instance;}

	@Override
	public void train(List<TrainingDataFormat> input, File model)
			throws Exception {
		throw new Exception("unhandled");
	}
	@Override
	public void train(File input, File model) throws Exception {
		throw new Exception("unhandled");
	}
	@Override
	public boolean nluTest(TrainingDataFormat testSample, List<NLUOutput> nluResults) throws Exception {
		return (nluResults!=null && !nluResults.isEmpty() && testSample.match(nluResults.get(0).getId()));
	}
	@Override
	public PerformanceResult test(List<TrainingDataFormat> test, File model,
			boolean printErrors) throws Exception {
		throw new Exception("unhandled");
	}
	@Override
	public PerformanceResult test(File test, File model, boolean printErrors)
			throws Exception {
		throw new Exception("unhandled");
	}
	@Override
	public boolean isPossibleNLUOutput(NLUOutput o) throws Exception {
		throw new Exception("unhandled");
	}
	@Override
	public Set<String> getAllSimplifiedPossibleOutputs() throws Exception {
		throw new Exception("unhandled");
	}

	@Override
	public List<Pair<String, Float>> getTokensScoresForLabel(String utt,String label,String modelFileName) throws Exception {
		throw new Exception("unhandled");
	}

	@Override
	public List<String> getFeaturesFromUtterance(String utt) {
		try {
			return (List<String>) featuresBuilder.invoke(null, utt);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public List<String> getFeaturesFromPositionInUtterance(String[] tokens,int pos) {
		try {
			return (List<String>) featuresAtPosBuilder.invoke(null, tokens,pos);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected Map<String,Model> readModels=null;
	protected Model readModelWithCache() throws Exception {
		NLUConfig config=getConfiguration();
		String modelFileName = config.getNluModelFile();
		return readModelWithCache(modelFileName);
	}
	protected Model readModelWithCache(String modelFileName) throws Exception {
		Model model=(readModels!=null)?readModels.get(modelFileName):null;
		if (model==null) {
			if (readModels==null) readModels=new HashMap<String, Model>();
			model = readModelFileNoCache(modelFileName);
			readModels.put(modelFileName, model);
		}
		return model;
	}

	protected Model readModelFileNoCache(String mfn) throws Exception {
		return readModelFileNoCache(new File(mfn));
	}
	public Model readModelFileNoCache(File mf) throws Exception {
		throw new Exception("unhandled");
	}

	public static Map<String,Object> createPayload(String varName,Object value,Map<String,Object> payload) {
		if (varName!=null) {
			if (payload==null) payload=new HashMap<String, Object>();
			payload.put(varName, value);
		}
		return payload;
	}

	public List<TrainingDataFormat> prepareTrainingDataForClassification(List<TrainingDataFormat> td) throws Exception {
		List<TrainingDataFormat> ret=null;
		Preprocess pr = getPreprocess(PreprocessingType.TRAINING);
		//ProgressTracker pt=new ProgressTracker(10, System.out);
		for(TrainingDataFormat d:td) {
			String sa=d.getLabel();
			//System.out.println(d.getUtterance()+" :: "+d.getLabel());
			List<List<Token>> nus = pr.process(d.getUtterance());
			if (nus!=null) {
				for(List<Token> nu:nus) {
					String nt=Preprocess.getString(nu,getConfiguration().getNluTokenizer(PreprocessingType.TRAINING),sa);
					if (StringUtils.isEmptyString(nt)) {
						getLogger().error("Empty utterance after filters to prepare it from training: ");
						getLogger().error("start='"+d.getUtterance()+"'");
						getLogger().error("end='"+nt+"'");
					} else {
						if (ret==null) ret=new ArrayList<TrainingDataFormat>();
						ret.add(new TrainingDataFormat(nt, sa));
					}
				}
			}
			//pt.updateDelta(1);
		}
		return ret;
	}

	public static Logger getLogger() {
		return logger;
	}

	public static final Pattern nluOutputLineFormat = Pattern.compile("^[\\s]*([\\d\\.]+[eE\\+\\-\\d]*)[\\s]+(.+)[\\s]*$");
	public static final Pattern rangePattern=Pattern.compile("([\\d]+)-([\\d]+)");
	public static final Pattern nluNERangesFormat = Pattern.compile("<("+rangePattern.toString()+"(,"+rangePattern.toString()+")*)>");
	protected List<NLUOutput> processNLUOutputs(String[] nlu,Integer nBest, Set<String> possibleUserEvents,ArrayList<String> sortedOutputKeys) throws Exception {
		Float acceptanceThreshold=this.acceptanceThreshold;
		NLUConfig config=getConfiguration();
		getLogger().debug("PROCESS NLU: input user speechActs: "+((nlu==null)?nlu:Arrays.asList(nlu)));
		if (sortedOutputKeys!=null) sortedOutputKeys.clear();

		List<NLUOutput> userEvents=new ArrayList<NLUOutput>();
		// if a particular nBest is given forget about the threshold and return the exact number of results.
		if (nBest==null) nBest=getConfiguration().getnBest();
		else acceptanceThreshold=null;

		if (nlu!=null) {
			for(String s:nlu) {
				Matcher m = nluOutputLineFormat.matcher(s);
				String prbString,sa;
				List<Pair<Integer,Integer>> ranges=null;
				if (m.matches() && (m.groupCount()==2)) {
					prbString=m.group(1);
					sa=StringUtils.removeLeadingAndTrailingSpaces(m.group(2));
					Matcher rm=nluNERangesFormat.matcher(sa);
					if (rm.find()) {
						sa=StringUtils.removeLeadingAndTrailingSpaces(sa.substring(0, rm.start()));
						String rangess=rm.group(1);
						rm=rangePattern.matcher(rangess);
						while(rm.find()) {
							try {
								Integer start=Integer.parseInt(rm.group(1));
								Integer end=Integer.parseInt(rm.group(2));
								if (ranges==null) ranges=new ArrayList<>();
								ranges.add(new Pair<Integer, Integer>(start, end));
							} catch (Exception e) {}
						}
					}
				} else {
					getLogger().error("NO MATCH WITH INPUT SPEECHACT AND PROBABILITY. Forcing P=1 and SpeechAct = '"+s+"'");
					prbString="1";
					sa=s;
				}
				try {
					float prb = Float.parseFloat(prbString);
					if ((acceptanceThreshold==null) || ((prb>=0) && (prb<=1) && (prb>=acceptanceThreshold))) {
						if ((possibleUserEvents==null) || (possibleUserEvents.contains(sa))) {
							if (userEvents.size()<=nBest) {
								NLUOutput o=new NLUOutput(null, sa, prb,null);
								o.setRanges(ranges);
								userEvents.add(o);
								if (sortedOutputKeys!=null) sortedOutputKeys.add(sa);
								getLogger().debug(" user speechAct: "+sa+" with probability "+prb);
								if (possibleUserEvents!=null) {
									possibleUserEvents.remove(sa);
									if (possibleUserEvents.size()<=0) break;
								}
							}
						}
					}
				} catch (NumberFormatException e) {
					getLogger().error(" probability associated with '"+s+"' is not a number.");
				}
			}
		}
		
		// if no event is left: update the current state by following all user edges (this is the case
		//  representing low certainty with the classification))
		if (userEvents.isEmpty()) {
			String lowConfidenceEvent=config.getLowConfidenceEvent();
			if (StringUtils.isEmptyString(lowConfidenceEvent)) {
				getLogger().warn(" no user speech acts left and LOW confidence event disabled, returning no NLU results.");
			} else {
				userEvents.add(new NLUOutput(null, lowConfidenceEvent, 1f, null));
				if (sortedOutputKeys!=null) sortedOutputKeys.add(lowConfidenceEvent);
				getLogger().warn(" no user speech acts left. adding the low confidence event.");
			}
		}
		return userEvents;
	}
	
	@Override
	public Set<String> getKnownWords() {
		return null;
	}
}
