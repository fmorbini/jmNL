package edu.usc.ict.nl.dm.reward.matcher;

import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import edu.usc.ict.nl.util.StringUtils;

class State<T> {
	private boolean isStarState=false;
	private String c;
	private boolean hasChar=false;
	private List<State> next;
	//State<T>[] next;
	private Set<T> payload=null;

	public State(String c) {
		if (c.equals("*")) isStarState=true;
		this.c=c;
		hasChar=true;
	}
	public State() {}
	
	public boolean isStar() {return isStarState;}
	public boolean withPayload() {return payload!=null && !payload.isEmpty();}
	public State<T> addNext(String c) {
		if (next==null) next=new ArrayList<>();
		int position=findElementWithCommonPrefix(c);
		if (position>=0) {
			State m=next.get(position);
			String s=m.toString();
			int prefixPosition=findCommonPrefix(s,c);
			int lc=c.length();
			int ls=s.length();
			/**
			 * 3 cases
			 *  prefixPosition is less than len(s) and len(c)
			 *   replace s with prefix and add as possible nexts 2 things, remaining s and remaining c. add all nexts of s to remaining s. return state of remaining c. 
			 *  prefixPosition is len(s) (len(c)>len(s))
			 *   add remaining c to nexts of s. return state of remaining c.
			 *  prefixPosition is len(c) (len(s)>len(c))
			 *   replace s with c. add as possible nexts of c 1 thing: remaining s. add all nexts of s to remaining s. return state or s.
			 */
			if (prefixPosition<lc && prefixPosition<ls) {
				String commonPrefix=c.substring(0, prefixPosition);
				State prefixState=new State<>(commonPrefix);
				prefixState.next=new ArrayList<>();
				String remainignS=s.substring(prefixPosition);
				State remainingSState=m;
				next.set(position, prefixState);
				remainingSState.c=remainignS;
				prefixState.next.add(remainingSState);
				String remainingC=c.substring(prefixPosition);
				State n=prefixState.addNext(remainingC);
				return n;
			} else if (prefixPosition==ls) {
				String remainingC=c.substring(prefixPosition);
				State n=m.addNext(remainingC);
				return n;
			} else {
				assert(prefixPosition==lc);
				State prefixState=new State<>(c);
				prefixState.next=new ArrayList<>();
				String remainignS=s.substring(prefixPosition);
				State remainingSState=m;
				next.set(position, prefixState);
				remainingSState.c=remainignS;
				prefixState.next.add(remainingSState);
				return prefixState;
			}
		} else {
			State<T> n=new State(c);
			next.add(n);
			return n;
		}
	}
	public int findElementWithCommonPrefix(String c) {
		if (next!=null) {
			int i=0;
			for(State p:next) {
				String s=p.toString();
				int position=findCommonPrefix(s,c);
				if (position>0) return i;
				i++;
			}
		}
		return -1;
	}
	public static int findCommonPrefix(String s1, String s2) {
		if (!StringUtils.isEmptyString(s1) && !StringUtils.isEmptyString(s2)) {
			int i=0;
			char[] s1a=s1.toCharArray();
			char[] s2a=s2.toCharArray();
			int l=Math.min(s1a.length,s2a.length);
			for(;(i<l) && (s1a[i]==s2a[i]);i++) {}
			if (i>0) return i; 
		}
		return -1;
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
		Deque<String> pathc=new LinkedList<String>();
		while(path.length()>0) {
			int position=current.findElementWithCommonPrefix(path);
			if (position>=0) {
				State m=current.next.get(position);
				String s=m.toString();
				int prefixPosition=findCommonPrefix(s,path);
				assert (prefixPosition==s.length());
				path=path.substring(prefixPosition);
				current=m;
				statesInPath.push(current);
				pathc.push(s);
			}
		}
		boolean first=true;
		while(statesInPath!=null && !statesInPath.isEmpty()) {
			State<T> s=statesInPath.pop();
			String c=pathc.pop();
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
					assert(parent.hasThisNext(c)>=0);
					parent.removeThisNext(c);
				}
			} else break;
		}
	}
	
	private void removeThisNext(String c) {
		if (next!=null) {
			Iterator<State> it=next.iterator();
			while(it.hasNext()) {
				State n=it.next();
				if (n.toString().equals(c)) {
					it.remove();
					return;
				}
			}
		}
	}
	public int hasThisNext(String c) {
		if (next!=null) {
			int i=0;
			for(State n:next) {
				if (n.toString().equals(c)) return i;
				i++;
			}
		}
		return -1;
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
	public State getNextAtPosition(int position) {
		if (next!=null && position<next.size()) return next.get(position);
		return null;
	}
}