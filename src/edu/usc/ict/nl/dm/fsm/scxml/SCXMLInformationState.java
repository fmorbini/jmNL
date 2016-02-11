package edu.usc.ict.nl.dm.fsm.scxml;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.scxml.Context;
import org.apache.commons.scxml.SCXMLExecutor;
import org.apache.commons.scxml.model.TransitionTarget;

import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.kb.EvalContext;

public class SCXMLInformationState extends DialogueKB {

	private SCXMLExecutor exe=null;

	public SCXMLInformationState(SCXMLRunner dm) {
		super(dm);
	}

	@Override
	public Object evaluate(Object f,EvalContext context) throws Exception {
		throw new Exception("unsupported");
	}

	@Override
	public Object evaluate(DialogueKBFormula f,EvalContext context) throws Exception {
		throw new Exception("unsupported");
	}

	@Override
	public Boolean evaluate(DialogueOperatorEffect e) throws Exception {
		throw new Exception("unsupported");
	}

	@Override
	public DialogueKB store(DialogueOperatorEffect e,ACCESSTYPE type,boolean doForwardInference) throws Exception {
		throw new Exception("unsupported");
	}

	@Override
	public DialogueKB storeAll(
			Collection<DialogueOperatorEffect> effects, ACCESSTYPE type,boolean doForwardInference)
			throws Exception {
		throw new Exception("unsupported");
	}

	@Override
	public boolean isSupportedFormulaToBeStored(DialogueOperatorEffect e) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public DialogueKB getParent() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Collection<DialogueKB> getChildren() {
		return null;
	}

	@Override
	public void setParent(DialogueKB parent) {
		// TODO Auto-generated method stub
		
	}

	public LinkedHashMap<String, Collection<DialogueOperatorEffect>> dumpKBTree() throws Exception {
		LinkedHashMap<String, Collection<DialogueOperatorEffect>> ret=null;
		Map<TransitionTarget,Context> variables=exe.getAllContexts();
		for(Entry<TransitionTarget, Context> ttc:variables.entrySet()) {
			Context c=ttc.getValue();
			TransitionTarget tt=ttc.getKey();
			String name=tt.getId();
			Map<String,Object> vars=c.getVars();
			for(Entry<String,Object>vv:vars.entrySet()) {
				if (ret==null) ret=new LinkedHashMap<String, Collection<DialogueOperatorEffect>>();
				Collection<DialogueOperatorEffect> list=ret.get(name);
				if (list==null) ret.put(name, list=new ArrayList<DialogueOperatorEffect>());
				String varName=vv.getKey();
				Object varValue=vv.getValue();
				list.add(DialogueOperatorEffect.createAssignment(DialogueKBFormula.create(varName, null), varValue));
			}
		}
		return ret;
	}
	@Override
	public Set<String> getAllVariables() throws Exception {
		Set<String> ret=null;
		Map<TransitionTarget,Context> variables=exe.getAllContexts();
		for(Entry<TransitionTarget, Context> ttc:variables.entrySet()) {
			Context c=ttc.getValue();
			TransitionTarget tt=ttc.getKey();
			String name=tt.getId();
			Map<String,Object> vars=c.getVars();
			for(Entry<String,Object>vv:vars.entrySet()) {
				if (ret==null) ret=new HashSet<String>();
				String varName=vv.getKey();
				ret.add(varName);
			}
		}
		return ret;
	}
	@Override
	public Set<String> getAllVariablesInThisKB() throws Exception {
		return getAllVariables();
	}

	@Override
	public Collection<DialogueOperatorEffect> dumpKB() throws Exception {
		LinkedHashMap<String, Collection<DialogueOperatorEffect>> contents = dumpKBTree();
		Collection<DialogueOperatorEffect> ret=new ArrayList<DialogueOperatorEffect>();
		if (contents!=null) {
			for(Collection<DialogueOperatorEffect> content:contents.values()) {
				ret.addAll(content);
			}
		}
		return ret;
	}

