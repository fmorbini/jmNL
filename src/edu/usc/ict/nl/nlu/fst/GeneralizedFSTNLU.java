package edu.usc.ict.nl.nlu.fst;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.fst.train.Aligner;
import edu.usc.ict.nl.nlu.fst.train.generalizer.GeneralizedAnnotation;
import edu.usc.ict.nl.nlu.fst.train.generalizer.TDGeneralizerAndLexiconBuilder;
import edu.usc.ict.nl.nlu.preprocessing.TokenizerI;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.Triple;
import edu.usc.ict.nl.utils.ExcelUtils;

public class GeneralizedFSTNLU extends FSTNLU {

	private TDGeneralizerAndLexiconBuilder gen=null;
	private Aligner a=null;
	
	private static final File lexicon=new File("test-lexicon-content.xlsx");
	/*
	 * <parent <leaf <text, count>>>
	 */
	private static Map<String,Map<String,Map<String,Integer>>> lexiconContent=null;
	private static final String ALLKEYCONSTANT="ALL";
	
	public GeneralizedFSTNLU(NLUConfig c) throws Exception {
		super(c);
		gen = new TDGeneralizerAndLexiconBuilder();
		a=new Aligner(new File(getConfiguration().getNLUContentRoot()),c);
		loadLexicon(lexicon);
	}

