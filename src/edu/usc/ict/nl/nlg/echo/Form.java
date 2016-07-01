package edu.usc.ict.nl.nlg.echo;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.utils.ExcelUtils;

public class Form {
	private TrainingDataFormat form;
	private List<TrainingDataFormat> userOptions;
	
	public void setFormSpeechAct(String sa) {
		if (form==null) form=new TrainingDataFormat();
		form.setLabel(sa);
	}
	public void setFormText(String text) {
		if (form==null) form=new TrainingDataFormat();
		form.setUtterance(text);
	}
	
	public void addUserOption(String sa,String text) {
		if (userOptions==null) userOptions=new ArrayList<>();
		try {
			userOptions.add(new TrainingDataFormat(text, sa));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public String getFormSpeechAct() {
		if (form!=null) return form.getLabel();
		return null;
	}
	public String getFormText() {
		if (form!=null) return form.getUtterance();
		return null;
	}
	public List<TrainingDataFormat> getUserOptions() {
		if (userOptions!=null) return userOptions;
		return null;
	}
	
	// question sa -> pair<response sa, sasponse text>
	public static void dumpFormResponsesFromXLSX(Collection<Form> forms,String file,String sheetName,final String[] header) throws Exception {
		dumpFormResponsesFromXLSX(forms,file, 0,sheetName,header);
	}
	public static void dumpFormResponsesFromXLSX(Collection<Form> forms,String xlsOutputFile,int skip,String sheetName,final String[] header) throws Exception {
		Workbook book = ExcelUtils.buildBaseWorkbook(header,sheetName);
		Sheet sheet = book.getSheet(sheetName);

		int i=1;
		for(Form q:forms) {
			Row row = ExcelUtils.insertRowAt(sheet, i++, 8+1);
			row.getCell(2).setCellValue("form");
			row.getCell(5).setCellValue(q.getFormSpeechAct());
			row.getCell(8).setCellValue(q.getFormText());
			for(TrainingDataFormat a:q.getUserOptions()) {
				row = ExcelUtils.insertRowAt(sheet, i++, 8+1);
				row.getCell(2).setCellValue("response");
				row.getCell(5).setCellValue(a.getLabel());
				row.getCell(8).setCellValue(a.getUtterance());
			}
		}

		ExcelUtils.autoLayout((header!=null)?header.length:2, sheet);
		FileOutputStream out=new FileOutputStream(xlsOutputFile);
		book.write(out);
	}

}
