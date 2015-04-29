package edu.usc.ict.nl.dm.fsm.advicer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.scxml.model.Initial;
import org.apache.commons.scxml.model.Parallel;
import org.apache.commons.scxml.model.State;
import org.apache.commons.scxml.model.Transition;
import org.apache.commons.scxml.model.TransitionTarget;

import edu.usc.ict.nl.dm.fsm.scxml.SCXMLRunner;
import edu.usc.ict.nl.nlu.NLUOutput;

public abstract class BaseGraphSCXMLAdvice extends SCXMLAdvice {
	protected HashMap<String,TransitionTarget> name2States=new HashMap<String, TransitionTarget>();
	protected HashMap<TransitionTarget,Boolean> reportingStates=new HashMap<TransitionTarget, Boolean>();

	public BaseGraphSCXMLAdvice(SCXMLRunner scxml,String[] reportingStates) throws Exception {
		super(scxml);		
		// compile hash of named states
		for(TransitionTarget s:allStates) {
			if(s.getId()!=null)
				name2States.put(s.getId(), s);
		}
		// save states that are descendants of the given named states
		Collection c=new HashSet();
		for(String sn:reportingStates) {
			TransitionTarget s=name2States.get(sn);
			if (s!=null) {
				c.add(s);
				this.reportingStates.put(s, true);
				for (TransitionTarget si:scxml.getAllDescendantsOf(c)) {
					this.reportingStates.put(si, true);
				}
				c.clear();
			}
		}
		//modifyStatesConsideringEmptytransitions(currentStates);
	}

	//============================================================================
	// methods to be Overridden to personalize this class to a given scxml network
	public boolean isThisUserEventPossible(String ue, Set<TransitionTarget> states) {
		ArrayList<Transition> allPossibleTransitions = getAllTransitionsForEventInStates(ue,states);
		return !allPossibleTransitions.isEmpty();
	}
	public Set<String> returnOnlySystemEvents(TransitionTarget s) {		
		if (s!=null) {
			HashSet<String> ret=new HashSet<String>();
			for(Object o:s.getTransitionsList()) {
				Transition t=(Transition)o;
				String event=t.getEvent();
				if ((event!=null) && (event.startsWith("system."))) {
					ret.add(event);
				}
			}
			return ret;
		} else return null;
	}
	public Set<String> returnOnlySystemEventsFromPreferredStates(TransitionTarget s) {
		if (s!=null) {
			HashSet<String> ret=new HashSet<String>();
			for(Object o:s.getTransitionsList()) {
				Transition t=(Transition)o;
				String event=t.getEvent();
				TransitionTarget sourceState = t.getParent();
				if ((event!=null) && (event.startsWith("system.")) && (reportingStates.containsKey(sourceState)))
					ret.add(event);
			}
			return ret;
		} else return null;
	}
	//============================================================================
	
	public ArrayList<Transition> getAllTransitionsForEventInStates(String event,Collection<TransitionTarget> allStates) {
		ArrayList<Transition> trs=new ArrayList<Transition>();
		for(TransitionTarget s:allStates) {
			Collection strs=s.getTransitionsList(event);
			if (strs!=null) trs.addAll(strs);
		}
		return trs;
	}

	public final Method getReturnOnlySystemEventsMethod(BaseGraphSCXMLAdvice o) throws NoSuchMethodException{
		return o.getClass().getDeclaredMethod("returnOnlySystemEvents", TransitionTarget.class);
	}
	public final Method getReturnOnlySystemEventsInPreferredStatesMethod(BaseGraphSCXMLAdvice o) throws NoSuchMethodException{
		return o.getClass().getDeclaredMethod("returnOnlySystemEventsFromPreferredStates", TransitionTarget.class);
	}

