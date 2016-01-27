package edu.usc.ict.nl.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.bus.modules.NLUInterface;
import edu.usc.ict.nl.nlu.ChartNLUOutput;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.chart.PartialClassification;
import edu.usc.ict.nl.nlu.io.BuildTrainingData;
import edu.usc.ict.nl.nlu.preprocessing.TokenizerI;
import edu.usc.ict.nl.nlu.preprocessing.tokenizer.Tokenizer;
import edu.usc.ict.nl.util.FileUtils;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.Triple;
import edu.usc.ict.nl.util.XMLUtils;

public class TargetDialogEntry {

	public final static Pattern numberPattern=Pattern.compile("([+-]?(([1-9][0-9]*(\\.[0-9])?)|(\\.[0-9])|(0?\\.[0-9]))[0-9]*)");
	public final static Pattern typeAndTextLinePattern=Pattern.compile("^[\\s]*("+numberPattern.pattern()+"[\\s]+)?(simcoach|user):[\\s]*(.+)[\\s]*$");
	public final static Pattern speechActPattern=Pattern.compile("^[\\s]*~[\\s]*(\\[([0-9]+),([0-9]+)\\])?[\\s]*(([\\d\\.]+[eE\\+\\-\\d]*)*)[\\s]*(.*)[\\s]*$");

	public enum Type {USER,SIMCOACH};
	Type type;
	String text;
	private ArrayList<NLUOutput> speechActs;
	private Float deltaT=null;

	public Float getDeltaT() {return deltaT;}
	public void setDeltaT(Float dt) {this.deltaT=dt;}
	public Type getType() {return type;}
	public String getText() {return text;}
	private void setType(Type t) { type=t;}
	private void setText(String t) {text=t;}
	public void setSpeechActs(ArrayList<NLUOutput> speechActs) {this.speechActs = speechActs;}
	public ArrayList<NLUOutput> getSpeechActs() {return speechActs;}
	public List<NLUOutput> getSpeechActsAndSetPayload(NLUInterface nlu) throws Exception {
		ArrayList<NLUOutput> sas = getSpeechActs();
		if (sas!=null) {
			for(NLUOutput sa:sas) {
				sa.setPayload(nlu.getPayload(sa.getId(), sa.getText()));
			}
		}
		return sas;
	}
	
	public boolean isUserAction() {return getType()==Type.USER;}
	public boolean isSystemAction() {return getType()==Type.SIMCOACH;}
	
	@Override
	public String toString() {
		String ret="";
		Float delta = getDeltaT();
		ret+=((delta!=null)?delta+" ":"")+type.toString().toLowerCase()+": "+text+"\n";
		if (getSpeechActs()!=null) {
			for (NLUOutput sa:getSpeechActs()) {
				if (sa instanceof ChartNLUOutput) {
					List<Triple<Integer, Integer, NLUOutput>> portions = ((ChartNLUOutput) sa).getPortions();
					if (portions!=null) {
						if (portions.size()==1) {
							ret+="~ "+sa.getId()+"\n";
						} else {
							for(Triple<Integer, Integer, NLUOutput> p:portions) {
								NLUOutput nlu=p.getThird();
								int start=p.getFirst();
								int end=p.getSecond();
								ret+="~ ["+start+","+end+"] "+nlu.getId()+"\n";
							}
						}
					}
				} else {
					ret+="~ "+sa.getId()+"\n";
				}
			}
		}
		return ret;
	}

	public boolean match(DMSpeakEvent sae) {
		if (sae!=null) {
			String sa=sae.getName();
			ArrayList<NLUOutput> sas = getSpeechActs();
			if (sas!=null) {
				for(NLUOutput sat:sas) {
					if (sat instanceof ChartNLUOutput) {
						List<Triple<Integer, Integer, NLUOutput>> portions = ((ChartNLUOutput) sat).getPortions();
						if (portions!=null) {
							for(Triple<Integer, Integer, NLUOutput> p:portions) {
								NLUOutput nlu=p.getThird();
								if (nlu.getId().equals(sa)) return true;
							}
						}
					} else {
						if (sat.getId().equals(sa)) return true;
					}
				}
			}
			return false;
		} else return false;
	}

