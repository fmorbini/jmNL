package edu.usc.ict.nl.ui.chat;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
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
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKB;

public class ISinspector extends JPanel {

	private DialogueKB is;
	private int level=0;
	private JLabel currentISLabel;
	private JTable list;
	private JCheckBox useInheritance;
	private DefaultTableModel listModel;
	
	public ISinspector(DialogueKB is) {
		super(new BorderLayout());
		this.is=is;
		
		useInheritance = new JCheckBox("Inheritance");
		useInheritance.setSelected(true);

		listModel = new DefaultTableModel(new Object[]{"Name","Value"},0);
		JTable table = new JTable(listModel);
		updateTable();
		
		//Create the list and put it in a scroll pane.
		list = new JTable(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane listScrollPane = new JScrollPane(list);

		JButton leftButton = new JButton(new AbstractAction("Down") {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (level>0) level--;
				updateSelectedIndex();
			}
		});
		JButton rightButton = new JButton(new AbstractAction("Up") {
			@Override
			public void actionPerformed(ActionEvent e) {
				level++;
				updateSelectedIndex();
			}
		});


		currentISLabel = new JLabel("", 10);
		updateSelectedIndex();

        
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
		buttonPane.add(useInheritance);
		buttonPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		//add(searchBox,BorderLayout.NORTH);
		add(buttonPane, BorderLayout.PAGE_START);
		add(listScrollPane, BorderLayout.CENTER);
		//add(buttonPane, BorderLayout.PAGE_END);
	}

	private void updateTable() {
		DialogueKB selectedIS=selectIS(is,level);
		try {
			Collection<DialogueOperatorEffect> tmp = useInheritance.isSelected()?selectedIS.flattenKBTree(selectedIS.dumpKBTree()):selectedIS.dumpKB();
			if (tmp!=null) {
				List<DialogueOperatorEffect> content = new ArrayList<>(tmp);
				Collections.sort(content,new Comparator<DialogueOperatorEffect>() {
					@Override
					public int compare(DialogueOperatorEffect o1,DialogueOperatorEffect o2) {
						if (o1!=null) return o1.compareUsingStrings(o2);
						else return -1; 
					}
				});
				int rows=listModel.getRowCount();
				if (rows>0) for(int i=0;i<rows;i++) listModel.removeRow(i);
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

	private void updateSelectedIndex() {
		DialogueKB selectedIS=selectIS(is,level);
		String name=useInheritance.isSelected()?selectedIS.getName():selectedIS.getID()+"";
		currentISLabel.setText(name);
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

	public static void createAndShowGUI(DM dm) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			
			public void run() {
				try {
					//Create and set up the window.
					JFrame frame = new JFrame("ListDemo");
					frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

					//Create and set up the content pane.
					JComponent newContentPane = new ISinspector(dm.getInformationState());
					newContentPane.setOpaque(true); //content panes must be opaque
					frame.setContentPane(newContentPane);

					//Display the window.
					frame.pack();
					frame.setVisible(true);
				} catch (Exception e) {e.printStackTrace();}
			}
		});
	}

}
