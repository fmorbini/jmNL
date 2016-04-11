/*
 * Copyright 2014 Radialpoint SafeCare Inc. All Rights Reserved.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package edu.usc.ict.nl.nlu.distributional.word2vec;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.usc.ict.nl.util.StringUtils;

/**
 * This program takes vectors are produced by the C program word2vec and transforms them into a Java binary file to be
 * read by the Vectors class
 */
public class W2V2 {

	private float[][] vectors;
	private String[] vocabVects;
	private int totalSize=-1;
	int vocabulary;
	int dimensions;

	public class ElementG<T> implements Comparable<ElementG<T>> {
		T thing;
		float similarity;
		public ElementG(T thing, float similarity) {
			this.thing=thing;
			this.similarity=similarity;
		}
		@Override
		public int compareTo(ElementG<T> o) {
			return Float.compare(similarity, o.similarity);
		}
		@Override
		public boolean equals(Object obj) {
			if (obj!=null && obj instanceof Element) {
				if (thing!=null) return thing.equals(((ElementG)obj).thing);
				else return thing==((ElementG)obj).thing;
			}
			return false;
		}
	}
	public class Element extends ElementG<Integer> {
		public Element(Integer index, float similarity) {
			super(index, similarity);
		}
		@Override
		public int compareTo(ElementG<Integer> o) {
			return Float.compare(similarity, o.similarity);
		}
		@Override
		public String toString() {
			return W2V2.this.getWord(thing)+": "+similarity;
		}
		public String getWord() {return W2V2.this.getWord(thing);}
	}

	public W2V2(File vectorFile, float portionToLoad) throws Exception {
		if (portionToLoad<0) portionToLoad=0;
		else if (portionToLoad>1) portionToLoad=1;


		double len;

		FileInputStream fis = new FileInputStream(vectorFile);

		StringBuilder sb = new StringBuilder();
		char ch = (char) fis.read();
		while (ch != '\n') {
			sb.append(ch);
			ch = (char) fis.read();
		}

		String line = sb.toString();
		String[] parts = line.split("\\s+");
		vocabulary = (int) Long.parseLong(parts[0]);
		dimensions = (int) Long.parseLong(parts[1]);
		vectors = new float[vocabulary][];
		vocabVects = new String[vocabulary];

		System.out.println("" + vocabulary + " words with size " + dimensions + " per vector.");

		byte[] orig = new byte[4*dimensions];
		byte[] buf = new byte[4];
		boolean toLoad;
		int w=0;
		for (int wg = 0; wg < vocabulary; wg++) {
			toLoad=((float)w/(float)(wg+1))<portionToLoad;

			if (((w+1) % 1000000) == 0 && toLoad) {
				System.out.println("Read " + (w+1) + " words");
			}

			sb.setLength(0);
			ch = (char) fis.read();
			while (!Character.isWhitespace(ch) && ch >= 0 && ch <= 256) {
				sb.append((char) ch);
				ch = (char) fis.read();
			}
			//ch = (char) fis.read();
			String st = sb.toString();
			st=normalize(st);
			float[] m = new float[dimensions];
			int q=fis.read(orig);
			if (q!=4*dimensions) {
				System.err.println("error reading "+(4*dimensions)+" bytes: "+q+" for word: "+w);
			}
			if (toLoad) {
				totalSize=w+1;
				vocabVects[w]=st;
				for (int i = 0; i < dimensions; i++) {
					int offset=i*4;
					buf[3] = orig[offset];
					buf[2] = orig[offset+1];
					buf[1] = orig[offset+2];
					buf[0] = orig[offset+3];
					float f = ByteBuffer.wrap(buf).getFloat();
					m[i]=f;
				}
				len = 0;
				for (int i = 0; i < dimensions; i++)
					len += m[i] * m[i];
				len = (float) Math.sqrt(len);
				for (int i = 0; i < dimensions; i++)
					m[i] /= len;
				vectors[w] = m;
				w++;
			}
		}
		fis.close();
	}

