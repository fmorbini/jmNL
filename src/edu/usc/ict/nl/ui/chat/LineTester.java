package edu.usc.ict.nl.ui.chat;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import edu.usc.ict.nl.bus.NLBus;
import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.bus.modules.NLGInterface;

public class LineTester extends JPanel implements ListSelectionListener {
	private JList list;
	private DefaultListModel listModel;
	private JLabel extraInfoLabel=null;

	private Long sessionID=null;
	private NLBus bus=null;

	public LineTester(final NLBus nlModule, final Long sid, final List<DMSpeakEvent> lines) throws Exception {
		super(new BorderLayout());

		Collections.sort(lines,new Comparator<DMSpeakEvent>() {
			@Override
			public int compare(DMSpeakEvent o1, DMSpeakEvent o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		
		this.bus=nlModule;
		this.sessionID=sid;

		final NLGInterface nlg = nlModule.getNlg(sessionID);

		listModel = new DefaultListModel();
		for(DMSpeakEvent ev:lines) {
			ev.setSessionID(sessionID);
			listModel.addElement(ev);
			if (ev!=null) {
				NLGEvent nlgEvent=nlg.doNLG(sessionID, ev, true);
				if (nlgEvent!=null) {
					System.out.println(ev.getName()+"\t"+nlgEvent.getName());
				} else {
					System.out.println(ev.getName()+"\t#######################failure retrieving line!");
				}
			}
		}

		//Create the list and put it in a scroll pane.
		list = new JList(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.addListSelectionListener(this);
		list.setSelectedIndex(0);
		list.setVisibleRowCount(5);
		JScrollPane listScrollPane = new JScrollPane(list);

		JButton playButton = new JButton(new AbstractAction("Play selected") {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					int index=list.getSelectedIndex();
					DMSpeakEvent ev = (DMSpeakEvent)listModel.getElementAt(index);
					if (ev!=null) {
						ev.setSessionID(sessionID);
						NLGEvent nlgEvent = nlg.doNLG(sessionID, ev, false);
						nlModule.handleNLGEvent(sessionID, nlgEvent);
						Float duration=nlg.getDurationOfThisDMEvent(sessionID, nlgEvent);
						if (duration!=null && duration>0) {
							Thread.sleep(Math.round(duration*1200f));
						}
						index++;
						updateSelectedIndex(index);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		});

		extraInfoLabel = new JLabel("", 10);
		updateSelectedIndex(list.getSelectedIndex());

		//Create a panel that uses BoxLayout.
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane,
				BoxLayout.LINE_AXIS));
		buttonPane.add(playButton);
		buttonPane.add(Box.createHorizontalStrut(5));
		buttonPane.add(new JSeparator(SwingConstants.VERTICAL));
		buttonPane.add(Box.createHorizontalStrut(5));
		buttonPane.add(extraInfoLabel);
		buttonPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

		add(listScrollPane, BorderLayout.CENTER);
		add(buttonPane, BorderLayout.PAGE_END);
	}

	private void updateSelectedIndex(int index) {
		list.setSelectedIndex(index);
		DMSpeakEvent ev = (DMSpeakEvent)listModel.getElementAt(list.getSelectedIndex());
		if (ev!=null) {
			extraInfoLabel.setText(ev.getName());
		}
		list.ensureIndexIsVisible(index);
	}

	//This method is required by ListSelectionListener.
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting() == false) {
		}
	}

	/**
	 * Create the GUI and show it.  For thread safety,
	 * this method should be invoked from the
	 * event-dispatching thread.
	 * @throws Exception 
	 */
	public static void createAndShowGUI(final NLBus nlModule,final Long sid,final List<DMSpeakEvent> lines) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					//Create and set up the window.
					JFrame frame = new JFrame("ListDemo");
					frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

					//Create and set up the content pane.
					JComponent newContentPane = new LineTester(nlModule, sid, lines);
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