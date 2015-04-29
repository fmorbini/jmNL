package edu.usc.ict.nl.nlu;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.usc.ict.nl.bus.modules.NLUInterface;
import edu.usc.ict.nl.config.NLConfig;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.utils.LogConfig;

public class TrainNLU {

	static protected AbstractApplicationContext context;
	private static TrainNLU _instance;
	
	public static final Logger logger = Logger.getLogger(TrainNLU.class.getName());

	protected NLUConfig configuration;
	private NLUInterface nlu;
	
	public NLUConfig getConfiguration() {return configuration;}
	public void setConfiguration(NLUConfig c) {this.configuration=c;}
	
	/** singleton scope controlled via spring initialization */
	public static final TrainNLU getInstance() {
		return _instance;
	}
	
	public TrainNLU() {
		_instance=this;
	}
	
	public static void init(String args[]) {
		System.out.println("Initializing NLU training configuration.");
		URL log4Jresource=LogConfig.findLogConfig("src","log4j.properties", false);
		if (log4Jresource != null)
			PropertyConfigurator.configure( log4Jresource );		
		
		if (args==null || args.length == 0)
			context = new ClassPathXmlApplicationContext(new String[] {"nluTraining.xml"});		
		else 
			context = new ClassPathXmlApplicationContext(new String[] {args[0]});		
		
	}
	public Object createSubcomponent(String nluClassName) throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		Class nluClass = Class.forName(nluClassName);
		Constructor nluconstructor = nluClass.getConstructor(NLConfig.class);
		return nluconstructor.newInstance(getConfiguration());
	}
	public void startup() {
		logger.info("Starting training.");

		NLUConfig config=getConfiguration();

		try {
			nlu=(NLUInterface) createSubcomponent(config.getNluClass());
			logger.info("Done training, now testing.");
			nlu.retrain();
		} catch (Exception e) {
			e.printStackTrace();
		}
		logger.info("Done.");
	}
	public static void main(String[] args) {
		init(args);
		getInstance().startup();
		System.exit(0);
	}
}
