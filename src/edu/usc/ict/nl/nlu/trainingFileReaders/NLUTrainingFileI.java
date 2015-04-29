package edu.usc.ict.nl.nlu.trainingFileReaders;

import java.io.File;
import java.util.List;

import edu.usc.ict.nl.nlu.TrainingDataFormat;

public interface NLUTrainingFileI {
	List<TrainingDataFormat> getTrainingInstances(File input) throws Exception;
	TrainingDataFormat getNextTrainingInstance(File input) throws Exception;
}
