package edu.usc.ict.nl.nlu.ne;

import java.util.List;

import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.Token;

public interface NamedEntityExtractorI {
	public List<NE> extractNamedEntitiesFromText(String text) throws Exception;
	public boolean isNEAvailableForSpeechAct(NE ne,String sa);
	public void setConfiguration(NLUConfig configuration);
	public List<SpecialVar> getSpecialVariables() throws Exception;
	public boolean generalize(List<Token> tokens);
	public List<Token> getModifiedTokens(List<Token> tokens);
}
