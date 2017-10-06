package com.camunda.consulting.dmn_excel_tester;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.junit.Test;
import org.xlsx4j.exceptions.Xlsx4jException;

import com.camunda.consulting.dmn_excel_tester.logic.ExcelDmnValidator;
import com.camunda.consulting.dmn_excel_tester.logic.ExcelSheetReader;

public class ExcelDmnValidatorTest {
  
  @Test
  public void testExcelDmnValidatorFullyAdjusted() throws Docx4JException, Xlsx4jException {
    Map<String, List<Map<String, Object>>> dataFromExcel = readExcelFile("src/test/resources/dish-3Expected.xlsx");
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/dish-3.dmn");
    
    ExcelDmnValidator excelDmnValidator = new ExcelDmnValidator(dataFromExcel, dmnModelInstance);
    List<String> errorList = excelDmnValidator.validateMatchingExcelAndDmnModel();
    
    assertThat(errorList).isEmpty();
  }

  @Test
  public void testUnmatchedHeaders() throws Docx4JException, Xlsx4jException {
    Map<String, List<Map<String, Object>>> dataFromExcel = readExcelFile("src/test/resources/validationErrors/dish-simpleExpected.xlsx");
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/validationErrors/dish-simple.dmn");
    
    ExcelDmnValidator excelDmnValidator = new ExcelDmnValidator(dataFromExcel, dmnModelInstance);
    List<String> errorList = excelDmnValidator.validateMatchingExcelAndDmnModel();
    
    assertThat(errorList).hasSize(3);
    assertThat(errorList).contains("Column 'Jahreszeit' not found in Dmn Inputs of table 'Dish'"); 
    assertThat(errorList).contains("Column 'Gästeanzahl' not found in Dmn Inputs of table 'Dish'");
    assertThat(errorList).contains("Column 'Mahlzeit' not found in Dmn Output of table 'Dish'");
  }
  
  @Test
  public void testDRDHeadersFullyAdjusted() throws Docx4JException, Xlsx4jException {
    Map<String, List<Map<String, Object>>> dataFromExcel = readExcelFile("src/test/resources/drd/dinnerDecisionsExpected.xlsx");
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/drd/dinnerDecisions.dmn");
    
    ExcelDmnValidator excelDmnValidator = new ExcelDmnValidator(dataFromExcel, dmnModelInstance);
    List<String> errorList = excelDmnValidator.validateMatchingExcelAndDmnModel();
    
    assertThat(errorList).isEmpty();
  }
  
  @Test
  public void testDRDUnmatchedTables() throws Docx4JException, Xlsx4jException {
    Map<String, List<Map<String, Object>>> dataFromExcel = readExcelFile("src/test/resources/validationErrors/invoiceBusinessDecisionsDRDExpected.xlsx");
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/validationErrors/invoiceBusinessDecisionsDRD.dmn");
    
    ExcelDmnValidator excelDmnValidator = new ExcelDmnValidator(dataFromExcel, dmnModelInstance);
    List<String> errorList = excelDmnValidator.validateMatchingExcelAndDmnModel();
    
    assertThat(errorList).contains("Excel sheet 'Tabelle1' didn't match decisions 'Invoice Classification' or 'Assign Approver Group'");
  }

  public Map<String, List<Map<String, Object>>> readExcelFile(String fileName) throws Docx4JException, Xlsx4jException {
    File excelFile = new File(fileName);
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);    
    Map<String, List<Map<String, Object>>> dataFromExcel = excelSheetReader.getDataFromExcel();
    return dataFromExcel;
  }

  public DmnModelInstance readDmnModelInstance(String fileName) {
    File dmnTableFile = new File(fileName);
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    return dmnModelInstance;
  }
  
  @Test 
  public void testCheckEqualSheetTableLists() {
    List<String> sheetNames = Arrays.asList("dish", "beverages");
    List<String> tableNames = Arrays.asList("beverages", "dish");
    
    ExcelDmnValidator excelDmnValidator = new ExcelDmnValidator(null, null);
    String resultList = excelDmnValidator.checkMatchingSheetAndTableNames(sheetNames, tableNames);
    assertThat(resultList).isEmpty();
  }
  
  @Test 
  public void testCheckUnmatchedSingleSheetName() {
    List<String> tablesNames = Arrays.asList("dish", "beverages");
    List<String> sheetNames = Arrays.asList("Tabelle1");
    
    ExcelDmnValidator excelDmnValidator = new ExcelDmnValidator(null, null);
    String resultList = excelDmnValidator.checkMatchingSheetAndTableNames(sheetNames, tablesNames);
    assertThat(resultList).isEqualTo("Excel sheet 'Tabelle1' didn't match decisions 'dish' or 'beverages'");
  }

  @Test 
  public void testCheckUnmatched2SheetNames() {
    List<String> tablesNames = Arrays.asList("dish", "beverages", "location");
    List<String> sheetNames = Arrays.asList("Tabelle1", "location");
    
    ExcelDmnValidator excelDmnValidator = new ExcelDmnValidator(null, null);
    String resultList = excelDmnValidator.checkMatchingSheetAndTableNames(sheetNames, tablesNames);
    assertThat(resultList).isEqualTo("Excel sheet 'Tabelle1' didn't match decisions 'dish' or 'beverages'");
  }

  @Test 
  public void testCheckUnmatched3SheetNames() {
    List<String> tablesNames = Arrays.asList("dish", "beverages", "location");
    List<String> sheetNames = Arrays.asList("Tabelle1", "Tabelle2", "location");
    
    ExcelDmnValidator excelDmnValidator = new ExcelDmnValidator(null, null);
    String resultList = excelDmnValidator.checkMatchingSheetAndTableNames(sheetNames, tablesNames);
    assertThat(resultList).isEqualTo("Excel sheets 'Tabelle1' and 'Tabelle2' didn't match decisions 'dish' or 'beverages'");
  }

}
