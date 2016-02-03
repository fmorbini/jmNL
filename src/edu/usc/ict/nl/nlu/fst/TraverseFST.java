package edu.usc.ict.nl.nlu.fst;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.io.BuildTrainingData;
import edu.usc.ict.nl.nlu.preprocessing.TokenizerI;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.Rational;
import edu.usc.ict.nl.util.StreamGobbler;
import edu.usc.ict.nl.util.graph.Edge;
import edu.usc.ict.nl.util.graph.GraphElement;
import edu.usc.ict.nl.util.graph.Node;
import edu.usc.ict.nl.utils.LogConfig;

public class TraverseFST {
	
	private File in=null;
	private File out=null;
	private File model=null;
	private static final Logger logger = Logger.getLogger(TraverseFST.class.getName());
	private String[] fstCmd;
	private NLUConfig config;
	//static String[] defaultCmd = {"/bin/bash","-c","fstcompile --isymbols=input.syms --osymbols=input.syms|fstcompose - alignments.fst|fstshortestpath --nshortest=5 |fstprint --isymbols=input.syms --osymbols=output.syms"};
	//static String[] defaultCmd = "{"C:\\cygwin\\bin\\bash","-c","export PATH=$PATH:/bin:/usr/local/bin; fstcompile.exe --isymbols=input.syms --osymbols=input.syms|fstcompose.exe - alignments.fst|fstshortestpath.exe --nshortest="+nBest+" |fstprint.exe --isymbols=input.syms --osymbols=output.syms"};
	static String[] defaultCmd ={"C:\\cygwin\\bin\\bash", "-c", "export PATH=$PATH:/bin:/usr/local/bin;fstcompile --isymbols=%IN% --osymbols=%IN%|fstcompose - alignments.fst|fstshortestpath --nshortest=%NBEST% |fstprint --isymbols=%IN% --osymbols=%OUT%"};
	
