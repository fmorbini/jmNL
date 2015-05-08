package edu.usc.ict.nl.nlu.keyword;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.utils.LogConfig;

public class KeywordREMatcher {

	private static final Pattern hierModelLine=Pattern.compile("^([^\\s]+)[\\s]+(.+)$");
	private List<TopicMatcherRE> topics=null;
	private TopicMatcherRE lastMatcher=null;
	protected static final Logger logger = Logger.getLogger(NLU.class.getName());
	static {
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );
	}

	public class TopicMatcherRE implements Comparable<TopicMatcherRE> {
		private Pattern p=null;
		private final String topicID; 
		private Matcher m=null;
		private String text;
		public TopicMatcherRE(String topicIdentifier, File patternFile) {
			topicID=topicIdentifier;
			p=loadPatternsFromFile(patternFile.getAbsolutePath());
		}

		private void reset() {
			if (m!=null) {
				m.reset();
			}
			text=null;
		}
		
		public Pattern loadPatternsFromFile(String patternFile) {
			try {
				String line;
				BufferedReader in=new BufferedReader(new FileReader(patternFile));
				StringBuffer patternString=new StringBuffer();
				int lineNumber=1;
				while((line=in.readLine())!=null) {
					try {
						Pattern.compile(line);
						if (patternString.length()>0) patternString.append("|");
						patternString.append("(?:"+line+")");
					} catch (PatternSyntaxException e) {
						logger.error("skipping pattern on line "+lineNumber+" in file "+patternFile,e);
					}
					lineNumber++;
				}
				in.close();
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

		public String getTopicID() {
			return topicID;
		}

		@Override
		public int compareTo(TopicMatcherRE o) {
			if (o!=null) return o.topicID.compareTo(topicID);
			return -1;
		}
		@Override
		public boolean equals(Object obj) {
			if (obj!=null && obj instanceof TopicMatcherRE) {
				return ((TopicMatcherRE) obj).topicID.equals(topicID);
			} else return false;
		}

	}

	public KeywordREMatcher(File modelFile) throws Exception {
		try {
			topics=null;
			String line;
			BufferedReader in=new BufferedReader(new FileReader(modelFile));
			while((line=in.readLine())!=null) {
				Matcher m=hierModelLine.matcher(line);
				if (m.matches() && (m.groupCount()==2)) {
					String topicIdentifier=m.group(1);
					File thisNodeModelFile=new File(modelFile.getParent(),new File(m.group(2)).getName());
					if (topics==null) topics=new ArrayList<TopicMatcherRE>();
					topics.add(new TopicMatcherRE(topicIdentifier,thisNodeModelFile));
				}
			}
			in.close();
		} catch (Exception e) {
			logger.warn("Error during hierarchical model building.",e);
		}
	}
	
	public boolean matches(String text) {
		if (topics!=null) {
			for(TopicMatcherRE tm:topics) {
				this.lastMatcher=tm;
				if (tm.matches(text)) {
					return true;
				}
			}
		}
		reset();
		return false;
	}
	private int topicIndex=0;
	public boolean findIn(String text) {
		if (topics!=null) {
			for(topicIndex=0;topicIndex<topics.size();topicIndex++) {
				TopicMatcherRE tm=topics.get(topicIndex);
				this.lastMatcher=tm;
				if (tm.find(text)) return true;
			}
		}
		reset();
		return false;
	}
	public boolean findNext() {
		String text=null;
		if (topics!=null) {
			boolean newTopic=false;
			for(;topicIndex<topics.size();topicIndex++) {
				TopicMatcherRE tm=topics.get(topicIndex);
				if (newTopic) tm.set(text);
				else text=tm.text;
				this.lastMatcher=tm;
				if (tm.find()) return true;
				newTopic=true;
			}
		}
		reset();
		return false;
	}
	
	public void reset() {
		topicIndex=0;this.lastMatcher=null;
	}
	
	public TopicMatcherRE getLastMatchMatcher() {
		return lastMatcher;
	}
	
	public List<TopicMatcherRE> getTopics() {
		return topics;
	}
}
