package edu.usc.ict.nl.nlu.svm;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.nlu.trainingFileReaders.MXNLUTrainingFile;

public class SVMModelAndDictionaries {
	private Map<String,Integer> outputClassDictionary=null;
	private Map<Integer,String> outputClassDictionaryInverse=null;
	private Map<String,Integer> featuresDictionary=null;
	private Map<Integer,String> featuresDictionaryInverse=null;
	private svm_model classifier=null;
	private svm_parameter param=initParams();

	private XStream outputCenverter=new XStream(new StaxDriver());
	private LibSVMNLU nlu=null;

	private svm_parameter initParams() {
		svm_parameter param = new svm_parameter();
		// default values
		param.svm_type = svm_parameter.C_SVC;
		param.kernel_type = svm_parameter.RBF;
		param.degree = 3;
		param.gamma = 0;	// 1/num_features
		param.coef0 = 0;
		param.nu = 0.5;
		param.cache_size = 100;
		param.C = 10000000;
		param.eps = 1e-3;
		param.p = 0.1;
		param.shrinking = 1;
		param.probability = 0;
		param.nr_weight = 0;
		param.weight_label = new int[0];
		param.weight = new double[0];
		return param;
	}

	public SVMModelAndDictionaries(LibSVMNLU nlu,File model) throws Exception {
		this(nlu);
		loadModel(model);
	}

	public SVMModelAndDictionaries(LibSVMNLU nlu) {
		this.nlu=nlu;
	}


	private static <Ti,To> Map<Ti,To> invertMap(Map<To,Ti> input) {
		Map<Ti,To> ret=null;
		if (input!=null) {
			for(To key:input.keySet()) {
				if (ret==null) ret=new HashMap<Ti, To>();
				Ti value=input.get(key);
				ret.put(value, key);
			}
		}
		return ret;
	}

	private Object loadObject(File p) {
		return outputCenverter.fromXML(p);
	}
	public svm_model loadSVMClassifier(File serializedFile) {
		svm_model classifier=null;

		try {
			classifier = svm.svm_load_model(serializedFile.getAbsolutePath());
		} catch (FileNotFoundException e) {
			System.err.println("Classifier model file does not exist: "+serializedFile);
			return null;
		} catch (Exception e) {
			System.err.println("Error loading model: "+serializedFile);
			e.printStackTrace();
		}

		return classifier;
	}
	enum STATE {SVM,OUTPUTCLASSES,FEATURES,END};
	private void loadModel(File model) throws Exception {
		BufferedReader in=new BufferedReader(new FileReader(model));
		String line;
		STATE readingState=STATE.SVM;
		while((line=in.readLine())!=null) {
			File p=null;
			switch (readingState) {
			case SVM:
				p=new File(line);
				this.classifier=loadSVMClassifier(p);
				readingState=STATE.OUTPUTCLASSES;
				break;
			case OUTPUTCLASSES:
				p=new File(line);
				this.outputClassDictionary=(Map<String, Integer>) loadObject(p);
				this.outputClassDictionaryInverse=invertMap(outputClassDictionary);
				readingState=STATE.FEATURES;
				break;
			case FEATURES:
				p=new File(line);
				this.featuresDictionary=(Map<String, Integer>) loadObject(p);
				this.featuresDictionaryInverse=invertMap(featuresDictionary);
				readingState=STATE.END;
				break;
			case END:
				break;
			}
			if (readingState==STATE.END) break;
		}
		in.close();
	}

	public Integer getIdOfFeature(String f) {
		if (featuresDictionary!=null) return featuresDictionary.get(f);
		else return null;
	}
	public svm_model getSVMClassifier() {
		return classifier;
	}
	public String getOutputClassForId(int c) {
		if (outputClassDictionaryInverse!=null) return outputClassDictionaryInverse.get(c);
		else return null;
	}

	public Set<String> getAllSimplifiedPossibleOutputs() {
		if (outputClassDictionary!=null) return outputClassDictionary.keySet();
		else return null;
	}

	public static SVMModelAndDictionaries train(LibSVMNLU nlu,String trainingFileName) throws Exception {
		File trainingFile=new File(trainingFileName);
		SVMModelAndDictionaries ret = new SVMModelAndDictionaries(nlu);
		ret.outputClassDictionary=new HashMap<String,Integer>();
		ret.featuresDictionary=new HashMap<String,Integer>();
		List<TrainingDataFormat> tds=ret.updateDictionariesFromTrainingData(trainingFile);
		ret.outputClassDictionaryInverse=invertMap(ret.outputClassDictionary);
		ret.featuresDictionaryInverse=invertMap(ret.featuresDictionary);
		File newTrainignFile=ret.convertTrainingDataToSVMFormat(tds,File.createTempFile("svm-training-data-", ".train",trainingFile.getParentFile()));
		ret.classifier = svm.svm_train(ret.readSVMTrainingFile(newTrainignFile), ret.param);
		return ret;
	}

