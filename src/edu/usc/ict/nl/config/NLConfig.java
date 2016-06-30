package edu.usc.ict.nl.config;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import edu.usc.ict.nl.config.NLConfig.ExecutablePlatform;
import edu.usc.ict.nl.util.StringUtils;



public abstract class NLConfig {
	// don't add both getters and setters for the above otherwise the cloneObject method will
	// try to automatically get and set those properties 
	protected NLBusConfig nlBusConfig=null;
	protected NLUConfig nluConfig=null;
	protected DMConfig dmConfig=null;
	protected NLGConfig nlgConfig=null;

	public void setNluConfig(NLUConfig nluConfig) {
		this.nluConfig = nluConfig;
		if (this instanceof NLBusConfig) this.nluConfig.nlBusConfig=(NLBusConfig) this;
	}
	public void setDmConfig(DMConfig dmConfig) {
		this.dmConfig = dmConfig;
		if (this instanceof NLBusConfig) this.dmConfig.nlBusConfig=(NLBusConfig) this;
	}
	public void setNlgConfig(NLGConfig nlgConfig) {
		this.nlgConfig = nlgConfig;
		if (this instanceof NLBusConfig) this.nlgConfig.nlBusConfig=(NLBusConfig) this;
	}
	public void setNlBusConfig(NLBusConfig nlBusConfig) {
		this.nlBusConfig = nlBusConfig;
	}

	public NLBusConfig getNlBusConfigNC() {
		return nlBusConfig;
	}
	public DMConfig getDmConfigNC() {
		return dmConfig;
	}
	public NLUConfig getNluConfigNC() {
		return nluConfig;
	}
	public NLGConfig getNlgConfigNC() {
		return nlgConfig;
	}
	
