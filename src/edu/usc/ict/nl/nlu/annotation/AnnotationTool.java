package edu.usc.ict.nl.nlu.annotation;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Event;
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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.io.BuildTrainingData;
import edu.usc.ict.nl.util.AhoCorasick;
import edu.usc.ict.nl.util.AhoCorasick.LazyArray;
import edu.usc.ict.nl.util.AhoCorasick.Match;
import edu.usc.ict.nl.util.AhoCorasick.MatchList;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.utils.ExcelUtils;

public class AnnotationTool extends JDialog implements WindowListener, ActionListener, DocumentListener, KeyListener {

	private JTextField searchBox;
	private JLabel counter;
	private JTextArea utteranceBox;
	private JList list;
	private DefaultListModel listModel;
    private JList answerList;
    private List<String> possibleLabels;
    private List<TrainingDataFormat> toBeAnnotated;
	private int currentPosition;
    private AnnotationTool instance=null;
    private Event event=null;
    private ArrayList<Integer> newLinesInLabelsString=null;

    public AnnotationTool(Collection<String> labels, List<TrainingDataFormat> tds) throws IOException, SecurityException, IllegalArgumentException, ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        super();
        setTitle("Wizard");
        setModal(true);
		addWindowListener(this);
		JPanel contentPane = new JPanel(new BorderLayout());
		populateGUI(contentPane);
		contentPane.setOpaque(true); //content panes must be opaque
		
		//Create and set up the window.
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setContentPane(contentPane);
        this.toBeAnnotated=tds;
        currentPosition=0;
        updateList(labels);
        setUtteranceBox();

