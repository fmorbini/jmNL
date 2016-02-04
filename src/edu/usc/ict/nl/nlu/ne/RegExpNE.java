	package edu.usc.ict.nl.nlu.ne;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.keyword.KeywordREMatcher;
import edu.usc.ict.nl.nlu.keyword.ActualMultiREMatcher;
import edu.usc.ict.nl.nlu.preprocessing.Preprocess;

public class RegExpNE extends BasicNE {

	private String modelName=null;
	private KeywordREMatcher matcher=null;

	public RegExpNE(String file) {
		this(true,file);
	}
	public RegExpNE(boolean generalize,String file) {
		this.modelName=file;
		/*
		try {
			File modelFile = new File(modelName);
			if (modelFile.exists()) {
				loadModel(modelFile);
			} else {
				logger.warn("error loading "+this.getClass().getName()+": no config file");
			}
		} catch (Exception e) {
			logger.warn("error loading config file.",e);
		}
		*/
	}
	
	@Override
	public void setConfiguration(NLUConfig configuration) {
		super.setConfiguration(configuration);
		try {
			File model=new File(configuration.getNLUContentRoot(),modelName);
			if (!model.exists() && configuration.getNlBusConfigNC()!=null) model=new File(configuration.getNlBusConfigNC().getContentRoot(),"common/nlu/"+modelName);
			if (model.exists())
				loadModel(model);
			else throw new IOException("File not found: " + model.getAbsolutePath());
		} catch (Exception e) {
			logger.error("error loading -with config- file",e);
		}
	}
	
	private void loadModel(File modelFile) throws Exception {
		this.matcher=new KeywordREMatcher(modelFile);
	}

	@Override
	public boolean isNEAvailableForSpeechAct(NE ne, String sa) {
		return true;
	}
	
	@Override
	public List<NE> extractNamedEntitiesFromText(String text,PreprocessingType type) throws Exception {
		List<NE> payload = null;
		if (text == null) // special events like 'login'
			return null;
		if (matcher!=null) {
			if (matcher.findIn(text)) {
				do {
					ActualMultiREMatcher tm = matcher.getLastMatchMatcher();
					String match=tm.getMatchedString(text);
					if (payload==null) payload=new ArrayList<NE>();
					payload.add(new NE(tm.getTopicID(),DialogueKBFormula.generateStringConstantFromContent(match),tm.getTopicID(),tm.getStart(),tm.getEnd(),match,this));
				} while (matcher.findNext());
			}
		}
		return payload;
	}

	public static void main(String[] args) throws Exception {
		NLUConfig config = NLU.getNLUConfig("testNLU");
		config.setForcedNLUContentRoot("C:\\Users\\morbini\\simcoach2\\svn_dcaps\\trunk\\core\\DM\\resources\\characters\\Ellie_DCAPS_AI\\nlu\\");
		NLBusConfig busconfig=(NLBusConfig) NLBusConfig.WIN_EXE_CONFIG.clone();
		busconfig.setNluConfig(config);
		NLU component=(NLU) NLBusBase.createSubcomponent(config, config.getNluClass());
		Preprocess preprocess = component.getPreprocess(PreprocessingType.RUN);
		List<List<Token>> out = preprocess.process("i want to eat a pig and an apple but also a lot of chickens");
		System.out.println(preprocess.getStrings(out));
		/*
		WordlistRENE t = new WordlistRENE("C:\\Users\\morbini\\simcoach2\\svn_dcaps\\trunk\\core\\DM\\resources\\characters\\Ellie_DCAPS_AI\\nlu\\test");
		List<NE> r = t.extractNamedEntitiesFromText("i want to eat a pig and an apple but also a lot of chickens", null);
		System.out.println(r);
		System.out.println(BasicNE.createPayload(r));
		*/
	}
}
