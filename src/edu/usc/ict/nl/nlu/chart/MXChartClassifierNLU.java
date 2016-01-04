package edu.usc.ict.nl.nlu.chart;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import edu.usc.ict.nl.bus.NLBus;
import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.ChartNLUOutput;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.io.BuildTrainingData;
import edu.usc.ict.nl.nlu.mxnlu.MXClassifierNLU;
import edu.usc.ict.nl.nlu.preprocessing.Preprocess;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.PerformanceResult;
import edu.usc.ict.nl.util.Rational;
import edu.usc.ict.nl.util.StringUtils;

public class MXChartClassifierNLU extends NLU {
	
	public MXChartClassifierNLU(NLUConfig config) throws Exception {
		super(config);
		NLUConfig internalConfig=(NLUConfig) config.clone();
		internalConfig.setNluHardLinks(null);
		internalConfig.setLowConfidenceEvent(null);
		this.internalNLU=(NLU) NLBus.createSubcomponent(internalConfig,config.getInternalNluClass4Chart());
	}

	protected NLU internalNLU;

	// additional test to make the approach linear
	public Collection<PartialClassification> runChartClassifier3(String text,MXClassifierNLU nlu, boolean applyTransformationsToInput,boolean onlyOneSpeechAct) throws Exception {
		Preprocess pr = nlu.getPreprocess();
		List<List<Token>> options = pr.process(text,true);
		String processedText=pr.getString(options.get(0));
		String[] words=processedText.split(" ");
		int numWords=words.length;
		Collection<PartialClassification> chart=new ArrayList<PartialClassification>();
		
		if (onlyOneSpeechAct) {
			PartialClassification res=new PartialClassification(0, numWords, processedText, null);
			res.classifyWith(nlu);
			if (res.getTopResult()!=null) chart.add(res);
			return chart;
		}

		Stack<PartialClassification> segmentation=new Stack<PartialClassification>();
		PartialClassification current;
		segmentation.push(current=new PartialClassification(0, words[0]));
		current.classifyWith(nlu);
		for(int end=1;end<numWords;end++) {
			String currentWord=words[end];
			PartialClassification lastClass=segmentation.peek();
			PartialClassification continuationOfLast=lastClass.clone();
			continuationOfLast.addWord(currentWord);
			continuationOfLast.classifyWith(nlu);
			
			Rational newProb=continuationOfLast.getProbTopResult();
			Rational oldProb=lastClass.getProbTopResult();
			String oldResult=(lastClass.getTopResult()!=null)?lastClass.getTopResult().getId():"";
			String newResult=(continuationOfLast.getTopResult()!=null)?continuationOfLast.getTopResult().getId():"";
			if (!oldResult.equals(newResult)) {
				if (newProb.compareTo(oldProb)>0) {
					segmentation.pop();
					segmentation.push(continuationOfLast);
				} else {
					lastClass=new PartialClassification(end, currentWord);
					lastClass.classifyWith(nlu);
					segmentation.push(lastClass);
				}
			} else {
				segmentation.pop();
				segmentation.push(continuationOfLast);
			}
		}

		for(PartialClassification pc:segmentation) {
			if (pc.getTopResult()!=null) {
				chart.add(pc);
			}
		}
		System.out.println(text+" "+chart);
		return chart;
	}
	// the one described in the paper
	public Collection<PartialClassification> runChartClassifier2(String text, Set<String> possibleNLUOutputIDs, boolean applyTransformationsToInput,boolean onlyOneSpeechAct) throws Exception {
		NLUOutput hardLabel=getHardLinkMappingOf(text);
		if (hardLabel!=null) {
			Collection<PartialClassification> ret=new ArrayList<PartialClassification>();
			PartialClassification pc = new PartialClassification(0,text);
			pc.setTopResult(hardLabel);
			ret.add(pc);
			return ret;
		}
		String lowConfidenceEvent=getConfiguration().getLowConfidenceEvent();
		Preprocess pr = internalNLU.getPreprocess();
		List<List<Token>> options = pr.process(text,true);
		String processedText=pr.getString(options.get(0));
		String[] words=processedText.split("[\\s]+");
		int numWords=words.length;
		
		// limit maximum length to apply the chart process.
		int maxLength=getConfiguration().getChartNluMaxLength();
		if (numWords>maxLength) onlyOneSpeechAct=true;
		
		Collection<PartialClassification> chart=new ArrayList<PartialClassification>();
		
		if (onlyOneSpeechAct) {
			PartialClassification res=new PartialClassification(0, numWords, processedText, null);
			res.classifyWith(internalNLU);
			if (res.getTopResult()!=null) chart.add(res);
			return chart;
		}

		PartialClassification[] bestAtPoint=new PartialClassification[numWords];
		bestAtPoint[0]=new PartialClassification(0, words[0]);
		bestAtPoint[0].classifyWith(internalNLU);
		PartialClassification tmp =new PartialClassification();
		for(int end=1;end<numWords;end++) {
			Rational bestAtNextPointValue=Rational.zero;
			PartialClassification bestAtNextPoint=null;
			for(int start=end;start>=0;start--) {
				// get prob of best result to be concatenated.
				PartialClassification bestToContinue=(start==0)?null:bestAtPoint[start-1];

				tmp.reset(start, start+1, words[start]);
				for(int i=start+1;i<=end;i++) {
					tmp.addWord(words[i]);
				}
				
				tmp.classifyWith(internalNLU);

				NLUOutput topResult=tmp.getTopResult();
				if (topResult!=null) {
					String id=topResult.getId();
					if (!StringUtils.isEmptyString(id) && id.equals(lowConfidenceEvent)) {
						tmp.clearResult();
					}
				}

				Rational newProb=tmp.getProbTopResult();
				Rational valueCurrentContinuation;
				if ((bestToContinue==null)) {
					if (tmp.getTopResult()!=null) valueCurrentContinuation=newProb;
					else valueCurrentContinuation=Rational.zero;
				} else {
					Pair<Integer,Rational> numSegmentsInBestToContinueAndProb=countPreviousSegmentsAndProb(bestAtPoint,tmp);
					if (numSegmentsInBestToContinueAndProb.getFirst()>0) {
						Rational value=numSegmentsInBestToContinueAndProb.getSecond();
						//consider all previous segments: uncomment for avg
						value=new Rational(Math.pow(value.toDouble(), ((double)1)/(double)(numSegmentsInBestToContinueAndProb.getFirst())));
						value=value.minus(new Rational(numSegmentsInBestToContinueAndProb.getFirst(), 100));
						valueCurrentContinuation=value;
					} else {
						valueCurrentContinuation=Rational.zero;
					}
				}
				if ((bestAtNextPointValue.compareTo(valueCurrentContinuation)<0) || bestAtNextPoint==null) {
					if (bestAtNextPoint==null) bestAtNextPoint=tmp.clone();
					else bestAtNextPoint=bestAtNextPoint.resetFrom(tmp);
					bestAtNextPointValue=valueCurrentContinuation;
				}
			}
			bestAtPoint[end]=bestAtNextPoint;
		}

		for(int i=numWords-1;i>=0;) {
			PartialClassification pc=bestAtPoint[i];
			if (pc.getTopResult()!=null) {
				chart.add(pc);
			}
			//System.out.println(i+" "+(i-(pc.getEnd()-pc.getStart()))+" "+pc.getStart());
			i=pc.getStart()-1;
			//i-=pc.getEnd()-pc.getStart();
		}
		
		// check if the result contains just one speech act, in that case re-run forcing one speech act as output.
		if (chart.size()==1) {
			PartialClassification pc=chart.iterator().next();
			if ((pc.getStart()>0) || (pc.getEnd()<numWords)) return runChartClassifier2(text, possibleNLUOutputIDs, applyTransformationsToInput, true);
		}
		
		return chart;
	}
	private Pair<Integer,Rational> countPreviousSegmentsAndProb(PartialClassification[] bestAtPoint,PartialClassification from) throws Exception {
		int ret=0;
		Rational prob=null;
		if (from.getTopResult()!=null) {
			prob=new Rational(from.getProbTopResult().numerator(),from.getProbTopResult().denominator());
			ret++;
		}
		for(int i=from.getStart()-1;i>=0;) {
			from=bestAtPoint[i];
			if (from.getTopResult()!=null) {
				prob=(prob!=null)?prob.times(from.getProbTopResult()):new Rational(from.getProbTopResult().numerator(),from.getProbTopResult().denominator());
				ret++;
			}
			i=from.getStart()-1;
		}
		return new Pair<Integer,Rational>(ret,prob);
	}