		//Display the window.
		pack();
		setVisible(true);
        instance=this;
	}

    public String extractTextFromUtterance(Object u) {
        return u==null?"":u.toString();
    }

    private String possibleLabelsString=null;
    private void updateList(Collection<String> ias) {
        boolean filterOnly=!isEmptyString(possibleLabelsString);
        if (!filterOnly) {
            possibleLabelsString="";
            possibleLabels = new ArrayList<String>(ias);
        }
        listModel.setSize(possibleLabels.size());
        int i=0;
        for(Object a:possibleLabels) {
            if (!filterOnly) possibleLabelsString+=extractTextFromUtterance(a)+"\n";
            listModel.set(i++, a);
        }
        if (!filterOnly) {
            possibleLabelsString=possibleLabelsString.toLowerCase();
            newLinesInLabelsString=getPositionOfNewLinesInString(possibleLabelsString);
        }
	}

    private void setUtteranceBox() {
    	TrainingDataFormat td=toBeAnnotated.get(currentPosition);
    	utteranceBox.setText(td.getUtterance());
    	counter.setText((currentPosition+1)+"/"+toBeAnnotated.size());
	}

	private void populateGUI(JPanel contentPane) {
		counter=new JLabel();
        JPanel utterancePanel = new JPanel();
        utterancePanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        utterancePanel.add(counter,c);
		utteranceBox = new JTextArea(2, 30);
		utteranceBox.setEditable(false);
		utteranceBox.setForeground(Color.GREEN);
		utteranceBox.setFont(new Font("Serif", Font.BOLD, 20));
		utteranceBox.setLineWrap(true);
		utteranceBox.setWrapStyleWord(true);
        JButton up = new JButton("Next");
        up.setActionCommand("next");
        up.addActionListener(this);
        JButton down = new JButton("Prev");
        down.setActionCommand("prev");
        down.addActionListener(this);
        utterancePanel.add(down,c);
        utterancePanel.add(utteranceBox,c);
        utterancePanel.add(up,c);

        searchBox = new JTextField();
        searchBox.addActionListener(this);
        searchBox.getDocument().addDocumentListener(this);
        searchBox.addKeyListener(this);

        JButton sendMySelectedButton = new JButton("Annotate");
        sendMySelectedButton.setActionCommand("annotate");
        sendMySelectedButton.addActionListener(this);

        JPanel sendSystemAnswerButtonPane = new JPanel();
		sendSystemAnswerButtonPane.setLayout(new BoxLayout(sendSystemAnswerButtonPane,BoxLayout.X_AXIS));
        sendSystemAnswerButtonPane.add(searchBox);
		sendSystemAnswerButtonPane.add(sendMySelectedButton);
		sendSystemAnswerButtonPane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

        //Create the list and put it in a scroll pane.
		listModel = new DefaultListModel();
		list = new JList(listModel);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setSelectedIndex(0);
		list.setVisibleRowCount(10);
        list.setCellRenderer((ListCellRenderer) new AnswerCellRenderer());        
		JScrollPane listScrollPane = new JScrollPane(list);

        JPanel annotationPane = new JPanel();
		annotationPane.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        annotationPane.add(sendSystemAnswerButtonPane,c);
        c=new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1.0;
        c.weighty = 1.0;
		annotationPane.add(listScrollPane,c);


        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, utterancePanel, annotationPane);
        contentPane.add(splitPane);
	}

    class AnswerCellRenderer extends JLabel implements ListCellRenderer {
        public Component getListCellRendererComponent(
                JList list,
                Object value,            // value to display
                int index,               // cell index
                boolean isSelected,      // is the cell selected
                boolean cellHasFocus)    // the list and the cell have the focus
        {
            Object a=(Object)value;
            String s="";
            setText(extractTextFromUtterance(a));
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }
            setEnabled(list.isEnabled());
            setFont(list.getFont());
            setOpaque(true);
            return this;
        }
    }

	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowClosed(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowClosing(WindowEvent arg0) {
	}

	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void actionPerformed(ActionEvent e) {
		String cmd=e.getActionCommand();
        if (cmd.equals("annotate")) {
        	doAnnotation();
        } else if  (cmd.equals("next")) {
        	goNext();
        } else if  (cmd.equals("prev")) {
        	goPrev();
        }
    }

	private void doAnnotation() {
		int[] sis=list.getSelectedIndices();
		String label=null;
		if (sis!=null && sis.length==1) label=(String) listModel.get(sis[0]);
		else label=searchBox.getText();
		
		TrainingDataFormat td=toBeAnnotated.get(currentPosition);
		System.out.println("labelling '"+td.getUtterance()+"' with "+label+".");
		td.setLabel(label);
        goNext();
	}

	public void goNext() {
		currentPosition++;
		if (toBeAnnotated.size()<=currentPosition) currentPosition--;
		setUtteranceBox();
	}
	private void goPrev() {
		currentPosition--;
		if (-1>=currentPosition) currentPosition=0;
		setUtteranceBox();
	}

    public ArrayList<Integer> getPositionOfNewLinesInString(String s) {
        ArrayList<Integer> ret=new ArrayList<Integer>();
        if (s!=null) {
            int l=s.length();
            for(int i=0;i<l;i++) {
                if (s.charAt(i)=='\n') ret.add(i);
            }
            return ret;
        } else return ret;
    }

    public Set<Integer> findLinesWithMatch(List<Match> found,String entireString) {
        int l=entireString.length();
        Set<Integer> ret=new HashSet<Integer>();
        if (found!=null) {
        	for(AhoCorasick.Match m:found) {
        		int numNewLineFromStartToMatch=0;
        		for(Integer i:newLinesInLabelsString) {
        			if (m.pos<=i) break;
        			else numNewLineFromStartToMatch++;
        		}
        		ret.add(numNewLineFromStartToMatch);
        	}
        }
        return ret;
    }

    public static boolean isEmptyString(String s) {
        return ((s==null) || (cleanupSpaces(s).length()==0));
    }

    public static String removeLeadingSpaces(String in) {
        return in.replaceAll("^[\\s]+", "");
    }
    public static String removeTrailingSpaces(String in) {
        return in.replaceAll("[\\s]+$", "");
    }
    public static String removeMultiSpaces(String in) {
        return in.replaceAll("[\\s]{2,}", " ");
    }
    public static String removeLeadingAndTrailingSpaces(String in) {
        return removeLeadingSpaces(removeTrailingSpaces(in));
    }
    public static String convertAllBlanksToSpaces(String in) {
        return in.replaceAll("[\\s]", " ");
    }
    public static String cleanupSpaces(String in) {
        return removeLeadingAndTrailingSpaces(removeMultiSpaces(convertAllBlanksToSpaces(in)));
    }

    public void findAndUpdateList(Document searchBox) {
        try {
            String query=searchBox.getText(0, searchBox.getLength());
            if (!isEmptyString(query)) {
            	String[] queries=query.split("[\\s]+");
            	Set<Integer> intersectionLineMatches=null;
            	for(String q:queries) {
            		Set<Integer> lineMatches=doSearch(q);	
                    if (lineMatches!=null) {
                    	if (intersectionLineMatches==null) intersectionLineMatches=lineMatches;
                    	else if (!intersectionLineMatches.isEmpty()) {
                    		intersectionLineMatches.retainAll(lineMatches);
                    	}
                    }
            	}
                listModel.clear();
                if (intersectionLineMatches!=null) {
                	List<Integer>sortedList=new ArrayList<Integer>(intersectionLineMatches);
                	Collections.sort(sortedList);
                    for(Integer pos:sortedList) {
                        listModel.addElement(possibleLabels.get(pos));
                    }
                }
            } else {
                updateList(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	private Set<Integer> doSearch(String q) {
        AhoCorasick search = new AhoCorasick();
        search.addPattern(q);
        search.finalizePatternAdditions();
        LazyArray<MatchList> chart = search.findMatchesInString(possibleLabelsString);
		List<Match> found = search.getOptimalMatchSequence(chart, 0, new HashMap<Integer, List<Match>>());
        Set<Integer> lineMatches=findLinesWithMatch(found,possibleLabelsString);
        return lineMatches;
	}

	public void changedUpdate(DocumentEvent e) {
		Document doc = e.getDocument();
        findAndUpdateList(doc);
	}
	public void insertUpdate(DocumentEvent e) {
		Document doc = e.getDocument();
        findAndUpdateList(doc);
	}
	public void removeUpdate(DocumentEvent e) {
		Document doc = e.getDocument();
        findAndUpdateList(doc);
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void keyPressed(KeyEvent e) {
		int key = e.getKeyCode();
		int mod=e.getModifiers();
		if (key == KeyEvent.VK_ENTER) {
			doAnnotation();
			e.consume();
		} else if (key == KeyEvent.VK_UP) {
			if ((mod & KeyEvent.SHIFT_MASK)!=0) {
				goPrev();
			} else {
				int is=list.getSelectedIndex();
				if (is>0) list.setSelectedIndex(is-1);  				
			}
			e.consume();
		} else if (key == KeyEvent.VK_DOWN) {
			if ((mod & KeyEvent.SHIFT_MASK)!=0) {
				goNext();
			} else {
				int is=list.getSelectedIndex();
				int ls=list.getModel().getSize();
				if (is<(ls-1)) list.setSelectedIndex(is+1);  				
			}
			e.consume();
		} else if (key == KeyEvent.VK_PAGE_UP) {
			if ((mod & KeyEvent.SHIFT_MASK)!=0) {
				for(int i=0;i<100;i++)goPrev();
			}
			e.consume();
		} else if (key == KeyEvent.VK_PAGE_DOWN) {
			if ((mod & KeyEvent.SHIFT_MASK)!=0) {
				for(int i=0;i<100;i++)goNext();
			}
			e.consume();
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// TODO Auto-generated method stub
		
	}
	private static File labelFile=null,annotateFile=null,outputFile=null;
	private static int labelColumn=1,annotationUtteranceColumn,annotationLabelColumn;
	private static final String ANNOTATE="a",ANNOTATELC="alc",ANNOTATEUC="auc", LABELS="l",LABELSCOLUMN="lc",HELP_OPTION="h",OUTFILE="o";
	private static final Options options = new Options();
	static {
		Option output=OptionBuilder.withArgName("output annotated file").withDescription("specifies the file that will be saved with the annotated data.").hasArg().create(OUTFILE);
		Option annotate=OptionBuilder.withArgName("file to annotate").withDescription("specifies the file that contains the data to annotate.").hasArg().create(ANNOTATE);
		Option labels=OptionBuilder.withArgName("file containing the labels").withDescription("specifies the file to use to load the annotation labels.").hasArg().create(LABELS);
		Option labelcolumn=OptionBuilder.withArgName("column").withDescription("column to use to read the labels file.").hasArg().create(LABELSCOLUMN);
		Option annotateuttcolumn=OptionBuilder.withArgName("column").withDescription("column to use to read the utterances in the file to annotate.").hasArg().create(ANNOTATEUC);
		Option annotatelblcolumn=OptionBuilder.withArgName("column").withDescription("column to use to read the labels in the file to annotate.").hasArg().create(ANNOTATELC);
		output.setRequired(true);
		annotate.setRequired(true);
		labels.setRequired(true);
		labelcolumn.setRequired(true);
		annotateuttcolumn.setRequired(true);
		annotatelblcolumn.setRequired(true);
		options.addOption(output);
		options.addOption(annotate);
		options.addOption(labels);
		options.addOption(labelcolumn);
		options.addOption(annotateuttcolumn);
		options.addOption(annotatelblcolumn);
		options.addOption(HELP_OPTION, false, "Request this help message to be printed.");
	}
	private static void printUsageHelp() {
		HelpFormatter f = new HelpFormatter();
		f.printHelp("[OPTIONS]", options);
	}
	private static void digestCommandLineArguments(String[] args) throws Exception {
		CommandLineParser parser = new PosixParser();
		try {
			CommandLine cmd = parser.parse( options, args);
			if ( cmd.hasOption('h') ) {
				printUsageHelp();
			} else {
				if (cmd.hasOption(LABELS)) {
					labelFile=new File(cmd.getOptionValue(LABELS));
				}
				if (cmd.hasOption(LABELSCOLUMN)) {
					labelColumn=Integer.parseInt(cmd.getOptionValue(LABELSCOLUMN));
				}
				if (cmd.hasOption(ANNOTATEUC)) {
					annotationUtteranceColumn=Integer.parseInt(cmd.getOptionValue(ANNOTATEUC));
				}
				if (cmd.hasOption(ANNOTATELC)) {
					annotationLabelColumn=Integer.parseInt(cmd.getOptionValue(ANNOTATELC));
				}
				if (cmd.hasOption(LABELSCOLUMN)) {
					labelColumn=Integer.parseInt(cmd.getOptionValue(LABELSCOLUMN));
				}
				if (cmd.hasOption(ANNOTATE)) {
					annotateFile=new File(cmd.getOptionValue(ANNOTATE));
				}
				if (cmd.hasOption(OUTFILE)) {
					outputFile=new File(cmd.getOptionValue(OUTFILE));
				}
			}
		} catch (ParseException e) {
			printUsageHelp();
			throw e;
		}
	}
	 
	public static List<TrainingDataFormat> integratePartialAnnotationWithNewTrainingData(File partialAnnotation,File newTraining) throws Exception {
		List<TrainingDataFormat> tds = BuildTrainingData.extractTrainingDataFromExcelMaintainingOrder(partialAnnotation,0,annotationLabelColumn,annotationUtteranceColumn,true);
		List<TrainingDataFormat> newtds = BuildTrainingData.extractTrainingDataFromExcel(newTraining,0,annotationLabelColumn,annotationUtteranceColumn,true,false);
		Map<String, List<String>> uls = BuildTrainingData.getAllUtterancesWithLabels(tds);
		if (newtds!=null && uls!=null) {
			for (TrainingDataFormat td:newtds) {
				String u=td.getUtterance();
				List<String> nuls=uls.get(u);
				if (nuls!=null && !nuls.isEmpty()) {
					Set<String> ls=new HashSet<String>(nuls);
					if (ls.size()>1) throw new Exception("Utterance assigned to multiple labels("+ls+"): '"+u+"'");
					else {
						td.setLabel(ls.iterator().next());
						td.setBackupLabels(null);
					}
				} else {
					td.setLabel(null);
					td.setBackupLabels(null);
				}
			}
		}
		return newtds;
	}
	/**
	 * the output is a pair of two lists,
	 * the first list contains the integration of the newdata and old annotated data that has a label based on the annotated data
	 * the second is the list of new data for which there is no annotation (last arg true) or that was not present in the annotated data (always) 
	 * @param partialAnnotation
	 * @param newTraining
	 * @return
	 * @throws Exception
	 */
	public static Pair<List<TrainingDataFormat>,List<TrainingDataFormat>> integrateAndRemovePartialAnnotationWithNewTrainingData(File partialAnnotation,int label,int utterance,File newTraining,int label2,int utterance2,boolean considerBlankLabelsNotAnnotated) throws Exception {
		List<TrainingDataFormat> tds = BuildTrainingData.extractTrainingDataFromExcelMaintainingOrder(partialAnnotation,0,label,utterance,true);
		List<TrainingDataFormat> newtds = BuildTrainingData.extractTrainingDataFromExcel(newTraining,0,label2,utterance2,true,false);
		Map<String, List<String>> uls = BuildTrainingData.getAllUtterancesWithLabels(tds);
		List<TrainingDataFormat> done=null,toBeDone=null;
		if (newtds!=null && uls!=null) {
			for (TrainingDataFormat td:newtds) {
				String u=td.getUtterance();
				boolean emptyAnnotation=true;
				List<String> nuls=uls.get(u);
				Set<String> ls=null;
				String l=null;
				if (nuls!=null && !nuls.isEmpty()) {
					ls=new HashSet<String>(nuls);
					if (ls.size()>1) throw new Exception("Utterance assigned to multiple labels("+ls+"): '"+u+"'");
					else {
						if (StringUtils.isEmptyString(l=ls.iterator().next())) {
							emptyAnnotation=true;
							l=null;
						} else {
							emptyAnnotation=false;
						}
					}
				}
				TrainingDataFormat newtd=new TrainingDataFormat(td);
				if (ls!=null && (!considerBlankLabelsNotAnnotated || !emptyAnnotation)) {
					if (done==null) done=new ArrayList<TrainingDataFormat>();
					newtd.setLabel(l);
					newtd.setBackupLabels(null);
					done.add(newtd);
				} else {
					if (toBeDone==null) toBeDone=new ArrayList<TrainingDataFormat>();
					newtd.setLabel(l);
					newtd.setBackupLabels(null);
					toBeDone.add(newtd);
				}
			}
		}
		return new Pair<List<TrainingDataFormat>, List<TrainingDataFormat>>(done, toBeDone);
	}

	public static void main(String[] args) throws Exception {
		/*
		Pair<List<TrainingDataFormat>, List<TrainingDataFormat>> done_tobedone = integrateAndRemovePartialAnnotationWithNewTrainingData(new File("annotation/output-expor.xlsx"), 0,1,new File("annotation/export-all.xlsx"),0,1,false);
		BuildTrainingData.dumpTrainingDataToExcel(done_tobedone.getFirst(), new File("annotation/export-all-done.xlsx"), "test", null, 0, 1, true);
		BuildTrainingData.dumpTrainingDataToExcel(done_tobedone.getSecond(), new File("annotation/export-all-NOT-done.xlsx"), "test", null, 0, 1, true);
		System.exit(1);
		*/
		/*
		List<TrainingDataFormat> tds = BuildTrainingData.extractTrainingDataFromExcel(new File("annotation/complete_all_spk1_lines.xlsx"),0,1,0,true);
		Map<String, List<TrainingDataFormat>> ssa = BuildTrainingData.getAllSpeechActsWithTrainingData(tds);
		List<TrainingDataFormat> tmp=new ArrayList<TrainingDataFormat>();
		for(String k:ssa.keySet()) {
			String sk=StringUtils.cleanupSpaces(k.toLowerCase());
			if (sk.equals("simple question") || sk.equals("s")) {
				List<TrainingDataFormat> sa = ssa.get(k);
				tmp.addAll(sa);
			} else {
				System.out.println(k);
			}
		}
		BuildTrainingData.dumpTrainingDataToExcel(tmp, new File("annotation/input.xlsx"), "test");
		System.exit(1);
		*/
		digestCommandLineArguments(args);
		List<TrainingDataFormat> tds = BuildTrainingData.extractTrainingDataFromExcelMaintainingOrder(annotateFile,0,annotationLabelColumn,annotationUtteranceColumn,true);
		Set<String> labels = ExcelUtils.extractUniqueValuesInThisColumn(labelFile.getAbsolutePath(), 0, labelColumn);
		labels.add("extra.offtopic");
		labels.add("extra.edit");
		System.out.println("start");
		new AnnotationTool(labels, tds);
		System.out.println("done");
		BuildTrainingData.dumpTrainingDataToExcel(tds, outputFile, "annotated data",new String[]{"0","1"},annotationLabelColumn,annotationUtteranceColumn,true);
	}
}
