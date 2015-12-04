package edu.usc.ict.nl.nlu.preprocessing.spellchecker;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.config.NLConfig.ExecutablePlatform;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.preprocessing.PreprocesserI;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.config.NLUConfig;


public class Hunspell extends SpellChecker {
	private NLUConfig configuration;
	private Process p=null;
	private ProcessBuilder pb;
	public BufferedReader from;
	public OutputStream to;
	public NLUConfig getConfiguration() {return configuration;}

	public Hunspell(NLUConfig config) throws Exception {
		super(config);
		this.configuration=config;
		new File(getExeName()).setExecutable(true);
		p=startSpellCheckProcess(getExeName());
	}

	private String getExeName() {
		ExecutablePlatform executablePlatform = getConfiguration().getExecutablePlatform();
		String exe="spell-checker/hunspell-"+executablePlatform.toString();
		if (executablePlatform == ExecutablePlatform.WIN32) exe+=".exe";
		return exe;
	}
	
	public Process startSpellCheckProcess(String exeName) throws IOException {
		List<String> command = new ArrayList<String>();
		File exeFile=new File(exeName);
		command.add(exeName);
		command.add("-d");
		command.add("en_US");
		pb = new ProcessBuilder(command);
		pb.directory(exeFile.getParentFile());
		p = pb.start();
		from = new BufferedReader(new InputStreamReader(p.getInputStream()));
		to = p.getOutputStream();
		// read version
		String res=from.readLine();
		return p;
	}
	
	public String sendWordGetFirstChoice(String w) throws Exception {
		// beginning of w: remove every non word character
		// end of w: remove everything except a single occurrence of: ":;?!.," , "'" is preceded by an s. remember the single occurrence. remove it. add it at the end.
		// middle of w: remove everything except a single occurrence of "'";
		String originalW=w;
		w=w.replaceAll("^[\\W]", "");
		Pattern p = Pattern.compile("^[\\w]*([\\W]+)$");
		Matcher m = p.matcher(w);
		String extraStuff="";
		if (m.matches()) {
			extraStuff = m.group(1);
			if ((extraStuff.length()==1) && ((":;?!.,".contains(extraStuff)) || ((extraStuff.charAt(0)=='\'') && (w.charAt(w.length()-1)=='s')))) { 
				//save extraStuff
			} else {
				//forget extraStuff
				extraStuff="";
			}
			w=w.replaceAll("[\\W]*$","");
		}
		// now is the middle part, see above
		//String patternStr = "([\\w])([\\W&&[^']]{1,}|[\\W]{2,})([\\w])";
		//String replaceStr = "$1$3";
		//Pattern pattern = Pattern.compile(patternStr);
		//Matcher matcher = pattern.matcher(w);
		//w = matcher.replaceAll(replaceStr); 
		w=w.replaceAll("[\\W&&[^']]", "");
		w=w.replaceAll("[']{2,}", "");
		
		//System.out.println("sending: "+w);
		
		if (w.equals("")) {
			return originalW;
		}
		
		String line;
		to.write((w+"\n").getBytes());
		to.flush();
		// read answer line
		line = from.readLine();
		// read empty line
		from.readLine();
		char c = line.charAt(0);
		if (c=='*'||c=='#'||c=='+'||c=='-') {
			// if line starts with * word is fine
			return w+extraStuff;
		} else if (c=='&'||c=='?') {
			// if line starts with & then we have suggestions: & ss 15 0: SS, sch, tis, sci, z, as, s, sis, ass, sos, ssh, sys, s's, es, is
			int colonPos=line.indexOf(':');
			if (colonPos>0) {
				int commaPos=line.indexOf(',',colonPos);
				if (commaPos>0) {
					return line.substring(colonPos+2, commaPos)+extraStuff;
				} else {
					return line.substring(colonPos+2)+extraStuff;
				}
			}
			throw new Exception("missing ':' "+line);
		} else {
			throw new Exception("invalid first character in response from Hunspell "+line);
		}
	}
	
	public String getFirstChoiceForEachWordInString(String s) throws Exception {
		String[] ws=s.split(" ");
		String ret=null;
		for (String w:ws){
			String correctedWord;
			//System.out.println(w);
			correctedWord=sendWordGetFirstChoice(w);
			if (ret==null) {
				ret=correctedWord;
			} else {
				ret+=" "+correctedWord;
			}
		}
		return ret;
	}
	
	@Override
	public String correct(String word) {
		try {
			if (!StringUtils.isEmptyString(word)) {
				return sendWordGetFirstChoice(word);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void main(String[] args) {

		try {
			Hunspell sc = new Hunspell(NLUConfig.WIN_EXE_CONFIG);
			System.out.println(sc.sendWordGetFirstChoice("nighmares"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
