package edu.usc.ict.nl.kb;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.dm.reward.model.RewardPolicy;
import edu.usc.ict.nl.dm.reward.model.XMLConstants;
import edu.usc.ict.nl.kb.VariableProperties.PROPERTY;
import edu.usc.ict.nl.util.XMLUtils;
import edu.usc.ict.nl.util.graph.Node;

public abstract class DialogueKB extends Node implements DialogueKBInterface {

	protected DM dm=null;
	protected Set<String> tracedConstants=null;
	protected Map<String,VariableProperties> ps;

	public DialogueKB(DM dm) {
		this.dm=dm;
	}

	@Override
	public DM getDM() {
		return dm;
	}

	@Override
	public String normalizeNames(String name) {
		NLBusConfig config = (dm!=null)?dm.getConfiguration():null;
		if (config!=null && config.getCaseSensitive()) return name;
		else return name.toLowerCase();
	}

	@Override
	public DialogueKB findFirstKBInHierarchyWithID(String name) {
		String thisName=getName();
		if (thisName.equalsIgnoreCase(name)) return this;
		else {
			DialogueKBInterface parent=null;
			if ((parent=getParent())!=null) return parent.findFirstKBInHierarchyWithID(name);
		}
		return null;
	}
	@Override
	public DialogueKB findThisKBInHierarchy(DialogueKB kb) {
		if (this.equals(kb)) return this;
		else {
			DialogueKBInterface parent=null;
			if ((parent=getParent())!=null) return parent.findThisKBInHierarchy(kb);
		}
		return null;
	}
	public static boolean isOverwriteMode(ACCESSTYPE type) {
		switch (type) {
		case AUTO_OVERWRITEAUTO:
		case AUTO_OVERWRITETHIS:
		case THIS_OVERWRITETHIS:
			return true;
		default:
			return false;
		}
	}

	public void addTracingFor(String n) {
		if (tracedConstants==null) tracedConstants=new HashSet<String>();
		tracedConstants.add(n);
	}
	public void removeTracingFor(String n) {
		if (tracedConstants!=null) tracedConstants.remove(n);
	}
	public boolean getTracing(String n) {
		if (tracedConstants!=null) return tracedConstants.contains(n);
		else return false;
	}

	public Collection<Change> saveAssignmentsAndGetUpdates(ACCESSTYPE type, boolean doForwardInference, DialogueOperatorEffect... effs) throws Exception {
		return saveAssignmentsAndGetUpdates(type, doForwardInference, Arrays.asList(effs));
	}
	public Collection<Change> saveAssignmentsAndGetUpdates(ACCESSTYPE type, boolean doForwardInference, List<DialogueOperatorEffect> effs) throws Exception {
		effs=processAssignmentListToRemoveImplications(effs);
		Collection<Change> ret=null;
		DialogueKBInterface tmpKB = storeAll(effs, ACCESSTYPE.AUTO_NEW, doForwardInference);
		if (tmpKB!=null) {
			Collection<DialogueOperatorEffect> changes = tmpKB.dumpKB();
			if (changes!=null) {
				for(DialogueOperatorEffect e:changes) {
					DialogueKBFormula var=e.getAssignedVariable();
					if (var!=null) {
						String varName=var.getName();
						Object newValue=e.getAssignedExpression();
						Object oldValue=getValueOfVariable(varName,ACCESSTYPE.AUTO_OVERWRITEAUTO,null);
						if (ret==null) ret=new ArrayList<Change>();
						ret.add(new Change(varName, oldValue, newValue));
						logger.info("variable "+varName+" changed from "+oldValue+" to "+newValue);
						// updating information state.
						// keep track of local variables
						store(e, type, false);
						/*if (localVars!=null && (localVars==localVars.findFirstKBInHierarchyThatContainsThisVariableName(varName))) {
							logger.info("NODE EXE: saving '"+varName+"' in local kb.");
							localVars.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
						} else {
							permanentInfoState.store(e, ACCESSTYPE.AUTO_OVERWRITEAUTO, false);
							logger.info("NODE EXE: saving '"+varName+"' in global kb.");
						}*/

					} else logger.error("Error in change effect: "+e);
				}
			}
		}
		return ret;
	}

	public List<DialogueOperatorEffect> processAssignmentListToRemoveImplications(List<DialogueOperatorEffect> effs) {
		List<DialogueOperatorEffect> ret=null;
		if (effs!=null) {
			for(DialogueOperatorEffect e:effs) {
				DialogueOperatorEffect newe=null;
				if (e.isImplication()) {
					DialogueKBFormula ante=e.getAntecedent();
					DialogueOperatorEffect conse=e.getConsequent();
					DialogueOperatorEffect elsePart=e.getElseConsequent();
					try {
						Boolean result=(Boolean) evaluate(ante,null);
						if (result!=null) {
							if (result)	newe=conse;
							else newe=elsePart;
						}
					} catch (Exception ex) {logger.error(ex);}
				} else {
					newe=e;
				}
				if (newe!=null) {
					if (ret==null) ret=new ArrayList<DialogueOperatorEffect>();
					ret.add(newe);
				}
			}
		}
		return ret;
	}

