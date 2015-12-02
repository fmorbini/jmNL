package edu.usc.ict.nl.ui.chat;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.Highlighter;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.usc.ict.nl.bus.ExternalListenerInterface;
import edu.usc.ict.nl.bus.NLBus;
import edu.usc.ict.nl.bus.events.DMInterruptionRequest;
import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.bus.events.NLUEvent;
import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.bus.modules.NLG;
import edu.usc.ict.nl.bus.modules.NLGInterface;
import edu.usc.ict.nl.bus.modules.NLUInterface;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.nlg.SpeechActWithProperties;
import edu.usc.ict.nl.nlg.echo.EchoNLG;
import edu.usc.ict.nl.nlg.echo.EchoNLGData;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.XMLUtils;
import edu.usc.ict.nl.utils.LogConfig;
import edu.usc.ict.nl.vhmsg.VHBridge;
import edu.usc.ict.vhmsg.MessageEvent;
import edu.usc.ict.vhmsg.MessageListener;

public class ChatInterface extends JPanel implements KeyListener, WindowListener, ActionListener, ExternalListenerInterface {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public static final long chatInterfaceSingleSessionID=999l;

	private static final int hSize=400,vSize=600;
	private static boolean startMinimized=false;

	private static final KeyStroke reloadKey=KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.CTRL_MASK);
	private static final KeyStroke trainKey=KeyStroke.getKeyStroke(KeyEvent.VK_T, ActionEvent.CTRL_MASK);
	private static final KeyStroke pauseKey=KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,ActionEvent.CTRL_MASK);

	private boolean personalizedSessionID=false;

	static protected AbstractApplicationContext context;

	private static final DefaultHighlightPainter defaultHighlighter=new DefaultHighlightPainter(new Color(213, 255, 177));

	private VHBridge vhBridge=null;
	private MessageListener createVHDMMessageListener() {
		return new MessageListener() {
			public void messageAction(MessageEvent e) {
				Map<String, ?> map = e.getMap();
				if (map!=null) {
					String msg=(String) map.get("dm");
					if (msg!=null && msg.equals("reset")) {
						try {
							reloadLock.acquire();
							sid=nlModule.startSession(nlModule.getCharacterName4Session(chatInterfaceSingleSessionID),chatInterfaceSingleSessionID);
						} catch (Exception e1) {
							displayError(e1,false);
						} finally {
							reloadLock.release();
						}
					} else if (msg!=null && msg.equals("login")) {
						handleLoginEvent();
					}
				}
			}
		};
	}

	private StyledDocument doc;
	private Highlighter h;
	private JScrollPane listScrollPane;
	private JTextField input;
	private JTextArea nluOutput;
	private JTextArea selectedNluOutput;
	private boolean nluOutputEnabled;
	private Feedback feedback=null;
	private JScrollPane feedbackScrollPane=null;
	private JButton sendFeedback;
	private final JMenuItem showDMReplies = new JCheckBoxMenuItem();

	FormList formResponseList=null;
	JScrollPane formScrollPane=null;

	private static NLBus nlModule=null;
	private static Long sid=null;
	private File chatLogFileName=null;

	private static ChatInterface _instance;

	private static int line=0;

	private static JMenuBar menuBar = new JMenuBar();

	private static JFrame window;

	private final AbstractAction retrainNluAction=new AbstractAction("Retrain NLU and reload model.") {
		@Override
		public void actionPerformed(ActionEvent e) {
			reTrainNLU();
		}
	};
	private final AbstractAction showForm=new AbstractAction() {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			input.setVisible(false);
			formScrollPane.setVisible(true);
			try {
				getInstance().validate();
			} catch (Exception e) {}
		}
	};
	private final AbstractAction sayAnything=new AbstractAction("Say something else") {
		@Override
		public void actionPerformed(ActionEvent arg0) {
			formScrollPane.setVisible(false);
			input.setVisible(true);
			enableInput(null);
			try {
				getInstance().validate();
			} catch (Exception e) {}
		}
	};
	private static final String LF="Leave feedback",GC="Save feedback and go back to chat";
	private final AbstractAction leaveFeedback=new AbstractAction(LF) {
		@Override
		public void actionPerformed(ActionEvent e) {
			toggleDisplayState();
		}
	};
	private enum MessageType {USER,SYSTEM,SPECIALDM};
	private enum MainDisplayStatus {FEEDBACK,CHAT};
	private MainDisplayStatus displayState=MainDisplayStatus.CHAT;

	public void setDisplayAccordingToState() {
		DM dm=getDM(sid);
		switch (displayState) {
		case CHAT:
			if (feedback.hasFeedback()) {
				if (dm!=null && dm.getPauseEventProcessing()) dm.setPauseEventProcessing(false);
				sendFeedback.setText(LF);
				feedback.getEditorPane().setVisible(false);
				feedbackScrollPane.setVisible(false);
			}
			listScrollPane.setVisible(true);
			break;
		case FEEDBACK:
			if (feedback.hasFeedback()) {
				sendFeedback.setEnabled(true);
				if (dm!=null) dm.setPauseEventProcessing(true);
				sendFeedback.setText(GC);
				feedback.getEditorPane().setVisible(true);
				feedbackScrollPane.setVisible(true);
				listScrollPane.setVisible(false);
			}
			break;
		}
	}
	private void toggleDisplayState() {
		switch (displayState) {
		case CHAT:
			displayState=MainDisplayStatus.FEEDBACK;
			break;
		case FEEDBACK:

			if (!feedback.isCompleted()) {
				JOptionPane.showMessageDialog(getInstance(),
						"Plase give an answer to all questions, thanks!",
						"Warning",
						JOptionPane.OK_OPTION);
			} else {
				try {
					feedback.saveFeedback(chatLogFileName,feedback.getFormValues());
				} catch (IOException e) {
					e.printStackTrace();
				}
				displayState=MainDisplayStatus.CHAT;
			}
			break;
		}
		setDisplayAccordingToState();
	}

	public ChatInterface(NLBus nl) {
		super(new GridBagLayout());
		nlModule=nl;
		nlModule.addBusListener(this);
		NLBusConfig config=nlModule.getConfiguration();
		_instance=this;

		//Create the list and put it in a scroll pane.
		JTextPane list = new JTextPane();
		list.setEnabled(true);
		list.setEditable(false);
		doc = list.getStyledDocument();
		h = list.getHighlighter();

		addStyles(doc,config);
		listScrollPane = new JScrollPane(list);
		listScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		listScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

		//Create a panel that uses BoxLayout.
		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane,BoxLayout.Y_AXIS));
		buttonPane.setAlignmentX(Component.LEFT_ALIGNMENT);

		nluOutputEnabled=config.getDisplayNluOutputChat();
		nluOutput=new JTextArea();
		nluOutput.setVisible(showDMReplies.isSelected());
		buttonPane.add(nluOutput);
		nluOutput.setAlignmentX(Component.LEFT_ALIGNMENT);
		selectedNluOutput=new JTextArea();
		selectedNluOutput.setVisible(nluOutputEnabled);
		Font f=new Font("SansSerif", Font.BOLD, (int) (12*config.getZoomFactorChat()));
		selectedNluOutput.setFont(f);

		buttonPane.add(selectedNluOutput);
		selectedNluOutput.setAlignmentX(Component.LEFT_ALIGNMENT);


		formResponseList=new FormList();
		formScrollPane = new JScrollPane(formResponseList);
		formScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		formScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		//setFormForEvent(new DMSpeakEvent(null, "questionnaire.ptsd.1", sid, null, null));
		formScrollPane.setVisible(false);
		buttonPane.add(formScrollPane);
		formScrollPane.setAlignmentX(Component.LEFT_ALIGNMENT);

		input=new JTextField();
		buttonPane.add(input);
		buttonPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
		input.setAlignmentX(Component.LEFT_ALIGNMENT);
		input.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent e) {
			}
			public void removeUpdate(DocumentEvent e) {
				setSpeackingState();
			}
			public void insertUpdate(DocumentEvent e) {
				setSpeackingState();
			}
			private void setSpeackingState() {
				try {
					String text=input.getText();
					if (!StringUtils.isEmptyString(StringUtils.removeLeadingAndTrailingSpaces(text))) {
						nlModule.setSpeakingStateVarForSessionAs(sid, true);
					} else {
						nlModule.setSpeakingStateVarForSessionAs(sid, false);
					}
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		input.addKeyListener(this);
		list.addKeyListener(this);

		try {
			feedback=new Feedback(config);
		} catch (Exception e) {e.printStackTrace();}
		if (feedback!=null && feedback.hasFeedback()) {
			buttonPane.add(sendFeedback=new JButton(leaveFeedback));
			sendFeedback.setAlignmentX(Component.LEFT_ALIGNMENT);
			JEditorPane feedbackEditorPane=feedback.getEditorPane();
			feedbackScrollPane = new JScrollPane(feedbackEditorPane);
			feedbackScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			feedbackScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			feedbackScrollPane.setVisible(false);
		}

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weighty=1;
		c.weightx=1;
		c.gridwidth = 3;
		c.anchor=GridBagConstraints.PAGE_START;
		c.fill=GridBagConstraints.BOTH;
		add(listScrollPane,c);
		if (feedback!=null && feedback.hasFeedback()) {
			c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 0;
			c.weighty=0.9;
			c.weightx=1;
			c.gridwidth = 3;
			c.anchor=GridBagConstraints.PAGE_START;
			c.fill=GridBagConstraints.BOTH;
			add(feedbackScrollPane,c);
		}
		c = new GridBagConstraints();
		c.fill=GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridwidth = 3;
		c.gridy = 2;
		c.weightx=1;
		c.weighty=0.2;
		c.anchor=GridBagConstraints.PAGE_END;
		add(buttonPane,c);
	}

	private void addStyles(StyledDocument doc, NLBusConfig config) {
		Style def = StyleContext.getDefaultStyleContext().
				getStyle(StyleContext.DEFAULT_STYLE);

		Style regular = doc.addStyle("regular", def);
		StyleConstants.setFontFamily(regular, "SansSerif");
		StyleConstants.setForeground(regular, Color.BLACK);
		StyleConstants.setFontSize(regular, (int) (StyleConstants.getFontSize(regular)*config.getZoomFactorChat()));

		Style s = doc.addStyle("lineNumber", regular);
		StyleConstants.setFontSize(s, (int) (StyleConstants.getFontSize(s)/1.5));

		s = doc.addStyle("systemText", regular);
		//StyleConstants.setItalic(s, true);
		StyleConstants.setBold(s, true);
		//StyleConstants.setLeftIndent(s, 160);
		//StyleConstants.setFirstLineIndent(s, 200);

		s = doc.addStyle("userText", regular);
		StyleConstants.setForeground(s, Color.RED);

		s = doc.addStyle("specialDM", regular);
		StyleConstants.setItalic(s, true);
		StyleConstants.setForeground(s, Color.GREEN);
	}

	private int startNew=0,endNew=0;
	@Override
	public void handleNLGEvent(Long sessionID,NLGEvent nlgOutput) throws Exception {
		h.removeAllHighlights();
		startNew=doc.getLength();

		String systext=nlgOutput.getName();
		if (StringUtils.isEmptyString(systext)) {
			systext="("+nlgOutput.getDMEventName()+")";
		}
		addTextToList(systext,MessageType.SYSTEM);

		DMSpeakEvent ev=nlgOutput.getPayload();
		if (ev!=null) {
			if (isFormPreferred(ev) && setFormForEvent(ev)) {
				showForm.actionPerformed(null);
			} else {
				sayAnything.actionPerformed(null);
			}
		}

		endNew=doc.getLength();
		try {
			h.addHighlight(startNew, endNew, defaultHighlighter);
		} catch (BadLocationException e) {
			displayError(e,false);
		}
	}
	@Override
	public void handleTextUtteranceEvent(Long sessionId, String text) {
		addTextToList(text,MessageType.USER);
		if (showDMReplies.isSelected()) {
			try {
				NLUInterface nlu = nlModule.getNlu(sid);
				NLUConfig nluConfig=nlu.getConfiguration();
				List<NLUOutput> nluNbestList = nlu.getNLUOutput(text, null,nluConfig.getnBest());
				StringBuffer sb=new StringBuffer();
				if (nluNbestList!=null) {
					List<String> originalNLUOutputLabels=(List)FunctionalLibrary.map(nluNbestList, NLUOutput.class.getMethod("getId"));
					DM dm=nlModule.getDM(sid);
					NLGInterface nlg=nlModule.getNlg(sid);
					Map<NLUOutput, List<List<String>>> dmPossibleReplies = dm.getPossibleSystemResponsesForThesePossibleInputs(nluNbestList);
					Iterator<NLUOutput> itNew=nluNbestList.iterator();
					Iterator<String> itOld=originalNLUOutputLabels.iterator();
					while(itNew.hasNext()) {
						NLUOutput nluAfterDM=itNew.next();
						String originalNLU=itOld.next();
						if (!originalNLU.equals(nluAfterDM)) {
							sb.append(nluAfterDM.getId()+"(was "+originalNLU+")\n");
						} else {
							sb.append(nluAfterDM.getId()+"\n");
						}
						List<List<String>> reponsesWithAlternatives=dmPossibleReplies.get(nluAfterDM);
						if (reponsesWithAlternatives!=null) {
							int i=1;
							for(List<String> responses:reponsesWithAlternatives) {
								if (reponsesWithAlternatives.size()>1) sb.append(" "+i+"\n");
								for(String r:responses) {
									NLGEvent nlgResponse = nlg.doNLG(sid, new DMSpeakEvent(null, r, sid, null, dm.getInformationState()), null,true);
									sb.append("  "+nlgResponse.getName()+"\n");
								}
								i++;
							}
						}
					}
				}
				nluOutput.setText(sb.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else nluOutput.setText("");
	}
	@Override
	public void handleNLUEvent(Long sessionId,NLUEvent selectedUserSpeechAct) {
		if (selectedUserSpeechAct!=null) {
			NLUOutput nluOutput=selectedUserSpeechAct.getPayload();
			String text=null;
			if (nluOutput!=null) {
				try {
					text=XMLUtils.prettyPrintXMLString(nluOutput.toXml()," ",false);
				} catch (Exception e) {
					text=nluOutput.toString();
				}
			} else {
				text=selectedUserSpeechAct.getName();
			}
			selectedNluOutput.setText(text);
		}
	}
	@Override
	public void handleDMSpeakEvent(DMSpeakEvent arg0) throws Exception {
	}
	@Override
	public void handleDMInterruptionRequestEvent(DMInterruptionRequest arg0) throws Exception {
		addTextToList("interrupt", MessageType.SPECIALDM);
	}
	@Override
	public Long startSession(String characterName,Long sid) {
		try {
			this.sid=sid;
			setInterfaceForSessionRestarted(characterName);
		} catch (Exception e) {
			NLBus.logger.error("Error while restarting the session.",e);
		}
		return null;
	}
	boolean finishedSession=false;
	boolean alreadyAskedFeedbackAtEnd=false;
	@Override
	public void terminateSession(Long sid) {
		try {
			if (ChatInterface.sid!=null) {
				if (!ChatInterface.sid.equals(sid)) NLBus.logger.warn("chat interface received a terminated session with id ("+sid+") different from chat session id: "+ChatInterface.sid);
				ChatInterface.sid=null;
				if (input.isEnabled() && !finishedSession) {
					disableInput("");
					addTextToList("END SESSION",MessageType.SYSTEM);
					finishedSession=true;
				}
				if (!alreadyAskedFeedbackAtEnd) {
					alreadyAskedFeedbackAtEnd=true;
					displayState=MainDisplayStatus.FEEDBACK;
					setDisplayAccordingToState();
				}
			}
		} catch (Exception e) {
			displayError(e,true);
		}
	}

	private void disableInput(String msg) {
		if (msg!=null) input.setText(msg);
		input.setEnabled(false);
		if (StringUtils.isEmptyString(msg)) input.setVisible(false);

	}
	private void enableInput(String msg) {
		if (sendFeedback!=null) sendFeedback.setEnabled(true);
		input.setVisible(true);
		if (msg!=null) input.setText(msg);
		input.setEnabled(true);
		input.requestFocus();
	}
	private void reTrainNLU() {
		disableInput("training in progress");
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					DM dm=nlModule.getDM(sid);
					boolean pauseEventProcessing=dm.getPauseEventProcessing();
					if (!pauseEventProcessing) dm.setPauseEventProcessing(true);
					NLUInterface nlu = nlModule.getNlu(sid);
					NLUConfig nluConfig=nlu.getConfiguration();
					nlu.retrain();
					nlu.loadModel(new File(nluConfig.getNluModelFile()));
					if (!pauseEventProcessing) dm.setPauseEventProcessing(false);
					enableInput("");
				} catch (Exception e) {
					displayError(e,false);
				}
			}
		});
	}
	private static final Semaphore reloadLock=new Semaphore(1);
	public void startDefaultCharacter() {
		NLBusConfig config=nlModule.getConfiguration();
		String character=config.getCharacter();
		if (character!=null && characterActions!=null) {
			HashSet<String> setOfCharacters=new HashSet<String>(characterActions.keySet());
			while(!setOfCharacters.isEmpty()) {
				setOfCharacters.remove(character);
				try {
					JRadioButton a=characterActions.get(character);
					if (a!=null) {
						a.setSelected(true);
						a.getAction().actionPerformed(null);
						break;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (!setOfCharacters.isEmpty()) character=setOfCharacters.iterator().next();
			}
		}
	}
	private void setInterfaceForSessionRestarted(String character) throws Exception {
		if (!StringUtils.isEmptyString(character)) {
			finishedSession=false;
			alreadyAskedFeedbackAtEnd=false;
			try {
				String pid=(getPersonalizedSessionID())?getPersonalSessionID():"";
				enableInput("");
				
				doc.remove(0, doc.getLength());
				displayState=MainDisplayStatus.CHAT;
				setDisplayAccordingToState();

				line=0;
				if (sid!=null) {
					DM dm = nlModule.getDM(sid,false);
					if (dm!=null) {
						dm.setPersonalSessionID(pid);
						chatLogFileName=dm.getCurrentChatLogFile();
					}

					if (window!=null) {
						window.setTitle(buildTitleString(sid));
					}

					displayState=MainDisplayStatus.CHAT;
					setDisplayAccordingToState();
				} else {
					System.out.println(Arrays.toString(Thread.currentThread().getStackTrace()));
					JOptionPane.showMessageDialog(getInstance(),
							"No character data was found for name '"+character+"'.",
							"No Character found",
							JOptionPane.WARNING_MESSAGE);
				}

				/*
			dm.setPauseEventProcessing(true);
			NLUInterface nlu = nlModule.getNlu(sid);
			Map<String, ConfusionEntry> cmx = nlu.computeConfusionMatrix();
			nlu.printConfusionMatrix(cmx);
			dm.setPauseEventProcessing(true);
				 */
			} catch (Exception e) {
				throw(e);
			}
		} else {
			NLBus.logger.warn("Restart session called in chat interface with empty character: '"+character+"'");
		}
	}

	public void startupDM() {
		try {
			NLBusConfig config=nlModule.getConfiguration();
			String vhTopic=config.getVhTopic();
			if (vhTopic!=null) {
				vhBridge=new VHBridge(config.getVhServer(),vhTopic, "dm", createVHDMMessageListener());
				NLBus.logger.info("started vh message listener in "+this.getClass().getCanonicalName());
			}
			nlModule.startup();

			String character=config.getCharacter();
			Map<String, String> availableCharacters = nlModule.getAvailableCharacterNames();
			setMenuBar(availableCharacters,character,config);

			if (doRetraining) {
				reTrainNLU();
				NLBus.stop();
				System.exit(0);
			}
		} catch (Exception e) {
			displayError(e,true);
		}
	}
	private void displayError(Exception e,boolean exit) {
		e.printStackTrace();
		JOptionPane.showMessageDialog(getInstance(),
				((e.getStackTrace()!=null)?StringUtils.getStackTrace(e):e.toString()),
				"Error",
				JOptionPane.ERROR_MESSAGE);
		NLBus.logger.error("Error in chat interface.", e);
		if (exit) System.exit(1);
	}

	private Map<String,JRadioButton> characterActions;
	private void setMenuBar(Map<String, String> availableCharacters,String currentCharacter,NLBusConfig config) {
		JMenu characterMenu = new JMenu("Characters");
		menuBar.add(characterMenu);

		final JMenuItem pauseEventsMenuItem = new JCheckBoxMenuItem();
		pauseEventsMenuItem.setAction(new AbstractAction("Pause DM event processing.") {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (sid!=null) {
					DM dm=getDM(sid);
					if (dm!=null) {
						boolean state=!dm.getPauseEventProcessing();
						dm.setPauseEventProcessing(state);
						pauseEventsMenuItem.setSelected(state);
					}
				}
			}
		});
		pauseEventsMenuItem.setAccelerator(pauseKey);
		pauseEventsMenuItem.setSelected(false);

		showDMReplies.setAction(new AbstractAction("Show possible DM replies") {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean newState=showDMReplies.isSelected();
				showDMReplies.setSelected(newState);
				nluOutput.setVisible(newState);
			}
		});
		showDMReplies.setSelected(false);

		if (availableCharacters!=null) {
			ButtonGroup group = new ButtonGroup();
			for(final String c:availableCharacters.keySet()) {
				if (characterActions==null) characterActions=new HashMap<String, JRadioButton>();
				JRadioButton menuItem = new JRadioButton(new AbstractAction(c) {
					@Override
					public void actionPerformed(ActionEvent e) {
						try {
							reloadLock.acquire();
							sid=nlModule.startSession(c,chatInterfaceSingleSessionID);
							pauseEventsMenuItem.setSelected(false);
						} catch (Exception e1) {
							displayError(e1,false);
						} finally {
							reloadLock.release();
						}
					}
				});
				characterActions.put(c,menuItem);
				group.add(menuItem);
				characterMenu.add(menuItem);
				if (c.equals(currentCharacter)) menuItem.setSelected(true);
			}
		}

		JMenu menu = new JMenu("NLU");
		menuBar.add(menu);
		JMenuItem menuItem = new JMenuItem(retrainNluAction);
		menuItem.setAccelerator(trainKey);
		menuItem.setEnabled(config.getAllowNluTraining());
		menu.add(menuItem);

		menu = new JMenu("DM");
		menuBar.add(menu);
		menu.add(pauseEventsMenuItem);
		menu.add(new JMenuItem(new AbstractAction("Save current information state.") {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					DM dm = nlModule.getDM(sid, false);
					dm.setPauseEventProcessing(true);
					try {
						nlModule.saveInformationStateForSession(sid,true);
					} catch (Exception t) {
						displayError(t,false);
					}
					dm.setPauseEventProcessing(false);
				} catch (Exception e1) {
					displayError(e1,false);
				}
			}
		}));
		menu.add(new JMenuItem(new AbstractAction("Load information state from file.") {
			@Override
			public void actionPerformed(ActionEvent e) {
				final JFileChooser fc = new JFileChooser();
				int returnVal = fc.showOpenDialog(ChatInterface.this);

				if (returnVal == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					try {
						DM dm = nlModule.getDM(sid, false);
						if (dm!=null) {
							dm.setPauseEventProcessing(true);
							try {
								nlModule.loadInformationStateForSession(sid, file);
							} catch (Exception t) {
								displayError(t,false);
							}
							dm.setPauseEventProcessing(false);
						}
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
				}
			}
		}));

		menu.add(new JMenuItem(new AbstractAction("Refresh current character policy.") {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					nlModule.refreshPolicyForCharacter(nlModule.getCharacterName4Session(sid));
				} catch (Exception e1) {
					displayError(e1, false);
				}
			}
		}));
		menu.add(new JMenuItem(new AbstractAction("Send login event") {
			@Override
			public void actionPerformed(ActionEvent e) {
				handleLoginEvent();
			}
		}));
		menu.addSeparator();
		menu.add(showDMReplies);

		menu = new JMenu("NLG");
		menuBar.add(menu);
		menu.add(new JMenuItem(new AbstractAction("Reload NLG data.") {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					NLGInterface nlg = nlModule.getNlg(sid);
					if (nlg!=null) nlg.reloadData(true);
				} catch (Exception t) {
					displayError(t,false);
				}
			}
		}));
		menu.add(new JMenuItem(new AbstractAction("Speaks all lines for current character.") {
			@Override
			public void actionPerformed(ActionEvent e) {
				DM dm=null;
				if (sid!=null && (dm=getDM(sid))!=null) {
					try {
						Map<String, List<SpeechActWithProperties>> lines=getLines(nlModule.getNlg(sid),dm);
						LineTester.createAndShowGUI(nlModule, sid,lines);
					} catch (Exception ex) {ex.printStackTrace();}
				}
			}
		}));

	}

	protected Map<String, List<SpeechActWithProperties>> getLines(final NLGInterface nlg, final DM dm) throws Exception {
		Map<String, List<SpeechActWithProperties>> ret = nlg.getAllLines();
		List<DMSpeakEvent> pls = dm.getAllPossibleSystemLines();
		if (pls!=null) {
			Set<String> psas=null;
			for(DMSpeakEvent ev:pls) {
				if (ev!=null) {
					String sa=ev.getName();
					if (!StringUtils.isEmptyString(sa)) {
						if (psas==null) psas=new HashSet<String>();
						psas.add(sa);
					}
				}
			}
			if (psas!=null) {
				for(List<SpeechActWithProperties> ls:ret.values()) {
					if (ls!=null) {
						for(SpeechActWithProperties l:ls) {
							l.setProperty(NLG.PROPERTY_USED,psas.contains(l.getSA())+"");
						}
					}
				}
			}
		}
		return ret;
	}
	private void handleLoginEvent() {
		try {
			nlModule.handleLoginEvent(sid,null);
		} catch (Exception e) {
			displayError(e,false);
		}
	}

	private class FormList extends JList implements ListSelectionListener, ListCellRenderer {
		public FormList() {
			super(new DefaultListModel());
			addListSelectionListener(this);
			setCellRenderer(this);
		}
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			String name=(String) ((AbstractAction)value).getValue(Action.NAME);
			JTextArea label=new JTextArea();
			label.setText(name);
			if (isSelected) {
				label.setBackground(list.getSelectionBackground());
				label.setForeground(list.getSelectionForeground());
			}
			else {
				label.setBackground(list.getBackground());
				label.setForeground(list.getForeground());
			}
			label.setEnabled(list.isEnabled());
			label.setFont(list.getFont());
			label.setOpaque(true);
			return label;
		}
		@Override
		public void valueChanged(ListSelectionEvent e) {
			if (e.getValueIsAdjusting() == false) {
				int i=getSelectedIndex();
				if (i>-1) {
					DefaultListModel model=(DefaultListModel) getModel();
					AbstractAction action=(AbstractAction) model.get(i);
					action.actionPerformed(null);
				}
			}
		}
	}

	private boolean isFormPreferred(DMSpeakEvent ev) {
		NLGInterface nlg;
		try {
			nlg = nlModule.getNlg(sid);
			DialogueKBInterface is = nlg.getKBForEvent(ev);
			return DM.isFormPreferred(ev,is);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	public void sendForcedNLU(String nluSA,long sid) throws Exception {
		// simulates the initial login event
		NLUInterface nlu = nlModule.getNlu(sid);
		DM dm=nlModule.getDM(sid);
		List<NLUOutput> userSpeechActs = nlu.getNLUOutputFake(new String[]{"1 "+nluSA}, null);
		NLUOutput selectedUserSpeechAct=dm.selectNLUOutput(nluSA,sid, userSpeechActs);
		nlModule.handleNLUEvent(sid, new NLUEvent(selectedUserSpeechAct, sid));
	}
	private boolean setFormForEvent(DMSpeakEvent ev) throws Exception {
		if (ev!=null) {
			String sa=ev.getName();
			NLGInterface nlg = nlModule.getNlg(sid,false);
			EchoNLGData data = nlg.getData();
			Map<String, List<Pair<String, String>>> formsResponses = data.getFormResponses();
			if (formsResponses!=null) {
				List<Pair<String, String>> choices=formsResponses.get(sa);
				if (choices!=null) {
					DefaultListModel listModel = (DefaultListModel) formResponseList.getModel();
					listModel.clear();
					for(Pair<String, String>c:choices) {
						String text=c.getSecond();
						DialogueKBInterface is = nlg.getKBForEvent(ev);
						text=EchoNLG.resolveTemplates(text,is);
						final String nlu=c.getFirst();
						AbstractAction action=new AbstractAction(text) {
							@Override
							public void actionPerformed(ActionEvent e) {
								try {
									addTextToList(nlu,MessageType.USER);
									sendForcedNLU(nlu, sid);
								} catch (Exception e1) {
									e1.printStackTrace();
								}
							}
						};
						listModel.addElement(action);
					}
					listModel.addElement(sayAnything);
					formResponseList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
					formResponseList.setLayoutOrientation(JList.VERTICAL);
					formResponseList.setVisibleRowCount(choices.size()+1);

					formScrollPane.setViewportView(formResponseList);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Create the GUI and show it.  For thread safety,
	 * this method should be invoked from the
	 * event-dispatching thread.
	 * @throws Exception 
	 */
	private static ChatInterface createAndShowGUI() throws Exception {
		//Create and set up the window.
		window = new JFrame(buildTitleString(ChatInterface.chatInterfaceSingleSessionID));
		window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		window.setState(startMinimized?JFrame.ICONIFIED:JFrame.NORMAL);

		ChatInterface newContentPane = getInstance();

		float zoom=ChatInterface.nlModule.getConfiguration().getZoomFactorChat();
		window.setPreferredSize(new Dimension((int)(hSize*zoom), (int)(vSize*zoom)));
		window.addWindowListener(newContentPane);
		//Create and set up the content pane.
		newContentPane.setOpaque(true); //content panes must be opaque
		window.setContentPane(newContentPane);

		window.setJMenuBar(menuBar);

		//Display the window.
		window.pack();
		window.setVisible(true);
		newContentPane.input.requestFocusInWindow();
		return newContentPane;
	}

	private static String buildTitleString(Long sid) {
		return "Session '"+sid+"', chat with: '"+nlModule.getCharacterName4Session(sid)+"'";
	}

	private static ChatInterface getInstance() {
		return _instance;
	}
	@Override
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();
		int mod=e.getModifiers();
		if (key == KeyEvent.VK_ENTER) {
			String text=input.getText();
			input.setText("");

			try {
				nlModule.setSpeakingStateVarForSessionAs(sid, false);
				nlModule.handleTextUtteranceEvent(sid, text);
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		} else if (key==reloadKey.getKeyCode() && ((mod&reloadKey.getModifiers())>0)) {			
			try {
				reloadLock.acquire();
				sid=nlModule.startSession(nlModule.getCharacterName4Session(sid),chatInterfaceSingleSessionID);
			} catch (Exception e1) {
				displayError(e1,false);
			} finally {
				reloadLock.release();
			}
		}
	}


	@Override
	public void keyReleased(KeyEvent e) {
	}


	private void addTextToList(String text,MessageType type) {
		try {
			doc.insertString(doc.getLength(), (++line)+": ", doc.getStyle("lineNumber"));
			switch (type) {
			case USER:
				doc.insertString(doc.getLength(), text+"\n", doc.getStyle("userText"));
				break;
			case SYSTEM:
				//String character=nlModule.getCharacterName4Session(sid);
				//doc.insertString(doc.getLength(), character+": ", doc.getStyle("lineNumber"));
				doc.insertString(doc.getLength(), text+"\n", doc.getStyle("systemText"));
				break;
			case SPECIALDM:
				doc.insertString(doc.getLength(), text+"\n", doc.getStyle("specialDM"));
				break;
			default:
				break;
			}
		} catch (BadLocationException e) {
			displayError(e,false);
		}
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				JScrollBar vs = listScrollPane.getVerticalScrollBar();
				if (vs!=null) vs.setValue(vs.getMaximum());
			}
		});
	}
	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub

	}

	private static String springConfig=null;
	private static boolean doRetraining=false;

	public static void init() {
		System.out.println("Initializing Chat configuration.");
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );		

		if (springConfig==null)
			//context = new ClassPathXmlApplicationContext(new String[] {"chatInterfaceVHToolkit.xml"});		
			context = new ClassPathXmlApplicationContext(new String[] {"chatInterface.xml"});		
		//context = new ClassPathXmlApplicationContext(new String[] {"simcoachChatInterface.xml"});		
		else 
			context = new ClassPathXmlApplicationContext(new String[] {springConfig});		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static final String TRAIN_NLU_OPTION="t",HELP_OPTION="h",SPRING_CONFIG="s",START_MINIMIZED="m";
	private static final Options options = new Options();
	static {
		options.addOption(TRAIN_NLU_OPTION, false, "re-trains the NLU on the files specified in the spring config file.");
		options.addOption(SPRING_CONFIG, true, "specifies the spring config file to load.");
		options.addOption(HELP_OPTION, false, "Request this help message to be printed.");
		options.addOption(START_MINIMIZED, false, "Request the gui to start minimized.");
	}
	private static void printUsageHelp() {
		HelpFormatter f = new HelpFormatter();
		f.printHelp("[-"+TRAIN_NLU_OPTION+"]"+"[-"+SPRING_CONFIG+"]", options);
	}
	private static void digestCommandLineArguments(String[] args) throws Exception {
		CommandLineParser parser = new PosixParser();
		try {
			CommandLine cmd = parser.parse( options, args);
			if ( cmd.hasOption(HELP_OPTION) ) {
				printUsageHelp();
			} else {
				doRetraining=cmd.hasOption(TRAIN_NLU_OPTION);
				if (cmd.hasOption(SPRING_CONFIG)) {
					springConfig=cmd.getOptionValue(SPRING_CONFIG);
				}
				if ( cmd.hasOption(START_MINIMIZED) ) {
					startMinimized=true;
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub

	}
	@Override
	public void windowClosed(WindowEvent e) {
	}
	@Override
	public void windowClosing(WindowEvent e) {
		if (nlModule!=null) {
			nlModule.terminateSession(sid);
			//displayState=MainDisplayStatus.FEEDBACK;
			//setDisplayAccordingToState();
			try {
				nlModule.stop();
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			nlModule.destroy();
		}
		System.exit(0);
	}
	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub

	}
	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub

	}
	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub

	}
	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub

	}

	private DM getDM(Long sid) {
		if (nlModule!=null) {
			try {
				DM dm=nlModule.getDM(sid);
				return dm;
			} catch (Exception e) {}
		}
		return null;
	}

	private static final Pattern getCharacterPattern=Pattern.compile("^Chat with: (.*)$");
	private String getCharacterFromMenuTitle(String title) {
		Matcher matcher=getCharacterPattern.matcher(title);
		if (matcher.matches() && matcher.groupCount()==1) {
			String character=matcher.group(1);
			return character;
		} else return null;
	}
	@Override
	public void actionPerformed(ActionEvent action) {
	}


	public boolean getPersonalizedSessionID() { return personalizedSessionID;}
	public void setPersonalizedSessionID(boolean x) {this.personalizedSessionID=x;}
	public String getPersonalSessionID() {
		String init=String.format("%05d", (int)(Math.random()*100000.0));
		String s = (String)JOptionPane.showInputDialog(
				window,
				"Insert here your personal session identifier or accept the one printed below (if any)",
				"Personal session ID",
				JOptionPane.QUESTION_MESSAGE,null,null,init);
		return s;
	}

	public static void main(final String[] args) throws Exception {
		digestCommandLineArguments(args);
		init();
		//Schedule a job for the event-dispatching thread:
		//creating and showing this application's GUI.
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					ChatInterface ui = createAndShowGUI();
					ui.startupDM();
					ui.startDefaultCharacter();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

}
