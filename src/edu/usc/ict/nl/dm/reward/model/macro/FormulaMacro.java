package edu.usc.ict.nl.dm.reward.model.macro;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.usc.ict.nl.kb.DialogueKBFormula;

public class FormulaMacro extends Macro {
	private DialogueKBFormula formulaToSubstitute;
	private DialogueKBFormula substitution;

	public FormulaMacro(DialogueKBFormula leftf, DialogueKBFormula rightf) {
		this.name=leftf.getName();
		this.formulaToSubstitute=leftf;
		this.substitution=rightf;
	}

	/**
	 * generates a variant of substitution using the argument names in f and their mappings to formulaToSubstitute.
	 * @param f
	 * @return
	 * @throws Exception 
	 */
	public DialogueKBFormula generateSubstituteFormula(DialogueKBFormula f) throws Exception {
		Map<DialogueKBFormula,DialogueKBFormula> argMap=getArgumentsMapping(formulaToSubstitute,f);
		return applySubstitutions(substitution,argMap);
	}

	private DialogueKBFormula applySubstitutions(DialogueKBFormula f, Map<DialogueKBFormula, DialogueKBFormula> argMap) throws Exception {
		if (f.hasChildren()) {
			List<DialogueKBFormula> args=applySubstitutionsToArgs(f.getAllArgs(),argMap);
			return DialogueKBFormula.create(f.getName(), args, f.getType());
		} else if (argMap.containsKey(f)) {
			return argMap.get(f);
		} else return f;
	}

	private List<DialogueKBFormula> applySubstitutionsToArgs(List<DialogueKBFormula> args,Map<DialogueKBFormula, DialogueKBFormula> argMap) throws Exception {
		List<DialogueKBFormula> ret=args;
		if (args!=null && !args.isEmpty() && argMap!=null && !argMap.isEmpty()) {
			boolean changed=false;
			int l=args.size();
			for(int i=0;i<l;i++) {
				DialogueKBFormula a=args.get(i);
				DialogueKBFormula s=applySubstitutions(a,argMap);
				if (a!=s) {
					if (!changed) {
						ret=new ArrayList<>(ret.subList(0, i));
						changed=true;
					}
					ret.add(s);
				} else {
					if (changed) ret.add(a);
				}
			}
		}
		return ret;
	}

	private Map<DialogueKBFormula, DialogueKBFormula> getArgumentsMapping(DialogueKBFormula source, DialogueKBFormula target) throws Exception {
		Map<DialogueKBFormula, DialogueKBFormula> ret=null;
		if (source!=null && target!=null) {
			if (source.getName().equals(target.getName())) {
				List<DialogueKBFormula> sourceArgs = source.getAllArgs();
				List<DialogueKBFormula> targetArgs = target.getAllArgs();
				if ((sourceArgs==null || sourceArgs.isEmpty()) && (targetArgs==null || targetArgs.isEmpty())) return null;
				else if (sourceArgs!=null && targetArgs!=null && sourceArgs.size()==targetArgs.size()) {
					int l=sourceArgs.size();
					for(int i=0;i<l;i++) {
						if (ret==null) ret=new HashMap<>();
						ret.put(sourceArgs.get(i), targetArgs.get(i));
					}
				} else throw new Exception("invalid use. source and target args match.");
			} else throw new Exception("invalid use. source and target don't match.");
		}
		return ret;
	}

	public int getArgCount() {
		if (formulaToSubstitute!=null) {
			return formulaToSubstitute.getArgCount();
		}
		return -1;
	}

}
