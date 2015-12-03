package edu.usc.ict.nl.ui.chat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.JToggleButton.ToggleButtonModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.PlainDocument;

import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.util.FileUtils;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;

public class Feedback {
	
	private JEditorPane editorPane;
	private Set<String> feedbackQuestions=null;

	public Feedback(NLBusConfig config) throws Exception {
		String feedbackInput=config.getFeedbackInputform();
		if (!StringUtils.isEmptyString(feedbackInput)) {
			File feedbackFile=new File(config.getContentRoot()+File.separator+feedbackInput);
			if (feedbackFile.exists()) {
				String chatLog=config.getChatLog();
				if (StringUtils.isEmptyString(chatLog)) throw new Exception("Configured feedback form but no chat log saved. Set the chat log property.");
				feedbackQuestions=getFeedbackQuestionsInForm(feedbackFile);
				java.net.URL helpURL = feedbackFile.toURI().toURL();
				editorPane=new JEditorPane();
				editorPane.setPage(helpURL);
				editorPane.setEditable(false);
			} else {
				throw new Exception("Configured feedback form but not found: "+feedbackFile.getAbsolutePath());
			}
		}
	}

	public boolean hasFeedback() {return (editorPane!=null) && (feedbackQuestions!=null);}

	private static final Pattern questionPattern=Pattern.compile("[\\s]+name[\\s]*=[\\s]*\"[\\s]*(q[1-9]+)[\\s]*\"");
	private Set<String> getFeedbackQuestionsInForm(File feedbackFile) throws IOException {
		if (feedbackFile!=null) {
			BufferedReader in=new BufferedReader(new FileReader(feedbackFile));
			StringBuffer content=new StringBuffer();
			String line;
			while((line=in.readLine())!=null) {
				content.append(line);
			}
			in.close();
			String all=content.substring(0);
			int start=0,length=all.length();
			Matcher m=questionPattern.matcher(all);
			Set<String> ret=new HashSet<String>();
			while(start>=0 && start<length) {
				if (m.find(start) && m.groupCount()==1) {
					start=m.end();
					String q=m.group(1);
					ret.add(q);
				} else break;
			}
			return ret;
		}
		return null;
	}
	
