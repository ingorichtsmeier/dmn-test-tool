package com.camunda.consulting.dmn_excel_tester;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.camunda.bpm.dmn.engine.DmnDecision;
import org.camunda.bpm.dmn.engine.DmnEngine;
import org.camunda.bpm.dmn.engine.DmnEngineConfiguration;
import org.camunda.bpm.model.dmn.Dmn;
import org.camunda.bpm.model.dmn.DmnModelInstance;
import org.camunda.bpm.model.dmn.instance.Decision;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.junit.Test;
import org.xlsx4j.exceptions.Xlsx4jException;

import com.camunda.consulting.dmn_excel_tester.functional.Tuple;
import com.camunda.consulting.dmn_excel_tester.logic.ExcelDmnValidator;
import com.camunda.consulting.dmn_excel_tester.logic.ExcelSheetReader;

public class ExcelDmnValidatorTest {
  
  @Test
  public void testExcelDmnValidatorFullyAdjusted() throws Docx4JException, Xlsx4jException {
    Map<String, List<Map<String, Object>>> dataFromExcel = readExcelFile("src/test/resources/dish-3Expected.xlsx");
    List<Map<String, Object>> sheetData = dataFromExcel.get("Tabelle1");
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/dish-3.dmn");
    Decision decision = ExcelDmnValidator.mapDecisionsByNames.apply(dmnModelInstance).get("Dish");
    
    List<String> errorList = ExcelDmnValidator.checkHeaders
        .apply(new Tuple<String, List<Map<String, Object>>>("Tabelle1", sheetData), 
            new Tuple<Decision, DmnModelInstance>(decision, dmnModelInstance))
        .collect(Collectors.toList());
    
    assertThat(errorList).isEmpty();
  }

  @Test
  public void testExcelDmnValidatorFullyAdjustedOfExcelFileAndModelInstance() throws Docx4JException, Xlsx4jException {
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
    Decision decision = ExcelDmnValidator.mapDecisionsByNames.apply(dmnModelInstance).get("Dish");
    
    List<String> errorList = ExcelDmnValidator.checkHeaders
        .apply(new Tuple<String, List<Map<String, Object>>>("Tabelle1", sheetdata), 
            new Tuple<Decision, DmnModelInstance>(decision, dmnModelInstance))
        .collect(Collectors.toList());
    
    assertThat(errorList).hasSize(6);
    assertThat(errorList).containsOnly("Column 'Jahreszeit' not found in Dmn Input of table 'Dish'",
        "Column 'Gästeanzahl' not found in Dmn Input of table 'Dish'",
        "Column 'Mahlzeit' not found in Dmn Output of table 'Dish'",
        "Input column 'Season' not found in Excel sheet 'Tabelle1'",
        "Input column 'Number of guests' not found in Excel sheet 'Tabelle1'",
        "Output column 'Expected: Dish' not found in Excel sheet 'Tabelle1'");
  }

  @Test
  public void testUnmatchedHeadersOfExcelFileAndModelInstance() throws Docx4JException, Xlsx4jException {
    Map<String, List<Map<String, Object>>> dataFromExcel = readExcelFile("src/test/resources/validationErrors/dish-simpleExpected.xlsx");
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/validationErrors/dish-simple.dmn");
    
    List<String> errorList = ExcelDmnValidator.validateExcelAndDmnModel.apply(dataFromExcel, dmnModelInstance);
    
    assertThat(errorList).hasSize(6);
    assertThat(errorList).containsOnly("Column 'Jahreszeit' not found in Dmn Input of table 'Dish'",
        "Column 'Gästeanzahl' not found in Dmn Input of table 'Dish'",
        "Column 'Mahlzeit' not found in Dmn Output of table 'Dish'",
        "Input column 'Season' not found in Excel sheet 'Tabelle1'",
        "Input column 'Number of guests' not found in Excel sheet 'Tabelle1'",
        "Output column 'Expected: Dish' not found in Excel sheet 'Tabelle1'");
  }
  
