package edu.usc.ict.nl.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.usc.ict.nl.bus.NLBus;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.bus.events.NLUEvent;
import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.bus.modules.NLUInterface;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.ui.chat.ChatInterface;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;

public class NLTester {
	private NLBus nlModule=null;
	static protected AbstractApplicationContext context;
	private static NLTester _instance=null;
	public static NLTester getInstance() {return _instance;}
	
	public NLTester(NLBus nlModule) throws Exception {
		_instance=this;
		this.nlModule=nlModule;
		this.nlModule.getInstance().startup();
	}
	
	public static void init() {
		context = new ClassPathXmlApplicationContext(new String[] {"nlModuleTesting.xml"});		
	}
	
	public NLBusConfig getConfiguration() {return nlModule.getConfiguration();}
	
	
	public Long initDM(String characterName) throws Exception {
		Long sid = nlModule.startSession(characterName,null);
		return sid;
	}
	public boolean batchDM(String characterName,String inputFile, boolean fakeNLU) throws Exception {
		Long sid = initDM(characterName);
		return batchDM(sid,inputFile,null,fakeNLU);		
	}
	public boolean batchDM(Long sid,String inputFile,String nluModel, boolean fakeNLU) throws Exception {
		String outputFile=inputFile+".output";
		BufferedWriter out=new BufferedWriter(new FileWriter(outputFile));
		return batchDM(sid, new BufferedReader(new FileReader(inputFile)), nluModel, fakeNLU,out);
	}
	public boolean batchDM(Long sid,BufferedReader inp,String nluModel, boolean fakeNLU,BufferedWriter out) throws Exception {
		boolean success=true;

		nlModule.setHoldProcessingOfResponseEvents(true);
		
		NLUInterface nlu = nlModule.getNlu(sid);
		DM dm=nlModule.getDM(sid);
		Timer timer=new Timer("BatchTimer");

		// initialize nlu
		if (!StringUtils.isEmptyString(nluModel)) nlu.loadModel(new File(nluModel));

		// read input file
		ArrayList<TargetDialogEntry> targetDialog=TargetDialogEntry.readTargetDialog(inp,nlu.getConfiguration().getLowConfidenceEvent());
		
		Collection<NLGEvent> dmReplies;
		//startSelfDeliveryOfEvents(dm, sid);
		//stopSelfDeliveryOfEvents(sid);
		//send opening login message
		{
			// simulates the initial login event
			Pair<NLUOutput, List<NLGEvent>> nluAnddmReplies = sendLoginAndGetReplies(sid);
			dmReplies=nluAnddmReplies.getSecond();
		}

		final Semaphore lock=new Semaphore(0);
		float currentTime=0;
		while(true) {
			String[] matchStringResult=new String[]{""};
			float systemWaitingTime=findTimeTillNextUserAction(targetDialog);
			float delta=systemWaitingTime-currentTime;
			if (systemWaitingTime<=0) lock.release();
			else {
				timer.schedule(new TimerTask() {
					@Override
					public void run() {
						lock.release();
					}
				}, Math.round(delta*1000));
			}
			lock.acquire();
			dmReplies=getAllSystemRepliesTillNow(sid,dmReplies);
			if (!matchAllSysyemRepliesWithTargetDialog(dmReplies,targetDialog,matchStringResult)) {
				out.write(matchStringResult[0]);
				success=false;
				//break;
			}
			out.write(matchStringResult[0]);

			if (targetDialog.isEmpty()) {
				out.write("SUCCESS\n");
				break;
			}
			TargetDialogEntry tde=targetDialog.remove(0);
			if (tde.isUserAction()) {
				Float dt=tde.getDeltaT();
				currentTime=(dt!=null)?dt:0;
				
				out.write(tde.toString());
				List<NLUOutput> userSpeechActs;
				if (fakeNLU || (isFormChoiceReply(tde.getSpeechActs(),tde.getText()))) userSpeechActs=tde.getSpeechActsAndSetPayload(nlu);
				else userSpeechActs = nlu.getNLUOutput(tde.getText(), null,null);
				
				NLUOutput selectedUserSpeechAct=dm.selectNLUOutput(tde.getText(),sid, userSpeechActs);
				out.write("> "+selectedUserSpeechAct+"\n\n");

				dm.handleEvent(new NLUEvent(selectedUserSpeechAct, sid));
				dmReplies=nlModule.processHeldDMEvents(sid);
			} else {
				out.write("ABORTED (unexpected user action)\n");
				success=false;
				//break;
			}
		}
		out.close();
		nlModule.terminateSession(sid);
		return success;
	}
	public Pair<NLUOutput, List<NLGEvent>> sendLoginAndGetReplies(long sid) throws Exception {
		return sendForcedNLU(getConfiguration().getDmConfigNC().getLoginEventName(), sid);
	}
	public Pair<NLUOutput, List<NLGEvent>> sendForcedNLU(String nluSA,long sid) throws Exception {
		// simulates the initial login event
		NLUInterface nlu = nlModule.getNlu(sid);
		DM dm=nlModule.getDM(sid);
		List<NLUOutput> userSpeechActs = nlu.getNLUOutputFake(new String[]{"1 "+nluSA}, null);
		NLUOutput selectedUserSpeechAct=dm.selectNLUOutput(nluSA,sid, userSpeechActs);
		dm.handleEvent(new NLUEvent(selectedUserSpeechAct, sid));
		List<NLGEvent> dmReplies=nlModule.processHeldDMEvents(sid);
		return new Pair<NLUOutput,List<NLGEvent>>(selectedUserSpeechAct,dmReplies);
	}
	private boolean isFormChoiceReply(ArrayList<NLUOutput> sas, String text) {
		if ((sas!=null) && (sas.size()==1)) {
			String sa=sas.get(0).getId();
			if (sa.startsWith("answer.form.choice")) return true;
			else if (text.equals(sa)) return true;
			else return false;
		}
		else return false;
	}
	private Collection<NLGEvent> getAllSystemRepliesTillNow(Long sid, Collection<NLGEvent> dmReplies) throws Exception {
		List<NLGEvent> newDmReplies = nlModule.processHeldDMEvents(sid);
		if (dmReplies==null) dmReplies=new ArrayList<NLGEvent>();
		if (newDmReplies!=null) dmReplies.addAll(newDmReplies);
		return dmReplies;
	}
	private float findTimeTillNextUserAction(ArrayList<TargetDialogEntry> targetDialog) {
		// scans the entries in target dialog until it finds a user action, returns the time taken to reach that point in the target dialog.
		if (targetDialog!=null) {
			float ret=0;
			for(TargetDialogEntry tde:targetDialog) {
				Float time=tde.getDeltaT();
				if (time!=null && time>=0) ret=time;
				if (tde.isUserAction()) break;
			}
			return ret;
		} else return 0;
	}
	private boolean matchAllSysyemRepliesWithTargetDialog(
			Collection<NLGEvent> dmReplies,
			ArrayList<TargetDialogEntry> targetDialog,String[] matchStringResult) {		
		if ((dmReplies==null) || (targetDialog.size()<dmReplies.size())) {
			matchStringResult[0]+="ABORTED (more: expected: "+targetDialog+" received: "+dmReplies+")\n";
			return false;
		} else {
			for(NLGEvent sr:dmReplies) {
				TargetDialogEntry tde = targetDialog.remove(0);
				if (!tde.match(sr.getPayload())) {
					matchStringResult[0]+=tde.toString()+"> simcoach: "+sr.getName()+"\n> ~ "+sr.getPayload()+"\nABORTED (failed match)\n";
					return false;
				} else {
					matchStringResult[0]+=tde.toString()+"> simcoach: "+sr.getName()+"\n> ~ "+sr.getPayload()+"\n\n";
				}
			}
			return true;
		}
	}

	
	public static void main(String[] args) {
		init();
	}
}