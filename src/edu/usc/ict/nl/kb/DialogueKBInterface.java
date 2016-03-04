package edu.usc.ict.nl.kb;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.VariableProperties.PROPERTY;

public interface DialogueKBInterface extends InformationStateInterface {
	public String getName();
		
	public Object evaluate(Object f,EvalContext context) throws Exception;
	public Object evaluate(DialogueKBFormula f,EvalContext context) throws Exception;
	public Boolean evaluate(DialogueOperatorEffect e) throws Exception;
	
	public String normalizeNames(String name);
	
	public boolean isSupportedFormulaToBeStored(DialogueOperatorEffect e);
	/**
	 * admissible values for type are:
	 *  if a new KB should be generated when a change to the KB is required, then use AUTO_NEW
	 *  else, all rules and non-trivial assertions (i.e. not converted to assignments) are always stored with access AUTO_OVERWRITETHIS
	 *        assignments and trivial assertions can be stored with access AUTO_OVERWRITETHIS or AUTO_OVERWRITEAUTO
	 * @param e
	 * @param overwrite
	 * @param doForwardInference
	 * @return
	 * @throws Exception
	 */
	public DialogueKB store(DialogueOperatorEffect e,ACCESSTYPE type,boolean doForwardInference) throws Exception; 
	public DialogueKB storeAll(Collection<DialogueOperatorEffect> effects,ACCESSTYPE type,boolean doForwardInference) throws Exception;

	public DialogueKB getParent();
	public void setParent(DialogueKB parent);
	public Collection<DialogueKB> getChildren();
	public void addChild(DialogueKB c);
	public void clearKBTree();
	
	public Collection<DialogueOperatorEffect> dumpKB() throws Exception;
	public Collection<DialogueOperatorEffect> dumpKB(File dumpFile) throws Exception;
	public Collection<DialogueOperatorEffect> readFromFile(File dumpFile) throws Exception;
	public LinkedHashMap<String, Collection<DialogueOperatorEffect>> dumpKBTree() throws Exception;
	public Collection<DialogueOperatorEffect> flattenKBTree(LinkedHashMap<String, Collection<DialogueOperatorEffect>> kbTree);
	public void printKB(String indent);
	public String getContentID() throws Exception;

	public void invalidateCache();

	public DialogueKB findFirstKBInHierarchyWithID(String name);
	public DialogueKB findThisKBInHierarchy(DialogueKB kb);
	public boolean hasVariableNamed(String vName,ACCESSTYPE type);
	public boolean hasPredication(DialogueKBFormula f,ACCESSTYPE type);
	public void removeVariable(String vName,ACCESSTYPE type) throws Exception;
	public Object getValueOfVariable(String vName,ACCESSTYPE type,EvalContext context);
	public Object getValueOfPredication(DialogueKBFormula f,ACCESSTYPE type,EvalContext context);
	public DialogueKB findFirstKBInHierarchyThatContainsThisVariableName(String vName);
	public DialogueKB findFirstKBInHierarchyThatContainsThisPredication(DialogueKBFormula f);

	public List getSatisfyingArguments(DialogueKBFormula f,ACCESSTYPE type,EvalContext context);

	/**
	 * use this to assign a value to a variable
	 * @param vName
	 * @param value
	 * @param type
	 * @return
	 * @throws Exception
	 */
	public DialogueKB setValueOfVariable(String vName,Object value,ACCESSTYPE type) throws Exception;
	/**
	 * use this one to store a complex predication (assertion) as opposed to an assignment of a variable.
	 * @param f
	 * @param value
	 * @param type
	 * @return
	 * @throws Exception
	 */
	public DialogueKB setValueOfPredication(DialogueKBFormula f,Object value,ACCESSTYPE type) throws Exception;
	public void setValueOfVariableInKBNamed(String kbName, String vName,Object value) throws Exception;
	public Boolean isTrueInKB(DialogueKBFormula f,EvalContext context) throws Exception;
	public Set<String> getAllVariables() throws Exception;
	public Set<String> getAllVariablesInThisKB() throws Exception;

	/**
	 * accepts either AUTO_OVERWRITETHIS or AUTO_OVERWRITEAUTO. With THIS all modifications are stored in the current KB.
	 * @param type
	 * @throws Exception
	 */
	public void runForwardInference(ACCESSTYPE type) throws Exception;
	public LinkedHashSet<DialogueOperatorEffect> getForwardInferenceRules();
	public Boolean doesItContainThisRule(DialogueOperatorEffect e,ACCESSTYPE type);

	public DM getDM();
	
	/**
	 * READONLY, means the variable cannot be changed (default=false)
	 * HIDDEN, means that the variable is not exposed to the authoring tool (default=false)
	 * PERSISTENT, means that the variable is saved (default=true)
	 * @author morbini
	 *
	 */
	public boolean getPropertyForVar(String varName,PROPERTY p);
	public VariableProperties getProperties(String vName);
	public void setPropertyForVar(String varName,PROPERTY p,Boolean value);
	public void setProperties(String varName,VariableProperties properties);


}
