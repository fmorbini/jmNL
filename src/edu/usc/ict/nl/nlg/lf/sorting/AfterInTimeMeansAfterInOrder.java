package edu.usc.ict.nl.nlg.lf.sorting;

import java.util.List;

import edu.usc.ict.nl.nlg.lf.time.Query;
import edu.usc.ict.nl.util.graph.Node;

public class AfterInTimeMeansAfterInOrder implements EvalLinearization {

	float reward=0;
	
	public AfterInTimeMeansAfterInOrder(float itemReward) {
		this.reward=itemReward;
	}
	
	@Override
	public float reward(List<Node> linearization) {
		float reward=0;
		int s=linearization.size();
		for(int i=0;i<s-1;i++) {
			for(int j=i+1;j<s;j++) {
				Node n1=linearization.get(i);
				Node n2=linearization.get(j);
				if (Query.after(n2, n1) || !Query.before(n2, n1)) {
					reward+=this.reward;
				}
			}
		}
		return reward;
	}

}