	// initial test
	public Collection<PartialClassification> runChartClassifier(String text,MXClassifierNLU nlu, boolean applyTransformationsToInput) throws Exception {
		Collection<PartialClassification> chart=new ArrayList<PartialClassification>();
		
		Preprocess pr = nlu.getPreprocess();
		List<List<Token>> options = pr.process(text,true);
		String processedText=pr.getString(options.get(0));

		String[] words=processedText.split(" ");
		int numWords=words.length;
		LinkedList<PartialClassification> partials=new LinkedList<PartialClassification>();
		for(int i=0;i<numWords;i++) {
			partials.add(new PartialClassification(i, words[i]));
		}
		while(!partials.isEmpty()) {
			/* pick a partial
			 * classify the partial
			 * if classification succesful, remove partial
			 * otherwise add next word to partial
			 */
			PartialClassification el = partials.remove();
			if (el.classifyWith(nlu)) {
				chart.add(el);
			} else if (el.getEnd()<numWords) {
				el.addWord(words[el.getEnd()]);
				partials.add(el);
			}
		}
		//System.out.println(chart);
		// try to increase the coverage of all elements in the chart
		for(PartialClassification el:chart) {
			PartialClassification tmp = el.clone();
			int numFailures=0;
			while (tmp.getEnd()<numWords) {
				tmp.addWord(words[tmp.getEnd()]);
				if (tmp.classifyWith(nlu) && (tmp.getProbTopResult().compareTo(el.getProbTopResult())>=0)) {
					el.updateFrom(tmp);
					numFailures=0;
				} else {
					numFailures++;
				}
				if (numFailures>2) break;
			}
		}
		chart=mergeSameFragments(chart);
		//System.out.println(chart);
		chart=removeContainedFragments(chart);
		//System.out.println(chart);
		//logger.info("input text: '"+text+"'");
		//logger.info("generalized text: '"+processedText+"'");
		return chart;
	}