	public static String normalize(String in) {
		if (!StringUtils.isEmptyString(in)) return in.toLowerCase();
		else return in;
	}

	public float[] computeAverageVector(String[] words) {
		float[] avgV=new float[dimensions];
		//System.out.println(Arrays.toString(inputs));
		boolean allIn=true;
		for(String i:words) {
			i=normalize(i);
			int index=isWordInVocabulary(i);
			if (index>=0) {
				float[] v=getVectorForWord(index);
				for(int j=0;j<dimensions;j++) {
					avgV[j]+=v[j];
				}
			} else {
				allIn=false;
				break;
			}
		}
		if (allIn) {
			float len = 0;
			for (int i = 0; i < dimensions; i++)
				len += avgV[i] * avgV[i];
			len = (float) Math.sqrt(len);
			for (int i = 0; i < dimensions; i++)
				avgV[i] /= len;
			return avgV;
		}
		return null;
	}

	public PriorityQueue<Element> getTopSingleWordsSimilarTo(int nbest,Pattern matcher,String... inputs) {
		PriorityQueue<Element> ret=null;
		Set<String> inSet=new HashSet<String>();
		float[] avgV=computeAverageVector(inputs);
		//System.out.println(Arrays.toString(inputs));
		for(String i:inputs) {
			i=normalize(i);
			inSet.add(i);
		}
		if (avgV!=null) {
			float len = 0;
			for (int i = 0; i < dimensions; i++)
				len += avgV[i] * avgV[i];
			len = (float) Math.sqrt(len);
			for (int i = 0; i < dimensions; i++)
				avgV[i] /= len;
			for(int i=0;i<totalSize;i++) {
				String w=getWord(i);
				Matcher m=matcher.matcher(w);
				if (m.matches()) {
					if (!inSet.contains(w)) {
						float[] v=getVectorForWord(i);
						float d=similarity(v,avgV);
						if (ret==null) ret=new PriorityQueue<W2V2.Element>();
						ret.add(new Element(i,d));
						if (ret.size()>nbest) ret.poll();
					}
				}
			}
		}
		return ret;
	}
	public PriorityQueue<Element> getTopSimilarTo(int nbest,String... inputs) {
		PriorityQueue<Element> ret=null;
		Set<String> inSet=new HashSet<String>();
		float[] avgV=computeAverageVector(inputs);
		//System.out.println(Arrays.toString(inputs));
		for(String i:inputs) {
			i=normalize(i);
			inSet.add(i);
		}
		if (avgV!=null) {
			float len = 0;
			for (int i = 0; i < dimensions; i++)
				len += avgV[i] * avgV[i];
			len = (float) Math.sqrt(len);
			for (int i = 0; i < dimensions; i++)
				avgV[i] /= len;
			for(int i=0;i<totalSize;i++) {
				String w=getWord(i);
				if (!inSet.contains(w)) {
					float[] v=getVectorForWord(i);
					float d=similarity(v,avgV);
					if (ret==null) ret=new PriorityQueue<W2V2.Element>();
					ret.add(new Element(i,d));
					if (ret.size()>nbest) ret.poll();
				}

			}
		}
		return ret;
	}

	public float similarity(String ws1, String ws2) {
		return similarity(ws1.split("[\\s]+"), ws2.split("[\\s]+"));
	}
	public float similarity(String[] ws1, String[] ws2) {
		float[] avg1=computeAverageVector(ws1);
		float[] avg2=computeAverageVector(ws2);
		float d = similarity(avg1, avg2);
		return d;
	}

	public static float similarity(float[] v1, float[] v2) {
		if (v1==null || v2==null) return Float.NaN;
		assert(v1.length==v2.length);
		int l=v1.length;
		float d=0;
		for(int i=0;i<l;i++) {
			d+=v1[i]*v2[i];
		}
		return Math.abs(d);
	}

