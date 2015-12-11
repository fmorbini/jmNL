package edu.usc.ict.nl.dm.reward.matcher;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.utils.LogConfig;

public class EventMatcher<T> {
	
	public static final Logger logger = Logger.getLogger(EventMatcher.class.getName());
	static {
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );	
	}
	
	private State<T> root;
	
	private HashMap<String,T> storedEvents;
	
	public EventMatcher() {
		root=new State<T>();
		this.storedEvents=new HashMap<String, T>();
	}
	
	public Set<T> match(String event) {
		if (root==null) return null;
		return search(event.toCharArray(),0,root,new LinkedHashSet<T>());
	}
	private Set<T> search(char[] charArray, int i,State<T> as,Set<T>result) {
		int inputLen=charArray.length;
		if (i>=inputLen) {
			if (as.withPayload()) result.addAll(as.getPayload());
			return result;
		}
		char currentChar=charArray[i];
		if (as.isStar()) {
			if (as.withPayload()) result.addAll(as.getPayload());
			// continue recursion only if the star is not the last char in this chain
			if (as.hasNext()) {
				for (int j=i;j<inputLen;j++) {
					char nextChar=charArray[j];
					List<State> next;
					if ((next=as.getNext(nextChar))!=null)
						for(State<T> nextState:next) search(charArray,j+1,nextState,result);
				}
			}
		}
		List<State> next = as.getNext(currentChar);
		if (next!=null) for(State<T> nextState:next) search(charArray,i+1,nextState,result);
		return result;
	}

	public void addEvent(String event,T payload) {
		if (storedEvents.containsKey(event)) logger.warn("ignoring adding event '"+event+"' because already present in this matcher.");
		else {
			storedEvents.put(event, payload);
			State<T> next=root;
			for(char c:event.toCharArray()) {
				next=next.addNext(c);
			}
			next.attachPayload(payload);
		}
	}
	public void removeEvent(String event, T payload) {
		T p=storedEvents.get(event);
		if (!storedEvents.containsKey(event)) logger.warn("ignoring removing event '"+event+"' because event not found.");
		else {
			if (p!=payload) logger.warn("ignoring removing event '"+event+"' because associated payload not found.");
			else {
				storedEvents.remove(event);
				root.removePayloadAndPath(event, payload);
			}
		}
	}

	public void addEventToList(String event, Object update) {
		T payload=storedEvents.get(event);
		if (payload==null) {
			payload=(T) new ArrayList<Object>();
			((List<Object>) payload).add(update);
			storedEvents.put(event, payload);
			State<T> next=root;
			for(char c:event.toCharArray()) {
				next=next.addNext(c);
			}
			next.attachPayload(payload);
		} else {
			((List<Object>) payload).add(update);
		}
	}
	public void removeEventFromList(String event, Object update) {
		if (!storedEvents.containsKey(event)) logger.warn("ignoring removing event '"+event+"' because event not found.");
		else {
			T payload=storedEvents.get(event);
			if (payload!=null) {
				List<Object> ps=(List) payload;
				if (ps.contains(update)) {
					ps.remove(update);
					root.removePayloadAndPath(event, (T) ((ps.isEmpty())?ps:null));
				} else {
					logger.warn("ignoring removing event '"+event+"' because associated payload not found.");
				}
			}
		}
	}

	
	public Set<String> getAllMatchedEvents() {
		return storedEvents.keySet();
		//return collectAllStoredEvents(root,"", new HashSet<String>());
	}
	public HashMap<String,T> getAllMatchedEventsWithPayload() {return storedEvents;}
	
	private Set<String> collectAllStoredEvents(State<T> state,String currentMatchedEvent,HashSet<String> result) {
		if (state.withPayload()) result.add(currentMatchedEvent);
		Iterator<State> it=state.iterator();
		if (it!=null) {
			while(it.hasNext()) {
				State cState=it.next();
				collectAllStoredEvents(cState, currentMatchedEvent+cState.toString(),result);
			}
		}
		return result;
	}

	public static boolean doPatternsIntersect(String p1,String p2) {
		int start=findMaximumErosion(p1,p2,false);
		if (start<0) return false;
		else { 
			p1=p1.substring(start);
			p2=p2.substring(start);
		}
		int end=findMaximumErosion(p1,p2,true);
		if (end<0) return false;
		else {
			p1=p1.substring(0,p1.length()-end);
			p2=p2.substring(0,p2.length()-end);
		}
		boolean p1Empty=p1.length()==0,p2Empty=p2.length()==0;
		if (p1Empty && p2Empty) return true; // same pattern
		else if (p1Empty || p2Empty) return false;
		boolean p1HasStar=p1.contains("*"),p2HasStar=p2.contains("*");
		if (p1HasStar && p2HasStar) return true;
		else {
			// one of p1 or p2 must start AND end with a start.
			String withStars=(p1HasStar)?p1:p2;
			String withoutStars=(p1HasStar)?p2:p1;
			if (withoutStars.length()<StringUtils.countOccurrencesOf(withStars,'*')) return false;
			else if (withStars.length()==1) return true;
			else {
				withStars=withStars.substring(1,withStars.length()-1);
				withoutStars=withoutStars.substring(1,withoutStars.length()-1);
				String[] parts=withStars.split("\\*");
				int pos=-1;
				int l=0;
				for(String p:parts) {
					pos=withoutStars.indexOf(p,pos+l+1); //+1 because a star here counts for at least 1 character
					if (pos<0) return false;
					l=p.length();
				}
				return true;
			}
		}
	}
	
	private static int findMaximumErosion(String p1, String p2, boolean fromEnd) {
		int l1=p1.length(),l2=p2.length();
		int l=Math.min(l1, l2);
		for(int i=0;i<l;i++) {
			int j1=(fromEnd)?l1-i-1:i;
			int j2=(fromEnd)?l2-i-1:i;
			char c1=p1.charAt(j1),c2=p2.charAt(j2);
			if((c1=='*') || (c2=='*')) return i;
			else if (c1!=c2) return -1;
		}
		return l;
	}

	/* 0: same pattern
	 * 1: p1 strictly includes p2
	 * -1: p2 strictly includes p1
	 * 2: non empty intersection (but no inclusion)
	 * -2: disjoint sets
	 */
	public static int compare(String p1,String p2) {
		if (p1.equals(p2)) return 0;
		else {
			EventMatcher<Integer> m=new EventMatcher<Integer>();
			m.addEvent(p1, 1);
			m.addEvent(p2, 2);
			Set<Integer> r=m.match(p2);
			boolean oneContainsTwo=r.contains(1);
			r=m.match(p1);
			boolean twoContainsOne=r.contains(2);
			if (oneContainsTwo && twoContainsOne) return 0;
			if (oneContainsTwo) return 1;
			else if (twoContainsOne) return -1;
			else {
				if (doPatternsIntersect(p1, p2)) return 2;
				else return -2;
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		EventMatcher<Integer> em = new EventMatcher<Integer>();
		em.addEvent("a.b", 1);
		em.addEvent("a.b.c", 2);
		System.out.println(em.compare("a.*.c", "a.b.*.b"));

		//em.addEvent("internal.timer", 1);
		//em.addEvent("answer.observable.*", 2);
		//em.addEvent("*", 3);
		//em.addEvent("a*d*f", 4);
		//em.addEvent("a*d", 4);
		em.addEvent("a*", 5);
		em.addEvent("ab*", 9);
		//em.addEvent("ab", 6);
		//em.addEvent("ab", 7);
		//em.addEvent("b", 8);
		Set<Integer> r = em.match("a*");
		System.out.println(r);
		r = em.match("ab");
		System.out.println(r);
		System.out.println(em.getAllMatchedEvents());
		em.removeEvent("ab", 1);
		r = em.match("ab");
		System.out.println(r);
		System.out.println(em.getAllMatchedEvents());
		System.out.println(compare("a***","ac"));
	}
}
