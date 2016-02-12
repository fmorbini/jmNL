package edu.usc.ict.nl.dm.reward.model.macro;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import edu.usc.ict.nl.dm.reward.model.RewardPolicy;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.XMLUtils;

public class MacroRepository {
	private static Map<String,Macro> repo=new HashMap<>();
	private static boolean blocked=false;

	public static void addMacro(String name,Macro macro) {
		repo.put(name, macro);
	}

	public static boolean containsMacro(String name) {
		if (blocked) return false;
		else return repo.containsKey(name);
	}

	public static Macro getMacro(String name) {
		if (blocked) return null;
		else return repo.get(name);
	}
	public static FormulaMacro getMacro(String name,int argCount) {
		if (blocked) return null;
		else {
			Macro m=repo.get(name);
			if (m!=null && m instanceof FormulaMacro) {
				int macroArgs=((FormulaMacro)m).getArgCount();
				if (macroArgs==argCount) return (FormulaMacro) m;
			}
		}
		return null;
	}

	public boolean isEmpty() {
		if (blocked) return true;
		else return repo.isEmpty();
	}
	
	public static void block() {blocked=true;}
	public static void unblock() {blocked=false;}

	public static void loadFromXML(File xmlFile) throws ParserConfigurationException, SAXException, IOException {
		MacroRepository.clear();
		Document doc = XMLUtils.parseXMLFile(xmlFile, true, true);
		Node rootNode = doc.getDocumentElement();
		if (RewardPolicy.isMacrosNode(rootNode)) {
			NodeList cs = rootNode.getChildNodes();
			for (int i = 0; i < cs.getLength(); i++) {
				Node c=cs.item(i);
				NamedNodeMap childAtt = c.getAttributes();
				if (RewardPolicy.isEventMacroNode(c)) {
					String left=RewardPolicy.getLeftNodeValue(childAtt);
					String right=RewardPolicy.getRightNodeValue(childAtt);
					if (!StringUtils.isEmptyString(left) && !StringUtils.isEmptyString(right)) {
						if (MacroRepository.containsMacro(left)) RewardPolicy.logger.warn("overwriting macro for: '"+left+"' with event macro '"+right+"'");
						MacroRepository.addMacro(left, new EventMacro(left,right));
					}
				} else if (RewardPolicy.isFormulatMacroNode(c)) {
					String left=RewardPolicy.getLeftNodeValue(childAtt);
					String right=RewardPolicy.getRightNodeValue(childAtt);
					try {
						DialogueKBFormula leftf=DialogueKBFormula.parse(left);
						DialogueKBFormula rightf=DialogueKBFormula.parse(right);
						if (leftf!=null && rightf!=null) {
							if (MacroRepository.containsMacro(leftf.getName())) RewardPolicy.logger.warn("overwriting macro for: '"+left+"' with formula macro '"+right+"'");
							MacroRepository.addMacro(leftf.getName(), new FormulaMacro(leftf,rightf));
						}
					} catch (Exception e) {
						RewardPolicy.logger.error("error parsing macro: '"+left+"' => '"+right+"'");
					}
				}
			}
		}
	}

	public static void clear() {
		repo.clear();
	}
}
