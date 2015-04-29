package edu.usc.ict.nl.dm.visualizer.kbDisplay;

import java.util.Map;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import edu.usc.ict.nl.dm.reward.model.XMLConstants;
import edu.usc.ict.nl.dm.visualizer.VisualizerConfig;
import edu.usc.ict.nl.util.StringUtils;

public class Variable {
	private String internalName,prettyName;
	private VarDisplay display;
	private Object value;

	public Variable(NamedNodeMap att, Map<String, Node> displayers) throws Exception {
		internalName=getVariableName(att);
		prettyName=getPrettyVariableName(att);
		Node displayerDefinition=displayers.get(getDisplayName(att));
		display=VisualizerConfig.buildDisplayer(displayerDefinition, displayerDefinition.getAttributes());
		display.setPrettyText(prettyName);
	}
	
	public VarDisplay getDisplay() {
		return display;
	}
	
	public boolean isValid() {
		return !StringUtils.isEmptyString(internalName) && !StringUtils.isEmptyString(prettyName);
	}
	
	private static String getVariableName(NamedNodeMap att) {
		Node node = att.getNamedItem(XMLConstants.NAMEID);
		if (node!=null) return StringUtils.cleanupSpaces(node.getNodeValue());
		else return null;
	}
	private static String getPrettyVariableName(NamedNodeMap att) {
		Node node = att.getNamedItem(XMLConstants.PRETTYNAMEID);
		if (node!=null) return StringUtils.cleanupSpaces(node.getNodeValue());
		else return null;
	}
	private static String getDisplayName(NamedNodeMap att) {
		Node node = att.getNamedItem(XMLConstants.DESIGNID);
		if (node!=null) return StringUtils.cleanupSpaces(node.getNodeValue());
		else return null;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj==null) return false;
		else if (obj instanceof String && obj.equals(internalName)) return true;
		else if (obj instanceof Variable && this==obj) return true;
		else return super.equals(obj);
	}

	public String getInternalName() {return internalName;}
	public String getPrettyName() {return prettyName;}

	public void setValue(Object vv) {
		value=vv;
		display.setValue(getInternalName(),vv);
	}

}
