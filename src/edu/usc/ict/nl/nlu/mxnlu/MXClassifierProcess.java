package edu.usc.ict.nl.nlu.mxnlu;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.usc.ict.nl.nlu.NLUProcess;
import edu.usc.ict.nl.util.StreamGobbler;


public class MXClassifierProcess extends NLUProcess {
	private Process p;
	private ProcessBuilder pb;
	public BufferedReader from;
	public BufferedReader errorFrom;
	public OutputStream to;
	private Long id=0l;
	private String exeFile;
	private String model=null;
	private Map<String,String> env;
	
	public MXClassifierProcess(String exe, Map<String,String> env) {
		this.exeFile=exe;
		this.env = env == null ? Collections.<String, String>emptyMap() : env;
	}

	public Set<String> getAllSimplifiedPossibleOutputs() {
		return null;
	}

	public List<String> buildCommandLineFromOptions(String model,String trainingFile,Integer nBest) {
		List<String> command = new ArrayList<String>();
		if (trainingFile!=null) {
			command.add(exeFile);
			command.add("-t");
			command.add(model);
			command.add(trainingFile);
		} else {
			command.clear();
			command.add(exeFile);
			command.add("-r");
			command.add("-n "+nBest);
			command.add(model);
		}
		return command;
	}
	
	@Override
	public void run(String model, int nb) throws Exception {
		this.nBest=nb;
		this.model=model;
		List<String> command = buildCommandLineFromOptions(model, null, nb);
		//System.out.println("COMMAND: "+command);
		//command.add("C:\\cygwin\\bin\\cat.exe");
		//System.out.println(command);
		pb = new ProcessBuilder(command);
		pb.environment().putAll(env);
		//pb.directory(new File(baseDirectory).getAbsoluteFile());
		p = pb.start();
		from = new BufferedReader(new InputStreamReader(p.getInputStream()));
		errorFrom = new BufferedReader(new InputStreamReader(p.getErrorStream()));
		to = p.getOutputStream();
	}
	
	@Override
	public void train(String model, String trainingFile) throws Exception {
		this.model=model;
		List<String> command = buildCommandLineFromOptions(model, trainingFile, null);
		//command.add("C:\\cygwin\\bin\\cat.exe");
		//System.out.println(command);
		ProcessBuilder pb = new ProcessBuilder(command);
		pb.environment().putAll(env);
		//pb.directory(new File(baseDirectory).getAbsoluteFile());
		Process p = pb.start();
		StreamGobbler output = new StreamGobbler(p.getInputStream(), "output",true,false);
		StreamGobbler error = new StreamGobbler(p.getErrorStream(), "error",true,false);
		output.start();
		error.start();
		output.join();
		error.join();
		p.destroy();
	}
	
	@Override
	public void kill() {
		if (p!=null) p.destroy();
	}
	
	@Override
	public String[] classify(String u,int nBest) throws Exception {
		if (nBest!=this.nBest) {
			kill();
			run(model, nBest);
		}
		String line;
		// first send ID as expected by MXNLU
		to.write(((id++).toString()+"\n").getBytes());
		to.flush();
		// then pack the utterance with the <s> and </s> delimiters and send that
		to.write((u+"\n").getBytes());
		to.flush();
		line = from.readLine();
		// first line received from program
		// this line should contain a number that is the same as id-1
		if ((line==null) && ((line=errorFrom.readLine())!=null)) {
			System.out.println(line);
			return null;
		} else if (Long.parseLong(line)!=(id-1)) {
			return null;
		} else if((line = from.readLine()) != null){
			int gotnBest=Integer.parseInt(line);
			String[] ret=new String[gotnBest];
			for (int i=0;i<gotnBest;i++) {
				while(!from.ready()) {Thread.sleep(100);}
				// second+ line received is the result of the classification
				ret[i] = from.readLine();
				//System.out.println(ret[i]);
			} 
			return ret;
		} else {
			return null;
		}
	}
	
	public static void main(String[] args) {
/*
		try {
			MXClassifierProcess nluP = new MXClassifierProcess("../../lib/mxClassifier/Release/mxClassifier-win32.exe","../chaos-nlu-dm/chaos-mxnlu-model",1);
			for (String out:nluP.sendUtteranceGetResult("we need to move the clinic")) System.out.println(out);
			nluP = new MXClassifierProcess("../../lib/mxClassifier/Release/mxClassifier-win32.exe","../chaos-nlu-dm/chaos-mxnlu-model","../chaos-nlu-dm/chaos-mxnlu-training.txt");
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}
		
}
