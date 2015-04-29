package edu.usc.ict.nl.dm.visualizer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;

import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEntranceTransition;
import edu.usc.ict.nl.dm.reward.possibilityGraph.PossibleIS;
import edu.usc.ict.nl.dm.reward.possibilityGraph.PossibleTransition;
import edu.usc.ict.nl.dm.visualizer.kbDisplay.VarDisplay;
import edu.usc.ict.nl.util.StringUtils;

public class RewardDMVisualizer extends BaseDMVisualizer {
	private JTable dialogArea;
	private JList dialogArea2;
	//private JList kbArea;
	private JScrollPane dialogAreaScrollPane;
	private JScrollPane kbAreaScrollPane;
	private Map<String,Integer> var2row;
	private static final DefaultHighlightPainter defaultHighlighter=new DefaultHighlightPainter(new Color(213, 255, 177));

	public RewardDMVisualizer(DM dm, Rectangle bounds) throws Exception {
		if (dm!=null) {
			this.dm=dm;
			String configFile=dm.getConfiguration().getVisualizerConfig();
			this.config=new VisualizerConfig(configFile);
			
			this.var2row=new HashMap<String, Integer>();

			//Create and set up the window.
			frame = new JFrame("RewardDM visualizer for session: "+dm.getSessionID());
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
	}

	private void createGUI(Container pane, VisualizerConfig config) {
		pane.setLayout(new GridBagLayout());
		
		DefaultListModel model=new DefaultListModel();
		dialogArea2=new JList(model);
		dialogArea2.setCellRenderer(new RewardDMInfoCellRenderer());
		dialogArea2.setVisibleRowCount(1);
		dialogArea2.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		dialogArea2.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		dialogAreaScrollPane = new JScrollPane(dialogArea2,JScrollPane.VERTICAL_SCROLLBAR_NEVER,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		dialogAreaScrollPane.setPreferredSize(new Dimension(200,100));
		dialogArea2.addListSelectionListener(new MyListSelectionListener(dialogArea2,dialogAreaScrollPane));
		
		/*model = new DefaultListModel();
		kbArea = new JList(model);
		kbArea.setCellRenderer(new KBCellRenderer());*/
		JPanel kbArea = new JPanel();
		kbArea.setLayout(new BoxLayout(kbArea, BoxLayout.Y_AXIS));
		Collection<VarDisplay> es=config.getKBGraphicalElements();
		if (es!=null) {
			for(VarDisplay e:es) {
				kbArea.add((Component) e);
			}
		}
		kbAreaScrollPane = new JScrollPane(kbArea);

		GridBagConstraints c = new GridBagConstraints();
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		pane.add(dialogAreaScrollPane,c);
		c = new GridBagConstraints();
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 0.8;
		pane.add(kbAreaScrollPane,c);
	}

	public class MyTableModel extends DefaultTableModel {
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			return String.class;
		}
	}
	
	public class RewardDMInfoCellRenderer  extends JTextArea implements ListCellRenderer {

		@Override
		public Component getListCellRendererComponent(
				JList list,
				Object value,            // value to display
				int index,               // cell index
				boolean isSelected,      // is the cell selected
				boolean cellHasFocus)    // the list and the cell have the focus
		{	
			Font font = new Font("Serif", Font.BOLD, 12);
			setFont(font);
		    FontMetrics fm = getFontMetrics(font);
			String text=(value!=null)?value.toString():"null";
			int columns=StringUtils.getColumns(text);
			int rows=StringUtils.getRows(text);
			
			String[] lines=text.split("\n");
		    int width=0;
		    for(String l:lines) {int nw=fm.stringWidth(l); if (nw>width) width=nw;}
		    int heigth=fm.getHeight()*rows;
			setText(text);
			if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			}
			else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			setPreferredSize(new Dimension(width, heigth));
			setEnabled(list.isEnabled());
			setOpaque(true);
			return this;
		}
	}
	public class KBCellRenderer  extends JTextArea implements ListCellRenderer {

		@Override
		public Component getListCellRendererComponent(
				JList list,
				Object value,            // value to display
				int index,               // cell index
				boolean isSelected,      // is the cell selected
				boolean cellHasFocus)    // the list and the cell have the focus
		{	    	 
			String text=(value!=null)?value.toString():"null";
		    this.setFont(new Font("Serif", Font.BOLD, 20));
			this.setText(text);
			this.setWrapStyleWord(true);                    
			this.setLineWrap(true);
			/*if (isSelected) {
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			}
			else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}*/
			setEnabled(list.isEnabled());
			setOpaque(true);
			return this;
		}
	}
	
	

	public class MyListSelectionListener implements ListSelectionListener {
		private JScrollPane scrollPane;
		private JList list;

		public MyListSelectionListener(JList dialogArea2,
				JScrollPane dialogAreaScrollPane) {
			this.list=dialogArea2;
			this.scrollPane=dialogAreaScrollPane;
		}

		@Override
		public void valueChanged(ListSelectionEvent e) {
			try {
				if (e.getValueIsAdjusting() == false) {
					DefaultListModel model=(DefaultListModel) list.getModel();
					int lastIndex = model.size()-1;
					int selectedIndex = list.getSelectedIndex();
					if ((lastIndex<0) || (selectedIndex>=lastIndex)) {
						SwingUtilities.invokeLater(new Runnable() {
							@Override
							public void run() {
								JScrollBar vs = scrollPane.getHorizontalScrollBar();
								if (vs!=null) vs.setValue(vs.getMaximum());
							}
						});
					}
				}
			} catch (Exception e1) {}//e1.printStackTrace();}
		}
	}
	
	public void addSearchResult(List<PossibleTransition> possibilities, PossibleIS bestPIS) {
		try {
			if (possibilities!=null) {
				StringBuffer msg=new StringBuffer();
				for(PossibleTransition ctr:possibilities) {
					DialogueOperatorEntranceTransition ec = ctr.getEntranceCondition();
					DecimalFormat df = new DecimalFormat("#.##");
					msg.append(" "+ec.getOperator().getName()+" ("+df.format(ctr.getReward())+")\n");
				}
				//(bestPIS!=null)?bestPIS.reason():null
				//DefaultTableModel  model = (DefaultTableModel) dialogArea.getModel();
				//model.addColumn(null, new Object[]{msg.toString()});
				String txt=msg.toString();
				
				/*int pos=txt.indexOf('\n');
				if (pos<0) pos=txt.length();
				final JTextArea txtArea=new JTextArea();
				txtArea.setFont(new Font("Serif", Font.BOLD, 20));
				txtArea.setText(txt);
				if (h!=null) h.removeAllHighlights();
				h=txtArea.getHighlighter();
				h.addHighlight(0,pos, defaultHighlighter);*/
				DefaultListModel model=(DefaultListModel) dialogArea2.getModel();
				model.addElement(txt);
				dialogArea2.setSelectedIndex(model.size()-1);
/*
				dialogAreaScrollPane.revalidate();
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						JScrollBar hs = dialogAreaScrollPane.getHorizontalScrollBar();
						if (hs!=null) hs.setValue(hs.getMaximum());
						hs = dialogAreaScrollPane.getVerticalScrollBar();
						if (hs!=null) hs.setValue(hs.getMinimum());
					}
				});*/
			}
		} catch (Exception e) {}//e.printStackTrace();}
	}
}