	@Override
	public void printKB(String indent) {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean hasVariableNamed(String vName,ACCESSTYPE type) {
		Context c=exe.getRootContext();
		return c.has(vName);
	}
	@Override
	public boolean hasPredication(DialogueKBFormula f, ACCESSTYPE type) {
		return hasVariableNamed(f.getName(), type);
	}

	@Override
	public Object getValueOfVariable(String vName,ACCESSTYPE type,EvalContext context) {
		Context c=exe.getRootContext();
		return c.get(vName);
	}
	@Override
	public Object getValueOfPredication(DialogueKBFormula f, ACCESSTYPE type, EvalContext context) {
		return getValueOfVariable(f.getName(), type, context);
	}


	@Override
	public DialogueKB findFirstKBInHierarchyThatContainsThisVariableName(String vName) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public DialogueKB findFirstKBInHierarchyThatContainsThisPredication(DialogueKBFormula f) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DialogueKB findThisKBInHierarchy(DialogueKB kb) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean isTrueInKB(DialogueKBFormula f,EvalContext context) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		return null;
	}
	/*
	public static final Logger logger = Logger.getLogger(SCXMLInformationState.class.getName());

	@Override
	public Object get(String vName) {
		Context c=exe.getRootContext();
		return c.get(vName);
	}

	@Override
	public void set(String vName, Object value) {
		Context c=exe.getRootContext();
		c.set(vName,value);
	}

	@Override
	public InfoStateEvent buildInfoStateEvent(Long sessionId) {
		InfoStateEvent replyEvent=new InfoStateEvent();
		Map<TransitionTarget,Context> variables=exe.getAllContexts();
		for(Entry<TransitionTarget, Context> ttc:variables.entrySet()) {
			Context c=ttc.getValue();
			TransitionTarget tt=ttc.getKey();
			Map<String,Object> vars=c.getVars();
			for(Entry<String,Object>vv:vars.entrySet()) {
				String varName=vv.getKey();
				Object varValue=vv.getValue();
				if (varValue instanceof String) {
					replyEvent.add(tt.getId(), varName, (String) varValue);
				} else if (varValue==null) {
					replyEvent.add(tt.getId(), varName, "null");
				} else if (varValue instanceof Set) {
					replyEvent.add(tt.getId(), varName, (Set) varValue);
				} else if (varValue instanceof Stack) {
					replyEvent.add(tt.getId(), varName, (Stack) varValue);
				} else {
					logger.warn("handleGetInfoStateEvent: ignoring variable: "+varName+" because has unknown type: "+varValue.getClass());
				}
			}
		}
		return replyEvent;
	}
*/	
	public void setIS(SCXMLExecutor is) {
		this.exe=is;
	}

	@Override
	public void clearKBTree() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public DialogueKB setValueOfVariable(String vName, Object value, ACCESSTYPE type) throws Exception {
		Context c=exe.getRootContext();
		c.set(vName,value);
		return this;
	}
	@Override
	public DialogueKB setValueOfPredication(DialogueKBFormula f, Object value, ACCESSTYPE type) throws Exception {
		return setValueOfVariable(f.getName(), value, type);
	}

	@Override
	public List getSatisfyingArguments(DialogueKBFormula f, ACCESSTYPE type, EvalContext context) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public Object get(String vName) {
		return getValueOfVariable(vName,ACCESSTYPE.AUTO_OVERWRITEAUTO,null);
	}

	@Override
	public void set(String vName, Object value) {
		try {
			setValueOfVariable(vName, value,ACCESSTYPE.AUTO_OVERWRITEAUTO);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
/*
	@Override
	public InfoStateEvent buildInfoStateEvent(Long sessionId) {
		// TODO Auto-generated method stub
		return null;
	}
*/
	/*@Override
	public DialogueKBInterface updateISwithTheseRules(List<Pair<DialogueKBFormula, DialogueOperatorEffect>> rules,boolean overwrite) throws Exception {
		throw new Exception("unsupported");
	}*/

	@Override
	public void removeVariable(String vName,ACCESSTYPE type) throws Exception {
		throw new Exception("unsupported");
	}
	@Override
	public void runForwardInference(ACCESSTYPE type) throws Exception {
		throw new Exception("unsupported");
	}

	@Override
	public LinkedHashSet<DialogueOperatorEffect> getForwardInferenceRules() {
		return null;
	}
	@Override
	public Boolean doesItContainThisRule(DialogueOperatorEffect e,ACCESSTYPE type) {
		return null;
	}

	@Override
	public String getContentID() {
		return null;
	}

	@Override
	public void addChild(DialogueKB c) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void invalidateCache() {
		// TODO Auto-generated method stub
		
	}





}
