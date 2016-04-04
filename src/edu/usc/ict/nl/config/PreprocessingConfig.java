package edu.usc.ict.nl.config;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.usc.ict.nl.nlu.ne.NamedEntityExtractorI;
import edu.usc.ict.nl.nlu.preprocessing.PreprocesserI;
import edu.usc.ict.nl.nlu.preprocessing.TokenizerI;
import edu.usc.ict.nl.nlu.preprocessing.tokenizer.Tokenizer;
import edu.usc.ict.nl.util.StringUtils;

public class PreprocessingConfig extends NLConfig {
	
	private TokenizerI tokenizer;
	private List<PreprocesserI> prs;
	private List<NamedEntityExtractorI> nes;
	
	private String forcedPreprocessingContentRoot=null,preprocessingDir="preprocessing";

	/** NLU preprocessing */
	public List<PreprocesserI> getNluPreprocessers() {return prs;}
	public void setNluPreprocessers(List<PreprocesserI> prs) {this.prs=prs;}
	/** NLU tokenizer */
	public TokenizerI getNluTokenizer() {return tokenizer;}
	public void setNluTokenizer(TokenizerI t) {this.tokenizer=t;}
	/** NLU named entities */
	public List<NamedEntityExtractorI> getNluNamedEntityExtractors() {return nes;}
	public void setNluNamedEntityExtractors(List<NamedEntityExtractorI> nes) {this.nes=nes;}

	public String getPreprocessingContentRoot() {
		String forcedContentRoot=getForcedPreprocessingContentRoot();
		if (!StringUtils.isEmptyString(forcedContentRoot)) return forcedContentRoot+File.separator;
		else {
			File file=new File(new File(getNlBusConfigNC().getContentRoot()).getParent(),preprocessingDir);
			return file.getAbsolutePath();
		}
	}
	public void setForcedPreprocessingContentRoot(String forcedContentRoot) {this.forcedPreprocessingContentRoot = forcedContentRoot;}
	public String getForcedPreprocessingContentRoot() {return this.forcedPreprocessingContentRoot;}

	public PreprocessingConfig() {
		super();
	}
	
	public PreprocessingConfig cloneObject() {
		PreprocessingConfig ret=null;
		try {
			// get all methods for which we have a getter and a setter.
			Constructor<? extends PreprocessingConfig> constructor = this.getClass().getConstructor();
			ret=constructor.newInstance();
			Method[] publicMethods = getClass().getMethods();
			if (publicMethods!=null) {
				Map<String,Method> mTable=new HashMap<String, Method>();
				for(Method m:publicMethods) mTable.put(m.getName(),m);
				filterMethodsLeavingOnlyGettersAndSetters(mTable);
				for(String m:mTable.keySet()) {
					if (isGetter(m)) {
						Method getter=mTable.get(m);
						Method setter=mTable.get(getSetter(m));
						if (getter!=null && setter!=null) {
							Object v=getter.invoke(this);
							setter.invoke(ret, v);
						}
					}
				}
			}
		} catch (Exception e) {e.printStackTrace();}
		return ret;
	}
	
	// sample config used to run mxnlu during testing
	public static final PreprocessingConfig WIN_EXE_CONFIG=new PreprocessingConfig();
	static{
		WIN_EXE_CONFIG.setNluTokenizer(new Tokenizer());
	}

}