	public static TargetDialogEntry parseTDE(String text,String lowConfidenceEventName) throws Exception {
		TokenizerI tokenizer=new Tokenizer();
		TargetDialogEntry tde=new TargetDialogEntry();
		String[] lines=text.split("\\n");
		boolean firstLine=true;
		boolean isChartNlu=false;
		ChartNLUOutput chartNlu=null;
		for (String line:lines) {
			Matcher m=(firstLine)?typeAndTextLinePattern.matcher(line):speechActPattern.matcher(line);
			if (firstLine && m.matches() && (m.groupCount()==9)) {
				isChartNlu=false;
				chartNlu=null;
				String type=m.group(8);
				Type typeValue = Type.valueOf(type.toUpperCase());
				tde.setType(typeValue);
				String entryText=m.group(9);
				tde.setText(entryText);
				try {
					Float dt=Float.parseFloat(m.group(1));
					tde.setDeltaT(dt);
				} catch (Exception e) {}
				firstLine=false;
			} else if (m.matches() && m.groupCount()==6) {
				ArrayList<NLUOutput> sas = tde.getSpeechActs();
				if (sas==null) {
					sas=new ArrayList<NLUOutput>();
					tde.setSpeechActs(sas);
				}
				String speechActText=m.group(6);
				if (StringUtils.isEmptyString(speechActText)) throw new Exception("Empty speech act for target dialog entry: '"+speechActText+"'");
				Float p=1f;
				try {
					p=Float.parseFloat(m.group(5));
				} catch (Exception e) {}
				Integer start=null;
				try {
					start=Integer.parseInt(m.group(2));
				} catch (Exception e) {}
				Integer end=null;
				try {
					end=Integer.parseInt(m.group(3));
				} catch (Exception e) {}
				if (start!=null && end!=null) isChartNlu=true;
				if (isChartNlu && chartNlu==null) {
					chartNlu=new ChartNLUOutput(tde.getText(), new ArrayList<PartialClassification>());
					sas.add(chartNlu);
				}
				if (isChartNlu) {
					List<Token> tokens = tokenizer.tokenize1(tde.getText());
					List<Token> subTokens = tokens.subList(start, end);
					String subText=tokenizer.untokenize(subTokens,null);
					chartNlu.addPortion(start, end, new NLUOutput(subText, speechActText, p, null));
				} else {
					if (chartNlu!=null) throw new Exception("mix between normal and chart nlu.");
					NLUOutput speechAct=new NLUOutput(tde.getText(), speechActText, p, null);
					sas.add(speechAct);
				}
			} else if (!firstLine) {
				tde.setText(tde.getText()+line);
			} else {
				throw new Exception("Illegal target dialog entry: '"+text+"'");
			}
		}
		if ((tde!=null) && (tde.getSpeechActs()==null)) {
			ArrayList<NLUOutput> sas = new ArrayList<NLUOutput>();
			tde.setSpeechActs(sas);
			String speechActText=lowConfidenceEventName;
			NLUOutput speechAct=new NLUOutput(tde.getText(), speechActText, 1, null);
			sas.add(speechAct);
		}
		return tde;
	}

