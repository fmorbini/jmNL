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

	private static final LinkedHashMap<TokenTypes, Pattern> tokenTypes=new LinkedHashMap<TokenTypes, Pattern>(){
		private static final long serialVersionUID = 1L;
		{
			put(TokenTypes.NUM, Pattern.compile("([0-9]*\\.[\\d]+)|([\\d]+)|(<"+TokenTypes.NUM.toString()+">)|(<"+TokenTypes.NUM.toString().toLowerCase()+">)"));
			put(TokenTypes.WORD, Pattern.compile("[\\d]*[a-zA-Z]+[\\d]*[a-zA-Z]*"));
			put(TokenTypes.OTHER,Pattern.compile("[^\\w\\s]+"));
		}
	};
	
	@Override
	public java.util.LinkedHashMap<TokenTypes,Pattern> getTokenTypes() {
		return tokenTypes;
	}
	
	@Override
	public List<List<Token>> tokenize(String text) {
		List<List<Token>> ret=new ArrayList<>();
		ret.add(tokenize(text, getTokenTypes()));
		return ret;
	}
	@Override
	public List<Token> tokenize1(String text) {
		return tokenize(text, getTokenTypes());
	}

	
	public static List<Token> tokenize(String u,LinkedHashMap<TokenTypes,Pattern> types) {
		ArrayList<Token> ret= new ArrayList<Token>();
		u=StringUtils.cleanupSpaces(u);
		if (!StringUtils.isEmptyString(u)) {
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
		}
		return ret;
	}

	@Override
	public String untokenize(List<Token> tokens,String sa) {
		if (tokens!=null) {
			return untokenize(tokens, sa, tokens.size());
		}
		return null;
	}
	public String untokenize(List<Token> tokens,String sa,int stop) {
		StringBuffer ret=null;
		if (tokens!=null) {
			int l=Math.min(stop,tokens.size());
			for(int i=0;i<l;i++) {
				ret=doUntokenizeStep(ret, tokens, sa, i);
			}
		}
		return (ret!=null)?ret.toString():null;
	}
	/**
	 * appends to the stringbuffer ret the string representation of token at position pos. takes care of spacing accordinghly to the tokenizer. 
	 * @param ret
	 * @param tokens
	 * @param sa
	 * @param pos
	 * @return
	 */
	public StringBuffer doUntokenizeStep(StringBuffer ret,List<Token> tokens,String sa,int pos) {
		if (tokens!=null) {
			if (ret==null) ret=new StringBuffer();
			boolean first=((ret==null) || (ret.length()<=0));
			if (!first) ret.append(" ");
			String s=toString(tokens, sa, pos);
			ret.append(s);
		}
		return ret;
	}
	
	/**
	 * generate the string representation for token at position pos.
	 * @param tokens
	 * @param sa
	 * @param pos
	 * @return
	 */
	private String toString(List<Token> tokens,String sa,int pos) {
		if (tokens!=null && pos<tokens.size()) {
			Token m=tokens.get(pos);
			
			String thingToAppend=m.getName();
			NE ne=m.getAssociatedNamedEntity();
			if (ne!=null) {
				boolean leave = (sa!=null)?ne.getExtractor().isNEAvailableForSpeechAct(ne, sa):true;
				if (leave) thingToAppend=m.getName();
				else thingToAppend=ne.getMatchedString();
			}
			return thingToAppend;
		}
		return null;
	}
	
	@Override
	public String tokAnduntok(String input) {
		return untokenize(tokenize1(input),null);
	}

	@Override
	public void updateStartsAndEnds(List<Token> tokens,String sa) {
		StringBuffer ret=null;
		if (tokens!=null) {
			int l=tokens.size();
			for(int i=0;i<l;i++) {
				Token t=tokens.get(i);
				ret=doUntokenizeStep(ret, tokens, sa, i);
				String tx=toString(tokens, sa, i);
				int tl=tx.length();
				int end=ret.length();
				int start=end-tl;
				t.setStart(start);
				t.setEnd(end);
			}
		}
	}
	
	public static void main(String[] args) {
		
		Tokenizer t=new Tokenizer();
		List<Token> test = t.tokenize1(null);
		System.out.println(test);
		
	}
}
