package edu.usc.ict.nl.dm.visualizer;

import java.awt.Rectangle;
import java.util.List;

import edu.usc.ict.nl.dm.reward.possibilityGraph.PossibleIS;
import edu.usc.ict.nl.dm.reward.possibilityGraph.PossibleTransition;

public interface DMVisualizerI {
	public void addSearchResult(List<PossibleTransition> possibilities, PossibleIS bestPIS);
	public void updatedKB();
	public Rectangle getBounds();
	public void kill();
}