	public NLConfig() {
		if (this instanceof NLUConfig) nluConfig=(NLUConfig) this;
		else if (this instanceof DMConfig) dmConfig=(DMConfig) this;
		else if (this instanceof NLGConfig) nlgConfig=(NLGConfig) this;
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
	/** Executable platform */
	public static ExecutablePlatform getExecutablePlatform() {
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
		if (this instanceof NLBusConfig) {
			if (nluConfig.nlBusConfig!=nlBusConfig) {
				System.err.println("nlu config with bus ("+nluConfig.nlBusConfig+") different then this: "+nlBusConfig);
				return false;
			}
			if (dmConfig.nlBusConfig!=nlBusConfig) {
				System.err.println("dm config with bus ("+dmConfig.nlBusConfig+") different then this: "+nlBusConfig);
				return false;
			}
			if (nlgConfig.nlBusConfig!=nlBusConfig) {
				System.err.println("nlg config with bus ("+nlgConfig.nlBusConfig+") different then this: "+nlBusConfig);
				return false;
			}
		} else if (this instanceof NLUConfig) {
			if (nlBusConfig.nluConfig!=nluConfig) {
				System.err.println("nl bus config with nlu ("+nlBusConfig.nluConfig+") different then this: "+nluConfig);
				return false;
			}
			if (((NLUConfig)this).getPreprocessingRunningConfig().getNlBusConfigNC()!=nlBusConfig) {
				System.err.println("nlu preprocessing with nl bus config different then this: "+nlBusConfig);
				return false; 
			}
			if (((NLUConfig)this).getPreprocessingTrainingConfig().getNlBusConfigNC()!=nlBusConfig) {
				System.err.println("nlu preprocessing with nl bus config different then this: "+nlBusConfig);
				return false; 
			}
		} else if (this instanceof NLGConfig) {
			if (nlBusConfig.nlgConfig!=nlgConfig) {
				System.err.println("nl bus config with nlg ("+nlBusConfig.nlgConfig+") different then this: "+nlgConfig);
				return false;
			}
		} else if (this instanceof DMConfig) {
			if (nlBusConfig.dmConfig!=dmConfig) {
				System.err.println("nl bus config with dm ("+nlBusConfig.dmConfig+") different then this: "+dmConfig);
				return false;
			}
		} else {
			return false;
		}
		return true;
	}
	
	public void fixLinkings() {
		if (this instanceof NLBusConfig) {
			this.nluConfig.nlBusConfig=(NLBusConfig) this;
			this.dmConfig.nlBusConfig=(NLBusConfig) this;
			this.nlgConfig.nlBusConfig=(NLBusConfig) this;
		}
	}

	protected boolean isGetter(String name) { return name.startsWith("get"); }
	protected String getSetter(String name) { return name.replaceFirst("get", "set"); }
	protected void filterMethodsLeavingOnlyGettersAndSetters(Map<String, Method> mTable) {
		if (mTable!=null) {
			List<String> toBeRemoved=null;
			for(String mName:mTable.keySet()) {
				if (mName!=null && isGetter(mName)) {
					String sName=getSetter(mName);
					if (!mTable.containsKey(sName)) {
						if (toBeRemoved==null) toBeRemoved=new ArrayList<String>();
						toBeRemoved.add(mName);
					}
				}
			}
			if (toBeRemoved!=null) for(String k:toBeRemoved) mTable.remove(k);
		}
	}

	@Override
	public NLConfig clone() throws CloneNotSupportedException {
		assert(this.checkLinking());
		if (this instanceof NLUConfig) {
			NLUConfig ret=nluConfig.cloneObject();
			if (ret.getPreprocessingRunningConfig()!=null) ret.setPreprocessingRunningConfig(ret.getPreprocessingRunningConfig().cloneObject());
			if (ret.getPreprocessingTrainingConfig()!=null) ret.setPreprocessingTrainingConfig(ret.getPreprocessingTrainingConfig().cloneObject());
			if (nlBusConfig!=null) {
				NLBusConfig retNlBusConfig = nlBusConfig.cloneObject();
				ret.nlBusConfig=retNlBusConfig;
				retNlBusConfig.nluConfig=ret;
			}
			assert(ret.checkLinking());
			assert(this.checkLinking());
			return ret;
		}
		else if (this instanceof DMConfig) {
			DMConfig ret=dmConfig.cloneObject();
			if (nlBusConfig!=null) {
				NLBusConfig retNlBusConfig = nlBusConfig.cloneObject();
				ret.nlBusConfig=retNlBusConfig;
				retNlBusConfig.dmConfig=ret;
			}

			assert(ret.checkLinking());
			assert(this.checkLinking());
			return ret;
		} else if (this instanceof NLGConfig) {
			NLGConfig ret=nlgConfig.cloneObject();
			if (nlBusConfig!=null) {
				NLBusConfig retNlBusConfig = nlBusConfig.cloneObject();
				ret.nlBusConfig=retNlBusConfig;
				retNlBusConfig.nlgConfig=ret;
			}

			assert(ret.checkLinking());
			assert(this.checkLinking());
			return ret;
		} else if (this instanceof NLBusConfig) {
			NLBusConfig ret=nlBusConfig.cloneObject();
			if (nluConfig!=null) {
				NLUConfig retNLUConfig = nluConfig.cloneObject();
				if (retNLUConfig.getPreprocessingRunningConfig()!=null) retNLUConfig.setPreprocessingRunningConfig(retNLUConfig.getPreprocessingRunningConfig().cloneObject());
				if (retNLUConfig.getPreprocessingTrainingConfig()!=null) retNLUConfig.setPreprocessingTrainingConfig(retNLUConfig.getPreprocessingTrainingConfig().cloneObject());
				ret.nluConfig=retNLUConfig;
				retNLUConfig.nlBusConfig=ret;
				retNLUConfig.getPreprocessingRunningConfig().nlBusConfig=ret;
				retNLUConfig.getPreprocessingTrainingConfig().nlBusConfig=ret;
			}
			if (dmConfig!=null) {
				DMConfig retDMConfig = dmConfig.cloneObject();
				ret.dmConfig=retDMConfig;
				retDMConfig.nlBusConfig=ret;
			}
			if (nlgConfig!=null) {
				NLGConfig retNLGConfig = nlgConfig.cloneObject();
				ret.nlgConfig=retNLGConfig;
				retNLGConfig.nlBusConfig=ret;
			}

			assert(ret.checkLinking());
			assert(this.checkLinking());
			return ret;
		}
		else throw new CloneNotSupportedException("invalid type to clone: "+this.getClass()); 
	}

}
