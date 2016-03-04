package edu.usc.ict.nl.ui.chat;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Document;

import edu.usc.ict.nl.bus.NLBusInterface;
import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.bus.modules.NLG;
import edu.usc.ict.nl.bus.modules.NLGInterface;
import edu.usc.ict.nl.nlg.SpeechActWithProperties;
import edu.usc.ict.nl.util.StringUtils;

public class LineTester extends JPanel implements ListSelectionListener, ActionListener, DocumentListener {
	private JList list;
	private DefaultListModel listModel;
	private JLabel extraInfoLabel=null;

	private Long sessionID=null;
	private NLBusInterface bus=null;
	private LineSearcher search;
	private List<SpeechActWithProperties> lines=new ArrayList<SpeechActWithProperties>();

	public LineTester(final NLBusInterface nlModule, final Long sid, final Map<String, List<SpeechActWithProperties>> linesRaw) throws Exception {
		super(new BorderLayout());
		if (linesRaw!=null) {
			for(List<SpeechActWithProperties> ls:linesRaw.values()) {
				lines.addAll(ls);
			}
		}
		Collections.sort(lines,new Comparator<SpeechActWithProperties>() {
			@Override
			public int compare(SpeechActWithProperties o1, SpeechActWithProperties o2) {
				int diff=o1.getProperty(NLG.PROPERTY_SA).compareTo(o2.getProperty(NLG.PROPERTY_SA));
				if (diff==0) diff=o1.getProperty(NLG.PROPERTY_ROW).compareTo(o2.getProperty(NLG.PROPERTY_ROW));
				return diff;
			}
		});
		
		search=new LineSearcher(linesRaw);
		this.bus=nlModule;
		this.sessionID=sid;

		
		JTextField searchBox = new JTextField();
		searchBox.addActionListener(this);
		searchBox.getDocument().addDocumentListener(this);

		listModel = new DefaultListModel();
		addLines();

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
					SpeechActWithProperties ev = (SpeechActWithProperties)listModel.getElementAt(index);
					NLGInterface nlg = nlModule.getNlg(sessionID);
					if (ev!=null && nlg!=null) {
						NLGEvent nlgEvent = nlg.doNLG(sessionID, new DMSpeakEvent(null,ev.getText(), sessionID, null,null),ev, false);
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
		add(searchBox,BorderLayout.NORTH);
		add(listScrollPane, BorderLayout.CENTER);
		add(buttonPane, BorderLayout.PAGE_END);
	}

	private void addLines() {
		for(SpeechActWithProperties l:lines) {
			listModel.addElement(l);
		}
	}

	private void updateSelectedIndex(int index) {
		list.setSelectedIndex(index);
		SpeechActWithProperties ev = (SpeechActWithProperties)listModel.getElementAt(list.getSelectedIndex());
		if (ev!=null) {
			extraInfoLabel.setText(ev.toString(true));
		}
		list.ensureIndexIsVisible(index);
	}

	//This method is required by ListSelectionListener.
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting() == false && extraInfoLabel!=null) {
			int p = list.getSelectedIndex();
			if (p>=0) {
				SpeechActWithProperties ev = (SpeechActWithProperties)listModel.getElementAt(p);
				if (ev!=null) {
					extraInfoLabel.setText(ev.toString(true));
				}
			}
		}
	}

	/**
	 * Create the GUI and show it.  For thread safety,
	 * this method should be invoked from the
	 * event-dispatching thread.
	 * @throws Exception 
	 */
	public static void createAndShowGUI(final NLBusInterface nlModule,final Long sid,final Map<String, List<SpeechActWithProperties>> lines) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			
			public void run() {
				try {
					//Create and set up the window.
					JFrame frame = new JFrame("NLG inspector for character "+nlModule.getCharacterName4Session(sid));
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

	public List<SpeechActWithProperties> findAndUpdateList(Document searchBox) {
		try {
			String query=searchBox.getText(0, searchBox.getLength());
			if (StringUtils.isEmptyString(query)) {
				listModel.clear();
				addLines();
			} else {
				List<SpeechActWithProperties> result = search.find(query,Integer.MAX_VALUE);
				listModel.clear();
				if (result!=null) {
					for(SpeechActWithProperties c:result) {
						listModel.addElement(c);
					}
				}
				return result;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public void insertUpdate(DocumentEvent e) {
		Document doc = e.getDocument();
		findAndUpdateList(doc);
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		Document doc = e.getDocument();
		findAndUpdateList(doc);
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		Document doc = e.getDocument();
		findAndUpdateList(doc);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}
	

}