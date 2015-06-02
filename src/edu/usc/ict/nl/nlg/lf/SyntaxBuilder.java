package edu.usc.ict.nl.nlg.lf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Stack;

import edu.usc.ict.nl.nlg.lf.pos.DT;
import edu.usc.ict.nl.nlg.lf.pos.NP;
import edu.usc.ict.nl.nlg.lf.pos.POS;
import edu.usc.ict.nl.nlg.lf.pos.PP;
import edu.usc.ict.nl.nlg.lf.pos.Sentence;
import edu.usc.ict.nl.nlg.lf.pos.VP;

public class SyntaxBuilder {
	public enum TYPE {MK_SENTENCE,MK_NP,MK_VP,MK_PP,SUBJECT_OF,VP_OF,VERB_OF,INFINITIVE_OF,OBJECT_OF,THIS,CNST,NEGATED,TYPE_OF,REFERENCES,LENGTH,FIRST};
	TYPE operation;
	String cnst;
	List<SyntaxBuilder> args;
	public SyntaxBuilder(String fname,List<SyntaxBuilder> args) throws Exception {
		if (fname.equalsIgnoreCase("sentence")) this.operation=TYPE.MK_SENTENCE;
		else if (fname.equalsIgnoreCase("subject")) this.operation=TYPE.SUBJECT_OF;
		else if (fname.equalsIgnoreCase("object")) this.operation=TYPE.OBJECT_OF;
		else if (fname.equalsIgnoreCase("v")) this.operation=TYPE.VP_OF;
		else if (fname.equalsIgnoreCase("infinitive")) this.operation=TYPE.INFINITIVE_OF;
		else if (fname.equalsIgnoreCase("np")) this.operation=TYPE.MK_NP;
		else if (fname.equalsIgnoreCase("vp")) this.operation=TYPE.MK_VP;
		else if (fname.equalsIgnoreCase("pp")) this.operation=TYPE.MK_PP;
		else if (fname.equalsIgnoreCase(".")) this.operation=TYPE.THIS;
		else if (fname.equalsIgnoreCase("verb")) this.operation=TYPE.VERB_OF;
		else if (fname.equalsIgnoreCase("negated")) this.operation=TYPE.NEGATED;
		else if (fname.equalsIgnoreCase("type")) this.operation=TYPE.TYPE_OF;
		else if (fname.equalsIgnoreCase("refs")) this.operation=TYPE.REFERENCES;
		else if (fname.equalsIgnoreCase("len")) this.operation=TYPE.LENGTH;
		else if (fname.equalsIgnoreCase("first")) this.operation=TYPE.FIRST;
		else {
			if (args==null) {
				this.operation=TYPE.CNST;
				this.cnst=fname;
			}
			else throw new Exception("unknown operation: "+fname);
		}
		this.args=args;
	}
	
