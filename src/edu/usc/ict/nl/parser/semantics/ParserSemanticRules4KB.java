package edu.usc.ict.nl.parser.semantics;

import java.util.Collection;
import java.util.LinkedList;

import edu.usc.ict.nl.dm.reward.model.DialogueOperatorEffect;
import edu.usc.ict.nl.kb.DialogueKBFormula;

public class ParserSemanticRules4KB extends ParserSemanticRulesTimeAndNumbers {
	public Object makeImplication(Object[] args) throws Exception {
		if (args!=null) {
			if ((args.length==2) && (args[0] instanceof DialogueKBFormula) && (args[1] instanceof DialogueOperatorEffect)) {
				try {
					return DialogueOperatorEffect.createImplication((DialogueKBFormula)args[0], (DialogueOperatorEffect)args[1]);
				} catch (Exception e) {
					return null;
				}
			} else if ((args.length==3) && (args[0] instanceof DialogueKBFormula) &&
					(args[1] instanceof DialogueOperatorEffect) &&
					(args[2] instanceof DialogueOperatorEffect)) {
				try {
					return DialogueOperatorEffect.createImplication((DialogueKBFormula)args[0], (DialogueOperatorEffect)args[1], (DialogueOperatorEffect)args[2]);
				} catch (Exception e) {
					return null;
				}
			}
		}
		return null;
	}
	public Object makeAssignment(Object[] args) throws Exception {
		if ((args!=null) && (args.length==2) && (args[0] instanceof DialogueKBFormula) && (args[1] instanceof DialogueKBFormula)) {
			try {
				return DialogueOperatorEffect.createAssignment((DialogueKBFormula)args[0], (DialogueKBFormula)args[1]);
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}
	public Object makeIncrement(Object[] args) throws Exception {
		if ((args!=null) && (args.length==2) && (args[0] instanceof String) && ((args[1] instanceof String) || (args[1] instanceof DialogueKBFormula))) {
			try {
				String var=(String)args[0];
				DialogueKBFormula inc=null;
				if (args[1] instanceof String) {
					inc=DialogueKBFormula.create((String) args[1], null);
				} else if (args[1] instanceof DialogueKBFormula) {
					inc=(DialogueKBFormula) args[1];
				}
				return DialogueOperatorEffect.createIncrementForVariable(var, inc);
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}
	public Object makeAssertion(Object[] args) throws Exception {
		if ((args!=null) && (args.length==1) && (args[0] instanceof DialogueKBFormula)) {
			try {
				return DialogueOperatorEffect.createAssertion((DialogueKBFormula)args[0]);
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}
	public Object makeFormula(Object[] args) throws Exception {
		try {
			return DialogueKBFormula.create((String)args[0],(args.length>1)?(Collection)args[1]:null);
		} catch (Exception e) {
			return null;
		}
	}
	public Object makeArglist(Object[] args) throws Exception {
		//empty
		//one formula
		//one formula and a list
		if ((args==null) || (args.length==0)) return new LinkedList<DialogueKBFormula>();
		else if ((args.length==1) && (args[0] instanceof DialogueKBFormula)) {
			LinkedList<DialogueKBFormula> ret=new LinkedList<DialogueKBFormula>();
			ret.add((DialogueKBFormula) args[0]);
			return ret;
		} else if ((args.length==2) && (args[0] instanceof DialogueKBFormula) && (args[1] instanceof LinkedList)) {
			LinkedList ret=new LinkedList<DialogueKBFormula>((LinkedList) args[1]);
			ret.addFirst((DialogueKBFormula) args[0]);
			return ret;
		} else return null;
	}
}
