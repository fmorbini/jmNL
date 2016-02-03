package edu.usc.ict.nl.nlu.preprocessing.normalization;

import java.util.List;
import java.util.ListIterator;

import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.preprocessing.Preprocesser;

public abstract class Normalizer extends Preprocesser implements NormalizerI {

	@Override
	public void run(List<List<Token>> input,PreprocessingType type) {
		if (input!=null) {
			ListIterator<List<Token>> it=input.listIterator();
			while(it.hasNext()) {
				List<Token> option=it.next();
				normalize(option,type);
				it.set(option);
			}
		}
	}
}