	public Collection<Change> getCurrentValues(Set<String> excludeTheseVars) throws Exception {
		Collection<DialogueOperatorEffect> effs = dumpKB();
		Collection<Change> ret=null;
		if (effs!=null) {
			for(DialogueOperatorEffect e:effs) {
				DialogueKBFormula v = e.getAssignedVariable();
				String name=v.getName();
				if (excludeTheseVars==null || !excludeTheseVars.contains(name)) {
					if (ret==null) ret=new ArrayList<Change>();
					Object newValue=e.getAssignedExpression();
					ret.add(new Change(name, null, newValue));
				}
			}
		}
		return ret;
	}

	@Override
	public Collection<DialogueOperatorEffect> dumpKB(File dumpFile)	throws Exception {
		Collection<DialogueOperatorEffect> content=dumpKB();
		if (dumpFile!=null && content!=null) {
			BufferedWriter out=new BufferedWriter(new FileWriter(dumpFile));
			out.write("<"+XMLConstants.INITISID+">\n");
			for(DialogueOperatorEffect e:content) {
				VariableProperties ps=e.getAssignmentProperties();
				if (ps.getProperty(PROPERTY.PERSISTENT)) {
					out.write(e.toString(false,XMLConstants.LOADISID)+"\n");
				} else {
					if (logger.isDebugEnabled()) logger.debug("skipping saving variable '"+e.getAssignedVariable()+"' as it is not persistent.");
				}
			}
			out.write("</"+XMLConstants.INITISID+">\n");
			out.close();
		}
		return content;
	}

	@Override
	public void readFromFile(File dumpFile) throws Exception {
		List<DialogueOperatorEffect> ret=null;
		Document doc = XMLUtils.parseXMLFile(dumpFile, true, true);
		org.w3c.dom.Node rootNode = doc.getDocumentElement();
		if (RewardPolicy.isInitsNode(rootNode)) {
			Queue<org.w3c.dom.Node> q=new LinkedList<org.w3c.dom.Node>();
			NodeList cs = rootNode.getChildNodes();
			for (int i = 0; i < cs.getLength(); i++) q.add(cs.item(i));
			while(!q.isEmpty()) {
				org.w3c.dom.Node c=q.poll();
				NamedNodeMap childAtt = c.getAttributes();
				if (RewardPolicy.isInitNode(c)) {
					DialogueOperatorEffect eff=null;
					try {
						eff=DialogueOperatorEffect.parse(RewardPolicy.getInitNodeValue(childAtt));
					} catch (Exception e) {e.printStackTrace();}
					if (eff!=null) {
						if (ret==null) ret=new ArrayList<DialogueOperatorEffect>();
						ret.add(eff);
					} else throw new Exception("Problem with IS initialization expr: "+XMLUtils.prettyPrintDom(c, " ", true, true));
				}
			}
		}
		if(ret!=null) {
			storeAll(ret, ACCESSTYPE.AUTO_OVERWRITEAUTO, true);
		}
	}
	
	@Override
	public boolean getPropertyForVar(String varName, PROPERTY p) {
		varName=normalizeNames(varName);
		VariableProperties vps=null;
		if (ps!=null) vps=ps.get(varName);
		if (vps==null) vps=VariableProperties.defaultProperties;
		return vps.getProperty(p);
	}
	@Override
	public VariableProperties getProperties(String varName) {
		varName=normalizeNames(varName);
		VariableProperties vps=null;
		if (ps!=null && ((vps=ps.get(varName))!=null)) return vps;
		else return VariableProperties.defaultProperties;
	}
	@Override
	public void setPropertyForVar(String varName, PROPERTY p, boolean value) {
		varName=normalizeNames(varName);
		if (!VariableProperties.isDefault(p, value)) {
			if (ps==null) ps=new HashMap<String, VariableProperties>();
			VariableProperties vps=ps.get(varName);
			if (vps==null) ps.put(varName, vps=new VariableProperties());
			vps.setProperty(p, value);
		}
	}
	public void setProperties(String varName, VariableProperties properties) {
		if (properties!=null) {
			for(PROPERTY p:PROPERTY.values()) {
				setPropertyForVar(varName, p, properties.getProperty(p));
			}
		}
	}
}
