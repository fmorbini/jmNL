package edu.usc.ict.nl.nlg.template;

import edu.usc.ict.nl.config.NLGConfig;
import edu.usc.ict.nl.util.StringUtils;
import simplenlg.features.Feature;
import simplenlg.features.Form;
import simplenlg.framework.DocumentElement;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.phrasespec.VPPhraseSpec;
import simplenlg.realiser.english.Realiser;

public class SimpleNLGTemplateNLG extends TemplatedNLG {

	private Lexicon lexicon;
	private NLGFactory nlgFactory;
	private Realiser realiser;

	public SimpleNLGTemplateNLG(NLGConfig c) {
		this(c,true);
	}
	public SimpleNLGTemplateNLG(NLGConfig c,boolean loadData) {
		super(c,loadData);
		lexicon = Lexicon.getDefaultLexicon();
		nlgFactory = new NLGFactory(lexicon);
		realiser = new Realiser(lexicon);
	}

	public Object functionBuildSentence(FunctionArguments args) {
		String sep=args.stringArg.substring(0, 1);
		String[] parts=args.stringArg.split(sep);
		if (parts==null || parts.length<3) return null;
		else {
			String subject=parts[1];
			String verb=parts[2];
			NLGElement c=null;
			if (!StringUtils.isEmptyString(subject)) {
				c = nlgFactory.createClause();
				if (!StringUtils.isEmptyString(subject)) {
					((SPhraseSpec)c).setSubject(subject);
				}
				((SPhraseSpec)c).setVerb(verb);
				for(int i=3;i<parts.length;i++) {
					if (!StringUtils.isEmptyString(parts[i])) {
						((SPhraseSpec)c).addComplement(parts[i]);
					}
				}
			} else {
				c=nlgFactory.createVerbPhrase(verb);
				((VPPhraseSpec)c).setFeature(Feature.FORM,Form.IMPERATIVE);
				for(int i=3;i<parts.length;i++) {
					if (!StringUtils.isEmptyString(parts[i])) {
						((VPPhraseSpec)c).addComplement(parts[i]);
					}
				}
			}
			if (c!=null) {
				try {
					DocumentElement output=nlgFactory.createParagraph(c);
					return realiser.realise(output).getRealisation();
				} catch (Exception e) {}
			}
			return null;
		}
	}

}
