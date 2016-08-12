package edu.usc.ict.nl.nlu.ne.wikidata;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import edu.usc.ict.nl.bus.special_variables.SpecialVar;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.config.PreprocessingConfig;
import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.ne.BasicNE;
import edu.usc.ict.nl.nlu.ne.NE;
import edu.usc.ict.nl.util.AhoCorasickList;
import edu.usc.ict.nl.util.AhoCorasickList.Match;
import edu.usc.ict.nl.util.ProgressTracker;

public class WikidataNE extends BasicNE {
	
	private final SpecialVar wikiThingVar=new SpecialVar(null,null,
			"Wikidata thing extracted from text.",null,String.class);
	
	private AhoCorasickList searcher=null;
	private String neName=null;
	
	public WikidataNE(String neName,String varName,String searchStringsForEntities,String... sas) throws Exception {
		super(sas);
		this.neName=neName;
		wikiThingVar.setName(varName);
		addSpecialVarToRepository(wikiThingVar);
		searcher=loadWiki(searchStringsForEntities);
	}
	
	/**
	 * 
	 * @param neFile same format as WikidataJsonProcessing.ITEMS
	 * @return
	 * @throws Exception
	 */
	public static AhoCorasickList loadWiki(String neFile) throws Exception {
		AhoCorasickList s=new AhoCorasickList();
		BufferedReader in=new BufferedReader(new FileReader(neFile));
		String line=null;
		ProgressTracker pt=new ProgressTracker(1000, System.out);
		int objectCount=0;
		while((line=in.readLine())!=null) {
			String[] parts=line.split("\t");
			String id=parts[0];
			for(int i=1;i<parts.length;i++) {
				String[] words=parts[i].toLowerCase().split("[\\s]+");
				List<String> wordsList=Arrays.asList(words);
				s.addPattern(wordsList, id);
			}
			objectCount++;
			pt.update(objectCount);
		}
		in.close();
		return s;
	}

	
	@Override
	public List<NE> extractNamedEntitiesFromText(String text,PreprocessingType type) {
		List<Token> tokens = getConfiguration().getNluTokenizer(type).tokenize1(text);
		List<String> words = tokens.stream().map(s->s.getOriginal()).collect(Collectors.toList());
		List<Match> match = searcher.findBestMatchInStringOfWords(words);
		List<NE> payloads = null;
		if (match!=null) {
			for(Match m:match) {
				int l=m.match.size();
				int start=tokens.get(m.pos-l+1).getStart();
				int end=tokens.get(m.pos).getEnd();
				if (payloads==null) payloads=new ArrayList<NE>();
				payloads.add(new NE(wikiThingVar.getName(),m.payload,neName,start,end,text.substring(start, end),this));
			}
		}
		return payloads;
	}

	public static void main(String[] args) throws Exception {
		WikidataNE ne = new WikidataNE("COUNTRY", "C", "q6256");
		ne.setConfiguration(NLUConfig.WIN_EXE_CONFIG);
		ne.getConfiguration().setPreprocessingRunningConfig(PreprocessingConfig.WIN_EXE_CONFIG);
		ne.getConfiguration().setPreprocessingTrainingConfig(PreprocessingConfig.WIN_EXE_CONFIG);
		List<NE> r = ne.extractNamedEntitiesFromText("what is the capital of france?",PreprocessingType.RUN);
		System.out.println(r);
	}
}
