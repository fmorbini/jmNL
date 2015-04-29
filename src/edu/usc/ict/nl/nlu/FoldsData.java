package edu.usc.ict.nl.nlu;

import java.util.ArrayList;
import java.util.List;

public class FoldsData {
	int folds=0;
	List<List<TrainingDataFormat>> data=null;
	
	public FoldsData() {}
	
	public FoldsData(List<TrainingDataFormat> tds, int folds) {
		int i=0;
		for(TrainingDataFormat td:tds) {
			addDataToFold(td, i);
			i=(++i)%folds;
		}
		assert(folds==this.folds);
	}

	public void addDataToFold(TrainingDataFormat td,int fold) {
		if (data==null) data=new ArrayList<List<TrainingDataFormat>>();
		if (fold>=data.size()) {
			for(int i=data.size();i<=fold;i++) data.add(new ArrayList<TrainingDataFormat>());
			folds=data.size();
		}
		data.get(fold).add(td);
	}

	public Integer getNumberOfFolds() {
		return folds;
	}

	public List<TrainingDataFormat> buildTrainingDataForFold(int currentFold) {
		return buildTrainingDataForFold(currentFold, null);
	}
	public List<TrainingDataFormat> buildTrainingDataForFold(int currentFold,List<TrainingDataFormat> trainingData) {
		if (trainingData==null) trainingData=new ArrayList<TrainingDataFormat>();
		else trainingData.clear();
		for(int j=0;j<folds;j++) if (currentFold!=j) trainingData.addAll(data.get(j));
		return trainingData;
	}

	public List<TrainingDataFormat> buildTestingDataForFold(int currentFold) {
		return buildTestingDataForFold(currentFold, null);
	}
	public List<TrainingDataFormat> buildTestingDataForFold(int currentFold,List<TrainingDataFormat> testingData) {
		if (testingData==null) return data.get(currentFold);
		else {
			testingData.clear();
			testingData.addAll(data.get(currentFold));
			return testingData;
		}
	}
}
