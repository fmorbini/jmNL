package edu.usc.ict.nl.nlu.preprocessing.ne;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.ne.BasicNE;
import edu.usc.ict.nl.nlu.ne.NamedEntityExtractorI;
import edu.usc.ict.nl.nlu.preprocessing.Preprocess;
import edu.usc.ict.nl.nlu.preprocessing.Preprocesser;
import edu.usc.ict.nl.nlu.preprocessing.TokenizerI;
import edu.usc.ict.nl.util.StringUtils;

public class Generalize extends Preprocesser {

	@Override
	public void run(List<List<Token>> input,PreprocessingType type) {
		if (input!=null) {
			int position=0;
			while(position<input.size()) {
				int size=1;
				List<Token> tokens=input.get(position);
				List<List<Token>> tmp = generalize(tokens,type);
				if (tmp!=null && !tmp.isEmpty()) {
					size=tmp.size();
					input.remove(position);
					for(int i=0;i<tmp.size();i++) {
						input.add(position,tmp.get(i));
					}
				}
				position+=size;
			}
		}
	}
	
	/**
	 * original list of tokens (one token for each word)
	 * 
	 * for each NE the list of tokens introduced
	 * 
	 * for each token get the list of tokens overlapping with it
	 * 
	 * for each token get list of tokens not overlapping with it (maybe only those that come after it)
	 * @return 
	 *
	 */
	public List<List<Token>> generalize(List<Token> tokens,PreprocessingType type) {
		if (tokens.size()==6 && tokens.get(0).getName().equals("2.0")) {
			System.out.println(tokens);
		}
		List<NamedEntityExtractorI> nes = getConfiguration(type).getNluNamedEntityExtractors();
		
		Map<Token,Set<Token>> overlappingTokens=null; // for a given token, returns the set of other tokens that overlap with it.
		for(NamedEntityExtractorI ne:nes) {
			List<Token> modified=ne.getModifiedTokens(tokens,type);
			if (modified!=null) {
				if (overlappingTokens==null) overlappingTokens=new HashMap<>();
				for(Token m:modified) updateOverlappingTokens(m,overlappingTokens);
			}
		}
		
		if (overlappingTokens!=null && !overlappingTokens.isEmpty()) {
			List<Token> sortedModifiedTokens=new ArrayList<>(overlappingTokens.keySet());
			Collections.sort(sortedModifiedTokens);
			List<List<Token>> sols=new ArrayList<>();
			getNESoptions(sortedModifiedTokens.get(0),sortedModifiedTokens,null,sols,overlappingTokens);
			List<List<Token>> newTokens=mergeTokensWithGeneralizedTokens(sols,tokens,type);
			return newTokens;
		}
		return null;
	}

	private List<List<Token>> mergeTokensWithGeneralizedTokens(List<List<Token>> generalizedTokens, List<Token> tokens, PreprocessingType type) {
		List<Integer> tokenStarts=BasicNE.computeTokenStarts(tokens);
		TokenizerI tokenizer = getConfiguration(type).getNluTokenizer();
		String input=Preprocess.getString(tokens, tokenizer);
		try {
			if (generalizedTokens!=null) {
				for(List<Token> gt:generalizedTokens) {
					int position=0;
					int pend=0;
					while(position<gt.size()) {
						Token current=gt.get(position);
						int start=current.getStart();
						int end=current.getEnd();
						boolean isWholeWordsSubstring=StringUtils.isWholeWordSubstring(start,end,input);
						if (isWholeWordsSubstring) {
							int startToken = BasicNE.getTokenAtPosition(start,tokenStarts);
							int endToken=BasicNE.getTokenAtPosition(end,tokenStarts);
							for (int j=pend;j<startToken;j++) {
								gt.add(position+j-pend, tokens.get(j));
							}
							position+=startToken-pend;
							pend=endToken+1;
						}
						position++;
					}
					for (int j=pend;j<tokens.size();j++) {
						gt.add(tokens.get(j));
					}
				}
			}
		} catch (Exception e) {
			logger.error("error generalizing text:",e);
		}
		return generalizedTokens;
	}

