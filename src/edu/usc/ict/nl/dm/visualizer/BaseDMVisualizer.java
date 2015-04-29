package edu.usc.ict.nl.dm.visualizer;

import java.awt.Rectangle;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;

import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.dm.reward.possibilityGraph.PossibleIS;
import edu.usc.ict.nl.dm.reward.possibilityGraph.PossibleTransition;
import edu.usc.ict.nl.dm.visualizer.kbDisplay.Variable;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;

public class BaseDMVisualizer extends JPanel implements WindowListener, DMVisualizerI {

	protected JFrame frame=null;
	protected VisualizerConfig config; 
	protected DM dm;

	@Override
	public void addSearchResult(List<PossibleTransition> possibilities,
			PossibleIS bestPIS) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updatedKB() {
		try {
			if (dm!=null) {
				DialogueKBInterface kb = dm.getInformationState();
				if (kb!=null) {
					Map<String, Variable> vs=config.getTrackedVariables();
					if (vs!=null && !vs.isEmpty()) {
						for(String vn:vs.keySet()) {
							Variable v=vs.get(vn);
							Object vv = kb.getValueOfVariable(vn,ACCESSTYPE.AUTO_OVERWRITEAUTO,null);
							v.setValue(vv);
						}
					}
				}
			}
		} catch (Exception e) {e.printStackTrace();}
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public void kill() {
		frame.dispose();
	}
	
	@Override
	public Rectangle getBounds() {
		return frame.getBounds();
	}
	
}
