package edu.usc.ict.nl.nlu.preprocessing.normalization;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.client.ClientBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLUConfig.PreprocessingType;
import edu.usc.ict.nl.nlu.Token;
import edu.usc.ict.nl.nlu.Token.TokenTypes;
import edu.usc.ict.nl.nlu.distributional.word2vec.W2V2;
import edu.usc.ict.nl.util.StringUtils;

public class MapToKnownWords extends Normalizer {

	//private W2V2 w2v=null;
	private LocalW2V2 w2v=null;
	private NLU nlu=null;
	private Info info=null;

	private class LocalW2V2 {
		
		private String baseUrl;
		private Integer timeout;
		public LocalW2V2(String baseUrl,Integer timeout) {
			this.baseUrl=baseUrl;
			this.timeout=timeout;
		}
		
		public float[] getVectorForWord(Integer pos) throws JSONException {
			String responseEntity = ClientBuilder.newClient()
					.target(baseUrl).path("w2v2/getVector").queryParam("pos", pos).queryParam("timeout", timeout)
					.request().get(String.class);
			JSONObject result=new JSONObject(responseEntity);
			JSONArray thing = result.getJSONArray("vector");
			int l=thing.length();
			float[] ret=new float[l];
			for(int i=0;i<l;i++) {
				ret[i]=(float)thing.getDouble(i);
			}
			return ret;
		}
		public Integer isWordInVocabulary(String word) throws JSONException {
			String responseEntity = ClientBuilder.newClient()
					.target(baseUrl).path("w2v2/getWordPos").queryParam("word", word).queryParam("timeout", timeout)
					.request().get(String.class);
			JSONObject result=new JSONObject(responseEntity);
			Integer pos=result.getInt("pos");
			return pos;
		}

	}
	
	private class Info {
		private String[] words=null;
		private float[][] vectors=null;
		private Set<String> wordsAsSet=null;
		
		public Info(String... words) {
			if (words!=null) {
				this.wordsAsSet=new HashSet<String>(Arrays.asList(words));
				this.words=words;
				this.vectors = getVectorsForWords(words);
			}
		}
		public Info(Set<String> kwords) {
			this((String[]) kwords.toArray());
		}

		public boolean contains(String word) {
			return wordsAsSet.contains(word);
		}

		public int length() {
			return words.length;
		}

		public float[] getVector(int pos) {
			return vectors[pos];
		}

		public String getWord(int pos) {
			return words[pos];
		}
	}
	
	public MapToKnownWords() {
    	try {
			//w2v = new W2V2(new File("C:/cygwin/home/morbini/word2vec/GoogleNews-vectors-negative300.bin"), 1f);
			w2v=new LocalW2V2("http://localhost:8080", 10);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public List<Token> normalize(List<Token> tokens,PreprocessingType type) {
		if (nlu==null || nlu!=getNLU()) {
			update(nlu.getKnownWords());
		}
		if (tokens!=null && !tokens.isEmpty()) {
			for(Token t:tokens) {
				if (t!=null && t.isType(TokenTypes.WORD)) {
					String word=t.getName();
					if (!info.contains(word)) {
						String ckw=searchClosestKnownWord(word, 0f);
						if (!StringUtils.isEmptyString(ckw)) t.setName(ckw);
					}
				}
			}
		}
		return tokens;
	}

	private void update(Set<String> knownWords) {
		this.info=new Info(knownWords);
	}
	private void update(String... knownWords) {
		this.info=new Info(knownWords);
	}

	private String searchClosestKnownWord(String word,float th) {
		try {
			if (!StringUtils.isEmptyString(word) && info!=null && w2v!=null) {
				int p=w2v.isWordInVocabulary(word);
				if (p>=0) {
					float[] wordVector=w2v.getVectorForWord(p);
					int l=info.length();
					float closest=0;
					int closestWord=-1;
					for(int i=0;i<l;i++) {
						float d=W2V2.similarity(wordVector,info.getVector(i));
						if (closestWord<0 || closest<d) {
							closest=d;
							closestWord=i;
						}
					}
					if (closestWord>=0 && closest>=th) {
						return info.getWord(closestWord);
					}
				}
			}
		} catch (Exception e) {logger.error(e);}
		return null;
	}

	private float[][] getVectorsForWords(String[] words) {
		if (w2v!=null && words!=null && words.length>0) {
			int l=words.length;
			float[][] ret=new float[l][];
			int[] knownWords=new int[l];
			for(int i=0;i<l;i++) {
				knownWords[i]=-1;
				try {
					knownWords[i]=w2v.isWordInVocabulary(words[i]);
				} catch (Exception e) {logger.error(e);}
			}
			for(int i=0;i<l;i++) {
				ret[i]=null;
				int kw=knownWords[i];
				if (kw>=0) {
					try {
						float[] v=w2v.getVectorForWord(kw);
						ret[i]=v;
					} catch (Exception e) {logger.error(e);}
				}
			}
			return ret;
		}
		return null;
	}
	
	public static void main(String[] args) {
		MapToKnownWords mtk = new MapToKnownWords();
		mtk.update("a","red","pear");
		String nw=mtk.searchClosestKnownWord("grape", 0.1f);
		System.out.println(nw);
		nw=mtk.searchClosestKnownWord("apple", 0.1f);
		System.out.println(nw);
		nw=mtk.searchClosestKnownWord("a", 0.1f);
		System.out.println(nw);
		nw=mtk.searchClosestKnownWord("fruit", 0.1f);
		System.out.println(nw);
	}
	
}
