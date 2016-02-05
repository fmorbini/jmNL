package edu.usc.ict.nl.nlu.ne;

public class Interval {
	int start,end;
	public Interval(int start,int end) {
		this.start=start;
		this.end=end;
	}
	public boolean inside(int start,int end) {
		return inside(start) && inside(end);
	}
	public boolean inside(int point) {
		return inside(start,end,point);
	}
	public static boolean inside(int start,int end,int point) {
		return (point>=start && point<=end);
	}
	public static boolean inside(int start,int end,int start2,int end2) {
		return inside(start,end,start2) && inside(start,end,end2);
	}
}