	private Collection<PartialClassification> mergeSameFragments(Collection<PartialClassification> chart) {
		HashMap<String,ArrayList<PartialClassification>> sameFragments=new HashMap<String, ArrayList<PartialClassification>>();
		for(PartialClassification el:chart) {
			String sa=el.getTopResult().getId();
			ArrayList<PartialClassification> list=sameFragments.get(sa);
			if (list==null) sameFragments.put(sa, list=new ArrayList<PartialClassification>());
			list.add(el);
		}
		HashSet<PartialClassification> ret=new HashSet<PartialClassification>(chart);
		for(String sa:sameFragments.keySet()) {
			ArrayList<PartialClassification> list=sameFragments.get(sa);
			if (list.size()>1) {
				
			}
		}
		return ret;
	}

	public int computeIntersection(PartialClassification el1, PartialClassification el2) {
		int intersection=0;
		if (el1.getStart()<=el2.getStart()) {
			if (el1.getEnd()>el2.getStart()) intersection=Math.min(el2.getEnd(), el1.getEnd())-el2.getStart();
		} else if (el1.getStart()>el2.getStart() && el1.getStart()<el2.getEnd()) {
			intersection=Math.min(el2.getEnd(), el1.getEnd())-el1.getStart();
		}
		return intersection;
	}

