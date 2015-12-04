package edu.usc.ict.nl.nlu.preprocessing.normalization;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.util.EnglishUtils;
import edu.usc.ict.nl.util.StringUtils;

public class UK2USNormalizer extends Normalizer {

	@Override
	public String normalize(String word) {
		if (!StringUtils.isEmptyString(word)) {
			String ret=EnglishUtils.getUSspellingFor(word);
			return ret;
		}
		return null;
	}

}
