package edu.usc.ict.nl.nlu.mxnlu;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLConfig.ExecutablePlatform;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.nlu.Model;
import edu.usc.ict.nl.nlu.Model.FeatureWeight;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.NLUProcess;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.io.BuildTrainingData;
import edu.usc.ict.nl.nlu.ne.BasicNE;
import edu.usc.ict.nl.nlu.ne.NE;
import edu.usc.ict.nl.nlu.preprocessing.Preprocess;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.PerformanceResult;
import edu.usc.ict.nl.util.StringUtils;

public class MXClassifierNLU extends NLU {

	protected NLUProcess nluP;

	private Float acceptanceThreshold;
	public void setAcceptanceThreshold(Float t) {this.acceptanceThreshold=t;}

	public MXClassifierNLU() throws Exception {
		this(NLUConfig.WIN_EXE_CONFIG);
	}
	public MXClassifierNLU(NLUConfig c) throws Exception {
		super(c);
		NLUConfig config=getConfiguration();
		Boolean inAdviserMode=config.isInAdvicerMode();
		setAcceptanceThreshold((inAdviserMode!=null && inAdviserMode)?null:config.getAcceptanceThreshold());
		setNLUProcess(startMXNLUProcessFromConfig(config));
	}

	private static final Pattern modelLine=Pattern.compile("^([^\\s]+)_\t([^\\s]+)\t([^\\s]+)$");
	@Override
	public boolean isPossibleNLUOutput(NLUOutput o) throws Exception {
		Set<String> possibilities = getAllSimplifiedPossibleOutputs();
		if (possibilities!=null) {
			String ev=o.getId();
			return possibilities.contains(ev);
		} else return false;
	}
	@Override
	public Set<String> getAllSimplifiedPossibleOutputs() throws Exception {
		Set<String> validSAs=null;
		NLUConfig config=getConfiguration();
		String modelFileName = config.getNluModelFile();

		Model model=null;
		try {
			model=readModelWithCache(modelFileName);
		} catch (Exception e) {
			getLogger().error("Error opening model file: '"+modelFileName+"' while asking for possible NLU speech acts. Returning none.");
		}

		if (model!=null) {
			validSAs=model.getOutputLabels();
			Map<String, String> hardLinks = getHardLinksMap();
			if (hardLinks!=null) for(String label:hardLinks.values()) validSAs.add(label);
		}
		return validSAs;
	}

	@Override
	public Model readModelFileNoCache(File mf) throws Exception {
		Model ret=null;
		BufferedReader in=new BufferedReader(new FileReader(mf));
		String line;
		while((line=in.readLine())!=null) {
			Matcher m=modelLine.matcher(line);
			if (m.matches() && m.groupCount()==3) {
				String l=m.group(1).replaceAll("_$","");
				String f=m.group(2);
				float w=Float.parseFloat(m.group(3));
				if (ret==null) ret=new Model();
				ret.addFeatureWeightForSA(f, l, ret.new FeatureWeight(f,w));
			}
		}
		return ret;
	}

	@Override
	public void loadModel(File model) throws Exception {
		kill();
		NLUConfig config=getConfiguration();
		String modelName=(model.isAbsolute())?model.getName():model.getPath();
		config.setNluModelFile(modelName);
		setNLUProcess(startMXNLUProcessFromConfig(config));
	}

	public NLUProcess startMXNLUProcessWithTheseParams(String model,int nbest) throws Exception {
		MXClassifierProcess nlup = new MXClassifierProcess(getExeName(getConfiguration()), getConfiguration().getNluExeEnv());
		if (model!=null) nlup.run(model,nbest);
		return nlup;
	}
	public NLUProcess startMXNLUProcessFromConfig(NLUConfig config) throws Exception {
		new File(getExeName(config)).setExecutable(true);
		File model=new File(config.getNluModelFile());
		if (!model.exists()) {
			getLogger().warn("no model found, retraining...");
			boolean startTrainingNLUP=getNLUProcess()==null;
			if (startTrainingNLUP) setNLUProcess(startMXNLUProcessWithTheseParams(null,(config.isInAdvicerMode())?-1:config.getnBest()));
			retrain();
			if (startTrainingNLUP) kill();
		}
		return startMXNLUProcessWithTheseParams(config.getNluModelFile(),(config.isInAdvicerMode())?-1:config.getnBest());
	}

	public static String getExeName(NLUConfig config) {
		ExecutablePlatform executablePlatform = config.getExecutablePlatform();
		String exe=config.getNluExeRoot()+"/" + "mxClassifier-"+executablePlatform.toString();
		if (executablePlatform == ExecutablePlatform.WIN32) exe+=".exe";
		return exe;
	}

