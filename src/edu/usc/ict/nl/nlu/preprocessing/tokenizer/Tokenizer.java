package edu.usc.ict.nl.nlu.preprocessing.tokenizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.Token.TokenTypes;
import edu.usc.ict.nl.nlu.ne.NE;
import edu.usc.ict.nl.nlu.preprocessing.TokenizerI;
import edu.usc.ict.nl.util.StringUtils;

public class Tokenizer implements TokenizerI {

	protected static LinkedHashMap<TokenTypes, Pattern> defaultTokenTypes=new LinkedHashMap<TokenTypes, Pattern>(){
		private static final long serialVersionUID = 1L;
		{
			put(TokenTypes.NUM, Pattern.compile("([0-9]*\\.[\\d]+)|([\\d]+)|(<"+TokenTypes.NUM.toString()+">)|(<"+TokenTypes.NUM.toString().toLowerCase()+">)"));
			put(TokenTypes.WORD, Pattern.compile("[\\d]*[a-zA-Z]+[\\d]*[a-zA-Z]*"));
			put(TokenTypes.OTHER,Pattern.compile("[^\\w\\s]+"));
		}
	};
	
	@Override
	public List<List<Token>> tokenize(String text) {
		List<List<Token>> ret=new ArrayList<>();
		ret.add(tokenize(text, defaultTokenTypes));
		return ret;
	}
	@Override
	public List<Token> tokenize1(String text) {
		return tokenize(text, defaultTokenTypes);
	}

	
	public static List<Token> tokenize(String u,LinkedHashMap<TokenTypes,Pattern> types) {
		ArrayList<Token> ret= new ArrayList<Token>();
		u=StringUtils.cleanupSpaces(u);
		LinkedHashMap<TokenTypes,Matcher> matchers=new LinkedHashMap<TokenTypes,Matcher>();
		for(Entry<TokenTypes, Pattern> tp:types.entrySet())	matchers.put(tp.getKey(),tp.getValue().matcher(u));
		int currentPos=0,length=u.length();		
		while(currentPos<length) {
			int start=length,end=0;
			TokenTypes type=null;
			for(Entry<TokenTypes, Matcher> mt:matchers.entrySet()) {
				Matcher m=mt.getValue();
				if (m.find(currentPos)) {
					if (m.start()<start) {
						start=m.start();
						end=m.end();
						type=mt.getKey();
					}
				}
			}
			if ((start<length) && (type!=null)) {
				Matcher m=matchers.get(type);
				String token=u.substring(m.start(),m.end());
				if ((token=token.replaceAll("[\\s]","")).length()>0) {
					ret.add(new Token(token, type,token,start,end));
				}
				currentPos=m.end();
			} else {
				break;
			}
		}
		return ret;
	}

	@Override
	public String untokenize(List<Token> tokens,String sa) {
		StringBuffer ret=null;
		if (tokens!=null) {
			boolean first=true;
			for (Token m:tokens) {
				if (ret==null) ret=new StringBuffer();

				if (first) first=false;
				else ret.append(" ");
				
				String thingToAppend=m.getName();
				NE ne=m.getAssociatedNamedEntity();
				if (ne!=null) {
					boolean leave = (sa!=null)?ne.getExtractor().isNEAvailableForSpeechAct(ne, sa):true;
					if (leave) thingToAppend=m.getName();
					else thingToAppend=ne.getMatchedString();
				}
				ret.append(thingToAppend);
			}
		}
		return (ret!=null)?ret.toString():null;
	}
	
	@Override
	public String tokAnduntok(String input) {
		return untokenize(tokenize1(input),null);
	}
}
