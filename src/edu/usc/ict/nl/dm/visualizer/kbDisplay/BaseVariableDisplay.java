package edu.usc.ict.nl.dm.visualizer.kbDisplay;

import java.awt.Color;
import java.awt.Font;
import java.lang.reflect.Field;

import javax.swing.JPanel;

public class BaseVariableDisplay extends JPanel implements VarDisplay {

	protected String trueValue="true",falseValue="false";
	protected Color trueColor=Color.GREEN,falseColor=Color.BLACK,nullColor=Color.GRAY;
	protected String trueText,falseText,nullText;
	protected int fontSize=12;
	protected Font font;
	
	public void setFontSize(String fontSizeS) {
		setFontSize(Integer.parseInt(fontSizeS));
	}
	public void setFontSize(int fontSize) {
		this.fontSize = fontSize;
		this.font = new Font("Serif", Font.BOLD, fontSize);
		setFont(font);
	}
	
	public void setTrueText(String trueText) {
		this.trueText = trueText;
	}
	public String getTrueText() {
		return trueText;
	}
	public void setNullText(String nullText) {
		this.nullText = nullText;
	}
	public String getNullText() {
		return nullText;
	}
	public void setFalseText(String falseText) {
		this.falseText = falseText;
	}
	public String getFalseText() {
		return falseText;
	}
	public Color getFalseColor() {
		return falseColor;
	}
	private Color getcolorFromString(String colorS) { 
		Color color;
		try {
		    Field field = Color.class.getField(colorS);
		    color = (Color)field.get(null);
		} catch (Exception e) {
		    color = null;
		}
		return color;
	}
	public void setFalseColor(String falseColor) {
		Color color=getcolorFromString(falseColor);
		this.falseColor = (color==null)?Color.BLACK:color;
	}
	public Color getNullColor() {
		return nullColor;
	}
	public void setNullColor(String nullColor) {
		Color color=getcolorFromString(nullColor);
		this.nullColor = (color==null)?Color.GRAY:color;
	}
	public Color getTrueColor() {
		return trueColor;
	}
	public void setTrueColor(String trueColor) {
		Color color=getcolorFromString(trueColor);
		this.trueColor = (color==null)?Color.BLACK:color;
	}
	public void setTrueValue(String trueValue) {
		this.trueValue = trueValue;
	}
	public String getTrueValue() {
		return trueValue;
	}
	public String getFalseValue() {
		return falseValue;
	}

	@Override
	public void setPrettyText(String prettyName) {
		if (getTrueText()==null) setTrueText(prettyName); 
		if (getFalseText()==null) setFalseText(prettyName); 
		if (getNullText()==null) setNullText(prettyName); 
	}
	@Override
	public void setValue(String internalName,Object vv) {
	}
	
	@Override
	public float getAlignmentX() {
		return LEFT_ALIGNMENT;
	}
}
