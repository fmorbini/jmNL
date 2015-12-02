package edu.usc.ict.nl.pml;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.usc.ict.nl.bus.NLBusBase;
import edu.usc.ict.nl.bus.events.PMLEvent;
import edu.usc.ict.nl.bus.modules.DM;
import edu.usc.ict.nl.kb.DialogueKB;
import edu.usc.ict.nl.kb.InformationStateInterface.ACCESSTYPE;
import edu.usc.ict.perception.pml.Activity;
import edu.usc.ict.perception.pml.Attention;
import edu.usc.ict.perception.pml.FaceGaze;
import edu.usc.ict.perception.pml.FaceGesture;
import edu.usc.ict.perception.pml.Pml;
import edu.usc.ict.perception.pml.SmileFrequency;

public class PMLStateKeeper {
	public static final Logger logger = Logger.getLogger(PMLStateKeeper.class.getName());

	private NLBusBase nlModule=null;
	Map<String,PMLVar> pmlVars=null;
	
	public PMLStateKeeper(NLBusBase nlModule) {
		logger.info("starting PML state keeper.");
		this.nlModule=nlModule;
		this.pmlVars=new LinkedHashMap<String,PMLVar>();
	}
	
	public void reset() {
		pmlVars.clear();
	}
	
	public void add(Pml pml) throws Exception {
		if (pml!=null) {
			List<PMLEvent> leaves = getPMLLeaves(pml);
			updateInformationStateWithPMLEvents(leaves);
		}
	}
	
	private void updateInformationStateWithPMLEvents(List<PMLEvent> pmls) throws Exception {
		if (pmls!=null && !pmls.isEmpty()) {
			for(PMLEvent pml:pmls) {
				Object event=pml.getPayload();
				if (event instanceof FaceGesture) {
					extractVariablesFromFaceGesture((FaceGesture)event);
				} else if (event instanceof Activity) {
					extractVariablesFromActivity((Activity)event);
				} else if (event instanceof Attention) {
					extractVariablesFromAttention((Attention)event);
				} else if (event instanceof FaceGaze) {
					extractVariablesFromFaceGaze((FaceGaze)event);
				} else if (event instanceof SmileFrequency) {
					extractVariablesFromSmileFrequency((SmileFrequency)event);
				}
			}
			saveVariablesInAllActiveSessions();
		}
	}
	
	private PMLVar getVarNamed(String name) {
		return (pmlVars!=null)?pmlVars.get(name):null;
	}
	private PMLVar createVarNamed(String name,int window) {
		if (getVarNamed(name)!=null) logger.warn("OVERWRITING variable with name: '"+name+"'");
		PMLVar v=new PMLVar(name, window);
		pmlVars.put(name, v);
		return v;
	}
	
	private void extractVariablesFromSmileFrequency(SmileFrequency sf) {
		PMLVar v=getVarNamed("smileFrequency");
		if (v==null) v=createVarNamed("smileFrequency", 0);
		v.setValue("'"+sf.getBehaviorType()+"'");
	}

	private void extractVariablesFromFaceGaze(FaceGaze fg) {
		String dir=fg.getGazeCategoryDirection();
		PMLVar v=getVarNamed("faceLooking");
		if (v==null) {
			v=createVarNamed("faceLooking", 10);
			v.setKeyForFrequency("away");
		}
		v.setValue(dir);
	}

	private void extractVariablesFromActivity(Activity act) {
		PMLVar v=getVarNamed("activity");
		if (v==null) v=createVarNamed("activity", 0);
		v.setValue("'"+act.getBehaviorType()+"'");
	}

	private void extractVariablesFromAttention(Attention att) {
		PMLVar v=getVarNamed("attention");
		if (v==null) v=createVarNamed("attention", 0);
		v.setValue("'"+att.getBehaviorType()+"'");
	}

	private void extractVariablesFromFaceGesture(FaceGesture fg) {
		String gestureType=fg.getFaceGestureType();
		if (!gestureType.contains("Eye")) {
			PMLVar v=getVarNamed(gestureType);
			if (v==null) {
				v=createVarNamed(gestureType, 10);
				v.setKeyForFrequency(true);
			}
			v.setValue(fg.isGestureBool());
		}
	}

	private void saveVariablesInAllActiveSessions() throws Exception {
		if (pmlVars!=null) {
			for(PMLVar v:pmlVars.values()) {
				if (v.hasChanged()) {
					String name=v.getName();
					Object value=v.getValue();
					Object freqValue=v.getFrequency();
					if (logger.isDebugEnabled()) {
						logger.debug("Setting value of variable '"+name+"' to "+value);
						if (v.hasHistory())
							logger.debug("Setting value of variable '"+v.getFrequencyVarName()+"' to "+freqValue);
					}
					for (Long sessionId : nlModule.getSessions()) {
						if (nlModule.getCharacterName4Session(sessionId)!=null) {
							DM dm=nlModule.getDM(sessionId,false);
							if (dm!=null) {
								DialogueKB informationState = dm.getInformationState();
								if (informationState!=null) {
									informationState.setValueOfVariable(name, value,ACCESSTYPE.AUTO_OVERWRITEAUTO);
									informationState.setValueOfVariable(v.getFrequencyVarName(), freqValue,ACCESSTYPE.AUTO_OVERWRITEAUTO);
								}
							}
						}
					}
				}
			}
		}
	}

	private List<PMLEvent> getPMLLeaves(Object event) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		//get all objects obtainable with a get... method that returns an object in the package edu.usc.ict.perception.pml that also has no children in that package.
		List<PMLEvent> ret=null;
		Method[] methods = event.getClass().getMethods();
		for(Method m:methods) {
			String methodName=m.getName();
			Class<?> methodClass = m.getDeclaringClass();
			Package returnTypePackage = m.getReturnType().getPackage();
			if (returnTypePackage!=null && methodName.startsWith("get") && methodClass.getPackage().getName().equals("edu.usc.ict.perception.pml")) {
				Object returnedObject = m.invoke(event);
				if (returnedObject!=null && ((returnedObject instanceof List) || returnTypePackage.getName().equals("edu.usc.ict.perception.pml"))) {
					if (!(returnedObject instanceof List)) {
						ArrayList<Object> tmp = new ArrayList<Object>();
						tmp.add(returnedObject);
						returnedObject=tmp;
					}
					for(Object r:(List)returnedObject) {
						List<PMLEvent> children = getPMLLeaves(r);
						if (ret==null) ret=new ArrayList<PMLEvent>();
						if (children==null) {
							ret.add(new PMLEvent(r.getClass().getName(), null, r));
						} else {
							ret.addAll(children);
						}
					}
				}
			}
		}
		return ret;
	}

}
