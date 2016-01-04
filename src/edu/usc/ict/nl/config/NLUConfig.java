package edu.usc.ict.nl.config;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.usc.ict.nl.nlu.multi.merger.Merger;
import edu.usc.ict.nl.nlu.ne.NamedEntityExtractorI;
import edu.usc.ict.nl.nlu.preprocessing.PreprocesserI;
import edu.usc.ict.nl.nlu.preprocessing.TokenizerI;
import edu.usc.ict.nl.nlu.preprocessing.tokenizer.Tokenizer;
import edu.usc.ict.nl.nlu.trainingFileReaders.NLUTrainingFileI;
import edu.usc.ict.nl.nlu.trainingFileReaders.SimcoachUserXLSXFile;
import edu.usc.ict.nl.util.StringUtils;

public class NLUConfig extends NLConfig {
	
	private String nluExeRoot="mxnlu"; //base file directory where nlu exe can be found if applicable. default set for legacy support
	private Map<String,String> nluExeEnv = new HashMap<String, String>(); // environment variables for the nlu process

	//vh setup
	private boolean nluVhGenerating=false;
	private boolean nluVhListening=false;
	
	private NLUTrainingFileI trainingDataReader=new SimcoachUserXLSXFile(0);
	
	// NLU specific
	private String nluClass,internalNluClass4Hier,internalNluClass4Chart,hierarchicalNluSeparator,nluFeaturesBuilder;
	private Map<String,NLUConfig> internalNluListForMultiNlu;
	private Merger mergerForMultiNlu=null;
	private boolean hierNluReturnsNonLeaves=true,printNluErrors=false,doSpellChecking=false,chartNluSingleMode=false;
	private int chartNluMaxLength;
	private String userUtterancesFile;
	private Integer maximumNumberOfLabels=null;
	// MXNLU specific
	private String nluModelFile,nluTrainingFile,hardLinksFile;
	private boolean useSystemFormsToTrainNLU=true;
	private int nBest;
	private Float acceptanceThreshold,regularization;
	private String lowConfidenceEvent,emptyTextEventName;
	
	private PreprocessingConfig prConfig=null;
	
	// fst specific
	private String fstInputSymbols=null,fstOutputSymbols=null;
	String[] trainingFstCommand=null,runningFstCommand;
	// sps fst specific
	private boolean spsMapperUsesNluOutput=false;
	private String spsMapperModelFile=null;

	
	public NLUConfig() {
		super();
	}
	
	public NLUConfig cloneObject() {
		NLUConfig ret=null;
		try {
			// get all methods for which we have a getter and a setter.
			Constructor<? extends NLUConfig> constructor = this.getClass().getConstructor();
			ret=constructor.newInstance();
			Method[] publicMethods = getClass().getMethods();
			if (publicMethods!=null) {
				Map<String,Method> mTable=new HashMap<String, Method>();
				for(Method m:publicMethods) mTable.put(m.getName(),m);
				filterMethodsLeavingOnlyGettersAndSetters(mTable);
				for(String m:mTable.keySet()) {
					if (isGetter(m)) {
						Method getter=mTable.get(m);
						Method setter=mTable.get(getSetter(m));
						if (getter!=null && setter!=null) {
							Object v=getter.invoke(this);
							setter.invoke(ret, v);
						}
					}
				}
			}
		} catch (Exception e) {e.printStackTrace();}
		return ret;
	}
	

	
	
