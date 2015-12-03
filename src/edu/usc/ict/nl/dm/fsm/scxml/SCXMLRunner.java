package edu.usc.ict.nl.dm.fsm.scxml;

import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Semaphore;

import org.apache.commons.scxml.Context;
import org.apache.commons.scxml.ErrorReporter;
import org.apache.commons.scxml.Evaluator;
import org.apache.commons.scxml.EventDispatcher;
import org.apache.commons.scxml.SCXMLExecutor;
import org.apache.commons.scxml.SCXMLListener;
import org.apache.commons.scxml.Status;
import org.apache.commons.scxml.TriggerEvent;
import org.apache.commons.scxml.env.SimpleErrorHandler;
import org.apache.commons.scxml.env.jexl.JexlContext;
import org.apache.commons.scxml.io.SCXMLParser;
import org.apache.commons.scxml.model.CustomAction;
import org.apache.commons.scxml.model.Parallel;
import org.apache.commons.scxml.model.SCXML;
import org.apache.commons.scxml.model.Transition;
import org.apache.commons.scxml.model.TransitionTarget;
import org.apache.log4j.PropertyConfigurator;

import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.bus.NLBusInterface;
import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.config.DMConfig;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.dm.fsm.advicer.SCXMLAdvice;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.nlu.NLUOutput;
import edu.usc.ict.nl.util.FileUtils;
import edu.usc.ict.nl.util.FunctionalLibrary;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.utils.LogConfig;

public class SCXMLRunner extends DM {
	protected boolean stopInfiniteLoopForTimer=false;
	protected SCXMLExecutor exec;
	protected SCXML scxml;
	protected TimerThread timerEventThread;
	protected float timer;
	Semaphore hasStarted=new Semaphore(1);
	protected SCXMLAdvice advicer;
	private Evaluator expressionEvaluator;
	private EventDispatcher eventDispatcher;
	private ErrorReporter errorReporter;
	private Context initialContext;
	private final SCXMLInformationState is=new SCXMLInformationState(this);

	public void go() throws InterruptedException {
		run();
		waitTillIsStarted();
	}
	
	private void configureAndRun(Long sid, Context initialContext, Evaluator ev,EventDispatcher ed,ErrorReporter er,float timer) throws Exception {
		this.initialContext=initialContext;
		this.timer=timer;
		this.expressionEvaluator=ev;
		this.eventDispatcher=ed;
		this.errorReporter=er;
		setSessionID(sid);
		hasStarted.acquire();
		run();
		waitTillIsStarted();
	}

	public SCXML parseSCXMLFile(String fileName) throws URISyntaxException, MalformedURLException {
		File policyFile = FileUtils.getFileFromStringInResources(fileName);
		URL SCXMLFileURL=policyFile.toURI().toURL();
		SCXMLParser.setURLsStatic(getConfiguration().getStaticURLs());
		return parseSCXML(SCXMLFileURL,getConfiguration().getScxmlCustomActions());
	}
	
	public SCXMLRunner(DMConfig config) {
		super(config);
	}
	public SCXMLRunner(Long sessionID,SCXML preparsedFSM,Context initialContext,Evaluator ev,EventDispatcher ed,ErrorReporter er,DMConfig config) throws Exception {
		super(config);
		this.scxml=preparsedFSM;
		this.timer=config.getTimerInterval();
		configureAndRun(sessionID,initialContext,ev, ed, er, timer);
	}

