package edu.usc.ict.nl.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import edu.usc.ict.nl.nlg.picker.NLGPickerI;
import edu.usc.ict.nl.nlg.picker.ShuffleAndLeastRecent;

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

	/** Prefer Forms Mode */
	public final boolean getAlwaysPreferForms() { return alwaysPreferForms; }
	public final void setAlwaysPreferForms(boolean status) { this.alwaysPreferForms = status; }

	public void setPicker(NLGPickerI picker) {
		this.picker = picker;
	}
	public NLGPickerI getPicker() {
		return picker;
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