  @Test
  public void testGetInputHeadersForDecicionId() {
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/dish-3.dmn");
    List<String> inputHeaders = ExcelDmnValidator.getInputHeadersForDecisionId.apply(dmnModelInstance, "dish-decision")
        .collect(Collectors.toList());
    assertThat(inputHeaders).containsOnly("Season", "How many guests");
  }
  
  @Test
  public void testGetOutputLabelsForDecicionId() {
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/dish-3.dmn");
    List<String> inputHeaders = ExcelDmnValidator.getOutputLabelsForDecisionId.apply(dmnModelInstance, "dish-decision")
        .collect(Collectors.toList());
    assertThat(inputHeaders).containsOnly("Dish");
  }
  
  @SuppressWarnings("unchecked")
  @Test
  public void testTupleMapping() {
    Tuple<List<String>, List<String>> tuple = new Tuple<>(asList("Special Guests", "Reason"), asList("Guest name"));
    
    List<Tuple<String, List<String>>> tupleList = ExcelDmnValidator.mapTupleListToListOfTuple.apply(tuple);
    
    assertThat(tupleList).containsOnly(
        new Tuple<String, List<String>>("Special Guests", asList("Guest name")), 
        new Tuple<String, List<String>>("Reason", new ArrayList<>()));
  }
  
