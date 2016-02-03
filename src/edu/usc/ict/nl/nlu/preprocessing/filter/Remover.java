package edu.usc.ict.nl.nlu.preprocessing.filter;

import java.util.Iterator;
import java.util.List;

import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.preprocessing.Preprocesser;
import edu.usc.ict.nl.nlu.preprocessing.PreprocesserI;
import edu.usc.ict.nl.util.StringUtils;

public abstract class Remover extends Preprocesser implements RemoverI {
	
	@Override
	public void run(List<List<Token>> input,PreprocessingType type) {
		if (input!=null) {
			for(List<Token> pi:input) {
				Iterator<Token> it=pi.iterator();
				while(it.hasNext()) {
					Token t=it.next();
					if (t!=null && t.isType(Token.TokenTypes.WORD)) {
						if (remove(t)) it.remove();
					}
				}
			}
		}
	}


}