	public static List<TargetDialogEntry> readTargetDialog(String inputFile,String lowConfidenceEventName) throws Exception {
		BufferedReader inp=new BufferedReader(new FileReader(inputFile));
		return readTargetDialog(inp,lowConfidenceEventName);
	}
	public static ArrayList<TargetDialogEntry> readTargetDialog(BufferedReader inp,String lowConfidenceEventName) throws Exception {
		String line;
		ArrayList<TargetDialogEntry> targetDialog=new ArrayList<TargetDialogEntry>();
		String dialogEntryString="";
		while ((line=inp.readLine())!=null) {
			if (!StringUtils.isEmptyString(line)) {
				dialogEntryString+=line+"\n";
			} else if (!StringUtils.isEmptyString(dialogEntryString)) {
				targetDialog.add(TargetDialogEntry.parseTDE(dialogEntryString,lowConfidenceEventName));
				dialogEntryString="";
			}
		}
		if (!StringUtils.isEmptyString(dialogEntryString)) {
			targetDialog.add(TargetDialogEntry.parseTDE(dialogEntryString,lowConfidenceEventName));
		}
		inp.close();
		return targetDialog;
	}
	private static final Pattern chartNLUpattern=Pattern.compile("^[\\s]*([0-9]+)/([0-9]+)[\\s]*\\[([0-9]+),([0-9]+)\\][\\s]*(.+)[\\s]*$");
	private static final Pattern chartIDpattern=Pattern.compile("^[\\s]*chart[\\s]+([0-9]+)[\\s]*$");
	//chart nlu: 1/1 [0,1] conventional-opening.generic
	//normal nlu: text string
	public static ArrayList<TargetDialogEntry> readTargetDialogFromXMLDMLog(String inputFile,NLU nlu) throws Exception {
		ArrayList<TargetDialogEntry> targetDialog=new ArrayList<TargetDialogEntry>();
		Document doc = XMLUtils.parseXMLFile(inputFile, false, false);
		Node rootNode = doc.getDocumentElement();
		Queue<Node> q=new LinkedList<Node>();
		NodeList cs = rootNode.getChildNodes();
		for (int i = 0; i < cs.getLength(); i++) q.add(cs.item(i));
		TargetDialogEntry tde;
		boolean first=true;
		while(!q.isEmpty()) {
			Node c=q.poll();
			NamedNodeMap childAtt = c.getAttributes();
			if (isUserNode(c)) {
				if (!first) {
					Float delta=getDeltaNodeValue(childAtt);
					NLUOutput nluOutput = getNLUOutputNodeValue(c,nlu);
					tde = new TargetDialogEntry();
					if (delta!=null) tde.setDeltaT(delta);
					tde.setType(Type.USER);
					tde.setText(nluOutput.getText());
					ArrayList<NLUOutput> sas = new ArrayList<NLUOutput>();
					sas.add(nluOutput);
					tde.setSpeechActs(sas);
					targetDialog.add(tde);
				}
			} else if (isSystemNode(c)) {
				first=false;
				Float delta=getDeltaNodeValue(childAtt);
				String systemSpeechAct=getIDNodeValue(childAtt);
				String text=getTextchildOf(c);
				tde = new TargetDialogEntry();
				if (delta!=null) tde.setDeltaT(delta);
				tde.setType(Type.SIMCOACH);
				tde.setText(text);
				ArrayList<NLUOutput> sas = new ArrayList<NLUOutput>();
				sas.add(new NLUOutput(text, systemSpeechAct, 1, null));
				tde.setSpeechActs(sas);
				targetDialog.add(tde);
			}
		}
		return targetDialog;
	}
	private static boolean isTextNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals("text");
	}
	private static boolean isNLUNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals("nlu");
	}
	private static String getTextNodeValue(Node c) {
		switch (c.getNodeType()) {
		case Node.TEXT_NODE:
			return c.getNodeValue();
		default:
			NodeList children = c.getChildNodes();
			if (children != null) {
				for (int i=0; i<children.getLength(); i++) {
					return getTextNodeValue(children.item(i));
				}
			}
		}
		return null;
	}
	private static boolean isSystemNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals("system");
	}
	private static boolean isUserNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals("user");
	}
	private static Float getDeltaNodeValue(NamedNodeMap att) {
		Node node = att.getNamedItem("delta");
		if (node!=null) return Float.parseFloat(node.getNodeValue());
		else return null;
	}
	private static float getProbNodeValue(NamedNodeMap att) {
		Node node = att.getNamedItem("prob");
		if (node!=null) return Float.parseFloat(node.getNodeValue());
		else return -1;
	}
	private static String getIDNodeValue(NamedNodeMap att) {
		Node node = att.getNamedItem("id");
		if (node!=null) return node.getNodeValue();
		else return null;
	}
	private static NLUOutput getNLUOutputNodeValue(Node c,NLU nlu) throws Exception {
		NLUOutput ret=null;
		if (c!=null && isUserNode(c)) {
			String text=null;
			NodeList cs = c.getChildNodes();
			int numChildren=cs.getLength();
			int numPortions=-1;
			for(int i=0;i<numChildren;i++) {
				Node cc=cs.item(i);
				NamedNodeMap childAtt = cc.getAttributes();
				if (isNLUNode(cc)) {
					String id=getIDNodeValue(childAtt);
					float prob=getProbNodeValue(childAtt);
					if (!StringUtils.isEmptyString(id)) {
						Matcher m=chartIDpattern.matcher(id);
						if (m.matches() && m.groupCount()==1) {
							ret=new ChartNLUOutput(text, null);
							numPortions=Integer.parseInt(m.group(1));
							continue;
						}
						m=chartNLUpattern.matcher(id);
						if (m.matches() && m.groupCount()==5) {
							int start=Integer.parseInt(m.group(3)),end=Integer.parseInt(m.group(4));
							id=m.group(5);
							((ChartNLUOutput)ret).addPortion(start, end, new NLUOutput(text, id, prob, null));
						} else {
							Map<String, Object> payload = nlu.getPayload(id, text);
							ret=new NLUOutput(text, id, prob, payload);
						}
					}
				} else if (isTextNode(cc)) {
					text=getTextNodeValue(cc);
				}
			}
		}
		return ret;
	}
	private static String getTextchildOf(Node c) throws Exception {
		if (c!=null) {
			NodeList cs = c.getChildNodes();
			int numChildren=cs.getLength();
			for(int i=0;i<numChildren;i++) {
				Node cc=cs.item(i);
				if (isTextNode(cc)) {
					return getTextNodeValue(cc);
				}
			}
		}
		return null;
	}

	public static String dumpTargetDialogToString(ArrayList<TargetDialogEntry> tdes) {
		if (tdes!=null) {
			String tot="";
			for(TargetDialogEntry tde:tdes) {
				tot+=tde.toString()+"\n";
			}
			return tot;
		}
		return null;
	}
	
	public static void extractTargetDialogFromXMLLogAndDumpIn(String log,NLU nlu,String dump) throws Exception {
		ArrayList<TargetDialogEntry> r = readTargetDialogFromXMLDMLog(log,nlu);
		String tot=dumpTargetDialogToString(r);
		FileUtils.dumpToFile(tot, dump);
	}
	
	public static void main(String[] args) throws Exception {
		NLU nlu=NLU.init("multiNLUSample");
		/*
		 	extractTargetDialogFromXMLLogAndDumpIn("C:\\Users\\morbini\\simcoach_svn\\trunk\\simcoach-runtime\\dialogManager\\logs\\chat-log-kineo-morbini-[2012_06_20]-[17_32_31]-sid=999-pid=.xml",
				nlu,
				"C:\\Users\\morbini\\simcoach_svn\\trunk\\simcoach-runtime\\dialogManager\\resources\\characters\\Bill_Ford_PB\\dm\\target dialogues\\annotated-target-dialogue-simcoach-4.txt");
				*/
		ArrayList<TargetDialogEntry> r = readTargetDialogFromXMLDMLog("logs/chat-log-kineo-morbini-[2012_07_31]-[14_31_46]-sid=999-pid=.xml",nlu);
		String tot=dumpTargetDialogToString(r);
		BufferedReader inp = new BufferedReader(new StringReader(tot));
		r=readTargetDialog(inp,nlu.getConfiguration().getLowConfidenceEvent());
		System.out.println(tot);
		/*Matcher m=TargetDialogEntry.numberPattern.matcher("12");
		System.out.println(m.matches());
		Matcher m1=TargetDialogEntry.speechActPattern.matcher("~ [6,12] 0.4 answer.observable.avoid-people");
		System.out.println(m1.matches()+" "+m1.groupCount()+" "+m1.group(2)+" "+m1.group(3));
		Matcher m2=TargetDialogEntry.speechActPattern.matcher("~ 0.4 answer.observable.avoid-people");
		System.out.println(m2.matches()+" "+m2.groupCount()+" "+m2.group(5)+" "+m2.group(6));
		Matcher m3=TargetDialogEntry.typeAndTextLinePattern.matcher("+208.4 simcoach: So you think it's just a small thing, that you're having these symptoms, or you think it's something more than that?");
		System.out.println(m3.matches()+" "+m3.groupCount()+" "+m3.group(1)+" "+m3.group(8)+" "+m3.group(9));
		Matcher m4=TargetDialogEntry.typeAndTextLinePattern.matcher("simcoach: So you think it's just a small thing, that you're having these symptoms, or you think it's something more than that?");
		System.out.println(m4.matches()+" "+m4.groupCount()+" "+m4.group(1)+" "+m4.group(8)+" "+m4.group(9));*/
	}
}
