package edu.usc.ict.nl.nlu.keyword;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ActualMultiREMatcher implements Comparable<ActualMultiREMatcher> {
	private Pattern p=null;
	private final String topicID; 
	private Matcher m=null;
	String text;
	
	public ActualMultiREMatcher(String topicIdentifier,List<String> patterns) {
		topicID=topicIdentifier;
		p=loadPatterns(patterns);
	}
	public ActualMultiREMatcher(String topicIdentifier, File patternFile) {
		this(topicIdentifier,loadPatternsFromFile(patternFile));
	}
	
	private void reset() {
		if (m!=null) {
			m.reset();
		}
		text=null;
	}
	
	public static List<String> loadPatternsFromFile(File patternFile) {
		List<String> ret=null;
		try {
			String line;
			BufferedReader in=new BufferedReader(new FileReader(patternFile));
			StringBuffer patternString=new StringBuffer();
			int lineNumber=1;
			while((line=in.readLine())!=null) {
				try {
					Pattern.compile(line);
					if (ret==null) ret=new ArrayList<>();
					ret.add(line);
				} catch (PatternSyntaxException e) {
					KeywordREMatcher.logger.error("skipping pattern on line "+lineNumber+" in file "+patternFile,e);
				}
				lineNumber++;
			}
			in.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return ret;
	}
	public Pattern loadPatterns(List<String> patterns) {
		try {
			StringBuffer patternString=new StringBuffer();
			for(String line:patterns) {
				try {
					Pattern.compile(line);
					if (patternString.length()>0) patternString.append("|");
					patternString.append("(?:"+line+")");
				} catch (PatternSyntaxException e) {
					KeywordREMatcher.logger.error("skipping pattern '"+line+"'.",e);
				}
			}
			if (patternString.length()>0) return Pattern.compile(patternString.toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void set(String text) {
		if (p!=null) {
			this.m=p.matcher(text);
			this.text=text;
		} else m=null;
	}
	
	public boolean matches(String text) {
		set(text);
		return matches();
	}
	public boolean matches() {
		return (m!=null)?m.matches():false;
	}
	public boolean find(String text) {
		set(text);
		return find();
	}
	public boolean find() {
		return (m!=null)?m.find():false;
	}
	public boolean find(int start) {
		return (m!=null)?m.find(start):false;
	}
	
	public String getMatchedString() {
		return getMatchedString(this.text);
	}
	public String getMatchedString(String text) {
		if (m!=null) {
			try {
				return text.substring(m.start(), m.end());
			} catch (Exception e) {}
		}
		return null;
	}
	public int getStart() {
		if (m!=null) return m.start();
		return -1;
	}
	public int getEnd() {
		if (m!=null) return m.end();
		return -1;
	}

	public int getGroupStart(int p) {
		if (m!=null) return m.start(p);
		return -1;
	}
	public int getGroupEnd(int p) {
		if (m!=null) return m.end(p);
		return -1;
	}
	public int getGroupCount() {
		if (m!=null) return m.groupCount();
		return -1;
	}
	
	public String getTopicID() {
		return topicID;
	}

	@Override
	public int compareTo(ActualMultiREMatcher o) {
		if (o!=null) return o.topicID.compareTo(topicID);
		return -1;
	}
	@Override
	public boolean equals(Object obj) {
		if (obj!=null && obj instanceof ActualMultiREMatcher) {
			return ((ActualMultiREMatcher) obj).topicID.equals(topicID);
		} else return false;
	}

}