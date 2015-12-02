package edu.usc.ict.nl.nlu;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;

public class BuildTrainingDataIOR extends BuildTrainingData {
	public BuildTrainingDataIOR(NLUConfig config) throws Exception {
		super(config);
	}

	public HashMap<String, Pair<String, Collection<String>>> produceDataForIOR(List<TrainingDataFormat> data, List<Pair<String, String>> suPairs) throws Exception {
		HashMap<String, Pair<String, Collection<String>>> ad=new HashMap<String, Pair<String,Collection<String>>>();
		HashMap<String,Collection<String>> userSystemMap=new HashMap<String, Collection<String>>();
		// first build the map of user utterance paired to the list of preceding system utterances.
		for(Pair<String, String> su:suPairs) {
			String systemUtt=su.getFirst();
			List<String> userUtts=prepareUtteranceForClassification(su.getSecond());
			String userUtt=userUtts.get(0);
			Collection<String> systemUtts=userSystemMap.get(userUtt);
			if (systemUtts==null) userSystemMap.put(userUtt, systemUtts=new HashSet<String>());
			systemUtts.add(systemUtt);
		}
		// then build the map of user utterances paired to the speech act and system utterances preceding that user utterance.
		for(TrainingDataFormat d:data) {
			List<String> userUtterances=prepareUtteranceForClassification(d.getUtterance());
			String userUtterance=userUtterances.get(0);
			String userUtteranceSpeechAct=d.getLabel();
			Pair<String, Collection<String>> saAndsystemUttsPair = ad.get(userUtterance);
			String a=(saAndsystemUttsPair!=null)?saAndsystemUttsPair.getFirst():null;
			if (!StringUtils.isEmptyString(a)) {
				if (!a.equals(userUtteranceSpeechAct)) {
					System.out.println("Line '"+userUtterance+"' was '"+a+"' and now it is '"+userUtteranceSpeechAct+"'.");
				}
			} else {
				if (saAndsystemUttsPair==null) saAndsystemUttsPair=new Pair<String, Collection<String>>(userUtteranceSpeechAct, new HashSet<String>());
				if (userSystemMap.containsKey(userUtterance))
					saAndsystemUttsPair.getSecond().addAll(userSystemMap.get(userUtterance));
				ad.put(userUtterance, saAndsystemUttsPair);
			}
		}
		return ad;
	}

	public ArrayList<Pair<String,Collection<String>>> readIORExportedLinks(String fileName) throws Exception {
		BufferedReader inp=new BufferedReader(new FileReader(fileName));
		ArrayList<Pair<String,Collection<String>>> ret=new ArrayList<Pair<String,Collection<String>>>();
		String line=null;
		String text="";
		Collection<String> sas=null;
		Pair<String, Collection<String>> lastItem=null;
		boolean foundText=false;
		while((line=inp.readLine())!=null) {
			if (!StringUtils.isEmptyString(line)) {
				if (!foundText) text+=line;
				else sas.add(line);
			} else {
				foundText=!foundText;
				if (!foundText) {
					text=text.replaceFirst("^.*=>[\\s]*", "");
					List<String> texts = prepareUtteranceForClassification(text);
					text=texts.get(0);
					if ((lastItem!=null) && text.equals(lastItem.getFirst())) {
						lastItem.getSecond().addAll(sas);
					} else {
						ret.add(lastItem=new Pair<String, Collection<String>>(text, sas));
					}
					text="";
				} else {
					sas=new HashSet<String>();
				}
			}
		}
		return ret;
	}
	
	public void dumpIORDataInImportableTXT(HashMap<String, Pair<String, Collection<String>>> iorData,String lineFile,String speechActFile,String linksFile) throws IOException {
		BufferedWriter lineOut=new BufferedWriter(new FileWriter(lineFile));
		BufferedWriter saOut=new BufferedWriter(new FileWriter(speechActFile));
		BufferedWriter linksOut=new BufferedWriter(new FileWriter(linksFile));
		HashSet<String> sas=new HashSet<String>();
		for (String line:iorData.keySet()) {
			Pair<String, Collection<String>> saAndSystemUttsPrecedingIt=iorData.get(line);
			String sa=saAndSystemUttsPrecedingIt.getFirst();
			Collection<String> systemUtts=saAndSystemUttsPrecedingIt.getSecond();
			
			String userLine="";
			for (String systemUtt:systemUtts) {
				userLine+="SIMCOACH: "+systemUtt+"\n";
			}
			userLine+="=> "+line+"\n\n";

			if (!StringUtils.isEmptyString(sa)) {
				sas.add(sa);
				linksOut.write(userLine+"\n\n"+sa+"\n\n");
			}

			lineOut.write(userLine);
		}
		for(String sa:sas) {
			saOut.write(sa+"\n\n");
		}
		lineOut.close();
		saOut.close();
		linksOut.close();
	}
	
	public static void main(String[] args) {
		try {
			
			
			//System.out.println(stemm("i'm cars read reads redded"));
			BuildTrainingDataIOR btd = new BuildTrainingDataIOR(NLUConfig.WIN_EXE_CONFIG);
			List<TrainingDataFormat> td2 = btd.buildTrainingData();
			System.out.println(btd.prepareUtteranceForClassification("are you really smart the cars <num> lifted tomatoes running matters years months sometimes on the streets nothing quite"));
			System.exit(1);
			ArrayList<Pair<String, Collection<String>>> r = btd.readIORExportedLinks("exported-links.txt");
			int tot=0,mul=0;
			for(Pair<String, Collection<String>>d:r) {
				if (d.getSecond().size()>1) mul++;
				tot++;
			}
			System.out.println(tot+" "+mul);
			System.out.println(btd.prepareUtteranceForClassification("<NUM> years"));
			System.exit(1);

			List<TrainingDataFormat> td=btd.getAllSimcoachData();
			List<Pair<String, String>> sud=btd.getAllSystemUserPairs();
			HashMap<String, Pair<String,Collection<String>>> iorData = btd.produceDataForIOR(td,sud);
			btd.dumpIORDataInImportableTXT(iorData, "allSimcoachLines", "allSimcoachSpeechActs","allSimcoachLinks");
			System.exit(1);
			
			List<TrainingDataFormat> a = btd.buildTrainingDataFromFormsExcel("../../simcoach-runtime/SimcoachApp/src/forms.xlsx", 0);
			//btd.mergeUserUtterances("localhost:8080","sclab1");
			//System.out.println(btd.tokenize("??"));
			//ArrayList<Pair<String, TokenTypes>> tokens = btd.tokenize("forty-four");			
			System.out.println(btd.applyBasicTransformationsToStringForClassification("fourty six"));
			//System.out.println(EnglishWrittenNumbers2Digits.parseWrittenNumbers(tokens));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
