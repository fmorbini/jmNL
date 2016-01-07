package edu.usc.ict.nl.dm.reward.matcher;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;

public class CharMap<T> {
	private T[] x;
	private int nonNullCount;
	
	public CharMap(T o) {
		nonNullCount=0;
		this.x=(T[]) Array.newInstance(o.getClass(), 256);
		Arrays.fill(this.x,null);
	}
	
	public void put(int i,T o) {
		if (o!=null && x[i]==null) nonNullCount++;
		if (o==null && x[i]!=null) nonNullCount--;
		x[i]=o;
	}
	
	public T get(int i) {
		return x[i];
	}

	public boolean containsKey(int i) {
		return x[i]!=null;
	}

	public boolean isEmpty() {
		return nonNullCount==0;
	}

	public void remove(int i) {
		if (x[i]!=null) nonNullCount--;
		x[i]=null;
	}

	public Iterator<T> iterator() {
		return new Iterator<T>() {
			private int position=0;
			@Override
			public boolean hasNext() {
				if (nonNullCount==0) return false;
				for(int i=position;i<x.length;i++) {
					if (x[i]!=null) return true;
				}
				return false;
			}

			@Override
			public T next() {
				if (nonNullCount==0) return null;
				for(int i=position;i<x.length;i++) {
					if (x[i]!=null) {
						position=i+1;
						return x[i];
					}
				}
				return null;
			}
		};
	}
}
