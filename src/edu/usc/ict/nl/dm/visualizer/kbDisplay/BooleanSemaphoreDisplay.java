package edu.usc.ict.nl.dm.visualizer.kbDisplay;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;

public class BooleanSemaphoreDisplay extends BaseVariableDisplay {
	
	private Boolean state=false;
	private JLabel textLabel;
	private JComponent semaphorePanel;
	private int circleSize=20,space=20;
	

	public BooleanSemaphoreDisplay() {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		buildGUI();
	}

	public void buildGUI() {
		semaphorePanel=(JComponent) Box.createRigidArea(new Dimension(circleSize+space, circleSize));
		((JComponent) semaphorePanel).setAlignmentY(CENTER_ALIGNMENT);
		add(semaphorePanel);
		textLabel=new JLabel();
		textLabel.setAlignmentY(CENTER_ALIGNMENT);
		setFontSize(fontSize);
		add(textLabel);
	}
	
	public void setCircleSize(String circleSizeS) {
		this.circleSize = Integer.parseInt(circleSizeS);
		removeAll();
		buildGUI();
	}
	public void setSpace(String spaceS) {
		this.space = Integer.parseInt(spaceS);
		removeAll();
		buildGUI();
	}
	
	@Override
	public void setValue(String internalName,Object vv) {
		if ((vv==getTrueValue()) || (vv!=null && vv.toString().equalsIgnoreCase(getTrueValue()))) {
			textLabel.setText(getTrueText());
			state=true;
		} else if ((vv==getFalseValue()) || (vv!=null && vv.toString().equalsIgnoreCase(getFalseValue()))) {
			textLabel.setText(getFalseText());
			state=false;
		} else {
			textLabel.setText(getNullText());
			state=null;
		}
		repaint();
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g);

		Point semaphorePoint = semaphorePanel.getLocationOnScreen();
		Point containerPoint = getLocationOnScreen();
		int x=semaphorePoint.x-containerPoint.x;
		int y=semaphorePoint.y-containerPoint.y;
		Color c=(state==null)?getNullColor():((state)?getTrueColor():getFalseColor());
		g.setColor(c);
		g.drawOval(x,y,semaphorePanel.getHeight(),semaphorePanel.getHeight());
		g.setColor(c);
		g.fillOval(x,y,semaphorePanel.getHeight(),semaphorePanel.getHeight()); 
	}
	


}