	public void run() {
		try {
			this.buildExecutor();
			this.runSCXMLExecutor();
			hasStarted.release();
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	public EventDispatcher getEventDispatcher() {
		return eventDispatcher;
	}
	public SCXMLExecutor getExecutor() {
		return exec;
	}

	public void addListner(SCXMLListener l) {
		exec.addListener(scxml, l);
	}
	public void removeListner(SCXMLListener l) {
		exec.removeListener(scxml, l);
	}
	public Set<SCXMLListener> getListeners() {
		return exec.getListenersFor(scxml);
	}
	public SCXMLListenerUnhandledEvents getListenerForUnhandledEvents() {
		Set<SCXMLListener> listeners = getListeners();
		for (SCXMLListener l:listeners) {
			if (l instanceof SCXMLListenerUnhandledEvents) return (SCXMLListenerUnhandledEvents) l;
		}
		return null;
	}
	
	public SCXML parseSCXML(URL scxmlFile, List<CustomAction> customActions) {
		try {
			if ((customActions==null) || (customActions.size()==0))
				return SCXMLParser.parse(scxmlFile, new SimpleErrorHandler());
			else
				return SCXMLParser.parse(scxmlFile, new SimpleErrorHandler(),customActions);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void stopAndSaveExecutor(OutputStream outStream) {
		if (timerEventThread!=null) timerEventThread.kill();
		if (outStream!=null) {
			ObjectOutputStream out = null;
			try
			{
				out = new ObjectOutputStream(outStream);
				out.writeObject(exec);
				out.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public void reloadAndStartExecutor(InputStream inStream) {
		stopInfiniteLoopForTimer=false;
		if (timer>0) timerEventThread = new TimerThread(timer,this);
		else timerEventThread=null;
		if (inStream!=null) {
			ObjectInputStream in = null;
			try
			{
				in = new ObjectInputStream(inStream);
				exec = (SCXMLExecutor)in.readObject();
				scxml = exec.getStateMachine();
				expressionEvaluator=exec.getEvaluator();
				eventDispatcher=exec.getEventdispatcher();
				errorReporter=exec.getErrorReporter();
				in.close();
				if (timerEventThread!=null) timerEventThread.start();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	public SCXMLExecutor buildExecutor() {
		stopInfiniteLoopForTimer=false;
		if (timer>0) timerEventThread = new TimerThread(timer,this);
		else timerEventThread=null;
		try {
			exec = new SCXMLExecutor(expressionEvaluator, eventDispatcher, errorReporter);
			exec.setStateMachine(scxml);
			if (initialContext==null) initialContext=new JexlContext();
			exec.setRootContext(initialContext);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return exec;
	}

	public void runSCXMLExecutor() {
		try {
			exec.go();
			if (timerEventThread!=null) timerEventThread.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendEvent(String ev,Object payload) {
		TriggerEvent event = new TriggerEvent(ev,TriggerEvent.SIGNAL_EVENT, payload);
		try {
			exec.triggerEvent(event);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void kill() {
		super.kill();
		killTimerThread();
	}
	
	public void killTimerThread() {
		if (timerEventThread!=null) timerEventThread.kill();
	}
	
	protected class TimerThread extends Thread {
		private float interval=1;
		private SCXMLRunner r=null;
		public TimerThread(float interval,SCXMLRunner r) {
			this.interval=interval;
			this.r=r;
		}

		public void kill() {
			stopInfiniteLoopForTimer=true;
		}

		public void run() {
			try {
				if (interval<=0) stopInfiniteLoopForTimer=true;
				long time=System.currentTimeMillis();
				final float millisecondsInInterval=interval*1000; //number of milliseconds
				while(!stopInfiniteLoopForTimer){
					Thread.sleep(100);
					long newTime=System.currentTimeMillis();
					if (newTime-time>millisecondsInInterval) {
						time=newTime;
						r.sendEvent("internal.timer",null);
						if (r.getExecutor().getCurrentStatus().isFinal())
							stopInfiniteLoopForTimer=true;
					}
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
			System.out.println("Terminated timer thread.");
		}
	}

	public void waitTillIsStarted() throws InterruptedException {
		hasStarted.acquire();
		hasStarted.release();
	}
	
	public SCXML getFSM() {
		return scxml;
	}
	public Map<String,TransitionTarget> getAllStatesMap() {
		return getExecutor().getStateMachine().getTargets();
	}
	public Collection<TransitionTarget> getAllStates() {
		return getAllStatesMap().values();
		//return getAllDescendantsOf(scxml.getChildren().values());
	}
	public Set<TransitionTarget> getAllDescendantsOf(Collection collection) {
		HashSet<TransitionTarget> states=new HashSet<TransitionTarget>();			
		Stack stack=new Stack();
		stack.addAll(collection);
		while(!stack.isEmpty()) {
			Object c = stack.pop();
			if (c instanceof Parallel) {
				states.add(((TransitionTarget)c));
				stack.addAll(((Parallel)c).getChildren());
			} else if (c instanceof org.apache.commons.scxml.model.State) {
				states.add(((TransitionTarget)c));
				stack.addAll(((org.apache.commons.scxml.model.State)c).getChildren().values());
			}
		}
		return states;
	}
	public Set<TransitionTarget> getAllAncestorsAndDescendatsOf(TransitionTarget o) {
		Set<TransitionTarget> states=new HashSet<TransitionTarget>();
		Collection children = scxml.getChildren().values();
		Stack stack=new Stack();
		stack.addAll(children);
		while(!stack.isEmpty()) {
			Object c = stack.pop();
			if (c instanceof Parallel) {
				states.add(((TransitionTarget)c));
				stack.addAll(((Parallel)c).getChildren());
			} else if (c instanceof org.apache.commons.scxml.model.State) {
				states.add(((TransitionTarget)c));
				stack.addAll(((org.apache.commons.scxml.model.State)c).getChildren().values());
			}
		}
		return states;
	}

	public Set<TransitionTarget> getActiveStates() {
		Status status = exec.getCurrentStatus();
		Set<TransitionTarget> states = status.getStates();
		return states;
	}

	public void setAdvicer(SCXMLAdvice scxmlAdvice) {
		this.advicer=scxmlAdvice;
	}
	public SCXMLAdvice getAdvicer() {
		return advicer;
	}

	private static HashSet<String> a=new  HashSet<String>();
	static {
		a.add("question.reason-to-visit");
	}
	
	public static class Test {
		public HashMap<Object,Object> getHashMap() {
			return new HashMap<Object,Object>();
		}
	}
	
	public void resetActiveStatesTo(Collection<String> statesToActivate) throws Exception {
		Set<TransitionTarget> initialStates=new HashSet<TransitionTarget>();
		Map<String, TransitionTarget> allStatesMap = getAllStatesMap();
		for (String stateToActivate:statesToActivate) {
			TransitionTarget itt = allStatesMap.get(stateToActivate);
			if (itt!=null) initialStates.add(itt);
			else throw new Exception("state '"+stateToActivate+"' not found in scxml network.");
		}
		SCXMLExecutor exe = getExecutor();
		exe.resetToStates(initialStates);
	}
	
	// to enable assertions use the -ea flag in the java virtual machine
	public static void main(String[] args) {
		try {
			URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
			if (log4Jresource != null)
				PropertyConfigurator.configure( log4Jresource );
			
			/*SCXMLExecutor e = new SCXMLExecutor(new JexlEvaluator(),null,new SimpleErrorReporter());
			e.setRootContext(new JexlContext());
			Context b = e.getRootContext();
			Map c = b.getVars();
			b.set("cook_time", 100);
			b.get("hotelbooking");
			

			HashMap initVars=new HashMap();
			initVars.put("abc", new Test());
			Context c =new JexlContext(initVars);
			SCXML scxml=
			SCXMLRunner scxmlRunner=new SCXMLRunner(null,999l,"e.scxml",c,new JexlEvaluator(),new SystemEventDispatcher(999l,null), new SimpleErrorReporter(),-1,null, true,true);
			
			scxmlRunner.sendEvent("aa", null);
			scxmlRunner.sendEvent("c", null);

			
			SCXMLExecutor exe = scxmlRunner.getExecutor();
			Status status = exe.getCurrentStatus();
			System.out.println(status.isFinal());

			scxmlRunner=new SCXMLRunner(null,999l,scxmlRunner.getFSM(),c,new JexlEvaluator(),new SystemEventDispatcher(999l,null), new SimpleErrorReporter(),-1,true,true);
						
			scxmlRunner.resetActiveStatesTo(Arrays.asList(new String[]{"new_node26"}));
			scxmlRunner.sendEvent("c", null);
			exe = scxmlRunner.getExecutor();
			status = exe.getCurrentStatus();
			System.out.println(status.isFinal());
			
			DialogueKBInterface c1=scxmlRunner.getInformationState();
			Map contexts = exe.getAllContexts();
			System.out.println(scxmlRunner.getActiveStates());
			
			Object abcd = c1.getValueOfVariable("abcd");
			Object abc = c1.getValueOfVariable("abc");
			System.out.println(abc);
			//scxmlRunner.getContext().set("abc", "a");
			//scxmlRunner.getContext().set("abcd", a);
			//scxmlRunner.getContext().set("lastSystemSpeechAct", "question.reason-to-visit1");
			//scxmlRunner.sendEvent("aa", null);
			//scxmlRunner.sendEvent("cc", null);
			
			/*String[] reportingStates={"main-network"};
			scxmlRunner.setAdvicer(new SCXMLAdvice(scxmlRunner,reportingStates));
			SCXMLAdvice advice=scxmlRunner.getAdvicer();
			System.out.println(advice.getAdviceForWizardGivenThisUserEvent("user.login"));
			advice.printCurrentStates();
			advice.applyThisWiwardSelectedEvent("system.conventional-opening.generic");
			advice.printCurrentStates();
			advice.applyThisWiwardSelectedEvent("system.conventional-opening.ask-for-name");
			advice.printCurrentStates();
			System.out.println(advice.getAdviceForWizardGivenThisUserEvent("user.answer.bio-info.nickname"));
			advice.printCurrentStates();
			advice.applyThisWiwardSelectedEvent("system.question.permission-to-ask-bio-info-questions");
			advice.printCurrentStates();
			System.out.println(advice.getAdviceForWizardGivenThisUserEvent("user.answer.yes"));
			advice.printCurrentStates();
			advice.applyThisWiwardSelectedEvent("system.thanking");
			System.out.println(advice.getAdviceForWizardGivenThisUserEvent(null));
			advice.printCurrentStates();
			advice.applyThisWiwardSelectedEvent("system.question.bio-info.age");
			System.out.println(advice.getAdviceForWizardGivenThisUserEvent("user.answer.bio-info.user.age"));
			advice.printCurrentStates();
			advice.applyThisWiwardSelectedEvent("system.question.bio-info.gender");
			System.out.println(advice.getAdviceForWizardGivenThisUserEvent("user.answer.bio-info.gender.female"));
			advice.printCurrentStates();*/
			//System.out.println(advice.getAllOptionsInStates(advice.getCurrentStates(), advice.getReturnOnlySystemEventsInPreferredStatesMethod(advice)));
			//System.out.println(scxmlRunner.getAdviceForWizardGivenThisUserEvent("user.conventional-opening.generic"));
			//scxmlRunner.sendEvent("user.conventional-opening");

			//scxmlRunner.sendEvent("s2.done");

			/*
			ByteArrayOutputStream d = new ByteArrayOutputStream();
			scxmlRunner.stopAndSaveExecutor(d);
			
			ByteArrayInputStream f = new ByteArrayInputStream(d.toByteArray());
			scxmlRunner.reloadAndStartExecutor(f);
			
			scxmlRunner.sendEvent("start", null);
			
			Context b = scxmlRunner.getContext();
			Map c = b.getVars();

			f = new ByteArrayInputStream(d.toByteArray());
			scxmlRunner.reloadAndStartExecutor(f);

			scxmlRunner.sendEvent("start2", null);
			
			b = scxmlRunner.getContext();
			c = b.getVars();

			
			b.set("cook_time", 100);
			b.get("hotelbooking");
			
			*/
			
			/*
			Context b = scxmlRunner.getContext();
			Map c = b.getVars();
			ElementNSImpl v = (ElementNSImpl) c.get("hotelbooking");
			System.out.println(v.getChildNodes().item(0).getChildNodes().item(3).getChildNodes().item(0).getNodeValue());
			ElementNSImpl z=(ElementNSImpl) v.cloneNode(true);
			z.getChildNodes().item(0).getChildNodes().item(3).getChildNodes().item(0).setNodeValue("3");
			System.out.println(v.getChildNodes().item(0).getChildNodes().item(3).getChildNodes().item(0).getNodeValue());
			System.out.println(z.getChildNodes().item(0).getChildNodes().item(3).getChildNodes().item(0).getNodeValue());
			scxmlRunner.sendEvent("start1", z);
			*/
			//System.out.println("");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public DialogueKB getInformationState() {is.setIS(getExecutor()); return is;}

	@Override
	public Map<NLUOutput, List<List<String>>> getPossibleSystemResponsesForThesePossibleInputs(
			List<NLUOutput> userInputs) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NLUOutput> getHandledUserEventsInCurrentState(
			List<NLUOutput> userInputs) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object parseDialoguePolicy(String policyURL) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DM createPolicyDM(Object parsedDialoguePolicy, Long sid,
			NLBusInterface listener) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSessionDone() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Set<String> getIDActiveStates() throws Exception {
		Set activeStates = getActiveStates();
		activeStates=(Set) FunctionalLibrary.map(activeStates, TransitionTarget.class.getMethod("getId"));
		return activeStates;
	}

	@Override
	public void resetActiveStatesTo(Set<String> activeStateIds) throws Exception {
		resetActiveStatesTo(activeStateIds);
	}

	@Override
	public NLUOutput selectNLUOutput(String text,Long sessionId,
			List<NLUOutput> userSpeechActs) throws Exception {
		throw new Exception("unhandled");
	}
	
	@Override
	public void validatePolicy(NLBusBase nlModule) throws Exception {
	}
	@Override
	public boolean isWaitingForUser() {
		Set<TransitionTarget> activeStates = getActiveStates();
		for(TransitionTarget s:activeStates) {
			List<Transition> transitions = s.getTransitionsList();
			for(Transition t:transitions) {
				String event = t.getEvent();
				if (!StringUtils.isEmptyString(event)) return true;
			}
		}
		return false;
	}

	@Override
	public void addOperator(String xml) throws Exception {
		throw new Exception("unhandled");
	}

	@Override
	public void removeOperator(String name) throws Exception {
		throw new Exception("unhandled");
	}
}