	public boolean doesThisCoverAllOfThat(PartialClassification el1, PartialClassification el2) {
		int intersection=computeIntersection(el1, el2);
		int el1Length=el1.getEnd()-el1.getStart();
		int el2Length=el2.getEnd()-el2.getStart();
		return (intersection>=el2Length) || (((el1.getProbTopResult().compareTo(el2.getProbTopResult())>0) || (el1Length>el2Length)) && (intersection>(el2Length*0.5)));
	}
	
	private Collection<PartialClassification> removeContainedFragments(Collection<PartialClassification> chart) {
		HashSet<PartialClassification> ret=new HashSet<PartialClassification>(chart);
		for(PartialClassification el1:chart) {
			if (ret.contains(el1)) {
				for (PartialClassification el2:chart) {
					if ((el1!=el2) && doesThisCoverAllOfThat(el1, el2)) ret.remove(el2);
				}
			}
		}
		return ret;
	}
	
	protected HashMap<String, ArrayList<TrainingDataFormat>> splitDataInSingleClass(List<TrainingDataFormat> td) throws Exception {
		HashMap<String, ArrayList<TrainingDataFormat>> ret=new HashMap<String, ArrayList<TrainingDataFormat>>();
		HashSet<String> allClasses=getAllClassesInData(td);
		for (String c:allClasses) ret.put(c, new ArrayList<TrainingDataFormat>());
		for(TrainingDataFormat d:td) {
			String c=d.getLabel();
			String u=d.getUtterance();
			for(Entry<String,ArrayList<TrainingDataFormat>> cd:ret.entrySet()) {
				String key=cd.getKey();
				if (key.equals(c)) {
					cd.getValue().add(new TrainingDataFormat(u, "true"));
				} else {
					cd.getValue().add(new TrainingDataFormat(u, "false"));
				}
			}
		}
		return ret;
	}

	private HashSet<String> getAllClassesInData(List<TrainingDataFormat> td) {
		HashSet<String> ret=new HashSet<String>();
		for(TrainingDataFormat uc:td) ret.add(uc.getLabel());
		return ret;
	}

	@Override
	public boolean nluTest(TrainingDataFormat testSample, List<NLUOutput> nluResults) throws Exception {
		boolean found=false;
		if (nluResults!=null && !nluResults.isEmpty()) {
			NLUOutput best=nluResults.get(0);
			if (best instanceof ChartNLUOutput) {
				if (((ChartNLUOutput)best).matchesInterpretation(testSample.getLabel())) found=true;
			} else {
				if (testSample.match(best.getId())) found=true;
			}
		}
		return found;
	}
	public PerformanceResult testNLUOnThisData(List<TrainingDataFormat> testing,File model,boolean printMistakes) throws Exception {
		internalNLU.loadModel(model);
		PerformanceResult result=new PerformanceResult();
		for(TrainingDataFormat td:testing) {
			List<NLUOutput> sortedNLUOutput = getNLUOutput(td.getUtterance(),null,null);
			boolean found=nluTest(td, sortedNLUOutput);
			if (found) {
				result.add(true);
			} else {
				result.add(false);
				if (printMistakes) {
					if (sortedNLUOutput==null || sortedNLUOutput.isEmpty()) getLogger().error("'"+td.getUtterance()+"' ("+td.getLabel()+") -> NOTHING");
					else getLogger().error("'"+td.getUtterance()+"' ("+td.getLabel()+") ->"+sortedNLUOutput.get(0));
				}
			}
		}
		return result;
	}

	private Pair<PartialClassification,Float> findBestMatchForIn(PartialClassification el,Collection<PartialClassification> list) {
		float bestMatch=0;
		PartialClassification match=null;
		NLUOutput sa=el.getTopResult();
		
		for(PartialClassification oEl:list) {
			NLUOutput oSA=oEl.getTopResult();
		
			if (sa.getId()==null || sa.getId().equals(oSA.getId())) {
				float thisMatch=computeCoverage(el, oEl);
				if (thisMatch>bestMatch) {
					bestMatch=thisMatch;
					match=oEl;
				}
			}
		}
		if (match!=null) return new Pair<PartialClassification, Float>(match,bestMatch);
		else return null;
	}
	
