package edu.usc.ict.nl.nlu.fst.sps;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.usc.ict.nl.bus.NLBus;
import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.fst.FSTNLU;
import edu.usc.ict.nl.nlu.fst.FSTNLUOutput;
import edu.usc.ict.nl.util.StringUtils;

public class SPSFSTNLU extends FSTNLU {
	private SAMapper mapper=null;
	private FSTNLU internalNLU=null;
	
	public SAMapper getMapper() {
		return mapper;
	}
	
	public SPSFSTNLU(NLUConfig c) throws Exception {
		super(c);
		
		String beanName=c.getInternalNluClass4Hier();
		NLUConfig config=NLU.getNLUConfig(beanName);

		NLBusConfig bc=c.getNlBusConfigNC();
		if (bc!=null) bc=(NLBusConfig) bc.clone();
		bc.setNluConfig(config);
		internalNLU = (FSTNLU) init(config);

		try {
			if (c.getSpsMapperModelFile()==null) throw new Exception("sps mapper model file not configured.");
			try {
				mapper=new SAMapper(c);
			} catch (Exception e) {
				getLogger().warn("mapper model not present, retraining.",e);
				SAMapper.trainMapperAndSave(new File(c.getSpsMapperModelFile()),this,new File(c.getUserUtterances()));
				mapper=new SAMapper(c);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public List<NLUOutput> getNLUOutput(String text,Set<String> possibleNLUOutputIDs, Integer nBest) throws Exception {
		List<FSTNLUOutput> nlus = getRawNLUOutput(text, possibleNLUOutputIDs, nBest);
		List<NLUOutput> ret=null;
		if (nlus!=null) {
			for(FSTNLUOutput nlu:nlus) {
				if (nlu.getId() == null)
					continue;
				String sa=mapper.getSimcoachSAForFSTNLUOutput(nlu,false);
				getLogger().info("SPS FSTNLU converted FST output: '"+nlu+"' to sps speech act: '"+sa+"'");
				if (ret==null) ret=new ArrayList<NLUOutput>();
				if (sa != null)
					ret.add(new NLUOutput(text, sa, nlu.getProb().getResult(), nlu.getPayload()));
			}
		}
		if (ret==null || ret.isEmpty()) {
			String lowConfidenceEvent=getConfiguration().getLowConfidenceEvent();
			if (StringUtils.isEmptyString(lowConfidenceEvent)) {
				getLogger().warn(" no user speech acts left and LOW confidence event disabled, returning no NLU results.");
			} else {
				if (ret==null) ret=new ArrayList<NLUOutput>();
				ret.add(new NLUOutput(text, lowConfidenceEvent, 1f, null));
				getLogger().warn(" no user speech acts left. adding the low confidence event.");
			}
		}

		return ret;
	}
	
	public List<FSTNLUOutput> getRawNLUOutput (String text,Set<String> possibleNLUOutputIDs,Integer nBest) throws Exception {
		List<FSTNLUOutput> nlus = (List)internalNLU.getNLUOutput(text, possibleNLUOutputIDs, nBest);
		return nlus;
	}

	@Override
	public void train(List<TrainingDataFormat> tds, File model)	throws Exception {
		internalNLU.train(tds, model);
		mapper.trainMapperAndSave(this,tds);
		mapper.loadMapperModel();
		getLogger().info("done construction of sps mapper to ontology labels.");
	}
	
	@Override
	public void retrain() throws Exception {
		NLUConfig c = getConfiguration();
		File u=new File(c.getUserUtterances()); // this file contains both the new nlu annotation and the ontology single label.
		retrain(u);
	}
	@Override
	public void retrain(File... files) throws Exception {
		internalNLU.retrain(files);
		mapper.trainMapperAndSave(this, files);
		mapper.loadMapperModel();
		getLogger().info("done construction of sps mapper to ontology labels.");
	}
	
	public static void main(String[] args) throws Exception {
		NLUConfig config=getNLUConfig("spsNLU");
		NLBusConfig tmp = NLBusConfig.WIN_EXE_CONFIG;
		tmp.setCharacter("Base-All");
		tmp.setNluConfig(config);
		NLU nlu=init(config);
	}
}
