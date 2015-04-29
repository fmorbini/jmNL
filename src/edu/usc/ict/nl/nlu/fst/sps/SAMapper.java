package edu.usc.ict.nl.nlu.fst.sps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.fst.FSTNLUOutput;
import edu.usc.ict.nl.nlu.fst.train.Aligner;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.PerformanceResult;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.util.Triple;
import edu.usc.ict.nl.utils.ExcelUtils;
import edu.usc.ict.nl.utils.LogConfig;

public class SAMapper {

	private class IntegerAndDebug {
		int id;
		String text;
		public IntegerAndDebug(int id,String text) {
			this.id=id;
			this.text=text;
		}
	}

	/**
	 * fst nlu is composed by a list of key-value pairs.
	 * <a nlu key, <a nlu value value, the set of id for simcoach sas that can be indicated by that key (with count)>>
	 */
	Map<String,Map<String,NavigableMap<Integer,Integer>>> content=null;
	private int currentID=0;
	/**
	 * contains a mapping between simcoach SA and integer (simcoach SA id).
	 */
	private Map<String,IntegerAndDebug> simcoachSA2id=null;
	/**
	 * contains a mapping between integers (simcoach SA id) and simcoach SA.
	 */
	private Map<Integer,String> id2SimcoachSA=null;
	/**
	 * contains a mapping between sorted fst output to simcoach SA id (with count).
	 */
	private Map<String,Map<Integer,Integer>> fstOutput2SimcoachId=null;
	private NLUConfig config=null;

	private void reset() {
		content=null;
		simcoachSA2id=null;
		fstOutput2SimcoachId=null;
		id2SimcoachSA=null;
		currentID=0;
	}

