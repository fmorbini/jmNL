package edu.usc.ict.nl.nlu.preprocessing;

import java.net.URL;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.config.PreprocessingConfig;
import edu.usc.ict.nl.utils.LogConfig;

public abstract class Preprocesser implements PreprocesserI {

	private NLU nlu;
	protected static final Logger logger = Logger.getLogger(Preprocesser.class.getName());
	static {
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );
	}

	@Override
	public void setNlu(NLU nlu) {
		this.nlu=nlu;
	}
	
	public PreprocessingConfig getConfiguration() {
		return nlu.getConfiguration().getPreprocessingConfig();
	}
	
	public NLUConfig getNluConfiguration() {return nlu.getConfiguration();}
}
