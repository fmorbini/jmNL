package edu.usc.ict.nl.nlu;


public abstract class NLUProcess {
	protected int nBest;
	public void train(String model, String trainingFile) throws Exception {}
	public void run(String model, int nb) throws Exception {}
	public void kill() {}
	public String[] classify(String u) throws Exception { return classify(u, nBest); }
	public String[] classify(String u, int nBest) throws Exception { return null; }
}