	private void loadLexicon(File lexiconFile) {
		try {
			Map<String, List<String>> content = ExcelUtils.extractMappingBetweenTheseTwoColumns(lexiconFile.getAbsolutePath(), 0, 0, 1);
			if (content!=null) {
				for(String k:content.keySet()) {
					k=k.toLowerCase();
					String leaf=FSTNLUOutput.getLastComponent(k);
					String parent=FSTNLUOutput.removeLastComponent(k);
					List<String> c=content.get(k);
					if (c!=null) {
						for(String s:c) {
							s=s.toLowerCase();
							s=StringUtils.cleanupSpaces(s);
							String[] parts=s.split("[\\s]+");
							if (parts!=null) {
								if (lexiconContent==null) lexiconContent=new HashMap<String,Map<String,Map<String,Integer>>>();
								for(String p:parts) {
									Map<String,Map<String,Integer>> contentForParent=lexiconContent.get(parent);
									if (contentForParent==null) lexiconContent.put(parent, contentForParent=new HashMap<String, Map<String,Integer>>());
									Map<String,Integer> contentForLeaf=contentForParent.get(leaf);
									if (contentForLeaf==null) contentForParent.put(leaf,contentForLeaf=new HashMap<String, Integer>());
									Integer count=contentForLeaf.get(p);
									if (count==null) contentForLeaf.put(p, 1);
									else contentForLeaf.put(p, count+1);
									
									count=contentForLeaf.get(ALLKEYCONSTANT);
									if (count==null) contentForLeaf.put(ALLKEYCONSTANT, 1);
									else contentForLeaf.put(ALLKEYCONSTANT,count+1);
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			getLogger().error("Error while reading the lexicon: '"+lexiconFile+"'",e);
		}
	}

	@Override
	public void retrain(File... files) throws Exception {
		List<TrainingDataFormat> tds = gen.readTDFiles(files);
		GeneralizedAnnotation ga = gen.createGeneralizerFromTrainingData(tds);
		ga.generalizeTrainingData(tds);
		//Map<String, Set<String>> prefixes = ga.getPrefixes();
		//ExcelUtils.dumpMapToExcel((Map)prefixes, new File("test-lexicon-prefixes.xlsx"), "test", null);
		super.train(tds, new File(getConfiguration().getNluModelFile()));
		
		// to properly generate the lexicon content, the aligner must have been trained with un-generalized data.
		// so don't run: ga.generalizeTrainingData(tds);
		/*List<Alignment> as = a.readAlignerOutputFile();
		AlignmentSummary asummary = new AlignmentSummary(as);
		Map<String, Map<String, Integer>> phrases = asummary.getNluConcepts2phrases();
		Map<String, Set<String>> lexiconContent = ga.buildActualLexicon(phrases);
		ExcelUtils.dumpMapToExcel((Map)lexiconContent, new File("test-lexicon-content.xlsx"), "test", null);
		Map<String, Set<String>> prefixes = ga.getPrefixes();
		ExcelUtils.dumpMapToExcel((Map)prefixes, new File("test-lexicon-prefixes.xlsx"), "test", null);
		 */
	}
	
	@Override
	public List<NLUOutput> getNLUOutput(String text,
			Set<String> possibleNLUOutputIDs, Integer nBest) throws Exception {
		nBest=(nBest==null || nBest<=0)?1:nBest;
		TokenizerI tokenizer=getConfiguration().getNluTokenizer(PreprocessingType.RUN);
		String input=tokenizer.tokAnduntok(text);
		String retFST=tf.getNLUforUtterance(input,nBest);
		//System.out.println(retFST);
		List<FSTNLUOutput> ret=tf.getResults(retFST);
		if (ret!=null && !ret.isEmpty()) {
			for(FSTNLUOutput o:ret) {
				LinkedHashMap<String, List<Triple<Integer, String, Float>>> parts = o.getParts();
				LinkedHashMap<String, List<Triple<Integer, String, Float>>> newParts=new LinkedHashMap<String, List<Triple<Integer,String,Float>>>();
				if (parts!=null) {
					for(String p:parts.keySet()) {
						String parent=FSTNLUOutput.getLastComponent(p);
						String leaf=getBestLeafFor(parent,parts.get(p));
						if (leaf!=null) {
							String newP=p+String.valueOf(FSTNLUOutput.keyValueSep)+leaf;
							newParts.put(newP, parts.get(p));
						} else {
							newParts.put(p, parts.get(p));
						}
					}
				}
				o.setParts(newParts);
			}
		}
		if (ret==null || ret.isEmpty()) {
			String lowConfidenceEvent=getConfiguration().getLowConfidenceEvent();
			if (StringUtils.isEmptyString(lowConfidenceEvent)) {
				getLogger().warn(" no user speech acts left and LOW confidence event disabled, returning no NLU results.");
			} else {
				if (ret==null) ret=new ArrayList<FSTNLUOutput>();
				ret.add(new FSTNLUOutput(text, lowConfidenceEvent, 1f, null));
				getLogger().warn(" no user speech acts left. adding the low confidence event.");
			}
		}

		return (List)ret;
	}

	private String getBestLeafFor(String parent,List<Triple<Integer, String, Float>> nlu) {
		Map<String,Integer> best=null;
		if (nlu!=null && !nlu.isEmpty() && !StringUtils.isEmptyString(parent)) {
			Map<String, Map<String, Integer>> contentForParent = lexiconContent.get(parent);
			if (contentForParent!=null) {
				for (Triple<Integer, String, Float> p:nlu) {
					String w=p.getSecond();
					w=w.toLowerCase();
					for(String leaf:contentForParent.keySet()) {
						Map<String,Integer> contentForLeaf=contentForParent.get(leaf);
						if (contentForLeaf!=null) {
							Integer count=contentForLeaf.get(w);
							if (count!=null) {
								if (best==null) best=new HashMap<String, Integer>();
								Integer cc=best.get(leaf);
								if (cc==null) best.put(leaf, count);
								else best.put(leaf, cc+count);
							}
						}
					}
				}
			}
			if (best!=null) {
				String bestLeaf=null;
				Integer max=null;
				for(String b:best.keySet()) {
					 Integer count=best.get(b);
					 if (count!=null) {
						 if (max==null || max<count) {
							 max=count;
							 bestLeaf=b;
						 }
					 }
				}
				return bestLeaf;
			}
		}
		return null;
	}
}
