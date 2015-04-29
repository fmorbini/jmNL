package edu.usc.ict.nl.dm.visualizer.kbDisplay;


public class TextDisplay extends BooleanTextAndColorDisplay {

	@Override
	public void setValue(String internalName,Object vv) {
		String name=(getTrueText()==null)?internalName:getTrueText();
		 textLabel.setText(name+": "+((vv==null)?"null":vv.toString()));
	}
}