	/** input: t initial token (first generalized token (by start position)) 
	 *         S=current solution (null initially)
	 *         SOLS=list of solutions
	 *         
	 *  ots=overlapping tokens(t) + t
	 *  boolean usedCurrentSolution=false;
	 *  for each ot in ots
	 *    if (usedCurrentSolution || S==null)
	 *     if S==null S=new list
	 *     else S=new list(S)
	 *     SOLS.add(S)
	 *     userCurrentSolution=true
	 *    S.add(ot)
	 *    nextTs=getNonOverlappingAhead(t)
	 *    recursiveCall(nextTs,S,SOLS)
	 * @param overlappingTokens 
	 * 
	 */
	private void getNESoptions(Token current, List<Token> sortedTokens,List<Token> sol, List<List<Token>> sols, Map<Token, Set<Token>> overlappingTokens) {
		Set<Token> ots = overlappingTokens.get(current);
		
		if (ots!=null && ots.size()>0) {
			boolean usedCurrentSolution=false;
			/**
			 * combinations to think:
			 * ots size:
			 *  null/0
			 *  1
			 *  more
			 * sol:
			 *  null
			 *  not null
			 */
			List<Token> savedSolution=sol;
			if (ots.size()>1) savedSolution=(sol!=null && !sol.isEmpty())?new ArrayList<>(sol):null;
			for(Token ot:ots) {
				if (usedCurrentSolution) {
					sol=savedSolution!=null?new ArrayList<>(savedSolution):new ArrayList<>();
					sols.add(sol);
				} else {
					if (sol==null) {
						sol=new ArrayList<>();
						sols.add(sol);
					}
					usedCurrentSolution=true;
				}
				sol.add(ot);
				Token nextToken=getNonOverlappingAhead(ot,sortedTokens);
				getNESoptions(nextToken,sortedTokens,sol,sols,overlappingTokens);
			}
		}
	}
	
	private Token getNonOverlappingAhead(Token current, List<Token> sortedTokens) {
		for(Token next:sortedTokens) {
			if (next.getStart()>=current.getEnd()) return next;
		}
		return null;
	}
	
	/*
	public List<List<Token>> generalize(List<Token> tokens) {
		return List<List<Token>> options = computePossibleNESoptions(tokens);
		
		List<Token> ret=new ArrayList<Token>(tokens);
		
		
		
		boolean generalized=false;
		if (nes!=null) {
			for(NamedEntityExtractorI ne:nes) {
				boolean changed=ne.generalize(ret);
				if (changed) {
					
				}
				
				generalized|=ne.generalize(ret);
			}
		}
		TokenTypes type;
		if (!generalized && getConfiguration().getGeneralizeNumbers()) {
			for(int i=0;i<ret.size();i++) {
				Token t=ret.get(i);
				type=t.getType();
				if ((type==TokenTypes.NUM)) {
					ret.set(i,new Token("<"+TokenTypes.NUM.toString()+">", TokenTypes.NUM,t.getOriginal()));
				}
			}
		}
		return ret;
	}*/

	private void updateOverlappingTokens(Token tobeadded, Map<Token, Set<Token>> overlappingTokens) {
		updateOverlappingTokens(tobeadded, overlappingTokens,new HashSet<Token>());
	}
	/**
	 * properly updates the overlappingTokens parameter given the new token tobeadded.
	 * @param existing
	 * @param tobeadded
	 * @param overlappingTokens
	 * @param visited
	 */
	private void updateOverlappingTokens(Token tobeadded, Map<Token, Set<Token>> overlappingTokens,Set<Token> visited) {
		//if overlappingtokens already contains tobeadded, terminate doing nothing
		//adds tobeadded as a key of overlappingTokens
		//gets the set of tokens (the keys of overlappingtokens
		// for t in tokens
		//  if t overlaps with tobeadded,
		//    add tobeadded to the value of t
		//    add t to the value of tobeadded
		if (!overlappingTokens.containsKey(tobeadded)) {
			overlappingTokens.put(tobeadded, null);
			for(Token t:overlappingTokens.keySet()) {
				if (t.overlaps(tobeadded)) {
					addOverlapping(t,tobeadded,overlappingTokens);
					addOverlapping(tobeadded,t,overlappingTokens);
				}
			}
		}
	}
	private void addOverlapping(Token key, Token toadd, Map<Token, Set<Token>> overlappingTokens) {
		Set<Token> things=overlappingTokens.get(key);
		if (things==null) overlappingTokens.put(key, things=new HashSet<>());
		things.add(toadd);
	}

}