	public int getTotalSize() {
		return totalSize;
	}
	public float[] getVectorForWord(int i) {
		return vectors[i];
	}
	public String getWord(int i) {
		return vocabVects[i];
	}
	public int isWordInVocabulary(String word) {
		word=normalize(word);
		int l=getTotalSize();
		for(int i=0;i<l;i++) {
			if (vocabVects[i].equals(word)) return i;
		}
		return -1;
	}
	public int[] areWordsInVocabulary(String... words) {
		if (words!=null && words.length>0) {
			int l=words.length;
			int[] ret=new int[l];
			String[] normalizedWords=new String[l];
			for(int i=0;i<l;i++) {
				normalizedWords[i]=normalize(words[i]);
				ret[i]=-1;
			}
			int tl=getTotalSize();
			for(int i=0;i<tl;i++) {
				for(int j=0;j<l;j++) {
					String nw=normalizedWords[j];
					if (vocabVects[i].equals(nw)) {
						ret[j]=i;
						break;
					}
				}
			}
			return ret;
		}
		return null;
	}
	public boolean containsWord(String... words) {
		if (words!=null) {
			for(String w:words) {
				if (isWordInVocabulary(w)<0) return false;
			}
			return true;
		}else return false;
	}

	private List<String> getWords() {
		List<String> ret=null;
		for(int i=0;i<totalSize;i++) {
			String w=getWord(i);
			if (ret==null) ret=new ArrayList<String>();
			ret.add(w);
		}
		if (ret!=null) {
			Collections.sort(ret);
		}
		return ret;
	}
	private void printWords(PrintStream out) {
		for(int i=0;i<totalSize;i++) {
			String w=getWord(i);
			out.println(w);
		}
	}

	/**
	 * combines all lists, sorts by number of lists that contain a certain element and break ties by using the average similarity score.
	 * @param lists
	 * @return
	 */
	public List<Element> intersect(Collection<Element>... lists) {
		Map<Integer,List<Element>> things=new HashMap<Integer, List<Element>>();
		for(Collection<Element> l:lists) {
			for(Element e:l) {
				Integer id=e.thing;
				List<Element> sameThings=things.get(id);
				if (sameThings==null) things.put(id, sameThings=new ArrayList<W2V2.Element>());
				sameThings.add(e);
			}
		}
		List<List<Element>> all=new ArrayList<List<Element>>(things.values());
		Collections.sort(all, new Comparator<List<Element>>() {
			@Override
			public int compare(List<Element> o1, List<Element> o2) {
				int l1=o1.size(),l2=o2.size();
				int d=l1-l2;
				if (d==0) {
					float avg1=0,avg2=0;
					for(Element e1:o1) avg1+=e1.similarity;
					avg1/=(float)l1;
					for(Element e2:o2) avg2+=e2.similarity;
					avg2/=(float)l2;
					return (int)Math.signum(avg1-avg2);
				}
				return d;
			}
		});
		List<Element> ret=new ArrayList<W2V2.Element>();
		for(List<Element> l:all) {
			ret.add(l.get(0));
		}
		return ret;
	}

	/**
	 * @param args
	 *            the input C vectors file, output Java vectors file
	 */
	public static void main(String[] args) throws Exception {
		W2V2 w2v = new W2V2(new File("C:/cygwin/home/morbini/word2vec/GoogleNews-vectors-negative300.bin"), 1f);
		//w2v.printWords(new PrintStream(new File("w2vdump.txt")));
		//System.out.println(w2v.similarity("it", "this"));
		//System.out.println(w2v.similarity("cans", "container"));
		//System.out.println(w2v.getWord(10));
		PriorityQueue<Element> l1=w2v.getTopSimilarTo(10, "cake");
		PriorityQueue<Element> l2=w2v.getTopSimilarTo(10, "fruit");
		PriorityQueue<Element> l3=w2v.getTopSimilarTo(10, "sugar");
		List<Element> r = w2v.intersect(l1,l2,l3);
		System.out.println(r);
	}
}
