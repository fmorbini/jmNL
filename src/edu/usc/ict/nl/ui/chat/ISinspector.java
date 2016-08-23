package edu.usc.ict.nl.ui.chat;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.config.DMConfig;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.EvalContext;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.nl.kb.cf.TestRewardDM;

public class ISinspector extends JPanel implements KeyListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private DM dm;
	private DialogueKB is;
	private int level=0;
	private JLabel currentISLabel=new JLabel("", 10);
	private JTable list;
	private JCheckBox useInheritance=new JCheckBox("Inheritance",true);
	private DefaultTableModel listModel=new DefaultTableModel(new Object[]{"Name","Value"},0);
	private JTextArea input,output;
	private AbstractAction evalAction=new AbstractAction("Eval") {
		@Override
		public void actionPerformed(ActionEvent e) {
			String f=input.getText();
			try {
				DialogueKBFormula pf=DialogueKBFormula.parse(f);
				if (pf!=null) input.setText(pf.toString());
				DialogueKB is=selectIS(ISinspector.this.is, level);
				Object result = is.evaluate(pf,new EvalContext(is,null));
				output.setText(result!=null?result.toString():null);
			} catch (Exception ee) {
				input.setToolTipText(ee.getMessage());
				try {
					DialogueOperatorEffect ef=DialogueOperatorEffect.parse(f);
					if (ef!=null) input.setText(ef.toString());
					is.store(ef, ACCESSTYPE.AUTO_OVERWRITETHIS, true);
					refreshTable();
				} catch (Exception eee) {
					eee.printStackTrace();
					input.setToolTipText(eee.getMessage());
				}
			}
		}
	};

	
	public ISinspector(DM dm) {
		super(new GridBagLayout());
		this.dm=dm;

		//Create the list and put it in a scroll pane.
		list = new JTable(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane listScrollPane = new JScrollPane(list);

		JButton leftButton = new JButton(new AbstractAction("Down") {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (level>0) level--;
				refreshSelectedIndex();
			}
		});
		JButton rightButton = new JButton(new AbstractAction("Up") {
			@Override
			public void actionPerformed(ActionEvent e) {
				level++;
				refreshSelectedIndex();
			}
		});
		JButton refreshButton = new JButton(new AbstractAction("Refresh") {
			@Override
			public void actionPerformed(ActionEvent e) {
				refreshIS();
			}

		});

		output=new JTextArea();
		output.setLineWrap(true);
		output.setWrapStyleWord(true);
		output.setEditable(false);
		JScrollPane outputPane = new JScrollPane(output); 
		input=new JTextArea();
		input.setLineWrap(true);
		input.setWrapStyleWord(true);
		input.setEditable(true);
		input.addKeyListener(this);
		JScrollPane inputPane = new JScrollPane(input); 
		JButton evalButton = new JButton(evalAction);
		
		//Create a panel that uses BoxLayout.
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane,
				BoxLayout.LINE_AXIS));
		buttonPane.add(leftButton);
		buttonPane.add(Box.createHorizontalStrut(5));
		buttonPane.add(currentISLabel);
		buttonPane.add(Box.createHorizontalStrut(5));
		buttonPane.add(rightButton);
		buttonPane.add(Box.createHorizontalStrut(5));
		buttonPane.add(refreshButton);
		buttonPane.add(Box.createHorizontalStrut(5));
		buttonPane.add(useInheritance);
		buttonPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		//add(searchBox,BorderLayout.NORTH);

		JPanel bottomButtonPane = new JPanel();
		bottomButtonPane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.gridx = 0;
		c.gridy = 0;
		c.anchor=GridBagConstraints.WEST;
		c.fill=GridBagConstraints.BOTH;
		bottomButtonPane.add(inputPane,c);
		c = new GridBagConstraints();
		c.weightx = 0;
		c.weighty = 1;
		c.gridx = 1;
		c.gridy = 0;
		c.anchor=GridBagConstraints.CENTER;
		c.fill=GridBagConstraints.BOTH;
		bottomButtonPane.add(evalButton,c);
		c = new GridBagConstraints();
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.gridx = 2;
		c.gridy = 0;
		c.anchor=GridBagConstraints.EAST;
		c.fill=GridBagConstraints.BOTH;
		bottomButtonPane.add(outputPane,c);

		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty=0;
		c.weightx=1;
		c.gridwidth = 3;
		c.anchor=GridBagConstraints.PAGE_START;
		c.fill=GridBagConstraints.BOTH;
		add(buttonPane,c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.weighty=.5;
		c.weightx=1;
		c.gridwidth = 3;
		c.anchor=GridBagConstraints.CENTER;
		c.fill=GridBagConstraints.BOTH;
		add(listScrollPane,c);
		c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 2;
		c.weighty=.5;
		c.weightx=1;
		c.gridwidth = 3;
		c.anchor=GridBagConstraints.PAGE_END;
		c.fill=GridBagConstraints.BOTH;
		add(bottomButtonPane,c);
		//add(buttonPane, BorderLayout.PAGE_START);
		//add(listScrollPane, BorderLayout.CENTER);
		//add(bottomButtonPane, BorderLayout.PAGE_END);
		//add(buttonPane, BorderLayout.PAGE_END);

		refreshIS();
		refreshTable();
	}

	private void refreshIS() {
		ISinspector.this.is=dm.getInformationState();
		level=0;
		refreshSelectedIndex();
	}

	private void refreshTable() {
		DialogueKB selectedIS=selectIS(is,level);
		try {
			Collection<DialogueOperatorEffect> tmp = useInheritance.isSelected()?selectedIS.flattenKBTree(selectedIS.dumpKBTree()):selectedIS.dumpKB();
			if (tmp!=null) {
				List<DialogueOperatorEffect> content = new ArrayList<>(tmp);
				Collections.sort(content,new Comparator<DialogueOperatorEffect>() {
					@Override
					public int compare(DialogueOperatorEffect o1,DialogueOperatorEffect o2) {
						if (o1!=null) {
							return o1.compareUsingStrings(o2);
						}
						else return -1; 
					}
				});
				int rows=listModel.getRowCount();
				if (rows>0) listModel.setRowCount(0);
				Object[] row=new Object[]{null,null};
				for(DialogueOperatorEffect c:content) {
					if (c.isAssertion()) {
						row[0]=c.getAssertedFormula();
						row[1]=c.getAssertionSign();
					} else if (c.isAssignment()) {
						row[0]=c.getAssignedVariable();
						row[1]=c.getAssignedExpression();
					} else if (c.isImplication()) {
						row[0]="=>";
						row[1]=c.toString();
					} else {
						row[0]="FIXME";
						row[1]=c.toString();
					}
					listModel.addRow(row);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void refreshSelectedIndex() {
		DialogueKB selectedIS=selectIS(is,level);
		String name=useInheritance.isSelected()?selectedIS.getName():selectedIS.getID()+"";
		currentISLabel.setText(name);
		refreshTable();
	}

	private DialogueKB selectIS(DialogueKB is, int level) {
		DialogueKB currentIS=is;
		while (level>0) {
			DialogueKB tmp=currentIS.getParent();
			if (tmp!=null) currentIS=tmp;
			else break;
			level--;
		}
		return currentIS;
	}

	public static void createAndShowGUI(DM dm, String character) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			
			public void run() {
				try {
					//Create and set up the window.
					JFrame frame = new JFrame("IS inspector for "+character);
					frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

					//Create and set up the content pane.
					JComponent newContentPane = new ISinspector(dm);
					newContentPane.setOpaque(true); //content panes must be opaque
					frame.setContentPane(newContentPane);

					//Display the window.
					frame.pack();
					frame.setVisible(true);
				} catch (Exception e) {e.printStackTrace();}
			}
		});
	}
	
	public static void main(String[] args) throws Exception {
		DM dm=new TestRewardDM(DMConfig.WIN_EXE_CONFIG); 
		createAndShowGUI(dm,"test");
	}

	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();
		int mod=e.getModifiers();
		if (key == KeyEvent.VK_ENTER && (mod&KeyEvent.CTRL_MASK)!=0) {
			evalAction.actionPerformed(null);
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

}
