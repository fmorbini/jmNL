package edu.usc.ict.nl.nlu.preprocessing.normalization;

import java.util.List;

import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.preprocessing.PreprocesserI;

public interface NormalizerI extends PreprocesserI {
	public List<Token> normalize(List<Token> t,PreprocessingType type);
}
