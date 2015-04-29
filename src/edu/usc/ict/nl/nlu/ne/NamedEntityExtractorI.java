package edu.usc.ict.nl.nlu.ne;

import java.util.List;
import java.util.Map;

import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.Token;

public interface NamedEntityExtractorI {
	public Map<String, Object> extractPayloadFromText(String text, String speechAct) throws Exception;
	public void setConfiguration(NLUConfig configuration);
	public Token generalize(Token input);
	public List<SpecialVar> getSpecialVariables() throws Exception;

}
