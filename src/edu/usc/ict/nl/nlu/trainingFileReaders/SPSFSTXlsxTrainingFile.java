package edu.usc.ict.nl.nlu.trainingFileReaders;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.fst.sps.SAMapper;
import edu.usc.ict.nl.nlu.fst.train.Aligner;

public class SPSFSTXlsxTrainingFile implements NLUTrainingFileI {

	/**
	 * label		utterance	utterance_type	predicate	predicate_modifier	object	object_modifier	time	temporal_complement	location	source
question.ros.general-health-constitutional.fever.fever-general		so do you have a temperature	yesno			symptom:fever					
	   label		utterance	type			subtype		what				subwhat	quality			location	when	cause	consequence
General Health & Constitutional.Illness.Illness - General	[describe] illness	can you describe your illness?	describe		illness						
	 */
	@Override
	public List<TrainingDataFormat> getTrainingInstances(File input) throws Exception {
		List<TrainingDataFormat> itds = Aligner.extractTrainingDataFromSingleStep1and3GoogleXLSXForSPS(input);
		if (itds!=null) {
			for(TrainingDataFormat td:itds) td.setLabel(SAMapper.convertSA(td.getId()));
		}
		return itds;
	}

	
	List<TrainingDataFormat> itds=null;
	Iterator<TrainingDataFormat> it=null;
	@Override
	public TrainingDataFormat getNextTrainingInstance(File input) throws Exception {
		if (itds==null) itds=getTrainingInstances(input);
		if (itds!=null) {
			it=itds.iterator();
		}
		if (it.hasNext()) return it.next();
		return null;
	}

}
