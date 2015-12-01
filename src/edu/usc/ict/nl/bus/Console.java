package edu.usc.ict.nl.bus;

import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import edu.usc.ict.nl.ui.chat.ChatInterface;
import edu.usc.ict.nl.vhmsg.VHBridge;
import edu.usc.ict.vhmsg.MessageEvent;
import edu.usc.ict.vhmsg.MessageListener;

public class Console {

	private static String springConfig=null;
	private static NLBus nlModule=null;
	private static AbstractApplicationContext context;
	private static String vhServer="localhost",vhScope="DEFAULT_SCOPE";
	private static String beanName=null;
	private static VHBridge vhBridge=null;
	private static MessageListener createVHDMMessageListener() {
		return new MessageListener() {
			public void messageAction(MessageEvent e) {
				Map<String, ?> map = e.getMap();
				if (map!=null) {
					String msg=(String) map.get("dm");
					if (msg!=null && msg.equals("reset")) {
						nlModule.startSession(nlModule.getCharacterName4Session(ChatInterface.chatInterfaceSingleSessionID),ChatInterface.chatInterfaceSingleSessionID);
					}
				}
			}
		};
	}
	private static final String HELP_OPTION="h",SPRING_CONFIG="s",BEANNAME="n",USEVHMSGS="usevh",VHSERVER="vhserver",VHSCOPE="vhscope";
	private static final Options options = new Options();
	static {
		Option bean=OptionBuilder.withArgName("id").withDescription("selects the particular NLU to use as defined in the provided spring config file").hasArg().isRequired().create(BEANNAME);
		options.addOption(SPRING_CONFIG, true, "specifies the spring config file to load.");
		options.addOption(VHSERVER, true, "VH server to which to connect");
		options.addOption(VHSCOPE, true, "VH scope to be used");
		options.addOption(USEVHMSGS, false, "Enables the use of VH messages");
		options.addOption(HELP_OPTION, false, "Request this help message to be printed.");
		options.addOption(bean);
	}
	private static void printUsageHelp() {
		HelpFormatter f = new HelpFormatter();
		f.printHelp("", options);
	}
	private static void digestCommandLineArguments(String[] args) throws Exception {
		CommandLineParser parser = new PosixParser();
		try {
			CommandLine cmd = parser.parse( options, args);
			if ( cmd.hasOption('h') ) {
				printUsageHelp();
			} else {
				beanName=cmd.getOptionValue(BEANNAME);
				if (cmd.hasOption(SPRING_CONFIG)) {
					springConfig=cmd.getOptionValue(SPRING_CONFIG);
				}
				if (cmd.hasOption(VHSCOPE)) {
					vhScope=cmd.getOptionValue(VHSCOPE);
				}
				if (cmd.hasOption(VHSERVER)) {
					vhServer=cmd.getOptionValue(VHSERVER);
				}
				if (cmd.hasOption(USEVHMSGS)) {
					vhBridge=new VHBridge(vhServer,vhScope, "dm", createVHDMMessageListener());
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
			printUsageHelp();
			throw e;
		}
	}

	
	public static NLBus getNLBus(String beanName) {
		String fileName="chat.xml";
		if (springConfig!=null) fileName=springConfig;
		context = new ClassPathXmlApplicationContext(new String[] {fileName});
		System.out.println("Initializing NL configuration with bean named: '"+beanName+"' from file: "+fileName);
		NLBus nlModule = (NLBus) context.getBean(beanName);
		return nlModule;
	}
	public static NLBus init(String beanName) throws Exception {
		nlModule=getNLBus(beanName);
		return nlModule;
	}

	public static void main(String[] args) throws Exception {
		digestCommandLineArguments(args);
		init(beanName);
		nlModule.startup();
	}
}
