package edu.usc.ict.nl.nlg.template;

import simplenlg.framework.DocumentElement;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.realiser.english.Realiser;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.vhmsg.VHBridge;

public class SimpleNLGTemplateNLG extends TemplatedNLG {

	private Lexicon lexicon;
	private NLGFactory nlgFactory;
	private Realiser realiser;

	public SimpleNLGTemplateNLG(NLBusConfig c) {
		super(c);
		lexicon = Lexicon.getDefaultLexicon();
		nlgFactory = new NLGFactory(lexicon);
		realiser = new Realiser(lexicon);
	}

	public Object functionBuildSentence(DialogueKBInterface is,VHBridge vhBridge,String args,boolean simulate) {
		String sep=args.substring(0, 1);
		String[] parts=args.split(sep);
		if (parts==null || parts.length<3) return null;
		else {
			String subject=parts[1];
			String verb=parts[2];
			SPhraseSpec c = nlgFactory.createClause();
			c.setSubject(subject);
			c.setVerb(verb);
			for(int i=3;i<parts.length;i++) {
				c.addComplement(parts[i]);
			}
			DocumentElement output=nlgFactory.createParagraph(c);
			return realiser.realise(output).getRealisation();
		}
	}

}