	public PerformanceResult compareChartNLUClassificationResult(Collection<PartialClassification> result,Collection<PartialClassification> expectedResult) throws Exception {
		if (expectedResult.isEmpty()) {
			throw new Exception("mepty expected result.");
			/*
			if (result.isEmpty()) return new PerformanceResult(0, 0, 0);
			else return new PerformanceResult(0, 0, result.size());
			*/
		}
		
		/*
		 * for every element, ee, in the expected result find the element, e, in result that best matches it. Record it and the match value.
		 * 
		 * for every e that was used only once, remove e and ee from the ER and R vectors and save it in the final pairing result.
		 * for every e that was used in multiple ee, select the ee with highest match value. Save e, ee in final pairing result.
		 *  Then result search for all others ee excluding all e that are in the final pairing result.
		 * continue checks till no more conflicts.
		 *  
		 */
		
		HashSet<PartialClassification> remainingResults=new HashSet<PartialClassification>(result);
		HashMap<PartialClassification,Pair<PartialClassification,Float>> finalPairings=new HashMap<PartialClassification, Pair<PartialClassification,Float>>();
		HashMap<PartialClassification,Pair<PartialClassification, Float>> bestMatchForExpected=new HashMap<PartialClassification, Pair<PartialClassification,Float>>();
		HashMap<PartialClassification,HashSet<PartialClassification>> useListForResultElement=new HashMap<PartialClassification, HashSet<PartialClassification>>();

		boolean first=true;
		
		while(!bestMatchForExpected.isEmpty() || first) {
			first=false;
			if (useListForResultElement!=null) useListForResultElement.clear();
			
			for(PartialClassification el:(bestMatchForExpected.isEmpty())?expectedResult:bestMatchForExpected.keySet()) {
				Pair<PartialClassification, Float> match = findBestMatchForIn(el, remainingResults);
				if (match!=null) {
					bestMatchForExpected.put(el, match);
					HashSet<PartialClassification> useList=useListForResultElement.get(match.getFirst());
					if (useList==null) useListForResultElement.put(match.getFirst(),useList=new HashSet<PartialClassification>());
					useList.add(el);
				} else {
					finalPairings.put(el, null);
				}
			}
			for(PartialClassification el:finalPairings.keySet()) {bestMatchForExpected.remove(el);}

			Stack<PartialClassification> stackMatches=new Stack<PartialClassification>();
			stackMatches.addAll(bestMatchForExpected.keySet());
			while(!stackMatches.isEmpty()) {
				PartialClassification el=stackMatches.pop();
				//expected result (el) -> <result, coverage> (match)
				Pair<PartialClassification, Float> match=bestMatchForExpected.get(el);
				// use list is a set of expected result nodes that are matched to the same match.getFirst()
				HashSet<PartialClassification> useList=useListForResultElement.get(match.getFirst());

				float bestMatchValueNONNULL=0;
				PartialClassification bestMatchNONNULL=null;
				float bestMatchValueNULL=0;
				PartialClassification bestMatchNULL=null;
				float bestMatchValue=match.getSecond();
				PartialClassification bestMatch=el;

				if (useList!=null && (useList.size()>1)) {
					// the use list contains expected results
					for(PartialClassification oEl:useList) {
						stackMatches.remove(oEl);
						Pair<PartialClassification, Float> oMatch=bestMatchForExpected.get(oEl);

						if (oMatch==null || match.getFirst()!=oMatch.getFirst()) throw new Exception("error in algo 4");

						boolean expectedIsNULL=oEl.getTopResult().getId()==null;
						if (expectedIsNULL) {
							if (oMatch.getSecond()>bestMatchValueNULL) {
								bestMatchValueNULL=oMatch.getSecond();
								bestMatchNULL=oEl;
							}
						} else {
							if (oMatch.getSecond()>bestMatchValueNONNULL) {
								bestMatchValueNONNULL=oMatch.getSecond();
								bestMatchNONNULL=oEl;
							}
						}
					}
					if (bestMatchNONNULL!=null) {
						bestMatch=bestMatchNONNULL;
					} else {
						if (bestMatchNULL==null) throw new Exception("algo error 3.");
						bestMatch=bestMatchNULL;
					}
				}
				finalPairings.put(bestMatch, new Pair<PartialClassification, Float>(match.getFirst(),bestMatchValue));
				if (!remainingResults.contains(match.getFirst())) throw new Exception("Error with algo2.");
				remainingResults.remove(match.getFirst());
			}
			for(PartialClassification el:finalPairings.keySet()) {bestMatchForExpected.remove(el);}
		}
		
		float accuracy=0;
		int positiveMatches=0;
		
		PerformanceResult edgeDiffs=new PerformanceResult();
		
		for(PartialClassification el:finalPairings.keySet()) {
			Pair<PartialClassification, Float> bestMatch=finalPairings.get(el);
			NLUOutput oSA=(bestMatch!=null)?bestMatch.getFirst().getTopResult():null;
			NLUOutput sa=el.getTopResult();
			if ((sa.getId()==null && oSA==null) || ((oSA!=null) && (sa.getId()!=null) && (sa.getId().equals(oSA.getId())))) {
				accuracy+=(bestMatch!=null)?bestMatch.getSecond():1;
				edgeDiffs.addMeasure(getExtremesDiffs(el,bestMatch.getFirst()));
				positiveMatches++;
			}
		}
		if (positiveMatches>expectedResult.size()) throw new Exception("error in counting: "+expectedResult+" "+result);
		if (positiveMatches>result.size()) throw new Exception("error in counting 2: "+expectedResult+" "+result);
		
		//return edgeDiffs;
		//return new PerformanceResult(accuracy, (float)expectedResult.size(), (float)result.size());
		return new PerformanceResult(positiveMatches, (float)expectedResult.size(), (float)result.size());
	}
	private float computeCoverage(PartialClassification p1,PartialClassification p2) {
		NLUOutput nlu1=p1.getTopResult(),nlu2=p2.getTopResult();
		if (nlu1!=null && nlu2!=null) {
			int intersection=computeIntersection(p1, p2);
			int el1Length=p1.getEnd()-p1.getStart();
			int el2Length=p2.getEnd()-p2.getStart();
			float cv1=(float)intersection/(float)el1Length;
			float cv2=(float)intersection/(float)el2Length;
			return (cv1+cv2)*0.5f;
		} else return 0;
	}
	private float getExtremesDiffs(PartialClassification p1,PartialClassification p2) {
		return (float)(Math.abs(p1.getStart()-p2.getStart())+Math.abs(p1.getEnd()-p2.getEnd()))/2f;
	}

