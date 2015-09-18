package edu.usc.ict.nl.nlg.lf;

import java.util.List;

import edu.usc.ict.nl.bus.events.DMSpeakEvent;
import edu.usc.ict.nl.bus.events.NLGEvent;
import edu.usc.ict.nl.config.NLBusConfig;
import edu.usc.ict.nl.config.NLGConfig;
import edu.usc.ict.nl.kb.DialogueKBInterface;
import edu.usc.ict.nl.nlg.echo.EchoNLG;
import edu.usc.ict.nl.nlg.lf.pos.POS;

public class LFNLG extends EchoNLG {

	private COPANLG2 nlg;
	
	public LFNLG(NLGConfig c) {
		super(c);
		nlg = new COPANLG2(c.getLfNlgLexiconFile(),logger);
	}

	@Override
	protected String getTextForSpeechAct(String sa, DialogueKBInterface is,boolean simulate) throws Exception {
		String lf=super.getTextForSpeechAct(sa, is, simulate);
		NLG2Data result=nlg.getSyntaxPrecursor(lf.split(","));
		List<POS> order=nlg.generateSyntax(result);
		String ss=nlg.processSyntax(order);
		return ss;
	}
	
	public static void main(String[] args) {
		NLBusConfig bc=NLBusConfig.WIN_EXE_CONFIG.cloneObject();
		LFNLG xx = new LFNLG(bc.nlgConfig);
		try {
			NLGEvent r = xx.doNLG(null, new DMSpeakEvent(null, "test", null, null, null), false);
			System.out.println(r);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
