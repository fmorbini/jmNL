package edu.usc.ict.nl.nlu.ne;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.bus.special_variables.SpecialEntitiesRepository;
import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.preprocessing.Preprocess;
import edu.usc.ict.nl.nlu.preprocessing.TokenizerI;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.utils.LogConfig;

public abstract class BasicNE implements NamedEntityExtractorI {
	private NLUConfig configuration;
	protected Pattern[] sas=null;

	protected boolean generalize=true;

	private SpecialEntitiesRepository svs=null;
	
	public static final Logger logger = Logger.getLogger(NLU.class.getName());
	static {
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );
	}
	
	public BasicNE(String... sas) {
		if (sas!=null) {
			this.sas=new Pattern[sas.length];
			for (int i=0;i<sas.length;i++) {
				this.sas[i]=Pattern.compile(sas[i]);
			}
		}
	}
	
	protected NLUConfig getConfiguration() {
		return configuration;
	}
	@Override
	public void setConfiguration(NLUConfig configuration) {
		this.configuration = configuration;
	}
	
	@Override
	public List<SpecialVar> getSpecialVariables() throws Exception {
		if (svs!=null)  return svs.getAllSpecialVariables();
		return null;
	}
	protected void addSpecialVarToRepository(SpecialVar v) {
		if (svs==null) svs=new SpecialEntitiesRepository(null);
		svs.addSpecialVariable(v);
	}

	public static Double convertSecondsIn(Long seconds,Double conversion) {		
		if (seconds!=null) {
			return seconds/conversion;
		}
		return null;
	}

	public static List<Integer> computeTokenStarts(List<Token> inputTokens) {
		if (inputTokens==null) return null;
		else return inputTokens.stream().map(s->s.getStart()).collect(Collectors.toList());
	}
	
	@Override
	public List<Token> getModifiedTokens(List<Token> inputTokens,PreprocessingType type) {
		List<Token> ret=null;
		List<Integer> tokenStarts=computeTokenStarts(inputTokens);
		TokenizerI tokenizer = getConfiguration().getNluTokenizer(type);
		String input=Preprocess.getString(inputTokens, tokenizer);
		if (!StringUtils.isEmptyString(input)) {
			try {
				List<NE> nes = extractNamedEntitiesFromText(input,type);
				filterOverlappingNES(nes);
				if (nes!=null) {
					for(NE ne:nes) {
						int start=ne.getStart();
						int end=ne.getEnd();
						boolean isWholeWordsSubstring=StringUtils.isWholeWordSubstring(start,end,input);
						if (isWholeWordsSubstring) {
							int startToken = getTokenAtPosition(start,tokenStarts);
							int endToken=getTokenAtPosition(end,tokenStarts);
							boolean generalize=generalizeText();
							for(int j=startToken;j<=endToken;j++) {
								Token newToken=null;
								if (j==startToken) {
									Token original=inputTokens.get(j);
									if (original!=null) {
										if (generalize) { 
											newToken=new Token(ne.getType().toUpperCase(), original.getType(), ne.getMatchedString(), start, end);
										} else {
											newToken=new Token(original.getName(), original.getType(), original.getOriginal(), original.getStart(), original.getEnd());
										}
										newToken.setAssociatedNamedEntity(ne);
										if (ret==null) ret=new ArrayList<>();
										ret.add(newToken);
									} else {
										logger.error("Trying to generalize null NE ("+ne+"). NE list: "+nes);
									}
								}
							}
						}
					}
				}
			} catch (Exception e) {
				logger.error("error generalizing text", e);
			}
		}
		return ret;
	}
	
	@Override
	public boolean generalize(List<Token> inputTokens,PreprocessingType type) {
		boolean generalized=false;
		List<Token> mods = getModifiedTokens(inputTokens,type);
		if (mods!=null && !mods.isEmpty()) {
			applyGeneralizations(mods,inputTokens);
			generalized=true;
		}
		return generalized;
	}
	private void applyGeneralizations(List<Token> mods, List<Token> inputTokens) {
		if (mods!=null && inputTokens!=null) {
			List<Integer> tokenStarts = computeTokenStarts(inputTokens);
			for(Token m:mods) {
				int start=m.getStart();
				int end=m.getEnd();
				int startToken = getTokenAtPosition(start,tokenStarts);
				int endToken=getTokenAtPosition(end,tokenStarts);
				for(int j=startToken;j<=endToken;j++) {
					Token newToken=null;
					if (j==startToken) {
						Token original=inputTokens.get(j);
						if (original!=null) {
							newToken=m;
						} else {
							logger.error("Trying to apply generalization to null token. position: "+j);
						}
					}
					inputTokens.set(j, newToken);
				}
			}
			Iterator<Token> it=inputTokens.iterator();
			while(it.hasNext()) {
				Token t=it.next();
				if (t==null) it.remove();
			}
		}
	}
	
	/**
	 * processes the list from first to last. If a later NE overlaps an earlier NE it'll be discarded.
	 * The order of NE recognizers is important.
	 * @param nes
	 */
	private void filterOverlappingNES(List<NE> nes) {
		List<Interval> intervals=null;
		if (nes!=null) {
			Iterator<NE> it=nes.iterator();
			while(it.hasNext()) {
				NE ne=it.next();
				int start=ne.getStart();
				if (intervals!=null) {
					boolean inside=false;
					for(Interval i:intervals) if (i.inside(start)) {
						inside=true;
						break;
					}
					if (inside) {
						it.remove();
						continue;
					}
				}
				if (intervals==null) intervals=new ArrayList<Interval>();
				intervals.add(new Interval(start, ne.getEnd()));
			}
		}
	}
	
	public static int getTokenAtPosition(int chPos, List<Integer> tokenStarts) {
		int token=0;
		for(int tokenStartPos:tokenStarts) {
			if (chPos<tokenStartPos) return token-1;
			token++;
		}
		return token-1;
	}
	public static Map<String, Object> createPayload(List<NE> foundNEs) {
		Map<String,Object> ret=null;
		if (foundNEs!=null) {
			for(NE ne:foundNEs) {
				if (ne!=null) {
					String vname=ne.getVarName();
					if (ret==null) ret=new HashMap<String, Object>();
					Object content=ret.get(vname);
					if (content==null) ret.put(vname, ne.getValue());
					else if (content instanceof List) ((List) content).add(ne.getValue());
					else {
						Object oldElement=content;
						content=new ArrayList();
						ret.put(vname, content);
						((List) content).add(oldElement);
						((List)content).add(ne.getValue());
					}
				}
			}
		}
		return ret;
	}
	
	public static List<NE> filterNESwithSpeechAct(List<Token> option, String speechAct,TokenizerI tokenizer) {
		return filterNESwithSpeechAct(option, speechAct, tokenizer,null);
	}
	public static List<NE> filterNESwithSpeechAct(List<Token> option, String speechAct,TokenizerI tokenizer,List<Pair<Integer,Integer>> ranges) {
		List<NE> ret=null;
		if (!StringUtils.isEmptyString(speechAct)) {
			int i=0;
			for(Token t:option) {
				NE ne=t.getAssociatedNamedEntity();
				if (ne!=null) {
					int start=tokenizer.getStart(option, speechAct, i+1);
					int end=tokenizer.getEnd(option, speechAct, i+1);
					if (isNEinRange(start,end,ranges)) {
						NamedEntityExtractorI ext=ne.getExtractor();
						if (ext==null || ext.isNEAvailableForSpeechAct(ne, speechAct)) {
							if (ret==null) ret=new ArrayList<>();
							ret.add(ne);
						}
					}
				}
				i++;
			}
		}
		return ret;
	}
	
	private static boolean isNEinRange(NE ne, List<Pair<Integer, Integer>> ranges) {
		return isNEinRange(ne.getStart(), ne.getEnd(), ranges);
	}
	private static boolean isNEinRange(int neStart,int neEnd, List<Pair<Integer, Integer>> ranges) {
		if (ranges!=null && !ranges.isEmpty()) {
			for(Pair<Integer,Integer> r:ranges) {
				if (Interval.inside(r.getFirst(), r.getSecond(), neStart, neEnd)) return true;
			}
			return false;
		}
		return true;
	}

	@Override
	public boolean isNEAvailableForSpeechAct(NE ne, String speechAct) {
		boolean match=true;
		if (speechAct!=null && sas!=null) {
			match=false;
			for(int i=0;i<sas.length;i++) {
				Matcher m=sas[i].matcher(speechAct);
				if (match=m.matches()) break;
			}
		}
		return match;
	}
	@Override
	public boolean generalizeText() {
		return generalize;
	}
}