	// returns the events satisfying the given filter among the outgoing transitions attached to the given states
	public Set<String> getAllOptionsInStates(Set<TransitionTarget> states,Method filter) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		HashSet<String> ret=new HashSet<String>();
		for(TransitionTarget s:states) {
			System.out.println(" Getting all options for state: "+s.getId());
			Set<String> results=(Set<String>) filter.invoke(this,s);
			if (results!=null) ret.addAll(results);
			System.out.println("  Options are: "+results);
		}
		return ret;
	}

	/**
	 * modifies states adding and removing those that can be skipped following empty transitions
	 * 
	 * @param states the initial set of states
	 * @return the states added to states
	 * @throws Exception
	 */
	private Set<TransitionTarget> modifyStatesConsideringEmptytransitions(Set<TransitionTarget> states) throws Exception {
		Set<TransitionTarget> add=new HashSet<TransitionTarget>();
		Stack<TransitionTarget> stack=new Stack<TransitionTarget>();
		stack.addAll(states);
		HashMap<TransitionTarget,Boolean> initialStates=new HashMap<TransitionTarget, Boolean>();
		for (TransitionTarget s:states) {
			//System.out.println("    =>"+s.getId()+" "+s);
			initialStates.put(s, true);
		}
		states.clear();
		while (!stack.isEmpty()) {
			List<TransitionTarget> newStates=new ArrayList<TransitionTarget>();
			
			TransitionTarget s=stack.pop();
			Collection<Transition> strs=s.getTransitionsList();
			boolean nonEmptyTransitions=false;
			// consider empty transitions
			for (Transition t:strs) {
				String event=t.getEvent();
				if ((event==null) || event.equals("")) {
					newStates.addAll(t.getTargets());
				} else nonEmptyTransitions=true;
			}
			// if s is a parallel state, take all its children
			if (s instanceof Parallel) {
				newStates.addAll(((Parallel)s).getChildren());
			} else if (s instanceof State) {
			// if s is a normal state, get its initial node, if it has no initial state but children terminate with an error
				Initial initial = ((State)s).getInitial();
				Collection<TransitionTarget> children = ((State) s).getChildren().values();
				if (initial!=null)
					newStates.addAll(initial.getTransition().getTargets());
				else if (children.size()>1) {
					throw new Exception("Error multiple children for state "+s.getId()+" but no initial state defined.");
				} else {
					newStates.addAll(children);
				}
			}
			// add all the collected options, reachable with no events
			for (TransitionTarget ns:newStates) {
				if ((!states.contains(ns)) && (ns!=s)) {
					stack.add(ns);
				}
			}
			// add the original state in case it has non-empty transitions, otherwise
			// keep only the children reachable with empty transitions.
			if (!initialStates.containsKey(s)) {
				//System.out.println("    ->"+s.getId()+" "+s);
				add.add(s);
			}
			if (nonEmptyTransitions) {
				states.add(s);
			}
		}
		return add;
	}

	private Set<TransitionTarget> updateCurrentStatesWithEvent(String event) throws Exception {
		System.out.println("  START OF UPDATE CURRENT STATE");
		System.out.println("  current states: "+printStates(currentStates));
		Set<TransitionTarget> add=new HashSet<TransitionTarget>();
		if (event==null) return add;
		// see which states are reachable using the given event
		ArrayList<Transition> allPossibleTransitions = getAllTransitionsForEventInStates(event,currentStates);
		if (allPossibleTransitions.isEmpty()) {
			System.out.println("  no possible transitions in current states, checking all states.");
			allPossibleTransitions = getAllTransitionsForEventInStates(event,allStates);
			// add the destination to currentStates
			for (Transition tr:allPossibleTransitions) {
				List<TransitionTarget> targets = tr.getTargets();
				add.addAll(targets);
				currentStates.addAll(targets);				
			}
		} else {
			System.out.println("  there are possible transitions in current states.");
			for (Transition tr:allPossibleTransitions) {
				List<TransitionTarget> targets = tr.getTargets();
				// delete the source from currentStates
				currentStates.remove(tr.getParent());
				// add the destination to currentStates
				add.addAll(targets);
				currentStates.addAll(targets);
			}
		}
		System.out.println("  BEFORE EMPTY TRANSITIONS");
		System.out.println("  destinations: "+printStates(add));
		// modify current states considering those reachable with empty transitions
		Set<TransitionTarget> add2=modifyStatesConsideringEmptytransitions(currentStates);
		add.addAll(add2);
		System.out.println("  END OF UPDATE CURRENT STATE");
		System.out.println("  additional destinations: "+printStates(add2));
		System.out.println("  current states: "+printStates(currentStates));
		return add;
	}
		
	// this updates the currentStates too because there is only one USER event as input
	public Set<String> getAdviceForWizardGivenThisUserEvent(String event) throws Exception {
		System.out.println("Advice possible for user event: "+event+". Taking this event.");
		Set<TransitionTarget> destinationsOfEvent=updateCurrentStatesWithEvent(event);
		System.out.println("given user event returns these new destinations: "+printStates(destinationsOfEvent)); 
		Set<String> result;
		// use the new destinations just introduced by using the event to give the advice
		if (!destinationsOfEvent.isEmpty()) {
			System.out.println("1: (destinations) preferred parent.");
			result = getAllOptionsInStates(destinationsOfEvent,getReturnOnlySystemEventsInPreferredStatesMethod(this));
			if (!result.isEmpty()) return result;
			System.out.println("2: (destinations) any parent.");
			result = getAllOptionsInStates(destinationsOfEvent,getReturnOnlySystemEventsMethod(this));
			if (!result.isEmpty()) return result;
		}
		// fallback: use all the currentStates
		System.out.println("3: (current states) preferred parent.");
		result=getAllOptionsInStates(currentStates,getReturnOnlySystemEventsInPreferredStatesMethod(this));
		if (!result.isEmpty()) return result;
		System.out.println("4: (current states) any parent.");
		result=getAllOptionsInStates(currentStates,getReturnOnlySystemEventsMethod(this));
		return result;
	}
	private Set<String> getAdviceForWizardGivenThisUserEventIfPossible(String event) throws Exception {
		// if currentStates allows for the user event 'event', then return getAdviceForWizardGivenThisUserEvent
		// else return null
		System.out.println("Attempting advice for user event: "+event);
		if (isThisUserEventPossible(event,currentStates))
			return getAdviceForWizardGivenThisUserEvent(event);
		else return null;
	}

	@Override
	public void applyThisWiwardSelectedEvent(String event) throws Exception {
		System.out.println("applying this wizard selection: "+event);
		//updateCurrentStatesWithEvent(event);
	}
	// this updates the currentStates by computing the union of the currentStates for each input user event
	// if the input array is NULL: gets the next advice for the wizard (no user interaction needed).
	@Override
	public HashMap<String, Set<String>> getAdviceForWizardGivenTheseUserEvents(List<NLUOutput> userEvents) throws Exception {
		System.out.println("ENTERING ADVICER:");
		System.out.println("Current states: "+printStates(currentStates));
		System.out.println("getting advice for wizard given these events: "+((userEvents==null)?"":Arrays.asList(userEvents)));
		HashMap<String, Set<String>> advices=new HashMap<String,Set<String>>();
		int lastUserEventPos;
		// in case no user event is given, return the previous advice
		if ((userEvents==null) || ((lastUserEventPos=userEvents.size()-1)<0)) {
			Set<String> advice = getAdviceForWizardGivenThisUserEvent(null);
			advices.put(null,advice);
		} else {
			// find the highest rated event (i.e. the first) that makes one of the available transitions.
			int i=0;
			System.out.println(lastUserEventPos);
			String event=userEvents.get(i).getId();
			Set<String> advice;
			while((advice = getAdviceForWizardGivenThisUserEventIfPossible(event))==null) {
				System.out.println(i);
				if (i<lastUserEventPos) event=userEvents.get(++i).getId();
			}
			if (advice==null) {
				System.out.println("No advices possible for given events, forcing highest prob event:");
				// add to the current states the states that accept as an outgoing transition the most probable
				// event (i.e. userEvents[0]).
				// report as advice the available system transitions for the added states.
				advice = getAdviceForWizardGivenThisUserEvent(userEvents.get(0).getId());
				advices.put(userEvents.get(0).getId(),advice);
			} else advices.put(event,advice);
		}
		System.out.println("EXITING ADVICER:");
		System.out.println("Current states: "+printStates(currentStates));
		return advices;
	}
}