	private List<TrainingDataFormat> updateDictionariesFromTrainingData(File trainingFile) throws Exception {
		MXNLUTrainingFile reader=new MXNLUTrainingFile();
		List<TrainingDataFormat> tds = reader.getTrainingInstances(trainingFile);

		Integer outputClassCounter=getMaxInteger(outputClassDictionary);
		Integer featuresCounter=getMaxInteger(featuresDictionary);
		if (outputClassCounter==null) outputClassCounter=1;
		if (featuresCounter==null) featuresCounter=1;

		if (tds!=null) {
			if (outputClassDictionary==null) outputClassDictionary=new HashMap<String, Integer>();
			if (featuresDictionary==null) featuresDictionary=new HashMap<String, Integer>();
			for(TrainingDataFormat td:tds) {
				String sa=td.getLabel();
				if (!outputClassDictionary.containsKey(sa)) {
					outputClassDictionary.put(sa, outputClassCounter);
					outputClassCounter++;
				}
				String text=nlu.doPreprocessingForClassify(td.getUtterance());
				String[] features=text.split("[\\s]+");
				for(String f:features) {
					if (!featuresDictionary.containsKey(f)) {
						featuresDictionary.put(f,featuresCounter);
						featuresCounter++;
					}
				}
			}
		}
		return tds;
	}

	public File convertTrainingDataToSVMFormat(List<TrainingDataFormat> tds,File output) throws Exception {
		if (output!=null && tds!=null && outputClassDictionary!=null && featuresDictionary!=null) {
			BufferedWriter out=new BufferedWriter(new FileWriter(output));
			for(TrainingDataFormat td:tds) {
				String sa=td.getLabel();
				String text=nlu.doPreprocessingForClassify(td.getUtterance());
				String[] features=text.split("[\\s]+");
				String line=writeSVMLine(outputClassDictionary.get(sa),features,featuresDictionary);
				out.write(line+"\n");
			}
			out.close();
		}
		return output;
	}
	
	private Integer getMaxInteger(Map<?,Integer> dictionary) {
		Integer ret=null;
		if (dictionary!=null) {
			for(Integer i:dictionary.values()) {
				if (ret==null || i>ret) ret=i;
			}
		}
		return ret;
	}
	
	private String writeSVMLine(Integer outputClass, String[] features,Map<String, Integer> featuresDictionary) {
		StringBuffer ret=new StringBuffer();
		ret.append(outputClass);
		for(String f:features) {
			ret.append("\t"+featuresDictionary.get(f)+":1");
		}
		return ret.toString();
	}

	public void save(String model) throws IOException {
		File modelFile=new File(model);
		BufferedWriter out=new BufferedWriter(new FileWriter(modelFile));
		File tmp=File.createTempFile("svmModel", ".model", modelFile.getParentFile());
		svm.svm_save_model(tmp.getAbsolutePath(),classifier);
		out.write(tmp.getAbsolutePath()+"\n");
		tmp=File.createTempFile("outputClassesDictionary", ".model", modelFile.getParentFile());
		outputCenverter.toXML(outputClassDictionary, new FileWriter(tmp));
		out.write(tmp.getAbsolutePath()+"\n");
		tmp=File.createTempFile("featuresDictionary", ".model", modelFile.getParentFile());
		outputCenverter.toXML(featuresDictionary, new FileWriter(tmp));
		out.write(tmp.getAbsolutePath()+"\n");
		out.close();
	}

	public svm_problem readSVMTrainingFile(File input) throws IOException {
		BufferedReader fp = new BufferedReader(new FileReader(input));
		Vector<Double> vy = new Vector<Double>();
		Vector<svm_node[]> vx = new Vector<svm_node[]>();
		int max_index = 0;

		while(true)
		{
			String line = fp.readLine();
			if(line == null) break;

			StringTokenizer st = new StringTokenizer(line," \t\n\r\f:");

			vy.addElement(atof(st.nextToken()));
			int m = st.countTokens()/2;
			svm_node[] x = new svm_node[m];
			for(int j=0;j<m;j++)
			{
				x[j] = new svm_node();
				x[j].index = atoi(st.nextToken());
				x[j].value = atof(st.nextToken());
			}
			if(m>0) max_index = Math.max(max_index, x[m-1].index);
			vx.addElement(x);
		}

		svm_problem prob = new svm_problem();
		prob.l = vy.size();
		prob.x = new svm_node[prob.l][];
		for(int i=0;i<prob.l;i++)
			prob.x[i] = vx.elementAt(i);
		prob.y = new double[prob.l];
		for(int i=0;i<prob.l;i++)
			prob.y[i] = vy.elementAt(i);

		if(param.gamma == 0 && max_index > 0)
			param.gamma = 1.0/max_index;

		if(param.kernel_type == svm_parameter.PRECOMPUTED)
			for(int i=0;i<prob.l;i++)
			{
				if (prob.x[i][0].index != 0)
				{
					System.err.print("Wrong kernel matrix: first column must be 0:sample_serial_number\n");
					System.exit(1);
				}
				if ((int)prob.x[i][0].value <= 0 || (int)prob.x[i][0].value > max_index)
				{
					System.err.print("Wrong input format: sample_serial_number out of range\n");
					System.exit(1);
				}
			}

		fp.close();
		return prob;
	}
	private static int atoi(String s)
	{
		return Integer.parseInt(s);
	}
	private static double atof(String s)
	{
		double d = Double.valueOf(s).doubleValue();
		if (Double.isNaN(d) || Double.isInfinite(d))
		{
			System.err.print("NaN or Infinity in input\n");
			System.exit(1);
		}
		return(d);
	}
}
