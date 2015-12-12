package edu.usc.ict.nl.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRElt;
import org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRst;

import edu.usc.ict.nl.nlu.TrainingDataFormat;
import edu.usc.ict.nl.util.FileUtils;
import edu.usc.ict.nl.util.Pair;
import edu.usc.ict.nl.util.StringUtils;

public class ExcelUtils {
	public static Set<String> extractUniqueValuesInThisColumn(String file,int skip,int column) {
		Set<String> ret=new LinkedHashSet<String>();
		Workbook wb=getWorkbook(file);
		Sheet sheet = getSpreadSheet(wb, 0);
		if (sheet != null)
		{
			for(Iterator<Row> rowIter = sheet.rowIterator(); rowIter.hasNext(); ) {
				Row row = rowIter.next();
				// skip first row
				if (row.getRowNum()>skip) {
					for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
						Cell cell = cellIter.next();
						if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
							int c=cell.getColumnIndex();
							if (c==column) {
								String tmp=cell.getStringCellValue();
								CellStyle style = cell.getCellStyle();
								RichTextString rs = cell.getRichStringCellValue();
								tmp=StringUtils.cleanupSpaces(tmp);
								if (!StringUtils.isEmptyString(tmp)) ret.add(tmp);
							}
						}
					}
				}
			}
		}
		return ret;
	}
	public static Map<Integer,String> extractRowAndThisColumn(String file,int skip,int column) {
		return extractRowAndThisColumn(file, 0, skip, column);
	}
	public static Map<Integer,String> extractRowAndThisColumn(String file,int sheetNum,int skip,int column) {
		Workbook wb=getWorkbook(file);
		Sheet sheet = getSpreadSheet(wb, sheetNum);
		return extractRowAndThisColumn(sheet, skip, column);
	}
	public static Map<Integer,String> extractRowAndThisColumn(Sheet sheet,int skip,int column) {
		Map<Integer,String> ret=new HashMap<Integer, String>();
		if (sheet != null)
		{
			for(Iterator<Row> rowIter = sheet.rowIterator(); rowIter.hasNext(); ) {
				Row row = rowIter.next();
				// skip first row
				if (row.getRowNum()>skip) {
					for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
						Cell cell = cellIter.next();
						int c=cell.getColumnIndex();
						if (c==column) {
							int type = cell.getCellType();
							String tmp=null;
							if (type == Cell.CELL_TYPE_STRING) tmp=cell.getStringCellValue();
							else if (type == Cell.CELL_TYPE_NUMERIC) tmp=cell.getNumericCellValue()+"";
							else if (type == Cell.CELL_TYPE_BOOLEAN) tmp=cell.getBooleanCellValue()+"";
							tmp=StringUtils.cleanupSpaces(tmp);
							if (!StringUtils.isEmptyString(tmp)) ret.put(row.getRowNum(),tmp);
						}
					}
				}
			}
		}
		return ret;
	}
	public static Collection<String> extractValuesInThisColumn(String file,int skip,int column) {
		Collection<String> ret=new ArrayList<String>();
		Workbook wb=getWorkbook(file);
		Sheet sheet = getSpreadSheet(wb, 0);
		if (sheet != null)
		{
			for(Iterator<Row> rowIter = sheet.rowIterator(); rowIter.hasNext(); ) {
				Row row = rowIter.next();
				// skip first row
				if (row.getRowNum()>skip) {
					for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
						Cell cell = cellIter.next();
						if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
							int c=cell.getColumnIndex();
							if (c==column) {
								String tmp=cell.getStringCellValue();
								CellStyle style = cell.getCellStyle();
								RichTextString rs = cell.getRichStringCellValue();
								tmp=StringUtils.cleanupSpaces(tmp);
								if (!StringUtils.isEmptyString(tmp)) ret.add(tmp);
							}
						}
					}
				}
			}
		}
		return ret;
	}
	// user-utterances excel file
	public static Map<String,List<String>> extractMappingBetweenTheseTwoColumns(String file,int skip,int keyColumn,int valueColumn) throws Exception {
		return extractMappingBetweenTheseTwoColumns(file, skip, keyColumn, valueColumn, false,true);
	}
	public static Map<String,List<String>> extractMappingBetweenTheseTwoColumns(String file,int skip,int keyColumn,int valueColumn,boolean noAutofillLabels) throws Exception {
		return extractMappingBetweenTheseTwoColumns(file, skip, keyColumn, valueColumn, noAutofillLabels,true);
	}
	public static Map<String,List<String>> extractMappingBetweenTheseTwoColumns(String file,int skip,int keyColumn,int valueColumn,boolean noAutofillLabels,boolean cleanupSpaces) throws Exception {
		Map<String,List<String>> ret=new LinkedHashMap<String, List<String>>();
		Sheet sheet = ExcelUtils.getSpreadSheet(file, 0);
		if (sheet != null)
		{
			String lastKey="";
			for(Iterator<Row> rowIter = sheet.rowIterator(); rowIter.hasNext(); ) {
				Row row = rowIter.next();
				// skip first row
				if (row.getRowNum()>skip) {
					if (noAutofillLabels) lastKey=null;
					for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
						Cell cell = cellIter.next();
						if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
							int column=cell.getColumnIndex();
							if (column==keyColumn) {
								String tmp=cell.getStringCellValue();
								if (cleanupSpaces) tmp=StringUtils.cleanupSpaces(tmp);
								if (!StringUtils.isEmptyString(tmp)) lastKey=tmp;
							} else if (column==valueColumn) {
								String value=cell.getStringCellValue();
								if (cleanupSpaces) value=StringUtils.cleanupSpaces(value);
								if (!StringUtils.isEmptyString(value)) {
									List<String> list = ret.get(lastKey);
									if (list==null) ret.put(lastKey,list=new ArrayList<String>());
									list.add(value);
								}
							}
						}
					}
				}
			}
		}
		return ret;
	}
	public static List<String> extractRowAndColumnWiseData(String file,int sheetNum,int skip,int start,int end,boolean cleanupSpaces,boolean skipBlanks) throws Exception {
		Map<Integer, String> ret=extractRowAndColumnWiseDataWithColumns(file, sheetNum, skip, start, end, cleanupSpaces, skipBlanks);
		if (ret!=null) return new ArrayList<String>(ret.values());
		return null;
	}
	public static Map<Integer,String> extractRowAndColumnWiseDataWithColumns(String file,int sheetNum,int rownum,int start,int end,boolean cleanupSpaces,boolean skipBlanks) throws Exception {
		Map<Integer,String> ret=new HashMap<Integer,String>();
		Sheet sheet = ExcelUtils.getSpreadSheet(file, sheetNum);
		if (sheet != null)
		{
			for(Iterator<Row> rowIter = sheet.rowIterator(); rowIter.hasNext(); ) {
				Row row = rowIter.next();
				if (row.getRowNum()==rownum) {
					for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
						Cell cell = cellIter.next();
						if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
							int column=cell.getColumnIndex();
							if ((column<=end || end<0) && (column>=start || start<0)) {
								String tmp=cell.getStringCellValue();
								if (cleanupSpaces) tmp=StringUtils.cleanupSpaces(tmp);
								if (!skipBlanks || !StringUtils.isEmptyString(tmp)) {
									ret.put(column,tmp);
								}
							}
						}
					}
					break;
				}
			}
		}
		return ret;
	}
	public static Map<Integer,String> extractRowAndColumnWiseDataWithColumns(Sheet sheet,int rownum,int start,int end,boolean cleanupSpaces,boolean skipBlanks) throws Exception {
		Map<Integer,String> ret=new HashMap<Integer,String>();
		if (sheet != null)
		{
			for(Iterator<Row> rowIter = sheet.rowIterator(); rowIter.hasNext(); ) {
				Row row = rowIter.next();
				if (row.getRowNum()==rownum) {
					for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
						Cell cell = cellIter.next();
						if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
							int column=cell.getColumnIndex();
							if ((column<=end || end<0) && (column>=start || start<0)) {
								String tmp=cell.getStringCellValue();
								if (cleanupSpaces) tmp=StringUtils.cleanupSpaces(tmp);
								if (!skipBlanks || !StringUtils.isEmptyString(tmp)) {
									ret.put(column,tmp);
								}
							}
						}
					}
					break;
				}
			}
		}
		return ret;
	}
	public static Map<Integer,List<String>> extractRowsAndColumnWiseData(String file,int sheetNum,int skip,int start,int end,boolean cleanupSpaces,boolean skipBlanks) throws Exception {
		Map<Integer,List<String>> ret=null;
		Sheet sheet = getSpreadSheet(file, sheetNum);
		if (sheet != null)
		{
			for(Iterator<Row> rowIter = sheet.rowIterator(); rowIter.hasNext(); ) {
				Row row = rowIter.next();
				int rown=row.getRowNum();
				if (rown>skip) {
					for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
						Cell cell = cellIter.next();
						if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
							int column=cell.getColumnIndex();
							if ((column<=end || end<0) && (column>=start || start<0)) {
								String tmp=cell.getStringCellValue();
								if (cleanupSpaces) tmp=StringUtils.cleanupSpaces(tmp);
								if (!skipBlanks || !StringUtils.isEmptyString(tmp)) {
									if (ret==null) ret=new HashMap<Integer, List<String>>();
									List<String> list = ret.get(rown);
									if (list==null) ret.put(rown, list=new ArrayList<String>());
									list.add(tmp);
								}
							}
						}
					}
				}
			}
		}
		return ret;
	}
	public static List<Pair<String,String>> extractListOfPairsBetweenTheseTwoColumns(String file,int skip,int keyColumn,int valueColumn,boolean noAutofillLabels,boolean cleanupSpaces) throws Exception {
		List<Pair<String,String>> ret=new ArrayList<Pair<String,String>>();
		Sheet sheet = ExcelUtils.getSpreadSheet(file, 0);
		if (sheet != null)
		{
			String lastKey="";
			for(Iterator<Row> rowIter = sheet.rowIterator(); rowIter.hasNext(); ) {
				Row row = rowIter.next();
				// skip first row
				if (row.getRowNum()>skip) {
					if (noAutofillLabels) lastKey=null;
					for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
						Cell cell = cellIter.next();
						if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
							int column=cell.getColumnIndex();
							if (column==keyColumn) {
								String tmp=cell.getStringCellValue();
								if (cleanupSpaces) tmp=StringUtils.cleanupSpaces(tmp);
								if (!StringUtils.isEmptyString(tmp)) lastKey=tmp;
							} else if (column==valueColumn) {
								String value=cell.getStringCellValue();
								if (cleanupSpaces) value=StringUtils.cleanupSpaces(value);
								if (!StringUtils.isEmptyString(value)) {
									ret.add(new Pair<String, String>(lastKey, value));
								}
							}
						}
					}
				}
			}
		}
		return ret;
	}
	
	public static Workbook getWorkbook(String file) {
		try {
			return WorkbookFactory.create(new FileInputStream(file));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;		
	}
	public static Sheet getSpreadSheet(Workbook wb, int sheetIndex) {
		try {
			return (wb!=null)?wb.getSheetAt(sheetIndex):null;			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	public static Sheet getSpreadSheet(String file, int sheetIndex) throws Exception {
		Workbook wb = WorkbookFactory.create(new FileInputStream(file));
		return wb.getSheetAt(sheetIndex);
	}
	
	public static XSSFWorkbook buildBaseWorkbook(String[] headers, String worksheetName) {
		final XSSFWorkbook spreadsheet = new XSSFWorkbook();
		if (worksheetName!=null) {
			final Sheet workingSheet = spreadsheet.createSheet(worksheetName);
			final Row headerRow = workingSheet.createRow(0);
			final Font headerFont = spreadsheet.createFont();
			headerFont.setFontHeightInPoints((short)18);
			headerFont.setFontName("Arial");
			headerFont.setBoldweight((short)Font.BOLDWEIGHT_BOLD);
			final CellStyle headerStyle = spreadsheet.createCellStyle();
			headerStyle.setFont(headerFont);
			headerStyle.setBorderBottom(CellStyle.BORDER_DOUBLE);

			if (headers!=null) {
				for (int i=0;i<headers.length;i++) {
					final Cell headerCell = headerRow.createCell(i);
					headerCell.setCellValue(headers[i]);
					headerCell.setCellStyle(headerStyle);
				}
			}
		}
		return spreadsheet;
	}
	public static CellStyle buildBasicDataStyle(Workbook spreadsheet) {
		final CellStyle dataStyle = spreadsheet.createCellStyle();
		final Font font = spreadsheet.createFont();
		font.setFontHeightInPoints((short)12);
		font.setFontName("Arial");
		dataStyle.setWrapText(true);
		dataStyle.setFont(font);
		return dataStyle;
	}
	public static void autoLayout(final int headerLength, Sheet workingSheet) {
		for (int i=0; i < headerLength; i++) {
			int maxLength = -1;
			for (int j=0; j<workingSheet.getLastRowNum(); j++) {
				Row row=workingSheet.getRow(j);
				if (row!=null && i<=row.getLastCellNum()) {
					final Cell cell = workingSheet.getRow(j).getCell(i);
					if (cell != null && cell.getCellType() == Cell.CELL_TYPE_STRING) {
						if (cell != null ) {
							if (cell.getRichStringCellValue().toString().trim().length()>maxLength) {
								maxLength = cell.getRichStringCellValue().toString().trim().length();
							}
						}
					}
				}
			}
			if (maxLength > 50) {
				final Float max = new Float(maxLength);
				final Float newSize = 12*max*.8f/(.1f);
				final Integer columnSize = new Integer(Math.round(newSize));
				try {
					workingSheet.setColumnWidth( i, columnSize );
				} catch (IllegalArgumentException e) { }
			} else {
				workingSheet.autoSizeColumn((short)i);
			}
		}
	}
	
	public static Row insertRowAt(Sheet sheet,int rowNumber,int numColumns) {
		if (sheet.getLastRowNum()>rowNumber) sheet.shiftRows(rowNumber, sheet.getLastRowNum(), 1);
		Row newRow = sheet.createRow(rowNumber);
		for(int i=0;i<numColumns;i++) {
			Cell newCell = newRow.createCell(i);
		}
		return newRow;
	}
	public static Row copyAndInsertRow(Sheet sheet,Row row) {
		if (sheet.getLastRowNum()>(row.getRowNum()+1)) sheet.shiftRows(row.getRowNum()+1, sheet.getLastRowNum(), 1);
		Row newRow = sheet.createRow(row.getRowNum()+1);
		for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext();) {
			Cell cell = cellIter.next();
			Cell newCell = newRow.createCell(cell.getColumnIndex());
			copyCell(newCell,cell);
		}
		return newRow;
	}
	private static void copyCell(Cell newCell, Cell cell) {
		newCell.setCellStyle(cell.getCellStyle());
		switch(cell.getCellType()) {
		case Cell.CELL_TYPE_STRING:
			newCell.setCellValue(cell.getStringCellValue());
			break;
		case Cell.CELL_TYPE_BOOLEAN:
			newCell.setCellValue(cell.getBooleanCellValue());
			break;
		case Cell.CELL_TYPE_NUMERIC:
			newCell.setCellValue(cell.getNumericCellValue());
			break;
		}
	}
	
	public static Collection<String> test(String file,int skip,int column) {

		Collection<String> ret=new LinkedHashSet<String>();
		Workbook wb=getWorkbook(file);
		Sheet sheet = getSpreadSheet(wb, 0);
		if (sheet != null)
		{
			for(Iterator<Row> rowIter = sheet.rowIterator(); rowIter.hasNext(); ) {
				Row row = rowIter.next();
				// skip first row
				if (row.getRowNum()>skip) {
					for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
						Cell cell = cellIter.next();
						int c=cell.getColumnIndex();
						if (c==column) {
							//System.out.print(cell.getRowIndex()+" ");
							if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
								String cleanedUpCell="";
								String tmp=cell.getStringCellValue();
								XSSFRichTextString rs = (XSSFRichTextString) cell.getRichStringCellValue();
								
								CTRst x = rs.getCTRst();
								CTRElt[] array = x.getRArray();
								if (array!=null && array.length>0) {
									for(CTRElt y:array) {
										String ss=y.getT();
										try {
											CTColor[] colors = y.getRPr().getColorArray();
											String color = colors[0].getDomNode().getAttributes().getNamedItem("rgb").getNodeValue();
											if (!color.equals("FF00B0F0")) {
												cleanedUpCell+=ss;
											}
										} catch (Exception ee) {
											cleanedUpCell+=ss;
										}
									}
								} else cleanedUpCell=tmp;

								tmp=cleanedUpCell;
								
								tmp=StringUtils.cleanupSpaces(tmp);
								if (!StringUtils.isEmptyString(tmp)) {
									ret.add(tmp);
								}
								System.out.print(tmp+"\n");
							} else {
								System.out.print(cell.getRowIndex()+" 1111111111111111111\n");
							}
						}
					}
				}
			}
		}
		return ret;
	}
	
	public static Map<String,List<String>> generateSasoLightTemplateFromSystemUtt(String file,int skip) throws Exception {
		Map<String,List<String>> ret=new LinkedHashMap<String, List<String>>();
		Sheet sheet = ExcelUtils.getSpreadSheet(file, 0);
		if (sheet != null)
		{
			String lastKey="",lastValue="",lastLabel="";
			for(Iterator<Row> rowIter = sheet.rowIterator(); rowIter.hasNext(); ) {
				Row row = rowIter.next();
				// skip first row
				if (row.getRowNum()>skip) {
					for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
						Cell cell = cellIter.next();
						if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
							int column=cell.getColumnIndex();
							if (column==4) {//speech act
								String tmp=cell.getStringCellValue();
								tmp=StringUtils.cleanupSpaces(tmp);
								if (!StringUtils.isEmptyString(tmp)) lastKey=tmp;
							} else if (column==5) {//text
								String value=cell.getStringCellValue();
								value=StringUtils.cleanupSpaces(value);
								if (!StringUtils.isEmptyString(value)) lastValue=value;
							} else if (column==6) {//label
								String label=cell.getStringCellValue();
								label=StringUtils.cleanupSpaces(label);
								if (!StringUtils.isEmptyString(label)) lastLabel=label;
							} else if (column==7) {//color
								String color=cell.getStringCellValue();
								color=StringUtils.cleanupSpaces(color);
								System.out.println("<button id=\""+lastKey+"\" label=\""+lastLabel+"\" tooltip=\""+lastValue+"\" "+((StringUtils.isEmptyString(color))?"":" color=\""+color+"\"")+">\n"+
										" <elvin src=\"vhmsg.tmpl\" >\n"+
										"  <param name=\"speech_act\" value=\""+lastKey+"\"/>\n"+
										" </elvin>\n"+
										"</button>\n");
							}
						}
					}
				}
			}
		}
		return ret;
	}

	public static void generateSasoLightTemplateFromMap(Map<String, List<String>> map) {
		if (map!=null) {
			for(String sa:map.keySet()) {
				List<String> lines=map.get(sa);
				if (lines!=null) {
					for(String line:lines) {
						System.out.println("<button id=\""+sa+"\" label=\""+line+"\" tooltip=\""+line+"\">\n"+
								" <elvin src=\"vhmsg.tmpl\" >\n"+
								"  <param name=\"speech_act\" value=\""+sa+"\"/>\n"+
							" </elvin>\n"+
						"</button>\n");
					}
				}
			}
		}
	}

	public static List<String> readRow(File file,int rowNum,int startCol,int endCol) throws Exception {
		List<String> ret=new ArrayList<String>();
		Sheet sheet = ExcelUtils.getSpreadSheet(file.getAbsolutePath(), 0);

		if (sheet != null)
		{
			Row row = sheet.getRow(rowNum);
			for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
				Cell cell = cellIter.next();
				if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
					int column=cell.getColumnIndex();
					if (column>=startCol && (column<=endCol || endCol<0)) {
						String tmp=cell.getStringCellValue();
						tmp=StringUtils.cleanupSpaces(tmp);
						ret.add(tmp);
					}
				}
			}
		}
		return ret;
	}
	
	public static void dumpMapToExcel(Map<String, Collection<String>> map,File xlsOutputFile,String sheetName,final String[] header) throws Exception {
		dumpMapToExcel(map, xlsOutputFile, sheetName, header, 0, 1);
	}
	public static void dumpMapToExcel(Map<String, Collection<String>> map,File xlsOutputFile,String sheetName,final String[] header, int key, int value) throws Exception {
		if (map!=null) {
			Workbook book = ExcelUtils.buildBaseWorkbook(header,sheetName);
			Sheet sheet = book.getSheet(sheetName);

			int i=1;
			for(String q:map.keySet()) {
				Collection<String> answers=map.get(q);
				for(String a:answers) {
					Row row = ExcelUtils.insertRowAt(sheet, i++, Math.max(key, value)+1);
					row.getCell(key).setCellValue(q);
					row.getCell(value).setCellValue(a);
				}
			}

			ExcelUtils.autoLayout((header!=null)?header.length:2, sheet);
			FileOutputStream out=new FileOutputStream(xlsOutputFile);
			book.write(out);
		}
	}
	public static void dumpListToExcel(List<TrainingDataFormat> list,File xlsOutputFile,String sheetName,final String[] header, int key, int value) throws Exception {
		if (list!=null) {
			Workbook book = ExcelUtils.buildBaseWorkbook(header,sheetName);
			Sheet sheet = book.getSheet(sheetName);

			int i=1;
			for(TrainingDataFormat td:list) {
				Row row = ExcelUtils.insertRowAt(sheet, i++, Math.max(key, value)+1);
				row.getCell(key).setCellValue(td.getLabel());
				row.getCell(value).setCellValue(td.getUtterance());
			}

			ExcelUtils.autoLayout((header!=null)?header.length:2, sheet);
			FileOutputStream out=new FileOutputStream(xlsOutputFile);
			book.write(out);
		}
	}
	
	public static List<List<String>> extractDataFromExcel(String file,int skip) {
		List<List<String>> ret=null;
		Workbook wb=getWorkbook(file);
		Sheet sheet = getSpreadSheet(wb, 0);
		if (sheet != null)
		{
			for(Iterator<Row> rowIter = sheet.rowIterator(); rowIter.hasNext(); ) {
				Row row = rowIter.next();
				// skip first row
				int r=row.getRowNum();
				if (r>skip) {
					for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
						Cell cell = cellIter.next();
						int type = cell.getCellType();
						if (type == Cell.CELL_TYPE_STRING) {
							int c=cell.getColumnIndex();
							if (ret==null) ret=new ArrayList<List<String>>();
							addCellToTable(ret,cell.getStringCellValue(),c,r,skip);
						} else if (type==Cell.CELL_TYPE_NUMERIC) {
							int c=cell.getColumnIndex();
							if (ret==null) ret=new ArrayList<List<String>>();
							addCellToTable(ret,cell.getNumericCellValue()+"",c,r,skip);
						}
					}
				}
			}
		}
		return ret;
	}
	private static void addCellToTable(List<List<String>> ret,String value, int c, int r, int skip) {
		// adjust the row using the skip.
		if (skip>=0) {
			r-=skip+1;
		}
		int rr=ret.size();
		if (rr<=r) {
			for(int k=rr;k<=r;k++) {
				ret.add(new ArrayList<String>());
			}
		}
		List<String> rowToEdit=ret.get(r);
		if (rowToEdit==null) ret.set(r, rowToEdit=new ArrayList<String>());
		int rc=rowToEdit.size();
		if (rc<=c) {
			for(int k=rc;k<=c;k++) {
				rowToEdit.add(null);
			}
		}
		rowToEdit.set(c, value);
	}
	
	public static void dumpDataToExcel(List<List<String>> list,File xlsOutputFile,String sheetName,final String[] header) throws Exception {
		if (list!=null) {
			Workbook book = ExcelUtils.buildBaseWorkbook(header,sheetName);
			Sheet sheet = book.getSheet(sheetName);

			int i=(header!=null)?1:0;
			int maxC=0;
			for(List<String> d:list) {
				if (d.size()>maxC) maxC=d.size();
				Row row = ExcelUtils.insertRowAt(sheet, i++, d.size());
				int j=0;
				for(String s:d) {
					row.getCell(j).setCellValue(s);
					j++;
				}
			}

			ExcelUtils.autoLayout((header!=null)?header.length:maxC, sheet);
			FileOutputStream out=new FileOutputStream(xlsOutputFile);
			book.write(out);
		}
	}

	public static Map<String, Integer> findColumnsWithHeader(Sheet sheet,int rowNum,boolean caseSensitive,boolean removeSpaces,String... columnNames) throws Exception {
		Map<String, Integer> ret=new HashMap<String, Integer>();
		if (sheet != null)
		{
			Row row = sheet.getRow(rowNum);
			for(Iterator<Cell> cellIter = row.cellIterator(); cellIter.hasNext(); ) {
				Cell cell = cellIter.next();
				if (cell.getCellType() == Cell.CELL_TYPE_STRING) {
					int column=cell.getColumnIndex();
					String v=cell.getStringCellValue();
					if (removeSpaces) v=v.replaceAll("[\\s]+", "");
					for(String cn:columnNames) {
						if (cn.equalsIgnoreCase(v)) {
							if (ret.containsKey(cn)) throw new Exception("multiple columns named (case insensitive comparison): "+cn);
							ret.put(cn, column);
							break;
						}
					}
				}
				boolean done=true;
				for(String cn:columnNames) {
					if (!ret.containsKey(cn)) {
						done=false;
						break;
					}
				}
				if (done) break;
			}
		}
		return ret;
	}

	public static void main(String[] args) throws Exception {
		//test("Simcoach_BillFord_TextNotes.xlsx", 0,1);
		List<File> files = FileUtils.getAllFiles(new File("C:\\Users\\morbini\\Desktop\\tmp\\sail-kiosk\\Plists\\"), ".*\\.xls");
		Map<String, List<String>> m=new HashMap<String, List<String>>();
		for (File f:files) {
			Map<String, List<String>> tmp = extractMappingBetweenTheseTwoColumns(f.getAbsolutePath(), 0, 0, 1,false);
			m.putAll(tmp);
		}
		dumpMapToExcel((Map)m, new File("C:\\Users\\morbini\\Desktop\\tmp\\sail-kiosk\\Plists\\combined.xlsx"), "plist", new String[]{"question","text"});
		System.exit(1);
		generateSasoLightTemplateFromSystemUtt("resources/characters/Ellie_DCAPS/content/system-utterances-woz.xlsx", 0);
	}
}
