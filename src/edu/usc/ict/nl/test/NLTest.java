package edu.usc.ict.nl.test;

import java.io.File;
import java.util.List;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import edu.usc.ict.nl.util.FileUtils;

public class NLTest extends TestCase {
	
	public boolean runDialogID(String id, boolean fakeNLU,NLTester nlModule) throws Exception {
		String prb=nlModule.getConfiguration().getTargetDialoguesRoot()+"annotated-target-dialogue-simcoach-"+id+".txt";
		return nlModule.batchDM(nlModule.getConfiguration().getCharacter(),prb, fakeNLU);
	}
	private static NLTester nlModule=null;
	private static NLTester initNL() {
		NLTester.init();
		return (NLTester) NLTester.getInstance();
	}
	
	public void testTargetDialogs() throws Exception {
		List<File> files = getAllTargetDialogues();
		if (files!=null) {
			String character = nlModule.getConfiguration().getCharacter();
			for(File test:files) {
				assertTrue(nlModule.batchDM(character,test.getAbsolutePath(), false));
			}
		}
	}
	public static List<File> getAllTargetDialogues() {
		String root=nlModule.getConfiguration().getTargetDialoguesRoot();
		File rootDirectory=new File(root);
		List<File> files=null;
		if (rootDirectory.exists() && rootDirectory.isDirectory()) {
			files = FileUtils.getAllFiles(rootDirectory, "annotated-target-dialogue-simcoach-[0-9]+.txt");
		}
		return files;
	}
	
	//methods to run Eliza's testing dialogs.
	/*public void testDM25() throws Exception {assertTrue(runDialogID("25",true,nlModule));}
	public void testDM34() throws Exception {assertTrue(runDialogID("34",true,nlModule));}
	public void testDM37() throws Exception {assertTrue(runDialogID("37",true,nlModule));}
	public void testDM38() throws Exception {assertTrue(runDialogID("38",true,nlModule));}
	public void testDM40() throws Exception {assertTrue(runDialogID("40",true,nlModule));}
	public void testDM52() throws Exception {assertTrue(runDialogID("52",true,nlModule));}
	public void testDM94() throws Exception {assertTrue(runDialogID("94",true,nlModule));}
	public void testDM152() throws Exception {assertTrue(runDialogID("152",true,nlModule));}
	public void testDM224() throws Exception {assertTrue(runDialogID("224",true,nlModule));}
	public void testDM239() throws Exception {assertTrue(runDialogID("239",true,nlModule));}
	
	public void testNL25() throws Exception {assertTrue(runDialogID("25",false,nlModule));}
	public void testNL34() throws Exception {assertFalse(runDialogID("34",false,nlModule));}
	public void testNL37() throws Exception {assertTrue(runDialogID("37",false,nlModule));}
	public void testNL38() throws Exception {assertFalse(runDialogID("38",false,nlModule));}
	public void testNL40() throws Exception {assertTrue(runDialogID("40",false,nlModule));}
	public void testNL52() throws Exception {assertFalse(runDialogID("52",false,nlModule));}
	public void testNL94() throws Exception {assertFalse(runDialogID("94",false,nlModule));}
	public void testNL152() throws Exception {assertFalse(runDialogID("152",false,nlModule));}
	public void testNL224() throws Exception {assertTrue(runDialogID("224",false,nlModule));}
	public void testNL239() throws Exception {assertTrue(runDialogID("239",false,nlModule));}*/
	
	public static Test suite(){
		nlModule=initNL();
		return new TestSuite(NLTest.class);
	}
	
	public static void main(String[] args) throws Exception {
		NLTest t = new NLTest();
		nlModule=initNL();
		t.testTargetDialogs();
		//t.runDialogID("38",true,nlModule);
		//t.runDialogID("34",false,nlModule);
		//t.runDialogID("37",false,nlModule);
		System.exit(0);
	}
}