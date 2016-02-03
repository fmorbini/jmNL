package edu.usc.ict.nl.nlu.preprocessing;

import java.util.List;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.nlu.Token;

public interface PreprocesserI {
	public void run(List<List<Token>> input,PreprocessingType type);
	public void setNlu(NLU nlu);
}
