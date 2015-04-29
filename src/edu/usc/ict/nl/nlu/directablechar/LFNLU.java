package edu.usc.ict.nl.nlu.directablechar;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.usc.ict.nl.nlg.directablechar.Messanger;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.directablechar.AbductionJsonUtils.LANG;
import edu.usc.ict.nl.nlu.directablechar.action.Action;
import edu.usc.ict.nl.util.FileUtils;

public class LFNLU {
	private LANG lang=LANG.EN;
	private String kb=null;
	private AbductionJsonUtils btd=null;
	private Messanger msg=null;
	private ObjectKB objectKB;
	private Integer maxDepth;
	private boolean doGraph=false;
	private String extractor=null;
	
	public LFNLU(String url,LANG lang,String kb) throws MalformedURLException {
		this(url,lang,kb,null,false,null);
	}
	public LFNLU(String url,LANG lang,String kb,Integer maxDepth,boolean doGraph,String extractor) throws MalformedURLException {
		btd=new AbductionJsonUtils(url);
		this.lang=lang;
		this.kb=kb;
		this.objectKB=new ObjectKB();
		this.maxDepth=maxDepth;
		this.doGraph=doGraph;
		this.extractor=extractor;
	}

	public ObjectKB getObjectKB() {
		return objectKB;
	}
	
	public NLUOutput testNLU(String u) throws Exception {
		return testNLU(u, null);
	}
	public NLUOutput testNLU(String u,StringBuffer kbContent) throws Exception {
		JSONObject r = null;
		if (kbContent!=null) {
			//use the custom kb content
			r=btd.buildJsonForRequest(extractor,doGraph,maxDepth,kbContent,lang);
		} else {
			//use the default kb
			r=btd.buildJsonForRequest(extractor,doGraph,maxDepth,kb,lang);
		}
		int v=btd.addUtteranceToJsonRemoveAllAlreadyThere(r, u);
		JSONObject result=btd.sendRequest(r);
		if (result!=null && result.has(AbductionJsonUtils.utterancesKey)) {
			JSONArray utterances=AbductionJsonUtils.getUtterances(result);
			if (utterances!=null) {
				int max=utterances.length();
				for(int i=0;i<max;i++) {
					JSONObject uo=utterances.getJSONObject(i);
					//System.out.println(uo);
					if (uo.has(AbductionJsonUtils.abductionKey)) {
						List<Literal> lf = AbductionJsonUtils.extractParsingPart(uo);
						NLUOutput inter=generateInterpretation(lf,u);
						if (inter!=null) return inter;
					}
				}
			}
		}
		return null;
	}
	
	private NLUOutput generateInterpretation(List<Literal> lf, String utterance) throws Exception {
		List<Literal> inference=getInferredParts(lf);
		List<Action> as=getAction(inference);
		if (as!=null) {
			for(Action a:as) {
				if (a!=null) {
					return new NLUOutput(utterance, a.getName(), 1f, a.getPayload());
				}
			}
		}
		return null;
	}

	private List<Action> getAction(List<Literal> lf) throws Exception {
		List<Action> ret=null;
		if(lf!=null && !lf.isEmpty()) {
			for(Literal l:lf) {
				if (l.getP().equals("RACTION")) {
					List<String> args=l.getArgs();
					if (args==null || args.isEmpty() || args.size()<1) throw new Exception("invalid number of arguments to action");
					else {
						String name=args.get(0);
						Action a=Action.create(name,args.subList(1,args.size()));
						if (a!=null && a.good()) {
							if (ret==null) ret=new ArrayList<Action>();
							ret.add(a);
						}
					}
				}
			}
		}
		if (ret!=null) Collections.sort(ret);
		return ret;
	}

	private List<Literal> getInferredParts(List<Literal> lf) {
		List<Literal> ret=null;
		if(lf!=null && !lf.isEmpty()) {
			for(Literal l:lf) {
				if (Character.isUpperCase(l.getP().charAt(0))) {
					if (ret==null) ret=new ArrayList<Literal>();
					ret.add(l);
				}
			}
		}
		return ret;
	}

	public void kill() {
		if (msg!=null) msg.kill();
	}
	
	public static void main(String[] args) throws Exception {
		LFNLU nlu = new LFNLU("http://colo-vm19.isi.edu:8081/",LANG.EN,"KBs/kb.da");
		StringBuffer kb=FileUtils.readFromFile("resources/characters/Directable/nlu/kb.txt");
		NLUOutput r = nlu.testNLU("create a red ball",kb);
		System.out.println(r);
	}
}
