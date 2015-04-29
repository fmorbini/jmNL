package edu.usc.ict.nl.bus.special_variables;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import edu.usc.ict.nl.dm.reward.model.XMLConstants;
import edu.usc.ict.nl.kb.VariableProperties;
import edu.usc.ict.nl.kb.VariableProperties.PROPERTY;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.XMLUtils;

public class SpecialVar {
	String name;
	String value;
	Class type;
	boolean hidden=VariableProperties.getDefault(PROPERTY.HIDDEN),
			readOnly=VariableProperties.getDefault(PROPERTY.READONLY),
			persistent=VariableProperties.getDefault(PROPERTY.PERSISTENT);
	String description;
	public SpecialVar(SpecialEntitiesRepository svs, String name,String description,String value,Class type,Boolean hidden,Boolean readOnly,Boolean persistent) {
		this.name=name;
		this.description=description;
		this.value=value;
		this.type=type;
		if (hidden!=null) this.hidden=hidden;
		if (readOnly!=null) this.readOnly=readOnly;
		if (persistent!=null) this.persistent=persistent;
		if (svs!=null) svs.addSpecialVariable(this);
	}
	public SpecialVar(SpecialEntitiesRepository svs, String name,String description,String value,Class type) {
		this(svs, name, description, value, type, null, null,null);
	}
	public SpecialVar(SpecialEntitiesRepository svs, String name,String description,String value,Class type,Boolean hidden) {
		this(svs, name, description, value, type, hidden, null,null);
	}
	public String toXml() {
		return "<"+XMLConstants.specialVar+" "+XMLConstants.IDID+"=\""+XMLUtils.escapeStringForXML(getName())+"\" "+
		XMLConstants.VALUEID+"=\""+XMLUtils.escapeStringForXML(value)+"\" "+
		XMLConstants.TYPEID+"=\""+type+"\" "+
		XMLConstants.VISIBLEID+"=\""+!isHidden()+"\" "+
		XMLConstants.READONLYID+"=\""+isReadOnly()+"\" "+
		XMLConstants.PERSISTENTID+"=\""+isPersistent()+"\" "+
		XMLConstants.DESCRIPTIONID+"=\""+XMLUtils.escapeStringForXML(description)+"\"/>";
	}
	@Override
	public String toString() {
		return getName();
	}
	public SpecialVar fromXml(SpecialEntitiesRepository svs,String xml) throws ParserConfigurationException, SAXException, IOException {
		Document doc = XMLUtils.parseXMLString(xml, false, false);
		Node rootNode = doc.getDocumentElement();
		if (isSpecialVariableNode(rootNode)) {
			NamedNodeMap att = rootNode.getAttributes();
			SpecialVar sv=new SpecialVar(svs,getName(att),getDescription(att),getValue(att),getType(att),getIsHidden(att),getIsReadOnly(att),getIsPersistent(att));
			return sv;
		}
		return null;
	}
	public boolean isSpecialVariableNode(Node n) {
		return (n.getNodeType()==Node.ELEMENT_NODE) && n.getNodeName().toLowerCase().equals(XMLConstants.specialVar);
	}
	private String getName(NamedNodeMap att) {
		Node nodeID = att.getNamedItem(XMLConstants.IDID);
		if (nodeID!=null) return StringUtils.cleanupSpaces(nodeID.getNodeValue());
		else return null;
	}
	private String getValue(NamedNodeMap att) {
		Node nodeID = att.getNamedItem(XMLConstants.VALUEID);
		if (nodeID!=null) return nodeID.getNodeValue();
		else return null;
	}
	public static Boolean getIsHidden(NamedNodeMap att) {
		Node nodeID = att.getNamedItem(XMLConstants.VISIBLEID);
		if (nodeID!=null) {
			Boolean tmp=Boolean.parseBoolean(nodeID.getNodeValue());
			return (tmp==null)?tmp:!tmp;
		}
		else return null;
	}
	public static Boolean getIsReadOnly(NamedNodeMap att) {
		Node nodeID = att.getNamedItem(XMLConstants.READONLYID);
		if (nodeID!=null) return Boolean.parseBoolean(nodeID.getNodeValue());
		else return null;
	}
	public static Boolean getIsPersistent(NamedNodeMap att) {
		Node nodeID = att.getNamedItem(XMLConstants.PERSISTENTID);
		if (nodeID!=null) return Boolean.parseBoolean(nodeID.getNodeValue());
		else return null;
	}
	private String getDescription(NamedNodeMap att) {
		Node nodeID = att.getNamedItem(XMLConstants.DESCRIPTIONID);
		if (nodeID!=null) return nodeID.getNodeValue();
		else return null;
	}
	private Class getType(NamedNodeMap att) {
		Node nodeID = att.getNamedItem(XMLConstants.TYPEID);
		if (nodeID!=null) {
			try {
				return Class.forName(nodeID.getNodeValue());
			} catch (Exception e) {
				return null;
			}
		}
		else return null;
	}
	public String getValue() {return value;}
	public String getDescription() {return description;}
	public String getName() {return name;}
	public Class getType() {return type;}
	public boolean isHidden() {return hidden;}
	public boolean isReadOnly() {return readOnly;}
	public boolean isPersistent() {return persistent;}
}