package edu.usc.ict.nl.nlu.preprocessing.ne;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.ne.NamedEntityExtractorI;
import edu.usc.ict.nl.nlu.preprocessing.PreprocesserI;

public class Generalize implements PreprocesserI {

	@Override
	public void run(List<List<Token>> input) {
		// TODO Auto-generated method stub
	}
	
	public List<NamedEntityExtractorI> getNamedEntityExtractors() {
		return getConfiguration().getNluNamedEntityExtractors();
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
	public List<List<Token>> generalize(List<Token> tokens) {
		List<NamedEntityExtractorI> nes = getNamedEntityExtractors();
		
		Map<Token,Set<Token>> overlappingTokens=null; // for a given token, returns the set of other tokens that overlap with it.
		for(NamedEntityExtractorI ne:nes) {
			List<Token> modified=ne.getModifiedTokens(tokens);
			if (modified!=null) {
				if (overlappingTokens==null || overlappingTokens.isEmpty()) {
					if (overlappingTokens==null) overlappingTokens=new HashMap<>();
					for(Token m:modified) overlappingTokens.put(m,null);
				} else {
					for(Token m:modified) {
						updateOverlappingTokens(m,overlappingTokens);
					}
				}
			}
		}
		
		List<Token> sortedModifiedTokens=new ArrayList<>(overlappingTokens.keySet());
		Collections.sort(sortedModifiedTokens);
		List<List<Token>> sols=new ArrayList<>();
		getNESoptions(sortedModifiedTokens.get(0),sortedModifiedTokens,null,sols,overlappingTokens);
		return sols;
		
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
		boolean usedCurrentSolution=false;
		for(Token ot:ots) {
			if (usedCurrentSolution || sol==null) {
				if (sol==null) sol=new ArrayList<>();
				else sol=new ArrayList<>(sol);
				sols.add(sol);
			}
			usedCurrentSolution=true;
			sol.add(ot);
			Token nextToken=getNonOverlappingAhead(ot,sortedTokens);
			getNESoptions(nextToken,sortedTokens,sol,sols,overlappingTokens);
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
		//adds to be added as a key of overlappingTokens
		//gets the set of tokens (the keys of overlappingtokens
		// for t in tokens
		//  if t overlaps with tobeadded,
		//    add tobeadded to the value of t
		//    add t to the value of tobeadded
		if (!overlappingTokens.containsKey(tobeadded)) {
			overlappingTokens.put(tobeadded, null);
			for(Token t:overlappingTokens.keySet()) {
				if (!(t==tobeadded)) {
					if (t.overlaps(tobeadded)) {
						addOverlapping(t,tobeadded,overlappingTokens);
						addOverlapping(tobeadded,t,overlappingTokens);
					}
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