	public NLUProcess getNLUProcess() {
		return nluP;
	}
	public void setNLUProcess(NLUProcess nluP) {
		this.nluP=nluP;
	}

	public List<NLUOutput> classify(String text,Set<String> possibleUserEvents, Integer nBest) throws Exception {
		List<NLUOutput> ret=null;

		if (getLogger().isDebugEnabled()) getLogger().info("input text: '"+text+"'");

		Preprocess pr=getPreprocess(PreprocessingType.RUN);
		List<List<Token>> options = pr.process(text);
		sortOptionsByText(options,pr);

		NLUOutput hardLabel=getHardLinkMappingOf(text);
		if (hardLabel!=null) {
			if (options!=null && !options.isEmpty()) {
				for(List<Token> option:options) {
					List<NE> fnes=BasicNE.filterNESwithSpeechAct(option,hardLabel.getId(),pr.getTokenizer());
					Map<String, Object> payload = BasicNE.createPayload(fnes);
					hardLabel.setPayload(payload);
					break;
				}
			}
			if (ret==null) ret=new ArrayList<>();
			ret.add(hardLabel);
			return ret;
		}

		if (options!=null && !options.isEmpty()) {
			String pt=null;
			for(List<Token> option:options) {
				String t=pr.getString(option);
				String features=null;
				String[] result=null;
				if (getLogger().isDebugEnabled()) getLogger().info(" considering preprocessed option: '"+t+"'");

				if (pt==null || !t.equals(pt)) {
					pt=t;
					features=FunctionalLibrary.printCollection(getFeaturesFromUtterance(t),"",""," ");
					if (StringUtils.isEmptyString(features)) features=t;
					result = (nBest!=null)?getNLUProcess().classify(features,nBest):getNLUProcess().classify(features);
				}

				List<NLUOutput> userSpeechActsWithProb = processNLUOutputs(result,nBest,possibleUserEvents,null);		
				if (userSpeechActsWithProb!=null) {
					if (Preprocess.hasAssociatedNamedEntities(option)) {
						for(NLUOutput o:userSpeechActsWithProb) {
							o.setText(t);
							List<NE> fnes=BasicNE.filterNESwithSpeechAct(option,o.getId(),pr.getTokenizer(),o.getRanges());
							Map<String, Object> payload = BasicNE.createPayload(fnes);
							o.setPayload(payload);
						}
					}
					if (ret==null) ret=userSpeechActsWithProb;
					else ret.addAll(userSpeechActsWithProb);
				}
			}
		}
		if (ret!=null && !ret.isEmpty()) {
			Collections.sort(ret, new Comparator<NLUOutput>() {
				@Override
				public int compare(NLUOutput o1, NLUOutput o2) {
					return o2.getProb().compareTo(o1.getProb());
				}
			});
		}
		return ret;
	}

	/**
	 * options may differ just because of different named entities recognized altough superficially the text is the same.
	 * to avoid computing features (for classifiers) over and over for the same text, this sorting routine can be used together
	 * with some more intelligent code.
	 * @param options
	 */
	private void sortOptionsByText(List<List<Token>> options,final Preprocess pr) {
		Collections.sort(options, new Comparator<List<Token>>(){
			@Override
			public int compare(List<Token> o1, List<Token> o2) {
				String t1=pr.getString(o1);
				String t2=pr.getString(o2);
				return t1.compareTo(t2);
			}
		});
	}

	@Override
	public List<NLUOutput> getNLUOutputFake(String[] nluOutputIDs,String inputText) throws Exception {
		List<NLUOutput> ret=new ArrayList<NLUOutput>();
		Preprocess pr=getPreprocess(PreprocessingType.RUN);
		List<List<Token>> options = pr.process(inputText);
		sortOptionsByText(options,pr);

		List<NLUOutput> userSpeechActsWithProb = processNLUOutputs(nluOutputIDs,null,null,null);		

		if (userSpeechActsWithProb!=null && options!=null && !options.isEmpty()) {
			for(int i=0;i<userSpeechActsWithProb.size();i++) {
				NLUOutput o=userSpeechActsWithProb.get(0);
				boolean added=false;
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
				if (!added) {
					if (ret==null) ret=new ArrayList<>();
					ret.add(o);
				}
			}
		}
		return ret;
	}
	@Override
	public List<NLUOutput> getNLUOutput(String text,Set<String> possibleUserEvents,Integer nBest) throws Exception {
		String emptyEvent=getConfiguration().getEmptyTextEventName();
		if (StringUtils.isEmptyString(text) && !StringUtils.isEmptyString(emptyEvent)) {
			List<NLUOutput> ret=new ArrayList<NLUOutput>();
			ret.add(new NLUOutput(text, emptyEvent, 1, null));
			return ret;
		}
		return classify(text, possibleUserEvents,nBest);
	}

