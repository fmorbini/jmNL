package edu.usc.ict.nl.dm.visualizer.prefuse;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;

import prefuse.Visualization;
import prefuse.data.Edge;
import prefuse.data.Node;
import prefuse.data.Tree;
import prefuse.data.tuple.TupleSet;
import prefuse.visual.VisualItem;
import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEntranceTransition;
import edu.usc.ict.nl.dm.reward.possibilityGraph.PossibleIS;
import edu.usc.ict.nl.dm.reward.possibilityGraph.PossibleTransition;
import edu.usc.ict.nl.dm.visualizer.BaseDMVisualizer;
import edu.usc.ict.nl.dm.visualizer.VisualizerConfig;
import edu.usc.ict.nl.dm.visualizer.kbDisplay.VarDisplay;
import edu.usc.ict.nl.util.StringUtils;

public class RewardDMVisualizerPrefuse extends BaseDMVisualizer {
	private DMHistoryGraph dialogArea2;
	//private JList kbArea;
	private JScrollPane kbAreaScrollPane;
	private static final DefaultHighlightPainter defaultHighlighter=new DefaultHighlightPainter(new Color(213, 255, 177));

	private final Tree t = new Tree();
	private Node n;

	public RewardDMVisualizerPrefuse(DM dm, Rectangle bounds) throws Exception {
		if (dm!=null) {
			this.dm=dm;

			// create tree and add root node.
			Node r=this.t.addRoot();
			this.t.addColumn("name", String.class);
			r.set("name","START");
			this.n=r;

			String configFile=dm.getConfiguration().getVisualizerConfig();
			startGUI(bounds, configFile,"RewardDM visualizer for session: "+dm.getSessionID());
		}
	}

	public void startGUI(Rectangle bounds,String configFile,String title) throws Exception {
		this.config=new VisualizerConfig(configFile);
		

		//Create and set up the window.
		frame = new JFrame(title);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

		Container root=frame.getContentPane();
		createGUI(root,config);

		//Add contents to the window.
		frame.add(this);

		//Display the window.
		frame.pack();
		if (bounds!=null) {
			frame.setBounds(bounds);
			frame.validate();
		}
		frame.setVisible(true);
		frame.addWindowListener(this);
	}
	
	private void createGUI(Container pane, VisualizerConfig config) {
		pane.setLayout(new GridBagLayout());
		
        dialogArea2 = new DMHistoryGraph(t, "name");

        JPanel kbArea = new JPanel();
		kbArea.setLayout(new BoxLayout(kbArea, BoxLayout.Y_AXIS));
		Collection<VarDisplay> es=config.getKBGraphicalElements();
		if (es!=null) {
			for(VarDisplay e:es) {
				kbArea.add(Box.createVerticalGlue()); 
				kbArea.add((Component) e);
			}
			kbArea.add(Box.createVerticalGlue()); 
		}
		kbAreaScrollPane = new JScrollPane(kbArea);

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		if (config.getDHEnabled()) pane.add(dialogArea2,c);
		c = new GridBagConstraints();
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 0.5;
		if (config.getISEnabled()) pane.add(kbAreaScrollPane,c);
	}

	public void addSearchResult(List<PossibleTransition> possibilities, PossibleIS bestPIS) {
		try {
			if (possibilities!=null) {
				StringBuffer msg=new StringBuffer();
				Node selectedNode=null;
				for(PossibleTransition ctr:possibilities) {
					DialogueOperatorEntranceTransition ec = ctr.getEntranceCondition();
					
					String opName=ec.getOperator().getName();
					if (!StringUtils.isEmptyString(opName)) {
						
						DecimalFormat df = new DecimalFormat("#.##");
						//msg.append(" "+ec.getOperator().getName()+" ("+df.format(ctr.getReward())+")\n");
	
						Node nn = t.addChild(n);
						if (selectedNode==null) selectedNode=nn;
						nn.set("name",ec.getOperator().getName());
						Edge edge=t.getEdge(n, nn);
						edge.set("name",df.format(ctr.getReward()));
						VisualItem vn = dialogArea2.m_vis.getVisualItem(dialogArea2.NODES, nn);
						TupleSet ts = dialogArea2.m_vis.getFocusGroup(Visualization.FOCUS_ITEMS);
						ts.setTuple(vn);
						dialogArea2.m_vis.run("filter");
					}
				}
				if (selectedNode!=null) n=selectedNode;
			}
		} catch (Exception e) {}//e.printStackTrace();}
	}

	public static void main(String[] args) throws Exception {
		RewardDMVisualizerPrefuse r=new RewardDMVisualizerPrefuse(null, null);
		r.startGUI(null, "dmVisualizer.xml","x");
		while(true){
			Node nn = r.t.addChild(r.n);//t.addNode();
			//Edge edge = t.addEdge(n, nn);
			nn.set("name",System.currentTimeMillis()+"");
			Edge edge=r.t.getEdge(r.n, nn);
			edge.set("name","xy");
			VisualItem vn = r.dialogArea2.m_vis.getVisualItem(r.dialogArea2.NODES, nn);
			TupleSet ts = r.dialogArea2.m_vis.getFocusGroup(Visualization.FOCUS_ITEMS);
			ts.setTuple(vn);
			r.dialogArea2.m_vis.run("filter");

			r.n=nn;
			
			Thread.sleep(3000);
		}
	}
}