  @SuppressWarnings("unchecked")
  @Test
  public void testCreateInputToReplaceListDRD() {
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/drd/dinnerDecisions.dmn");
    Decision beveragesDecision = ExcelDmnValidator.mapDecisionsByNames.apply(dmnModelInstance).get("Beverages");

    List<Tuple<List<String>, List<String>>> mappingList = ExcelDmnValidator.createInputToReplaceList.apply(dmnModelInstance, beveragesDecision);
    
    assertThat(mappingList).containsOnly(new Tuple<>(asList("Dish"), asList("Season", "Number of Guests")));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testCreateInputToReplaceListDRDTwoRequirements() {
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/drd/dinner-with-normal-guests.dmn");
    Decision beveragesDecision = ExcelDmnValidator.mapDecisionsByNames.apply(dmnModelInstance).get("Beverages");

    List<Tuple<List<String>, List<String>>> mappingList = ExcelDmnValidator.createInputToReplaceList.apply(dmnModelInstance, beveragesDecision);
    
    assertThat(mappingList).containsOnly(
        new Tuple<>(asList("Dish"), asList("Season")),
        new Tuple<>(asList("Special Guests"), asList("Guest name")));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testCreateInputToReplaceListDRDTwoRequirementsAndTwoOutputs() {
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/drd/dinner-with-special-guests.dmn");
    Decision beveragesDecision = ExcelDmnValidator.mapDecisionsByNames.apply(dmnModelInstance).get("Beverages");

    List<Tuple<List<String>, List<String>>> mappingList = ExcelDmnValidator.createInputToReplaceList.apply(dmnModelInstance, beveragesDecision);
    
    assertThat(mappingList).containsOnly(
        new Tuple<>(asList("Dish"), asList("Season")),
        new Tuple<>(asList("Special Guests", "Reason"), asList("Guest name")));
  }
  
  @Test
  public void testCreateInputReplaceMappingDRD() {
    List<Tuple<List<String>, List<String>>> mappingList = asList(new Tuple<>(asList("Dish"), asList("Season", "Number of Guests")));
    Map<String, List<String>> mapping = ExcelDmnValidator.createReplaceMapping.apply(mappingList);
    assertThat(mapping).containsOnly(entry("Dish", asList("Season", "Number of Guests")));
  }

  @Test
  public void testCreateInputReplaceMappingDRDTwoRequirements() {
    List<Tuple<List<String>, List<String>>> mappingList = asList(
        new Tuple<>(asList("Dish"), asList("Season")),
        new Tuple<>(asList("Special Guests"), asList("Guest name")));
    Map<String, List<String>> mapping = ExcelDmnValidator.createReplaceMapping.apply(mappingList);
    assertThat(mapping).containsOnly(
        entry("Dish", asList("Season")), 
        entry("Special Guests", asList("Guest name")));
  }

  // TODO: @Test testCreateInputReplaceMappingDRDTwoRequirementsTwoOutputs()
  @Test
  public void testCreateInputReplaceMappingDRDTwoRequirementsTwoOutputs() {
    List<Tuple<List<String>, List<String>>> mappingList = asList(
        new Tuple<>(asList("Dish"), asList("Season")),
        new Tuple<>(asList("Special Guests", "Reason"), asList("Guest name")));
    Map<String, List<String>> mapping = ExcelDmnValidator.createReplaceMapping.apply(mappingList);
    assertThat(mapping).containsOnly(
        entry("Dish", asList("Season")), 
        entry("Special Guests", asList("Guest name")), 
        entry("Reason", new ArrayList<>()));
  }

  @Test
  public void testReplaceRequiredInputHeadersNothingToReplace() {
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/dish-3.dmn");
    Decision dishDecision = ExcelDmnValidator.mapDecisionsByNames.apply(dmnModelInstance).get("Dish");

    List<String> inputHeaders = ExcelDmnValidator.replaceRequiredInputHeaders.apply(dmnModelInstance, dishDecision).collect(Collectors.toList());
    assertThat(inputHeaders).containsOnly("Season", "How many guests");
  }
  
  @Test
  public void testReplaceRequiredInputHeaders() {
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/drd/dinnerDecisions.dmn");
    Decision beveragesDecision = ExcelDmnValidator.mapDecisionsByNames.apply(dmnModelInstance).get("Beverages");

    List<String> inputHeaders = ExcelDmnValidator.replaceRequiredInputHeaders.apply(dmnModelInstance, beveragesDecision).collect(Collectors.toList());
    assertThat(inputHeaders).containsOnly("Season", "Number of Guests", "Guests with children");
  }
  
  @Test
  public void testReplaceRequiredInputHeadersTwoRequirements() {
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/drd/dinner-with-normal-guests.dmn");
    Decision beveragesDecision = ExcelDmnValidator.mapDecisionsByNames.apply(dmnModelInstance).get("Beverages");

    List<String> inputHeaders = ExcelDmnValidator.replaceRequiredInputHeaders.apply(dmnModelInstance, beveragesDecision).collect(Collectors.toList());
    assertThat(inputHeaders).containsOnly("Season", "Guest name");
  }
  
  // TODO: @Test testReplaceRequiredInputHeadersTwoOutputsForRequirements
  @Test
  public void testReplaceRequiredInputHeadersTwoOutputsForRequirements() {
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/drd/dinner-with-special-guests.dmn");
    Decision beveragesDecision = ExcelDmnValidator.mapDecisionsByNames.apply(dmnModelInstance).get("Beverages");

    List<String> inputHeaders = ExcelDmnValidator.replaceRequiredInputHeaders.apply(dmnModelInstance, beveragesDecision).collect(Collectors.toList());
    assertThat(inputHeaders).containsOnly("Season", "Guest name");
  }
  
  @Test
  public void testDRDHeadersFullyAdjusted() throws Docx4JException, Xlsx4jException {
    Map<String, List<Map<String, Object>>> dataFromExcel = readExcelFile("src/test/resources/drd/dinnerDecisionsExpected.xlsx");
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/drd/dinnerDecisions.dmn");
    
    List<String> errorList = ExcelDmnValidator.validateExcelAndDmnModel.apply(dataFromExcel, dmnModelInstance);
    
    assertThat(errorList).isEmpty();
  }
  
  @Test
  public void testDRDMissingRequiredInput() throws Docx4JException, Xlsx4jException {
    Map<String, List<Map<String, Object>>> dataFromExcel = readExcelFile("src/test/resources/drd/dinnerDecisionsExpected-missingRequired.xlsx");
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/drd/dinnerDecisions.dmn");
    
    List<String> errorList = ExcelDmnValidator.validateExcelAndDmnModel.apply(dataFromExcel, dmnModelInstance);
    
    assertThat(errorList).hasSize(1);
    assertThat(errorList).containsOnly("Input column 'Season' not found in Excel sheet 'Beverages'");
  }

  @Test
  public void testDRDHeadersMismatch() throws Docx4JException, Xlsx4jException {
    Map<String, List<Map<String, Object>>> dataFromExcel = readExcelFile("src/test/resources/drd/dinnerDecisionsExpected-errors.xlsx");
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/drd/dinnerDecisions.dmn");
    
    List<String> errorList = ExcelDmnValidator.validateExcelAndDmnModel.apply(dataFromExcel, dmnModelInstance);
    
    assertThat(errorList).hasSize(8);
    assertThat(errorList).containsOnly("Column 'Anzahl der Gäste' not found in Dmn Input of table 'Dish'",
        "Column 'Mahlzeit' not found in Dmn Output of table 'Dish'",
        "Column 'Jahreszeit' not found in Dmn Input of table 'Beverages'",
        "Column 'Getränke' not found in Dmn Output of table 'Beverages'",
        "Input column 'Number of Guests' not found in Excel sheet 'Dish'",
        "Output column 'Expected: Dish' not found in Excel sheet 'Dish'",
        "Input column 'Season' not found in Excel sheet 'Beverages'",
        "Output column 'Expected: Beverages' not found in Excel sheet 'Beverages'");
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
    List<String> sheetNames = asList("dish", "beverages");
    List<String> tableNames = asList("beverages", "dish");
    
    String resultList = ExcelDmnValidator.checkMatchingSheetAndTableNames.apply(sheetNames, tableNames);
    assertThat(resultList).isEmpty();
  }
  
  @Test
  public void testCheckSingleSheetAndSingleTable() {
    List<String> sheetNames = asList("Tabelle1");
    List<String> tableNames = asList("Dish");
    
    String result = ExcelDmnValidator.checkMatchingSheetAndTableNames.apply(sheetNames, tableNames);
    assertThat(result).isEmpty();
  }
  
  @Test 
  public void testCheckUnmatchedSingleSheetName() {
    List<String> tablesNames = asList("dish", "beverages");
    List<String> sheetNames = asList("Tabelle1");
    
    String resultList = ExcelDmnValidator.checkMatchingSheetAndTableNames.apply(sheetNames, tablesNames);
    assertThat(resultList).isEqualTo("Excel sheet 'Tabelle1' didn't match decisions 'dish' or 'beverages'");
  }

  @Test 
  public void testCheckUnmatched2SheetNames() {
    List<String> tablesNames = asList("dish", "beverages", "location");
    List<String> sheetNames = asList("Tabelle1", "location");
    
    String resultList = ExcelDmnValidator.checkMatchingSheetAndTableNames.apply(sheetNames, tablesNames);
    assertThat(resultList).isEqualTo("Excel sheet 'Tabelle1' didn't match decisions 'dish' or 'beverages'");
  }

  @Test 
  public void testCheckUnmatched3SheetNames() {
    List<String> tablesNames = asList("dish", "beverages", "location");
    List<String> sheetNames = asList("Tabelle1", "Tabelle2", "location");
    
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
  
  @Test
  public void testFindDecisionForSheetFromDRD() throws Docx4JException, Xlsx4jException {
    DmnModelInstance dmnModelInstance = readDmnModelInstance("src/test/resources/drd/dinnerDecisions.dmn");
    
    Decision dishDecision = ExcelDmnValidator.findDecisionForSheet.apply(dmnModelInstance, "Dish");
    Decision beveragesDecision = ExcelDmnValidator.findDecisionForSheet.apply(dmnModelInstance, "Beverages");

    assertThat(dishDecision.getName()).isEqualTo("Dish");
    assertThat(beveragesDecision.getName()).isEqualTo("Beverages");
  }
}