	public void trainChartNLUOnThisData(List<TrainingDataFormat> training,File trainingFile, File modelFile, BuildTrainingData btd) throws Exception {
		// dumping training data to file
		dumpTrainingDataToFileNLUFormat(trainingFile,training);
		// using the file created above to train a model
		internalNLU.train(trainingFile, modelFile);
	}

	
	private Collection<PartialClassification> classify(String text, Set<String> possibleNLUOutputIDs) throws Exception {
		return runChartClassifier2(text,possibleNLUOutputIDs,true,getConfiguration().getChartNluInSingleMode());
	}

	@Override
	public List<NLUOutput> getNLUOutput(String text,Set<String> possibleNLUOutputIDs,Integer nBest) throws Exception {
		String emptyEvent=getConfiguration().getEmptyTextEventName();
		if (StringUtils.isEmptyString(text) && !StringUtils.isEmptyString(emptyEvent)) {
			List<NLUOutput> ret=new ArrayList<NLUOutput>();
			ret.add(new NLUOutput(text, emptyEvent, 1, null));
			return ret;
		}
		Collection<PartialClassification> out = classify(text,possibleNLUOutputIDs);
		List<NLUOutput> ret = new ArrayList<NLUOutput>();
		if (out!=null && !out.isEmpty()) {
			ret.add(new ChartNLUOutput(text, out));
		} else {
			String lowConfidenceEvent=getConfiguration().getLowConfidenceEvent();
			if (StringUtils.isEmptyString(lowConfidenceEvent)) {
				getLogger().warn(" no user speech acts left and LOW confidence event disabled, returning no NLU results.");
			} else {
				ret.add(new NLUOutput(text,lowConfidenceEvent, 1f, null));
				getLogger().warn(" no user speech acts left. adding the low confidence event.");
			}
		}
		return ret;
	}
	@Override
	public List<NLUOutput> getNLUOutputFake(String[] NLUOutputIDs, String text)
			throws Exception {
		return internalNLU.getNLUOutputFake(NLUOutputIDs, text);
	}
	@Override
	public Map<String, Object> getPayload(String sa, String text)
			throws Exception {
		return internalNLU.getPayload(sa, text);
	}
	
