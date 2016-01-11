package edu.usc.ict.nl.nlu.directablechar;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.nlu.directablechar.AbductionJsonUtils.LANG;
import edu.usc.ict.nl.nlu.directablechar.ObjectKB.COLOR;
import edu.usc.ict.nl.nlu.directablechar.ObjectKB.DCObject;
import edu.usc.ict.nl.nlu.directablechar.ObjectKB.SHAPE;
import edu.usc.ict.nl.nlu.directablechar.ObjectKB.SIZE;
import edu.usc.ict.nl.util.FileUtils;
import edu.usc.ict.nl.util.StringUtils;

public class LFNLU2 extends NLU {

	private LFNLU internalnlu=null;
	
	public LFNLU2(NLUConfig c) throws Exception {
		super(c);
		internalnlu = new LFNLU("http://colo-vm19.isi.edu:8084/",LANG.EN,"KBs/kb.da",7,true,null);
	}

	@Override
	public void kill() throws Exception {
		internalnlu.kill();
	}
	
	@Override
	public List<NLUOutput> getNLUOutput(String text,Set<String> possibleNLUOutputIDs, Integer nBest) throws Exception {
		NLUConfig config = getConfiguration();
		
		StringBuffer kb = buildKB();
		NLUOutput r = internalnlu.testNLU(text,kb);
		List<NLUOutput> result=null;
		if (r!=null) {
			if (result==null) result=new ArrayList<NLUOutput>();
			result.add(r);
		} else {
			String lowConfidenceEvent=config.getLowConfidenceEvent();
			if (StringUtils.isEmptyString(lowConfidenceEvent)) {
				getLogger().warn(" no user speech acts left and LOW confidence event disabled, returning no NLU results.");
			} else {
				if (result==null) result=new ArrayList<NLUOutput>();
				result.add(new NLUOutput(text,lowConfidenceEvent,1f,null));
				getLogger().warn(" no user speech acts left. adding the low confidence event.");
			}
		}
		return result;
	}

	private StringBuffer buildKB() throws IOException {
		StringBuffer resolverAxioms=null;
		try {
			resolverAxioms=internalnlu.getObjectKB().generateAxioms();
		} catch (NullPointerException e) {}
		File baseKb=new File(getConfiguration().getNLUContentRoot(),"kb.txt");
		StringBuffer kb=FileUtils.readFromFile(baseKb.getAbsolutePath());
		if (resolverAxioms!=null) kb.append(resolverAxioms);
		return kb;
	}
	
	@Override
	public Set<String> getAllSimplifiedPossibleOutputs() throws Exception {
		return null;
	}

	public ObjectKB getObjectKB() {
		if (internalnlu!=null) return internalnlu.getObjectKB();
		return null;
	}
	
	public static void main(String[] args) throws Exception {
		NLUConfig config=NLU.getNLUConfig("DC");
		NLBusConfig busconfig=(NLBusConfig) NLBusConfig.WIN_EXE_CONFIG.clone();
		busconfig.setCharacter("Directable");
		busconfig.setNluConfig(config);
		LFNLU2 u=new LFNLU2(config);
		ObjectKB kb = u.getObjectKB();
		DCObject o=kb.new DCObject(SHAPE.BOX, SIZE.BIG, COLOR.RED);
		o.setProperty("o1");
		kb.storeObject(o);
		o=kb.new DCObject(SHAPE.BOX, SIZE.SMALL, COLOR.RED);
		o.setProperty("o2");
		kb.storeObject(o);
		o=kb.new DCObject(SHAPE.SPHERE, SIZE.BIG, COLOR.RED);
		o.setProperty("o3");
		kb.storeObject(o);
		o=kb.new DCObject(SHAPE.SPHERE, SIZE.BIG, COLOR.YELLOW);
		o.setProperty("o4");
		kb.storeObject(o);
		StringBuffer skb=u.buildKB();
		//System.out.println(skb);
		NLUOutput r = u.internalnlu.testNLU("pick the red thing",skb);
		System.out.println(r.getPayload());
	}
}
