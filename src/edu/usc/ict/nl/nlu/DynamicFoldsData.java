package edu.usc.ict.nl.nlu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.usc.ict.nl.util.Rational;


public class DynamicFoldsData extends FoldsData {

	/**
	 * list of speech acts
	 *  list of folds
	 *   list of trianing data for that fold for that speech act
	 */
	List<List<List<TrainingDataFormat>>> dynamicData=null;
	
	public DynamicFoldsData(List<TrainingDataFormat> tds, int folds) {
		Map<String, List<TrainingDataFormat>> saWithTds = BuildTrainingData.getAllSpeechActsWithTrainingData(tds);
		for(String sa:saWithTds.keySet()) {
			List<TrainingDataFormat> saTds=saWithTds.get(sa);
			Rational p=new Rational(saTds.size(), folds);
			if (dynamicData==null) dynamicData=new ArrayList<List<List<TrainingDataFormat>>>();
			List<List<TrainingDataFormat>> saData=new ArrayList<List<TrainingDataFormat>>();
			dynamicData.add(saData);
			if (p.getResult()<1) {
				for(TrainingDataFormat td:saTds) {
					List<TrainingDataFormat> foldForSa;
					saData.add(foldForSa=new ArrayList<TrainingDataFormat>());
					foldForSa.add(td);
				}
				this.folds=Math.max(this.folds,saTds.size());
			} else {
				float pf=p.getResult();
				float i=0;
				for(TrainingDataFormat td:saTds) {
					int fold=(int)Math.floor(i/pf);
					if (fold>=saData.size()) {
						for(int j=saData.size();j<=fold;j++) saData.add(new ArrayList<TrainingDataFormat>());
					}
					List<TrainingDataFormat> foldForSa=saData.get(fold);
					foldForSa.add(td);
					i++;
				}
				assert(saData.size()==folds);
				this.folds=Math.max(this.folds,saData.size());
			}
		}
	}

	@Override
	public List<TrainingDataFormat> buildTrainingDataForFold(int currentFold,List<TrainingDataFormat> trainingData) {
		if (trainingData==null) trainingData=new ArrayList<TrainingDataFormat>();
		else trainingData.clear();
		for(List<List<TrainingDataFormat>> foldsForSA:dynamicData) {
			int saFolds=foldsForSA.size();
			for(int j=0;j<saFolds;j++) if (j!=currentFold) trainingData.addAll(foldsForSA.get(j));
		}
		return trainingData;
	}

	@Override
	public List<TrainingDataFormat> buildTestingDataForFold(int currentFold,List<TrainingDataFormat> testingData) {
		if (testingData==null) testingData=new ArrayList<TrainingDataFormat>();
		else testingData.clear();
		for(List<List<TrainingDataFormat>> foldsForSA:dynamicData) {
			int saFolds=foldsForSA.size();
			if (currentFold<saFolds) testingData.addAll(foldsForSA.get(currentFold));
		}
		return testingData;
	}

}
