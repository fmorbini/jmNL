package edu.usc.ict.nl.dm.reward.model;

import java.util.List;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import edu.usc.ict.nl.util.StringUtils;

public class DialogueOperatorTopic extends edu.usc.ict.nl.util.graph.Node {

	public static final String separator=".";
	
	public DialogueOperatorTopic(String id) {
		super(id);
	}
	
	public static String getTopicName(NamedNodeMap att) {
		Node nodeID = att.getNamedItem(XMLConstants.IDID);
		if (nodeID!=null) return StringUtils.cleanupSpaces(nodeID.getNodeValue());
		else return null;
	}

	public static boolean isTopicNode(Node c) {
		return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.TOPICID);
	}

	public String getCompleteName() {
		String totalName=getName();
		DialogueOperatorTopic parent=this;
		while(true) {
			try {
				parent=(DialogueOperatorTopic) parent.getSingleParent();
				if (parent!=null && parent.hasParents()) totalName=parent.getName()+separator+totalName;
				else return totalName;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public String getValue() {
		return name; 
	}
	@Override
	public void setValue(Object v) {
		name=(String)v;
	}

	@Override
	public String toString() {
		return toString(true);
	}
	public String toString(boolean shortForm) {
		if (shortForm) return getCompleteName();
		else return "<"+XMLConstants.TOPICID+" "+XMLConstants.IDID+"=\""+getCompleteName()+"\"/>";
	}

	/** return true if any ancestor of this including itself is the given argument.
	 * 
	 * @param node
	 * @throws Exception 
	 */
	public boolean contains(DialogueOperatorTopic node) throws Exception {
		if (node==null) return false;
		else {
			DialogueOperatorTopic current=this;
			while(current!=null) {
				if (current==node) return true;
				else current=(DialogueOperatorTopic) current.getSingleParent();
			}
		}
		return false;
	}
	
	public boolean containsSomeOfThese(List<DialogueOperatorTopic> topics) throws Exception {
		if (topics!=null) {
			for(DialogueOperatorTopic topic:topics) {
				if (this.contains(topic)) return true;
			}
		}
		return false;
	}
}
