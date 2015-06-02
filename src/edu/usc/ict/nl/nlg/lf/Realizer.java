package edu.usc.ict.nl.nlg.lf;

import java.util.ArrayList;
import java.util.List;

import edu.usc.ict.nl.nlg.lf.pos.POS;
import simplenlg.framework.DocumentElement;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.realiser.english.Realiser;

/**
 * uses simple nlg as a realizer.
 * @author morbini
 *
 */
public class Realizer {
	private Realiser realiser;
	private NLGFactory nlgFactory;
	private Lexicon lexicon;

	public Realizer() {
		lexicon = Lexicon.getDefaultLexicon();
		nlgFactory = new NLGFactory(lexicon);
		realiser = new Realiser(lexicon);
	}
	
	public String process(List<POS> sentences) {
		List<DocumentElement> ss=realize(sentences);
		DocumentElement output=nlgFactory.createParagraph(ss);
		return realiser.realise(output).getRealisation();
	}

	private List<DocumentElement> realize(List<POS> sentences) {
		List<DocumentElement> ret=null;
		for(POS s:sentences) {
			NLGElement e = s.toSimpleNLG(nlgFactory);
			if (ret==null) ret=new ArrayList<DocumentElement>();
			ret.add(nlgFactory.createSentence(e));
		}
		return ret;
	}
}
