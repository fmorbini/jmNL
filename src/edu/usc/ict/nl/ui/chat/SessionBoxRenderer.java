package edu.usc.ict.nl.ui.chat;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import edu.usc.ict.nl.bus.NLBus;

public class SessionBoxRenderer extends JLabel implements ListCellRenderer<Long> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private NLBus bus=null;
	
	public SessionBoxRenderer(NLBus bus) {
		this.bus=bus;
	}
	
	@Override
	public Component getListCellRendererComponent(JList<? extends Long> list, Long value, int index, boolean isSelected,boolean cellHasFocus) {
		String ch=bus.getCharacterName4Session(value);
		return new JLabel(ch+" "+value);
	}

}
