package edu.usc.ict.nl.dm.visualizer.kbDisplay;

import javax.swing.BoxLayout;
import javax.swing.JLabel;

public class BooleanTextAndColorDisplay extends BaseVariableDisplay {
	
	protected JLabel textLabel;

	public BooleanTextAndColorDisplay() {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		buildGUI();
	}
	
	public void buildGUI() {
		textLabel=new JLabel();
		setFontSize(fontSize);
		add(textLabel);
	}
	
	@Override
	public void setValue(String internalName,Object vv) {
		if ((vv==getTrueValue()) || (vv!=null && vv.toString().equalsIgnoreCase(getTrueValue()))) {
			textLabel.setText(getTrueText());
			setForeground(getTrueColor());
		} else if ((vv==getFalseValue()) || (vv!=null && vv.toString().equalsIgnoreCase(getFalseValue()))) {
			textLabel.setText(getFalseText());
			setForeground(getFalseColor());
		} else {
			textLabel.setText(getNullText());
			setForeground(getNullColor());
		}
	}
	
}
