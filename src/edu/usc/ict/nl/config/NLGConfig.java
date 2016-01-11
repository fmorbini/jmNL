package edu.usc.ict.nl.config;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import edu.usc.ict.nl.nlg.picker.NLGPickerI;
import edu.usc.ict.nl.nlg.picker.ShuffleAndLeastRecent;
import edu.usc.ict.nl.util.StringUtils;

public class NLGConfig extends NLConfig {
	// NLG specific
	private String nlgClass;
	private boolean strictNLG=false;
	private boolean asciiNLG=false;
	private boolean normalizeBlanksNLG=false;
	private boolean allowEmptyNLGOutput=true;
	private boolean displayFormAnswerInNlg=true;
	private String lfNlgLexiconFile;
	private NLGPickerI picker=new ShuffleAndLeastRecent();
	private boolean alwaysPreferForms;
	private float defaultDuration=30f;

	private boolean nlgVhGenerating=false;
	private boolean nlgVhListening=false;

	private String systemUtterancesFile,systemResourcesFile,systemFormsFile,nvbFile=null; 

	public float getDefaultDuration() {	return defaultDuration; }
	public void setDefaultDuration(float defaultDuration) {	this.defaultDuration = defaultDuration; }
	
	public String getLfNlgLexiconFile() {
		if (nlBusConfig!=null)
			return nlBusConfig.getXLSXContentRoot()+lfNlgLexiconFile;
		else return lfNlgLexiconFile;
	}
	public void setLfNlgLexiconFile(String file) {this.lfNlgLexiconFile = removeAbsolutePath(file);}

	public String getNlgClass() {return nlgClass;}
	public void setNlgClass(String nlg) {this.nlgClass=nlg;}
	
	public final boolean getIsStrictNLG() { return strictNLG; }
	public final void setIsStrictNLG(boolean s) { this.strictNLG = s; }
	public final boolean getIsAsciiNLG() { return asciiNLG; }
	public final void setIsAsciiNLG(boolean s) { this.asciiNLG = s; }
	public final boolean getIsNormalizeBlanksNLG() { return normalizeBlanksNLG; }
	public final void setIsNormalizeBlanksNLG(boolean s) { this.normalizeBlanksNLG = s; }
	public boolean getDisplayFormAnswerInNlg() {return displayFormAnswerInNlg;}
	public void setDisplayFormAnswerInNlg(boolean s) {this.displayFormAnswerInNlg=s;}
	public boolean getAllowEmptyNLGOutput() {return allowEmptyNLGOutput;}
	public void setAllowEmptyNLGOutput(boolean allowEmptyNLGOutput) {this.allowEmptyNLGOutput = allowEmptyNLGOutput;}

	public boolean getNlgVhGenerating() {return nlgVhGenerating;}
	public void setNlgVhGenerating(boolean s) {this.nlgVhGenerating=s;}
	public boolean getNlgVhListening() {return nlgVhListening;}
	public void setNlgVhListening(boolean s) {this.nlgVhListening=s;}

	/** Prefer Forms Mode */
	public final boolean getAlwaysPreferForms() { return alwaysPreferForms; }
	public final void setAlwaysPreferForms(boolean status) { this.alwaysPreferForms = status; }

	public void setPicker(NLGPickerI picker) {
		this.picker = picker;
	}
	public NLGPickerI getPicker() {
		return picker;
	}
	
	public String getXLSXContentRoot() {
		return (nlBusConfig!=null)?nlBusConfig.getXLSXContentRoot():"";
	}

	public String getSystemUtterances() {return getXLSXContentRoot()+systemUtterancesFile;}
	public void setSystemUtterances(String file) {this.systemUtterancesFile = removeAbsolutePath(file);}
	public String getNvbs() {return getXLSXContentRoot()+nvbFile;}
	public void setNvbs(String file) {this.nvbFile = removeAbsolutePath(file);}
	public String getSystemForms() {return getXLSXContentRoot()+systemFormsFile;}
	public void setSystemForms(String file) {this.systemFormsFile = removeAbsolutePath(file);}
	public String getSystemResources() {return getXLSXContentRoot()+systemResourcesFile;}
	public void setSystemResources(String file) {this.systemResourcesFile = removeAbsolutePath(file);}

	@Override
	public NLGConfig getNlgConfigNC() {
		return this;
	}
	@Override
	public NLUConfig getNluConfigNC() {
		return getNlBusConfigNC().getNluConfigNC();
	}
	@Override
	public DMConfig getDmConfigNC() {
		return getNlBusConfigNC().getDmConfigNC();
	}
	
	public NLGConfig cloneObject() {
		NLGConfig ret=null;
		try {
			// get all methods for which we have a getter and a setter.
			Constructor<? extends NLGConfig> constructor = this.getClass().getConstructor();
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
	public static final NLGConfig WIN_EXE_CONFIG=new NLGConfig();
	static {
		WIN_EXE_CONFIG.setAlwaysPreferForms(false);
	}
}
