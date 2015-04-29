package edu.usc.ict.nl.dm.visualizer;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.usc.ict.nl.dm.reward.model.XMLConstants;
import edu.usc.ict.nl.dm.visualizer.kbDisplay.VarDisplay;
import edu.usc.ict.nl.dm.visualizer.kbDisplay.Variable;
import edu.usc.ict.nl.util.FileUtils;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.XMLUtils;

public class VisualizerConfig {

	private Boolean dhEnabled=false,isEnabled=false;
	private LinkedHashMap<String,Variable> trackedVariables=null;
	private Map<String,Node> displayers=new HashMap<String, Node>();
	
	public VisualizerConfig(String configFile) throws Exception {
		parseConfig(configFile);
	}
	
	public void parseConfig(String fileName) throws Exception {
		File f=FileUtils.getFileFromStringInResources(fileName);
		Document doc = XMLUtils.parseXMLFile(f, true, true);
		Node rootNode = doc.getDocumentElement();
		Queue<Node> q=new LinkedList<Node>();
		NodeList cs = rootNode.getChildNodes();
		for (int i = 0; i < cs.getLength(); i++) q.add(cs.item(i));
		while(!q.isEmpty()) {
			Node c=q.poll();
			NamedNodeMap childAtt = c.getAttributes();
			if (isInformationStateConfig(c)) {
				parseInformationStateConfig(c);
				isEnabled=getIsVisible(childAtt);
			} else if (isDisplay(c)) {
				addDisplayer(c,childAtt);
			} else if (isDialogueHistoryConfig(c)) {
				dhEnabled=getIsVisible(childAtt);
			}
		}
	}
	
	public Boolean getDHEnabled() {
		return dhEnabled;
	}
	public Boolean getISEnabled() {
		return isEnabled;
	}

	private void addDisplayer(Node node, NamedNodeMap att) throws Exception {
		String displayerName=getName(att);
		displayers.put(displayerName, node);
	}
	
	public static VarDisplay buildDisplayer(Node node, NamedNodeMap att) throws Exception {
		String className=getClass(att);
		
		Class c=Class.forName(className);
		Constructor constructor = c.getConstructor();
		VarDisplay displayer=(VarDisplay) constructor.newInstance();
		
		NodeList cs = node.getChildNodes();
		for (int i = 0; i < cs.getLength(); i++) {
			Node cn=cs.item(i);
			if (isDisplayProperty(cn)) {
				NamedNodeMap childAtt = cn.getAttributes();
				String value=getValue(childAtt);
				String setterMethodName="set"+StringUtils.capitalize(getName(childAtt));
				Method m=displayer.getClass().getMethod(setterMethodName, String.class);
				m.invoke(displayer, value);
			}
		}

		return displayer;
	}

	private void parseInformationStateConfig(Node rootNode) throws Exception {
		Queue<Node> q=new LinkedList<Node>();
		NodeList cs = rootNode.getChildNodes();
		for (int i = 0; i < cs.getLength(); i++) q.add(cs.item(i));
		while(!q.isEmpty()) {
			Node c=q.poll();
			NamedNodeMap childAtt = c.getAttributes();
			if (isVariableConfig(c)) {
				addVariableToConfig(childAtt);
			}
		}
	}

	private void addVariableToConfig(NamedNodeMap childAtt) throws Exception {
		Variable v=new Variable(childAtt,displayers);
		if (v.isValid()) {
			if (trackedVariables==null) trackedVariables=new LinkedHashMap<String,Variable>();
			trackedVariables.put(v.getInternalName(),v);
		}
	}

	public static boolean isDialogueHistoryConfig(Node rootNode) {
		return (rootNode.getNodeType()==Node.ELEMENT_NODE) && rootNode.getNodeName().equals(XMLConstants.DHCONFIG);
	}
	public static boolean isInformationStateConfig(Node rootNode) {
		return (rootNode.getNodeType()==Node.ELEMENT_NODE) && rootNode.getNodeName().equals(XMLConstants.ISCONFIG);
	}
	public static boolean isVariableConfig(Node rootNode) {
		return (rootNode.getNodeType()==Node.ELEMENT_NODE) && rootNode.getNodeName().equals(XMLConstants.ISVARIABLE);
	}
	public static boolean isDisplay(Node rootNode) {
		return (rootNode.getNodeType()==Node.ELEMENT_NODE) && rootNode.getNodeName().equals(XMLConstants.DISPLAYID);
	}
	public static boolean isDisplayProperty(Node rootNode) {
		return (rootNode.getNodeType()==Node.ELEMENT_NODE) && rootNode.getNodeName().equals(XMLConstants.PROPID);
	}
	private static String getName(NamedNodeMap att) {
		Node node = att.getNamedItem(XMLConstants.NAMEID);
		if (node!=null) return StringUtils.cleanupSpaces(node.getNodeValue());
		else return null;
	}
	private static Boolean getIsVisible(NamedNodeMap att) {
		Node node = att.getNamedItem(XMLConstants.VISIBLEID);
		if (node!=null) return Boolean.parseBoolean(node.getNodeValue());
		else return null;
	}
	private static String getClass(NamedNodeMap att) {
		Node node = att.getNamedItem(XMLConstants.CLASSID);
		if (node!=null) return StringUtils.cleanupSpaces(node.getNodeValue());
		else return null;
	}
	private static String getValue(NamedNodeMap att) {
		Node nodeID = att.getNamedItem(XMLConstants.VALUEID);
		if (nodeID!=null) return nodeID.getNodeValue();
		else return null;
	}

	public boolean tracksVariable(String vn) {
		if (trackedVariables!=null) return trackedVariables.containsKey(vn);
		return false;
	}

	public Variable getTrackedVariable(String vn) {
		if (trackedVariables!=null) return trackedVariables.get(vn);
		return null;
	}
	public LinkedHashMap<String, Variable> getTrackedVariables() {
		return trackedVariables;
	}
	
	public Collection<VarDisplay> getKBGraphicalElements() {
		List<VarDisplay> ret=null;
		if (trackedVariables!=null) {
			for(Variable v:trackedVariables.values()) {
				if(ret==null) ret=new ArrayList<VarDisplay>();
				ret.add(v.getDisplay());
			}
		}
		return ret;
	}

}
