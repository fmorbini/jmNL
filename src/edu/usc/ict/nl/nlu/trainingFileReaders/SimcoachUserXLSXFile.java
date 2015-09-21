package edu.usc.ict.nl.nlu.trainingFileReaders;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.util.StringUtils;
import edu.usc.ict.nl.utils.ExcelUtils;

public class SimcoachUserXLSXFile implements NLUTrainingFileI {

	protected int skip=0;
	private static final int sacolumn=4,uttcolumn=5;
	
	public static int getSpeechActColumn() {return sacolumn;}
	public static int getUtteranceColumn() {return uttcolumn;}
	private Iterator<Row> rowIter=null;
	private String lastSpeechAct=null;

	public SimcoachUserXLSXFile(int skip) {
		setSkip(skip);
	}

	private void resetRowIterator(File input) throws Exception {
		Sheet sheet = ExcelUtils.getSpreadSheet(input.getAbsolutePath(), 0);
		if (sheet != null) {
			rowIter = sheet.rowIterator();
			lastSpeechAct=null;
		}
	}
	
	@Override
	public TrainingDataFormat getNextTrainingInstance(File input) throws Exception {
		if (rowIter==null) {
			resetRowIterator(input);
		}
		if (rowIter!=null) {
			for(; rowIter.hasNext(); ) {
				Row row = rowIter.next();
				// skip first row
				if (row.getRowNum()>getSkip()) {
					for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
						Cell cell = cellIter.next();
						if (cell.getCellType() == Cell.CELL_TYPE_STRING || cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
							int column=cell.getColumnIndex();
							switch(column) {
							case sacolumn:
								String tmp=cell.getStringCellValue();
								if (!StringUtils.isEmptyString(tmp)) lastSpeechAct = StringUtils.cleanupSpaces(tmp);
								break;
							case uttcolumn:
								String utterance=(cell.getCellType()==Cell.CELL_TYPE_NUMERIC)?cell.getNumericCellValue()+"":cell.getStringCellValue();
								if (!StringUtils.isEmptyString(lastSpeechAct) && !StringUtils.isEmptyString(utterance)) {
									//utterance=prepareUtteranceForClassification(utterance);
									return new TrainingDataFormat(utterance, lastSpeechAct);
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	@Override
	public List<TrainingDataFormat> getTrainingInstances(File fileToRead) throws Exception {
		List<TrainingDataFormat> td=new ArrayList<TrainingDataFormat>();
		TrainingDataFormat instance=null;
		resetRowIterator(fileToRead);
		while((instance=getNextTrainingInstance(null))!=null) {
			td.add(instance);
		}
		return td;
	}

	public void setSkip(int skip) {this.skip=skip;}
	public int getSkip() {return skip;}

}
