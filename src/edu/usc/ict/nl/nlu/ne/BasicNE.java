package edu.usc.ict.nl.nlu.ne;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.bus.special_variables.SpecialEntitiesRepository;
import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.utils.LogConfig;

public abstract class BasicNE implements NamedEntityExtractorI {
	private NLUConfig configuration;
	
	private SpecialEntitiesRepository svs=null;
	
	public static final Logger logger = Logger.getLogger(NLU.class.getName());
	static {
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );
	}
	
	public NLUConfig getConfiguration() {
		return configuration;
	}
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

	public static String fromTokensToOriginalString(List<Token> in) {
		try {
			return FunctionalLibrary.printCollection(FunctionalLibrary.map(in, Token.class.getMethod("getOriginal")), "", "", " ");
		} catch (Exception e) {
			logger.error(e);
		}
		return null;
	}

	private List<Integer> computeTokenStarts(List<Token> inputTokens) {
		List<Integer> tokenStarts=null;
		if (inputTokens!=null) {
			new ArrayList<Integer>();
			int i=0;
			for(Token t:inputTokens) {
				if (tokenStarts==null) tokenStarts=new ArrayList<>();
				tokenStarts.add(i);
				i+=1+t.getOriginal().length();
			}
			assert(tokenStarts.size()==inputTokens.size());
		}
		return tokenStarts;
	}
	
	@Override
	public List<Token> getModifiedTokens(List<Token> inputTokens) {
		List<Token> ret=null;
		List<Integer> tokenStarts=computeTokenStarts(inputTokens);
		String input=fromTokensToOriginalString(inputTokens);
		try {
			List<NE> nes = extractNamedEntitiesFromText(input);
			filterOverlappingNES(nes);
			if (nes!=null) {
				for(NE ne:nes) {
					int start=ne.getStart();
					int end=ne.getEnd();
					boolean isWholeWordsSubstring=StringUtils.isWholeWordSubstring(start,end,input);
					if (isWholeWordsSubstring) {
						int startToken = getTokenAtPosition(start,tokenStarts);
						int endToken=getTokenAtPosition(end,tokenStarts);
						for(int j=startToken;j<=endToken;j++) {
							Token newToken=null;
							if (j==startToken) {
								Token original=inputTokens.get(j);
								if (original!=null) {
									newToken=new Token("<"+ne.getType().toUpperCase()+">", original.getType(), ne.getMatchedString(), start, end);
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
		return ret;
	}
	
	@Override
	public boolean generalize(List<Token> inputTokens) {
		boolean generalized=false;
		List<Token> mods = getModifiedTokens(inputTokens);
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
	
	private class Interval {
		int start,end;
		public Interval(int start,int end) {
			this.start=start;
			this.end=end;
		}
		public boolean inside(int point) {
			return (point>=start && point<=end);
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
				if (intervals==null) intervals=new ArrayList<BasicNE.Interval>();
				intervals.add(new Interval(start, ne.getEnd()));
			}
		}
	}
	
	private int getTokenAtPosition(int chPos, List<Integer> tokenStarts) {
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
	public static List<NE> filterNESwithSpeechAct(List<NE> nes, String speechAct) {
		List<NE> ret=null;
		if (nes!=null && !StringUtils.isEmptyString(speechAct)) {
			for(NE ne:nes) {
				NamedEntityExtractorI ext=ne.getExtractor();
				if (ext==null || ext.isNEAvailableForSpeechAct(ne, speechAct)) {
					if (ret==null) ret=new ArrayList<>();
					ret.add(ne);
				}
			}
		}
		return ret;
	}
}