	@Override
	public void kill() throws Exception {
		internalNLU.kill();
	}
	
	@Override
	public void retrain() throws Exception {
		NLUConfig c=getConfiguration();
		BuildTrainingData btd=getBTD();

		File trainingFile=new File(c.getNluTrainingFile());

		/*
		// read all the tacq data available
		//bunchTacqXMLTrainingData(); // builds the all-data.xml
		ArrayList<TacqData> newData= getAllTacqDataFromDefaultLocation();
		ArrayList<Pair<String, String>> td = btd.convertTacqDataToSimpleTrainingData(newData);
		printTacqTrainingDataStats(newData);
		*/
		List<TrainingDataFormat> td=btd.buildTrainingData();
		dumpTrainingDataToFileNLUFormat(trainingFile,td);

		train(trainingFile, new File(c.getNluModelFile()));
	}
	@Override
	public void loadModel(File nluModel) throws Exception {
		internalNLU.loadModel(nluModel);
	}
	@Override
	public void train(File input, File model) throws Exception {
		boolean printErrors=getConfiguration().getPrintNluErrors();

		internalNLU.getConfiguration().setPrintNluErrors(false);
		internalNLU.train(input, model);
		/*
		BuildTrainingData btd=getBTD();
		List<TrainingDataFormat> td=btd.buildTrainingDataFromNLUFormatFile(input);
        td=btd.cleanTrainingData(td);

		return testNLUOnThisData(td, model, printErrors);*/
	}
	@Override
	public void train(List<TrainingDataFormat> input, File model) throws Exception {
		boolean printErrors=getConfiguration().getPrintNluErrors();
		internalNLU.getConfiguration().setPrintNluErrors(false);
		internalNLU.train(input, model);
	}

	@Override
	public PerformanceResult test(File test, File model, boolean printErrors)
			throws Exception {
		BuildTrainingData btd=getBTD();
		List<TrainingDataFormat> td=btd.buildTrainingDataFromNLUFormatFile(test);

        td=btd.cleanTrainingData(td);
        
		return testNLUOnThisData(td, model, printErrors);
	}
	@Override
	public PerformanceResult test(List<TrainingDataFormat> test, File model,boolean printErrors) throws Exception {
		return testNLUOnThisData(test, model, printErrors);
	}

	@Override
	public boolean isPossibleNLUOutput(NLUOutput o) throws Exception {
		return internalNLU.isPossibleNLUOutput(o);
	}

	@Override
	public Set<String> getAllSimplifiedPossibleOutputs() throws Exception {
		Set<String> ret = internalNLU.getAllSimplifiedPossibleOutputs();
		Map<String, String> hardLinks = getHardLinksMap();
		if (hardLinks!=null) for(String label:hardLinks.values()) {
			if (ret==null) ret=new HashSet<String>();
			ret.add(label);
		}
		return ret;
	}

}
