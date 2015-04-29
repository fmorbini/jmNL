package edu.usc.ict.nl.nlu.multi.merger;

import edu.usc.ict.nl.nlu.ChartNLUOutput;

public interface Merger {
	public String getNluNameToBeCalledForThisNluAndResult(String nluName,String result);
	public ChartNLUOutput mergeResults(ChartNLUOutput result) throws Exception;
}
