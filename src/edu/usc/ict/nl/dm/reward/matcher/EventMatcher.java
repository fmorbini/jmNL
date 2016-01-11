package edu.usc.ict.nl.dm.reward.matcher;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

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
		return search(event,root,new LinkedHashSet<T>());
	}
	private Set<T> search(String event,State<T> as,Set<T>result) {
		int eventlength=event.length();
		if (eventlength==0) {
			if (as.withPayload()) result.addAll(as.getPayload());
			return result;
		}
		
		int position=as.findElementWithCommonPrefix(event);
		if (position>=0) {
			State m=as.getNextAtPosition(position);
			String s=m.toString();
			int prefixPosition=State.findCommonPrefix(s,event);
			if (prefixPosition==s.length()) {
				String remainingEvent=event.substring(prefixPosition);
				search(remainingEvent, m, result);
			}
		}
		int starPos=as.hasThisNext("*");
		if (starPos>=0) {
			search(event, as.getNextAtPosition(starPos), result);
		}
		if (as.isStar()) {
			search("", as, result);
			if (as.hasNext()) {
				for (int j=1;j<eventlength;j++) {
					String continuationEvent=event.substring(j);
					position=as.findElementWithCommonPrefix(continuationEvent);
					if (position>=0) {
						State m=as.getNextAtPosition(position);
						String s=m.toString();
						int prefixPosition=State.findCommonPrefix(s,event);
						if (prefixPosition==s.length()) {
							search(continuationEvent, m, result);
						}
					}
				}
			}
		}
		
		return result;
	}

	public void addEvent(String event,T payload) {
		if (storedEvents.containsKey(event)) logger.warn("ignoring adding event '"+event+"' because already present in this matcher.");
		else {
			storedEvents.put(event, payload);
			List<String> parts=split(event);
			State<T> next=null;
			event="";
			for(String part:parts) {
				event+=part;
				next=root.addNext(event);
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
			State<T> next=root.addNext(event);
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
	
	/*
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
*/


	private static final Pattern eventDelimiters=Pattern.compile("([\\*]+)");
	public static List<String> split(String path) {
		List<String> ret=null;
		Matcher m=eventDelimiters.matcher(path);
		int pEnd=0;
		while(m.find()) {
			int start=m.start();
			int end=m.end();
			String matched=m.group(1);
			if (start>0) {
				if (ret==null) ret=new ArrayList<>();
				ret.add(path.substring(pEnd, start));
			}
			if (matched.equals("*")) {
				if (ret==null) ret=new ArrayList<>();
				ret.add(matched);
			}
			pEnd=end;
		}
		if (pEnd<path.length()) {
			if (ret==null) ret=new ArrayList<>();
			ret.add(path.substring(pEnd));
		}
		return ret;
	}

	public static void main(String[] args) throws Exception {
		System.out.println(split("*ab*c*"));
		EventMatcher<Integer> em = new EventMatcher<Integer>();
		em.addEvent("a.b", 1);
		em.addEvent("a.b.c", 2);
		System.out.println("compare "+EventComparer.compare("a.*.c", "a.b.*.b"));

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
		System.out.println("result "+r);
		r = em.match("abc");
		System.out.println(r);
		System.out.println(em.getAllMatchedEvents());
		em.removeEvent("ab", 1);
		r = em.match("ab");
		System.out.println(r);
		System.out.println(em.getAllMatchedEvents());
		System.out.println(EventComparer.compare("a***","ac"));
	}
}
