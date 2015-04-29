package edu.usc.ict.nl.nlu.svm;

import java.io.File;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.Model;
import edu.usc.ict.nl.nlu.NLUProcess;
import edu.usc.ict.nl.nlu.jmxnlu.JMXClassifierNLU;


public class LibSVMNLU extends JMXClassifierNLU {


	public LibSVMNLU(NLUConfig c) throws Exception {
		super(c);
	}
	
	@Override
	public NLUProcess startMXNLUProcessWithTheseParams(String model,int nbest) throws Exception {
		NLUProcess p = new SvmClassifierProcess(this);
		try {
			p.run(model, nbest);
		} catch (Exception e) {
			logger.warn(e);
		}
		return p;
	}
	
	@Override
	public Model readModelFileNoCache(File mf) throws Exception {
		Model ret=null;
		return ret;
	}
}
