package edu.usc.ict.nl.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.Token.TokenTypes;
import edu.usc.ict.nl.nlu.io.BuildTrainingData;
import edu.usc.ict.nl.parser.ChartParser;
import edu.usc.ict.nl.parser.ChartParser.Item;
import edu.usc.ict.nl.util.LevenshteinDistance;
import edu.usc.ict.nl.util.Pair;

public class EnglishWrittenNumbers2Digits {
	
	
	public static void main(String[] args) throws Exception {
		// one hundred twenty five
		// fifty two
		// thirteen
		// for every word in input string, find the nearest
		String s="one hundred twenty five";
		s=s.toLowerCase();
		List<Token> tokens = BuildTrainingData.tokenize(s);
		tokens=parseWrittenNumbers(NLUConfig.WIN_EXE_CONFIG,tokens);
		System.out.println(tokens);
	}

	public static Pair<String,Integer> findBestMatch(String m,Collection<String> c) {
		int minD=Integer.MAX_VALUE;
		String ret=null;
		for(String pm:c) {
			int d=LevenshteinDistance.computeLevenshteinDistance(m,pm);
			if (d<minD) ret=pm;
		}
		return new Pair<String,Integer>(ret,minD);
	}

	public static List<Token> parseWrittenNumbers(NLUConfig nluConfig,List<Token> tokens) throws Exception {
		File grammar=new File(nluConfig.getNLUContentRoot(),"written-numbers-grammar.txt");
		if (nluConfig.nlBusConfig!=null && !grammar.exists()) grammar=new File(nluConfig.nlBusConfig.getContentRoot(),"common/nlu/written-numbers-grammar.txt");
		if (!grammar.exists()) return tokens;
		else {
			ChartParser parser=ChartParser.getParserForGrammar(grammar);
			ArrayList<String> input=new ArrayList<String>();
			for (Token t:tokens) {
				input.add(t.getName());
			}
			Collection<Item> result = parser.parseAndFilter(input,"<N>");
			return replaceItemsInTokens(tokens,result);
		}
	}
	
	private static List<Token> replaceItemsInTokens(List<Token> tokens, Collection<Item> numbers) {
		List<Token> out=new ArrayList<Token>();
		int lastInsertedToken=0;
		for(Item it:numbers) {
			for (int i=lastInsertedToken;i<it.getStart();i++) {
				out.add(tokens.get(i));
			}
			out.add(new Token(it.getSemantics().toString(), TokenTypes.NUM,BuildTrainingData.getStringOfTokensSpan(tokens, it.getStart(), it.getEnd()+1)));
			lastInsertedToken=it.getEnd();
		}
		for (int i=lastInsertedToken;i<tokens.size();i++) out.add(tokens.get(i));
		return out;
	}
}