	public Object generateSyntax(POS p,Deque<POS> references) {
		POS ret=null;
		switch (operation) {
		case CNST:
			return cnst;
		case MK_SENTENCE:
			assert(args!=null && args.size()==2); //subject and vp
			ret=new Sentence();
			SyntaxBuilder subjectArg=args.get(0);
			if (subjectArg!=null) { 
				POS subject=(POS)subjectArg.generateSyntax(p,references);
				((Sentence)ret).addSubject(subject);
			}
			SyntaxBuilder vpArg=args.get(1);
			if (vpArg!=null) { 
				POS vp=(POS)vpArg.generateSyntax(p,references);
				((Sentence)ret).addVerbPhrase((VP) vp);
			}
			break;
		case SUBJECT_OF:
			assert(args!=null && args.size()==1); //the thing from which to extract the subject
			SyntaxBuilder arg=args.get(0);
			if (arg!=null) { 
				POS thing=(POS)arg.generateSyntax(p,references);
				if (thing instanceof Sentence) return ((Sentence)thing).subject;
			}
			break;
		case OBJECT_OF:
			assert(args!=null && args.size()==1); //the thing from which to extract the object
			arg=args.get(0);
			if (arg!=null) { 
				POS thing=(POS)arg.generateSyntax(p,references);
				if (thing instanceof Sentence && ((Sentence) thing).verbPhrase instanceof VP) return ((VP)((Sentence) thing).verbPhrase).getObject();
				else if (thing instanceof VP) return ((VP) thing).getObject();
			}
			break;
		case THIS:
			return p;
		case MK_NP:
			assert(args!=null && args.size()==1); //the String to become the subject 
			SyntaxBuilder npArg=args.get(0);
			if (npArg!=null) { 
				Object thing=npArg.generateSyntax(p,references);
				if (thing!=null) {
					if (thing instanceof NP) return thing;
					else if (thing instanceof String) {
						NP r=new NP((String) thing);
						r.determiner=DT.NULL;
						return r;
					}
					else System.err.println("asking a "+thing.getClass().getName()+" to be returned as a NP.");
				} 
			}
			break;
		case MK_VP:
			assert(args!=null && args.size()>=1); //the vrb plus its complements
			ret=new VP();
			if (p!=null) {
				if (p instanceof Sentence) {
					POS vp = ((Sentence) p).verbPhrase;
					if (vp instanceof VP) {
						ret=vp.clone();
					}
				} else if (p instanceof VP) {
					ret=p.clone();
				}
			}
			((VP)ret).arguments.clear();
			SyntaxBuilder verbArg=args.get(0);
			if (verbArg!=null) { 
				Object verb=verbArg.generateSyntax(p,references);
				((VP)ret).verb=verb.toString();
			}
			for(int i=1;i<args.size();i++) {
				SyntaxBuilder aArg=args.get(i);
				if (aArg!=null) { 
					POS a=(POS)aArg.generateSyntax(p,references);
					((VP)ret).addArgument(a);
				}
			}
			break;
		case MK_PP:
			assert(args!=null && args.size()==2); //the preposition plus its argument 
			ret=new PP();
			SyntaxBuilder prepositionArg=args.get(0);
			if (prepositionArg!=null) { 
				Object preposition=prepositionArg.generateSyntax(p,references);
				((PP)ret).setPreposition(preposition.toString());
			}
			SyntaxBuilder complementArg=args.get(1);
			if (complementArg!=null) { 
				POS complement=(POS)complementArg.generateSyntax(p,references);
				((PP)ret).setComplement(complement);
			}
			break;
		case INFINITIVE_OF:
			assert(args!=null && args.size()==1); //the thing (VP) to be made in infinitive form
			arg=args.get(0);
			if (arg!=null) { 
				POS thing=(POS)arg.generateSyntax(p,references);
				if (thing instanceof VP) {
					VP newp;
					newp = ((VP)thing).clone();
					newp.setInfinitive(true);
					return newp;
				}
			}
			break;
		case NEGATED:
			assert(args!=null && args.size()==1); //the thing (VP) to be made in infinitive form
			arg=args.get(0);
			if (arg!=null) { 
				POS thing=(POS)arg.generateSyntax(p,references);
				if (thing instanceof VP) {
					VP newp;
					newp = ((VP)thing).clone();
					newp.setNegated(!((VP)thing).isNegated());
					return newp;
				}
			}
			break;
		case VP_OF:
			assert(args!=null && args.size()==1); //the sentence from which to get the VP
			arg=args.get(0);
			if (arg!=null) { 
				POS thing=(POS)arg.generateSyntax(p,references);
				if (thing instanceof Sentence) return ((Sentence) thing).verbPhrase;
			}
			break;
		case VERB_OF:
			assert(args!=null && args.size()==1); //the VP from which to get the verb
			arg=args.get(0);
			if (arg!=null) { 
				POS thing=(POS)arg.generateSyntax(p,references);
				if (thing instanceof VP) return ((VP) thing).verb;
			}
			break;
		case TYPE_OF:
			return p.getClass().getName().toLowerCase();
		case LENGTH:
			assert(args!=null && args.size()==1); //the thing to calculate the length
			arg=args.get(0);
			if (arg!=null) { 
				Object thing=arg.generateSyntax(p,references);
				if (thing instanceof Collection) {
					return ((Collection)thing).size();
				}
			}
			break;
		case REFERENCES:
			return references;
		case FIRST:
			assert(args!=null && args.size()==1); //the thing to get the first
			arg=args.get(0);
			if (arg!=null) { 
				Object thing=arg.generateSyntax(p,references);
				if (thing instanceof Collection && !((Collection) thing).isEmpty()) {
					return ((Collection)thing).iterator().next();
				}
			}
			return null;
		default:
			System.err.println(operation+" not implemented.");
			break;
		}
		return ret;
	}
	
	@Override
	public String toString() {
		if (operation==TYPE.CNST) return cnst;
		else return operation+"("+args+")";
	}
	
	public static void main(String[] args) throws Exception {
		SyntaxBuilder sb = new SyntaxBuilder("type", null);
		POS noun=new NP("test");
		Object z = sb.generateSyntax(noun,null);
		System.out.println(z);
		sb = new SyntaxBuilder("lf.pos.np", null);
		System.out.println(sb.generateSyntax(null,null));
	}
}
