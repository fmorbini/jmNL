package edu.usc.ict.nl.dm.reward.util;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKBFormula;
import edu.usc.ict.nl.utils.LogConfig;

public class CompressedEffectList {
	
	private static final Logger logger = Logger.getLogger(CompressedEffectList.class.getName());
	static {
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );	
	}
	
	private enum ReadWriteStatus {WRITABLE,READABLE,NULL};

	public class ReadWriteEffect implements Comparable<ReadWriteEffect> {
		private DialogueOperatorEffect effect;
		private ReadWriteStatus status=ReadWriteStatus.WRITABLE;
		private Set<DialogueKBFormula> uses;
		
		public ReadWriteEffect(DialogueOperatorEffect e, Set<DialogueKBFormula> uses) {
			this.effect=e;
			this.uses=uses;
			if (!this.effect.isAssignment()) removeWrite();
		}
		public boolean isReadable() {return status==ReadWriteStatus.WRITABLE || status==ReadWriteStatus.READABLE;}
		public boolean isWritable() {return status==ReadWriteStatus.WRITABLE;}
		public void removeAll() {status=ReadWriteStatus.NULL;}
		public void removeWrite() {if (isReadable()) status=ReadWriteStatus.READABLE;}
		public void removeRead() {removeAll();}
		
		@Override
		public String toString() {
			return "["+effect+":"+(isWritable()?"W":"")+(isReadable()?"R":"")+"]";
		}
		
		@Override
		protected ReadWriteEffect clone() throws CloneNotSupportedException {
			ReadWriteEffect copy=new ReadWriteEffect(effect,(uses!=null)?new HashSet<DialogueKBFormula>(uses):null);
			copy.status=status;
			return copy;
		}
		@Override
		public int compareTo(ReadWriteEffect o) {
			if (this.equals(o)) return 0;
			//System.out.print(this+" "+o+" "+o.uses+" ");
			if (effect==null) {
				//System.out.println("1");
				return -1;
			}
			// if it's an assignment and the assigned variable is in the use list of o then this must be before o.
			// else alphabetically sort the ids of the 2 effects.
			if (effect.isAssignment() && (o.uses!=null) && o.uses.contains(effect.getAssignedVariable())) {
				//System.out.println("2.1");
				return -1;
			} else if (o.effect.isAssignment() && (uses!=null) && uses.contains(o.effect.getAssignedVariable())) {
					//System.out.println("2.2");
					return 1;
			} else {
				//System.out.println("3");
				return effect.getID().compareTo(o.effect.getID());
			}
		}
		public DialogueOperatorEffect getEffect() {return effect;}
	}

	
	private SortedSet<ReadWriteEffect> compressedEffects;
	private HashMap<String, ReadWriteEffect> lastValidAssignmentForVar;
	
	public SortedSet<ReadWriteEffect> getCompressedEffects() {return compressedEffects;}
	public List<DialogueOperatorEffect> getEffects() {
		SortedSet<ReadWriteEffect> rwes = getCompressedEffects();
		List<DialogueOperatorEffect> ret=null;
		if (rwes!=null) {
			for(ReadWriteEffect rwe:rwes) {
				if (ret==null) ret=new ArrayList<DialogueOperatorEffect>();
				ret.add(rwe.getEffect());
			}
		}
		return ret;
	}
	
	public CompressedEffectList() {}
	public CompressedEffectList(List<DialogueOperatorEffect> effects) {
		try {
			mergeTheseEffects(effects);
		} catch (Exception e) {
			logger.error("error while compressing effects: ",e);
		}
	}

	@Override
	public CompressedEffectList clone() throws CloneNotSupportedException {
		boolean needsTranslationTable=(lastValidAssignmentForVar!=null) && !lastValidAssignmentForVar.isEmpty();
		CompressedEffectList copy=new CompressedEffectList();
		if (compressedEffects!=null) {
			HashMap<ReadWriteEffect, ReadWriteEffect> conversionMap=null;
			copy.compressedEffects=new TreeSet<ReadWriteEffect>();
			for (ReadWriteEffect e:compressedEffects) {
				ReadWriteEffect newe = e.clone();
				if (needsTranslationTable) {
					if (conversionMap==null) conversionMap=new HashMap<ReadWriteEffect, ReadWriteEffect>();
					conversionMap.put(e,newe);
				}
				copy.compressedEffects.add(newe);
			}
			if (needsTranslationTable)
				copy.lastValidAssignmentForVar=new HashMap<String, ReadWriteEffect>();
				for(String varName:lastValidAssignmentForVar.keySet()) {
					ReadWriteEffect oldRWE = lastValidAssignmentForVar.get(varName);
					assert(oldRWE!=null);
					ReadWriteEffect newRWE = conversionMap.get(oldRWE);
					assert(newRWE!=null);
					copy.lastValidAssignmentForVar.put(varName, newRWE);
				}
		}
		return copy;
	}
	
	public void mergeTheseEffects(List<DialogueOperatorEffect> effects) throws Exception {
		if (effects!=null && !effects.isEmpty()) {
			if (compressedEffects==null) compressedEffects=new TreeSet<ReadWriteEffect>();
			/* if list is empty
			 *  add effect to list.
			 *  if assignment store position for last assigned variable.
			 * else
			 *  if assignment
			 *   extract used variables
			 *   if assigned variable is writable and all used variables are readable
			 *    substitute value of used variables and simplify and update assignment.
			 *   else, add, invalidate all used arguments
			 *  else, add
			 *  
			 *  when adding an effect to the list:
			 *   all used variables, are not writable anymore
			 *   if an assignment: all previous assignments to that same variable are not readable
			 */
			if (lastValidAssignmentForVar==null) lastValidAssignmentForVar=new HashMap<String, ReadWriteEffect>();
			for(DialogueOperatorEffect e:effects) {
				Set<DialogueKBFormula> names = e.extractAllNamesUsed();
				if (e.isAssignment()) {
					Object thing = e.getAssignedExpression();
					if (thing instanceof DialogueKBFormula) {
						String var=e.getAssignedVariable().getName();
						boolean assignedVariableWritable=isVariableWritable(var);
						boolean usedVariablesReadable=true;
						if(names!=null) {
							for(DialogueKBFormula v:names) {
								if (!isVariableReadable(v.getName()) && isVariableKnown(v.getName())) {
									usedVariablesReadable=false;
									break;
								}
							}
						}
						if (usedVariablesReadable) {
							HashMap<DialogueKBFormula,DialogueKBFormula> replacements=buildMapFromNames(names);
							DialogueKBFormula r=(DialogueKBFormula) thing;
							DialogueKBFormula nf = r.substitute(replacements);
							if (isVariableReadable(var)) {
								DialogueKBFormula oldValue=getValueForVariable(var);
								if (oldValue!=nf) {
									if (assignedVariableWritable) updateAssignmentWith(e,nf);
									else addEffect(e, names);
								}
							} else addEffect(e, names);
						} else addEffect(e, names);
					}
				} else  {
					addEffect(e,names);
				}
			}
		}
	}

	private HashMap<DialogueKBFormula, DialogueKBFormula> buildMapFromNames(Set<DialogueKBFormula> names) {
		HashMap<DialogueKBFormula, DialogueKBFormula>ret=null;
		if (names!=null) {
			for(DialogueKBFormula f:names) {
				DialogueKBFormula value=getValueForVariable(f.getName());
				if (value!=null) {
					if (ret==null) ret=new HashMap<DialogueKBFormula, DialogueKBFormula>();
					ret.put(f, value);
				}
			}
		}
		return ret;
	}
	private DialogueKBFormula getValueForVariable(String vName) {
		ReadWriteEffect rwe=lastValidAssignmentForVar.get(vName);
		if (rwe!=null) {
			DialogueOperatorEffect e=rwe.effect;
			assert(e.isAssignment() && rwe.isReadable());
			return (DialogueKBFormula) e.getAssignedExpression();
		}
		return null;
	}

	private void updateAssignmentWith(DialogueOperatorEffect e,DialogueKBFormula nr) throws Exception {
		if (e.isAssignment()) {
			DialogueKBFormula var=e.getAssignedVariable();
			String varName=var.getName();
			ReadWriteEffect effectToUpdate=lastValidAssignmentForVar.get(varName);
			if (effectToUpdate!=null) {
				compressedEffects.remove(effectToUpdate);
				DialogueOperatorEffect ne=DialogueOperatorEffect.createAssignment(var, nr);
				Set<DialogueKBFormula> names = nr.extractAllNamesUsed();
				if (names!=null) for(DialogueKBFormula n:names) removeWriteForVariable(n.getName());
				effectToUpdate.effect=ne;
				effectToUpdate.status=ReadWriteStatus.WRITABLE;
				compressedEffects.add(effectToUpdate);
			}
		}
	}

	private void addEffect(DialogueOperatorEffect e,Set<DialogueKBFormula> uses) {
		if (uses!=null) for(DialogueKBFormula n:uses) removeWriteForVariable(n.getName());
		ReadWriteEffect rwe = new ReadWriteEffect(e,uses);
		if (e.isAssignment()) {
			String var=e.getAssignedVariable().getName();
			removeReadForVariable(var);
			lastValidAssignmentForVar.put(var, rwe);
		}
		compressedEffects.add(rwe);
	}
	
	private boolean isVariableKnown(String var) {
		ReadWriteEffect rwe=lastValidAssignmentForVar.get(var);
		return (rwe!=null);
	}
	private void removeReadForVariable(String var) {
		ReadWriteEffect rwe=lastValidAssignmentForVar.get(var);
		if (rwe!=null) {
			rwe.removeRead();
		}
	}
	private void removeWriteForVariable(String var) {
		ReadWriteEffect rwe=lastValidAssignmentForVar.get(var);
		if (rwe!=null) {
			rwe.removeWrite();
		}
	}
	private boolean isVariableReadable(String var) {
		ReadWriteEffect rwe=lastValidAssignmentForVar.get(var);
		if (rwe!=null) {
			return rwe.isReadable();
		}
		return false;
	}
	private boolean isVariableWritable(String var) {
		ReadWriteEffect rwe=lastValidAssignmentForVar.get(var);
		if (rwe!=null) {
			return rwe.isWritable();
		}
		return false;
	}

	public String getID() {
		if (getCompressedEffects()!=null) {
			String id="";
			for(ReadWriteEffect e:getCompressedEffects()) id+=e.effect.getID();
			return id;
		} else return null;
	}
	
	public static void main(String[] args) throws Exception {
		CompressedEffectList cel = new CompressedEffectList();
		DialogueOperatorEffect e1=DialogueOperatorEffect.createAssignment(DialogueKBFormula.create("smalltalk_hawaii_beat_counter", null), "smalltalk");
		DialogueOperatorEffect e2=DialogueOperatorEffect.createIncrementForVariable("smalltalk",5);
		DialogueOperatorEffect e3=DialogueOperatorEffect.createGoal("rapport", DialogueKBFormula.create("20", null));
		DialogueOperatorEffect e4=DialogueOperatorEffect.createGoal("rapport", DialogueKBFormula.create("21", null));
		List<DialogueOperatorEffect> el=new ArrayList<DialogueOperatorEffect>();
		el.add(e1);el.add(e2);el.add(e3);el.add(e4);
		System.out.println("111111111111111");
		cel.mergeTheseEffects(el);
		System.out.println("111111111111111111111111111");
		for(ReadWriteEffect ex:cel.getCompressedEffects()) {
			System.out.println("  "+ex+" "+ex.effect.getID());
		}
		List<ReadWriteEffect> effects = new ArrayList<ReadWriteEffect>(cel.getCompressedEffects());
		Collections.sort(effects);
		for(ReadWriteEffect ex:effects) {
			System.out.println("  "+ex+" "+ex.effect.getID());
		}

		cel=new CompressedEffectList();
		el.clear();
		el.add(e2);el.add(e3);el.add(e1);el.add(e4);
		System.out.println("111111111111111");
		cel.mergeTheseEffects(el);
		System.out.println("111111111111111111111111111");
		for(ReadWriteEffect ex:cel.getCompressedEffects()) {
			System.out.println("  "+ex+" "+ex.effect.getID());
		}
		effects = new ArrayList<ReadWriteEffect>(cel.getCompressedEffects());
		Collections.sort(effects);
		for(ReadWriteEffect ex:effects) {
			System.out.println("  "+ex+" "+ex.effect.getID());
		}

		cel=new CompressedEffectList();
		el.clear();
		el.add(e4);el.add(e3);el.add(e2);el.add(e1);
		System.out.println("111111111111111");
		cel.mergeTheseEffects(el);
		System.out.println("111111111111111111111111111");
		for(ReadWriteEffect ex:cel.getCompressedEffects()) {
			System.out.println("  "+ex+" "+ex.effect.getID());
		}
		effects = new ArrayList<ReadWriteEffect>(cel.getCompressedEffects());
		Collections.sort(effects);
		for(ReadWriteEffect ex:effects) {
			System.out.println("  "+ex+" "+ex.effect.getID());
		}

	}
}

