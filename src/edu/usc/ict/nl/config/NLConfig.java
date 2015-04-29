package edu.usc.ict.nl.config;

import java.io.File;

import edu.usc.ict.nl.util.StringUtils;



public abstract class NLConfig {
	// don't add both getters and setters for the above otherwise the cloneObject method will
	// try to automatically get and set those properties 
	public NLBusConfig nlBusConfig=null;
	public NLUConfig nluConfig=null;

	public void setNluConfig(NLUConfig nluConfig) {
		this.nluConfig = nluConfig;
		if (this instanceof NLBusConfig) this.nluConfig.nlBusConfig=(NLBusConfig) this;
	}
	
	public NLConfig() {
		if (this instanceof NLUConfig) nluConfig=(NLUConfig) this;
		else if (this instanceof NLBusConfig) nlBusConfig=(NLBusConfig) this;
	}
	
	/** Executable platform */
	public static enum ExecutablePlatform {
		UNKNOWN, MACOSX, LINUXi386, WIN32;

		@Override
		public String toString() {
			if (this.equals(MACOSX))
				return "macosx";
			else if (this.equals(LINUXi386))
				return "linux-i386";
			else if (this.equals(WIN32))
				return "win32";
			else
				return null;
		}
	}
	protected ExecutablePlatform getExecutablePlatform() {
		ExecutablePlatform result = ExecutablePlatform.UNKNOWN;
		String os = System.getProperty("os.name").toLowerCase();
		if(os.indexOf( "lin" ) >= 0)
			result = ExecutablePlatform.LINUXi386;
		else if(os.indexOf( "mac" ) >= 0)
			result = ExecutablePlatform.MACOSX;
		else if(os.indexOf( "win" ) >= 0)
			result = ExecutablePlatform.WIN32;
		return result;
	}
	
	protected String removeAbsolutePath(String fileName) {
		if (!StringUtils.isEmptyString(fileName)) {
			File f=new File(fileName);
			if  (f.isAbsolute()) return f.getName();
		}
		return fileName;
	}
	
	public boolean checkLinking() {
		if (nluConfig!=null) return nluConfig.nlBusConfig==nlBusConfig;
		if (nlBusConfig!=null) return nlBusConfig.nluConfig==nluConfig;
		else return false;
	}
	
	@Override
	public NLConfig clone() throws CloneNotSupportedException {
		assert(this.checkLinking());
		if (this instanceof NLUConfig) {
			NLUConfig ret=nluConfig.cloneObject();
			if (nlBusConfig!=null) {
				NLBusConfig retNlBusConfig = nlBusConfig.cloneObject();
				ret.nlBusConfig=retNlBusConfig;
				retNlBusConfig.nluConfig=ret;
			}

			assert(ret.checkLinking());
			assert(this.checkLinking());
			return ret;
		} else if (this instanceof NLBusConfig) {
			NLBusConfig ret=nlBusConfig.cloneObject();
			if (nluConfig!=null) {
				NLUConfig retNLUConfig = nluConfig.cloneObject();
				ret.nluConfig=retNLUConfig;
				retNLUConfig.nlBusConfig=ret;
			}
			
			assert(ret.checkLinking());
			assert(this.checkLinking());
			return ret;
		}
		else throw new CloneNotSupportedException("invalid type to clone: "+this.getClass()); 
	}
	
}