	public LinkedHashMap<String,Pair<String,String>> getFormValues() {
		Element root=editorPane.getDocument().getDefaultRootElement();
		return getFormValues(root, new LinkedHashMap<String, Pair<String,String>>());
	}
	public LinkedHashMap<String,Pair<String,String>> getFormValues(Element root,LinkedHashMap<String,Pair<String,String>> ret) {
		LinkedList<Element> els=new LinkedList<Element>();
		HashMap<String,String> questions=new HashMap<String,String>();
		els.add(root);
		while(!els.isEmpty()) {
			Element n=els.poll();
			int count=n.getElementCount();
			String name=n.getName();
			AttributeSet attrs = n.getAttributes();
			String id=getID(attrs);
			if (name.equalsIgnoreCase("p") && id!=null && id.matches("q[0-9]+")) {
				String q=getTextCorrespondentToElement(n);
				q=StringUtils.cleanupSpaces(q);
				questions.put(id, q);
			} else if (name.equalsIgnoreCase("input")) {
				ToggleButtonModel model=(ToggleButtonModel) getModel(attrs,ToggleButtonModel.class);
				if (model.isSelected()) {
					String group=getName("input",attrs);
					String value=getRabioButtonValue(attrs);
					ret.put(group, new Pair<String, String>(value, questions.get(group)));
				}
			} else if (name.equalsIgnoreCase("textarea")) {
				String group=getName("textarea",attrs);
				PlainDocument d=(PlainDocument)getModel(attrs, PlainDocument.class);
				if (d!=null) {
					String value;
					try {
						value = d.getText(0, d.getLength());
						ret.put(group, new Pair<String, String>(value, questions.get(group)));
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}
			} else {
				for(int i=0;i<count;i++) {
					Element c = n.getElement(i);
					els.addLast(c);
				}
			}
		}
		return ret;
	}

	private String getTextCorrespondentToElement(Element n) {
		try {
			int start=n.getStartOffset();
			int length=n.getEndOffset()-start;
			Document d=n.getDocument();
			return d.getText(start,length);
		} catch (BadLocationException e1) {}
		return null;
	}
	private String getID(AttributeSet attrs) {
		Enumeration<?> names = attrs.getAttributeNames();
		while(names.hasMoreElements()) {
			Object el = names.nextElement();
			if (el.toString().equals("id")) {
				Object id = (Object)attrs.getAttribute(el);
				return (id!=null)?id.toString():null;
			}
		}
		return null;
	}
	
	private String getRabioButtonValue(AttributeSet attrs) {
		Enumeration<?> names = attrs.getAttributeNames();
		while(names.hasMoreElements()) {
			Object el = names.nextElement();
			if (el.toString().equalsIgnoreCase("value")) {
				Object value = attrs.getAttribute(el);
				String vs=value.toString();
				return vs;
			}
		}
		return null;
	}

	private String getName(String elementName,AttributeSet attrs) {
		Enumeration<?> names = attrs.getAttributeNames();
		while(names.hasMoreElements()) {
			Object el = names.nextElement();
			if (el.toString().equalsIgnoreCase("name")) {
				Object value = attrs.getAttribute(el);
				String vs=value.toString();
				if(!vs.equalsIgnoreCase(elementName)) return vs;
			}
		}
		return null;
	}

	private Object getModel(AttributeSet attrs,Class modelClass) {
		Enumeration<?> names = attrs.getAttributeNames();
		while(names.hasMoreElements()) {
			Object el = names.nextElement();
			if (el.toString().equals("model"));
			Object model = (Object)attrs.getAttribute(el);
			if (modelClass.isInstance(model)) {
				return model;
			}
		}
		return null;
	}

	public Set<String> getFeedbackQuestions() { return feedbackQuestions;}
	public JEditorPane getEditorPane() {return editorPane;}

	public void saveFeedback(File currentChatLogFile,Map<String, Pair<String, String>> results) throws IOException {
		if (currentChatLogFile!=null && hasFeedback()) {
			File ff=new File(currentChatLogFile.getAbsolutePath()+".feedback");
			BufferedWriter out=new BufferedWriter(new FileWriter(ff));
			if (results!=null) {
				for(String key:results.keySet()) {
					Pair<String, String> valueAndTitle=results.get(key);
					String q=valueAndTitle.getSecond();
					if (!StringUtils.isEmptyString(q)) out.write(key+"='"+q+"'\n");
					else out.write(key+"\n");
					out.write(valueAndTitle.getFirst()+"\n");
					out.flush();
				}
			}
			out.close();
		}
	}
	
	public boolean isCompleted() {
		if (hasFeedback()) {
			LinkedHashMap<String, Pair<String, String>> results = getFormValues();
			Set<String> needed = getFeedbackQuestions();
			if (results!=null && needed!=null) {
				return results.keySet().containsAll(needed);
			}
		}
		return false;
	}

	//q1='Bill understood what I said to him:'
	private static final Pattern fileQPattern=Pattern.compile("^[\\s]*(q[0-9]+)[\\s]*=[\\s]*'(.*)'[\\s]*$");
	public static Map<String, Pair<String, String>> getResultsFromSavedFile(String filename) throws IOException {
		Map<String, Pair<String, String>> ret=null;
		File file=new File(filename);
		if (file.exists()) {
			BufferedReader in=new BufferedReader(new FileReader(file));
			String line,qID=null,qText=null,qResponse=null;
			while((line=in.readLine())!=null) {
				Matcher m=fileQPattern.matcher(line);
				if (m.matches() && (m.groupCount()==2)) {
					if (!StringUtils.isEmptyString(qID) && !StringUtils.isEmptyString(qText)) {
						if (ret==null) ret=new HashMap<String, Pair<String,String>>();
						ret.put(qID,new Pair<String, String>(qResponse, qText));
					}
					qResponse=null;
					qID=m.group(1);
					qText=m.group(2);
				} else {
					if (!StringUtils.isEmptyString(qID) && !StringUtils.isEmptyString(qText)) {
						qResponse=(qResponse!=null)?qResponse+line:line;
					} else {
						qID=null;
						qText=null;
					}
				}
			}
			// write last question
			if (!StringUtils.isEmptyString(qID) && !StringUtils.isEmptyString(qText)) {
				if (ret==null) ret=new HashMap<String, Pair<String,String>>();
				ret.put(qID,new Pair<String, String>(qResponse, qText));
			}
		}
		return ret;
	}
	public static Map<String,Pair<String,Map<String,Integer>>> aggregateResponses(Map<String,Pair<String,Map<String,Integer>>> tot,Map<String, Pair<String, String>> single) {
		if (single!=null) {
			if (tot==null) tot=new HashMap<String, Pair<String,Map<String,Integer>>>();
			for(String qID:single.keySet()) {
				Pair<String,String> responseAndText=single.get(qID);
				String response=responseAndText.getFirst();
				if (!StringUtils.isEmptyString(response)) {
					Pair<String, Map<String, Integer>> qTextAndMap = tot.get(qID);
					if (qTextAndMap==null) tot.put(qID, qTextAndMap=new Pair<String,Map<String, Integer>>(responseAndText.getSecond(),new HashMap<String, Integer>()));
					Map<String, Integer> qMap=qTextAndMap.getSecond();
					Integer count = qMap.get(response);
					if (count==null) qMap.put(response, 1);
					else qMap.put(response, ++count);
				}
			}
		}
		return tot;
	}
	public static Map<String, Pair<String, Map<String, Integer>>> aggreragateAllFeedbackResponsesInDirectory(Map<String, Pair<String, Map<String, Integer>>> tot,String directory) {
		List<File> files = FileUtils.getAllFiles(new File(directory), ".*\\.feedback");
		if (files!=null) {
			for(File file:files) {
				try {
					Map<String, Pair<String, String>> results = getResultsFromSavedFile(file.getAbsolutePath());
					tot=aggregateResponses(tot, results);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return tot;
	}
	
	public static void main(String[] args) throws Exception {
		Map<String, Pair<String, Map<String, Integer>>> tot = aggreragateAllFeedbackResponsesInDirectory(null,"../../../support/dialogue_testbed/logs/user-testing-2011-08-02/");
		aggreragateAllFeedbackResponsesInDirectory(tot,"../../../support/dialogue_testbed/logs/user-testing-2011-07-26/");
		System.out.println(tot);
	}
}
