package edu.usc.ict.nl.nlu.fst;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.BuildTrainingData;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.fst.train.Aligner;
import edu.usc.ict.nl.nlu.fst.train.Alignment;
import edu.usc.ict.nl.nlu.fst.train.AlignmentSummary;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.PerformanceResult;
import edu.usc.ict.nl.util.StringUtils;

public class FSTNLU extends NLU {

	protected TraverseFST tf=null;

	public TraverseFST getFSTTraverser() {return tf;}
	
	public FSTNLU(NLUConfig c) throws Exception {
		super(c);
		File in=new File(c.getFstInputSymbols());
		File out=new File(c.getFstOutputSymbols());
		File model=new File(c.getNluModelFile());
		tf=new TraverseFST(in,out,model,c.getRunningFstCommand());
		if (!in.exists() || !out.exists() || !model.exists()) {
			logger.warn("input/output/model not existing, retraining...");
			retrain();
		}
	}
	
	private String[] getTrainingCommand(String in, String out, String model,String nluDir,String[] templatedTrainingCommand) {
		String[] trainingCMD=new String[templatedTrainingCommand.length];
		for (int i=0;i<templatedTrainingCommand.length;i++) {
			trainingCMD[i] = templatedTrainingCommand[i];
			trainingCMD[i] = trainingCMD[i].replaceAll("%IN%",in);
			trainingCMD[i] = trainingCMD[i].replaceAll("%OUT%",out);
			trainingCMD[i] = trainingCMD[i].replaceAll("%MODEL%",model);
			trainingCMD[i] = trainingCMD[i].replaceAll("%NLUDIR%",nluDir);
		}
		return trainingCMD;
	}
	
	@Override
	public List<NLUOutput> getNLUOutput(String text,
			Set<String> possibleNLUOutputIDs, Integer nBest) throws Exception {
		nBest=(nBest==null || nBest<=0)?1:nBest;
		BuildTrainingData btd = getBTD();
		List<Token> tokens = btd.applyBasicTransformationsToStringForClassification(text);
		String input=BuildTrainingData.untokenize(tokens);
		String retFST=tf.getNLUforUtterance(input,nBest);
		//System.out.println(retFST);
		List<NLUOutput> ret=(List)tf.getResults(retFST);
		if (ret==null || ret.isEmpty()) {
			String lowConfidenceEvent=getConfiguration().getLowConfidenceEvent();
			if (StringUtils.isEmptyString(lowConfidenceEvent)) {
				logger.warn(" no user speech acts left and LOW confidence event disabled, returning no NLU results.");
			} else {
				if (ret==null) ret=new ArrayList<NLUOutput>();
				ret.add(new FSTNLUOutput(text, lowConfidenceEvent, 1f, null));
				logger.warn(" no user speech acts left. adding the low confidence event.");
			}
		}

		return ret;
	}
	
	@Override
	public PerformanceResult test(List<TrainingDataFormat> test, File model,boolean printErrors) throws Exception {
		if (model!=null) throw new Exception("unhandled model setting.");
		PerformanceResult p=new PerformanceResult();
		for(TrainingDataFormat td:test) {
			List<NLUOutput> r = getNLUOutput(td.getUtterance(), null, 1);
			boolean rr=nluTest(td,r);
			p.add(rr);
			if (!rr && printErrors) {
				logger.error("'"+td.getUtterance()+"' classified as: "+r+" instead of "+td.getLabel());
			}
		}
		return p;
	}
	
	@Override
	public Set<String> getAllSimplifiedPossibleOutputs() throws Exception {
		return null;
	}
	
	@Override
	public void kill() throws Exception {
	}
	
	public static void produceAcceptorForPathContainingOneOf(String type,File outputSymbols) throws Exception {
		Set<String> allType=new HashSet<String>();
		Set<String> allElse=new HashSet<String>();
		Map<Integer, String> oSyms = TraverseFST.openSymbols(outputSymbols);
		for(String label:oSyms.values()) {
			List<Pair<String, String>> keyValuePair = FSTNLUOutput.getPairsFromString(label);
			for(Pair<String,String> p:keyValuePair) {
				if (p.getFirst().equals(type)) {
					String r=FSTNLUOutput.composeNLUKeyValuePair(type, p.getSecond());
					allType.add(r.toLowerCase());
				} else {
					String r=FSTNLUOutput.composeNLUKeyValuePair(p.getFirst(), p.getSecond());
					allElse.add(r.toLowerCase());
				}
			}
		}
		Set<String> modelOutputSyms=new HashSet<String>(oSyms.values());
		Set<String> allTDSymbols=new HashSet<String>(allType);
		allTDSymbols.addAll(allElse);
		allTDSymbols.removeAll(modelOutputSyms);

		allElse.removeAll(allTDSymbols);
		allType.removeAll(allTDSymbols);
		BufferedWriter out=new BufferedWriter(new FileWriter(type+".fsa"));
		for(String e:allElse) {
			out.write("0 0 "+e+" "+e+"\n");
			out.write("1 1 "+e+" "+e+"\n");
		}
		for(String e:allType) {
			out.write("0 1 "+e+" "+e+"\n");
		}
		out.write("1\n");
		out.close();
	}

