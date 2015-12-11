package edu.usc.ict.nl.dm.reward.matcher;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

class State<T> {
	private boolean isStarState=false;
	private char c;
	private boolean hasChar=false;
	private CharMap<State> next;
	//State<T>[] next;
	private Set<T> payload=null;

	public State(char c) {
		if (c=='*') isStarState=true;
		this.c=c;
		hasChar=true;
	}
	public State() {}
	
	public boolean isStar() {return isStarState;}
	public boolean withPayload() {return payload!=null && !payload.isEmpty();}
	public State<T> addNext(char c) {
		if (next==null) next=new CharMap(this);
		State<T> n=next.get(c);
		if (n==null) next.put(c,n=new State(c));
		return n;
	}
	public void attachPayload(T p) {
		if (payload==null) payload=new HashSet<T>();
		payload.add(p);
	}
	/**
	 * should be called only on the State that is the root of the event matcher. 
	 * @param path
	 * @param p
	 */
	public void removePayloadAndPath(String path,T p) {
		Deque<State> statesInPath=new LinkedList<State>();
		State<T> current=this;
		statesInPath.push(current);
		Deque<Character> pathc=new LinkedList<Character>();
		for(char c:path.toCharArray()) {
			pathc.push(c);
			if (current.next!=null && current.next.containsKey(c)) {
				current=current.next.get(c);
				statesInPath.push(current);
			}
		}
		boolean first=true;
		while(statesInPath!=null && !statesInPath.isEmpty()) {
			State<T> s=statesInPath.pop();
			char c=pathc.pop();
			State<T> parent=statesInPath.peek();
			if (first) {
				if (p!=null) {
					if (s.payload==null || !s.payload.contains(p)) EventMatcher.logger.error("Error removing path '"+path+"', no payload found.");
					else s.payload.remove(p);
				}
				first=false;
			}
			if ((s.next==null || s.next.isEmpty()) && (s.payload==null || s.payload.isEmpty())) {
				if (parent!=null && parent.next!=null) {
					if (!parent.next.containsKey(c)) EventMatcher.logger.error("Char not found in next where is supposed to be.");
					else parent.next.remove(c);
				}
			} else break;
		}
	}
	public List<State> getNext(char c) {
		List<State>l=null;
		State<T> n=null;
		if (next!=null) {
			if ((n=next.get(c))!=null) {
				if (l==null) l=new ArrayList<State>();
				l.add(n);
			}
			if ((n=next.get('*'))!=null) {
				if (l==null) l=new ArrayList<State>();
				l.add(n);
			}
		}
		return l;
	}
	@Override
	public String toString() {
		return (isStarState)?"*":((hasChar)?c+"":"");
	}
	public Set<T> getPayload() {
		return payload;
	}
	public boolean hasNext() {
		return next!=null && !next.isEmpty();
	}
	public Iterator<State> iterator() {
		return next!=null?next.iterator():null;
	}
}