	private static final Logger logger = Logger.getLogger(SAMapper.class.getName());
	static {
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );
	}

	public SAMapper(NLUConfig configuration) throws Exception {
		this.config=configuration;
		String filename=configuration.getSpsMapperModelFile();
		if (filename!=null) {
			File model=new File(filename);
			loadMapperModel(model);
		}
	}
	public void trainMapperAndSave(SPSFSTNLU spsfstnlu,File step1, File step3,File model) throws Exception {
		Map<Integer, String> taxonomySAs = ExcelUtils.extractRowAndThisColumn(step1.getAbsolutePath(), 0, 0);
		List<TrainingDataFormat> tds=Aligner.getTrainingDataFromGoogle(step3, 0, 1, 2, 11, 1, -1,taxonomySAs);
		List<Triple<String, String, String>> mappingInfo = buildMappingInfo(spsfstnlu,tds);
		saveMapperModel(mappingInfo,model);
	}
	public void trainMapperAndSave(SPSFSTNLU spsfstnlu,File... step1andStep3s) throws Exception {
		trainMapperAndSave(new File(config.getSpsMapperModelFile()),spsfstnlu,step1andStep3s);
	}
	public void trainMapperAndSave(SPSFSTNLU spsfstnlu,List<TrainingDataFormat> tds) throws Exception {
		trainMapperAndSave(new File(config.getSpsMapperModelFile()),spsfstnlu, tds);
	}
	public static void trainMapperAndSave(File model,SPSFSTNLU spsfstnlu,File... step1andStep3s) throws Exception {
		List<TrainingDataFormat> tds=null;
		if (step1andStep3s!=null && step1andStep3s.length>0) {
			for(File step1andStep3:step1andStep3s) {
				Map<Integer, String> taxonomySAs = ExcelUtils.extractRowAndThisColumn(step1andStep3.getAbsolutePath(), 0, 0);
				List<TrainingDataFormat> tmp=Aligner.getTrainingDataFromGoogle(step1andStep3, 0, 2, 3, 11, 1, -1,taxonomySAs);
				if (tmp!=null) {
					if (tds==null) tds=new ArrayList<TrainingDataFormat>();
					tds.addAll(tmp);
				}
			}
		}
		trainMapperAndSave(model,spsfstnlu, tds);
	}
	public static void trainMapperAndSave(File model,SPSFSTNLU spsfstnlu,List<TrainingDataFormat> tds) throws Exception {
		if (tds!=null && !tds.isEmpty()) {
			List<Triple<String, String, String>> mappingInfo = buildMappingInfo(spsfstnlu,tds);
			saveMapperModel(mappingInfo,model);
		} else {
			logger.warn("skipping training the mapper as no training data was provided.");
		}
	}
	enum READSTATE {UTTERANCE,SPEECHACT,FSTOUTPUT};
	public void loadMapperModel() throws Exception {
		reset();
		loadMapperModel(new File(config.getSpsMapperModelFile()));
	}
	public void loadMapperModel(File model) throws Exception {
		BufferedReader in=new BufferedReader(new FileReader(model));
		String line;
		READSTATE state=READSTATE.UTTERANCE;
		String u=null,s=null,f=null;
		int errors=0,total=0;
		while((line=in.readLine())!=null) {
			switch (state) {
			case UTTERANCE:
				u=line;
				state=READSTATE.SPEECHACT;
				break;
			case SPEECHACT:
				s=line;
				state=READSTATE.FSTOUTPUT;
				break;
			case FSTOUTPUT:
				f=line;
				if (StringUtils.isEmptyString(u) || StringUtils.isEmptyString(s) || StringUtils.isEmptyString(f)) {
					in.close();
					throw new Exception("empty utterance, sa or fst nlu output.");
				}
				List<Map<String, String>> fstNluOutputs=FSTNLUOutput.decomposeNLUOutput(f);
				boolean error=addAllMappings(u,s,fstNluOutputs);
				if (error) errors++;
				total++;
				state=READSTATE.UTTERANCE;
				u=s=f=null;
				break;
			default:
				in.close();
				throw new Exception("invalid state");
			}
		}
		in.close();
		if (errors>0) logger.error(errors+"/"+total+" error(s) loading mapper model.");
		logger.info("finished loading mapper model");
	}
	public static void saveMapperModel(List<Triple<String, String, String>> mappingInfo, File model) throws IOException {
		BufferedWriter out=new BufferedWriter(new FileWriter(model));
		for(Triple<String, String, String> info:mappingInfo) {
			String utterance=info.getFirst();
			String spsSA=info.getSecond();
			String fstNLUOutputs=info.getThird();
			out.write(utterance+"\n");
			out.write(spsSA+"\n");
			out.write(fstNLUOutputs+"\n");
		}
		out.close();
	}
	public static List<Triple<String,String,String>> buildMappingInfo(SPSFSTNLU spsfstnlu, List<TrainingDataFormat> tds) {
		List<Triple<String,String,String>> ret=null;
		logger.info("======================starting to build sps simcoach mapper===============");
		for(TrainingDataFormat td:tds) {

			// try the nlu and add as possible nlu encoding what the nlu actually returns
			if (spsfstnlu!=null && spsfstnlu.getConfiguration().getSpsMapperUsesNluOutput()) {
				try {
					List<FSTNLUOutput> result = spsfstnlu.getRawNLUOutput(td.getUtterance(), null,1);
					if (result!=null) {
						String fstNLUOutputs=result.get(0).getId();
						if (ret==null) ret=new ArrayList<Triple<String,String,String>>();
						ret.add(new Triple<String, String, String>(td.getUtterance(), convertSA(td.getId()), fstNLUOutputs));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			// add the expected nlu return (gold label)
			String fstNLULabel=td.getLabel();
			if (ret==null) ret=new ArrayList<Triple<String,String,String>>();
			ret.add(new Triple<String, String, String>(td.getUtterance(), convertSA(td.getId()), fstNLULabel));
		}
		logger.info("==========================================================================");
		return ret;
	}
	private boolean addAllMappings(String utterance, String spsSA,List<Map<String, String>> fstNluOutputs) {
		boolean error=false;
		if (fstNluOutputs!=null) {
			for(Map<String,String> fstNluOutput:fstNluOutputs) {
				try {
					error|=addMapping(utterance,spsSA,fstNluOutput);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		return error;
	}
	public static String convertSA(String rawSA) {
		rawSA=rawSA.replaceAll("([a-z])([A-Z])", "$1-$2");
		rawSA=rawSA.toLowerCase();
		rawSA=rawSA.replaceAll("[^a-z0-9\\.]", "-");
		rawSA=rawSA.replaceAll("-+", "-");
		rawSA=StringUtils.cleanupSpaces(rawSA);
		rawSA=rawSA.replaceAll("^[0-9]*", "");
		rawSA=rawSA.replaceAll("\\.[0-9]*", ".");
		rawSA=rawSA.replaceAll("\\.[^a-z]*", ".");
		rawSA=rawSA.replaceAll("\\.+", ".");
		rawSA=rawSA.replaceAll("^\\.|\\.$", "");
		rawSA=rawSA.replaceAll("^\\-|\\-$", "");
		return "question.ros."+rawSA;
	}
	public static void convertSAInTrainingData(List<TrainingDataFormat> toModify) {
		if (toModify!=null) {
			for(TrainingDataFormat td:toModify) {
				td.setLabel(convertSA(td.getLabel()));
			}
		}
	}

	/**
	 *
	 * @param simcoach2Utterances: simcoach speech act and utterances
	 * @param taxonomyTrainingData: training data with utterances, new nlu and taxonomy speech act in getid field.
	 */
	public void checkSimcoachMapping(File simcoach2Utterances, List<TrainingDataFormat> taxonomyTrainingData) {
		Set<String> simcoachSAs = ExcelUtils.extractUniqueValuesInThisColumn(simcoach2Utterances.getAbsolutePath(), 0, 4);
		Set<String> copySimcoachSAs=new HashSet<String>(simcoachSAs);
		Set<String> notFound=new HashSet<String>();
		System.out.println(copySimcoachSAs.size());
		for(TrainingDataFormat td:taxonomyTrainingData) {
			String taxonomy2simcoach=convertSA(td.getId());
			if (!simcoachSAs.contains(taxonomy2simcoach)) {
				notFound.add(taxonomy2simcoach);
			}
			copySimcoachSAs.remove(taxonomy2simcoach);
		}
		System.err.println(notFound.size());
		System.err.println("taxonomy items not found in simcoach training data:\n"+notFound);
		System.err.println(copySimcoachSAs.size());
		System.err.println("remaining in training data unmatched by taxonomy:\n"+copySimcoachSAs);
	}

	public boolean addMapping(String text, String simcoachSA, Map<String, String> fstNluOutput) throws Exception {
		boolean error=false;
		if (!StringUtils.isEmptyString(simcoachSA) && fstNluOutput!=null && !fstNluOutput.isEmpty()) {
			//if (!consistencyCheckWith(fstNluOutput)) logger.error("adding "+fstNluOutput+" will add an inconsistency.");
			Integer id=addToDictionary(simcoachSA,text);

			if (fstOutput2SimcoachId==null) fstOutput2SimcoachId=new HashMap<String, Map<Integer,Integer>>();
			String si=produceSortedSignature(fstNluOutput);
			Map<Integer,Integer> siToSimcoachSAs=fstOutput2SimcoachId.get(si);

			if (siToSimcoachSAs==null) fstOutput2SimcoachId.put(si, siToSimcoachSAs=new HashMap<Integer, Integer>());
			Integer count=siToSimcoachSAs.get(id);
			if (count==null) siToSimcoachSAs.put(id,1);
			else siToSimcoachSAs.put(id, count+1);

			if (content==null) content=new HashMap<String, Map<String,NavigableMap<Integer,Integer>>>();
			for(String key:fstNluOutput.keySet()) {
				String value=fstNluOutput.get(key);
				Map<String, NavigableMap<Integer,Integer>> values2SimcoachSAs = content.get(key);
				if (values2SimcoachSAs==null) content.put(key, values2SimcoachSAs=new HashMap<String, NavigableMap<Integer,Integer>>());
				NavigableMap<Integer,Integer> simcoachSAs = values2SimcoachSAs.get(value);
				if (simcoachSAs==null) values2SimcoachSAs.put(value, simcoachSAs=new TreeMap<Integer,Integer>());
				count=simcoachSAs.get(id);
				if (count==null) simcoachSAs.put(id,1);
				else simcoachSAs.put(id,count+1);
			}
		} else {
			throw new Exception("trying to add to the mapping an empty speech act mapping.");
		}
		return error;
	}

	public static String produceSortedSignature(Map<String, String> fstNluOutput) throws Exception {
		if (fstNluOutput!=null) {
			List<String> keys=new ArrayList<String>(fstNluOutput.keySet());
			Collections.sort(keys);
			boolean first=true;
			StringBuffer ret=new StringBuffer();
			for(String k:keys) {
				ret.append((first?"":" ")+FSTNLUOutput.composeNLUKeyValuePair(k, fstNluOutput.get(k)));
				first=false;
			}
			return ret.toString();
		}
		return null;
	}

	private Integer getIDForSpeechAct(String sa) {
		IntegerAndDebug d=simcoachSA2id.get(sa);
		if (d!=null) return d.id;
		else return null;
	}
	private Integer addToDictionary(String simcoachSA,String text) throws Exception {
		Integer ret=null;
		if (simcoachSA2id==null) simcoachSA2id=new HashMap<String, IntegerAndDebug>();
		if (id2SimcoachSA==null) id2SimcoachSA=new HashMap<Integer, String>();
		IntegerAndDebug dkey=simcoachSA2id.get(simcoachSA);
		Integer key=(dkey!=null)?dkey.id:null;
		ret=key;
		if (key!=null && !id2SimcoachSA.containsKey(key)) throw new Exception("direct has mapping but inverse doesn't: "+simcoachSA+" "+key);
		if (key==null) {
			simcoachSA2id.put(simcoachSA, new IntegerAndDebug(currentID, text));
			id2SimcoachSA.put(currentID, simcoachSA);
			ret=currentID;
			currentID++;
		}
		return ret;
	}
	
	public Set<Pair<String,Integer>> getFSTNLUOutputsForSimcoachSA(String sa) {
		Set<Pair<String,Integer>> ret=null;
		IntegerAndDebug said=simcoachSA2id.get(sa);
		if (said==null) return null;
		else {
			//said.id;
			for (String fst:fstOutput2SimcoachId.keySet()) {
				Map<Integer, Integer> saidAndCount=fstOutput2SimcoachId.get(fst);
				if (saidAndCount!=null) {
					for(Integer id:saidAndCount.keySet()) {
						if (id.equals(said.id)) {
							if (ret==null) ret=new HashSet<Pair<String,Integer>>();
							ret.add(new Pair<String,Integer>(fst,saidAndCount.get(id)));
						}
					}
				}
			}
		}
		return ret;
	}
	
	public String getSimcoachSAForFSTNLUOutput(FSTNLUOutput fstNluOutput,boolean doRelaxedMatch) {
		return pickSimcoachSAForFSTNLUOutput(fstNluOutput.getId(),doRelaxedMatch);
	}
	/**
	 * given the String representation of the FST output, it returns the best simcoach SA.
	 * @param fstNluOutput
	 * @return
	 */
	public String pickSimcoachSAForFSTNLUOutput(String fstNluOutput,boolean doRelaxedMatch) {
		List<Pair<Set<String>, Float>> r = getSimcoachSAForFSTNLUOutput(fstNluOutput,doRelaxedMatch);
		String bestMatch=null;
		if (r!=null) {
			float bestMatchValue=0;
			for(Pair<Set<String>, Float> sas:r) {
				if (sas!=null) {
					float v=sas.getSecond();
					if (v>bestMatchValue) {
						bestMatchValue=v;
						bestMatch=sas.getFirst().iterator().next();
					}
				}
			}
		}
		return bestMatch;
	}
	public List<Pair<Set<String>, Float>> getSimcoachSAForFSTNLUOutput(String fstNluOutput,boolean doRelaxedMatch) {
		List<Map<String, String>> decomposedFSTNLUOutputs = FSTNLUOutput.decomposeNLUOutput(fstNluOutput);
		List<Pair<Set<String>,Float>> allMatches=null;
		if (decomposedFSTNLUOutputs!=null) {
			for(Map<String,String> decomposedFSTNLUOutput:decomposedFSTNLUOutputs) {
				List<Pair<Set<String>, Float>> sas=getSAsFromConstraints(decomposedFSTNLUOutput,doRelaxedMatch);
				if (sas!=null) {
					if (allMatches==null) allMatches=new ArrayList<Pair<Set<String>,Float>>();
					allMatches.addAll(sas);
				}
			}
		}
		if (allMatches!=null) {
			Collections.sort(allMatches, new Comparator<Pair<Set<String>,Float>>() {
				@Override
				public int compare(Pair<Set<String>, Float> o1,Pair<Set<String>, Float> o2) {
					return -o1.getSecond().compareTo(o2.getSecond());
				}
			});
		}
		return allMatches;
	}

	private List<Pair<Set<String>, Float>> getSAsFromConstraints(Map<String, String> constraints,boolean doRelaxedMatch) {
		List<Pair<Set<String>, Float>> result=null;
		try {
			result=getSAFromFullConstraintsMatch(constraints);
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (result!=null) return result;
		if (doRelaxedMatch) result=getSAsFromSingleConstraintMatches(constraints);
		return result;
	}
	private List<Pair<Set<String>,Float>> getSAFromFullConstraintsMatch(Map<String, String> constraints) throws Exception {
		List<Pair<Set<String>,Float>> ret=null;
		if (fstOutput2SimcoachId!=null) {
			String si=produceSortedSignature(constraints);
			Map<Integer,Integer> idstoCounts=fstOutput2SimcoachId.get(si);
			if (idstoCounts!=null) {
				List<Pair<Integer,Float>> foundIdsWithFrequency=findKeyWithHighestValue(idstoCounts);
				for(Pair<Integer,Float> foundIdWithFrequency:foundIdsWithFrequency) {
					String found=id2SimcoachSA.get(foundIdWithFrequency.getFirst());
					if (found!=null) {
						Set<String> foundSet=new HashSet<String>();
						foundSet.add(found);
						if (ret==null) ret=new ArrayList<Pair<Set<String>,Float>>();
						ret.add(new Pair<Set<String>, Float>(foundSet, foundIdWithFrequency.getSecond()));
					}
				}
			}
		}
		return ret;
	}
	private List<Pair<Integer,Float>> findKeyWithHighestValue(final Map<Integer, Integer> m) {
		List<Integer> tmp=new ArrayList<Integer>(m.keySet());
		Collections.sort(tmp, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return -m.get(o1).compareTo(m.get(o2));
			}
		});
		List<Pair<Integer,Float>> ret=new ArrayList<Pair<Integer,Float>>();
		int total=0;
		for(Integer k:tmp) total+=m.get(k);
		for(Integer k:tmp) {
			ret.add(new Pair<Integer, Float>(k, (float)m.get(k)/(float)total));
		}
		return ret;
	}
	private List<Pair<Set<String>, Float>> getSAsFromSingleConstraintMatches(Map<String, String> constraints) {
		SortedMap<Integer,Integer> rawMap=null;
		for(String k:constraints.keySet()) {
			String v=constraints.get(k);
			SortedMap<Integer, Integer> tmpSAs=getSetSAs(k,v);
			if (tmpSAs!=null) {
				if (rawMap==null) rawMap=tmpSAs;
				else rawMap.keySet().retainAll(tmpSAs.keySet());
				if (rawMap==null || rawMap.isEmpty()) return null;
			}
		}
		Map<Float,Set<String>> tmp=null;
		if (rawMap!=null) {
			int maxCount=0;
			for(Integer e:rawMap.keySet()) maxCount+=rawMap.get(e);
			for(Integer e:rawMap.keySet()) {
				Float f=(float)rawMap.get(e)/(float)maxCount;
				String sa=id2SimcoachSA.get(e);
				if (sa!=null) {
					if (tmp==null) tmp=new TreeMap<Float, Set<String>>();
					Set<String> sasForFrequency=tmp.get(f);
					if (sasForFrequency!=null) {
						tmp.put(f, sasForFrequency=new HashSet<String>());
						sasForFrequency.add(sa);
					}
				}
			}
		}
		List<Pair<Set<String>, Float>> ret=null;
		if (tmp!=null) {
			for(Float f:tmp.keySet()) {
				if (ret==null) ret=new ArrayList<Pair<Set<String>,Float>>();
				ret.add(new Pair<Set<String>, Float>(tmp.get(f),f));
			}
		}
		return ret;
	}

	private SortedMap<Integer,Integer> getSetSAs(String fstNluKey, String fstNluValue) {
		Map<String, NavigableMap<Integer, Integer>> values = content.get(fstNluKey);
		if (values!=null) {
			NavigableMap<Integer, Integer> map = values.get(fstNluValue);
			return map;
		}
		return null;
	}
	
	private static final String oneBest="1B";
	private static final String oneBestAllSet="1BS";
	private static final String nBest="NB";
	public Map<String,PerformanceResult> test(List<TrainingDataFormat> testData, boolean relaxedMatch) {
		Map<String,PerformanceResult> out=new HashMap<String, PerformanceResult>();
		PerformanceResult p1=out.get(oneBest);
		if (p1==null) out.put(oneBest, p1=new PerformanceResult());
		PerformanceResult p1s=out.get(oneBestAllSet);
		if (p1s==null) out.put(oneBestAllSet, p1s=new PerformanceResult());
		PerformanceResult pn=out.get(nBest);
		if (pn==null) out.put(nBest, pn=new PerformanceResult());
		if (testData!=null) {
			for(TrainingDataFormat td:testData) {
				boolean r1b=false,r1bs=false,rnb=false;
				String fstOutput = td.getLabel();
				List<Pair<Set<String>, Float>> foundSimcoachSAs=getSimcoachSAForFSTNLUOutput(fstOutput, relaxedMatch);
				if (foundSimcoachSAs!=null) {
					String goldSimcoachSA=SAMapper.convertSA(td.getId());
					if (foundSimcoachSAs!=null && !foundSimcoachSAs.isEmpty()) {
						Set<String> first=foundSimcoachSAs.get(0).getFirst();
						String firstPick=first.iterator().next();
						r1b=goldSimcoachSA.equalsIgnoreCase(firstPick);
						if (!r1b) {
							for(String s:first) {
								if (goldSimcoachSA.equalsIgnoreCase(s)) {
									r1bs=true;
									break;
								}
							}
							if (!r1bs) {
								for(Pair<Set<String>,Float> x:foundSimcoachSAs) {
									for(String s:x.getFirst()) {
										if (goldSimcoachSA.equalsIgnoreCase(s)) {
											rnb=true;
											System.out.println(td.getUtterance());
											System.out.println("  extra info: "+td.getExtraInfo());
											System.out.println("  ANNOTATION: "+fstOutput+" EXPECTED: "+goldSimcoachSA);
											System.out.println("  FOUND: "+foundSimcoachSAs);
											break;
										}
									}
									if (rnb) break;
								}
							} else {
								rnb=true;
							}
						} else {
							r1bs=true;
							rnb=true;
						}
					}
				}
				p1.add(r1b);
				p1s.add(r1bs);
				pn.add(rnb);
			}
		}
		return out;
	}

	public static List<List<String>> export(List<TrainingDataFormat> tds) {
		List<List<String>> ret=null;
		if (tds!=null && !tds.isEmpty()) {
			final Map<String,Integer> columns=new HashMap<String, Integer>();
			columns.put("sps label",0);
			columns.put("templated utterance",1);
			columns.put("utterance",2);
			columns.put("utterance_type",3);
			columns.put("predicate",4);
			columns.put("predicate_modifier",5);
			columns.put("object",6);
			columns.put("object_modifier",7);
			columns.put("time",8);
			columns.put("temporal_complement",9);
			columns.put("location",10);
			columns.put("source",11);
			for(TrainingDataFormat td:tds) {String label=td.getLabel();
				List<Pair<String, String>> decomposedLabel = FSTNLUOutput.getPairsFromString(label);
				List<String> row=null; 
				if (decomposedLabel!=null) {
					row=new ArrayList<String>();
					setPositionInListWith(row,columns.get("sps label"),td.getId());
					setPositionInListWith(row,columns.get("utterance"),td.getUtterance());
					for(Pair<String, String> kv:decomposedLabel) {
						String key=kv.getFirst().toLowerCase();
						Integer pos=columns.get(key);
						setPositionInListWith(row,pos,kv.getSecond());
					}
				}
				if (row!=null) {
					if (ret==null) {
						ret=new ArrayList<List<String>>();
						List<String> header=new ArrayList<String>(columns.keySet());
						Collections.sort(header,new Comparator<String>() {
							@Override
							public int compare(String o1, String o2) {
								return columns.get(o1)-columns.get(o2);
							}
						});
						ret.add(header);
					}
					ret.add(row);
				}
			}

		}
		return ret;
	}
	private static void setPositionInListWith(List<String> row, Integer pos,String value) {
		if (pos!=null) {
			if (row==null) row=new ArrayList<String>();
			if (pos>=row.size()) for(int i=pos-row.size();i>=0;i--) row.add(null);
			row.set(pos, value);
		}
	}

}