	@Override
	public void retrain() throws Exception {
		NLUConfig c = getConfiguration();
		File u=new File(c.getUserUtterances()); // this file contains both the new nlu annotation and the ontology single label.
		retrain(u);
	}
	@Override
	public void retrain(File... files) throws Exception {
		NLUConfig c = getConfiguration();
		logger.info("preparing training data for aligner");
		if (files!=null && files.length>0) {
			List<TrainingDataFormat> itds=null;
			for(File u:files) {
				List<TrainingDataFormat> tds=null;
				try {
					tds = Aligner.extractTrainingDataFromSingleStep1and3GoogleXLSXForSPS(u);
					if (tds!=null) {
						if (itds==null) itds=new ArrayList<TrainingDataFormat>();
						itds.addAll(tds);
					}
				} catch (Exception e) {
					logger.error("error reading training data from file: "+u,e);
				}
			}
			File model=new File(c.getNluModelFile());
			train(itds,model);
		} else {
			logger.warn("skipping retraining as passed empty list of files.");
		}
	}

	@Override
	public void train(List<TrainingDataFormat> tds, File model) throws Exception {
		NLUConfig c = getConfiguration();
		Aligner a=new Aligner(new File(c.getNLUContentRoot()));
		logger.info("starting aligner and fst generation");
		tds=getBTD().prepareTrainingDataForClassification(tds);
		File in=new File(c.getFstInputSymbols());
		File out=new File(c.getFstOutputSymbols());
		a.buildFSTFromGoogleXLSX(getTrainingCommand(in.getName(),out.getName(),model.getName(), c.getNluDir(), c.getTrainingFstCommand()),tds, null);//new File(c.getNLUContentRoot(),"dictionary.xlsx"));
		checkAlignedOutputSymbols(tds);
		logger.info("done aligner and fst generation");
	}
	
	public AlignmentSummary readAlignerInfo() throws Exception {
		NLUConfig c = getConfiguration();
		Aligner a=new Aligner(new File(c.getNLUContentRoot()));
		List<Alignment> as = a.readAlignerOutputFile();
		AlignmentSummary asum=new AlignmentSummary(as);
		return asum;
	}
	
	private void checkAlignedOutputSymbols(List<TrainingDataFormat> tds) {
		try {
			Set<String> allLearned=new HashSet<String>();
			for(TrainingDataFormat td:tds) {
				String label=td.getLabel();
				List<Pair<String, String>> possibilities = FSTNLUOutput.getPairsFromString(label);
				for(Pair<String,String>p:possibilities) {
					String r=FSTNLUOutput.composeNLUKeyValuePair(p.getFirst(), p.getSecond());
					allLearned.add(r.toLowerCase());
				}
			}
			Map<Integer, String> oSyms = TraverseFST.openSymbols(tf.getOutputSymbols());
			Set<String> modelOutputSyms=new HashSet<String>(oSyms.values());
			Set<String> allTDSymbols=new HashSet<String>(allLearned);
			allTDSymbols.removeAll(modelOutputSyms);
			if (!allTDSymbols.isEmpty()) {
				logger.error(" these key-value pairs were not aligned and so will not be generated by the NLU: ");
				for(String e:allTDSymbols) {
					logger.error("  "+e);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void loadModel(File model) throws Exception {
		NLUConfig c = getConfiguration();
		File in=new File(c.getFstInputSymbols());
		File out=new File(c.getFstOutputSymbols());
		logger.warn("loading fst model using predefined input and output symbols:");
		logger.warn(" new model: "+model.getAbsolutePath());
		logger.warn(" input symbols: "+in.getAbsolutePath());
		logger.warn(" output symbols: "+out.getAbsolutePath());
		tf=new TraverseFST(in,out,model,c.getRunningFstCommand());
	}
	
	public static List<NLUOutput> runFSTNLUTest(String character,String text,int nBest) throws Exception {
		NLUConfig config=getNLUConfig("FSTNLU");
		NLBusConfig tmp = NLBusConfig.WIN_EXE_CONFIG;
		tmp.setCharacter(character);
		tmp.setNluConfig(config);
		NLU nlu=init(config);

		return nlu.getNLUOutput(text, null, nBest);
	}

	public static void main(String[] args) throws Exception {
		/*
		NLUConfig config=getNLUConfig("FSTNLU");
		NLBusConfig tmp = NLBusConfig.WIN_EXE_CONFIG;
		tmp.setDefaultCharacter("Base-All");
		tmp.setNluConfig(config);
		NLU nlu=init(config);
		nlu.retrain();*/
		List<NLUOutput> r=runFSTNLUTest("Base-All","do you feel any pain in your joints?",10);
		System.out.println(FunctionalLibrary.printCollection(r, "", "", "\n"));
	}

}
