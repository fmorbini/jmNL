package edu.usc.ict.nl.nlu.ne;

import java.util.List;

import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.nlu.Token;

public interface NamedEntityExtractorI {
	public boolean isNEAvailableForSpeechAct(NE ne,String sa);
	public void setConfiguration(NLUConfig configuration);
	public List<SpecialVar> getSpecialVariables() throws Exception;
	
	public List<NE> extractNamedEntitiesFromText(String text,PreprocessingType type) throws Exception;
	public boolean generalize(List<Token> tokens,PreprocessingType type);
	public List<Token> getModifiedTokens(List<Token> tokens,PreprocessingType type);
	
	/**
	 * is the generalize flag is set to true then this ne will be used to generalize text, otherwise only to extract variables.
	 * @return
	 */
	public boolean generalizeText();
}
