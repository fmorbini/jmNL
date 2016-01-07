package edu.usc.ict.nl.dm.reward.matcher;

import java.util.Set;

import edu.usc.ict.nl.util.StringUtils;

public class EventComparer {
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

}
