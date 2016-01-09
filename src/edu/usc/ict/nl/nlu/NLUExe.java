package edu.usc.ict.nl.nlu;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import edu.usc.ict.nl.bus.modules.NLU;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.config.NLUConfig;
import edu.usc.ict.nl.nlu.trainingFileReaders.MXNLUTrainingFile;
import edu.usc.ict.nl.nlu.trainingFileReaders.NLUTrainingFileI;
import edu.usc.ict.nl.util.PerformanceResult;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.vhmsg.VHBridge;
import edu.usc.ict.vhmsg.MessageEvent;
import edu.usc.ict.vhmsg.MessageListener;

public class NLUExe extends NLU {

	private enum MODE {RUN,TRAIN,TEST};
	private static NLUTrainingFileI reader=null;
	private static String beanName=null;
	private static File modelFile=null,trainOrTestFile=null,rootDir=null;
	private static MODE mode=MODE.RUN;
	private static NLU nlu=null;
	private static VHBridge vhBridge=null;
	private static int nBest=2;
	private static String vhServer="localhost",vhScope="DEFAULT_SCOPE";
	private static int vhMessagesSentCounter=1;
	
	public NLUExe(NLUConfig c) throws Exception {
		super(c);
	}
	
	private static final String READER="reader", TRAIN_NLU_OPTION="t",TEST_NLU_OPTION="e",RUN_NLU_OPTION="r",ROOTDIR_OPTION="root",MODEL_NLU_OPTION="m",HELP_OPTION="h",SPRING_CONFIG="s",BEANNAME="n",USEVHMSGS="usevh",VHSERVER="vhserver",VHSCOPE="vhscope";
	private static final Options options = new Options();
	static {
		Option model=OptionBuilder.withArgName("model_file").withDescription("selects which NLU model file to use").hasArg().create(MODEL_NLU_OPTION);
		Option nluDir=OptionBuilder.withArgName("root-dir-for-nlu-files").withDescription("selects the directory where the training files and nlu models are going to be found/created").isRequired().hasArg().create(ROOTDIR_OPTION);
		Option bean=OptionBuilder.withArgName("id").withDescription("selects the particular NLU to use as defined in the provided spring config file").hasArg().isRequired().create(BEANNAME);
		OptionGroup mode=new OptionGroup();
		mode.setRequired(true);
		Option train=OptionBuilder.withDescription("Trains the nlu using the provided file.").hasArg().create(TRAIN_NLU_OPTION);
		Option test=OptionBuilder.withDescription("Tests the nlu using the provided file.").hasArg().create(TEST_NLU_OPTION);
		Option run=OptionBuilder.withDescription("Runs the given nlu.").hasArg(false).create(RUN_NLU_OPTION);
		mode.addOption(train);
		mode.addOption(test);
		mode.addOption(run);
		options.addOption(SPRING_CONFIG, true, "specifies the spring config file to load.");
		options.addOption(VHSERVER, true, "VH server to which to connect");
		options.addOption(VHSCOPE, true, "VH scope to be used");
		options.addOption(USEVHMSGS, false, "Enables the use of VH messages");
		options.addOptionGroup(mode);
		options.addOption(HELP_OPTION, false, "Request this help message to be printed.");
		options.addOption(model);
		options.addOption(nluDir);
		options.addOption(bean);
		options.addOption(READER, true, "specifies the class to use to read the input training file (default: "+MXNLUTrainingFile.class.getCanonicalName()+").");
	}
	private static void printUsageHelp() {
		HelpFormatter f = new HelpFormatter();
		f.printHelp("[OPTIONS] -"+BEANNAME+" <id> -"+MODEL_NLU_OPTION+" <model_file>", options);
	}
	private static void digestCommandLineArguments(String[] args) throws Exception {
		CommandLineParser parser = new PosixParser();
		try {
			CommandLine cmd = parser.parse( options, args);
			if ( cmd.hasOption('h') ) {
				printUsageHelp();
			} else {
				beanName=cmd.getOptionValue(BEANNAME);
				if (cmd.hasOption(MODEL_NLU_OPTION)) {
					modelFile=new File(cmd.getOptionValue(MODEL_NLU_OPTION));
				}
				rootDir=new File(cmd.getOptionValue(ROOTDIR_OPTION)).getAbsoluteFile();
				if (cmd.hasOption(RUN_NLU_OPTION)) mode=MODE.RUN;
				else if (cmd.hasOption(TRAIN_NLU_OPTION)) {
					trainOrTestFile=new File(cmd.getOptionValue(TRAIN_NLU_OPTION));
					mode=MODE.TRAIN;
				} else if (cmd.hasOption(TEST_NLU_OPTION)) {
					trainOrTestFile=new File(cmd.getOptionValue(TEST_NLU_OPTION));
					mode=MODE.TEST;
				}
				if (cmd.hasOption(SPRING_CONFIG)) {
					springConfig=cmd.getOptionValue(SPRING_CONFIG);
				}
				if (cmd.hasOption(READER)) {
					String readerConfig=StringUtils.cleanupSpaces(cmd.getOptionValue(READER));
					if (!StringUtils.isEmptyString(readerConfig)) {
						reader=(NLUTrainingFileI) Class.forName(readerConfig).getConstructor().newInstance();
					}
				}
				if (reader==null) reader=MXNLUTrainingFile.class.getConstructor().newInstance();
				if (cmd.hasOption(USEVHMSGS)) {
					vhBridge=new VHBridge(vhServer, vhScope);
					vhBridge.addMessageListenerFor("vrSpeech", createVrSpeechMessageListener());
				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
			printUsageHelp();
			throw e;
		}
	}
	
	private static MessageListener createVrSpeechMessageListener() {
		return new MessageListener() {

			public void messageAction(MessageEvent e)
			{
				VHBridge.VRSpeech msg=null;
				try {
					// this will fire an exception if the input message is not a vrExpress message. 
					msg=vhBridge.processVrSpeechEvent(e);
				} catch (Exception ex){
					//System.out.println("MessageListener.messageAction received non vrExpress message.");
				}
				if (msg!=null) {
					if (msg.isComplete()) {
						System.out.println(msg.getUtterance());
						try {
							List<NLUOutput> result = nlu.getNLUOutput(msg.getUtterance(), null, nBest);
							sendResult(result);
						} catch (Exception e1) {
							e1.printStackTrace();
						}
					}
				}
			}
		};
	}

	public static void sendResult(List<NLUOutput> result) {
		if (result!=null) {
			for(NLUOutput r:result) {
				if (vhBridge!=null) vhBridge.sendVRNLU(r.getId(), "test", vhMessagesSentCounter++);
				System.out.println(r);
			}
		}
	}
	
	public static void main(final String[] args) throws Exception {
		digestCommandLineArguments(args);
		NLBusConfig busConfig = NLBusConfig.WIN_EXE_CONFIG;
		busConfig.setCharacter("test");
		NLUConfig config=getNLUConfig(beanName);
		config.setNlBusConfig(busConfig);
		String nluRootDir=rootDir.getAbsolutePath();
		if (modelFile!=null) {
			config.setNluModelFile(modelFile.getPath());
		}
		if (trainOrTestFile!=null) {
			config.setNluTrainingFile(trainOrTestFile.getPath());
		}
		config.setForcedNLUContentRoot(nluRootDir);
		nlu=init(config);
		switch (mode) {
		case RUN:
			Runnable n=new Runnable() {
				
				@Override
				public void run() {
					InputStreamReader converter = new InputStreamReader(System.in);
					BufferedReader in = new BufferedReader(converter);

					String line=null;
					while (true) {
						try {
							if ((line=in.readLine())==null) break;
							List<NLUOutput> result = nlu.getNLUOutput(line, null, nBest);
							sendResult(result);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			};
			n.run();
			break;
		case TRAIN:
		case TEST:
			File inputFile=new File(config.getNluTrainingFile());
			List<TrainingDataFormat> tds = reader.getTrainingInstances(inputFile);
			if (mode==MODE.TRAIN) {
				logger.info("starting training with data from: "+inputFile);
				if (tds!=null) logger.info("lines: "+tds.size());
				else logger.info("null data.");
				nlu.train(tds, new File(config.getNluModelFile()));
			} else {
				logger.info("starting testing with data from: "+inputFile);
				if (tds!=null) logger.info("lines: "+tds.size());
				else logger.info("null data.");
				PerformanceResult result = nlu.test(tds, new File(config.getNluModelFile()),true);
				logger.info(result);
			}
			nlu.kill();
			break;
		}
		System.exit(0);
	}

}