	private String forcedNluContentRoot=null,nluDir="nlu";
	public String getNluDir() {
		return nluDir;
	}
	public void setNluDir(String nluDir) {
		this.nluDir = nluDir;
	}
	public void setForcedNLUContentRoot(String nluContentRoot) {this.forcedNluContentRoot = nluContentRoot;}
	public String getForcedNLUContentRoot() {return this.forcedNluContentRoot;}
	public String getNLUContentRoot() {
		String forcedContentRoot=getForcedNLUContentRoot();
		if (!StringUtils.isEmptyString(forcedContentRoot)) return forcedContentRoot+File.separator;
		else return (nlBusConfig!=null)?nlBusConfig.getCharacterContentRoot()+File.separator+nluDir+File.separator:"";
	}
	/** NLU model file name */
	public String getNluModelFile() {return getNLUContentRoot()+nluModelFile;}
	public void setNluModelFile(String nluModelFile) { this.nluModelFile = removeAbsolutePath(nluModelFile); }
	/** nlu spell checking */
	public boolean getDoSpellChecking() {return this.doSpellChecking;}
	public void setDoSpellchecking(boolean s) {this.doSpellChecking=s;}
	/** NLU training file */
	public String getNluTrainingFile() {return getNLUContentRoot()+nluTrainingFile;}
	public void setNluTrainingFile(String nluTrainingFile) {this.nluTrainingFile = removeAbsolutePath(nluTrainingFile);}
	public String getNluHardLinks() {return (!StringUtils.isEmptyString(hardLinksFile))?getNLUContentRoot()+hardLinksFile:null;}
	public void setNluHardLinks(String file) {this.hardLinksFile = removeAbsolutePath(file);}
	public boolean getUseSystemFormsToTrainNLU() {return this.useSystemFormsToTrainNLU;}
	public void setUseSystemFormsToTrainNLU(boolean useForms) {this.useSystemFormsToTrainNLU=useForms;}
	public String getUserUtterances() {
		if (nlBusConfig!=null)
			return nlBusConfig.getXLSXContentRoot()+userUtterancesFile;
		else return getNLUContentRoot()+userUtterancesFile;
	}
	public void setUserUtterances(String file) {userUtterancesFile = removeAbsolutePath(file);}
	public String getSystemForms() {return (nlgConfig!=null)?nlgConfig.getSystemForms():null;}
	/** maximum number of labels */
	public Integer getMaximumNumberOfLabels() {
		return maximumNumberOfLabels;
	}
	public void setMaximumNumberOfLabels(Integer maximumNumberOfLabels) {
		this.maximumNumberOfLabels = maximumNumberOfLabels;
	}
	public NLUTrainingFileI getTrainingDataReader() {
		return trainingDataReader;
	}
	public void setTrainingDataReader(NLUTrainingFileI trainingDataReader) {
		this.trainingDataReader = trainingDataReader;
	}
	/** NLU nbest */
	public int getnBest() {return nBest;}
	public void setnBest(int nBest) {this.nBest = nBest;}
	/** NLU errors */
	public boolean getPrintNluErrors() {return printNluErrors;}
	public void setPrintNluErrors(boolean pe) {this.printNluErrors=pe;}
	/** NLU low confidence event */
	public String getLowConfidenceEvent() {return lowConfidenceEvent;}
	public void setLowConfidenceEvent(String e) {this.lowConfidenceEvent=e;}
	/** dm name of empty text event */
	public String getEmptyTextEventName() {return emptyTextEventName;}
	public void setEmptyTextEventName(String name){this.emptyTextEventName=name;}
	/** NLU minimum confidence score for acceptance */
	public Float getAcceptanceThreshold() {return acceptanceThreshold;}
	public void setAcceptanceThreshold(Float t) {this.acceptanceThreshold=t;}
	/** NLU reguralization parameter (if used) */
	public Float getRegularization() {return regularization;}
	public void setRegularization(Float t) {this.regularization=t;}
	/** nlu and dm class to be used to create nlu and dm instances */
	public String getNluClass() {return nluClass;}
	public void setNluClass(String nlu) {this.nluClass=nlu;}	
	public String getNluExeRoot() { return this.nluExeRoot; }
	public void setNluExeRoot(String nluExeRoot) { this.nluExeRoot = nluExeRoot; }
	public Map<String,String> getNluExeEnv() { return this.nluExeEnv; }
	public void setNluExeEnv(Map<String,String> nluExeEnv) { this.nluExeEnv = nluExeEnv; }

