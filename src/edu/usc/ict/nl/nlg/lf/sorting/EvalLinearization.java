package edu.usc.ict.nl.nlg.lf.sorting;

import java.util.List;

import edu.usc.ict.nl.util.graph.Node;

public interface EvalLinearization {
	public float reward(List<Node> linearization) throws Exception;
}