	public static final Pattern nluOutputLineFormat = Pattern.compile("^[\\s]*([\\d\\.]+[eE\\+\\-\\d]*)[\\s]+(.+)[\\s]*$");
	public static final Pattern rangePattern=Pattern.compile("([\\d]+)-([\\d]+)");
	public static final Pattern nluNERangesFormat = Pattern.compile("<("+rangePattern.toString()+"(,"+rangePattern.toString()+")*)>");
	private List<NLUOutput> processNLUOutputs(String[] nlu,Integer nBest, Set<String> possibleUserEvents,ArrayList<String> sortedOutputKeys) throws Exception {
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

	public void kill() {getNLUProcess().kill();}

	public void trainNLUOnThisData(List<TrainingDataFormat> td,File trainingFile, File modelFile) throws Exception {
		// dumping training data to file
		dumpTrainingDataToFileNLUFormat(trainingFile,td);
		// using the file created above to train a model
		getNLUProcess().train(modelFile.getAbsolutePath(),trainingFile.getAbsolutePath());
	}
	public PerformanceResult testNLUOnThisData(List<TrainingDataFormat> testing,File modelFile,boolean printMistakes) throws Exception {
		boolean restart=(modelFile!=null);
		NLUProcess oldNluProcess = getNLUProcess();
		PerformanceResult result=new PerformanceResult();
		if(restart) setNLUProcess(startMXNLUProcessWithTheseParams(modelFile.getAbsolutePath(),1));
		for(TrainingDataFormat td:testing) {
			List<NLUOutput> sortedNLUOutput = getNLUOutput(td.getUtterance(),null,null);
			if (nluTest(td,sortedNLUOutput)) {
				result.add(true);
			} else {
				result.add(false);
				if (printMistakes) {
					getLogger().error(generateErrorString(sortedNLUOutput,td,modelFile));
				}
			}
		}
		if (restart) {
			kill();
			setNLUProcess(oldNluProcess);
		}
		return result;
	}
	private String generateErrorString(List<NLUOutput> sortedNLUOutput,TrainingDataFormat td, File modelFile) throws Exception {
		String ret=null;
		String text=td.getUtterance();
		String obtainedLabel=(sortedNLUOutput==null || sortedNLUOutput.isEmpty())?"null":sortedNLUOutput.get(0).toString();
		String expectedLabel=td.getLabel();

		String indent="    ";
		//String label=td.getLabel().replaceAll("[\\s]+", "_");
		//Map<String, List<FeatureWeight>> features4Labels = getFeatures4Labels(modelFile.getAbsolutePath());

		ret="'"+text+"'\n"+indent+"expected:\n"+indent+expectedLabel+"\n"+indent+"obtained:\n"+indent+obtainedLabel;
		//ret="'"+text+"'\t"+expectedLabel+"\t"+obtainedLabel;

		return ret;
	}

	@Override
	public void train(File trainingFile, File modelFile) throws Exception {
		BuildTrainingData btd=getBTD();
		Integer maximumNumberOfLabels=getConfiguration().getMaximumNumberOfLabels();
		if (!trainingFile.isAbsolute()) trainingFile=new File(getConfiguration().getNLUContentRoot(),trainingFile.getPath());
		if (!modelFile.isAbsolute()) modelFile=new File(getConfiguration().getNLUContentRoot(),modelFile.getPath());
		List<TrainingDataFormat> td =BuildTrainingData.buildTrainingDataFromNLUFormatFile(trainingFile);
		if (td!=null && !td.isEmpty()) {
			Set<String> sas = BuildTrainingData.getAllSpeechActsInTrainingData(td);
			if (maximumNumberOfLabels!=null && sas.size()>maximumNumberOfLabels) getLogger().error("skipping training because too many labels ("+sas.size()+">"+maximumNumberOfLabels+") in "+trainingFile);
			else {

				td=btd.cleanTrainingData(td);

				trainNLUOnThisData(td, trainingFile, modelFile);
			}
		} else {
			getLogger().warn("couldn't train because didn't find any data: '"+trainingFile+"'");
		}
	}
	@Override
	public void train(List<TrainingDataFormat> td, File model)
			throws Exception {
		Integer maximumNumberOfLabels=getConfiguration().getMaximumNumberOfLabels();
		Set<String> sas = BuildTrainingData.getAllSpeechActsInTrainingData(td);
		if (maximumNumberOfLabels!=null && sas.size()>maximumNumberOfLabels) getLogger().error("skipping training because too many labels ("+sas.size()+">"+maximumNumberOfLabels+") in training data");
		else {
			NLUConfig config=getConfiguration();
			trainNLUOnThisData(td, new File(config.getNluTrainingFile()), model);
		}
	}

	@Override
	public PerformanceResult test(File testingFile, File modelFile,boolean printErrors) throws Exception {
		if (!testingFile.isAbsolute()) testingFile=new File(getConfiguration().getNLUContentRoot(),testingFile.getPath());
		if (!modelFile.isAbsolute()) modelFile=new File(getConfiguration().getNLUContentRoot(),modelFile.getPath());
		List<TrainingDataFormat> td=BuildTrainingData.buildTrainingDataFromNLUFormatFile(testingFile);
		return testNLUOnThisData(td, modelFile, printErrors);
	}
	@Override
	public PerformanceResult test(List<TrainingDataFormat> td, File model,
			boolean printErrors) throws Exception {
		return testNLUOnThisData(td, model, printErrors);
	}


	@Override
	public List<Pair<String,Float>> getTokensScoresForLabel(String utt,String label,String modelFileName) throws Exception {
		List<Pair<String,Float>> ret=null;
		Model m=readModelWithCache(modelFileName);
		if (m!=null) {
			Preprocess pr = getPreprocess(PreprocessingType.RUN);
			String processedText=pr.getString(pr.process(utt).get(0));
			if (!StringUtils.isEmptyString(processedText)) {
				String[] tokens=("<s> "+processedText+" </s>").split("[\\s]+");
				int l=tokens.length;
				for(int i=0;i<l-2;i++) {
					Float totalAtPos=0f;
					List<String> features = getFeaturesFromPositionInUtterance(tokens, i);
					if (features!=null && !features.isEmpty()) {
						for(String f:features) {
							Map<String,FeatureWeight> weights=m.getWeightsForFeature(f);
							if (weights!=null && weights.containsKey(label)) {
								if (weights.containsKey(label)) {
									float w=weights.get(label).getWeight();
									totalAtPos+=w;
								}
							}
						}
					}
					if (ret==null) ret=new ArrayList<Pair<String,Float>>();
					ret.add(new Pair<String, Float>(tokens[i+1], totalAtPos));
				}
			}
		}
		return ret;
	}
	public Map<String,List<FeatureWeight>> getFeatures4Labels(String modelFileName) throws Exception {
		Map<String,List<FeatureWeight>> ret=null;
		Model m=readModelWithCache(modelFileName);
		if (m!=null) {
			Map<String, Map<String, FeatureWeight>> fs4ls = m.getFeatures4Labels();
			if (fs4ls!=null) {
				for (String f:fs4ls.keySet()) {
					Map<String, FeatureWeight> fs4l = fs4ls.get(f);
					if (ret==null) ret=new HashMap<String, List<FeatureWeight>>();
					for(String l:fs4l.keySet()) {
						List<FeatureWeight> features4Label = ret.get(l);
						if (features4Label==null) ret.put(l, features4Label=new ArrayList<Model.FeatureWeight>());
						features4Label.add(fs4l.get(l));
					}
				}
			}
			if (ret!=null) {
				for(String l:ret.keySet()) {
					List<FeatureWeight> features4Label = ret.get(l);
					Collections.sort(features4Label);
				}
			}
		}
		return ret;
	}

	public static void main(String[] args) throws Exception {
		/*
		MXClassifierNLU cl = new MXClassifierNLU();
		System.out.println(BuildTrainingData.tokenize("<NUM> years"));

		System.out.println(Arrays.asList(cl.classify("he usually pays them on time, so not very often. ",null)));
		System.exit(1);
		System.out.println(cl.getBTD().prepareUtteranceForClassification("2 or 3"));
		cl.getPayload("answer.bio-info.user.age",cl.getClassifierInputUtteranceBeforeGeneralization("i'm twenty six years old"));

		String text=cl.getClassifierInputUtteranceBeforeGeneralization("Two years and 51 weeks.");
		//text=cl.getClassifierInputUtteranceBeforeGeneralization("twice weekly");
		System.out.println(text+" "+cl.getPayload("answer.time-period",text));
		List<Token> tokens = BuildTrainingData.tokenize(text);
		System.out.println(BuildTrainingData.tokenize("or(a , and ( a,or(d,e)),c)",DialogueKBFormula.formulaTokenTypes));
		TimePeriodSearcher ts = new TimePeriodSearcher(cl.getConfiguration(),text);
		Long num=ts.getTimePeriodInSeconds();
		System.out.println("count/day: "+ts.getTimesEachDay());
		System.out.println("count/week: "+ts.getTimesEachDay()*7);
		System.out.println("count/month: "+ts.getTimesEachDay()*30);
		if (num!=null) {
			System.out.println("num days: "+BasicNE.convertSecondsIn(num,ParserSemanticRulesTimeAndNumbers.numSecondsInDay));
		}
		 */
	}

}
