package edu.usc.ict.nl.nlu.opennlp;

import java.io.File;

import opennlp.maxent.DataStream;
import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.trainingFileReaders.MXNLUTrainingFile;
import edu.usc.ict.nl.util.StringUtils;

public class NLUTrainingFileReader implements DataStream {

	private MXNLUTrainingFile dataReader=null;
	private TrainingDataFormat next=null;

	public NLUTrainingFileReader(File input) {
		dataReader = new MXNLUTrainingFile();
		try {
			next = dataReader.getNextTrainingInstance(input);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Object nextToken() {
		return convertToString(nextTrainingData());
	}
	
	public TrainingDataFormat nextTrainingData() {
		TrainingDataFormat current=next;
		try {
			next=dataReader.getNextTrainingInstance();
		} catch (Exception e) {
			next=null;
			e.printStackTrace();
		}
		return current;
	}
	
	@Override
	public boolean hasNext() {
		return next!=null;
	}
	
	public String convertToString(TrainingDataFormat td) {
		if (td!=null) {
			String fs=td.getFeatures();
			if (StringUtils.isEmptyString(fs)) fs=td.getUtterance();
			return fs+" "+td.getLabel();
		}
		return null;
	}
	
	

}
