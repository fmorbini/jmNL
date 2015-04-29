package edu.usc.ict.nl.vhmsg;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.usc.ict.nl.dm.reward.model.XMLConstants;
import edu.usc.ict.nl.util.XMLUtils;
import edu.usc.ict.vhmsg.MessageEvent;
public class VHBridgewithMinat extends VHBridge {

	public VHBridgewithMinat(String server, String topic) {
		super(server, topic);
	}

	public class Minat {
		public class Decision {
			String name;
			Float confidence;
			public Decision(String name, Float confidence) {
				this.name=name;
				this.confidence=confidence;
			}
			public Float getScore() {return confidence;}
			public String getName() {return name;}
		}
		private boolean isLabels(Node c) {
			return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.LABELSIB);
		}
		private boolean isLabel(Node c) {
			return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.LABELIB);
		}
		private boolean isTriage(Node c) {
			return (c.getNodeType()==Node.ELEMENT_NODE) && c.getNodeName().toLowerCase().equals(XMLConstants.TRIAGEID);
		}
		private String getLabelName(NamedNodeMap att) {
			Node nodeID = att.getNamedItem(XMLConstants.CLASSID);
			if (nodeID!=null) return nodeID.getNodeValue();
			else return null;
		}
		private Float getScore(NamedNodeMap att) {
			Node nodeID = att.getNamedItem(XMLConstants.SCOREID);
			if (nodeID!=null) return Float.parseFloat(nodeID.getNodeValue());
			else return null;
		}

		private Decision triageClass;
		private List<Decision> labels;
		public Minat(String xml) throws Exception {
			Document doc = XMLUtils.parseXMLString(xml, false, false);
			//System.out.println(XMLUtils.prettyPrintDom(doc, " ", true, true));
			Element rootNode = doc.getDocumentElement();
			logger.debug(XMLUtils.prettyPrintDom(rootNode, " ", true, true));
			Queue<Node> q=new LinkedList<Node>();
			NodeList cs = rootNode.getChildNodes();
			for (int i = 0; i < cs.getLength(); i++) q.add(cs.item(i));
			while(!q.isEmpty()) {
				Node c=q.poll();
				NamedNodeMap att = c.getAttributes();
				if (isLabels(c)) {
					cs = c.getChildNodes();
					for (int i = 0; i < cs.getLength(); i++) q.add(cs.item(i));
				} else if (isTriage(c)) {
					triageClass=new Decision(getLabelName(att), getScore(att));
				} else if (isLabel(c)) {
					Decision label=new Decision(getLabelName(att), getScore(att));
					if (labels==null) labels=new ArrayList<Minat.Decision>();
					labels.add(label);
				}
			}
		}
		public Decision getTriage() {
			return triageClass;
		}
		public List<Decision> getLabels() {
			return labels;
		}
	}
	
	public Minat processMinatEvent(MessageEvent e) throws Exception {
		Map<String, ?> map = e.getMap();
		if(map.containsKey("minat")){
			String msg=(String) map.get("minat");
			if (logger.isDebugEnabled()) logger.debug("minat message received: '"+msg+"'");
			return new Minat(msg);
		} else {
			throw new Exception("The message passed to VRPerception is not a vrPerception message: "+e.toString());
		}
	}
}