	/** hier NLU, internal NLU class */
	public boolean getHierNluReturnsNonLeaves() {return hierNluReturnsNonLeaves;}
	public void setHierNluReturnsNonLeaves(boolean l) {this.hierNluReturnsNonLeaves=l;}
	public String getInternalNluClass4Hier() {return internalNluClass4Hier;}
	public void setInternalNluClass4Hier(String nlu) {this.internalNluClass4Hier=nlu;}
	public String getInternalNluClass4Chart() {return internalNluClass4Chart;}
	public void setInternalNluClass4Chart(String nlu) {this.internalNluClass4Chart=nlu;}
	public Map<String,NLUConfig> getInternalNluListForMultiNlu() {return internalNluListForMultiNlu;}
	public void setInternalNluListForMultiNlu(Map<String,NLUConfig> nlu) {this.internalNluListForMultiNlu=nlu;}
	public Merger getMergerForMultiNlu() {return mergerForMultiNlu;}
	public void setMergerForMultiNlu(Merger mergerForMultiNlu) {this.mergerForMultiNlu = mergerForMultiNlu;}
	public int getChartNluMaxLength() {return this.chartNluMaxLength;}
	public void setChartNluMaxLength(int l) {this.chartNluMaxLength=l;}
	public boolean getChartNluInSingleMode() {return this.chartNluSingleMode;}
	public void setChartNluInSingleMode(boolean m) {this.chartNluSingleMode=m;}
	public String getHierarchicalNluSeparator() {return hierarchicalNluSeparator;}
	public void setHierarchicalNluSeparator(String s) {this.hierarchicalNluSeparator=s;}
	public String getNluFeaturesBuilderClass() {return this.nluFeaturesBuilder;}
	public void setNluFeaturesBuilderClass(String c) {this.nluFeaturesBuilder=c;}
	public boolean getNluVhListening() {return nluVhListening;}
	public void setNluVhListening(boolean s) {this.nluVhListening=s;}
	public boolean getNluVhGenerating() {return nluVhGenerating;}
	public void setNluVhGenerating(boolean s) {this.nluVhGenerating=s;}
	
	public String getFstInputSymbols() {return getNLUContentRoot()+fstInputSymbols;}
	public void setFstInputSymbols(String fstInputSymbols) {this.fstInputSymbols = removeAbsolutePath(fstInputSymbols);}
	public String getFstOutputSymbols() {return getNLUContentRoot()+fstOutputSymbols;}
	public void setFstOutputSymbols(String fstOutputSymbols) {this.fstOutputSymbols = removeAbsolutePath(fstOutputSymbols);}
	public String[] getRunningFstCommand() {return runningFstCommand;}
	public void setRunningFstCommand(String[] fstCommand) {this.runningFstCommand = fstCommand;}
	public String[] getTrainingFstCommand() {return trainingFstCommand;}
	public void setTrainingFstCommand(String[] fstCommand) {this.trainingFstCommand = fstCommand;}
	public String getSpsMapperModelFile() {return (spsMapperModelFile!=null)?getNLUContentRoot()+spsMapperModelFile:null;}
	public void setSpsMapperModelFile(String mapperFile) {this.spsMapperModelFile = removeAbsolutePath(mapperFile);}
	public boolean getSpsMapperUsesNluOutput() {return spsMapperUsesNluOutput;}
	public void setSpsMapperUsesNluOutput(boolean mapperUsesNlu) {this.spsMapperUsesNluOutput=mapperUsesNlu;}
	
	public Boolean isInAdvicerMode() {return (nlBusConfig!=null)?nlBusConfig.isInAdvicerMode():false;}  
	
	public PreprocessingConfig getPreprocessingConfig() {return prConfig;}
	public void setPreprocessingConfig(PreprocessingConfig prConfig) {this.prConfig = prConfig;}
	public List<NamedEntityExtractorI> getNluNamedEntityExtractors() {
		if (getPreprocessingConfig()!=null) {
			return getPreprocessingConfig().getNluNamedEntityExtractors();
		}
		return null;
	}
	public TokenizerI getNluTokenizer() {
		if (getPreprocessingConfig()!=null) {
			return getPreprocessingConfig().getNluTokenizer();
		}
		return null;
	}
	
	// sample config used to run mxnlu during testing
	public static final NLUConfig WIN_EXE_CONFIG=new NLUConfig();
	static{
		WIN_EXE_CONFIG.setnBest(2);
		WIN_EXE_CONFIG.setAcceptanceThreshold(0.4f);
		WIN_EXE_CONFIG.setLowConfidenceEvent("internal.low-confidence");
		WIN_EXE_CONFIG.setNluModelFile("classifier-model");
		WIN_EXE_CONFIG.setNluTrainingFile("classifier-training.txt");
		WIN_EXE_CONFIG.setChartNluMaxLength(30);
		WIN_EXE_CONFIG.setDoSpellchecking(false);
		WIN_EXE_CONFIG.setNluFeaturesBuilderClass("edu.usc.ict.nl.nlu.features.FeaturesBuilderForMXClassifier");
		WIN_EXE_CONFIG.setNluHardLinks("hardlinks.txt");
		WIN_EXE_CONFIG.setUserUtterances("user-utterances.xlsx");
		WIN_EXE_CONFIG.setMaximumNumberOfLabels(255);
		//WIN_EXE_CONFIG.nlBusConfig=NLBusConfig.WIN_EXE_CONFIG;
	}

}
