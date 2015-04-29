package edu.usc.ict.nl.nlu.trainingFileReaders;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.util.StringUtils;

public class MXNLUTrainingFile implements NLUTrainingFileI {

	protected boolean hasFeatures=true;
	
	private static enum NLUTrainingDataState {TEXT,FEATURES,LABEL,INLABEL};
	private static final Pattern nluTrainingFormat=Pattern.compile("^[\\s]*<s>(.*)</s>[\\s]*$");
	private BufferedReader inp=null;
	NLUTrainingDataState state=NLUTrainingDataState.TEXT;
	
	public void resetReaderToFile(File inputFile) throws FileNotFoundException {
		inp=new BufferedReader(new FileReader(inputFile));
	}
	public void resetReaderToReader(Reader reader) {
		inp=new BufferedReader(reader);
	}
	
	@Override
	public TrainingDataFormat getNextTrainingInstance(File input) throws Exception {
		if (inp==null)
			inp=new BufferedReader(new FileReader(input));
		return getNextTrainingInstance();
	}
	public TrainingDataFormat getNextTrainingInstance() throws Exception {
		if (inp!=null) {
			String line;
			String input=null,nluOutput=null,features=null;
			try {
				while(inp.ready() && (line=inp.readLine())!=null) {
					line=StringUtils.cleanupSpaces(line);
					switch (state) {
					case TEXT:
						if (!StringUtils.isEmptyString(line)) {
							Matcher m=nluTrainingFormat.matcher(line);
							if (m.matches() && (m.groupCount()==1)) {
								line=StringUtils.cleanupSpaces(m.group(1));
								input=line;
								if (StringUtils.isEmptyString(input)) throw new Exception("empty line in training data.");
								if (nluOutput!=null) throw new Exception("state error in readNLUTrainingFormatFile (non null output).");
								if (hasFeatures)
									state=NLUTrainingDataState.FEATURES;
								else
									state=NLUTrainingDataState.LABEL;
							} else {
								throw new Exception("invalid text line: '"+line+"'");
							}
						}
						break;
					case FEATURES:
						features=line;
						state=NLUTrainingDataState.LABEL;
						break;
					case LABEL:
						if (!StringUtils.isEmptyString(line)) {
							if (input==null) throw new Exception("state error in readNLUTrainingFormatFile (null input).");
							nluOutput=line;
							state=NLUTrainingDataState.INLABEL;
						}
						break;
					case INLABEL:
						if (!StringUtils.isEmptyString(line)) {
							nluOutput+="||"+line;
						} else {
							TrainingDataFormat next=new TrainingDataFormat(input, nluOutput);
							next.setFeatures(features);
							input=null;nluOutput=null;features=null;
							state=NLUTrainingDataState.TEXT;
							return next;
						}
					}
				}
				inp.close();
			} catch (Exception e) {
				inp.close();
				throw e;
			}
		}
		return null;
	}
	
	@Override
	public List<TrainingDataFormat> getTrainingInstances(File fileToRead) throws Exception {
		List<TrainingDataFormat> td=new ArrayList<TrainingDataFormat>();
		inp=new BufferedReader(new FileReader(fileToRead));
		TrainingDataFormat instance=null;
		while((instance=getNextTrainingInstance())!=null) {
			td.add(instance);
		}
		return td;
	}

	public boolean getHasFeatures() {return hasFeatures;}
	public void setHasFeatures(boolean h) {this.hasFeatures=h;}
	

}