	static {
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );
	}

	public TraverseFST() throws Exception {
		this(new File("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\resources\\characters\\Base-All\\nlu\\input.syms"),
				new File("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\resources\\characters\\Base-All\\nlu\\output.syms"),
				new File("C:\\Users\\morbini\\simcoach_svn\\trunk\\core\\NLModule\\resources\\characters\\Base-All\\nlu\\alignments.fst"),
				defaultCmd,
				NLUConfig.WIN_EXE_CONFIG);
	}
	public TraverseFST(File iSyms,File oSyms, File fst, String[] cmd,NLUConfig config) throws Exception {
		this.config=config;
		in=iSyms;
		out=oSyms;
		model=fst;
		fstCmd = new String[cmd.length];
		for(int i=0;i<fstCmd.length;i++) {
			fstCmd[i] = cmd[i];
			fstCmd[i] = fstCmd[i].replaceAll("%IN%",in.getName());
			fstCmd[i] = fstCmd[i].replaceAll("%OUT%",out.getName());
			fstCmd[i] = fstCmd[i].replaceAll("%MODEL%",model.getName());
		}

		File inP=in.getParentFile(),outP=out.getParentFile(),modelP=model.getParentFile();
		if (!((inP==outP && inP==modelP) ||
				(inP.equals(outP) && inP.equals(modelP))))
			throw new Exception("input, output and model files need to be in the same directory."+
					"\n"+in+
					"\n"+out+
					"\n"+model); 
	}
	
	public NLUConfig getConfiguration() {
		return config;
	}
	
	public File getInputSymbols() {return in;}
	public File getOutputSymbols() {return out;}
	
	public static Map<Integer,String> openSymbols(File symbols) throws Exception {
		Map<Integer,String> ret=new HashMap<Integer, String>();
		BufferedReader in=new BufferedReader(new FileReader(symbols));
		String line;
		int lineCount=1;
		while((line=in.readLine())!=null) {
			String[] portions=line.split("[\\s]+");
			if (portions!=null && portions.length==2) {
				int symID=Integer.parseInt(portions[1]);
				if (ret==null) ret=new HashMap<Integer, String>();
				ret.put(symID,portions[0]);
			} else {
				in.close();
				throw new Exception("error in line "+lineCount);
			}
			lineCount++;
		}
		in.close();
		return ret;
	}
	
	private static final String space="[\\s]+";
	private static final String id="([^\\s]+)";
	private static final String integer="([-+]?[\\d]+)";
	private static final String floating="([-+]?(\\d*[.])?\\d+([eE][-+]?[\\d]+)?)";
	private static final Pattern transducerWithWeights=Pattern.compile("[\\s]*"+integer+space+integer+
			space+"("+integer+"|"+id+")"+space+"("+integer+"|"+id+")("+space+floating+")?");
	private static final Pattern finalWithWeights=Pattern.compile("[\\s]*"+integer+"("+space+floating+")?");
	public Collection<Node> openFST(Reader fst) throws Exception {
		return openFST(fst, in, out);
	}
	public Collection<Node> openFST(Reader fst, File inputSymbols, File outputSymbols) throws Exception {
		Map<String,Node> nodes=new HashMap<String, Node>();
		Set<Node> notInit=new HashSet<Node>();
		Map<Integer,String> iSyms=openSymbols(inputSymbols);
		Map<Integer,String> oSyms=openSymbols(outputSymbols);
		BufferedReader in=new BufferedReader(fst);
		String line;
		int lineCount=1;
		while((line=in.readLine())!=null) {
			Matcher m=transducerWithWeights.matcher(line);
			Matcher fm=finalWithWeights.matcher(line);
			if (m.matches()) {
				String source=m.group(1),target=m.group(2);
				String consume=resolveSymbol(m.group(3),iSyms);
				String produce=resolveSymbol(m.group(6),oSyms);
				float w=0;
				if(m.group(9)!=null)
					w=Float.parseFloat(m.group(9));
				Node sn=nodes.get(source),tn=nodes.get(target);
				if (sn==null) nodes.put(source, sn=new Node(source));
				if (tn==null) nodes.put(target, tn=new Node(target));
				notInit.add(tn);
				Edge e=new Edge();
				e.setSource(sn);
				e.setTarget(tn);
				e.setConsume(consume);
				e.setProduce(produce);
				e.setWeight(new Rational(w));
				sn.addEdge(e, false, false);
			} else if (fm.matches()) {
				String source=fm.group(1);
				float w=0;
				if(fm.group(2)!=null)
					w=Float.parseFloat(fm.group(2));
				Node sn=nodes.get(source);
				if (sn==null) nodes.put(source, sn=new Node(source));
				sn.setWeight(new Rational(w));
			} else throw new Exception("error in line '"+line+"'.");
			lineCount++;
		}
		Collection<Node> init = nodes.values();
		init.removeAll(notInit);
		return init;
	}
	
	private String resolveSymbol(String id, Map<Integer, String> syms) {
		if (syms==null) return id;
		try {
			int idn=Integer.parseInt(id);
			if (syms.containsKey(idn)) return syms.get(idn);
			else return id;
		} catch (NumberFormatException e) {}
		return id;
	}

	public String generateFSTforUtterance(String input)  throws Exception {
		return generateFSTforUtterance(input, in);
	}
	/**
	 * if iSymbols is not provided, it'll ignore the symbols and leave all words.
	 * @param input
	 * @param iSymbols
	 * @return
	 * @throws Exception
	 */
	public List<Token> generateInputTextForFST(String input,File iSymbols) throws Exception {
		Set<String> knownWords=null;
		if (iSymbols!=null) {
			Map<Integer, String> syms = openSymbols(iSymbols);
			knownWords=new HashSet<String>(syms.values());
		}
		TokenizerI tokenizer = getConfiguration().getNluTokenizer(PreprocessingType.RUN);
		List<Token> tokens = tokenizer.tokenize1(input);
		Iterator<Token> it=tokens.iterator();
		while(it.hasNext()) {
			Token t=it.next();
			if (knownWords!=null && !knownWords.contains(t.getName().toLowerCase())) {
				//t.setName("<unk>");
				it.remove();
			}
		}
		return tokens;
	}
	public String generateFSTforUtterance(String input,File iSymbols)  throws Exception {
		List<Token> tokens = generateInputTextForFST(input, iSymbols);
		StringBuffer out=new StringBuffer();
		if (!tokens.isEmpty()) {
			int i=0;
			for(Token t:tokens) {
				out.append(i+" "+(++i)+" "+t.getName()+" "+t.getName()+"\n");
			}
			out.append(i+"\n");
		}
		
		return out.toString();
	}
	
	public String getNLUforUtterance(String input,int nBest) throws Exception {
		if (nBest<1) nBest=1;
		TokenizerI tokenizer = getConfiguration().getNluTokenizer(PreprocessingType.RUN);
		input=tokenizer.tokAnduntok(input);
		for (int i=0;i<fstCmd.length;i++) {
			fstCmd[i] = fstCmd[i].replaceAll("%NBEST%",nBest+"");
		}
		Process p = Runtime.getRuntime().exec(fstCmd,new String[0],in.getParentFile());
		StreamGobbler output = new StreamGobbler(p.getInputStream(), "output",false,true);
		StreamGobbler error = new StreamGobbler(p.getErrorStream(), "error",true,false);
		output.start();
		error.start();
		OutputStream stdin=p.getOutputStream();
		String inputFST=generateFSTforUtterance(input);
		if (logger.isDebugEnabled()) logger.debug("input utterance:\n"+inputFST);
		stdin.write(inputFST.getBytes());
		stdin.close();
		p.waitFor();
		StringBuffer ret= output.getContent();
		if (logger.isDebugEnabled()) logger.debug("returned NLU lattice:\n"+ret);
		return (ret!=null)?ret.toString():null;
	}
	
	public List<FSTNLUOutput> getResults(String retFST) throws Exception {
		List<FSTNLUOutput> ret=null;
		if (retFST!=null) {
			Collection<Node> start = openFST(new StringReader(retFST), in, out);
			if (start!=null) {
				for(Node i:start) {
					Set<List<GraphElement>> paths = i.getAllEdgesInPathsStartingFromHere();
					List<Pair<Float,List<GraphElement>>> results=extractResultsFromPaths(paths);
					//normalizeProbabilities(results);
					if (results!=null) {
						for(Pair<Float,List<GraphElement>> p:results) {
							FSTNLUOutput r=new FSTNLUOutput(p);
							if (r.getId()!=null) {
								if (ret==null) ret=new ArrayList<FSTNLUOutput>();
								ret.add(r);
							}
						}
					}
				}	
			}
		}
		return ret;
	}

	private void normalizeProbabilities(List<Pair<Float, List<GraphElement>>> results) throws IllegalArgumentException, SecurityException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
		if (results!=null) {
			List<Float> ps=(List<Float>) FunctionalLibrary.map(results, Pair.class.getMethod("getFirst"));
			Float max=Collections.max(ps);
			Float min=Collections.min(ps);
			Float c=0f;
			if (max==min) {
				logger.debug("probabilities normalization. no range (max==min) => set to 1.");
				for(Pair<Float,List<GraphElement>> p:results) {
					if (p!=null) {
						p.setFirst(1f);
					}
				}
			} else {
				logger.debug("probabilities normalization. max="+max+" min="+min+" mult="+c);
				c=1.0f/(max-min);
				for(Pair<Float,List<GraphElement>> p:results) {
					if (p!=null) {
						p.setFirst((p.getFirst()-min)*c);
					}
				}
			}
		}
	}

	public List<Pair<Float, List<GraphElement>>> extractResultsFromPaths(Set<List<GraphElement>> paths) {
		List<Pair<Float, List<GraphElement>>> ret=null;
		if (paths!=null) {
			for(List<GraphElement> p:paths) {
				if (p!=null) {
					float w=0;
					Node last=null;
					for(GraphElement g:p) {
						Edge e=(Edge)g;
						w+=e.getWeight();
						last=e.getTarget();
					}
					w+=last.getWeight();
					if (ret==null) ret=new ArrayList<Pair<Float,List<GraphElement>>>();
					ret.add(new Pair<Float, List<GraphElement>>(w, p));
				}
			}
			if (ret!=null) {
				Collections.sort(ret, new Comparator<Pair<Float,List<GraphElement>>>() {
					@Override
					public int compare(Pair<Float, List<GraphElement>> o1,
							Pair<Float, List<GraphElement>> o2) {
						return (int)Math.signum(o1.getFirst()-o2.getFirst());
					}
				});
			}
		}	
		return ret;
	}

	public static void main(String[] args) throws Exception {
		TraverseFST tf=new TraverseFST();
		String inputFST=tf.generateFSTforUtterance("this is a test",null);
		System.out.println(inputFST);
		String retFST=tf.getNLUforUtterance("sharp pain",10);
		List<FSTNLUOutput> results = tf.getResults(retFST);
		System.out.println(FunctionalLibrary.printCollection(results, "", "", "\n"));
	}
}
