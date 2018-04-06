package com.camunda.consulting.dmn_excel_tester;

import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.Decision;
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
    
    List<String> errorList = ExcelDmnValidator.validateExcelAndDmnModel.apply(dataFromExcel, dmnModelInstance);
    
    assertThat(errorList).isEmpty();
  }

  @Test
  public void testUnmatchedHeaders() throws Docx4JException, Xlsx4jException {
    Map<String, List<Map<String, Object>>> dataFromExcel = readExcelFile("src/test/resources/validationErrors/dish-simpleExpected.xlsx");
    List<Map<String, Object>> sheetdata = dataFromExcel.get("Tabelle1");
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/validationErrors/dish-simple.dmn");
    Decision decision = dmnModelInstance.getModelElementsByType(Decision.class).iterator().next();
    
    List<String> errorList = ExcelDmnValidator.checkHeaders.apply(sheetdata, decision);
    
    assertThat(errorList).hasSize(3);
    assertThat(errorList).containsOnly("Column 'Jahreszeit' not found in Dmn Input of table 'Dish'",
        "Column 'Gästeanzahl' not found in Dmn Input of table 'Dish'",
        "Column 'Mahlzeit' not found in Dmn Output of table 'Dish'");
  }

  @Test
  public void testUnmatchedHeadersOfExcelFileAndModelInstance() throws Docx4JException, Xlsx4jException {
    Map<String, List<Map<String, Object>>> dataFromExcel = readExcelFile("src/test/resources/validationErrors/dish-simpleExpected.xlsx");
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/validationErrors/dish-simple.dmn");
    
    List<String> errorList = ExcelDmnValidator.validateExcelAndDmnModel.apply(dataFromExcel, dmnModelInstance);
    
    assertThat(errorList).hasSize(3);
    assertThat(errorList).containsOnly("Column 'Jahreszeit' not found in Dmn Input of table 'Dish'",
        "Column 'Gästeanzahl' not found in Dmn Input of table 'Dish'",
        "Column 'Mahlzeit' not found in Dmn Output of table 'Dish'");
  }
  
  @Test
  public void testDRDHeadersFullyAdjusted() throws Docx4JException, Xlsx4jException {
    Map<String, List<Map<String, Object>>> dataFromExcel = readExcelFile("src/test/resources/drd/dinnerDecisionsExpected.xlsx");
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/drd/dinnerDecisions.dmn");
    
    List<String> errorList = ExcelDmnValidator.validateExcelAndDmnModel.apply(dataFromExcel, dmnModelInstance);
    
    assertThat(errorList).isEmpty();
  }
  
  @Test
  public void testDRDHeadersMismatch() throws Docx4JException, Xlsx4jException {
    Map<String, List<Map<String, Object>>> dataFromExcel = readExcelFile("src/test/resources/drd/dinnerDecisionsExpected-errors.xlsx");
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/drd/dinnerDecisions.dmn");
    
    List<String> errorList = ExcelDmnValidator.validateExcelAndDmnModel.apply(dataFromExcel, dmnModelInstance);
    
    assertThat(errorList).hasSize(4);
    assertThat(errorList).containsOnly("Column 'Anzahl der Gäste' not found in Dmn Input of table 'Dish'",
        "Column 'Mahlzeit' not found in Dmn Output of table 'Dish'",
        "Column 'Jahreszeit' not found in Dmn Input of table 'Beverages'",
        "Column 'Getränke' not found in Dmn Output of table 'Beverages'");
  }
  
  @Test
  public void testDRDMissingRequiredInput() throws Docx4JException, Xlsx4jException {
    Map<String, List<Map<String, Object>>> dataFromExcel = readExcelFile("src/test/resources/drd/dinnerDecisionsExpected-missingRequired.xlsx");
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/drd/dinnerDecisions.dmn");
    
    List<String> errorList = ExcelDmnValidator.validateExcelAndDmnModel.apply(dataFromExcel, dmnModelInstance);
    
    assertThat(errorList).hasSize(1);
    assertThat(errorList).containsOnly("Column 'Season' not found in Dmn Input of table 'Beverages'");
  }
  
  @Test
  public void testDRDUnmatchedTables() throws Docx4JException, Xlsx4jException {
    Map<String, List<Map<String, Object>>> dataFromExcel = readExcelFile("src/test/resources/validationErrors/invoiceBusinessDecisionsDRDExpected.xlsx");
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/validationErrors/invoiceBusinessDecisionsDRD.dmn");
    
    List<String> errorList = ExcelDmnValidator.validateExcelAndDmnModel.apply(dataFromExcel, dmnModelInstance);
    
    assertThat(errorList).contains("Excel sheet 'Tabelle1' didn't match decisions 'Invoice Classification' or 'Assign Approver Group'");
  }

  private Map<String, List<Map<String, Object>>> readExcelFile(String fileName) throws Docx4JException, Xlsx4jException {
    File excelFile = new File(fileName);
    ExcelSheetReader excelSheetReader = new ExcelSheetReader(excelFile);    
    Map<String, List<Map<String, Object>>> dataFromExcel = excelSheetReader.getDataFromExcel();
    return dataFromExcel;
  }

  private DmnModelInstance readDmnModelInstance(String fileName) {
    File dmnTableFile = new File(fileName);
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(dmnTableFile);
    return dmnModelInstance;
  }
  
  @Test 
  public void testCheckEqualSheetTableLists() {
    List<String> sheetNames = Arrays.asList("dish", "beverages");
    List<String> tableNames = Arrays.asList("beverages", "dish");
    
    String resultList = ExcelDmnValidator.checkMatchingSheetAndTableNames.apply(sheetNames, tableNames);
    assertThat(resultList).isEmpty();
  }
  
  @Test
  public void testCheckSingleSheetAndSingleTable() {
    List<String> sheetNames = Arrays.asList("Tabelle1");
    List<String> tableNames = Arrays.asList("Dish");
    
    String result = ExcelDmnValidator.checkMatchingSheetAndTableNames.apply(sheetNames, tableNames);
    assertThat(result).isEmpty();
  }
  
  @Test 
  public void testCheckUnmatchedSingleSheetName() {
    List<String> tablesNames = Arrays.asList("dish", "beverages");
    List<String> sheetNames = Arrays.asList("Tabelle1");
    
    String resultList = ExcelDmnValidator.checkMatchingSheetAndTableNames.apply(sheetNames, tablesNames);
    assertThat(resultList).isEqualTo("Excel sheet 'Tabelle1' didn't match decisions 'dish' or 'beverages'");
  }

  @Test 
  public void testCheckUnmatched2SheetNames() {
    List<String> tablesNames = Arrays.asList("dish", "beverages", "location");
    List<String> sheetNames = Arrays.asList("Tabelle1", "location");
    
    String resultList = ExcelDmnValidator.checkMatchingSheetAndTableNames.apply(sheetNames, tablesNames);
    assertThat(resultList).isEqualTo("Excel sheet 'Tabelle1' didn't match decisions 'dish' or 'beverages'");
  }

  @Test 
  public void testCheckUnmatched3SheetNames() {
    List<String> tablesNames = Arrays.asList("dish", "beverages", "location");
    List<String> sheetNames = Arrays.asList("Tabelle1", "Tabelle2", "location");
    
    String resultList = ExcelDmnValidator.checkMatchingSheetAndTableNames.apply(sheetNames, tablesNames);
    assertThat(resultList).isEqualTo("Excel sheets 'Tabelle1' and 'Tabelle2' didn't match decisions 'dish' or 'beverages'");
  }
  
  @Test
  public void testMapDmnDecisions() {
    File drdFile = new File("src/test/resources/drd/dinnerDecisions.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(drdFile);

    DmnEngine dmnEngine = DmnEngineConfiguration.createDefaultDmnEngineConfiguration().buildEngine();
    List<DmnDecision> decisions = dmnEngine.parseDecisions(dmnModelInstance);
    
    Map<String, DmnDecision> decisionMap = ExcelDmnValidator.mapDmnDecisionByNames.apply(decisions);
    
    assertThat(decisionMap).containsOnlyKeys("Beverages", "Dish");
  }
  
  @Test 
  public void testMapDecisions() {
    File drdFile = new File("src/test/resources/drd/dinnerDecisions.dmn");
    DmnModelInstance dmnModelInstance = Dmn.readModelFromFile(drdFile);

    Map<String, Decision> decisionMap = ExcelDmnValidator.mapDecisionsByNames.apply(dmnModelInstance);
    
    assertThat(decisionMap).containsOnlyKeys("Beverages", "Dish");
  }
  
  /**
   * Decisons: Dish
   * Sheets: Tabelle 1
   * result: Dish
   */
  @Test
  public void testFindDecisionForSheet() throws Docx4JException, Xlsx4jException {
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/dish-3.dmn");
    
    Decision decision = ExcelDmnValidator.findDecisionForSheet.apply(dmnModelInstance, "Tabelle1");
    
    assertThat(decision.getName()).isEqualTo("Dish");
  }
}
